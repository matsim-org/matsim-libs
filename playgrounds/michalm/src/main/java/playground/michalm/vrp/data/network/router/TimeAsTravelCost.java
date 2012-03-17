package playground.michalm.vrp.data.network.router;

import org.matsim.api.core.v01.network.*;
import org.matsim.core.router.util.*;


public class TimeAsTravelCost
    implements TravelDisutility
{
    private TravelTime travelTime;


    public TimeAsTravelCost(TravelTime travelTime)
    {
        this.travelTime = travelTime;
    }


    @Override
    public double getLinkTravelDisutility(Link link, double time)
    {
        return travelTime.getLinkTravelTime(link, time);
    }
}
