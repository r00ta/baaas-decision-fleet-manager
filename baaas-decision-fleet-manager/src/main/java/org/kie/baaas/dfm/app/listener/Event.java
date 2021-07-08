package org.kie.baaas.dfm.app.listener;

import java.util.Optional;

public interface Event {

    default Optional<String> getEventId() {
        return Optional.empty();
    }

}
