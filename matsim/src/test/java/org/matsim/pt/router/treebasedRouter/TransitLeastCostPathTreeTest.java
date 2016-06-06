package org.matsim.pt.router.treebasedRouter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.router.TransitLeastCostPathTree;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.testcases.MatsimTestUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Junit Test for the TransitLeastCostPathTree.
 *
 * @author gabriel.thunig on 23.05.2016.
 */
public class TransitLeastCostPathTreeTest {

    private TransitRouterNetwork network;
    private TransitRouterNetworkTravelTimeAndDisutility travelDisutility;

    /**
     * Gets executed before each Method.
     * Instantiates a new TransitLeastCostPathTree object with a sample transitSchedule and default configuration.
     */
    @Before
    public void instantiateNetworkAndTravelDisutility() {
        String transitScheduleFile = "examples/pt-tutorial/transitschedule.xml";

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.loadScenario(config);
        scenario.getConfig().transit().setUseTransit(true);
        TransitScheduleReader reader = new TransitScheduleReader(scenario);
        TransitRouterConfig transitRouterConfig = new TransitRouterConfig(scenario.getConfig());
        reader.readFile(transitScheduleFile);
        TransitSchedule transitSchedule = scenario.getTransitSchedule();
        network = TransitRouterNetwork.createFromSchedule(transitSchedule, transitRouterConfig.getBeelineWalkConnectionDistance());

        PreparedTransitSchedule preparedTransitSchedule = new PreparedTransitSchedule(transitSchedule);
        travelDisutility = new TransitRouterNetworkTravelTimeAndDisutility(transitRouterConfig, preparedTransitSchedule);
    }

    /**
     * Check whether the @Before-instantiation is instantiating a network and a travelDisutility.
     */
    @Test
    public void TestNetworkAndTravelDisutilityInstantiated() {
        Assert.assertNotNull(network);
        Assert.assertNotNull(travelDisutility);
    }

    /**
     * Get the very next transitNode.
     * @param person Which person we are routing for. For default leave null.
     * @param coord The origin of the tree.
     * @param departureTime The time the person departures at the origin.
     * @return the next transitNode.
     */
    private Map<Node, TransitLeastCostPathTree.InitialNode> locateWrappedNearestTransitNode(Person person, Coord coord, double departureTime) {
        TransitRouterNetwork.TransitRouterNetworkNode nearestNode = network.getNearestNode(coord);
        Map<Node, TransitLeastCostPathTree.InitialNode> wrappedNearestNodes = new LinkedHashMap<>();
        Coord toCoord = nearestNode.stop.getStopFacility().getCoord();
        double initialTime = travelDisutility.getTravelTime(person, coord, toCoord);
        double initialCost = travelDisutility.getTravelDisutility(person, coord, toCoord);
        wrappedNearestNodes.put(nearestNode, new TransitLeastCostPathTree.InitialNode(initialCost, initialTime + departureTime));
        return wrappedNearestNodes;
    }

    /**
     * Try to route a standard connection.
     */
    @Test
    public void TestValidRouting() {
        Coord fromCoord = new Coord(1050d, 1050d);
        Map<Node, TransitLeastCostPathTree.InitialNode> wrappedFromNodes = this.locateWrappedNearestTransitNode(null, fromCoord, 28800);
		TransitLeastCostPathTree tree = new TransitLeastCostPathTree(network, travelDisutility, travelDisutility, wrappedFromNodes, null);
        Coord toCoord = new Coord(3950d, 1050d);
        Map<Node, TransitLeastCostPathTree.InitialNode> wrappedToNodes = this.locateWrappedNearestTransitNode(null, toCoord, 28800);
        Path path = tree.getPath(wrappedToNodes);
        Assert.assertNotNull(path);
        double pathCost = path.travelCost;
        Assert.assertEquals(1.8d, pathCost, MatsimTestUtils.EPSILON);
        double pathTime = path.travelTime;
        Assert.assertEquals(540d, pathTime, MatsimTestUtils.EPSILON);
    }

    /**
     * Try to route a connection with interchange.
     */
    @Test
    public void TestValidRoutingWithInterchange() {
        Coord fromCoord = new Coord(1050d, 1050d);
        Map<Node, TransitLeastCostPathTree.InitialNode> wrappedFromNodes = this.locateWrappedNearestTransitNode(null, fromCoord, 28800);
		TransitLeastCostPathTree tree = new TransitLeastCostPathTree(network, travelDisutility, travelDisutility, wrappedFromNodes, null);
        Coord toCoord = new Coord(2050d, 2960d);
        Map<Node, TransitLeastCostPathTree.InitialNode> wrappedToNodes = this.locateWrappedNearestTransitNode(null, toCoord, 28800);
        Path path = tree.getPath(wrappedToNodes);
        Assert.assertNotNull(path);
        double pathCost = path.travelCost;
        Assert.assertEquals(1.7706666666666668d, pathCost, MatsimTestUtils.EPSILON);
        double pathTime = path.travelTime;
        Assert.assertEquals(231.20000000000073d, pathTime, MatsimTestUtils.EPSILON);
    }

}
