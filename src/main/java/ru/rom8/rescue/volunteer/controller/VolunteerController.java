package ru.rom8.rescue.volunteer.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.rom8.rescue.volunteer.dto.VolunteerDto;
import ru.rom8.rescue.volunteer.dto.VolunteerRegisterRequest;
import ru.rom8.rescue.volunteer.dto.VolunteerUpdateRequest;
import ru.rom8.rescue.volunteer.service.VolunteerRegistrationService;

@RestController
@RequestMapping("/api/v1/volunteer")
@RequiredArgsConstructor
public class VolunteerController {

    private static final String USER_ID_HEADER = "X-USER-ID";

    private final VolunteerRegistrationService volunteerRegistrationService;

    /**
     * Регистрация волонтёра
     * @param request {@link VolunteerRegisterRequest}
     * @return {@link VolunteerDto}
     */
    @PostMapping("/register/me")
    @ResponseStatus(HttpStatus.CREATED)
    public VolunteerDto registerMe(@Valid @RequestBody VolunteerRegisterRequest request) {
        return volunteerRegistrationService.register(request);
    }

    /**
     * Просмотр данных о своей регистрации.
     * @param userId идентификатор пользователя из заголовка X-USER-ID
     * @return {@link VolunteerDto}
     */
    @GetMapping("/me")
    public VolunteerDto getMe(@RequestHeader(USER_ID_HEADER) String userId) {
        return volunteerRegistrationService.getByUserId(userId);
    }

    /**
     * Обновление данных о себе.
     * @param userId идентификатор пользователя из заголовка X-USER-ID
     * @param request {@link VolunteerUpdateRequest}
     * @return {@link VolunteerDto}
     */
    @PatchMapping("/me")
    public VolunteerDto updateMe(
            @RequestHeader(USER_ID_HEADER) String userId,
            @Valid @RequestBody VolunteerUpdateRequest request
    ) {
        return volunteerRegistrationService.updateByUserId(userId, request);
    }

    /**
     * Удаление своей учётной записи волонтёра.
     * @param userId идентификатор пользователя из заголовка X-USER-ID
     */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMe(@RequestHeader(USER_ID_HEADER) String userId) {
        volunteerRegistrationService.deleteByUserId(userId);
    }
}
