package ru.rom8.rescue.volunteer.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import ru.rom8.rescue.volunteer.api.AdminVolunteerApi;
import ru.rom8.rescue.volunteer.api.model.VolunteerDtoApi;
import ru.rom8.rescue.volunteer.api.model.VolunteerStatusApi;
import ru.rom8.rescue.volunteer.service.VolunteerRegistrationService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminVolunteerController implements AdminVolunteerApi {

    private final VolunteerRegistrationService volunteerRegistrationService;

    @Override
    public VolunteerDtoApi getVolunteer(Long id) {
        return volunteerRegistrationService.getById(id);
    }

    @Override
    public List<VolunteerDtoApi> getVolunteerList(
            String settlementName,
            String settlementDistrictName,
            VolunteerStatusApi status
    ) {
        return volunteerRegistrationService.getList(settlementName, settlementDistrictName, status);
    }
}
