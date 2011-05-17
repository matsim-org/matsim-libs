package playground.michalm.vrp.events;

import org.matsim.api.core.v01.*;
import org.matsim.core.api.experimental.events.*;

import pl.poznan.put.vrp.dynamic.customer.*;


public interface VRPCustomerEvent
    extends Event
{
    Id getFromLinkId();


    Id getToLinkId();
    
    
    CustomerAction getCustomerAction();
}
