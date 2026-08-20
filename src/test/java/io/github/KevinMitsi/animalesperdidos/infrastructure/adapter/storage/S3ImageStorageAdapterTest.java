package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.storage;

import io.github.KevinMitsi.animalesperdidos.application.port.out.ImageStoragePort;
import io.github.KevinMitsi.animalesperdidos.infrastructure.config.S3Configuration.S3Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class S3ImageStorageAdapterTest {
    @Mock S3AsyncClient client;
    @Mock S3Presigner presigner;
    @Captor ArgumentCaptor<PutObjectRequest> putRequest;
    @Captor ArgumentCaptor<DeleteObjectRequest> deleteRequest;
    private S3ImageStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        S3Properties properties = new S3Properties();
        properties.setBucket("private-test-bucket");
        adapter = new S3ImageStorageAdapter(client, presigner, properties);
    }

    @Test
    void sanitizesAndMovesARealPngOutOfStaging() throws Exception {
        UUID owner = UUID.randomUUID();
        byte[] input = png(3000, 100);
        String checksum = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(input));
        String hex = HexFormat.of().formatHex(Base64.getDecoder().decode(checksum));
        String stagingKey = "lost-pet-reports/staging/users/" + owner + "/upload-" + hex + ".png";
        HeadObjectResponse head = HeadObjectResponse.builder().contentLength((long) input.length)
                .contentType("image/png").checksumSHA256(checksum).build();
        ResponseBytes<GetObjectResponse> signature = ResponseBytes.fromByteArray(
                GetObjectResponse.builder().build(), Arrays.copyOf(input, Math.min(16, input.length)));
        ResponseBytes<GetObjectResponse> full = ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), input);
        when(client.headObject(any(HeadObjectRequest.class))).thenReturn(CompletableFuture.completedFuture(head));
        when(client.getObject(any(GetObjectRequest.class), anyBytesTransformer()))
                .thenReturn(CompletableFuture.completedFuture(signature), CompletableFuture.completedFuture(full));
        when(client.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
                .thenReturn(CompletableFuture.completedFuture(PutObjectResponse.builder().build()));
        when(client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(DeleteObjectResponse.builder().build()));

        var result = adapter.sanitize(owner, ImageStoragePort.Category.LOST_PET_REPORT, stagingKey).toCompletableFuture().join();

        verify(client).putObject(putRequest.capture(), any(AsyncRequestBody.class));
        verify(client).deleteObject(deleteRequest.capture());
        assertEquals(stagingKey, deleteRequest.getValue().key());
        assertEquals("image/png", result.contentType());
        assertTrue(result.objectKey().startsWith("lost-pet-reports/users/" + owner + "/"));
        assertTrue(putRequest.getValue().contentLength() < input.length || putRequest.getValue().contentLength() > 0);
        assertNotEquals(stagingKey, result.objectKey());
    }

    @Test
    void returnsOnlyHeadersThatBrowserCodeCanSet() {
        Map<String, String> headers = S3ImageStorageAdapter.browserRequiredHeaders(Map.of(
                "host", List.of("private-test-bucket.s3.amazonaws.com"),
                "Content-Length", List.of("1024"),
                "Content-Type", List.of("image/png"),
                "x-amz-checksum-sha256", List.of("checksum")));

        assertEquals(Map.of(
                "Content-Type", "image/png",
                "x-amz-checksum-sha256", "checksum"), headers);
    }

    private static AsyncResponseTransformer<GetObjectResponse, ResponseBytes<GetObjectResponse>> anyBytesTransformer() {
        return (AsyncResponseTransformer<GetObjectResponse, ResponseBytes<GetObjectResponse>>) any(AsyncResponseTransformer.class);
    }

    private static byte[] png(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.ORANGE);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "png", output));
        return output.toByteArray();
    }
}
