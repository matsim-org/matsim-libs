package playground.michalm.dynamic;

import java.util.Iterator;

import org.matsim.api.core.v01.Id;
import org.matsim.core.population.routes.NetworkRoute;


public class DynLegImpl
    implements DynLeg
{
    private Iterator< ? extends Id> linkIdIter;
    private Id destinationLinkId;


    public DynLegImpl(NetworkRoute route)
    {
        this(route.getLinkIds().iterator(), route.getEndLinkId());
    }


    public DynLegImpl(Iterator< ? extends Id> linkIdIter, Id destinationLinkId)
    {
        this.linkIdIter = linkIdIter;
        this.destinationLinkId = destinationLinkId;
    }


    @Override
    public Id getNextLinkId()
    {
        if (linkIdIter.hasNext()) {
            return linkIdIter.next();
        }

        return null;
    }


    @Override
    public Id getDestinationLinkId()
    {
        return destinationLinkId;
    }
}
