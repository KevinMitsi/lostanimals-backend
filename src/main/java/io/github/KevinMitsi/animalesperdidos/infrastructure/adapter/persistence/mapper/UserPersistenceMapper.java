package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.persistence.mapper;

import io.github.KevinMitsi.animalesperdidos.domain.model.User;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.persistence.entity.UserEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserPersistenceMapper {
    UserEntity toEntity(User user);
    User toDomain(UserEntity entity);
}
