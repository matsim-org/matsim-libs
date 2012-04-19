package playground.michalm.vrp.data.network.shortestpath.full;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import pl.poznan.put.util.TypedStringTokenizer;
import pl.poznan.put.vrp.dynamic.data.network.FixedSizeVrpGraph;
import pl.poznan.put.vrp.dynamic.data.network.Vertex;
import playground.michalm.vrp.data.MatsimVrpData;
import playground.michalm.vrp.data.network.MatsimVertex;
import playground.michalm.vrp.data.network.MatsimVrpGraph;
import playground.michalm.vrp.data.network.router.TimeAsTravelCost;
import playground.michalm.vrp.data.network.shortestpath.ShortestPath;
import playground.michalm.vrp.data.network.shortestpath.ShortestPath.SPEntry;


public class FullShortestPathsFinder
{
    private final static Logger log = Logger.getLogger(FullShortestPathsFinder.class);

    private MatsimVrpData data;

    final int TIME_BIN_SIZE;// in seconds
    final int NUM_SLOTS;
    final boolean CYCLIC = true;


    // public

    public FullShortestPathsFinder(MatsimVrpData data)
    {
        this.data = data;
        TIME_BIN_SIZE = 15 * 60;// 15 minutes
        NUM_SLOTS = 24 * 4;// 24 hours split into quarters
    }


    public FullShortestPathsFinder(MatsimVrpData data, int travelTimeBinSize, int numSlots)
    {
        this.data = data;
        this.TIME_BIN_SIZE = travelTimeBinSize;
        this.NUM_SLOTS = numSlots;
    }


    public FullShortestPath[][] findShortestPaths(TravelTime travelTime,
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

        MatsimVrpGraph graph = data.getVrpGraph();
        int n = graph.getVertexCount();

        FullShortestPath[][] shortestPaths = (FullShortestPath[][])Array.newInstance(
                FullShortestPath.class, n, n);

        List<Vertex> vertices = graph.getVertices();

        for (Vertex a : vertices) {
            MatsimVertex vA = (MatsimVertex)a;

            log.info("findShortestPaths ==== vertex: " + vA + " ====");
            FullShortestPath[] sPath_A = shortestPaths[vA.getId()];
            Link fromLink = vA.getLink();

            for (Vertex b : vertices) {
                MatsimVertex vB = (MatsimVertex)b;

                FullShortestPath sPath_AB = new FullShortestPath(NUM_SLOTS, TIME_BIN_SIZE, true);
                sPath_A[vB.getId()] = sPath_AB;

                Link toLink = vB.getLink();

                if (fromLink != toLink) {
                    for (int k = 0; k < NUM_SLOTS; k++) {
                        int departTime = k * TIME_BIN_SIZE;// + travelTimeBinSize/2 TODO
                        Path path = router.calcLeastCostPath(fromLink.getToNode(),
                                toLink.getFromNode(), departTime, null, null);

                        double time = path.travelTime;
                        double cost = path.travelCost;
                        int idCount = path.links.size() + 1;
                        Id[] ids = new Id[idCount];
                        int idxShift;

                        if (ShortestPath.INCLUDE_TO_LINK) {
                            time += travelTime.getLinkTravelTime(toLink, departTime);
                            cost += travelCost.getLinkTravelDisutility(toLink, time, null, null);
                            ids[idCount - 1] = toLink.getId();
                            idxShift = 0;
                        }
                        else {
                            time += travelTime.getLinkTravelTime(fromLink, departTime);
                            cost += travelCost.getLinkTravelDisutility(fromLink, time, null, null);
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
                    for (int k = 0; k < NUM_SLOTS; k++) {
                        sPath_AB.entries[k] = ShortestPath.ZERO_PATH_ENTRY;
                    }
                }
            }
        }

        log.info("findShortestPaths(Controler) ==== FINISHED ====");
        // Check out "NetworkLegRouter" what one can make with Paths in order to build Routes

        return shortestPaths;
    }


    public void writeShortestPaths(FullShortestPath[][] shortestPaths, String timesFileName,
            String costsFileName, String pathsFileName)
        throws IOException
    {
        log.info("writeShortestPaths() ==== STARTED ====");

        BufferedWriter timesBW = new BufferedWriter(new FileWriter(timesFileName));
        BufferedWriter costsBW = new BufferedWriter(new FileWriter(costsFileName));
        BufferedWriter pathsBW = new BufferedWriter(new FileWriter(pathsFileName));

        MatsimVrpGraph graph = data.getVrpGraph();
        int n = graph.getVertexCount();

        for (int i = 0; i < n; i++) {
            FullShortestPath[] sPath_i = shortestPaths[i];

            for (int j = 0; j < n; j++) {
                FullShortestPath sPath_ij = sPath_i[j];
                timesBW.write(i + "->" + j + "\t");
                costsBW.write(i + "->" + j + "\t");
                pathsBW.write(i + "->" + j + "\t");

                SPEntry[] entries = sPath_ij.entries;

                for (int k = 0; k < NUM_SLOTS; k++) {
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


    public FullShortestPath[][] readShortestPaths(String timesFileName, String costsFileName,
            String pathsFileName)
        throws IOException
    {
        log.info("readShortestPaths() ==== STARTED ====");

        MatsimVrpGraph graph = data.getVrpGraph();
        int n = graph.getVertexCount();
        boolean readPaths = pathsFileName != null;

        FullShortestPath[][] shortestPaths = (FullShortestPath[][])Array.newInstance(
                FullShortestPath.class, n, n);

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

                FullShortestPath sPath_ij = new FullShortestPath(NUM_SLOTS, TIME_BIN_SIZE, true);
                sPath_i[j] = sPath_ij;

                SPEntry[] entries = sPath_ij.entries;

                for (int k = 0; k < NUM_SLOTS; k++) {
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
        return shortestPaths;
    }


    /**
     * Updates travel times and costs
     */
    public void upadateVrpArcs(FullShortestPath[][] shortestPaths)
    {
        log.info("upadateVrpArcs() ==== STARTED ====");
        FixedSizeVrpGraph graph = (FixedSizeVrpGraph)data.getVrpGraph();
        List<Vertex> vertices = graph.getVertices();

        for (Vertex vA : vertices) {
            FullShortestPath[] sPath_i = shortestPaths[vA.getId()];

            for (Vertex vB : vertices) {
                graph.setArc(vA, vB,
                        FullShortestPathArc.createArc(sPath_i[vB.getId()], TIME_BIN_SIZE, true));
            }
        }

        log.info("upadateVrpArcs() ==== FINISHED ====");
    }
}
