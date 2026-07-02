package ru.rom8.rescue.volunteer.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.rom8.rescue.volunteer.dto.VolunteerDto;
import ru.rom8.rescue.volunteer.service.VolunteerRegistrationService;

import java.util.List;

@RestController
@RequestMapping(InternalVolunteerController.INTERNAL_VOLUNTEER_API_PATH)
@RequiredArgsConstructor
public class InternalVolunteerController {

    static final String INTERNAL_VOLUNTEER_API_PATH = "/internal/api/v1/volunteer";
    private static final String VOLUNTEER_LIST_PATH = "/list";

    private final VolunteerRegistrationService volunteerRegistrationService;

    /**
     * Получение контактных данных волонтёров по списку идентификаторов.
     * @param ids список идентификаторов волонтёров
     * @return список {@link VolunteerDto} с контактными данными
     */
    @PostMapping(VOLUNTEER_LIST_PATH)
    public List<VolunteerDto> getVolunteerList(@RequestBody List<Long> ids) {
        return volunteerRegistrationService.getByIds(ids);
    }
}
