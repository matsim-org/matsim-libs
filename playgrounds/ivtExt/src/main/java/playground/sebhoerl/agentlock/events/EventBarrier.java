package playground.sebhoerl.agentlock.events;

public class EventBarrier extends Event {
    int registered = 0;
    int fired = 0;
    
    public void fire() {
        throw new UnsupportedOperationException("Can only listen to EventBarriers");
    }
    
    private void count() {
        fired++;
        
        if (fired == registered) {
            super.fire();
        }
    }
    
    public void addEvent(Event event) {
        event.addListener(new EventListener() {
            @Override
            public void notifyEvent(Event event) {
                count();
            }
        });
    }
}
