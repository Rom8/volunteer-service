package ru.rom8.rescue.volunteer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record VolunteerUpdateRequest(
        @Size(max = 100)
        String familyName,

        @Size(max = 100)
        String firstName,

        @Size(max = 100)
        String patronymic,

        @Size(max = 255)
        String phoneNumber,

        @Email
        @Size(max = 255)
        String email,

        @Size(max = 255)
        String settlementName,

        @Size(max = 255)
        String settlementDistrictName
) {
}
