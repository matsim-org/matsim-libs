package playground.michalm.vrp.events.handler;

import org.matsim.core.events.handler.*;

import playground.michalm.vrp.events.*;


public interface VRPCustomerEventHandler
    extends EventHandler
{
    void handleEvent(VRPCustomerEvent event);
}
