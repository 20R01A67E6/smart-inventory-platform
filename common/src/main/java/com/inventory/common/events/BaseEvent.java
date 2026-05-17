package com.inventory.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public abstract class BaseEvent {

    private final String eventId;
    private final Instant timestamp;
    private final String correlationId;

    @JsonCreator
    protected BaseEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("correlationId") String correlationId) {
        this.eventId = eventId;
        this.timestamp = timestamp;
        this.correlationId = correlationId;
    }

    public String getEventId() { return eventId; }
    public Instant getTimestamp() { return timestamp; }
    public String getCorrelationId() { return correlationId; }
}
