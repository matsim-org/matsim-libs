package playground.pieter.router;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.NetworkReaderMatsimV2;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.vehicles.Vehicle;
import playground.pieter.network.SimpleNetworkPainter;
import playground.pieter.singapore.utils.Sample;

import java.util.*;

public class DrunkDudePathfinder implements LeastCostPathCalculator {
    private final Network network;
    private final TravelDisutility travelCosts;
    private final TravelTime travelTimes;
    // sensitivity parameter, making it smaller makes it less sensitive to distance
    private final double beta;
    private SimpleNetworkPainter networkPainter;

    public DrunkDudePathfinder(Network network, TravelDisutility travelCosts,
                               TravelTime travelTimes, double beta) {
        super();
        this.network = network;
        this.travelCosts = travelCosts;
        this.travelTimes = travelTimes;
        this.beta = beta;
    }

    @Override
    public Path calcLeastCostPath(Node fromNode, Node toNode, double starttime,
                                  Person person, Vehicle vehicle) {


        List<Node> nodes = new ArrayList<>();
        List<Link> links = new ArrayList<>();
        double travelTime = starttime;
        double travelCost = 0;
        Node currentNode = fromNode;
        if (networkPainter != null) {
            networkPainter.addForegroundPixel(fromNode.getId());
            networkPainter.addForegroundPixel(toNode.getId());
        }
        while (currentNode != toNode) {
            Object[] outLinks = currentNode.getOutLinks().values().toArray();
            //come up witha set of costs based on how much nearer (eucl) the link brings you to your destination,
            // use them as probabilities of selecting the link
            double[] cumulativeWeights = new double[outLinks.length];
            double[] weights = new double[outLinks.length];
            double[] dists = new double[outLinks.length];
            double minCost = Double.POSITIVE_INFINITY;
            double maxCost = 0;
            Link selection = null;
            for (int i = 0; i < outLinks.length; i++) {
                double linkCost = CoordUtils.calcEuclideanDistance(toNode.getCoord(), ((Link) outLinks[i]).getToNode().getCoord());
                if (((Link) outLinks[i]).getToNode().equals(toNode)) { //found destination, stop mucking about
                    selection = (Link) outLinks[i];
                    break;
                }
                dists[i] = linkCost;
                minCost = Math.min(minCost, linkCost);
                maxCost = Math.max(maxCost, linkCost);
            }
            for (int i = 0; i < outLinks.length; i++) {
                double linkCost = dists[i] - minCost;
                if (links.contains(outLinks[i]) || nodes.contains(((Link) outLinks[i]).getToNode())) {
                    linkCost = 1e6 * (maxCost - minCost);
                }
                linkCost = Math.exp(-beta * linkCost);
                linkCost = linkCost == 1 ? 2 : linkCost;
                cumulativeWeights[i] = i > 0 ? cumulativeWeights[i - 1] + linkCost : linkCost;
                weights[i] = linkCost;
            }
            if (selection == null) {
                //sample if the destination has not been found
                double sampleProb = MatsimRandom.getRandom().nextDouble() * cumulativeWeights[cumulativeWeights.length - 1];
                for (int i = 0; i < outLinks.length; i++) {
                    if (cumulativeWeights[i] >= sampleProb) {
                        selection = (Link) outLinks[i];
                        links.add(selection);
                        nodes.add(currentNode);
                        travelCost += travelCosts.getLinkTravelDisutility(
                                selection, travelTime, person, vehicle);
                        travelTime += travelTimes.getLinkTravelTime(selection,
                                travelTime, person, vehicle);
                        break;
                    }
                }
            }
            if (networkPainter != null) {
                networkPainter.addForegroundLine(selection.getId());
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            currentNode = selection.getToNode();


        }
        if (networkPainter != null) {
            try {
                Thread.sleep(500);
                networkPainter.clearForeground();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return new Path(nodes, links, travelTime, travelCost);

    }

    public static void main(String[] args) {
        Scenario scenario;
        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new NetworkReaderMatsimV2(scenario.getNetwork())
                .readFile(args[0]);
        Network network = scenario.getNetwork();
        Network net = NetworkUtils.createNetwork();
        Set<String> modes = new HashSet<>();
        modes.add(TransportMode.car);
        new TransportModeNetworkFilter(network).filter(net, modes);
        new NetworkCleaner().run(net);
        MatsimRandom.reset(123);
        DrunkDudePathfinder stochasticRouter = new DrunkDudePathfinder(net,
                new TravelDisutility() {

                    @Override
                    public double getLinkTravelDisutility(Link link,
                                                          double time, Person person, Vehicle vehicle) {
                        return link.getLength() / link.getFreespeed();
                    }

                    @Override
                    public double getLinkMinimumTravelDisutility(Link link) {
                        return link.getLength() / link.getFreespeed();
                    }
                }, new TravelTime() {

            @Override
            public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
                return link.getLength() / link.getFreespeed();
            }
        }, 0.5);
        SimpleNetworkPainter networkPainter = new SimpleNetworkPainter(400, 400);
        networkPainter.setNetworkTransformation(network);
        stochasticRouter.setNetworkPainter(networkPainter);
//        stochasticRouter.calcLeastCostPath(network.getNodes().get(Id.createNodeId("11_311")),
//                network.getNodes().get(Id.createNodeId("18_566")), 3600, null, null);
        Set<Node> nodeSet = new HashSet<>();
        nodeSet.addAll(net.getNodes().values());
        Node[] nodesArray = nodeSet.toArray(new Node[nodeSet.size()]);

        long currentTimeMillis = -System.currentTimeMillis();
        for (int i = 0; i < 1e3; i++) {
            int[] ints = Sample.sampleMfromN(2, nodesArray.length);
            System.out.println(nodesArray[ints[0]] + " " + nodesArray[ints[1]]);
            stochasticRouter.calcLeastCostPath(nodesArray[ints[0]], nodesArray[ints[1]], 3600, null, null);
        }
        currentTimeMillis += System.currentTimeMillis();
        System.out.println(currentTimeMillis);

        Dijkstra dijkstra = new Dijkstra(net,
                new TravelDisutility() {

                    @Override
                    public double getLinkTravelDisutility(Link link,
                                                          double time, Person person, Vehicle vehicle) {
                        return link.getLength() / link.getFreespeed();
                    }

                    @Override
                    public double getLinkMinimumTravelDisutility(Link link) {
                        return link.getLength() / link.getFreespeed();
                    }
                }, new TravelTime() {

            @Override
            public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
                return link.getLength() / link.getFreespeed();
            }
        }, null);
        currentTimeMillis = -System.currentTimeMillis();
        for (int i = 0; i < 1e3; i++) {
            int[] ints = Sample.sampleMfromN(2, nodesArray.length);
            dijkstra.calcLeastCostPath(nodesArray[ints[0]], nodesArray[ints[1]], 3600, null, null);
        }
        currentTimeMillis += System.currentTimeMillis();
        System.out.println(currentTimeMillis);
    }

    public void setNetworkPainter(SimpleNetworkPainter networkPainter) {
        this.networkPainter = networkPainter;
    }
}
