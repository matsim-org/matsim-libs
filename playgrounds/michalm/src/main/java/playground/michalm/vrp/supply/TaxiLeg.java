package playground.michalm.vrp.supply;

import org.matsim.api.core.v01.*;

import playground.michalm.dynamic.*;
import playground.michalm.vrp.data.network.ShortestPath.SPEntry;

import com.google.common.collect.*;


public class TaxiLeg
    extends DynLegImpl
{
    public TaxiLeg(SPEntry path, Id destinationLinkId)
    {
        super(Iterators.forArray(path.linkIds), destinationLinkId);
    }


    public void endLeg(double now)
    {}
}
