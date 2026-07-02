package ru.rom8.rescue.volunteer.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@MappedSuperclass
public abstract class AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "create_date", nullable = false, updatable = false)
    private OffsetDateTime createDate;

    @Column(name = "update_date")
    private OffsetDateTime updateDate;

    @PrePersist
    protected void onCreate() {
        createDate = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateDate = OffsetDateTime.now();
    }
}
