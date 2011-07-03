package playground.michalm.vrp.data.network;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.zip.*;

import org.apache.log4j.*;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.controler.*;
import org.matsim.core.router.util.*;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import pl.poznan.put.util.*;
import pl.poznan.put.vrp.dynamic.data.network.*;
import playground.michalm.vrp.data.*;
import playground.michalm.vrp.data.network.ShortestPath.SPEntry;


public class ShortestPathsFinder
{
    // include toLink or fromLink in time/cost (depends on the way the qsim is implemented...)
    // by default: true (toLinks are included)
    private final static boolean INCLUDE_TO_LINK = true;
    
    //just for memory optimization
    private static SPEntry ZERO_PATH_ENTRY = new SPEntry(0, 0, new Id[0]);

    private final static Logger log = Logger.getLogger(ShortestPathsFinder.class);

    private MATSimVRPData data;

    public final int travelTimeBinSize;// in seconds
    public final int numSlots;


    // public

    public ShortestPathsFinder(MATSimVRPData data)
    {
        this.data = data;
        travelTimeBinSize = 15 * 60;// 15 minutes
        numSlots = 24 * 4;// 24 hours split into quarters
    }


    public ShortestPathsFinder(MATSimVRPData data, int travelTimeBinSize, int numSlots)
    {
        this.data = data;
        this.travelTimeBinSize = travelTimeBinSize;
        this.numSlots = numSlots;
    }


    public void findShortestPaths(Controler controler)
    {
        log.info("findShortestPaths(Controler) ==== STARTED ====");

        Network network = data.getScenario().getNetwork();

        // created by: TravelTimeCalculatorFactoryImpl, setting from:
        // TravelTimeCalculatorConfigGroup
        // a. travelTimeCalculatorType: "TravelTimeCalculatorArray"
        // b. travelTimeAggregator: "optimistic"
        TravelTime travelTime = controler.getTravelTimeCalculator();
        TravelCost travelCost = new TimeAsTravelCost(travelTime);

        LeastCostPathCalculator router = controler.getLeastCostPathCalculatorFactory()
                .createPathCalculator(network, travelCost, travelTime);

        VRPGraph graph = data.getVrpData().getVrpGraph();
        int n = graph.getVertexCount();

        ShortestPath[][] shortestPaths = (ShortestPath[][])Array.newInstance(ShortestPath.class, n,
                n);
        data.setShortestPaths(shortestPaths);

        List<Vertex> vertices = graph.getVertices();

        for (Vertex a : vertices) {
            MATSimVertex vA = (MATSimVertex)a;

            log.info("findShortestPaths ==== vertex: " + vA + " ====");
            ShortestPath[] sPath_A = shortestPaths[vA.getId()];
            Link fromLink = vA.getLink();

            for (Vertex b : vertices) {
                MATSimVertex vB = (MATSimVertex)b;

                ShortestPath sPath_AB = sPath_A[vB.getId()] = new ShortestPath(numSlots,
                        travelTimeBinSize, true);

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

                        if (INCLUDE_TO_LINK) {
                            time += travelTime.getLinkTravelTime(toLink, departTime);
                            cost += travelCost.getLinkGeneralizedTravelCost(toLink, time);
                            ids[idCount - 1] = toLink.getId();
                            idxShift = 0;
                        }
                        else {
                            time += travelTime.getLinkTravelTime(fromLink, departTime);
                            cost += travelCost.getLinkGeneralizedTravelCost(fromLink, time);
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
                        sPath_AB.entries[k] = ZERO_PATH_ENTRY;
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

        VRPGraph graph = data.getVrpData().getVrpGraph();
        int n = graph.getVertexCount();

        ShortestPath[][] shortestPaths = data.getShortestPaths();

        for (int i = 0; i < n; i++) {
            ShortestPath[] sPath_i = shortestPaths[i];

            for (int j = 0; j < n; j++) {
                ShortestPath sPath_ij = sPath_i[j];
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

        VRPGraph graph = data.getVrpData().getVrpGraph();
        int n = graph.getVertexCount();
        boolean readPaths = pathsFileName != null;

        ShortestPath[][] shortestPaths = (ShortestPath[][])Array.newInstance(ShortestPath.class, n,
                n);
        data.setShortestPaths(shortestPaths);

        BufferedReader timesBR = getReader(new File(timesFileName));
        BufferedReader costsBR = getReader(new File(costsFileName));
        BufferedReader pathsBR = readPaths ? getReader(new File(pathsFileName)) : null;

        for (int i = 0; i < n; i++) {
            ShortestPath[] sPath_i = shortestPaths[i];

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

                ShortestPath sPath_ij = sPath_i[j] = new ShortestPath(numSlots, travelTimeBinSize,
                        true);

                SPEntry[] entries = sPath_ij.entries;

                for (int k = 0; k < numSlots; k++) {
                    double travelTime = timesTST.nextDouble();
                    double travelCost = costsTST.nextDouble();
                    Id[] linkIds = null;

                    if (readPaths) {
                        int linkIdCount = pathsTST.nextInt();

                        if (linkIdCount == 0) {
                            entries[k] = ZERO_PATH_ENTRY;
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
        VRPGraph graph = data.getVrpData().getVrpGraph();
        List<Vertex> vertices = graph.getVertices();

        ShortestPath[][] shortestPaths = data.getShortestPaths();

        for (Vertex vA : vertices) {
            ShortestPath[] sPath_i = shortestPaths[vA.getId()];

            for (Vertex vB : vertices) {
                ShortestPath sPath_ij = sPath_i[vB.getId()];
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
