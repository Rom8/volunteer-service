package ru.rom8.rescue.volunteer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rom8.rescue.volunteer.domain.entity.ContactInfo;

public interface ContactInfoRepository extends JpaRepository<ContactInfo, Long> {
}
