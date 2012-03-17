package playground.michalm.vrp.data.network.shortestpath.full;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.zip.*;

import org.apache.log4j.*;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.router.util.*;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import pl.poznan.put.util.*;
import pl.poznan.put.vrp.dynamic.data.network.*;
import playground.michalm.vrp.data.*;
import playground.michalm.vrp.data.network.*;
import playground.michalm.vrp.data.network.router.*;
import playground.michalm.vrp.data.network.shortestpath.*;
import playground.michalm.vrp.data.network.shortestpath.ShortestPath.SPEntry;


public class FullShortestPathsFinder
{
    private final static Logger log = Logger.getLogger(FullShortestPathsFinder.class);

    private MATSimVRPData data;

    public final int travelTimeBinSize;// in seconds
    public final int numSlots;


    // public

    public FullShortestPathsFinder(MATSimVRPData data)
    {
        this.data = data;
        travelTimeBinSize = 15 * 60;// 15 minutes
        numSlots = 24 * 4;// 24 hours split into quarters
    }


    public FullShortestPathsFinder(MATSimVRPData data, int travelTimeBinSize, int numSlots)
    {
        this.data = data;
        this.travelTimeBinSize = travelTimeBinSize;
        this.numSlots = numSlots;
    }


    public void findShortestPaths(TravelTime travelTime,
            LeastCostPathCalculatorFactory leastCostPathCalculatorFactory)
    {
        log.info("findShortestPaths(Controler) ==== STARTED ====");

        Network network = data.getScenario().getNetwork();

        // created by: TravelTimeCalculatorFactoryImpl, setting from:
        // TravelTimeCalculatorConfigGroup
        // a. travelTimeCalculatorType: "TravelTimeCalculatorArray"
        // b. travelTimeAggregator: "optimistic"
        TravelDisutility travelCost = new TimeAsTravelCost(travelTime);

        LeastCostPathCalculator router = leastCostPathCalculatorFactory.createPathCalculator(
                network, travelCost, travelTime);

        MATSimVRPGraph graph = data.getVrpGraph();
        int n = graph.getVertexCount();

        FullShortestPath[][] shortestPaths = (FullShortestPath[][])Array.newInstance(
                FullShortestPath.class, n, n);
        graph.setShortestPaths(shortestPaths);

        List<Vertex> vertices = graph.getVertices();

        for (Vertex a : vertices) {
            MATSimVertex vA = (MATSimVertex)a;

            log.info("findShortestPaths ==== vertex: " + vA + " ====");
            FullShortestPath[] sPath_A = shortestPaths[vA.getId()];
            Link fromLink = vA.getLink();

            for (Vertex b : vertices) {
                MATSimVertex vB = (MATSimVertex)b;

                FullShortestPath sPath_AB = new FullShortestPath(numSlots, travelTimeBinSize, true);
                sPath_A[vB.getId()] = sPath_AB;

                Link toLink = vB.getLink();

                if (fromLink != toLink) {
                    for (int k = 0; k < numSlots; k++) {
                        int departTime = k * travelTimeBinSize;// + travelTimeBinSize/2 TODO
                        Path path = router.calcLeastCostPath(fromLink.getToNode(),
                                toLink.getFromNode(), departTime);

                        double time = path.travelTime;
                        double cost = path.travelCost;
                        int idCount = path.links.size() + 1;
                        Id[] ids = new Id[idCount];
                        int idxShift;

                        if (ShortestPath.INCLUDE_TO_LINK) {
                            time += travelTime.getLinkTravelTime(toLink, departTime);
                            cost += travelCost.getLinkTravelDisutility(toLink, time);
                            ids[idCount - 1] = toLink.getId();
                            idxShift = 0;
                        }
                        else {
                            time += travelTime.getLinkTravelTime(fromLink, departTime);
                            cost += travelCost.getLinkTravelDisutility(fromLink, time);
                            ids[0] = fromLink.getId();
                            idxShift = 1;
                        }

                        for (int idx = 0; idx < idCount - 1; idx++) {
                            ids[idx + idxShift] = path.links.get(idx).getId();
                        }

                        sPath_AB.entries[k] = new SPEntry((int)time, cost, ids);

                    }
                }
                else {
                    for (int k = 0; k < numSlots; k++) {
                        sPath_AB.entries[k] = ShortestPath.ZERO_PATH_ENTRY;
                    }
                }
            }
        }

        log.info("findShortestPaths(Controler) ==== FINISHED ====");
        // Check out "NetworkLegRouter" what one can make with Paths in order to build Routes
    }


    public void writeShortestPaths(String timesFileName, String costsFileName, String pathsFileName)
        throws IOException
    {
        log.info("writeShortestPaths() ==== STARTED ====");

        BufferedWriter timesBW = new BufferedWriter(new FileWriter(timesFileName));
        BufferedWriter costsBW = new BufferedWriter(new FileWriter(costsFileName));
        BufferedWriter pathsBW = new BufferedWriter(new FileWriter(pathsFileName));

        MATSimVRPGraph graph = data.getVrpGraph();
        int n = graph.getVertexCount();

        FullShortestPath[][] shortestPaths = (FullShortestPath[][])graph.getShortestPaths();

        for (int i = 0; i < n; i++) {
            FullShortestPath[] sPath_i = shortestPaths[i];

            for (int j = 0; j < n; j++) {
                FullShortestPath sPath_ij = sPath_i[j];
                timesBW.write(i + "->" + j + "\t");
                costsBW.write(i + "->" + j + "\t");
                pathsBW.write(i + "->" + j + "\t");

                SPEntry[] entries = sPath_ij.entries;

                for (int k = 0; k < numSlots; k++) {
                    SPEntry entry = entries[k];
                    timesBW.write(entry.travelTime + "\t");
                    costsBW.write(entry.travelCost + "\t");
                    pathsBW.write(entry.linkIds.length + "\t");// number of linkIds

                    for (Id id : entry.linkIds) {
                        pathsBW.write(id + "\t");// each linkId
                    }
                }

                timesBW.newLine();
                costsBW.newLine();
                pathsBW.newLine();
            }
        }

        timesBW.close();
        costsBW.close();
        pathsBW.close();

        log.info("writeShortestPaths() ==== FINISHED ====");
    }


    public void readShortestPaths(String timesFileName, String costsFileName)
        throws IOException
    {
        readShortestPaths(timesFileName, costsFileName, null);
    }


    private BufferedReader getReader(File file)
        throws IOException
    {
        if (file.getName().endsWith(".gz")) {
            return new BufferedReader(new InputStreamReader(new GZIPInputStream(
                    new FileInputStream(file))));
        }
        else {
            return new BufferedReader(new FileReader(file));
        }
    }


    public void readShortestPaths(String timesFileName, String costsFileName, String pathsFileName)
        throws IOException
    {
        log.info("readShortestPaths() ==== STARTED ====");

        MATSimVRPGraph graph = data.getVrpGraph();
        int n = graph.getVertexCount();
        boolean readPaths = pathsFileName != null;

        FullShortestPath[][] shortestPaths = (FullShortestPath[][])Array.newInstance(
                FullShortestPath.class, n, n);
        graph.setShortestPaths(shortestPaths);

        BufferedReader timesBR = getReader(new File(timesFileName));
        BufferedReader costsBR = getReader(new File(costsFileName));
        BufferedReader pathsBR = readPaths ? getReader(new File(pathsFileName)) : null;

        for (int i = 0; i < n; i++) {
            FullShortestPath[] sPath_i = shortestPaths[i];

            for (int j = 0; j < n; j++) {
                TypedStringTokenizer timesTST = new TypedStringTokenizer(timesBR.readLine(), "\t");
                TypedStringTokenizer costsTST = new TypedStringTokenizer(costsBR.readLine(), "\t");
                TypedStringTokenizer pathsTST = readPaths ? new TypedStringTokenizer(
                        pathsBR.readLine(), "\t") : null;

                timesTST.nextToken();// line beginning
                costsTST.nextToken();// line beginning

                if (readPaths) {
                    pathsTST.nextToken();// line beginning
                }

                FullShortestPath sPath_ij = new FullShortestPath(numSlots, travelTimeBinSize, true);
                sPath_i[j] = sPath_ij;

                SPEntry[] entries = sPath_ij.entries;

                for (int k = 0; k < numSlots; k++) {
                    double travelTime = timesTST.nextDouble();
                    double travelCost = costsTST.nextDouble();
                    Id[] linkIds = null;

                    if (readPaths) {
                        int linkIdCount = pathsTST.nextInt();

                        if (linkIdCount == 0) {
                            entries[k] = ShortestPath.ZERO_PATH_ENTRY;
                            continue;
                        }

                        Scenario scenario = data.getScenario();
                        linkIds = new Id[linkIdCount];

                        for (int l = 0; l < linkIdCount; l++) {
                            linkIds[l] = scenario.createId(pathsTST.nextToken());
                        }
                    }

                    entries[k] = new SPEntry((int)travelTime, travelCost, linkIds);
                }
            }
        }

        timesBR.close();
        costsBR.close();

        if (readPaths) {
            pathsBR.close();
        }

        log.info("readShortestPaths() ==== FINISHED ====");
    }


    /**
     * Updates travel times and costs
     */
    public void upadateVRPArcTimesAndCosts()
    {
        log.info("upadateVRPArcTimesAndCosts() ==== STARTED ====");
        MATSimVRPGraph graph = data.getVrpGraph();
        List<Vertex> vertices = graph.getVertices();

        ShortestPath[][] shortestPaths = graph.getShortestPaths();

        for (Vertex vA : vertices) {
            ShortestPath[] sPath_i = shortestPaths[vA.getId()];

            for (Vertex vB : vertices) {
                FullShortestPath sPath_ij = (FullShortestPath)sPath_i[vB.getId()];
                SPEntry[] entries = sPath_ij.entries;

                int[] timesOnDeparture = new int[numSlots];
                double[] costsOnDeparture = new double[numSlots];

                for (int k = 0; k < numSlots; k++) {
                    timesOnDeparture[k] = entries[k].travelTime;
                    costsOnDeparture[k] = entries[k].travelCost;
                }

                graph.setArcTime(vA, vB, new InterpolatedArcTime(timesOnDeparture,
                        travelTimeBinSize, true));
                graph.setArcCost(vA, vB, new InterpolatedArcCost(costsOnDeparture,
                        travelTimeBinSize, true));
            }
        }

        log.info("upadateVRPArcTimesAndCosts() ==== FINISHED ====");
    }
}
