package ru.rom8.rescue.volunteer.dto;

import ru.rom8.rescue.volunteer.domain.entity.Gender;
import ru.rom8.rescue.volunteer.domain.entity.VolunteerStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record VolunteerDto(
        Long id,
        OffsetDateTime createDate,
        OffsetDateTime updateDate,
        String userId,
        String firstName,
        String familyName,
        String patronymic,
        Gender gender,
        LocalDate birthDate,
        VolunteerStatus status,
        Long locationId,
        UUID currentIncidentId,
        Set<ContactInfoDto> contacts
) {
}
