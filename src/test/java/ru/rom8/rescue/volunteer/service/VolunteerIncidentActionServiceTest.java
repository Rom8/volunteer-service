package ru.rom8.rescue.volunteer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.server.ResponseStatusException;
import ru.rom8.rescue.volunteer.api.model.VolunteerDto;
import ru.rom8.rescue.volunteer.api.model.VolunteerIncidentAction;
import ru.rom8.rescue.volunteer.api.model.VolunteerIncidentActionRequest;
import ru.rom8.rescue.volunteer.domain.entity.Volunteer;
import ru.rom8.rescue.volunteer.mapper.VolunteerMapper;
import ru.rom8.rescue.volunteer.repository.VolunteerRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VolunteerIncidentActionServiceTest {

    private static final Long VOLUNTEER_ID = 42L;
    private static final String USER_ID = "volunteer-user-id";
    private static final UUID INCIDENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_INCIDENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String VOLUNTEER_INCIDENT_ASSIGN_TOPIC = "volunteer_incident_assign_event_v1";
    private static final String USER_ID_REQUIRED_MESSAGE = "Header X-USER-ID is required.";
    private static final String VOLUNTEER_NOT_FOUND_MESSAGE = "Волонтёр не найден.";
    private static final String ALREADY_ASSIGNED_MESSAGE = "Волонтер может принимать участие только в одном инциденте.";

    @Mock
    private VolunteerRepository volunteerRepository;

    @Mock
    private VolunteerMapper volunteerMapper;

    @Mock
    private KafkaTemplate<String, VolunteerIncidentAssignEvent> kafkaTemplate;

    private VolunteerIncidentActionService volunteerIncidentActionService;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");

        volunteerIncidentActionService = new VolunteerIncidentActionService(
                volunteerRepository,
                volunteerMapper,
                messageSource,
                kafkaTemplate
        );
    }

    @Test
    @DisplayName("Свободный волонтёр принимает инцидент через сервис")
    void shouldAcceptIncidentForFreeVolunteer() {
        Volunteer volunteer = freeVolunteer();
        VolunteerDto expectedDto = volunteerDto(
                ru.rom8.rescue.volunteer.api.model.VolunteerStatus.ASSIGNED_TASK,
                INCIDENT_ID
        );
        when(volunteerRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(volunteer));
        when(volunteerRepository.save(volunteer)).thenReturn(volunteer);
        when(volunteerMapper.toDto(volunteer)).thenReturn(expectedDto);

        VolunteerDto result = volunteerIncidentActionService.actOnIncident(
                USER_ID,
                new VolunteerIncidentActionRequest(INCIDENT_ID, VolunteerIncidentAction.ACCEPT)
        );

        assertThat(volunteer.getStatus()).isEqualTo(ru.rom8.rescue.volunteer.domain.entity.VolunteerStatus.ASSIGNED_TASK);
        assertThat(volunteer.getCurrentIncidentId()).isEqualTo(INCIDENT_ID);
        assertThat(result.getId()).isEqualTo(VOLUNTEER_ID);
        assertThat(result.getStatus()).isEqualTo(ru.rom8.rescue.volunteer.api.model.VolunteerStatus.ASSIGNED_TASK);
        assertThat(result.getCurrentIncidentId()).isEqualTo(INCIDENT_ID);
        verify(kafkaTemplate).send(
                VOLUNTEER_INCIDENT_ASSIGN_TOPIC,
                INCIDENT_ID.toString(),
                new VolunteerIncidentAssignEvent(INCIDENT_ID.toString(), VOLUNTEER_ID, "ACCEPT")
        );
        verify(volunteerRepository).save(volunteer);
    }

    @Test
    @DisplayName("Назначенный волонтёр не может принять другой инцидент")
    void shouldReturnConflictWhenAssignedVolunteerAcceptsAnotherIncident() {
        Volunteer volunteer = assignedVolunteer();
        when(volunteerRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(volunteer));

        assertThatThrownBy(() -> volunteerIncidentActionService.actOnIncident(
                USER_ID,
                new VolunteerIncidentActionRequest(OTHER_INCIDENT_ID, VolunteerIncidentAction.ACCEPT)
        ))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason()).isEqualTo(ALREADY_ASSIGNED_MESSAGE);
                });

        assertThat(volunteer.getStatus()).isEqualTo(ru.rom8.rescue.volunteer.domain.entity.VolunteerStatus.ASSIGNED_TASK);
        assertThat(volunteer.getCurrentIncidentId()).isEqualTo(INCIDENT_ID);
        verify(volunteerRepository).findByUserIdForUpdate(USER_ID);
        verify(volunteerRepository, never()).save(any());
        verifyNoInteractions(volunteerMapper, kafkaTemplate);
    }

    @Test
    @DisplayName("Свободный волонтёр отклоняет инцидент, на который не был назначен")
    void shouldKeepFreeVolunteerWhenRejectingUnassignedIncident() {
        Volunteer volunteer = freeVolunteer();
        VolunteerDto expectedDto = volunteerDto(
                ru.rom8.rescue.volunteer.api.model.VolunteerStatus.FREE,
                null
        );
        when(volunteerRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(volunteer));
        when(volunteerRepository.save(volunteer)).thenReturn(volunteer);
        when(volunteerMapper.toDto(volunteer)).thenReturn(expectedDto);

        VolunteerDto result = volunteerIncidentActionService.actOnIncident(
                USER_ID,
                new VolunteerIncidentActionRequest(INCIDENT_ID, VolunteerIncidentAction.REJECT)
        );

        assertThat(volunteer.getStatus()).isEqualTo(ru.rom8.rescue.volunteer.domain.entity.VolunteerStatus.FREE);
        assertThat(volunteer.getCurrentIncidentId()).isNull();
        assertThat(result.getId()).isEqualTo(VOLUNTEER_ID);
        assertThat(result.getStatus()).isEqualTo(ru.rom8.rescue.volunteer.api.model.VolunteerStatus.FREE);
        assertThat(result.getCurrentIncidentId()).isNull();
        verifyNoInteractions(kafkaTemplate);
        verify(volunteerRepository).save(volunteer);
    }

    @Test
    @DisplayName("Запрос без идентификатора пользователя отклоняется как некорректный")
    void shouldReturnBadRequestWhenUserIdIsBlank() {
        assertThatThrownBy(() -> volunteerIncidentActionService.actOnIncident(
                " ",
                new VolunteerIncidentActionRequest(INCIDENT_ID, VolunteerIncidentAction.ACCEPT)
        ))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getReason()).isEqualTo(USER_ID_REQUIRED_MESSAGE);
                });

        verifyNoInteractions(volunteerRepository, volunteerMapper, kafkaTemplate);
    }

    @Test
    @DisplayName("Запрос от неизвестного пользователя отклоняется как ненайденный волонтёр")
    void shouldReturnNotFoundWhenVolunteerDoesNotExist() {
        when(volunteerRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> volunteerIncidentActionService.actOnIncident(
                USER_ID,
                new VolunteerIncidentActionRequest(INCIDENT_ID, VolunteerIncidentAction.ACCEPT)
        ))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getReason()).isEqualTo(VOLUNTEER_NOT_FOUND_MESSAGE);
                });

        verify(volunteerRepository).findByUserIdForUpdate(USER_ID);
        verify(volunteerRepository, never()).save(any());
        verifyNoInteractions(volunteerMapper, kafkaTemplate);
    }

    private Volunteer freeVolunteer() {
        Volunteer volunteer = new Volunteer();
        volunteer.setId(VOLUNTEER_ID);
        volunteer.setUserId(USER_ID);
        volunteer.setStatus(ru.rom8.rescue.volunteer.domain.entity.VolunteerStatus.FREE);
        volunteer.setCurrentIncidentId(null);
        return volunteer;
    }

    private Volunteer assignedVolunteer() {
        Volunteer volunteer = new Volunteer();
        volunteer.setId(VOLUNTEER_ID);
        volunteer.setUserId(USER_ID);
        volunteer.setStatus(ru.rom8.rescue.volunteer.domain.entity.VolunteerStatus.ASSIGNED_TASK);
        volunteer.setCurrentIncidentId(INCIDENT_ID);
        return volunteer;
    }

    private VolunteerDto volunteerDto(
            ru.rom8.rescue.volunteer.api.model.VolunteerStatus status,
            UUID currentIncidentId
    ) {
        return new VolunteerDto()
                .id(VOLUNTEER_ID)
                .userId(USER_ID)
                .status(status)
                .currentIncidentId(currentIncidentId);
    }
}
