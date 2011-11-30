package playground.michalm.vrp.data.network.shortestpath.full;

import playground.michalm.vrp.data.network.shortestpath.*;


public class FullShortestPath
    implements ShortestPath
{
    private int timeInterval;
    private int intervalCoutn;
    private boolean cyclic;

    SPEntry entries[];


    public FullShortestPath(int numIntervals, int timeInterval, boolean cyclic)
    {
        this.timeInterval = timeInterval;
        this.intervalCoutn = numIntervals;
        this.cyclic = cyclic;

        entries = new SPEntry[numIntervals];
    }


    private int getIdx(int departTime)
    {
        int idx = (departTime / timeInterval);
        return cyclic ? (idx % intervalCoutn) : idx;
    }


    @Override
    public SPEntry getSPEntry(int departTime)
    {
        return entries[getIdx(departTime)];
    }
}
