package ru.rom8.rescue.volunteer.dto;

import ru.rom8.rescue.volunteer.domain.entity.LocationKind;

import java.time.OffsetDateTime;

public record LocationDto(
        Long id,
        OffsetDateTime createDate,
        OffsetDateTime updateDate,
        String name,
        Long parentId,
        LocationKind locationKind
) {
}
