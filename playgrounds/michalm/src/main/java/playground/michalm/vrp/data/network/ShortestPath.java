package playground.michalm.vrp.data.network;

import java.util.*;

import org.matsim.api.core.v01.network.*;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;


public class ShortestPath
{
    public static final Path ZERO_PATH = new Path(new ArrayList<Node>(0), new ArrayList<Link>(0),
            0, 0);

    public static final ShortestPath NO_SHORTEST_PATH = new ShortestPath(0, 0, false);

    private int timeInterval;
    private int intervalCoutn;
    private boolean cyclic;

    Path[] paths;// this contains travel times in-between "fromLink" and "toLink".
    int[] travelTimes;// in QSim a vehicle traverses also "toLink"


    public ShortestPath(int numIntervals, int timeInterval, boolean cyclic)
    {
        this.timeInterval = timeInterval;
        this.intervalCoutn = numIntervals;
        this.cyclic = cyclic;

        paths = new Path[numIntervals];
        travelTimes = new int[numIntervals];
    }


    // public void setPath(int departTime, Path path, double toLinkTravelTime)
    // {
    // int idx = departTime / timeInterval;
    // paths[idx] = path;
    // travelTimes[idx] = (int) (path.travelTime + toLinkTravelTime);
    // }
    //

    private int getIdx(int departTime)
    {
        int idx = (departTime / timeInterval);
        return cyclic ? (idx % intervalCoutn) : idx;
    }


    public Path getPath(int departTime)
    {
        return paths[getIdx(departTime)];
    }


    public int getTravelTime(int departTime)
    {
        return travelTimes[getIdx(departTime)];
    }


    public int getIntervalCount()
    {
        return intervalCoutn;
    }


    public int getTimeInterval()
    {
        return timeInterval;
    }


    public boolean isCyclic()
    {
        return cyclic;
    }
}
