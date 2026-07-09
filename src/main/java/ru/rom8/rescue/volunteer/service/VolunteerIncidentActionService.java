package ru.rom8.rescue.volunteer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
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
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VolunteerIncidentActionService {

    private static final String VOLUNTEER_INCIDENT_ASSIGN_TOPIC = "volunteer_incident_assign_event_v1";
    private static final String KAFKA_MESSAGE_SERIALIZATION_ERROR = "Failed to serialize volunteer incident assign event";

    private final VolunteerRepository volunteerRepository;
    private final VolunteerMapper volunteerMapper;
    private final MessageSource messageSource;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public VolunteerDto actOnIncident(String userId, VolunteerIncidentActionRequest request) {
        Volunteer volunteer = getVolunteerByUserIdForUpdate(userId);
        UUID incidentId = request.getIncidentId();
        log.atInfo()
                .addKeyValue("event", "VolunteerIncidentActionRequested")
                .addKeyValue("volunteer_id", volunteer.getId())
                .addKeyValue("user_id", volunteer.getUserId())
                .addKeyValue("incident_id", incidentId)
                .addKeyValue("action", request.getAction())
                .log("Volunteer incident action requested");

        if (request.getAction() == VolunteerIncidentAction.ACCEPT) {
            acceptIncident(volunteer, incidentId);
        } else if (request.getAction() == VolunteerIncidentAction.REJECT) {
            rejectIncident(volunteer, incidentId);
        }

        return volunteerMapper.toDto(volunteerRepository.save(volunteer));
    }

    private Volunteer getVolunteerByUserIdForUpdate(String userId) {
        if (!StringUtils.hasText(userId)) {
            log.atWarn()
                    .addKeyValue("event", "VolunteerUserIdMissing")
                    .log("Volunteer user id is missing");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, getMessage("volunteer.error.user-id-header-required"));
        }

        String normalizedUserId = userId.trim();
        return volunteerRepository.findByUserIdForUpdate(normalizedUserId)
                .orElseThrow(() -> {
                    log.atWarn()
                            .addKeyValue("event", "VolunteerNotFound")
                            .addKeyValue("user_id", normalizedUserId)
                            .log("Volunteer not found");
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, getMessage("volunteer.error.not-found"));
                });
    }

    private void acceptIncident(Volunteer volunteer, UUID incidentId) {
        if (volunteer.getStatus() == VolunteerStatus.ASSIGNED_TASK && !incidentId.equals(volunteer.getCurrentIncidentId())) {
            log.atWarn()
                    .addKeyValue("event", "VolunteerAlreadyAssigned")
                    .addKeyValue("volunteer_id", volunteer.getId())
                    .addKeyValue("user_id", volunteer.getUserId())
                    .addKeyValue("incident_id", incidentId)
                    .addKeyValue("current_incident_id", volunteer.getCurrentIncidentId())
                    .log("Volunteer already assigned to another incident");
            throw new ResponseStatusException(HttpStatus.CONFLICT, getMessage("volunteer.error.already-assigned"));
        }

        volunteer.setStatus(VolunteerStatus.ASSIGNED_TASK);
        volunteer.setCurrentIncidentId(incidentId);
        log.atInfo()
                .addKeyValue("event", "VolunteerIncidentAccepted")
                .addKeyValue("volunteer_id", volunteer.getId())
                .addKeyValue("user_id", volunteer.getUserId())
                .addKeyValue("incident_id", incidentId)
                .addKeyValue("status", volunteer.getStatus())
                .log("Volunteer incident accepted");
        sendIncidentEvent(volunteer, incidentId, "ACCEPT");
    }

    private void rejectIncident(Volunteer volunteer, UUID incidentId) {
        if (volunteer.getStatus() == VolunteerStatus.ASSIGNED_TASK && incidentId.equals(volunteer.getCurrentIncidentId())) {
            volunteer.setStatus(VolunteerStatus.FREE);
            volunteer.setCurrentIncidentId(null);
            log.atInfo()
                    .addKeyValue("event", "VolunteerIncidentRejected")
                    .addKeyValue("volunteer_id", volunteer.getId())
                    .addKeyValue("user_id", volunteer.getUserId())
                    .addKeyValue("incident_id", incidentId)
                    .addKeyValue("status", volunteer.getStatus())
                    .log("Volunteer incident rejected");
            sendIncidentEvent(volunteer, incidentId, "REJECT");
        }
    }

    //todo: make transactional outbox
    private void sendIncidentEvent(Volunteer volunteer, UUID incidentId, String status) {
        VolunteerIncidentAssignEvent event = new VolunteerIncidentAssignEvent(incidentId, volunteer.getId(), status);

        try {
            kafkaTemplate.send(VOLUNTEER_INCIDENT_ASSIGN_TOPIC, incidentId.toString(), objectMapper.writeValueAsString(event));
            log.atInfo()
                    .addKeyValue("event", "VolunteerIncidentAssignEventSendInitiated")
                    .addKeyValue("topic", VOLUNTEER_INCIDENT_ASSIGN_TOPIC)
                    .addKeyValue("volunteer_id", volunteer.getId())
                    .addKeyValue("incident_id", incidentId)
                    .addKeyValue("status", status)
                    .log("Volunteer incident assign event send initiated");
        } catch (JacksonException exception) {
            log.atError()
                    .addKeyValue("event", "VolunteerIncidentAssignEventSerializationFailed")
                    .addKeyValue("topic", VOLUNTEER_INCIDENT_ASSIGN_TOPIC)
                    .addKeyValue("volunteer_id", volunteer.getId())
                    .addKeyValue("incident_id", incidentId)
                    .addKeyValue("status", status)
                    .setCause(exception)
                    .log("Failed to serialize volunteer incident assign event");
            throw new IllegalStateException(KAFKA_MESSAGE_SERIALIZATION_ERROR, exception);
        }
    }

    private String getMessage(String messageKey) {
        return messageSource.getMessage(messageKey, null, LocaleContextHolder.getLocale());
    }
}
