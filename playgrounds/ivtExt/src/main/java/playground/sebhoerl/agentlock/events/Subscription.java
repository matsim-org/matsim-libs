package playground.sebhoerl.agentlock.events;

public class Subscription {
    final private Event event;
    final private EventListener listener;
    
    private Subscription(Event event, EventListener listener) {
        this.event = event;
        this.listener = listener;
        
        event.addListener(listener);
    }
    
    public void cancel() {
        event.removeListener(listener);
    }
    
    static public Subscription create(Event event, EventListener listener) {
        return new Subscription(event, listener);
    }
}