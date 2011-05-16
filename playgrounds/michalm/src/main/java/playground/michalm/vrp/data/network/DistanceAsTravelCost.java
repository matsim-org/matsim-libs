package playground.michalm.vrp.data.network;

import org.matsim.api.core.v01.network.*;
import org.matsim.core.router.util.*;


public class DistanceAsTravelCost
    implements TravelCost
{
    @Override
    public double getLinkGeneralizedTravelCost(Link link, double time)
    {
        return link.getLength();
    }
}
