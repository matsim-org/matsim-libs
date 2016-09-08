package playground.sebhoerl.av.logic.events;

import playground.sebhoerl.av.logic.service.Service;

public class PickupEvent extends ServiceEvent {
    public PickupEvent(Service service) {
        super(service);
    }
}
