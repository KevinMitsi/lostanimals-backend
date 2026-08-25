package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.validation.NoSqlInjection;
import jakarta.validation.constraints.*;

@NoSqlInjection
public record PrepareImageUploadRequest(
        @NotBlank @Size(max = 255) @Pattern(regexp = "^[^/\\\\]+$", message = "must be a file name without a path")
        @Schema(example = "luna.webp") String fileName,
        @NotBlank @Pattern(regexp = "^image/(jpeg|png)$") @Schema(example = "image/jpeg") String contentType,
        @Positive @Max(8388608) @Schema(example = "1250000") long contentLength,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9+/]{43}=$", message = "must be a Base64 SHA-256 digest")
        @Schema(description = "Base64-encoded SHA-256 checksum calculated before upload") String checksumSha256) { }
