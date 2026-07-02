package ru.rom8.rescue.volunteer.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.rom8.rescue.volunteer.domain.entity.ContactInfo;
import ru.rom8.rescue.volunteer.dto.ContactInfoDto;

@Mapper(componentModel = "spring")
public interface ContactInfoMapper {

    @Mapping(target = "volunteerId", source = "volunteer.id")
    ContactInfoDto toDto(ContactInfo contactInfo);
}
