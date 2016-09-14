package playground.sebhoerl.agentlock.events;

import java.util.HashSet;
import java.util.Set;

public class Event {
    final private Set<EventListener> listeners = new HashSet<EventListener>();
    
    boolean invalid = false;
    boolean fired = false;
    
    public void addListener(EventListener listener) {
        if (invalid) return;
        
        if (fired) {
            listener.notifyEvent(this);
        } else {
            listeners.add(listener);
        }
    }
    
    public void removeListener(EventListener listener) {
        listeners.remove(listener);
    }
    
    public void fire() {
        if (fired || invalid) return;
        fired = true;
        
        // This makes is safe to remove listeners at any time
        Set<EventListener> local = new HashSet<EventListener>();
        local.addAll(listeners);
        
        for (EventListener listener : local) {
            listener.notifyEvent(this);
        }
    }
    
    public boolean hasFired() {
        return fired;
    }
}
