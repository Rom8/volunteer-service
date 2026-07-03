package ru.rom8.rescue.volunteer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.rom8.rescue.volunteer.domain.entity.Volunteer;
import ru.rom8.rescue.volunteer.domain.entity.VolunteerStatus;

import java.util.List;
import java.util.Optional;

public interface VolunteerRepository extends JpaRepository<Volunteer, Long> {

    Optional<Volunteer> findByUserId(String userId);

    @Query(value = """
            select volunteer.*
            from volunteer volunteer
                join location location on location.id = volunteer.location_id
                left join location parent_location on parent_location.id = location.parent_loc_id
            where (:settlementName is null or location.name = :settlementName or parent_location.name = :settlementName)
              and (:settlementDistrictName is null or parent_location.id is not null and location.name = :settlementDistrictName)
              and (:status is null or volunteer.status = :status)
            """, nativeQuery = true)
    List<Volunteer> findByFilters(
            @Param("settlementName") String settlementName,
            @Param("settlementDistrictName") String settlementDistrictName,
            @Param("status") String status
    );
}
