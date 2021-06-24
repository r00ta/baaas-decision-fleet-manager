package org.kie.baaas.mcp.app.listener;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ListenerManager {

    private static final Logger LOG = LoggerFactory.getLogger(ListenerManager.class);

    private Set<Listener> listeners = new HashSet<>();

    public ListenerManager() {
    }

    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }

    public void addListeners(Collection<Listener> listeners) {
        this.listeners.addAll(listeners);
    }

    public void removeListener(Listener listener) {
        this.listeners.remove(listener);
    }

    public Set<Listener> getListeners() {
        return this.listeners;
    }

    public boolean hasListeners() {
        return !this.listeners.isEmpty();
    }

    public void notifyListeners(Event event) {
        for (Listener l : getListeners()) {
            try {
                l.onEvent(event);
            } catch (Throwable t) {
                LOG.error("Error notifying logger", t);
            }
        }
    }

    public <E extends Event> Optional<E> notifyListeners(Supplier<E> eventSupplier) {
        if (hasListeners()) {
            E event = eventSupplier.get();
            notifyListeners(event);
            return Optional.ofNullable(event);
        } else {
            return Optional.empty();
        }
    }
}
