package ru.rom8.rescue.volunteer.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "volunteer")
public class Volunteer extends AbstractAuditableEntity {

    @Column(name = "user_id", nullable = false, unique = true, length = 100)
    private String userId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "family_name", nullable = false, length = 100)
    private String familyName;

    @Column(name = "patronymic", length = 100)
    private String patronymic;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 10)
    private Gender gender;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VolunteerStatus status = VolunteerStatus.FREE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "current_incident_id")
    private UUID currentIncidentId;

    @OneToMany(mappedBy = "volunteer")
    private Set<ContactInfo> contacts = new HashSet<>();
}
