package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.persistence.mapper;

import io.github.KevinMitsi.animalesperdidos.domain.model.User;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserPersistenceMapper {
    UserEntity toEntity(User user);
    @Mapping(target = "verifyEmail", ignore = true)
    @Mapping(target = "changePassword", ignore = true)
    @Mapping(target = "changeRole", ignore = true)
    @Mapping(target = "profileComplete", ignore = true)
    User toDomain(UserEntity entity);
}
