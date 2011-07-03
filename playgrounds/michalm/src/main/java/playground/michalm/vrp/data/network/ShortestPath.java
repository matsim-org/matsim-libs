package playground.michalm.vrp.data.network;

import org.matsim.api.core.v01.*;


public class ShortestPath
{
    public static class SPEntry
    {
        public final int travelTime;
        public final double travelCost;
        public final Id[] linkIds;


        public SPEntry(int travelTime, double travelCost, Id[] linkIds)
        {
            this.travelTime = travelTime;
            this.travelCost = travelCost;
            this.linkIds = linkIds;
        }
    }

//    public static final ShortestPath NO_SHORTEST_PATH = new ShortestPath(0, 0, false);

    private int timeInterval;
    private int intervalCoutn;
    private boolean cyclic;

    SPEntry entries[];


    public ShortestPath(int numIntervals, int timeInterval, boolean cyclic)
    {
        this.timeInterval = timeInterval;
        this.intervalCoutn = numIntervals;
        this.cyclic = cyclic;

        entries = new SPEntry[numIntervals];
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


    public SPEntry getSPEntry(int departTime)
    {
        return entries[getIdx(departTime)];
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
