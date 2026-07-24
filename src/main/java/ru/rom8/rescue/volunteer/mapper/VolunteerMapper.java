package ru.rom8.rescue.volunteer.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.rom8.rescue.volunteer.api.model.VolunteerDto;
import ru.rom8.rescue.volunteer.api.model.VolunteerRegisterRequest;
import ru.rom8.rescue.volunteer.domain.entity.Location;
import ru.rom8.rescue.volunteer.domain.entity.Volunteer;

import java.util.List;

@Mapper(componentModel = "spring", uses = ContactInfoMapper.class)
public interface VolunteerMapper {

    @Mapping(target = "settlementName", ignore = true)
    @Mapping(target = "settlementDistrictName", ignore = true)
    @Mapping(target = "locationId", source = "location.id")
    VolunteerDto toDto(Volunteer volunteer);

    @AfterMapping
    default void mapSettlementNames(Volunteer volunteer, @MappingTarget VolunteerDto volunteerDto) {
        Location location = volunteer.getLocation();
        if (location.getParent() == null) {
            volunteerDto.setSettlementName(location.getName());
        } else {
            volunteerDto.setSettlementDistrictName(location.getName());
            volunteerDto.setSettlementName(location.getParent().getName());
        }
    }

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
