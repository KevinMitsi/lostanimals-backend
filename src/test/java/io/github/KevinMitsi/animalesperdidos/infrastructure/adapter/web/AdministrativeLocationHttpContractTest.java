package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web;

import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.CreateLostPetReportRequest;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.SpeciesDto;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.mapper.LostPetReportWebMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AdministrativeLocationHttpContractTest {
    @Test
    void jakartaValidationRejectsMalformedCodesAndOversizedNeighborhood() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var request = request("6A", "6301", "x".repeat(121));
            var fields = factory.getValidator().validate(request).stream()
                    .map(error -> error.getPropertyPath().toString()).toList();
            assertTrue(fields.contains("departmentCode"));
            assertTrue(fields.contains("municipalityCode"));
            assertTrue(fields.contains("neighborhood"));
        }
    }

    @Test
    void mapperEnforcesDepartmentMunicipalityRelationshipInDomain() {
        var mapper = Mappers.getMapper(LostPetReportWebMapper.class);
        var error = assertThrows(IllegalArgumentException.class,
                () -> mapper.toCommand(request("63", "05001", "Granada"), UUID.randomUUID()));
        assertTrue(error.getMessage().contains("does not belong"));
    }

    private static CreateLostPetReportRequest request(String department, String municipality, String neighborhood) {
        return new CreateLostPetReportRequest("Luna", SpeciesDto.DOG, "Collar rojo",
                Instant.parse("2026-08-01T12:00:00Z"), 4.53, -75.68,
                department, municipality, neighborhood, List.of("staging/key"));
    }
}
