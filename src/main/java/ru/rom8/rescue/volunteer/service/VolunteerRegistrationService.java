package ru.rom8.rescue.volunteer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import ru.rom8.rescue.volunteer.api.model.VolunteerDto;
import ru.rom8.rescue.volunteer.api.model.VolunteerRegisterRequest;
import ru.rom8.rescue.volunteer.api.model.VolunteerStatus;
import ru.rom8.rescue.volunteer.api.model.VolunteerUpdateRequest;
import ru.rom8.rescue.volunteer.domain.entity.ContactInfo;
import ru.rom8.rescue.volunteer.domain.entity.ContactType;
import ru.rom8.rescue.volunteer.domain.entity.Location;
import ru.rom8.rescue.volunteer.domain.entity.LocationKind;
import ru.rom8.rescue.volunteer.domain.entity.Volunteer;
import ru.rom8.rescue.volunteer.mapper.VolunteerMapper;
import ru.rom8.rescue.volunteer.repository.ContactInfoRepository;
import ru.rom8.rescue.volunteer.repository.LocationRepository;
import ru.rom8.rescue.volunteer.repository.VolunteerRepository;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VolunteerRegistrationService {

    private static final String USER_ID_PREFIX = "volunteer-";
    private static final String USER_ID_HEADER_REQUIRED_MESSAGE = "Header X-USER-ID is required";
    private static final String VOLUNTEER_NOT_FOUND_MESSAGE = "Volunteer registration not found";

    private final VolunteerRepository volunteerRepository;
    private final LocationRepository locationRepository;
    private final ContactInfoRepository contactInfoRepository;
    private final VolunteerMapper volunteerMapper;

    @Transactional
    public VolunteerDto register(VolunteerRegisterRequest request) {
        Location location = resolveLocation(request.getSettlementName(), request.getSettlementDistrictName());

        Volunteer volunteer = volunteerMapper.toEntity(request);
        volunteer.setUserId(generateUserId());  //todo надо ли это?
        volunteer.setLocation(location);

        Volunteer savedVolunteer = volunteerRepository.save(volunteer);
        savedVolunteer.getContacts().add(createContact(savedVolunteer, request.getPhoneNumber(), ContactType.PHONE));
        savedVolunteer.getContacts().add(createContact(savedVolunteer, request.getEmail(), ContactType.EMAIL));

        return volunteerMapper.toDto(savedVolunteer);
    }

    @Transactional(readOnly = true)
    public VolunteerDto getByUserId(String userId) {
        return volunteerMapper.toDto(getVolunteerByUserId(userId));
    }

    @Transactional(readOnly = true)
    public VolunteerDto getById(Long id) {
        Volunteer volunteer = volunteerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, VOLUNTEER_NOT_FOUND_MESSAGE));
        return volunteerMapper.toDto(volunteer);
    }

    @Transactional(readOnly = true)
    public List<VolunteerDto> getList(String settlementName, String settlementDistrictName, VolunteerStatus status) {
        return volunteerMapper.toDtoList(volunteerRepository.findByFilters(
                normalizeFilter(settlementName),
                normalizeFilter(settlementDistrictName),
                toVolunteerStatus(status)
        ));
    }

    @Transactional(readOnly = true)
    public List<VolunteerDto> getByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> volunteerIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (volunteerIds.isEmpty()) {
            return Collections.emptyList();
        }

        return volunteerMapper.toDtoList(volunteerRepository.findAllById(volunteerIds));
    }

    @Transactional
    public VolunteerDto updateByUserId(String userId, VolunteerUpdateRequest request) {
        Volunteer volunteer = getVolunteerByUserId(userId);

        updatePersonalInfo(volunteer, request);
        updateLocation(volunteer, request);
        updateContact(volunteer, request.getPhoneNumber(), ContactType.PHONE);
        updateContact(volunteer, request.getEmail(), ContactType.EMAIL);

        return volunteerMapper.toDto(volunteerRepository.save(volunteer));
    }

    @Transactional
    public void deleteByUserId(String userId) {
        volunteerRepository.delete(getVolunteerByUserId(userId));
    }

    private Volunteer getVolunteerByUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, USER_ID_HEADER_REQUIRED_MESSAGE);
        }

        return volunteerRepository.findByUserId(userId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, VOLUNTEER_NOT_FOUND_MESSAGE));
    }

    private String normalizeFilter(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String toVolunteerStatus(VolunteerStatus status) {
        return status == null
                ? null
                : status.name();
    }

    private void updatePersonalInfo(Volunteer volunteer, VolunteerUpdateRequest request) {
        if (StringUtils.hasText(request.getFamilyName())) {
            volunteer.setFamilyName(request.getFamilyName().trim());
        }
        if (StringUtils.hasText(request.getFirstName())) {
            volunteer.setFirstName(request.getFirstName().trim());
        }
        if (request.getPatronymic() != null) {
            volunteer.setPatronymic(StringUtils.hasText(request.getPatronymic()) ? request.getPatronymic().trim() : null);
        }
    }

    private void updateLocation(Volunteer volunteer, VolunteerUpdateRequest request) {
        if (request.getSettlementName() == null && request.getSettlementDistrictName() == null) {
            return;
        }

        String settlementName = StringUtils.hasText(request.getSettlementName())
                ? request.getSettlementName().trim()
                : getCurrentSettlementName(volunteer);
        String districtName = request.getSettlementDistrictName() == null
                ? getCurrentDistrictName(volunteer)
                : request.getSettlementDistrictName().trim();

        volunteer.setLocation(resolveLocation(settlementName, districtName));
    }

    private String getCurrentSettlementName(Volunteer volunteer) {
        Location location = volunteer.getLocation();
        if (location.getParent() == null) {
            return location.getName();
        }
        return location.getParent().getName();
    }

    private String getCurrentDistrictName(Volunteer volunteer) {
        Location location = volunteer.getLocation();
        if (location.getParent() == null) {
            return null;
        }
        return location.getName();
    }

    private void updateContact(Volunteer volunteer, String contact, ContactType contactType) {
        if (!StringUtils.hasText(contact)) {
            return;
        }

        ContactInfo contactInfo = volunteer.getContacts().stream()
                .filter(currentContact -> currentContact.getContactType() == contactType)
                .findFirst()
                .orElseGet(() -> createContact(volunteer, contact, contactType));
        contactInfo.setContact(contact.trim());
    }

    private Location resolveLocation(String settlementName, String districtName) {  //todo разобраться в логике
        Location settlement = locationRepository.findByNameAndParentIsNull(settlementName)
                .orElseGet(() -> createLocation(settlementName, null, LocationKind.PARENT));

        if (!StringUtils.hasText(districtName)) {
            return settlement;
        }

        String normalizedDistrictName = districtName.trim();
        return locationRepository.findByNameAndParent(normalizedDistrictName, settlement)
                .orElseGet(() -> createLocation(normalizedDistrictName, settlement, LocationKind.CHILD));
    }

    private Location createLocation(String name, Location parent, LocationKind locationKind) {
        Location location = new Location();
        location.setName(name.trim());
        location.setParent(parent);
        location.setLocationKind(locationKind);
        return locationRepository.save(location);
    }

    private ContactInfo createContact(Volunteer volunteer, String contact, ContactType contactType) {
        ContactInfo contactInfo = new ContactInfo();
        contactInfo.setVolunteer(volunteer);
        contactInfo.setContact(contact.trim());
        contactInfo.setContactType(contactType);
        return contactInfoRepository.save(contactInfo);
    }

    private String generateUserId() {
        return USER_ID_PREFIX + UUID.randomUUID();
    }
}
