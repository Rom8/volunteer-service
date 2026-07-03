package ru.rom8.rescue.volunteer.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import ru.rom8.rescue.volunteer.api.VolunteerApi;
import ru.rom8.rescue.volunteer.api.model.VolunteerDtoApi;
import ru.rom8.rescue.volunteer.api.model.VolunteerRegisterRequestApi;
import ru.rom8.rescue.volunteer.api.model.VolunteerUpdateRequestApi;
import ru.rom8.rescue.volunteer.service.VolunteerRegistrationService;

@RestController
@RequiredArgsConstructor
public class VolunteerController implements VolunteerApi {

    private final VolunteerRegistrationService volunteerRegistrationService;

    @Override
    public VolunteerDtoApi registerMe(VolunteerRegisterRequestApi volunteerRegisterRequestApi) {
        return volunteerRegistrationService.register(volunteerRegisterRequestApi);
    }

    @Override
    public VolunteerDtoApi getMe(String xUserId) {
        return volunteerRegistrationService.getByUserId(xUserId);
    }

    @Override
    public VolunteerDtoApi updateMe(String xUserId, VolunteerUpdateRequestApi volunteerUpdateRequestApi) {
        return volunteerRegistrationService.updateByUserId(xUserId, volunteerUpdateRequestApi);
    }

    @Override
    public void deleteMe(String xUserId) {
        volunteerRegistrationService.deleteByUserId(xUserId);
    }
}
