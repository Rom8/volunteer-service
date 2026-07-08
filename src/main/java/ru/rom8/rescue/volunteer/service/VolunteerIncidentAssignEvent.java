package ru.rom8.rescue.volunteer.service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record VolunteerIncidentAssignEvent(
        @JsonProperty("incident_id") UUID incidentId,
        @JsonProperty("volunteer_id") Long volunteerId,
        @JsonProperty("status") String status
) {
}
