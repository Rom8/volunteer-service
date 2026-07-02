package ru.rom8.rescue.volunteer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rom8.rescue.volunteer.domain.entity.Location;

import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {

    Optional<Location> findByNameAndParentIsNull(String name);

    Optional<Location> findByNameAndParent(String name, Location parent);
}
