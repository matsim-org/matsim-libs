package playground.michalm.vrp.data.network.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelDisutility;


public class DistanceAsTravelCost
    implements TravelDisutility
{
    @Override
    public double getLinkTravelDisutility(Link link, double time)
    {
        return link.getLength();
    }
}
