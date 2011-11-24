package playground.michalm.dynamic;

import org.matsim.api.core.v01.*;


public interface DynLeg
{
    Id getNextLinkId();


    Id getDestinationLinkId();
}
