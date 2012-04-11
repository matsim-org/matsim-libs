package playground.michalm.vrp.data.network.shortestpath.sparse;

import java.lang.reflect.Array;
import java.util.List;

import org.matsim.core.router.util.*;

import pl.poznan.put.vrp.dynamic.data.network.Vertex;
import playground.michalm.vrp.data.MatsimVrpData;
import playground.michalm.vrp.data.network.*;


public class SparseShortestPathFinder
{
    final int TIME_BIN_SIZE;// in seconds
    final int NUM_SLOTS;
    final boolean CYCLIC = true;

    final private MatsimVrpData data;

    TravelTime travelTime;
    TravelDisutility travelCost;
    LeastCostPathCalculator router;


    public SparseShortestPathFinder(MatsimVrpData data)
    {
        this.data = data;
        TIME_BIN_SIZE = 15 * 60;// 15 minutes
        NUM_SLOTS = 24 * 4;// 24 hours split into quarters
    }


    public SparseShortestPathFinder(MatsimVrpData data, int timeBinSize, int numSlots)
    {
        this.data = data;
        this.TIME_BIN_SIZE = timeBinSize;
        this.NUM_SLOTS = numSlots;
    }


    // TODO: maybe map: (n x n) => SP ?????
    public SparseShortestPathArc[][] findShortestPaths(TravelTime travelTime,
            TravelDisutility travelCost, LeastCostPathCalculator router)
    {
        this.travelTime = travelTime;
        this.travelCost = travelCost;
        this.router = router;

        MatsimVrpGraph graph = data.getVrpGraph();
        int n = graph.getVertexCount();

        SparseShortestPathArc[][] shortestPaths = (SparseShortestPathArc[][])Array.newInstance(
                SparseShortestPathArc.class, n, n);

        List<Vertex> vertices = graph.getVertices();

        for (Vertex a : vertices) {
            MatsimVertex vA = (MatsimVertex)a;

            SparseShortestPathArc[] sPath_A = shortestPaths[vA.getId()];

            for (Vertex b : vertices) {
                MatsimVertex vB = (MatsimVertex)b;

                SparseShortestPathArc sPath_AB = new SparseShortestPathArc(new SparseShortestPath(
                        this, vA, vB));

                sPath_A[vB.getId()] = sPath_AB;
            }
        }

        return shortestPaths;
    }
}
