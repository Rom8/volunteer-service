package ru.rom8.rescue.volunteer.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.rom8.rescue.volunteer.domain.entity.VolunteerStatus;
import ru.rom8.rescue.volunteer.dto.VolunteerDto;
import ru.rom8.rescue.volunteer.service.VolunteerRegistrationService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/volunteer")
@RequiredArgsConstructor
public class AdminVolunteerController {

    private final VolunteerRegistrationService volunteerRegistrationService;

    /**
     * Получение данных волонтёра по идентификатору.
     * @param id идентификатор волонтёра
     * @return данные {@link VolunteerDto}
     */
    @GetMapping("/{id}")
    public VolunteerDto getVolunteer(@PathVariable Long id) {
        return volunteerRegistrationService.getById(id);
    }

    /**
     * Получение списка зарегистрированных волонтёров с фильтрацией.
     * @param settlementName населённый пункт
     * @param settlementDistrictName район населённого пункта
     * @param status статус волонтёра
     * @return список {@link VolunteerDto}
     */
    @GetMapping("/list")
    public List<VolunteerDto> getVolunteerList(
            @RequestParam(required = false) String settlementName,
            @RequestParam(required = false) String settlementDistrictName,
            @RequestParam(required = false) VolunteerStatus status
    ) {
        return volunteerRegistrationService.getList(settlementName, settlementDistrictName, status);
    }
}
