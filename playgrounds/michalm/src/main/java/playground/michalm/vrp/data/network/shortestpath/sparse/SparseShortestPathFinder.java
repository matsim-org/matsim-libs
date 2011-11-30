package playground.michalm.vrp.data.network.shortestpath.sparse;

import java.lang.reflect.*;
import java.util.*;

import org.matsim.api.core.v01.network.*;
import org.matsim.core.router.util.*;

import pl.poznan.put.vrp.dynamic.data.network.*;
import playground.michalm.vrp.data.*;
import playground.michalm.vrp.data.network.*;
import playground.michalm.vrp.data.network.router.*;
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
    public ShortestPathAsArcTimeAndCost[][] findShortestPaths(TravelTime travelTime,
            LeastCostPathCalculatorFactory leastCostPathCalculatorFactory)
    {
        Network network = data.getScenario().getNetwork();

        // created by: TravelTimeCalculatorFactoryImpl, setting from:
        // TravelTimeCalculatorConfigGroup
        // a. travelTimeCalculatorType: "TravelTimeCalculatorArray"
        // b. travelTimeAggregator: "optimistic"
        TravelCost travelCost = new TimeAsTravelCost(travelTime);

        LeastCostPathCalculator router = leastCostPathCalculatorFactory.createPathCalculator(
                network, travelCost, travelTime);

        MATSimVRPGraph graph = data.getVrpGraph();
        int n = graph.getVertexCount();

        ShortestPathAsArcTimeAndCost[][] shortestPaths = (ShortestPathAsArcTimeAndCost[][])Array
                .newInstance(ShortestPathAsArcTimeAndCost.class, n, n);

        graph.setShortestPaths(shortestPaths);

        List<Vertex> vertices = graph.getVertices();

        for (Vertex a : vertices) {
            MATSimVertex vA = (MATSimVertex)a;

            ShortestPathAsArcTimeAndCost[] sPath_A = shortestPaths[vA.getId()];

            for (Vertex b : vertices) {
                MATSimVertex vB = (MATSimVertex)b;

                ShortestPathAsArcTimeAndCost sPath_AB = new ShortestPathAsArcTimeAndCost(
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

        ShortestPathAsArcTimeAndCost[][] shortestPaths = (ShortestPathAsArcTimeAndCost[][])graph
                .getShortestPaths();

        graph.setArcTimes(shortestPaths);
        graph.setArcCosts(shortestPaths);
    }
}
