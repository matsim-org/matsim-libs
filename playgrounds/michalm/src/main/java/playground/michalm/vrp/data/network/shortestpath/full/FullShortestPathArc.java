package playground.michalm.vrp.data.network.shortestpath.full;

import pl.poznan.put.vrp.dynamic.data.network.*;
import playground.michalm.vrp.data.network.shortestpath.*;
import playground.michalm.vrp.data.network.shortestpath.ShortestPath.SPEntry;


public class FullShortestPathArc
    extends InterpolatedArc
    implements ShortestPathArc
{
    private FullShortestPath shortestPath;


    public FullShortestPathArc(int interval, boolean cyclic, int[] timesOnDeparture,
            double[] costsOnDeparture, FullShortestPath shortestPath)
    {
        super(interval, cyclic, timesOnDeparture, costsOnDeparture);
        this.shortestPath = shortestPath;
    }


    @Override
    public ShortestPath getShortestPath()
    {
        return shortestPath;
    }


    public static FullShortestPathArc createArc(FullShortestPath shortestPath, int interval,
            boolean cyclic)
    {
        SPEntry[] entries = shortestPath.entries;
        int numSlots = entries.length;

        int[] timesOnDeparture = new int[numSlots];
        double[] costsOnDeparture = new double[numSlots];

        for (int k = 0; k < numSlots; k++) {
            timesOnDeparture[k] = entries[k].travelTime;
            costsOnDeparture[k] = entries[k].travelCost;
        }

        return new FullShortestPathArc(interval, cyclic, timesOnDeparture, costsOnDeparture,
                shortestPath);
    }
}
