
/* *********************************************************************** *
 * project: org.matsim.*
 * TransitLeastCostPathTreeTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

 package org.matsim.pt.router;

import static org.hamcrest.Matchers.greaterThan;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.InitialNode;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

/**
 * Junit Test for the TransitLeastCostPathTree.
 *
 * @author gabriel.thunig on 23.05.2016.
 */
public class TransitLeastCostPathTreeTest {

    private TransitRouterNetwork network;
    private TransitRouterNetworkTravelTimeAndDisutility travelDisutility;

    /**
     * Instantiates a new TransitLeastCostPathTree object with a sample transitSchedule and default configuration.
     */
    public void instantiateNetworkAndTravelDisutility() {
        String transitScheduleFile = "test/scenarios/pt-tutorial/transitschedule.xml";

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
        instantiateNetworkAndTravelDisutility();
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
    private Map<Node, InitialNode> locateWrappedNearestTransitNode(Person person, Coord coord, double departureTime) {
        TransitRouterNetwork.TransitRouterNetworkNode nearestNode = network.getNearestNode(coord);
        Map<Node, InitialNode> wrappedNearestNodes = new LinkedHashMap<>();
        Coord toCoord = nearestNode.stop.getStopFacility().getCoord();
        double initialTime = travelDisutility.getWalkTravelTime(person, coord, toCoord);
        double initialCost = travelDisutility.getWalkTravelDisutility(person, coord, toCoord);
        wrappedNearestNodes.put(nearestNode, new InitialNode(initialCost, initialTime + departureTime));
        return wrappedNearestNodes;
    }

    /**
     * Try to route a standard connection.
     */
    @Test
    public void TestValidRouting() {
        instantiateNetworkAndTravelDisutility();
        Coord fromCoord = new Coord(1050d, 1050d);
        Map<Node, InitialNode> wrappedFromNodes = this.locateWrappedNearestTransitNode(null, fromCoord, 28800);
		TransitLeastCostPathTree tree = new TransitLeastCostPathTree(network, travelDisutility, travelDisutility, wrappedFromNodes, null);
        Coord toCoord = new Coord(3950d, 1050d);
        Map<Node, InitialNode> wrappedToNodes = this.locateWrappedNearestTransitNode(null, toCoord, 28800);
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
        instantiateNetworkAndTravelDisutility();
        Coord fromCoord = new Coord(1050d, 1050d);
        Map<Node, InitialNode> wrappedFromNodes = this.locateWrappedNearestTransitNode(null, fromCoord, 28800);
		TransitLeastCostPathTree tree = new TransitLeastCostPathTree(network, travelDisutility, travelDisutility, wrappedFromNodes, null);
        Coord toCoord = new Coord(2050d, 2960d);
        Map<Node, InitialNode> wrappedToNodes = this.locateWrappedNearestTransitNode(null, toCoord, 28800);
        Path path = tree.getPath(wrappedToNodes);
        Assert.assertNotNull(path);
        double pathCost = path.travelCost;
        Assert.assertEquals(1.7706666666666668d, pathCost, MatsimTestUtils.EPSILON);
        double pathTime = path.travelTime;
        Assert.assertEquals(231.20000000000073d, pathTime, MatsimTestUtils.EPSILON);
    }

    @Ignore
    @Test
    public void TestSpeedImprovementOnStopCriterion() {
        Fixture f = new Fixture();
        TestTimeCost tc = new TestTimeCost();
        for (int i = 0; i < f.count-1; i++) {
            tc.setData(Id.create(i, Link.class), 1.0, 1.0);
        }
        Map<Node, InitialNode> fromNodes = new HashMap<>();
        fromNodes.put(f.network.getNodes().get(Id.create(0, Node.class)), new InitialNode(0.0, 0.0));
        Map<Node, InitialNode> toNodes = new HashMap<>();
        toNodes.put(f.network.getNodes().get(Id.create(1, Node.class)), new InitialNode(0.0, 0.0));
        for (Node node : fromNodes.keySet()) {
            System.out.println("From Node = " + node.getCoord());
        }
        for (Node node : toNodes.keySet()) {
            System.out.println("To Node = " + node.getCoord());
        }
        long startTime = System.currentTimeMillis();
        new TransitLeastCostPathTree(f.network, tc, tc, fromNodes, null);
        long endTime = System.currentTimeMillis();
        long elapsedTimeWithoutStopCreterion = (endTime - startTime);
        startTime = System.currentTimeMillis();
        new TransitLeastCostPathTree(f.network, tc, tc, fromNodes, toNodes, null);
        endTime = System.currentTimeMillis();
        long elapsedTimeWithStopCreterion = (endTime - startTime);
        Double bareRatio = (double)elapsedTimeWithoutStopCreterion / (double)elapsedTimeWithStopCreterion;
        System.out.println("bareRatio = " + bareRatio);
        int ratio = (int) ((bareRatio) * 10);
        System.out.println("Time without stop criterion = " + elapsedTimeWithoutStopCreterion);
        System.out.println("Time with stop criterion = " + elapsedTimeWithStopCreterion);
        System.out.println("ratio = " + ratio);
        Assert.assertThat("Bad ratio",
                ratio,
                greaterThan(15));
    }

    /**
     * Creates a simple network to be used in TestSpeedImprovementOnStopCriterion.
     *
     * <pre>
     *   (0)--(1)--(2)--...--(9997)--(9998)--(9999)
     * </pre>
     *
     * @author gthunig
     */
	/*package*/ static class Fixture {
        /*package*/ Network network;

        int count = 10000;

        public Fixture() {
            this.network = NetworkUtils.createNetwork();
            Node linkStartNode = NetworkUtils.createAndAddNode(this.network, Id.create(0, Node.class), new Coord((double) 0, (double) 0));
            for (int i = 1; i < count; i++) {
                Node linkEndNode = NetworkUtils.createAndAddNode(this.network, Id.create(i, Node.class), new Coord((double) i, (double) 0));
		final Node fromNode = linkStartNode;
		final Node toNode = linkEndNode;
                NetworkUtils.createAndAddLink(this.network,Id.create(i-1, Link.class), fromNode, toNode, 1000.0, 10.0, 2000.0, (double) 1 );
                linkStartNode = linkEndNode;
            }

        }
    }

    /*package*/ static class TestTimeCost implements TravelTime, TransitTravelDisutility {

        private final Map<Id<Link>, Double> travelTimes = new HashMap<>();
        private final Map<Id<Link>, Double> travelCosts = new HashMap<>();

        public void setData(final Id<Link> id, final double travelTime, final double travelCost) {
            this.travelTimes.put(id, travelTime);
            this.travelCosts.put(id, travelCost);
        }

        @Override
        public double getLinkTravelTime(final Link link, final double time, Person person, Vehicle vehicle) {
            return this.travelTimes.get(link.getId());
        }

        @Override
        public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle, final CustomDataManager dataManager) {
            return this.travelCosts.get(link.getId());
        }

        @Override
        public double getWalkTravelTime(Person person, Coord coord, Coord toCoord) {
            return 0;
        }

        @Override
        public double getWalkTravelDisutility(Person person, Coord coord, Coord toCoord) {
            return 0;
        }

    }
}
