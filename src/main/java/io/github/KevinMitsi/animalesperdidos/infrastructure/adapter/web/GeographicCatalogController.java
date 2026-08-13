package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web;

import io.github.KevinMitsi.animalesperdidos.application.port.in.QueryGeographicCatalogUseCase;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.*;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.mapper.GeographicCatalogWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.*;

@RestController
@RequestMapping("/api/v1/geography")
@RequiredArgsConstructor
@Validated
@Tag(name = "Geography", description = "Public geographic catalog used by search filters")
public class GeographicCatalogController {
    private final QueryGeographicCatalogUseCase queries;
    private final GeographicCatalogWebMapper mapper;

    @GetMapping("/departments")
    @Operation(summary = "List enabled departments")
    public Mono<List<DepartmentResponse>> departments() {
        return Mono.fromCompletionStage(queries.departments()).map(mapper::toDepartmentResponses);
    }

    @GetMapping("/cities")
    @Operation(summary = "List cities belonging to a department")
    public Mono<List<CityResponse>> cities(@RequestParam @NotNull UUID departmentId) {
        return Mono.fromCompletionStage(queries.cities(departmentId)).map(mapper::toCityResponses);
    }

    @GetMapping("/neighborhoods")
    @Operation(summary = "List neighborhoods belonging to a city")
    public Mono<List<NeighborhoodResponse>> neighborhoods(@RequestParam @NotNull UUID cityId) {
        return Mono.fromCompletionStage(queries.neighborhoods(cityId)).map(mapper::toNeighborhoodResponses);
    }
}
