package playground.michalm.vrp.data.network.shortestpath.sparse;

import java.lang.reflect.*;
import java.util.*;

import org.matsim.core.router.util.*;

import pl.poznan.put.vrp.dynamic.data.network.*;
import playground.michalm.vrp.data.*;
import playground.michalm.vrp.data.network.*;
import playground.michalm.vrp.data.network.shortestpath.*;


public class SparseShortestPathFinder
{
    public final int travelTimeBinSize;// in seconds
    public final int numSlots;
    private MATSimVRPData data;


    public SparseShortestPathFinder(MATSimVRPData data)
    {
        this.data = data;
        travelTimeBinSize = 15 * 60;// 15 minutes
        numSlots = 24 * 4;// 24 hours split into quarters
    }


    public SparseShortestPathFinder(MATSimVRPData data, int travelTimeBinSize, int numSlots)
    {
        this.data = data;
        this.travelTimeBinSize = travelTimeBinSize;
        this.numSlots = numSlots;
    }


    // TODO: maybe map: (n x n) => SP ?????
    public ShortestPathToArcTimeArcCostAdapter[][] findShortestPaths(TravelTime travelTime,
            TravelDisutility travelCost, LeastCostPathCalculator router)
    {
        MATSimVRPGraph graph = data.getVrpGraph();
        int n = graph.getVertexCount();

        ShortestPathToArcTimeArcCostAdapter[][] shortestPaths = (ShortestPathToArcTimeArcCostAdapter[][])Array
                .newInstance(ShortestPathToArcTimeArcCostAdapter.class, n, n);

        graph.setShortestPaths(shortestPaths);

        List<Vertex> vertices = graph.getVertices();

        for (Vertex a : vertices) {
            MATSimVertex vA = (MATSimVertex)a;

            ShortestPathToArcTimeArcCostAdapter[] sPath_A = shortestPaths[vA.getId()];

            for (Vertex b : vertices) {
                MATSimVertex vB = (MATSimVertex)b;

                ShortestPathToArcTimeArcCostAdapter sPath_AB = new ShortestPathToArcTimeArcCostAdapter(
                        new SparseShortestPath(numSlots, travelTimeBinSize, true, router,
                                travelTime, travelCost, vA, vB));

                sPath_A[vB.getId()] = sPath_AB;
            }
        }

        return shortestPaths;
    }


    public void upadateVRPArcTimesAndCosts()
    {
        MATSimVRPGraph graph = data.getVrpGraph();

        ShortestPathToArcTimeArcCostAdapter[][] shortestPaths = (ShortestPathToArcTimeArcCostAdapter[][])graph
                .getShortestPaths();

        graph.setArcTimes(shortestPaths);
        graph.setArcCosts(shortestPaths);
    }
}
