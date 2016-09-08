package playground.sebhoerl.av.logic.events;

import playground.sebhoerl.agentlock.events.Event;
import playground.sebhoerl.av.logic.service.Service;

public class ServiceEvent extends Event {
    final private Service service;
    
    public ServiceEvent(Service service) {
        this.service = service;
    }
    
    public Service getService() {
        return this.service;
    }
}
