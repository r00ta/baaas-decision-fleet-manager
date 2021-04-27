package org.kie.baaas.mcp.app.listener;

import java.util.Optional;

public interface Event {

    default Optional<String> getEventId() {
        return Optional.empty();
    }

}
