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

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "location")
public class Location extends AbstractAuditableEntity {

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_loc_id")
    private Location parent;

    @OneToMany(mappedBy = "parent")
    private Set<Location> children = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "location_kind", nullable = false, length = 10)
    private LocationKind locationKind = LocationKind.PARENT;
}
