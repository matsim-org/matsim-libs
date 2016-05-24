package playground.dziemke.accessibility.ptmatrix.TransitLeastCostPathRouting;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.*;
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

    private TransitLeastCostPathTree tree;
    private TransitRouterNetwork network;
    private TransitRouterNetworkTravelTimeAndDisutility travelDisutility;

    /**
     * Gets executed before each Method.
     * Instantiates a new TransitLeastCostPathTree object with a sample transitSchedule and default configuration.
     */
    @Before
    public void instantiateTree() {
        String transitScheduleFile = "../../matsim/examples/pt-tutorial/transitschedule.xml";

        Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup(), new MatrixBasedPtRouterConfigGroup());
        Scenario scenario = ScenarioUtils.loadScenario(config);
        scenario.getConfig().transit().setUseTransit(true);
        TransitScheduleReader reader = new TransitScheduleReader(scenario);
        TransitRouterConfig transitRouterConfig = new TransitRouterConfig(scenario.getConfig());
        reader.readFile(transitScheduleFile);
        TransitSchedule transitSchedule = scenario.getTransitSchedule();
        network = TransitRouterNetwork.createFromSchedule(transitSchedule, transitRouterConfig.getBeelineWalkConnectionDistance());

        PreparedTransitSchedule preparedTransitSchedule = new PreparedTransitSchedule(transitSchedule);
        travelDisutility = new TransitRouterNetworkTravelTimeAndDisutility(transitRouterConfig, preparedTransitSchedule);

        TransitTravelDisutility costFunction = travelDisutility;
        TravelTime timeFunction = travelDisutility;
        tree = new TransitLeastCostPathTree(network, costFunction, timeFunction);
    }

    /**
     * Check whether the @Before-instantiation is instantiating a TransitLeastCostPathTree-object.
     */
    @Test
    public void TestTreeInstantiated() {
        Assert.assertNotNull(tree);
    }

    /**
     * Create a tree and verify it.
     */
    @Test
    public void TestCreateLeastCostPathTree() {
        Coord fromCoord = new Coord(1050d, 1050d);
        Map<Node, TransitLeastCostPathTree.InitialNode> wrappedFromNodes = this.locateWrappedNearestTransitNode(null, fromCoord, 28800);
        tree.createLeastCostPathTree(wrappedFromNodes, null, fromCoord);
        Assert.assertEquals("Coords differ.", fromCoord, tree.getFromCoord());
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
     * Try to access a path without initially creating the tree.
     * Should throw a java.lang.NullPointerException.
     */
    @Test
    public void TestGetPathWithoutTree() {
        Coord toCoord = new Coord(3950d, 1050d);
        Map<Node, TransitLeastCostPathTree.InitialNode> wrappedToNodes = this.locateWrappedNearestTransitNode(null, toCoord, 28800);
        String exceptionMessage = "";
        try {
            tree.getPath(wrappedToNodes);
        } catch (Exception e) {
            exceptionMessage = e.toString();
        }
        Assert.assertEquals("java.lang.NullPointerException", exceptionMessage);
    }

    /**
     * Try to route a standard connection.
     */
    @Test
    public void TestValidRouting() {
        Coord fromCoord = new Coord(1050d, 1050d);
        Map<Node, TransitLeastCostPathTree.InitialNode> wrappedFromNodes = this.locateWrappedNearestTransitNode(null, fromCoord, 28800);
        tree.createLeastCostPathTree(wrappedFromNodes, null, fromCoord);
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
        tree.createLeastCostPathTree(wrappedFromNodes, null, fromCoord);
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
