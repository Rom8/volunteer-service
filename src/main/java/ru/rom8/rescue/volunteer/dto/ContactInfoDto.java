package ru.rom8.rescue.volunteer.dto;

import ru.rom8.rescue.volunteer.domain.entity.ContactType;

import java.time.OffsetDateTime;

public record ContactInfoDto(
        Long id,
        OffsetDateTime createDate,
        OffsetDateTime updateDate,
        String contact,
        ContactType contactType,
        Long volunteerId
) {
}
