package playground.michalm.vrp.data.network;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import playground.michalm.vrp.data.*;

import org.apache.log4j.*;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.controler.*;
import org.matsim.core.router.util.*;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import pl.poznan.put.util.*;
import pl.poznan.put.vrp.dynamic.data.network.*;


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

        Network network = data.scenario.getNetwork();

        // created by: TravelTimeCalculatorFactoryImpl, setting from:
        // TravelTimeCalculatorConfigGroup
        // a. travelTimeCalculatorType: "TravelTimeCalculatorArray"
        // b. travelTimeAggregator: "optimistic"
        TravelTime travelTime = controler.getTravelTimeCalculator();
        TravelCost travelCost = new TimeAsTravelCost(travelTime);

        LeastCostPathCalculator router = controler.getLeastCostPathCalculatorFactory()
                .createPathCalculator(network, travelCost, travelTime);

        int n = data.vrpData.nodes.length;
        ShortestPath[][] shortestPaths = data.shortestPaths = (ShortestPath[][])Array.newInstance(
                ShortestPath.class, n, n);

        for (int i = 0; i < n; i++) {
            log.info("findShortestPaths ==== node: " + i + " ====");
            Link fromLink = data.nodeToLinks[i];
            ShortestPath[] sPath_i = shortestPaths[i];

            for (int j = 0; j < n; j++) {
                Link toLink = data.nodeToLinks[j];

                ShortestPath sPath_ij = sPath_i[j] = new ShortestPath(numSlots, travelTimeBinSize,
                        true);

                if (fromLink != toLink) {
                    for (int k = 0; k < numSlots; k++) {
                        int departTime = k * travelTimeBinSize;// + travelTimeBinSize/2 TODO
                        Path path = router.calcLeastCostPath(fromLink.getToNode(),
                                toLink.getFromNode(), departTime);
                        sPath_ij.setPath(departTime, path);
                    }
                }
                else {
                    for (int k = 0; k < numSlots; k++) {
                        int departTime = k * travelTimeBinSize;
                        sPath_ij.setPath(departTime, ShortestPath.ZERO_PATH);
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

        int n = data.vrpData.nodes.length;
        ShortestPath[][] shortestPaths = data.shortestPaths;

        for (int i = 0; i < n; i++) {
            ShortestPath[] sPath_i = shortestPaths[i];

            for (int j = 0; j < n; j++) {
                ShortestPath sPath_ij = sPath_i[j];
                timesBW.write(i + "->" + j + "\t");
                costsBW.write(i + "->" + j + "\t");
                pathsBW.write(i + "->" + j + "\t");

                for (int k = 0; k < numSlots; k++) {
                    Path path = sPath_ij.paths[k];
                    timesBW.write(path.travelTime + "\t");
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


    public void readShortestPaths(String timesFileName, String costsFileName, String pathsFileName)
        throws IOException
    {
        log.info("readShortestPaths() ==== STARTED ====");

        int n = data.vrpData.nodes.length;
        boolean readPaths = pathsFileName != null;

        // only in "readRoutes" mode
        Network network = readPaths ? data.scenario.getNetwork() : null;

        ShortestPath[][] shortestPaths = data.shortestPaths = (ShortestPath[][])Array.newInstance(
                ShortestPath.class, n, n);

        BufferedReader timesBR = new BufferedReader(new FileReader(timesFileName));
        BufferedReader costsBR = new BufferedReader(new FileReader(costsFileName));
        BufferedReader pathsBR = readPaths ? new BufferedReader(new FileReader(pathsFileName))
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
                pathsTST.nextToken();// line beginning

                ShortestPath sPath_ij = sPath_i[j] = new ShortestPath(numSlots, travelTimeBinSize,
                        true);

                for (int k = 0; k < numSlots; k++) {
                    List<Link> links = null;
                    List<Node> nodes = null;

                    double travelTime = timesTST.nextDouble();
                    double travelCost = costsTST.nextDouble();

                    if (readPaths) {
                        Map<Id, ? extends Link> linkMap = network.getLinks();
                        Map<Id, ? extends Node> nodeMap = network.getNodes();
                        Scenario scenario = data.scenario;

                        String nodeId = pathsTST.nextToken();

                        if (nodeId.equals("-1")) {
                            sPath_ij.paths[k] = ShortestPath.ZERO_PATH;
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

                    sPath_ij.paths[k] = new Path(nodes, links, travelTime, travelCost);
                }
            }
        }

        timesBR.close();
        costsBR.close();
        pathsBR.close();

        log.info("readShortestPaths() ==== FINISHED ====");
    }


    /**
     * Updates travel times and costs
     */
    public void upadateVRPArcTimesAndCosts()
    {
        log.info("upadateVRPArcTimesAndCosts() ==== STARTED ====");
        int n = data.vrpData.nodes.length;

        InterpolatedArcTime[][] arcTimes = (InterpolatedArcTime[][])Array.newInstance(
                InterpolatedArcTime.class, n, n);
        InterpolatedArcCost[][] arcCosts = (InterpolatedArcCost[][])Array.newInstance(
                InterpolatedArcCost.class, n, n);
        ShortestPath[][] shortestPaths = data.shortestPaths;

        for (int i = 0; i < n; i++) {
            ShortestPath[] sPath_i = shortestPaths[i];
            InterpolatedArcTime[] arcTimes_i = arcTimes[i];
            InterpolatedArcCost[] arcCosts_i = arcCosts[i];

            for (int j = 0; j < n; j++) {
                ShortestPath sPath_ij = sPath_i[j];

                int[] timesOnDeparture = new int[numSlots];
                double[] costsOnDeparture = new double[numSlots];

                for (int k = 0; k < numSlots; k++) {
                    Path path = sPath_ij.paths[k];
                    timesOnDeparture[k] = (int)path.travelTime;
                    costsOnDeparture[k] = path.travelCost;
                }

                arcTimes_i[j] = new InterpolatedArcTime(timesOnDeparture, travelTimeBinSize, true);
                arcCosts_i[j] = new InterpolatedArcCost(costsOnDeparture, travelTimeBinSize, true);
            }
        }

        data.vrpData.times = arcTimes;
        data.vrpData.costs = arcCosts;

        log.info("upadateVRPArcTimesAndCosts() ==== FINISHED ====");
    }
}
