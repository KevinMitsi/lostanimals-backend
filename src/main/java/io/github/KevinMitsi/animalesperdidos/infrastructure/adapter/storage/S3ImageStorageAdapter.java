package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.storage;

import io.github.KevinMitsi.animalesperdidos.application.port.out.ImageStoragePort;
import io.github.KevinMitsi.animalesperdidos.infrastructure.config.S3Configuration.S3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

@Component
@RequiredArgsConstructor
public class S3ImageStorageAdapter implements ImageStoragePort {
    private final S3AsyncClient client;
    private final S3Presigner presigner;
    private final S3Properties properties;

    @Override
    public CompletionStage<String> store(UUID reportId, String fileName, String contentType, byte[] content) {
        String key = "lost-pet-reports/" + reportId + "/" + UUID.randomUUID() + extension(fileName);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .contentType(contentType)
                .build();
        return client.putObject(request, AsyncRequestBody.fromBytes(content)).thenApply(ignored -> key);
    }

    @Override
    public CompletionStage<Void> delete(String objectKey) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .build();
        return client.deleteObject(request).thenApply(ignored -> null);
    }

    @Override
    public CompletionStage<PreparedUpload> prepareUpload(UUID ownerId, String fileName, String contentType,
                                                          long contentLength, String checksumSha256, Duration validity) {
        String checksumHex = java.util.HexFormat.of().formatHex(java.util.Base64.getDecoder().decode(checksumSha256));
        String key = "lost-pet-reports/staging/users/" + ownerId + "/" + UUID.randomUUID() + "-" + checksumHex
                + extensionFor(contentType);
        PutObjectRequest objectRequest = PutObjectRequest.builder().bucket(properties.getBucket()).key(key)
                .contentType(contentType).contentLength(contentLength).checksumSHA256(checksumSha256).build();
        var signed = presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(validity).putObjectRequest(objectRequest).build());
        Map<String, String> headers = new LinkedHashMap<>();
        signed.signedHeaders().forEach((name, values) -> {
            if (!name.equalsIgnoreCase("host")) headers.put(name, String.join(",", values));
        });
        return java.util.concurrent.CompletableFuture.completedFuture(new PreparedUpload(key,
                signed.url().toExternalForm(), "PUT", Map.copyOf(headers), Instant.now().plus(validity)));
    }

    @Override
    public CompletionStage<StoredObject> inspect(String objectKey) {
        HeadObjectRequest request = HeadObjectRequest.builder().bucket(properties.getBucket()).key(objectKey)
                .checksumMode(ChecksumMode.ENABLED).build();
        return client.headObject(request).thenCompose(response -> {
            GetObjectRequest sniffRequest = GetObjectRequest.builder().bucket(properties.getBucket()).key(objectKey)
                    .range("bytes=0-15").build();
            return client.getObject(sniffRequest, AsyncResponseTransformer.toBytes())
                    .thenApply(bytes -> new StoredObject(objectKey, detectContentType(bytes.asByteArray()),
                            response.contentLength(), response.checksumSHA256()));
        });
    }

    @Override
    public CompletionStage<StoredObject> sanitize(UUID ownerId, String stagingObjectKey) {
        String requiredPrefix = "lost-pet-reports/staging/users/" + ownerId + "/";
        if (!stagingObjectKey.startsWith(requiredPrefix)) {
            return java.util.concurrent.CompletableFuture.failedFuture(new IllegalArgumentException("Invalid staging key"));
        }
        return inspect(stagingObjectKey).thenCompose(staged -> {
            if (!checksumMatchesKey(staged) || staged.contentLength() <= 0 || staged.contentLength() > 8L * 1024 * 1024
                    || !java.util.List.of("image/jpeg", "image/png").contains(staged.contentType())) {
                return java.util.concurrent.CompletableFuture.failedFuture(new IllegalArgumentException("Invalid image object"));
            }
            GetObjectRequest download = GetObjectRequest.builder().bucket(properties.getBucket()).key(stagingObjectKey).build();
            return client.getObject(download, AsyncResponseTransformer.toBytes())
                    .thenCompose(bytes -> Mono.fromCallable(() -> sanitizeBytes(bytes.asByteArray(), staged.contentType()))
                            .subscribeOn(Schedulers.boundedElastic()).toFuture())
                    .thenCompose(sanitized -> persistSanitized(ownerId, stagingObjectKey, sanitized));
        });
    }

    @Override
    public CompletionStage<String> createDownloadUrl(String objectKey, Duration validity) {
        GetObjectRequest objectRequest = GetObjectRequest.builder().bucket(properties.getBucket()).key(objectKey).build();
        var signed = presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(validity).getObjectRequest(objectRequest).build());
        return java.util.concurrent.CompletableFuture.completedFuture(signed.url().toExternalForm());
    }

    private static String extension(String fileName) {
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        String extension = fileName.substring(dot).toLowerCase(Locale.ROOT);
        return extension.matches("\\.[a-z0-9]{1,5}") ? extension : "";
    }

    private static String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }

    private static String detectContentType(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8
                && (bytes[2] & 0xff) == 0xff) return "image/jpeg";
        if (bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e
                && bytes[3] == 0x47 && bytes[4] == 0x0d && bytes[5] == 0x0a && bytes[6] == 0x1a
                && bytes[7] == 0x0a) return "image/png";
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') return "image/webp";
        return "application/octet-stream";
    }

    private CompletionStage<StoredObject> persistSanitized(UUID ownerId, String stagingKey, SanitizedBytes sanitized) {
        String checksumHex = java.util.HexFormat.of().formatHex(java.util.Base64.getDecoder().decode(sanitized.checksum()));
        String finalKey = "lost-pet-reports/users/" + ownerId + "/" + UUID.randomUUID() + "-" + checksumHex
                + extensionFor(sanitized.contentType());
        PutObjectRequest put = PutObjectRequest.builder().bucket(properties.getBucket()).key(finalKey)
                .contentType(sanitized.contentType()).contentLength((long) sanitized.bytes().length)
                .checksumSHA256(sanitized.checksum()).build();
        return client.putObject(put, AsyncRequestBody.fromBytes(sanitized.bytes()))
                .thenCompose(ignored -> delete(stagingKey))
                .thenApply(ignored -> new StoredObject(finalKey, sanitized.contentType(), sanitized.bytes().length,
                        sanitized.checksum()));
    }

    private static SanitizedBytes sanitizeBytes(byte[] input, String contentType) throws IOException {
        BufferedImage source;
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(input))) {
            java.util.Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new IOException("Unsupported image");
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > 12000 || height > 12000
                        || (long) width * height > 40_000_000L) throw new IOException("Image dimensions are unsafe");
                source = reader.read(0);
            } finally {
                reader.dispose();
            }
        }
        int maxSide = Math.max(source.getWidth(), source.getHeight());
        double scale = Math.min(1d, 2048d / maxSide);
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        int imageType = contentType.equals("image/jpeg") ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage clean = new BufferedImage(width, height, imageType);
        Graphics2D graphics = clean.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        String format = contentType.equals("image/jpeg") ? "jpg" : "png";
        if (!ImageIO.write(clean, format, output)) throw new IOException("Image encoding failed");
        byte[] bytes = output.toByteArray();
        if (bytes.length > 8L * 1024 * 1024) throw new IOException("Sanitized image remains too large");
        String checksum;
        try {
            checksum = java.util.Base64.getEncoder().encodeToString(
                    java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
        return new SanitizedBytes(bytes, contentType, checksum);
    }

    private static boolean checksumMatchesKey(StoredObject object) {
        if (object.checksumSha256() == null) return false;
        try {
            String hex = java.util.HexFormat.of().formatHex(java.util.Base64.getDecoder().decode(object.checksumSha256()));
            return object.objectKey().contains("-" + hex + ".");
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    private record SanitizedBytes(byte[] bytes, String contentType, String checksum) { }
}
