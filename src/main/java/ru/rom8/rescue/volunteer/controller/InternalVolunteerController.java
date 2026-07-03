package ru.rom8.rescue.volunteer.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import ru.rom8.rescue.volunteer.api.InternalVolunteerApi;
import ru.rom8.rescue.volunteer.api.model.VolunteerDto;
import ru.rom8.rescue.volunteer.service.VolunteerRegistrationService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class InternalVolunteerController implements InternalVolunteerApi {

    private final VolunteerRegistrationService volunteerRegistrationService;

    @Override
    public List<VolunteerDto> getInternalVolunteerList(List<Long> ids) {
        return volunteerRegistrationService.getByIds(ids);
    }
}
