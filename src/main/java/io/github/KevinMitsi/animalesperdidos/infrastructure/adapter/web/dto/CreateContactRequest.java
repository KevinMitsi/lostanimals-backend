package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.validation.NoSqlInjection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.UUID;
@Schema(description = "Request permission to start an internal conversation about a publication")
@NoSqlInjection
public record CreateContactRequest(@NotNull PublicationTypeDto publicationType, @NotNull UUID publicationId,
                                   @NotBlank @Size(max=500) String note) { }
