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
import ru.rom8.rescue.volunteer.domain.entity.VolunteerStatus;
import ru.rom8.rescue.volunteer.dto.VolunteerDto;
import ru.rom8.rescue.volunteer.dto.VolunteerRegisterRequest;
import ru.rom8.rescue.volunteer.dto.VolunteerUpdateRequest;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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
    private static final String REGISTER_REQUEST_FIXTURE = "/volunteer/register-request.json";
    private static final String UPDATE_REQUEST_FIXTURE = "/volunteer/update-request.json";

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
        VolunteerRegisterRequest request = readRegisterRequestFixture();

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
        VolunteerUpdateRequest request = readUpdateRequestFixture();

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

    private HttpRequest.BodyPublisher jsonBody(Object body) {
        return HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body));
    }

    private VolunteerDto readVolunteer(HttpResponse<String> response) {
        assertThat(response.body()).isNotBlank();
        return objectMapper.readValue(response.body(), VolunteerDto.class);
    }

    private VolunteerRegisterRequest readRegisterRequestFixture() throws Exception {
        return readJsonFixture(REGISTER_REQUEST_FIXTURE, VolunteerRegisterRequest.class);
    }

    private VolunteerUpdateRequest readUpdateRequestFixture() throws Exception {
        return readJsonFixture(UPDATE_REQUEST_FIXTURE, VolunteerUpdateRequest.class);
    }

    private <T> T readJsonFixture(String resourcePath, Class<T> type) throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            assertThat(inputStream)
                    .as("JSON fixture %s must exist", resourcePath)
                    .isNotNull();
            return objectMapper.readValue(inputStream, type);
        }
    }

    private void assertVolunteerMatchesRegistration(VolunteerDto volunteer) throws Exception {
        VolunteerRegisterRequest expected = readRegisterRequestFixture();

        assertThat(volunteer.firstName()).isEqualTo(expected.firstName());
        assertThat(volunteer.familyName()).isEqualTo(expected.familyName());
        assertThat(volunteer.patronymic()).isEqualTo(expected.patronymic());
        assertThat(volunteer.gender()).isEqualTo(expected.gender());
        assertThat(volunteer.birthDate()).isEqualTo(expected.birthDate());
        assertThat(volunteer.status()).isEqualTo(VolunteerStatus.FREE);
        assertThat(volunteer.locationId()).isNotNull();
        assertThat(volunteer.currentIncidentId()).isNull();
        assertThat(volunteer.contacts())
                .extracting(contact -> contact.contactType() + ":" + contact.contact())
                .containsExactlyInAnyOrder(
                        ContactType.PHONE + ":" + expected.phoneNumber(),
                        ContactType.EMAIL + ":" + expected.email()
                );
    }

    private void assertVolunteerMatchesUpdate(VolunteerDto volunteer) throws Exception {
        VolunteerRegisterRequest registration = readRegisterRequestFixture();
        VolunteerUpdateRequest expected = readUpdateRequestFixture();

        assertThat(volunteer.firstName()).isEqualTo(expected.firstName());
        assertThat(volunteer.familyName()).isEqualTo(expected.familyName());
        assertThat(volunteer.patronymic()).isEqualTo(expected.patronymic());
        assertThat(volunteer.gender()).isEqualTo(registration.gender());
        assertThat(volunteer.birthDate()).isEqualTo(registration.birthDate());
        assertThat(volunteer.status()).isEqualTo(VolunteerStatus.FREE);
        assertThat(volunteer.locationId()).isNotNull();
        assertThat(volunteer.currentIncidentId()).isNull();
        assertThat(volunteer.contacts())
                .extracting(contact -> contact.contactType() + ":" + contact.contact())
                .containsExactlyInAnyOrder(
                        ContactType.PHONE + ":" + expected.phoneNumber(),
                        ContactType.EMAIL + ":" + expected.email()
                );
    }
}
