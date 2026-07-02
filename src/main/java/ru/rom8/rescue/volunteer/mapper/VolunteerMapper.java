package ru.rom8.rescue.volunteer.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.rom8.rescue.volunteer.domain.entity.Volunteer;
import ru.rom8.rescue.volunteer.dto.VolunteerDto;
import ru.rom8.rescue.volunteer.dto.VolunteerRegisterRequest;

import java.util.List;

@Mapper(componentModel = "spring", uses = ContactInfoMapper.class)
public interface VolunteerMapper {

    @Mapping(target = "locationId", source = "location.id")
    VolunteerDto toDto(Volunteer volunteer);

    List<VolunteerDto> toDtoList(List<Volunteer> volunteers);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createDate", ignore = true)
    @Mapping(target = "updateDate", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "currentIncidentId", ignore = true)
    @Mapping(target = "contacts", ignore = true)
    Volunteer toEntity(VolunteerRegisterRequest request);
}
