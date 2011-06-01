package playground.michalm.vrp.events.handler;

import org.matsim.core.events.handler.*;

import playground.michalm.vrp.events.*;


public interface VRPVehicleEventHandler
    extends EventHandler
{
    void handleEvent(VRPVehicleEvent event);
}
