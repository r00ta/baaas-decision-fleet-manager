package org.kie.baaas.dfm.app.listener;

import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class EventWithId implements Event {

    @JsonIgnore
    private final String eventId = UUID.randomUUID().toString();

    @JsonIgnore
    @Override
    public Optional<String> getEventId() {
        return Optional.of(eventId);
    }
}
