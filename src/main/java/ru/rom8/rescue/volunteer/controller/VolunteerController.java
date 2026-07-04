package ru.rom8.rescue.volunteer.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import ru.rom8.rescue.volunteer.api.VolunteerApi;
import ru.rom8.rescue.volunteer.api.model.VolunteerDto;
import ru.rom8.rescue.volunteer.api.model.VolunteerIncidentActionRequest;
import ru.rom8.rescue.volunteer.api.model.VolunteerRegisterRequest;
import ru.rom8.rescue.volunteer.api.model.VolunteerUpdateRequest;
import ru.rom8.rescue.volunteer.service.VolunteerIncidentActionService;
import ru.rom8.rescue.volunteer.service.VolunteerRegistrationService;

@RestController
@RequiredArgsConstructor
public class VolunteerController implements VolunteerApi {

    private final VolunteerRegistrationService volunteerRegistrationService;
    private final VolunteerIncidentActionService volunteerIncidentActionService;

    @Override
    public VolunteerDto actOnIncident(String xUserId, VolunteerIncidentActionRequest volunteerIncidentActionRequest) {
        return volunteerIncidentActionService.actOnIncident(xUserId, volunteerIncidentActionRequest);
    }

    @Override
    public VolunteerDto registerMe(VolunteerRegisterRequest volunteerRegisterRequest) {
        return volunteerRegistrationService.register(volunteerRegisterRequest);
    }

    @Override
    public VolunteerDto getMe(String xUserId) {
        return volunteerRegistrationService.getByUserId(xUserId);
    }

    @Override
    public VolunteerDto updateMe(String xUserId, VolunteerUpdateRequest volunteerUpdateRequest) {
        return volunteerRegistrationService.updateByUserId(xUserId, volunteerUpdateRequest);
    }

    @Override
    public void deleteMe(String xUserId) {
        volunteerRegistrationService.deleteByUserId(xUserId);
    }
}
