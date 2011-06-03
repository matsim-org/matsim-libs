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


public class ShortestPathsFinder
{
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
                        sPath_AB.paths[k] = path;

                        double toLinkTravelTime = travelTime.getLinkTravelTime(toLink, departTime);
                        sPath_AB.travelTimes[k] = (int) (path.travelTime + toLinkTravelTime);
                    }
                }
                else {
                    for (int k = 0; k < numSlots; k++) {
                        // int departTime = k * travelTimeBinSize;
                        sPath_AB.paths[k] = ShortestPath.ZERO_PATH;

                        sPath_AB.travelTimes[k] = 0;

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

                Path[] paths = sPath_ij.paths;

                for (int k = 0; k < numSlots; k++) {
                    Path path = paths[k];
                    timesBW.write(sPath_ij.travelTimes[k] + "\t");
                    costsBW.write(path.travelCost + "\t");

                    if (path == ShortestPath.ZERO_PATH) {
                        pathsBW.write("-1\t");
                        continue;
                    }

                    pathsBW.write(path.nodes.get(0).getId() + "\t");// first node
                    pathsBW.write(path.links.size() + "\t");// number of links

                    for (Link link : path.links) {
                        pathsBW.write(link.getId() + "\t");// each link
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
            return new BufferedReader(
                    new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
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

        // only in "readRoutes" mode
        Network network = readPaths ? data.getScenario().getNetwork() : null;

        ShortestPath[][] shortestPaths = (ShortestPath[][])Array.newInstance(ShortestPath.class, n,
                n);
        data.setShortestPaths(shortestPaths);

        BufferedReader timesBR = getReader(new File(timesFileName));
        BufferedReader costsBR = getReader(new File(costsFileName));
        BufferedReader pathsBR = readPaths ? getReader(new File(pathsFileName))
                : null;

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

                Path[] paths = sPath_ij.paths;

                for (int k = 0; k < numSlots; k++) {
                    List<Link> links = null;
                    List<Node> nodes = null;

                    double travelTime = timesTST.nextDouble();
                    double travelCost = costsTST.nextDouble();

                    if (readPaths) {
                        Map<Id, ? extends Link> linkMap = network.getLinks();
                        Map<Id, ? extends Node> nodeMap = network.getNodes();
                        Scenario scenario = data.getScenario();

                        String nodeId = pathsTST.nextToken();

                        if (nodeId.equals("-1")) {
                            paths[k] = ShortestPath.ZERO_PATH;
                            continue;
                        }

                        Node firstNode = nodeMap.get(scenario.createId(nodeId));
                        int linkCount = pathsTST.nextInt();
                        links = new ArrayList<Link>(linkCount);
                        nodes = new ArrayList<Node>(linkCount + 1);
                        nodes.add(firstNode);

                        for (int l = 0; l < linkCount; l++) {
                            Id id = scenario.createId(pathsTST.nextToken());
                            Link link = linkMap.get(id);

                            if (link == null) {
                                System.out.println("i=" + i + " j=" + j + " k=" + k);
                                System.out.println(id);
                            }

                            links.add(link);
                            nodes.add(link.getToNode());
                        }
                    }

                    paths[k] = new Path(nodes, links, travelTime, travelCost);
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
                Path[] paths = sPath_ij.paths;

                int[] timesOnDeparture = sPath_ij.travelTimes;
                double[] costsOnDeparture = new double[numSlots];

                for (int k = 0; k < numSlots; k++) {
                    costsOnDeparture[k] = paths[k].travelCost;
                }

                graph.setTime(vA, vB, new InterpolatedArcTime(timesOnDeparture, travelTimeBinSize,
                        true));
                graph.setCost(vA, vB, new InterpolatedArcCost(costsOnDeparture, travelTimeBinSize,
                        true));
            }
        }

        log.info("upadateVRPArcTimesAndCosts() ==== FINISHED ====");
    }
}
