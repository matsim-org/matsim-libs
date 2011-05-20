package playground.michalm.vrp.data.network;

import org.matsim.core.router.util.LeastCostPathCalculator.Path;


public class ShortestPath
{
    public static final Path ZERO_PATH = new Path(null, null, 0, 0);

    public static final ShortestPath NO_SHORTEST_PATH = new ShortestPath(0, 0, false);

    private int timeInterval;
    private int numIntervals;
    private boolean cyclic;

    // can be used to create NetworkRoute (in Leg).
    private Path[] paths;


    public Path[] getPaths()
    {
        return paths;
    }


    public ShortestPath(int numIntervals, int timeInterval, boolean cyclic)
    {
        this.timeInterval = timeInterval;
        this.numIntervals = numIntervals;
        this.cyclic = cyclic;

        paths = new Path[numIntervals];
    }


    public void setPath(int departTime, Path path)
    {
        paths[departTime / timeInterval] = path;
    }


    public Path getPath(int departTime)
    {
        int idx = (departTime / timeInterval);

        if (cyclic) {
            idx %= numIntervals;
        }

        return paths[idx];
    }


    public double getTravelTime(int departTime)
    {
        return getPath(departTime).travelTime;
    }
}
