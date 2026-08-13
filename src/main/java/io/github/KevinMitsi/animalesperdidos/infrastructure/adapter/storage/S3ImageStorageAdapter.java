package io.github.kevinmitsi.animalesperdidos.infrastructure.adapter.storage;

import io.github.kevinmitsi.animalesperdidos.application.port.out.ImageStoragePort;
import io.github.kevinmitsi.animalesperdidos.infrastructure.config.S3Configuration.S3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

@Component
@RequiredArgsConstructor
public class S3ImageStorageAdapter implements ImageStoragePort {
    private final S3AsyncClient client;
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

    private static String extension(String fileName) {
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        String extension = fileName.substring(dot).toLowerCase(Locale.ROOT);
        return extension.matches("\\.[a-z0-9]{1,5}") ? extension : "";
    }
}
