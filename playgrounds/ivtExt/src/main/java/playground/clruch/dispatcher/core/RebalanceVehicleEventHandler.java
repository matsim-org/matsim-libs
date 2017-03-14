package playground.clruch.dispatcher.core;

import org.matsim.core.events.handler.EventHandler;

public interface RebalanceVehicleEventHandler extends EventHandler {
    public void handleEvent(RebalanceVehicleEvent event);

}
