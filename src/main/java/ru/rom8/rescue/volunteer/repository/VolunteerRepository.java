package ru.rom8.rescue.volunteer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rom8.rescue.volunteer.domain.entity.Volunteer;

import java.util.Optional;

public interface VolunteerRepository extends JpaRepository<Volunteer, Long> {

    Optional<Volunteer> findByUserId(String userId);
}
