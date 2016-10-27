package playground.sebhoerl.av.logic.events;

import playground.sebhoerl.av.logic.service.Service;

public class DropoffEvent extends ServiceEvent {
    public DropoffEvent(Service service) {
        super(service);
    }
}
