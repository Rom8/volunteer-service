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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import ru.rom8.rescue.volunteer.api.model.ContactTypeApi;
import ru.rom8.rescue.volunteer.api.model.GenderApi;
import ru.rom8.rescue.volunteer.api.model.VolunteerDtoApi;
import ru.rom8.rescue.volunteer.api.model.VolunteerRegisterRequestApi;
import ru.rom8.rescue.volunteer.api.model.VolunteerStatusApi;
import ru.rom8.rescue.volunteer.api.model.VolunteerUpdateRequestApi;

import java.io.InputStream;
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
    private static final String REGISTER_REQUEST_FIXTURE = "/volunteer/register-request.json";
    private static final String UPDATE_REQUEST_FIXTURE = "/volunteer/update-request.json";
    private static final String CONTACT_VALUE_SEPARATOR = ":";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE);

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
        VolunteerDtoApi volunteer = registerVolunteer();

        registeredVolunteerId = volunteer.getId();
        registeredUserId = volunteer.getUserId();

        assertThat(registeredUserId).startsWith("volunteer-");
        assertVolunteerMatchesRegistration(volunteer);
    }

    @Test
    @Order(2)
    @DisplayName("2. Просмотр данных о своей регистрации")
    void shouldViewOwnRegistrationViaRestEndpoint() throws Exception {
        assertThat(registeredVolunteerId).isNotNull();
        assertThat(registeredUserId).isNotBlank();

        VolunteerDtoApi volunteer = getVolunteer(registeredUserId, HttpStatus.OK);

        assertThat(volunteer.getId()).isEqualTo(registeredVolunteerId);
        assertVolunteerMatchesRegistration(volunteer);
    }

    @Test
    @Order(3)
    @DisplayName("3. Обновление данных о себе")
    void shouldUpdateOwnRegistrationViaRestEndpoint() throws Exception {
        assertThat(registeredVolunteerId).isNotNull();
        assertThat(registeredUserId).isNotBlank();

        VolunteerDtoApi volunteer = updateVolunteer(registeredUserId);

        assertThat(volunteer.getId()).isEqualTo(registeredVolunteerId);
        assertVolunteerMatchesUpdate(volunteer);
    }

    @Test
    @Order(4)
    @DisplayName("4. Просмотр обновлённых данных о своей регистрации")
    void shouldViewUpdatedOwnRegistrationViaRestEndpoint() throws Exception {
        assertThat(registeredVolunteerId).isNotNull();
        assertThat(registeredUserId).isNotBlank();

        VolunteerDtoApi volunteer = getVolunteer(registeredUserId, HttpStatus.OK);

        assertThat(volunteer.getId()).isEqualTo(registeredVolunteerId);
        assertVolunteerMatchesUpdate(volunteer);
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

    private VolunteerDtoApi registerVolunteer() throws Exception {
        VolunteerRegisterRequestApi request = readRegisterRequestFixture();

        HttpResponse<String> response = httpClient.send(
                requestBuilder(REGISTER_ME_URL)
                        .POST(jsonBody(request))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        return readVolunteer(response);
    }

    private VolunteerDtoApi getVolunteer(String userId, HttpStatus expectedStatus) throws Exception {
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

    private VolunteerDtoApi updateVolunteer(String userId) throws Exception {
        VolunteerUpdateRequestApi request = readUpdateRequestFixture();

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

    private VolunteerDtoApi readVolunteer(HttpResponse<String> response) {
        assertThat(response.body()).isNotBlank();
        return objectMapper.readValue(response.body(), VolunteerDtoApi.class);
    }

    private VolunteerRegisterRequestApi readRegisterRequestFixture() throws Exception {
        return readJsonFixture(REGISTER_REQUEST_FIXTURE, VolunteerRegisterRequestApi.class);
    }

    private VolunteerUpdateRequestApi readUpdateRequestFixture() throws Exception {
        return readJsonFixture(UPDATE_REQUEST_FIXTURE, VolunteerUpdateRequestApi.class);
    }

    private <T> T readJsonFixture(String resourcePath, Class<T> type) throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            assertThat(inputStream)
                    .as("JSON fixture %s must exist", resourcePath)
                    .isNotNull();
            return objectMapper.readValue(inputStream, type);
        }
    }

    private void assertVolunteerMatchesRegistration(VolunteerDtoApi volunteer) throws Exception {
        VolunteerRegisterRequestApi expected = readRegisterRequestFixture();

        assertVolunteerPersonalData(
                volunteer,
                expected.getFirstName(),
                expected.getFamilyName(),
                expected.getPatronymic(),
                expected.getGender(),
                expected.getBirthDate()
        );
        assertVolunteerCommonState(volunteer);
        assertVolunteerContacts(volunteer, expected.getPhoneNumber(), expected.getEmail());
    }

    private void assertVolunteerMatchesUpdate(VolunteerDtoApi volunteer) throws Exception {
        VolunteerRegisterRequestApi registration = readRegisterRequestFixture();
        VolunteerUpdateRequestApi expected = readUpdateRequestFixture();

        assertVolunteerPersonalData(
                volunteer,
                expected.getFirstName(),
                expected.getFamilyName(),
                expected.getPatronymic(),
                registration.getGender(),
                registration.getBirthDate()
        );
        assertVolunteerCommonState(volunteer);
        assertVolunteerContacts(volunteer, expected.getPhoneNumber(), expected.getEmail());
    }

    private void assertVolunteerPersonalData(
            VolunteerDtoApi volunteer,
            String expectedFirstName,
            String expectedFamilyName,
            String expectedPatronymic,
            GenderApi expectedGender,
            LocalDate expectedBirthDate
    ) {
        assertThat(volunteer.getFirstName()).isEqualTo(expectedFirstName);
        assertThat(volunteer.getFamilyName()).isEqualTo(expectedFamilyName);
        assertThat(volunteer.getPatronymic()).isEqualTo(expectedPatronymic);
        assertThat(volunteer.getGender()).isEqualTo(expectedGender);
        assertThat(volunteer.getBirthDate()).isEqualTo(expectedBirthDate);
    }

    private void assertVolunteerCommonState(VolunteerDtoApi volunteer) {
        assertThat(volunteer.getStatus()).isEqualTo(VolunteerStatusApi.FREE);
        assertThat(volunteer.getLocationId()).isNotNull();
        assertThat(volunteer.getCurrentIncidentId()).isNull();
    }

    private void assertVolunteerContacts(VolunteerDtoApi volunteer, String expectedPhoneNumber, String expectedEmail) {
        assertThat(volunteer.getContacts())
                .extracting(contact -> contact.getContactType() + CONTACT_VALUE_SEPARATOR + contact.getContact())
                .containsExactlyInAnyOrder(
                        ContactTypeApi.PHONE + CONTACT_VALUE_SEPARATOR + expectedPhoneNumber,
                        ContactTypeApi.EMAIL + CONTACT_VALUE_SEPARATOR + expectedEmail
                );
    }
}