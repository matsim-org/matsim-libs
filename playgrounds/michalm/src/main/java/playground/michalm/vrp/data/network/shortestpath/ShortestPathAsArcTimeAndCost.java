package playground.michalm.vrp.data.network.shortestpath;

import pl.poznan.put.vrp.dynamic.data.network.*;


/**
 * The current implementation is simplistic; the class will be re-implemented in the future.
 * 
 * @author michalm
 *
 */
public class ShortestPathAsArcTimeAndCost
    implements ShortestPath, ArcTime, ArcCost
{
    private ShortestPath shortestPath;


    public ShortestPathAsArcTimeAndCost(ShortestPath shortestPath)
    {
        this.shortestPath = shortestPath;
    }


    @Override
    public SPEntry getSPEntry(int departTime)
    {
        return shortestPath.getSPEntry(departTime);
    }


    @Override
    public int getArcTimeOnDeparture(int departureTime)
    {
        // no interpolation between consecutive timeSlices!
        return shortestPath.getSPEntry(departureTime).travelTime;
    }


    @Override
    public int getArcTimeOnArrival(int arrivalTime)
    {
        // very rough!!!
        return shortestPath.getSPEntry(arrivalTime).travelTime;

        // probably a bit more accurate but still rough and more time consuming
        // return shortestPath.getSPEntry(arrivalTime -
        // shortestPath.getSPEntry(arrivalTime).travelTime);
    }


    @Override
    public double getArcCostOnDeparture(int departureTime)
    {
        // no interpolation between consecutive timeSlices!
        return shortestPath.getSPEntry(departureTime).travelCost;
    }
}
