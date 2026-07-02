package ru.rom8.rescue.volunteer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import ru.rom8.rescue.volunteer.domain.entity.ContactInfo;
import ru.rom8.rescue.volunteer.domain.entity.ContactType;
import ru.rom8.rescue.volunteer.domain.entity.Location;
import ru.rom8.rescue.volunteer.domain.entity.LocationKind;
import ru.rom8.rescue.volunteer.domain.entity.Volunteer;
import ru.rom8.rescue.volunteer.dto.VolunteerDto;
import ru.rom8.rescue.volunteer.dto.VolunteerRegisterRequest;
import ru.rom8.rescue.volunteer.mapper.VolunteerMapper;
import ru.rom8.rescue.volunteer.repository.ContactInfoRepository;
import ru.rom8.rescue.volunteer.repository.LocationRepository;
import ru.rom8.rescue.volunteer.repository.VolunteerRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VolunteerRegistrationService {

    private static final String USER_ID_PREFIX = "volunteer-";

    private final VolunteerRepository volunteerRepository;
    private final LocationRepository locationRepository;
    private final ContactInfoRepository contactInfoRepository;
    private final VolunteerMapper volunteerMapper;

    @Transactional
    public VolunteerDto register(VolunteerRegisterRequest request) {
        Location location = resolveLocation(request.settlementName(), request.settlementDistrictName());

        Volunteer volunteer = volunteerMapper.toEntity(request);
        volunteer.setUserId(generateUserId());  //todo надо ли это?
        volunteer.setLocation(location);

        Volunteer savedVolunteer = volunteerRepository.save(volunteer);
        savedVolunteer.getContacts().add(createContact(savedVolunteer, request.phoneNumber(), ContactType.PHONE));
        savedVolunteer.getContacts().add(createContact(savedVolunteer, request.email(), ContactType.EMAIL));

        return volunteerMapper.toDto(savedVolunteer);
    }

    @Transactional(readOnly = true)
    public VolunteerDto getByUserId(String userId) {
        return volunteerMapper.toDto(getVolunteerByUserId(userId));
    }

    @Transactional
    public void deleteByUserId(String userId) {
        volunteerRepository.delete(getVolunteerByUserId(userId));
    }

    private Volunteer getVolunteerByUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Header X-USER-ID is required");
        }

        return volunteerRepository.findByUserId(userId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Volunteer registration not found"));
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
