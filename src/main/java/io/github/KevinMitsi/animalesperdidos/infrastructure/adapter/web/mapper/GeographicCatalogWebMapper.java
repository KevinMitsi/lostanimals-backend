package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.mapper;

import io.github.KevinMitsi.animalesperdidos.application.port.in.QueryGeographicCatalogUseCase;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.dto.*;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring")
public interface GeographicCatalogWebMapper {
    DepartmentResponse toResponse(QueryGeographicCatalogUseCase.DepartmentView value);
    CityResponse toResponse(QueryGeographicCatalogUseCase.CityView value);
    NeighborhoodResponse toResponse(QueryGeographicCatalogUseCase.NeighborhoodView value);
    List<DepartmentResponse> toDepartmentResponses(List<QueryGeographicCatalogUseCase.DepartmentView> values);
    List<CityResponse> toCityResponses(List<QueryGeographicCatalogUseCase.CityView> values);
    List<NeighborhoodResponse> toNeighborhoodResponses(List<QueryGeographicCatalogUseCase.NeighborhoodView> values);
}
