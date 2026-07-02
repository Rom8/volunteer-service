package ru.rom8.rescue.volunteer.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.rom8.rescue.volunteer.domain.entity.Volunteer;
import ru.rom8.rescue.volunteer.dto.VolunteerDto;

@Mapper(componentModel = "spring", uses = ContactInfoMapper.class)
public interface VolunteerMapper {

    @Mapping(target = "locationId", source = "location.id")
    VolunteerDto toDto(Volunteer volunteer);
}
