package ru.rom8.rescue.volunteer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import ru.rom8.rescue.volunteer.api.model.VolunteerDto;
import ru.rom8.rescue.volunteer.api.model.VolunteerIncidentAction;
import ru.rom8.rescue.volunteer.api.model.VolunteerIncidentActionRequest;
import ru.rom8.rescue.volunteer.domain.entity.Volunteer;
import ru.rom8.rescue.volunteer.domain.entity.VolunteerStatus;
import ru.rom8.rescue.volunteer.mapper.VolunteerMapper;
import ru.rom8.rescue.volunteer.repository.VolunteerRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VolunteerIncidentActionService {

    private static final String USER_ID_HEADER_REQUIRED_MESSAGE = "Header X-USER-ID is required";
    private static final String VOLUNTEER_NOT_FOUND_MESSAGE = "Volunteer registration not found";
    private static final String VOLUNTEER_ALREADY_ASSIGNED_MESSAGE = "Volunteer is already assigned to another incident";

    private final VolunteerRepository volunteerRepository;
    private final VolunteerMapper volunteerMapper;

    @Transactional
    public VolunteerDto actOnIncident(String userId, VolunteerIncidentActionRequest request) {
        Volunteer volunteer = getVolunteerByUserIdForUpdate(userId);
        UUID incidentId = request.getIncidentId();

        if (request.getAction() == VolunteerIncidentAction.ACCEPT) {
            acceptIncident(volunteer, incidentId);
        } else if (request.getAction() == VolunteerIncidentAction.REJECT) {
            rejectIncident(volunteer, incidentId);
        }

        return volunteerMapper.toDto(volunteerRepository.save(volunteer));
    }

    private Volunteer getVolunteerByUserIdForUpdate(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, USER_ID_HEADER_REQUIRED_MESSAGE);
        }

        return volunteerRepository.findByUserIdForUpdate(userId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, VOLUNTEER_NOT_FOUND_MESSAGE));
    }

    private void acceptIncident(Volunteer volunteer, UUID incidentId) {
        if (volunteer.getStatus() == VolunteerStatus.ASSIGNED_TASK && !incidentId.equals(volunteer.getCurrentIncidentId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, VOLUNTEER_ALREADY_ASSIGNED_MESSAGE);
        }

        volunteer.setStatus(VolunteerStatus.ASSIGNED_TASK);
        volunteer.setCurrentIncidentId(incidentId);
    }

    private void rejectIncident(Volunteer volunteer, UUID incidentId) {
        if (volunteer.getStatus() == VolunteerStatus.ASSIGNED_TASK && incidentId.equals(volunteer.getCurrentIncidentId())) {
            volunteer.setStatus(VolunteerStatus.FREE);
            volunteer.setCurrentIncidentId(null);
        }
    }
}
