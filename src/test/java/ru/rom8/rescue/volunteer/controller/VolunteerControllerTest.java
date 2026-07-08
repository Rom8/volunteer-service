package ru.rom8.rescue.volunteer.controller;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import ru.rom8.rescue.volunteer.api.model.*;
import ru.rom8.rescue.volunteer.service.VolunteerIncidentAssignEvent;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(
        topics = VolunteerControllerTest.VOLUNTEER_INCIDENT_ASSIGN_TOPIC
)
class VolunteerControllerTest {

    private static final String POSTGRES_IMAGE = "postgres:17.10";
    private static final String BASE_URL = "/api/v1/volunteer";
    private static final String ADMIN_BASE_URL = "/api/v1/admin/volunteer";
    private static final String INTERNAL_BASE_URL = "/internal/api/v1/volunteer";
    private static final String REGISTER_ME_URL = BASE_URL + "/register/me";
    private static final String ME_URL = BASE_URL + "/me";
    private static final String INCIDENT_ACTION_URL = ME_URL + "/incident/act";
    private static final String ADMIN_VOLUNTEER_LIST_URL = ADMIN_BASE_URL + "/list";
    private static final String INTERNAL_VOLUNTEER_LIST_URL = INTERNAL_BASE_URL + "/list";
    private static final String USER_ID_HEADER = "X-USER-ID";
    private static final String REGISTER_REQUEST_FIXTURE = "/volunteer/register-request.json";
    private static final String UPDATE_REQUEST_FIXTURE = "/volunteer/update-request.json";
    private static final String CONTACT_VALUE_SEPARATOR = ":";
    private static final String PHONE_NUMBER_FORMAT = "+7999001%04d";
    private static final String EMAIL_FORMAT = "volunteer-%d@example.org";
    static final String VOLUNTEER_INCIDENT_ASSIGN_TOPIC = "volunteer_incident_assign_event_v1";
    private static final UUID INCIDENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_INCIDENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE);

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private final AtomicInteger testVolunteerCounter = new AtomicInteger();

    private Long id;
    private String userId;

    Consumer<String, String> consumer;

    @BeforeAll
    void beforeAll() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(embeddedKafkaBroker, "test-VolunteerControllerTest", false);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        var kafkaConsumerFactory = new DefaultKafkaConsumerFactory<String, String>(consumerProps);
        consumer = kafkaConsumerFactory.createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, VOLUNTEER_INCIDENT_ASSIGN_TOPIC);
    }

    @Test
    @Order(1)
    @DisplayName("1. Регистрация волонтёра")
    void shouldRegisterVolunteerViaRestEndpoint() throws Exception {
        VolunteerDto volunteer = registerVolunteer();

        id = volunteer.getId();
        userId = volunteer.getUserId();

        assertThat(userId).startsWith("volunteer-");
        assertVolunteerMatchesRegistration(volunteer);
    }

    @Test
    @Order(2)
    @DisplayName("2. Просмотр данных о своей регистрации")
    void shouldViewOwnRegistrationViaRestEndpoint() throws Exception {
        assertThat(id).isNotNull();
        assertThat(userId).isNotBlank();

        VolunteerDto volunteer = getVolunteer(userId, HttpStatus.OK);

        assertThat(volunteer.getId()).isEqualTo(id);
        assertVolunteerMatchesRegistration(volunteer);
    }

    @Test
    @Order(3)
    @DisplayName("3. Обновление данных о себе")
    void shouldUpdateOwnRegistrationViaRestEndpoint() throws Exception {
        assertThat(id).isNotNull();
        assertThat(userId).isNotBlank();

        VolunteerDto volunteer = updateVolunteer(userId);

        assertThat(volunteer.getId()).isEqualTo(id);
        assertVolunteerMatchesUpdate(volunteer);
    }

    @Test
    @Order(4)
    @DisplayName("4. Просмотр обновлённых данных о своей регистрации")
    void shouldViewUpdatedOwnRegistrationViaRestEndpoint() throws Exception {
        assertThat(id).isNotNull();
        assertThat(userId).isNotBlank();

        VolunteerDto volunteer = getVolunteer(userId, HttpStatus.OK);

        assertThat(volunteer.getId()).isEqualTo(id);
        assertVolunteerMatchesUpdate(volunteer);
    }

    @Test
    @Order(5)
    @DisplayName("5. Получение волонтёра администратором по идентификатору")
    void shouldGetVolunteerByIdViaAdminRestEndpoint() throws Exception {
        VolunteerRegisterRequest request = uniqueRegisterRequest();
        VolunteerDto registeredVolunteer = registerVolunteer(request);

        HttpResponse<String> response = httpClient.send(
                requestBuilder(ADMIN_BASE_URL + "/" + registeredVolunteer.getId())
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        VolunteerDto volunteer = readVolunteer(response);
        assertThat(volunteer.getId()).isEqualTo(registeredVolunteer.getId());
        assertThat(volunteer.getUserId()).isEqualTo(registeredVolunteer.getUserId());
        assertVolunteerMatchesRegistration(volunteer, request);
    }

    @Test
    @Order(6)
    @DisplayName("6. Получение списка волонтёров администратором с фильтрацией")
    void shouldGetVolunteerListViaAdminRestEndpoint() throws Exception {
        VolunteerRegisterRequest request = uniqueRegisterRequest();
        VolunteerDto registeredVolunteer = registerVolunteer(request);

        HttpResponse<String> response = httpClient.send(
                requestBuilder(ADMIN_VOLUNTEER_LIST_URL
                        + "?settlementName=" + request.getSettlementName()
                        + "&status=" + VolunteerStatus.FREE)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<VolunteerDto> volunteers = readVolunteerList(response);
        assertThat(volunteers)
                .extracting(VolunteerDto::getId)
                .contains(registeredVolunteer.getId());
        VolunteerDto volunteer = findVolunteer(volunteers, registeredVolunteer.getId());
        assertVolunteerMatchesRegistration(volunteer, request);
    }

    @Test
    @Order(7)
    @DisplayName("7. Получение списка волонтёров внутренним API по идентификаторам")
    void shouldGetVolunteerListByIdsViaInternalRestEndpoint() throws Exception {
        VolunteerRegisterRequest firstRequest = uniqueRegisterRequest();
        VolunteerRegisterRequest secondRequest = uniqueRegisterRequest();
        VolunteerDto firstVolunteer = registerVolunteer(firstRequest);
        VolunteerDto secondVolunteer = registerVolunteer(secondRequest);

        HttpResponse<String> response = httpClient.send(
                requestBuilder(INTERNAL_VOLUNTEER_LIST_URL)
                        .POST(jsonBody(List.of(firstVolunteer.getId(), secondVolunteer.getId())))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<VolunteerDto> volunteers = readVolunteerList(response);
        assertThat(volunteers)
                .extracting(VolunteerDto::getId)
                .containsExactlyInAnyOrder(firstVolunteer.getId(), secondVolunteer.getId());
        assertVolunteerMatchesRegistration(findVolunteer(volunteers, firstVolunteer.getId()), firstRequest);
        assertVolunteerMatchesRegistration(findVolunteer(volunteers, secondVolunteer.getId()), secondRequest);
    }

    @Test
    @Order(8)
    @DisplayName("8. Принятие участия в инциденте")
    void shouldAcceptIncidentViaRestEndpoint() throws Exception {
        assertThat(id).isNotNull();
        assertThat(userId).isNotBlank();

        VolunteerDto volunteer = actOnIncident(userId, VolunteerIncidentAction.ACCEPT);

        assertThat(volunteer.getId()).isEqualTo(id);
        assertThat(volunteer.getStatus()).isEqualTo(VolunteerStatus.ASSIGNED_TASK);
        assertThat(volunteer.getCurrentIncidentId()).isEqualTo(INCIDENT_ID);

        checkLastMessageFromTopic("ACCEPT");
    }

    private void checkLastMessageFromTopic(String status) {
        ConsumerRecord<String, String> consumerRecord = KafkaTestUtils.getSingleRecord(
                consumer, VOLUNTEER_INCIDENT_ASSIGN_TOPIC, Duration.ofSeconds(5));

        UUID key = UUID.fromString(consumerRecord.key());
        assertThat(key).isEqualTo(INCIDENT_ID);

        VolunteerIncidentAssignEvent volunteerIncidentAssignEvent =
                objectMapper.readValue(consumerRecord.value(), VolunteerIncidentAssignEvent.class);
        assertThat(volunteerIncidentAssignEvent.incidentId()).isEqualTo(INCIDENT_ID);
        assertThat(volunteerIncidentAssignEvent.volunteerId()).isEqualTo(id);
        assertThat(volunteerIncidentAssignEvent.status()).isEqualTo(status);
    }

    @Test
    @Order(9)
    @DisplayName("9. Ошибка при принятии другого инцидента назначенным волонтёром")
    void shouldReturnConflictWhenAssignedVolunteerAcceptsAnotherIncidentViaRestEndpoint() throws Exception {
        assertThat(id).isNotNull();
        assertThat(userId).isNotBlank();

        VolunteerIncidentActionRequest request = new VolunteerIncidentActionRequest(
                OTHER_INCIDENT_ID,
                VolunteerIncidentAction.ACCEPT
        );

        HttpResponse<String> response = httpClient.send(
                requestBuilder(INCIDENT_ACTION_URL)
                        .header(USER_ID_HEADER, userId)
                        .POST(jsonBody(request))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());

        VolunteerDto volunteer = getVolunteer(userId, HttpStatus.OK);
        assertThat(volunteer.getId()).isEqualTo(id);
        assertThat(volunteer.getStatus()).isEqualTo(VolunteerStatus.ASSIGNED_TASK);
        assertThat(volunteer.getCurrentIncidentId()).isEqualTo(INCIDENT_ID);
    }

    @Test
    @Order(10)
    @DisplayName("10. Отказ от участия в инциденте")
    void shouldRejectIncidentViaRestEndpoint() throws Exception {
        assertThat(id).isNotNull();
        assertThat(userId).isNotBlank();

        VolunteerDto volunteer = actOnIncident(userId, VolunteerIncidentAction.REJECT);

        assertThat(volunteer.getId()).isEqualTo(id);
        assertThat(volunteer.getStatus()).isEqualTo(VolunteerStatus.FREE);
        assertThat(volunteer.getCurrentIncidentId()).isNull();

        checkLastMessageFromTopic("REJECT");
    }

    @Test
    @Order(11)
    @DisplayName("11. Удаление своей учётной записи волонтёра")
    void shouldDeleteOwnRegistrationViaRestEndpoint() throws Exception {
        assertThat(userId).isNotBlank();

        HttpResponse<String> deleteResponse = httpClient.send(
                requestBuilder(ME_URL)
                        .header(USER_ID_HEADER, userId)
                        .DELETE()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(deleteResponse.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());

        getVolunteer(userId, HttpStatus.NOT_FOUND);
    }

    private VolunteerDto registerVolunteer() throws Exception {
        return registerVolunteer(readRegisterRequestFixture());
    }

    private VolunteerDto registerVolunteer(VolunteerRegisterRequest request) throws Exception {
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

    private VolunteerDto actOnIncident(String userId, VolunteerIncidentAction action) throws Exception {
        VolunteerIncidentActionRequest request = new VolunteerIncidentActionRequest(INCIDENT_ID, action);

        HttpResponse<String> response = httpClient.send(
                requestBuilder(INCIDENT_ACTION_URL)
                        .header(USER_ID_HEADER, userId)
                        .POST(jsonBody(request))
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

    private List<VolunteerDto> readVolunteerList(HttpResponse<String> response) {
        assertThat(response.body()).isNotBlank();
        return List.of(objectMapper.readValue(response.body(), VolunteerDto[].class));
    }

    private VolunteerDto findVolunteer(List<VolunteerDto> volunteers, Long volunteerId) {
        return volunteers.stream()
                .filter(volunteer -> volunteerId.equals(volunteer.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Volunteer not found in response: " + volunteerId));
    }

    private VolunteerRegisterRequest readRegisterRequestFixture() throws Exception {
        return readJsonFixture(REGISTER_REQUEST_FIXTURE, VolunteerRegisterRequest.class);
    }

    private VolunteerRegisterRequest uniqueRegisterRequest() throws Exception {
        VolunteerRegisterRequest request = readRegisterRequestFixture();
        int sequence = testVolunteerCounter.incrementAndGet();
        request.setPhoneNumber(PHONE_NUMBER_FORMAT.formatted(sequence));
        request.setEmail(EMAIL_FORMAT.formatted(sequence));
        return request;
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
        assertVolunteerMatchesRegistration(volunteer, readRegisterRequestFixture());
    }

    private void assertVolunteerMatchesRegistration(VolunteerDto volunteer, VolunteerRegisterRequest expected) {
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

    private void assertVolunteerMatchesUpdate(VolunteerDto volunteer) throws Exception {
        VolunteerRegisterRequest registration = readRegisterRequestFixture();
        VolunteerUpdateRequest expected = readUpdateRequestFixture();

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
            VolunteerDto volunteer,
            String expectedFirstName,
            String expectedFamilyName,
            String expectedPatronymic,
            Gender expectedGender,
            LocalDate expectedBirthDate
    ) {
        assertThat(volunteer.getFirstName()).isEqualTo(expectedFirstName);
        assertThat(volunteer.getFamilyName()).isEqualTo(expectedFamilyName);
        assertThat(volunteer.getPatronymic()).isEqualTo(expectedPatronymic);
        assertThat(volunteer.getGender()).isEqualTo(expectedGender);
        assertThat(volunteer.getBirthDate()).isEqualTo(expectedBirthDate);
    }

    private void assertVolunteerCommonState(VolunteerDto volunteer) {
        assertThat(volunteer.getStatus()).isEqualTo(VolunteerStatus.FREE);
        assertThat(volunteer.getLocationId()).isNotNull();
        assertThat(volunteer.getCurrentIncidentId()).isNull();
    }

    private void assertVolunteerContacts(VolunteerDto volunteer, String expectedPhoneNumber, String expectedEmail) {
        assertThat(volunteer.getContacts())
                .extracting(contact -> contact.getContactType() + CONTACT_VALUE_SEPARATOR + contact.getContact())
                .containsExactlyInAnyOrder(
                        ContactType.PHONE + CONTACT_VALUE_SEPARATOR + expectedPhoneNumber,
                        ContactType.EMAIL + CONTACT_VALUE_SEPARATOR + expectedEmail
                );
    }
}