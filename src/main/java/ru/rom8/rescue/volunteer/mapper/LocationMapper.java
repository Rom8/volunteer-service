package ru.rom8.rescue.volunteer.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.rom8.rescue.volunteer.domain.entity.Location;
import ru.rom8.rescue.volunteer.dto.LocationDto;

@Mapper(componentModel = "spring")
public interface LocationMapper {

    @Mapping(target = "parentId", source = "parent.id")
    LocationDto toDto(Location location);
}
