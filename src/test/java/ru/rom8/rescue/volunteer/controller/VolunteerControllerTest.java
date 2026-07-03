package ru.rom8.rescue.volunteer.controller;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.rom8.rescue.volunteer.domain.entity.ContactType;
import ru.rom8.rescue.volunteer.domain.entity.Gender;
import ru.rom8.rescue.volunteer.domain.entity.VolunteerStatus;
import ru.rom8.rescue.volunteer.dto.VolunteerDto;
import ru.rom8.rescue.volunteer.dto.VolunteerRegisterRequest;
import ru.rom8.rescue.volunteer.dto.VolunteerUpdateRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VolunteerControllerTest {

    private static final String POSTGRES_IMAGE = "postgres:17.10";
    private static final String BASE_URL = "/api/v1/volunteer";
    private static final String REGISTER_ME_URL = BASE_URL + "/register/me";
    private static final String ME_URL = BASE_URL + "/me";
    private static final String USER_ID_HEADER = "X-USER-ID";

    private static final String INITIAL_FAMILY_NAME = "Иванов";
    private static final String INITIAL_FIRST_NAME = "Иван";
    private static final String INITIAL_PATRONYMIC = "Иванович";
    private static final String INITIAL_PHONE_NUMBER = "+79990000001";
    private static final String INITIAL_EMAIL = "ivan.volunteer@example.org";
    private static final LocalDate INITIAL_BIRTH_DATE = LocalDate.of(1990, 1, 15);
    private static final String INITIAL_SETTLEMENT_NAME = "Москва";
    private static final String INITIAL_DISTRICT_NAME = "Центральный";

    private static final String UPDATED_FAMILY_NAME = "Петров";
    private static final String UPDATED_FIRST_NAME = "Пётр";
    private static final String UPDATED_PATRONYMIC = "Петрович";
    private static final String UPDATED_PHONE_NUMBER = "+79990000002";
    private static final String UPDATED_EMAIL = "petr.volunteer@example.org";
    private static final String UPDATED_SETTLEMENT_NAME = "Санкт-Петербург";
    private static final String UPDATED_DISTRICT_NAME = "Адмиралтейский";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_IMAGE);

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    private Long registeredVolunteerId;
    private String registeredUserId;

    @Test
    @Order(1)
    @DisplayName("1. Регистрация волонтёра")
    void shouldRegisterVolunteerViaRestEndpoint() throws Exception {
        VolunteerDto registeredVolunteer = registerVolunteer();

        registeredVolunteerId = registeredVolunteer.id();
        registeredUserId = registeredVolunteer.userId();

        assertThat(registeredUserId).startsWith("volunteer-");
        assertVolunteerMatchesRegistration(registeredVolunteer);
    }

    @Test
    @Order(2)
    @DisplayName("2. Просмотр данных о своей регистрации")
    void shouldViewOwnRegistrationViaRestEndpoint() throws Exception {
        assertThat(registeredVolunteerId).isNotNull();
        assertThat(registeredUserId).isNotBlank();

        VolunteerDto volunteerBeforeUpdate = getVolunteer(registeredUserId, HttpStatus.OK);

        assertThat(volunteerBeforeUpdate.id()).isEqualTo(registeredVolunteerId);
        assertVolunteerMatchesRegistration(volunteerBeforeUpdate);
    }

    @Test
    @Order(3)
    @DisplayName("3. Обновление данных о себе")
    void shouldUpdateOwnRegistrationViaRestEndpoint() throws Exception {
        assertThat(registeredVolunteerId).isNotNull();
        assertThat(registeredUserId).isNotBlank();

        VolunteerDto updatedVolunteer = updateVolunteer(registeredUserId);

        assertThat(updatedVolunteer.id()).isEqualTo(registeredVolunteerId);
        assertVolunteerMatchesUpdate(updatedVolunteer);
    }

    @Test
    @Order(4)
    @DisplayName("4. Просмотр обновлённых данных о своей регистрации")
    void shouldViewUpdatedOwnRegistrationViaRestEndpoint() throws Exception {
        assertThat(registeredVolunteerId).isNotNull();
        assertThat(registeredUserId).isNotBlank();

        VolunteerDto volunteerAfterUpdate = getVolunteer(registeredUserId, HttpStatus.OK);

        assertThat(volunteerAfterUpdate.id()).isEqualTo(registeredVolunteerId);
        assertVolunteerMatchesUpdate(volunteerAfterUpdate);
    }

    @Test
    @Order(5)
    @DisplayName("5. Удаление своей учётной записи волонтёра")
    void shouldDeleteOwnRegistrationViaRestEndpoint() throws Exception {
        assertThat(registeredUserId).isNotBlank();

        HttpResponse<String> deleteResponse = httpClient.send(
                requestBuilder(ME_URL)
                        .header(USER_ID_HEADER, registeredUserId)
                        .DELETE()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(deleteResponse.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());

        getVolunteer(registeredUserId, HttpStatus.NOT_FOUND);
    }

    private VolunteerDto registerVolunteer() throws Exception {
        VolunteerRegisterRequest request = new VolunteerRegisterRequest(
                INITIAL_FAMILY_NAME,
                INITIAL_FIRST_NAME,
                INITIAL_PATRONYMIC,
                Gender.MALE,
                INITIAL_PHONE_NUMBER,
                INITIAL_EMAIL,
                INITIAL_BIRTH_DATE,
                INITIAL_SETTLEMENT_NAME,
                INITIAL_DISTRICT_NAME
        );

        HttpResponse<String> response = httpClient.send(
                requestBuilder(REGISTER_ME_URL)
                        .POST(jsonBody(request))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        return readVolunteer(response);
    }

    private VolunteerDto getVolunteer(String userId, HttpStatus expectedStatus) throws Exception {
        HttpResponse<String> response = httpClient.send(
                requestBuilder(ME_URL)
                        .header(USER_ID_HEADER, userId)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode()).isEqualTo(expectedStatus.value());
        if (expectedStatus.isError()) {
            return null;
        }
        return readVolunteer(response);
    }

    private VolunteerDto updateVolunteer(String userId) throws Exception {
        VolunteerUpdateRequest request = new VolunteerUpdateRequest(
                UPDATED_FAMILY_NAME,
                UPDATED_FIRST_NAME,
                UPDATED_PATRONYMIC,
                UPDATED_PHONE_NUMBER,
                UPDATED_EMAIL,
                UPDATED_SETTLEMENT_NAME,
                UPDATED_DISTRICT_NAME
        );

        HttpResponse<String> response = httpClient.send(
                requestBuilder(ME_URL)
                        .header(USER_ID_HEADER, userId)
                        .method("PATCH", jsonBody(request))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        return readVolunteer(response);
    }

    private HttpRequest.Builder requestBuilder(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json");
    }

    private HttpRequest.BodyPublisher jsonBody(Object body) throws Exception {
        return HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body));
    }

    private VolunteerDto readVolunteer(HttpResponse<String> response) throws Exception {
        assertThat(response.body()).isNotBlank();
        return objectMapper.readValue(response.body(), VolunteerDto.class);
    }

    private void assertVolunteerMatchesRegistration(VolunteerDto volunteer) {
        assertThat(volunteer.firstName()).isEqualTo(INITIAL_FIRST_NAME);
        assertThat(volunteer.familyName()).isEqualTo(INITIAL_FAMILY_NAME);
        assertThat(volunteer.patronymic()).isEqualTo(INITIAL_PATRONYMIC);
        assertThat(volunteer.gender()).isEqualTo(Gender.MALE);
        assertThat(volunteer.birthDate()).isEqualTo(INITIAL_BIRTH_DATE);
        assertThat(volunteer.status()).isEqualTo(VolunteerStatus.FREE);
        assertThat(volunteer.locationId()).isNotNull();
        assertThat(volunteer.currentIncidentId()).isNull();
        assertThat(volunteer.contacts())
                .extracting(contact -> contact.contactType() + ":" + contact.contact())
                .containsExactlyInAnyOrder(
                        ContactType.PHONE + ":" + INITIAL_PHONE_NUMBER,
                        ContactType.EMAIL + ":" + INITIAL_EMAIL
                );
    }

    private void assertVolunteerMatchesUpdate(VolunteerDto volunteer) {
        assertThat(volunteer.firstName()).isEqualTo(UPDATED_FIRST_NAME);
        assertThat(volunteer.familyName()).isEqualTo(UPDATED_FAMILY_NAME);
        assertThat(volunteer.patronymic()).isEqualTo(UPDATED_PATRONYMIC);
        assertThat(volunteer.gender()).isEqualTo(Gender.MALE);
        assertThat(volunteer.birthDate()).isEqualTo(INITIAL_BIRTH_DATE);
        assertThat(volunteer.status()).isEqualTo(VolunteerStatus.FREE);
        assertThat(volunteer.locationId()).isNotNull();
        assertThat(volunteer.currentIncidentId()).isNull();
        assertThat(volunteer.contacts())
                .extracting(contact -> contact.contactType() + ":" + contact.contact())
                .containsExactlyInAnyOrder(
                        ContactType.PHONE + ":" + UPDATED_PHONE_NUMBER,
                        ContactType.EMAIL + ":" + UPDATED_EMAIL
                );
    }
}
