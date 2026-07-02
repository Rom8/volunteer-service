package ru.rom8.rescue.volunteer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import ru.rom8.rescue.volunteer.domain.entity.Gender;

import java.time.LocalDate;

public record VolunteerRegisterRequest(
        @NotBlank
        @Size(max = 100)
        String familyName,

        @NotBlank
        @Size(max = 100)
        String firstName,

        @Size(max = 100)
        String patronymic,

        @NotNull
        Gender gender,

        @NotBlank
        @Size(max = 255)
        String phoneNumber,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotNull
        @Past
        LocalDate birthDate,

        @NotBlank
        @Size(max = 255)
        String settlementName,

        @Size(max = 255)
        String settlementDistrictName
) {
}
