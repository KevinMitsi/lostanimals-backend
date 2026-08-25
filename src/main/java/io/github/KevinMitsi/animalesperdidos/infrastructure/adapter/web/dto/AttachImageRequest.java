package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;

import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.validation.NoSqlInjection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@NoSqlInjection
public record AttachImageRequest(@NotBlank @Size(max = 1024) String objectKey) { }
