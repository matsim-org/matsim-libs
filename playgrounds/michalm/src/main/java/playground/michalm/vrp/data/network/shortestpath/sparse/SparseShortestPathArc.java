package playground.michalm.vrp.data.network.shortestpath.sparse;

import pl.poznan.put.vrp.dynamic.data.network.*;
import playground.michalm.vrp.data.network.MatsimVertex;
import playground.michalm.vrp.data.network.shortestpath.*;


/**
 * TODO The current implementation is simplistic; the class will be re-implemented in the future.
 * 
 * @author michalm
 */
public class SparseShortestPathArc
    implements ShortestPathArc
{
    private ShortestPath shortestPath;


    public SparseShortestPathArc(ShortestPath shortestPath)
    {
        this.shortestPath = shortestPath;
    }


    @Override
    public int getTimeOnDeparture(int departureTime)
    {
        // no interpolation between consecutive timeSlices!
        return shortestPath.getSPEntry(departureTime).travelTime;
    }


    @Override
    public int getTimeOnArrival(int arrivalTime)
    {
        // TODO: very rough!!!
        return shortestPath.getSPEntry(arrivalTime).travelTime;

        // probably a bit more accurate but still rough and more time consuming
        // return shortestPath.getSPEntry(arrivalTime -
        // shortestPath.getSPEntry(arrivalTime).travelTime);
    }


    @Override
    public double getCostOnDeparture(int departureTime)
    {
        // no interpolation between consecutive timeSlices!
        return shortestPath.getSPEntry(departureTime).travelCost;
    }


    @Override
    public ShortestPath getShortestPath()
    {
        return shortestPath;
    }


    public static class SparseShortestPathArcBuilder
        implements ArcBuilder
    {
        private SparseShortestPathFinder sspFinder;


        public SparseShortestPathArcBuilder(SparseShortestPathFinder sspFinder)
        {
            this.sspFinder = sspFinder;
        }


        @Override
        public Arc build(Vertex vertexFrom, Vertex vertexTo)
        {
            return new SparseShortestPathArc(new SparseShortestPath(sspFinder,
                    (MatsimVertex)vertexFrom, (MatsimVertex)vertexTo));
        }
    }
}
