package playground.michalm.vrp.taxi.taxicab;

import org.matsim.api.core.v01.Id;

import playground.michalm.dynamic.DynLegImpl;
import playground.michalm.vrp.data.network.shortestpath.ShortestPath.SPEntry;

import com.google.common.collect.Iterators;


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
