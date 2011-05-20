package playground.michalm.vrp.events;

import org.matsim.api.core.v01.*;
import org.matsim.core.api.experimental.events.*;


public interface VRPCustomerEvent
    extends Event
{
    Id getFromLinkId();


    Id getToLinkId();
}
