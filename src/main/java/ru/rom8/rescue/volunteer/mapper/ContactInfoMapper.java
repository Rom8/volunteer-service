package ru.rom8.rescue.volunteer.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.rom8.rescue.volunteer.api.model.ContactInfoDtoApi;
import ru.rom8.rescue.volunteer.domain.entity.ContactInfo;

@Mapper(componentModel = "spring")
public interface ContactInfoMapper {

    @Mapping(target = "volunteerId", source = "volunteer.id")
    ContactInfoDtoApi toDto(ContactInfo contactInfo);
}
