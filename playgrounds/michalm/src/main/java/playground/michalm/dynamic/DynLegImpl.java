package playground.michalm.dynamic;

import java.util.*;

import org.matsim.api.core.v01.*;


public class DynLegImpl
    implements DynLeg
{
    private Iterator< ? extends Id> linkIdIter;
    private Id destinationLinkId;


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
