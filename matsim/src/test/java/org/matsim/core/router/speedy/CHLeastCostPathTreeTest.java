/* *********************************************************************** *
 * project: org.matsim.*
 * CHLeastCostPathTreeTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
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

package org.matsim.core.router.speedy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.OptionalTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Tests for {@link CHLeastCostPathTree}: verifies that the CH-based
 * one-to-all search produces the same costs as the Dijkstra-based
 * {@link LeastCostPathTree}.
 *
 * @author Steffen Axer
 */
public class CHLeastCostPathTreeTest {

    private static final int    NUM_SOURCE_NODES = 20;
    private static final int    NUM_TARGET_NODES = 50;
    private static final double COST_TOLERANCE   = 1e-3;

    @Test
    void testForwardSearchSmallGrid() {
        Network network = buildGridNetwork(5);
        runForwardCorrectnessTest(network);
    }

    @Test
    void testForwardSearchLargerGrid() {
        Network network = buildGridNetwork(15);
        runForwardCorrectnessTest(network);
    }

    @Test
    void testForwardSearchEquilNetwork() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile("test/scenarios/equil/network.xml");
        runForwardCorrectnessTest(scenario.getNetwork());
    }

    @Test
    void testForwardSearchBerlinNetwork() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile("test/scenarios/berlin/network.xml.gz");
        runForwardCorrectnessTest(scenario.getNetwork());
    }

    @Test
    void testBackwardSearchSmallGrid() {
        Network network = buildGridNetwork(5);
        runBackwardCorrectnessTest(network);
    }

    @Test
    void testBackwardSearchLargerGrid() {
        Network network = buildGridNetwork(15);
        runBackwardCorrectnessTest(network);
    }

    @Test
    void testBackwardSearchBerlinNetwork() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile("test/scenarios/berlin/network.xml.gz");
        runBackwardCorrectnessTest(scenario.getNetwork());
    }

    // ---- forward search correctness ----

    private void runForwardCorrectnessTest(Network network) {
        FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(
                new ScoringConfigGroup());

        SpeedyGraph baseGraph = SpeedyGraphBuilder.build(network);

        // Build CH
        InertialFlowCutter.NDOrderResult orderResult =
                new InertialFlowCutter(baseGraph).computeOrderWithBatches();
        CHGraph chGraph = new CHBuilder(baseGraph, tc).buildWithOrderParallel(orderResult);
        new CHTTFCustomizer().customize(chGraph, tc, tc);

        // CH tree
        CHLeastCostPathTree chTree = new CHLeastCostPathTree(chGraph, tc, tc);

        // Reference Dijkstra tree
        LeastCostPathTree dijkstraTree = new LeastCostPathTree(baseGraph, tc, tc);

        List<Link> linkList = new ArrayList<>(network.getLinks().values());
        int n = linkList.size();
        if (n < 2) return;

        Random rng = new Random(42);
        int mismatches = 0;
        int comparisons = 0;

        for (int s = 0; s < NUM_SOURCE_NODES; s++) {
            Link srcLink = linkList.get(rng.nextInt(n));
            double startTime = 8.0 * 3600;

            // Run both trees from the same source
            chTree.calculate(srcLink, startTime, null, null);
            dijkstraTree.calculate(srcLink, startTime, null, null);

            // Compare travel times at random target nodes
            // (CH tree uses TTF-based travel time as cost, matching CHRouterTimeDep behavior)
            for (int t = 0; t < NUM_TARGET_NODES; t++) {
                Link tgtLink = linkList.get(rng.nextInt(n));
                int tgtNodeIdx = tgtLink.getFromNode().getId().index();

                OptionalTime chTimeOpt = chTree.getTime(tgtNodeIdx);
                OptionalTime dijTimeOpt = dijkstraTree.getTime(tgtNodeIdx);

                // Both should be defined or both undefined
                if (chTimeOpt.isUndefined() && dijTimeOpt.isUndefined()) {
                    comparisons++;
                    continue;
                }

                if (chTimeOpt.isUndefined() || dijTimeOpt.isUndefined()) {
                    if (dijTimeOpt.isDefined() && chTimeOpt.isUndefined()) {
                        mismatches++;
                        System.err.printf("FORWARD MISMATCH src=%s tgt=%s: CH=unreachable  Dijkstra=%.6f%n",
                                srcLink.getId(), tgtLink.getId(), dijTimeOpt.seconds());
                    }
                    comparisons++;
                    continue;
                }

                comparisons++;
                double chTime = chTimeOpt.seconds() - startTime;
                double dijTime = dijTimeOpt.seconds() - startTime;

                if (Math.abs(chTime - dijTime) > COST_TOLERANCE) {
                    mismatches++;
                    System.err.printf("FORWARD MISMATCH src=%s tgt=%s: CH_time=%.6f  Dijkstra_time=%.6f%n",
                            srcLink.getId(), tgtLink.getId(), chTime, dijTime);
                }
            }
        }

        Assertions.assertEquals(0, mismatches,
                mismatches + " forward cost mismatches out of " + comparisons
                        + " comparisons with CH LeastCostPathTree.");
    }

    // ---- backward search correctness ----

    private void runBackwardCorrectnessTest(Network network) {
        FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(
                new ScoringConfigGroup());

        SpeedyGraph baseGraph = SpeedyGraphBuilder.build(network);

        InertialFlowCutter.NDOrderResult orderResult =
                new InertialFlowCutter(baseGraph).computeOrderWithBatches();
        CHGraph chGraph = new CHBuilder(baseGraph, tc).buildWithOrderParallel(orderResult);
        new CHTTFCustomizer().customize(chGraph, tc, tc);

        CHLeastCostPathTree chTree = new CHLeastCostPathTree(chGraph, tc, tc);
        LeastCostPathTree dijkstraTree = new LeastCostPathTree(baseGraph, tc, tc);

        List<Link> linkList = new ArrayList<>(network.getLinks().values());
        int n = linkList.size();
        if (n < 2) return;

        Random rng = new Random(42);
        int mismatches = 0;
        int comparisons = 0;

        for (int s = 0; s < NUM_SOURCE_NODES; s++) {
            Link arrivalLink = linkList.get(rng.nextInt(n));
            double arrivalTime = 8.0 * 3600;

            chTree.calculateBackwards(arrivalLink, arrivalTime, null, null);
            dijkstraTree.calculateBackwards(arrivalLink, arrivalTime, null, null);

            for (int t = 0; t < NUM_TARGET_NODES; t++) {
                Link srcLink = linkList.get(rng.nextInt(n));
                int srcNodeIdx = srcLink.getToNode().getId().index();

                OptionalTime chTimeOpt = chTree.getTime(srcNodeIdx);
                OptionalTime dijTimeOpt = dijkstraTree.getTime(srcNodeIdx);

                if (chTimeOpt.isUndefined() && dijTimeOpt.isUndefined()) {
                    comparisons++;
                    continue;
                }

                if (chTimeOpt.isUndefined() || dijTimeOpt.isUndefined()) {
                    if (dijTimeOpt.isDefined() && chTimeOpt.isUndefined()) {
                        mismatches++;
                        System.err.printf("BACKWARD MISMATCH arrival=%s src=%s: CH=unreachable  Dijkstra=%.6f%n",
                                arrivalLink.getId(), srcLink.getId(), dijTimeOpt.seconds());
                    }
                    comparisons++;
                    continue;
                }

                comparisons++;
                double chTime = arrivalTime - chTimeOpt.seconds();
                double dijTime = arrivalTime - dijTimeOpt.seconds();

                if (Math.abs(chTime - dijTime) > COST_TOLERANCE) {
                    mismatches++;
                    System.err.printf("BACKWARD MISMATCH arrival=%s src=%s: CH_time=%.6f  Dijkstra_time=%.6f%n",
                            arrivalLink.getId(), srcLink.getId(), chTime, dijTime);
                }
            }
        }

        Assertions.assertEquals(0, mismatches,
                mismatches + " backward cost mismatches out of " + comparisons
                        + " comparisons with CH LeastCostPathTree.");
    }

    // ---- network builders ----

    private static Network buildGridNetwork(int size) {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Network network = scenario.getNetwork();
        NetworkFactory nf = network.getFactory();

        Node[][] nodes = new Node[size][size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                String id = x + "_" + y;
                nodes[x][y] = nf.createNode(Id.createNodeId(id), new Coord(x * 1000, y * 1000));
                network.addNode(nodes[x][y]);
            }
        }

        int linkId = 0;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (x + 1 < size) {
                    addBidirectionalLink(network, nf, linkId++, nodes[x][y], nodes[x + 1][y]);
                    linkId++;
                }
                if (y + 1 < size) {
                    addBidirectionalLink(network, nf, linkId++, nodes[x][y], nodes[x][y + 1]);
                    linkId++;
                }
            }
        }

        return network;
    }

    private static void addBidirectionalLink(Network network, NetworkFactory nf,
                                              int baseLinkId, Node from, Node to) {
        double length = Math.sqrt(
                Math.pow(from.getCoord().getX() - to.getCoord().getX(), 2) +
                Math.pow(from.getCoord().getY() - to.getCoord().getY(), 2));
        Link fwd = nf.createLink(Id.createLinkId(baseLinkId), from, to);
        fwd.setLength(length);
        fwd.setFreespeed(13.89);
        fwd.setCapacity(1000);
        fwd.setNumberOfLanes(1);
        network.addLink(fwd);

        Link bwd = nf.createLink(Id.createLinkId(baseLinkId + 1), to, from);
        bwd.setLength(length);
        bwd.setFreespeed(13.89);
        bwd.setCapacity(1000);
        bwd.setNumberOfLanes(1);
        network.addLink(bwd);
    }

    // =========================================================================
    // StopCriterion tests — verify that early termination produces the same
    // costs as the full (unrestricted) search for all nodes within the limit.
    // =========================================================================

    /**
     * Forward search with TravelTimeStopCriterion: all nodes within the travel
     * time limit must have the same cost as an unrestricted full search.
     */
    @Test
    void testForwardWithMaxTravelTimeBerlinNetwork() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile("test/scenarios/berlin/network.xml.gz");
        runForwardStopCriterionTest(scenario.getNetwork(), 600.0); // 10 min limit
    }

    @Test
    void testForwardWithMaxTravelTimeGrid() {
        runForwardStopCriterionTest(buildGridNetwork(15), 500.0);
    }

    /**
     * Backward search with TravelTimeStopCriterion.
     */
    @Test
    void testBackwardWithMaxTravelTimeBerlinNetwork() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile("test/scenarios/berlin/network.xml.gz");
        runBackwardStopCriterionTest(scenario.getNetwork(), 600.0);
    }

    @Test
    void testBackwardWithMaxTravelTimeGrid() {
        runBackwardStopCriterionTest(buildGridNetwork(15), 500.0);
    }

    /**
     * Forward search with allEndNodesReached StopCriterion: costs at the
     * specified target nodes must match the unrestricted search.
     */
    @Test
    void testForwardWithEndNodesReachedBerlinNetwork() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile("test/scenarios/berlin/network.xml.gz");
        runForwardEndNodesTest(scenario.getNetwork());
    }

    @Test
    void testBackwardWithEndNodesReachedBerlinNetwork() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile("test/scenarios/berlin/network.xml.gz");
        runBackwardEndNodesTest(scenario.getNetwork());
    }

    // ---- StopCriterion test helpers ----

    private void runForwardStopCriterionTest(Network network, double maxTravelTime) {
        FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(
                new ScoringConfigGroup());

        SpeedyGraph baseGraph = SpeedyGraphBuilder.build(network);
        InertialFlowCutter.NDOrderResult orderResult =
                new InertialFlowCutter(baseGraph).computeOrderWithBatches();
        CHGraph chGraph = new CHBuilder(baseGraph, tc).buildWithOrderParallel(orderResult);
        new CHTTFCustomizer().customize(chGraph, tc, tc);

        CHLeastCostPathTree chTreeFull = new CHLeastCostPathTree(chGraph, tc, tc);
        CHLeastCostPathTree chTreeBounded = new CHLeastCostPathTree(chGraph, tc, tc);

        List<Link> linkList = new ArrayList<>(network.getLinks().values());
        Random rng = new Random(42);

        LeastCostPathTree.StopCriterion criterion =
                new LeastCostPathTree.TravelTimeStopCriterion(maxTravelTime);

        int mismatches = 0;
        int comparisons = 0;

        for (int s = 0; s < 10; s++) {
            Link srcLink = linkList.get(rng.nextInt(linkList.size()));
            double startTime = 8.0 * 3600;

            chTreeFull.calculate(srcLink, startTime, null, null);
            chTreeBounded.calculate(srcLink, startTime, null, null, criterion);

            for (int t = 0; t < 100; t++) {
                Link tgtLink = linkList.get(rng.nextInt(linkList.size()));
                int tgtIdx = tgtLink.getFromNode().getId().index();

                OptionalTime fullTime = chTreeFull.getTime(tgtIdx);
                if (fullTime.isUndefined()) continue;

                double fullTT = fullTime.seconds() - startTime;
                if (fullTT > maxTravelTime) continue; // beyond limit — don't check

                OptionalTime boundedTime = chTreeBounded.getTime(tgtIdx);
                comparisons++;

                if (boundedTime.isUndefined()) {
                    mismatches++;
                    System.err.printf("FWD STOP-CRITERION MISMATCH src=%s tgt=%s: bounded=unreachable full=%.6f%n",
                            srcLink.getId(), tgtLink.getId(), fullTT);
                    continue;
                }

                double boundedTT = boundedTime.seconds() - startTime;
                if (Math.abs(fullTT - boundedTT) > COST_TOLERANCE) {
                    mismatches++;
                    System.err.printf("FWD STOP-CRITERION MISMATCH src=%s tgt=%s: bounded=%.6f full=%.6f%n",
                            srcLink.getId(), tgtLink.getId(), boundedTT, fullTT);
                }
            }
        }

        Assertions.assertEquals(0, mismatches,
                mismatches + " forward stop-criterion mismatches out of " + comparisons);
    }

    private void runBackwardStopCriterionTest(Network network, double maxTravelTime) {
        FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(
                new ScoringConfigGroup());

        SpeedyGraph baseGraph = SpeedyGraphBuilder.build(network);
        InertialFlowCutter.NDOrderResult orderResult =
                new InertialFlowCutter(baseGraph).computeOrderWithBatches();
        CHGraph chGraph = new CHBuilder(baseGraph, tc).buildWithOrderParallel(orderResult);
        new CHTTFCustomizer().customize(chGraph, tc, tc);

        CHLeastCostPathTree chTreeFull = new CHLeastCostPathTree(chGraph, tc, tc);
        CHLeastCostPathTree chTreeBounded = new CHLeastCostPathTree(chGraph, tc, tc);

        List<Link> linkList = new ArrayList<>(network.getLinks().values());
        Random rng = new Random(42);

        LeastCostPathTree.StopCriterion criterion =
                new LeastCostPathTree.TravelTimeStopCriterion(maxTravelTime);

        int mismatches = 0;
        int comparisons = 0;

        for (int s = 0; s < 10; s++) {
            Link arrLink = linkList.get(rng.nextInt(linkList.size()));
            double arrivalTime = 8.0 * 3600;

            chTreeFull.calculateBackwards(arrLink, arrivalTime, null, null);
            chTreeBounded.calculateBackwards(arrLink, arrivalTime, null, null, criterion);

            for (int t = 0; t < 100; t++) {
                Link srcLink = linkList.get(rng.nextInt(linkList.size()));
                int srcIdx = srcLink.getToNode().getId().index();

                OptionalTime fullTime = chTreeFull.getTime(srcIdx);
                if (fullTime.isUndefined()) continue;

                double fullTT = arrivalTime - fullTime.seconds();
                if (fullTT > maxTravelTime) continue;

                OptionalTime boundedTime = chTreeBounded.getTime(srcIdx);
                comparisons++;

                if (boundedTime.isUndefined()) {
                    mismatches++;
                    System.err.printf("BWD STOP-CRITERION MISMATCH arr=%s src=%s: bounded=unreachable full=%.6f%n",
                            arrLink.getId(), srcLink.getId(), fullTT);
                    continue;
                }

                double boundedTT = arrivalTime - boundedTime.seconds();
                if (Math.abs(fullTT - boundedTT) > COST_TOLERANCE) {
                    mismatches++;
                    System.err.printf("BWD STOP-CRITERION MISMATCH arr=%s src=%s: bounded=%.6f full=%.6f%n",
                            arrLink.getId(), srcLink.getId(), boundedTT, fullTT);
                }
            }
        }

        Assertions.assertEquals(0, mismatches,
                mismatches + " backward stop-criterion mismatches out of " + comparisons);
    }

    private void runForwardEndNodesTest(Network network) {
        FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(
                new ScoringConfigGroup());

        SpeedyGraph baseGraph = SpeedyGraphBuilder.build(network);
        InertialFlowCutter.NDOrderResult orderResult =
                new InertialFlowCutter(baseGraph).computeOrderWithBatches();
        CHGraph chGraph = new CHBuilder(baseGraph, tc).buildWithOrderParallel(orderResult);
        new CHTTFCustomizer().customize(chGraph, tc, tc);

        CHLeastCostPathTree chTreeFull = new CHLeastCostPathTree(chGraph, tc, tc);
        CHLeastCostPathTree chTreeBounded = new CHLeastCostPathTree(chGraph, tc, tc);

        List<Link> linkList = new ArrayList<>(network.getLinks().values());
        Random rng = new Random(42);

        int mismatches = 0;
        int comparisons = 0;

        for (int s = 0; s < 10; s++) {
            Link srcLink = linkList.get(rng.nextInt(linkList.size()));
            double startTime = 8.0 * 3600;

            // Pick 5-10 random target nodes
            int numTargets = 5 + rng.nextInt(6);
            List<Node> targetNodes = new ArrayList<>();
            for (int t = 0; t < numTargets; t++) {
                Link tgtLink = linkList.get(rng.nextInt(linkList.size()));
                targetNodes.add(tgtLink.getFromNode());
            }

            // Build allEndNodesReached criterion (fresh per source — stateful!)
            LeastCostPathTree.StopCriterion criterion =
                    allEndNodesReachedCriterion(targetNodes);

            chTreeFull.calculate(srcLink, startTime, null, null);
            chTreeBounded.calculate(srcLink, startTime, null, null, criterion);

            for (Node tgtNode : targetNodes) {
                int tgtIdx = tgtNode.getId().index();
                OptionalTime fullTime = chTreeFull.getTime(tgtIdx);
                OptionalTime boundedTime = chTreeBounded.getTime(tgtIdx);
                comparisons++;

                if (fullTime.isUndefined() && boundedTime.isUndefined()) continue;

                if (fullTime.isDefined() && boundedTime.isUndefined()) {
                    mismatches++;
                    continue;
                }

                if (fullTime.isDefined() && boundedTime.isDefined()) {
                    double fullTT = fullTime.seconds() - startTime;
                    double boundedTT = boundedTime.seconds() - startTime;
                    if (Math.abs(fullTT - boundedTT) > COST_TOLERANCE) {
                        mismatches++;
                    }
                }
            }
        }

        Assertions.assertEquals(0, mismatches,
                mismatches + " forward endNodesReached mismatches out of " + comparisons);
    }

    private void runBackwardEndNodesTest(Network network) {
        FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(
                new ScoringConfigGroup());

        SpeedyGraph baseGraph = SpeedyGraphBuilder.build(network);
        InertialFlowCutter.NDOrderResult orderResult =
                new InertialFlowCutter(baseGraph).computeOrderWithBatches();
        CHGraph chGraph = new CHBuilder(baseGraph, tc).buildWithOrderParallel(orderResult);
        new CHTTFCustomizer().customize(chGraph, tc, tc);

        CHLeastCostPathTree chTreeFull = new CHLeastCostPathTree(chGraph, tc, tc);
        CHLeastCostPathTree chTreeBounded = new CHLeastCostPathTree(chGraph, tc, tc);

        List<Link> linkList = new ArrayList<>(network.getLinks().values());
        Random rng = new Random(42);

        int mismatches = 0;
        int comparisons = 0;

        for (int s = 0; s < 10; s++) {
            Link arrLink = linkList.get(rng.nextInt(linkList.size()));
            double arrivalTime = 8.0 * 3600;

            int numTargets = 5 + rng.nextInt(6);
            List<Node> targetNodes = new ArrayList<>();
            for (int t = 0; t < numTargets; t++) {
                Link srcLink = linkList.get(rng.nextInt(linkList.size()));
                targetNodes.add(srcLink.getToNode());
            }

            LeastCostPathTree.StopCriterion criterion =
                    allEndNodesReachedCriterion(targetNodes);

            chTreeFull.calculateBackwards(arrLink, arrivalTime, null, null);
            chTreeBounded.calculateBackwards(arrLink, arrivalTime, null, null, criterion);

            for (Node srcNode : targetNodes) {
                int srcIdx = srcNode.getId().index();
                OptionalTime fullTime = chTreeFull.getTime(srcIdx);
                OptionalTime boundedTime = chTreeBounded.getTime(srcIdx);
                comparisons++;

                if (fullTime.isUndefined() && boundedTime.isUndefined()) continue;

                if (fullTime.isDefined() && boundedTime.isUndefined()) {
                    mismatches++;
                    continue;
                }

                if (fullTime.isDefined() && boundedTime.isDefined()) {
                    double fullTT = arrivalTime - fullTime.seconds();
                    double boundedTT = arrivalTime - boundedTime.seconds();
                    if (Math.abs(fullTT - boundedTT) > COST_TOLERANCE) {
                        mismatches++;
                    }
                }
            }
        }

        Assertions.assertEquals(0, mismatches,
                mismatches + " backward endNodesReached mismatches out of " + comparisons);
    }

    /**
     * Creates a fresh allEndNodesReached criterion (the same logic used in
     * {@link org.matsim.contrib.dvrp.path.LeastCostPathTreeStopCriteria}).
     * Duplicated here to avoid a test dependency on the dvrp contrib.
     */
    private static LeastCostPathTree.StopCriterion allEndNodesReachedCriterion(
            java.util.Collection<Node> endNodes) {
        final java.util.BitSet toVisit = new java.util.BitSet(Id.getNumberOfIds(Node.class));
        endNodes.forEach(n -> toVisit.set(n.getId().index()));
        return new LeastCostPathTree.StopCriterion() {
            private int remaining = toVisit.cardinality();
            public boolean stop(int nodeIndex, double arrivalTime, double travelCost,
                                double distance, double departureTime) {
                if (toVisit.get(nodeIndex)) {
                    remaining--;
                    toVisit.clear(nodeIndex);
                }
                return remaining == 0;
            }
        };
    }

    // =========================================================================
    // Thread-safety test — multiple threads query the SAME shared CHGraph
    // concurrently, each with its own CHLeastCostPathTree instance.
    // =========================================================================

    /**
     * Verifies that multiple threads can concurrently run shortest-path-tree
     * queries on a SHARED {@link CHGraph}. Each thread owns its own
     * {@link CHLeastCostPathTree} (mutable query state) but all share
     * the same read-only CH overlay graph.
     * <p>
     * All results are compared against a single-threaded Dijkstra baseline.
     */
    @Test
    void testConcurrentQueriesOnSharedCHGraph() throws Exception {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile("test/scenarios/berlin/network.xml.gz");
        Network network = scenario.getNetwork();

        FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(
                new ScoringConfigGroup());

        SpeedyGraph baseGraph = SpeedyGraphBuilder.build(network);
        InertialFlowCutter.NDOrderResult orderResult =
                new InertialFlowCutter(baseGraph).computeOrderWithBatches();
        CHGraph sharedCHGraph = new CHBuilder(baseGraph, tc).buildWithOrderParallel(orderResult);
        new CHTTFCustomizer().customize(sharedCHGraph, tc, tc);

        LeastCostPathTree dijkstraTree = new LeastCostPathTree(baseGraph, tc, tc);

        List<Link> linkList = new ArrayList<>(network.getLinks().values());

        int numThreads = 8;
        int queriesPerThread = 30;
        java.util.concurrent.CyclicBarrier barrier =
                new java.util.concurrent.CyclicBarrier(numThreads);
        java.util.concurrent.atomic.AtomicInteger failureCount =
                new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.ExecutorService executor =
                java.util.concurrent.Executors.newFixedThreadPool(numThreads);

        List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            futures.add(executor.submit(() -> {
                try {
                    // Each thread creates its OWN tree sharing the graph
                    CHLeastCostPathTree chTree =
                            new CHLeastCostPathTree(sharedCHGraph, tc, tc);
                    Random rng = new Random(42 + threadId);

                    barrier.await(); // synchronize start

                    for (int q = 0; q < queriesPerThread; q++) {
                        Link srcLink = linkList.get(rng.nextInt(linkList.size()));
                        double startTime = 6.0 * 3600 + rng.nextDouble() * 12 * 3600;
                        boolean forward = rng.nextBoolean();

                        if (forward) {
                            chTree.calculate(srcLink, startTime, null, null);
                        } else {
                            chTree.calculateBackwards(srcLink, startTime, null, null);
                        }

                        // Verify a few random targets
                        for (int i = 0; i < 5; i++) {
                            Link tgtLink = linkList.get(rng.nextInt(linkList.size()));
                            int tgtIdx = forward ?
                                    tgtLink.getFromNode().getId().index() :
                                    tgtLink.getToNode().getId().index();

                            OptionalTime chTimeOpt = chTree.getTime(tgtIdx);

                            // Compare against Dijkstra (synchronized — single instance)
                            synchronized (dijkstraTree) {
                                if (forward) {
                                    dijkstraTree.calculate(srcLink, startTime, null, null);
                                } else {
                                    dijkstraTree.calculateBackwards(srcLink, startTime, null, null);
                                }
                                OptionalTime dijTimeOpt = dijkstraTree.getTime(tgtIdx);

                                if (chTimeOpt.isDefined() && dijTimeOpt.isDefined()) {
                                    double chTT = forward ?
                                            (chTimeOpt.seconds() - startTime) :
                                            (startTime - chTimeOpt.seconds());
                                    double dijTT = forward ?
                                            (dijTimeOpt.seconds() - startTime) :
                                            (startTime - dijTimeOpt.seconds());
                                    if (Math.abs(chTT - dijTT) > COST_TOLERANCE) {
                                        failureCount.incrementAndGet();
                                    }
                                } else if (chTimeOpt.isDefined() != dijTimeOpt.isDefined()) {
                                    // one reachable, other not
                                    if (dijTimeOpt.isDefined() && chTimeOpt.isUndefined()) {
                                        failureCount.incrementAndGet();
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    e.printStackTrace();
                }
            }));
        }

        for (var f : futures) f.get();
        executor.shutdown();

        Assertions.assertEquals(0, failureCount.get(),
                "Concurrent CH queries should produce identical results to Dijkstra");
    }

    // =========================================================================
    // Performance benchmark: CH tree vs Dijkstra tree with bounded queries
    // (the DRT use case).  This test prints timing info to System.out — it
    // does NOT assert speed (too environment-dependent) but verifies correctness.
    // =========================================================================

    /**
     * Benchmarks CH tree vs Dijkstra tree for DRT-style bounded queries
     * (forward + backward with maxTravelTime StopCriterion) on the Berlin
     * network.  Prints the average query time for each approach.
     */
    @Test
    void benchmarkBoundedQueriesBerlinNetwork() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile("test/scenarios/berlin/network.xml.gz");
        Network network = scenario.getNetwork();

        FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(
                new ScoringConfigGroup());

        SpeedyGraph baseGraph = SpeedyGraphBuilder.build(network);

        // Build CH graph
        InertialFlowCutter.NDOrderResult orderResult =
                new InertialFlowCutter(baseGraph).computeOrderWithBatches();
        CHGraph chGraph = new CHBuilder(baseGraph, tc).buildWithOrderParallel(orderResult);
        new CHTTFCustomizer().customize(chGraph, tc, tc);

        System.out.printf("Network: %d nodes, %d links. CH: %d total edges (%.1fx overhead)%n",
                network.getNodes().size(), network.getLinks().size(),
                chGraph.totalEdgeCount, (double) chGraph.totalEdgeCount / baseGraph.linkCount);

        CHLeastCostPathTree chTree = new CHLeastCostPathTree(chGraph, tc, tc);
        LeastCostPathTree dijkstraTree = new LeastCostPathTree(baseGraph, tc, tc);

        List<Link> linkList = new ArrayList<>(network.getLinks().values());

        int numQueries = 200;
        int warmup = 50;

        // Test multiple maxTravelTime values
        for (double maxTravelTime : new double[]{30, 60, 120, 300, 600}) {
            Random rng;

            // Warm up
            rng = new Random(42);
            for (int i = 0; i < warmup; i++) {
                Link srcLink = linkList.get(rng.nextInt(linkList.size()));
                LeastCostPathTree.StopCriterion sc1 = new LeastCostPathTree.TravelTimeStopCriterion(maxTravelTime);
                LeastCostPathTree.StopCriterion sc2 = new LeastCostPathTree.TravelTimeStopCriterion(maxTravelTime);
                boolean forward = rng.nextBoolean();
                if (forward) {
                    chTree.calculate(srcLink, 8.0 * 3600, null, null, sc1);
                    dijkstraTree.calculate(srcLink, 8.0 * 3600, null, null, sc2);
                } else {
                    chTree.calculateBackwards(srcLink, 8.0 * 3600, null, null, sc1);
                    dijkstraTree.calculateBackwards(srcLink, 8.0 * 3600, null, null, sc2);
                }
            }

            // Benchmark CH tree
            rng = new Random(123);
            long chStart = System.nanoTime();
            for (int i = 0; i < numQueries; i++) {
                Link srcLink = linkList.get(rng.nextInt(linkList.size()));
                LeastCostPathTree.StopCriterion sc = new LeastCostPathTree.TravelTimeStopCriterion(maxTravelTime);
                boolean forward = rng.nextBoolean();
                if (forward) {
                    chTree.calculate(srcLink, 8.0 * 3600, null, null, sc);
                } else {
                    chTree.calculateBackwards(srcLink, 8.0 * 3600, null, null, sc);
                }
            }
            long chTime = System.nanoTime() - chStart;

            // Benchmark Dijkstra tree
            rng = new Random(123);
            long dijStart = System.nanoTime();
            for (int i = 0; i < numQueries; i++) {
                Link srcLink = linkList.get(rng.nextInt(linkList.size()));
                LeastCostPathTree.StopCriterion sc = new LeastCostPathTree.TravelTimeStopCriterion(maxTravelTime);
                boolean forward = rng.nextBoolean();
                if (forward) {
                    dijkstraTree.calculate(srcLink, 8.0 * 3600, null, null, sc);
                } else {
                    dijkstraTree.calculateBackwards(srcLink, 8.0 * 3600, null, null, sc);
                }
            }
            long dijTime = System.nanoTime() - dijStart;

            double chAvgMs = (chTime / 1_000_000.0) / numQueries;
            double dijAvgMs = (dijTime / 1_000_000.0) / numQueries;
            double speedup = dijAvgMs / chAvgMs;

            System.out.printf("  maxTT=%4.0fs  CH=%.3f ms  Dijkstra=%.3f ms  speedup=%.2fx%n",
                    maxTravelTime, chAvgMs, dijAvgMs, speedup);
        }

        // Verify correctness at 120s
        Random rng2 = new Random(999);
        int mismatches = 0;
        double verifyMaxTT = 120.0;
        for (int i = 0; i < 50; i++) {
            Link srcLink = linkList.get(rng2.nextInt(linkList.size()));
            LeastCostPathTree.StopCriterion sc1 = new LeastCostPathTree.TravelTimeStopCriterion(verifyMaxTT);
            LeastCostPathTree.StopCriterion sc2 = new LeastCostPathTree.TravelTimeStopCriterion(verifyMaxTT);
            double startTime = 8.0 * 3600;

            chTree.calculate(srcLink, startTime, null, null, sc1);
            dijkstraTree.calculate(srcLink, startTime, null, null, sc2);

            for (int t = 0; t < 20; t++) {
                Link tgtLink = linkList.get(rng2.nextInt(linkList.size()));
                int tgtIdx = tgtLink.getFromNode().getId().index();

                OptionalTime chTimeOpt = chTree.getTime(tgtIdx);
                OptionalTime dijTimeOpt = dijkstraTree.getTime(tgtIdx);

                if (dijTimeOpt.isDefined()) {
                    double dijTT = dijTimeOpt.seconds() - startTime;
                    if (dijTT <= verifyMaxTT) {
                        if (chTimeOpt.isUndefined()) {
                            mismatches++;
                        } else {
                            double chTT = chTimeOpt.seconds() - startTime;
                            if (Math.abs(chTT - dijTT) > COST_TOLERANCE) {
                                mismatches++;
                            }
                        }
                    }
                }
            }
        }

        Assertions.assertEquals(0, mismatches,
                mismatches + " mismatches in benchmark correctness check");
    }

    // =========================================================================
    // Path reconstruction tests: verify that getLinkPathIterator produces
    // connected link sequences (each link's toNode == next link's fromNode).
    // This catches bugs in shortcut unpacking order (e.g., lower1/lower2 swap).
    // =========================================================================

    @Test
    void testForwardPathConnectivitySmallGrid() {
        Network network = buildGridNetwork(5);
        runForwardPathConnectivityTest(network);
    }

    @Test
    void testForwardPathConnectivityBerlinNetwork() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile("test/scenarios/berlin/network.xml.gz");
        runForwardPathConnectivityTest(scenario.getNetwork());
    }

    @Test
    void testBackwardPathConnectivitySmallGrid() {
        Network network = buildGridNetwork(5);
        runBackwardPathConnectivityTest(network);
    }

    @Test
    void testBackwardPathConnectivityBerlinNetwork() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile("test/scenarios/berlin/network.xml.gz");
        runBackwardPathConnectivityTest(scenario.getNetwork());
    }

    /**
     * Forward search: verify that the link path from source to every reachable
     * target forms a connected chain and matches the Dijkstra reference path.
     */
    private void runForwardPathConnectivityTest(Network network) {
        FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(
                new ScoringConfigGroup());

        SpeedyGraph baseGraph = SpeedyGraphBuilder.build(network);
        InertialFlowCutter.NDOrderResult orderResult =
                new InertialFlowCutter(baseGraph).computeOrderWithBatches();
        CHGraph chGraph = new CHBuilder(baseGraph, tc).buildWithOrderParallel(orderResult);
        new CHTTFCustomizer().customize(chGraph, tc, tc);

        CHLeastCostPathTree chTree = new CHLeastCostPathTree(chGraph, tc, tc);
        LeastCostPathTree dijkstraTree = new LeastCostPathTree(baseGraph, tc, tc);

        List<Link> linkList = new ArrayList<>(network.getLinks().values());
        int n = linkList.size();
        if (n < 2) return;

        Random rng = new Random(42);
        int disconnectedPaths = 0;
        int totalPaths = 0;

        for (int s = 0; s < NUM_SOURCE_NODES; s++) {
            Link srcLink = linkList.get(rng.nextInt(n));
            double startTime = 8.0 * 3600;

            chTree.calculate(srcLink, startTime, null, null);
            dijkstraTree.calculate(srcLink, startTime, null, null);

            for (int t = 0; t < NUM_TARGET_NODES; t++) {
                Link tgtLink = linkList.get(rng.nextInt(n));
                if (tgtLink == srcLink) continue;

                Node endNode = tgtLink.getFromNode();
                int endIdx = chTree.getNodeIndex(endNode);

                OptionalTime chTimeOpt = chTree.getTime(endIdx);
                if (chTimeOpt.isUndefined()) continue;

                // Reconstruct the link path (target to source, as the iterator produces)
                java.util.List<Link> chLinks = new ArrayList<>();
                Iterator<Link> chLinkIter = chTree.getLinkPathIterator(endNode);
                chLinkIter.forEachRemaining(chLinks::add);

                if (chLinks.isEmpty()) continue;
                totalPaths++;

                // After reversal (as OneToManyPathCalculator does for forward search)
                java.util.Collections.reverse(chLinks);

                // Verify the link chain is connected
                boolean connected = true;
                for (int i = 0; i < chLinks.size() - 1; i++) {
                    if (chLinks.get(i).getToNode() != chLinks.get(i + 1).getFromNode()) {
                        connected = false;
                        break;
                    }
                }

                // Verify the path starts at source and ends at target
                Node srcNode = srcLink.getToNode();
                if (!chLinks.isEmpty()) {
                    if (chLinks.get(0).getFromNode() != srcNode) connected = false;
                    if (chLinks.get(chLinks.size() - 1).getToNode() != endNode) connected = false;
                }

                if (!connected) {
                    disconnectedPaths++;
                }
            }
        }

        Assertions.assertEquals(0, disconnectedPaths,
                disconnectedPaths + " disconnected forward paths out of " + totalPaths);
    }

    /**
     * Backward search: verify that the link path from every reachable source to
     * the target forms a connected chain.
     */
    private void runBackwardPathConnectivityTest(Network network) {
        FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(
                new ScoringConfigGroup());

        SpeedyGraph baseGraph = SpeedyGraphBuilder.build(network);
        InertialFlowCutter.NDOrderResult orderResult =
                new InertialFlowCutter(baseGraph).computeOrderWithBatches();
        CHGraph chGraph = new CHBuilder(baseGraph, tc).buildWithOrderParallel(orderResult);
        new CHTTFCustomizer().customize(chGraph, tc, tc);

        CHLeastCostPathTree chTree = new CHLeastCostPathTree(chGraph, tc, tc);

        List<Link> linkList = new ArrayList<>(network.getLinks().values());
        int n = linkList.size();
        if (n < 2) return;

        Random rng = new Random(42);
        int disconnectedPaths = 0;
        int totalPaths = 0;

        for (int s = 0; s < NUM_SOURCE_NODES; s++) {
            Link arrivalLink = linkList.get(rng.nextInt(n));
            double arrivalTime = 8.0 * 3600;

            chTree.calculateBackwards(arrivalLink, arrivalTime, null, null);

            for (int t = 0; t < NUM_TARGET_NODES; t++) {
                Link startLink = linkList.get(rng.nextInt(n));
                if (startLink == arrivalLink) continue;

                Node endNode = startLink.getToNode();
                int endIdx = chTree.getNodeIndex(endNode);

                OptionalTime chTimeOpt = chTree.getTime(endIdx);
                if (chTimeOpt.isUndefined()) continue;

                // Reconstruct the link path (source to target, no reversal for backward)
                java.util.List<Link> chLinks = new ArrayList<>();
                Iterator<Link> chLinkIter = chTree.getLinkPathIterator(endNode);
                chLinkIter.forEachRemaining(chLinks::add);

                if (chLinks.isEmpty()) continue;
                totalPaths++;

                // Verify the link chain is connected
                boolean connected = true;
                for (int i = 0; i < chLinks.size() - 1; i++) {
                    if (chLinks.get(i).getToNode() != chLinks.get(i + 1).getFromNode()) {
                        connected = false;
                        break;
                    }
                }

                // Verify the path ends at the arrival node
                Node arrNode = arrivalLink.getFromNode();
                if (!chLinks.isEmpty()) {
                    if (chLinks.get(0).getFromNode() != endNode) connected = false;
                    if (chLinks.get(chLinks.size() - 1).getToNode() != arrNode) connected = false;
                }

                if (!connected) {
                    disconnectedPaths++;
                }
            }
        }

        Assertions.assertEquals(0, disconnectedPaths,
                disconnectedPaths + " disconnected backward paths out of " + totalPaths);
    }

    // =========================================================================
    // Turn-restriction tests
    //
    // These cover the case where the base graph carries DisallowedNextLinks.
    // SpeedyGraphBuilder responds by inserting "colored" copies of nodes and
    // links so the shortest path respects forbidden manoeuvres.  The CH built
    // on top inherits those colored copies, but only some are reached during
    // the search.  The test verifies that:
    //   (a) forward and backward searches still settle every node that plain
    //       Dijkstra (LeastCostPathTree) settles,
    //   (b) the cost on the real (uncolored) node index matches plain
    //       Dijkstra, i.e. consolidateColoredNodes() has run, and
    //   (c) for a hand-crafted network with a forbidden direct path, the CH
    //       result is the expected long detour rather than the forbidden short
    //       direct route.
    // =========================================================================

    /**
     * Hand-crafted network from {@link org.matsim.core.router.AbstractLeastCostPathCalculatorTestWithTurnRestrictions}:
     * {@code S -> 1 -> T} is the direct two-link path but is forbidden by a
     * turn restriction on link {@code S1}.  The only legal way is the
     * six-link detour {@code S -> 1 -> 2 -> 3 -> 4 -> 5 -> T}.
     */
    @Test
    void testForwardSearchTurnRestrictions_handcrafted() {
        Network network = buildTurnRestrictionsTestNetwork();
        FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());

        SpeedyGraph baseGraph = SpeedyGraphBuilder.build(network);
        InertialFlowCutter.NDOrderResult order = new InertialFlowCutter(baseGraph).computeOrderWithBatches();
        CHGraph chGraph = new CHBuilder(baseGraph, tc).buildWithOrderParallel(order);
        new CHTTFCustomizer().customize(chGraph, tc, tc);

        CHLeastCostPathTree chTree = new CHLeastCostPathTree(chGraph, tc, tc);
        LeastCostPathTree dijTree = new LeastCostPathTree(baseGraph, tc, tc);

        Link startLink = network.getLinks().get(Id.createLinkId("S1"));
        double depTime = 8.0 * 3600;
        chTree.calculate(startLink, depTime, null, null);
        dijTree.calculate(startLink, depTime, null, null);

        Node nodeT = network.getNodes().get(Id.createNodeId("T"));
        int tIdx = nodeT.getId().index();

        OptionalTime chT = chTree.getTime(tIdx);
        OptionalTime dijT = dijTree.getTime(tIdx);
        Assertions.assertTrue(dijT.isDefined(),
                "test fixture broken: Dijkstra should reach T via the legal detour");
        Assertions.assertTrue(chT.isDefined(),
                "CH must reach T via the legal detour (forbidden direct turn must be respected)");
        Assertions.assertEquals(dijT.seconds(), chT.seconds(), COST_TOLERANCE,
                "CH arrival time at T must match Dijkstra (legal 6-link detour)");

        // Cross-check every reachable node: CH must agree with Dijkstra wherever
        // Dijkstra has a defined time.  This catches the case where CH leaves a
        // colored copy populated but the real node slot empty.
        int mismatches = 0;
        for (Node n : network.getNodes().values()) {
            int idx = n.getId().index();
            OptionalTime cht = chTree.getTime(idx);
            OptionalTime djt = dijTree.getTime(idx);
            if (djt.isDefined()) {
                if (cht.isUndefined()) {
                    mismatches++;
                    System.err.printf("FORWARD-TR MISMATCH node=%s: CH=unreachable Dijkstra=%.3f%n",
                            n.getId(), djt.seconds());
                } else if (Math.abs(cht.seconds() - djt.seconds()) > COST_TOLERANCE) {
                    mismatches++;
                    System.err.printf("FORWARD-TR MISMATCH node=%s: CH=%.6f Dijkstra=%.6f%n",
                            n.getId(), cht.seconds(), djt.seconds());
                }
            }
        }
        Assertions.assertEquals(0, mismatches, mismatches + " forward TR mismatches");
    }

    /**
     * Same hand-crafted network as
     * {@link #testForwardSearchTurnRestrictions_handcrafted()} but with a
     * backward search from {@code T}.  Every node reachable by plain Dijkstra
     * must also be reachable by CH with the same cost.
     */
    @Test
    void testBackwardSearchTurnRestrictions_handcrafted() {
        Network network = buildTurnRestrictionsTestNetwork();
        FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());

        SpeedyGraph baseGraph = SpeedyGraphBuilder.build(network);
        InertialFlowCutter.NDOrderResult order = new InertialFlowCutter(baseGraph).computeOrderWithBatches();
        CHGraph chGraph = new CHBuilder(baseGraph, tc).buildWithOrderParallel(order);
        new CHTTFCustomizer().customize(chGraph, tc, tc);

        CHLeastCostPathTree chTree = new CHLeastCostPathTree(chGraph, tc, tc);
        LeastCostPathTree dijTree = new LeastCostPathTree(baseGraph, tc, tc);

        Link arrivalLink = network.getLinks().get(Id.createLinkId("5T"));
        double arrTime = 8.0 * 3600;
        chTree.calculateBackwards(arrivalLink, arrTime, null, null);
        dijTree.calculateBackwards(arrivalLink, arrTime, null, null);

        Node nodeS = network.getNodes().get(Id.createNodeId("S"));
        int sIdx = nodeS.getId().index();
        OptionalTime chS = chTree.getTime(sIdx);
        OptionalTime djS = dijTree.getTime(sIdx);
        Assertions.assertTrue(djS.isDefined(),
                "test fixture broken: Dijkstra backwards should reach S");
        Assertions.assertTrue(chS.isDefined(),
                "CH backwards must reach S via the legal detour");
        Assertions.assertEquals(djS.seconds(), chS.seconds(), COST_TOLERANCE,
                "CH backwards departure time at S must match Dijkstra");

        int mismatches = 0;
        for (Node n : network.getNodes().values()) {
            int idx = n.getId().index();
            OptionalTime cht = chTree.getTime(idx);
            OptionalTime djt = dijTree.getTime(idx);
            if (djt.isDefined()) {
                if (cht.isUndefined()) {
                    mismatches++;
                    System.err.printf("BACKWARD-TR MISMATCH node=%s: CH=unreachable Dijkstra=%.3f%n",
                            n.getId(), djt.seconds());
                } else if (Math.abs(cht.seconds() - djt.seconds()) > COST_TOLERANCE) {
                    mismatches++;
                    System.err.printf("BACKWARD-TR MISMATCH node=%s: CH=%.6f Dijkstra=%.6f%n",
                            n.getId(), cht.seconds(), djt.seconds());
                }
            }
        }
        Assertions.assertEquals(0, mismatches, mismatches + " backward TR mismatches");
    }

    /**
     * Realistic-scale coverage check: load the equil scenario, mark a handful
     * of disallowed turns, and confirm that for many random sources the CH
     * tree reaches the same set of nodes as plain Dijkstra with the same
     * costs.  This is the regression test for the production failure where
     * CH workers threw {@code "Undefined Time"} for destinations that plain
     * Dijkstra reached because colored copies were never consolidated.
     */
    @Test
    void testForwardSearchTurnRestrictions_equilCoverage() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile("test/scenarios/equil/network.xml");
        Network network = scenario.getNetwork();

        // Add a few disallowed turns spread across the network so multiple
        // colored copies are inserted.  We pick links by iteration order so
        // the test is reproducible without depending on exact equil link IDs.
        List<Link> links = new ArrayList<>(network.getLinks().values());
        int restrictionsAdded = 0;
        for (int i = 0; i < links.size() && restrictionsAdded < 3; i++) {
            Link from = links.get(i);
            // Find an outgoing link of from.getToNode() to forbid.
            for (Link out : from.getToNode().getOutLinks().values()) {
                if (out.getToNode() != from.getFromNode()) { // skip U-turn (often already disallowed)
                    NetworkUtils.addDisallowedNextLinks(from, TransportMode.car,
                            Arrays.asList(out.getId()));
                    restrictionsAdded++;
                    break;
                }
            }
        }
        Assertions.assertTrue(restrictionsAdded >= 1,
                "expected to add at least one disallowed-turn restriction");

        FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());

        SpeedyGraph baseGraph = SpeedyGraphBuilder.build(network);
        InertialFlowCutter.NDOrderResult order = new InertialFlowCutter(baseGraph).computeOrderWithBatches();
        CHGraph chGraph = new CHBuilder(baseGraph, tc).buildWithOrderParallel(order);
        new CHTTFCustomizer().customize(chGraph, tc, tc);

        CHLeastCostPathTree chTree = new CHLeastCostPathTree(chGraph, tc, tc);
        LeastCostPathTree dijTree = new LeastCostPathTree(baseGraph, tc, tc);

        Random rng = new Random(7);
        int mismatches = 0;
        int comparisons = 0;
        double depTime = 8.0 * 3600;
        for (int s = 0; s < 10; s++) {
            Link startLink = links.get(rng.nextInt(links.size()));
            chTree.calculate(startLink, depTime, null, null);
            dijTree.calculate(startLink, depTime, null, null);

            for (Node n : network.getNodes().values()) {
                int idx = n.getId().index();
                OptionalTime cht = chTree.getTime(idx);
                OptionalTime djt = dijTree.getTime(idx);
                if (djt.isUndefined()) continue;
                comparisons++;
                if (cht.isUndefined()) {
                    mismatches++;
                    if (mismatches < 10) {
                        System.err.printf("EQUIL-TR MISMATCH src=%s node=%s: CH=unreachable Dijkstra=%.3f%n",
                                startLink.getId(), n.getId(), djt.seconds());
                    }
                } else if (Math.abs(cht.seconds() - djt.seconds()) > COST_TOLERANCE) {
                    mismatches++;
                    if (mismatches < 10) {
                        System.err.printf("EQUIL-TR MISMATCH src=%s node=%s: CH=%.6f Dijkstra=%.6f%n",
                                startLink.getId(), n.getId(), cht.seconds(), djt.seconds());
                    }
                }
            }
        }
        Assertions.assertEquals(0, mismatches,
                mismatches + " TR coverage mismatches out of " + comparisons + " comparisons");
    }

    /**
     * Backward equivalent of {@link #testForwardSearchTurnRestrictions_equilCoverage()}.
     * Loads the equil scenario with the same disallowed turns and confirms
     * backward CH matches backward Dijkstra everywhere the latter is defined.
     * This exercises the colored-arrival seeding in {@code calculateBackwardImpl}.
     */
    @Test
    void testBackwardSearchTurnRestrictions_equilCoverage() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile("test/scenarios/equil/network.xml");
        Network network = scenario.getNetwork();

        List<Link> links = new ArrayList<>(network.getLinks().values());
        int restrictionsAdded = 0;
        for (int i = 0; i < links.size() && restrictionsAdded < 3; i++) {
            Link from = links.get(i);
            for (Link out : from.getToNode().getOutLinks().values()) {
                if (out.getToNode() != from.getFromNode()) {
                    NetworkUtils.addDisallowedNextLinks(from, TransportMode.car,
                            Arrays.asList(out.getId()));
                    restrictionsAdded++;
                    break;
                }
            }
        }
        Assertions.assertTrue(restrictionsAdded >= 1,
                "expected to add at least one disallowed-turn restriction");

        FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());

        SpeedyGraph baseGraph = SpeedyGraphBuilder.build(network);
        InertialFlowCutter.NDOrderResult order = new InertialFlowCutter(baseGraph).computeOrderWithBatches();
        CHGraph chGraph = new CHBuilder(baseGraph, tc).buildWithOrderParallel(order);
        new CHTTFCustomizer().customize(chGraph, tc, tc);

        CHLeastCostPathTree chTree = new CHLeastCostPathTree(chGraph, tc, tc);
        LeastCostPathTree dijTree = new LeastCostPathTree(baseGraph, tc, tc);

        Random rng = new Random(11);
        int mismatches = 0;
        int comparisons = 0;
        double arrTime = 8.0 * 3600;
        for (int s = 0; s < 10; s++) {
            Link arrivalLink = links.get(rng.nextInt(links.size()));
            chTree.calculateBackwards(arrivalLink, arrTime, null, null);
            dijTree.calculateBackwards(arrivalLink, arrTime, null, null);

            for (Node n : network.getNodes().values()) {
                int idx = n.getId().index();
                OptionalTime cht = chTree.getTime(idx);
                OptionalTime djt = dijTree.getTime(idx);
                if (djt.isUndefined()) continue;
                comparisons++;
                if (cht.isUndefined()) {
                    mismatches++;
                    if (mismatches < 10) {
                        System.err.printf("EQUIL-TR-BWD MISMATCH arr=%s node=%s: CH=unreachable Dijkstra=%.3f%n",
                                arrivalLink.getId(), n.getId(), djt.seconds());
                    }
                } else if (Math.abs(cht.seconds() - djt.seconds()) > COST_TOLERANCE) {
                    mismatches++;
                    if (mismatches < 10) {
                        System.err.printf("EQUIL-TR-BWD MISMATCH arr=%s node=%s: CH=%.6f Dijkstra=%.6f%n",
                                arrivalLink.getId(), n.getId(), cht.seconds(), djt.seconds());
                    }
                }
            }
        }
        Assertions.assertEquals(0, mismatches,
                mismatches + " backward TR coverage mismatches out of " + comparisons + " comparisons");
    }

    /**
     * {@link org.matsim.core.router.AbstractLeastCostPathCalculatorTestWithTurnRestrictions}
     * with forbidden turns {@code S1->1T} and {@code 23->34}, {@code 23->4T}.
     */
    private static Network buildTurnRestrictionsTestNetwork() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Network network = scenario.getNetwork();
        Node nodeS = NetworkUtils.createAndAddNode(network, Id.createNodeId("S"), new Coord(1, 2));
        Node node1 = NetworkUtils.createAndAddNode(network, Id.createNodeId("1"), new Coord(1, 1));
        Node node2 = NetworkUtils.createAndAddNode(network, Id.createNodeId("2"), new Coord(0, 1));
        Node node3 = NetworkUtils.createAndAddNode(network, Id.createNodeId("3"), new Coord(0, 0));
        Node node4 = NetworkUtils.createAndAddNode(network, Id.createNodeId("4"), new Coord(1, 0));
        Node node5 = NetworkUtils.createAndAddNode(network, Id.createNodeId("5"), new Coord(2, 0));
        Node nodeT = NetworkUtils.createAndAddNode(network, Id.createNodeId("T"), new Coord(2, 0));

        Link linkS1 = NetworkUtils.createAndAddLink(network, Id.createLinkId("S1"), nodeS, node1, 1, 1, 1, 1);
        NetworkUtils.createAndAddLink(network, Id.createLinkId("12"), node1, node2, 1, 1, 1, 1);
        NetworkUtils.createAndAddLink(network, Id.createLinkId("1T"), node1, nodeT, 1, 1, 1, 1);
        Link link23 = NetworkUtils.createAndAddLink(network, Id.createLinkId("23"), node2, node3, 1, 1, 1, 1);
        NetworkUtils.createAndAddLink(network, Id.createLinkId("34"), node3, node4, 1, 1, 1, 1);
        NetworkUtils.createAndAddLink(network, Id.createLinkId("4T"), node4, nodeT, 1, 1, 1, 1);
        NetworkUtils.createAndAddLink(network, Id.createLinkId("45"), node4, node5, 1, 1, 1, 1);
        NetworkUtils.createAndAddLink(network, Id.createLinkId("5T"), node5, nodeT, 1, 1, 1, 1);

        NetworkUtils.addDisallowedNextLinks(linkS1, TransportMode.car, Arrays.asList(Id.createLinkId("1T")));
        NetworkUtils.addDisallowedNextLinks(link23, TransportMode.car,
                Arrays.asList(Id.createLinkId("34"), Id.createLinkId("4T")));
        return network;
    }

    // =========================================================================
    // Distance accumulation tests
    //
    // CHLeastCostPathTree.getDistance(idx) must return the same network distance
    // (sum of link lengths along the resolved shortest path) as plain
    // LeastCostPathTree.  Distance is precomputed once per CH global edge
    // (including shortcuts) at build time and accumulated during relaxation.
    // =========================================================================

    @Test
    void testForwardDistanceMatchesDijkstra_grid() {
        Network network = buildGridNetwork(10);
        assertForwardDistanceMatchesDijkstra(network, 5);
    }

    @Test
    void testBackwardDistanceMatchesDijkstra_grid() {
        Network network = buildGridNetwork(10);
        assertBackwardDistanceMatchesDijkstra(network, 5);
    }

    @Test
    void testForwardDistanceMatchesDijkstra_equil() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile("test/scenarios/equil/network.xml");
        assertForwardDistanceMatchesDijkstra(scenario.getNetwork(), 10);
    }

    @Test
    void testBackwardDistanceMatchesDijkstra_equil() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile("test/scenarios/equil/network.xml");
        assertBackwardDistanceMatchesDijkstra(scenario.getNetwork(), 10);
    }

    /**
     * Forward distance with turn restrictions: the start link itself is not
     * traversed (the tree is rooted at {@code startLink.getToNode()}); the
     * legal path from node 1 is {@code 1->2->3->4->5->T} (5 links of length 1),
     * not the forbidden {@code 1->T} (1 link).
     */
    @Test
    void testForwardDistanceMatchesDijkstra_turnRestrictions() {
        Network network = buildTurnRestrictionsTestNetwork();
        FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());

        SpeedyGraph baseGraph = SpeedyGraphBuilder.build(network);
        InertialFlowCutter.NDOrderResult order = new InertialFlowCutter(baseGraph).computeOrderWithBatches();
        CHGraph chGraph = new CHBuilder(baseGraph, tc).buildWithOrderParallel(order);
        new CHTTFCustomizer().customize(chGraph, tc, tc);

        CHLeastCostPathTree chTree = new CHLeastCostPathTree(chGraph, tc, tc);
        LeastCostPathTree dijTree = new LeastCostPathTree(baseGraph, tc, tc);

        Link startLink = network.getLinks().get(Id.createLinkId("S1"));
        double depTime = 8.0 * 3600;
        chTree.calculate(startLink, depTime, null, null);
        dijTree.calculate(startLink, depTime, null, null);

        int tIdx = network.getNodes().get(Id.createNodeId("T")).getId().index();
        Assertions.assertEquals(5.0, dijTree.getDistance(tIdx), COST_TOLERANCE,
                "test fixture broken: Dijkstra distance at T should be 5 (legal detour from node 1)");
        Assertions.assertEquals(dijTree.getDistance(tIdx), chTree.getDistance(tIdx), COST_TOLERANCE,
                "CH distance at T must match Dijkstra (legal 5-link detour from node 1)");
    }

    private static void assertForwardDistanceMatchesDijkstra(Network network, int numSources) {
        FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());
        SpeedyGraph baseGraph = SpeedyGraphBuilder.build(network);
        InertialFlowCutter.NDOrderResult order = new InertialFlowCutter(baseGraph).computeOrderWithBatches();
        CHGraph chGraph = new CHBuilder(baseGraph, tc).buildWithOrderParallel(order);
        new CHTTFCustomizer().customize(chGraph, tc, tc);

        CHLeastCostPathTree chTree = new CHLeastCostPathTree(chGraph, tc, tc);
        LeastCostPathTree dijTree = new LeastCostPathTree(baseGraph, tc, tc);

        List<Link> links = new ArrayList<>(network.getLinks().values());
        Random rng = new Random(13);
        int mismatches = 0;
        int comparisons = 0;
        double depTime = 8.0 * 3600;
        for (int s = 0; s < numSources; s++) {
            Link srcLink = links.get(rng.nextInt(links.size()));
            chTree.calculate(srcLink, depTime, null, null);
            dijTree.calculate(srcLink, depTime, null, null);

            for (Node n : network.getNodes().values()) {
                int idx = n.getId().index();
                if (dijTree.getTime(idx).isUndefined()) continue;
                comparisons++;
                double chD = chTree.getDistance(idx);
                double djD = dijTree.getDistance(idx);
                if (Math.abs(chD - djD) > COST_TOLERANCE) {
                    mismatches++;
                    if (mismatches < 10) {
                        System.err.printf("FWD-DIST MISMATCH src=%s node=%s: CH=%.6f Dijkstra=%.6f%n",
                                srcLink.getId(), n.getId(), chD, djD);
                    }
                }
            }
        }
        Assertions.assertEquals(0, mismatches,
                mismatches + " forward-distance mismatches out of " + comparisons + " comparisons");
    }

    private static void assertBackwardDistanceMatchesDijkstra(Network network, int numArrivals) {
        FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());
        SpeedyGraph baseGraph = SpeedyGraphBuilder.build(network);
        InertialFlowCutter.NDOrderResult order = new InertialFlowCutter(baseGraph).computeOrderWithBatches();
        CHGraph chGraph = new CHBuilder(baseGraph, tc).buildWithOrderParallel(order);
        new CHTTFCustomizer().customize(chGraph, tc, tc);

        CHLeastCostPathTree chTree = new CHLeastCostPathTree(chGraph, tc, tc);
        LeastCostPathTree dijTree = new LeastCostPathTree(baseGraph, tc, tc);

        List<Link> links = new ArrayList<>(network.getLinks().values());
        Random rng = new Random(17);
        int mismatches = 0;
        int comparisons = 0;
        double arrTime = 8.0 * 3600;
        for (int s = 0; s < numArrivals; s++) {
            Link arrLink = links.get(rng.nextInt(links.size()));
            chTree.calculateBackwards(arrLink, arrTime, null, null);
            dijTree.calculateBackwards(arrLink, arrTime, null, null);

            for (Node n : network.getNodes().values()) {
                int idx = n.getId().index();
                if (dijTree.getTime(idx).isUndefined()) continue;
                comparisons++;
                double chD = chTree.getDistance(idx);
                double djD = dijTree.getDistance(idx);
                if (Math.abs(chD - djD) > COST_TOLERANCE) {
                    mismatches++;
                    if (mismatches < 10) {
                        System.err.printf("BWD-DIST MISMATCH arr=%s node=%s: CH=%.6f Dijkstra=%.6f%n",
                                arrLink.getId(), n.getId(), chD, djD);
                    }
                }
            }
        }
        Assertions.assertEquals(0, mismatches,
                mismatches + " backward-distance mismatches out of " + comparisons + " comparisons");
    }

    // ---- static (time-independent) CHLeastCostPathTree correctness ----
    //
    // Mirrors runForward/Backward CorrectnessTest but builds the CH with
    // withTimeDependence(false) and customizes with CHCustomizer instead of
    // CHTTFCustomizer.  Exercises the static branches added in
    // CHLeastCostPathTree (forwardPhase1Static + edgeWeights[] in backward
    // phase 1).  Since FreespeedTravelTimeAndDisutility is time-independent,
    // the static tree must match the time-dependent reference Dijkstra exactly.

    @Test
    void testForwardSearchSmallGridStatic() {
        runForwardStaticCorrectnessTest(buildGridNetwork(5));
    }

    @Test
    void testForwardSearchEquilNetworkStatic() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile("test/scenarios/equil/network.xml");
        runForwardStaticCorrectnessTest(scenario.getNetwork());
    }

    @Test
    void testBackwardSearchSmallGridStatic() {
        runBackwardStaticCorrectnessTest(buildGridNetwork(5));
    }

    @Test
    void testBackwardSearchEquilNetworkStatic() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile("test/scenarios/equil/network.xml");
        runBackwardStaticCorrectnessTest(scenario.getNetwork());
    }

    private void runForwardStaticCorrectnessTest(Network network) {
        FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(
                new ScoringConfigGroup());

        SpeedyGraph baseGraph = SpeedyGraphBuilder.build(network);

        InertialFlowCutter.NDOrderResult orderResult =
                new InertialFlowCutter(baseGraph).computeOrderWithBatches();
        CHGraph chGraph = new CHBuilder(baseGraph, tc)
                .withTimeDependence(false)
                .buildWithOrderParallel(orderResult);
        new CHCustomizer().customize(chGraph, tc);
        Assertions.assertFalse(chGraph.isTimeDependent(),
                "CHGraph must be marked time-independent in static mode.");

        CHLeastCostPathTree chTree = new CHLeastCostPathTree(chGraph, tc, tc);
        LeastCostPathTree dijkstraTree = new LeastCostPathTree(baseGraph, tc, tc);

        List<Link> linkList = new ArrayList<>(network.getLinks().values());
        int n = linkList.size();
        if (n < 2) return;

        Random rng = new Random(42);
        int mismatches = 0;
        int comparisons = 0;

        for (int s = 0; s < NUM_SOURCE_NODES; s++) {
            Link srcLink = linkList.get(rng.nextInt(n));
            double startTime = 8.0 * 3600;

            chTree.calculate(srcLink, startTime, null, null);
            dijkstraTree.calculate(srcLink, startTime, null, null);

            for (int t = 0; t < NUM_TARGET_NODES; t++) {
                Link tgtLink = linkList.get(rng.nextInt(n));
                int tgtNodeIdx = tgtLink.getFromNode().getId().index();

                // Compare cost (= sum of edge disutilities). In static mode
                // the "time" field is not meaningful because edgeWeights[]
                // holds disutility, not travel time.
                double chCost = chTree.getCost(tgtNodeIdx);
                double dijCost = dijkstraTree.getCost(tgtNodeIdx);

                boolean chUnreach = Double.isInfinite(chCost) || Double.isNaN(chCost);
                boolean dijUnreach = Double.isInfinite(dijCost) || Double.isNaN(dijCost);
                if (chUnreach && dijUnreach) {
                    comparisons++;
                    continue;
                }
                if (chUnreach || dijUnreach) {
                    if (!dijUnreach && chUnreach) mismatches++;
                    comparisons++;
                    continue;
                }

                comparisons++;
                if (Math.abs(chCost - dijCost) > COST_TOLERANCE) {
                    mismatches++;
                    System.err.printf("STATIC FORWARD MISMATCH src=%s tgt=%s: CH=%.6f  Dijkstra=%.6f%n",
                            srcLink.getId(), tgtLink.getId(), chCost, dijCost);
                }
            }
        }

        Assertions.assertEquals(0, mismatches,
                mismatches + " static-forward cost mismatches out of " + comparisons + " comparisons.");
    }

    private void runBackwardStaticCorrectnessTest(Network network) {
        FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(
                new ScoringConfigGroup());

        SpeedyGraph baseGraph = SpeedyGraphBuilder.build(network);

        InertialFlowCutter.NDOrderResult orderResult =
                new InertialFlowCutter(baseGraph).computeOrderWithBatches();
        CHGraph chGraph = new CHBuilder(baseGraph, tc)
                .withTimeDependence(false)
                .buildWithOrderParallel(orderResult);
        new CHCustomizer().customize(chGraph, tc);

        CHLeastCostPathTree chTree = new CHLeastCostPathTree(chGraph, tc, tc);
        LeastCostPathTree dijkstraTree = new LeastCostPathTree(baseGraph, tc, tc);

        List<Link> linkList = new ArrayList<>(network.getLinks().values());
        int n = linkList.size();
        if (n < 2) return;

        Random rng = new Random(42);
        int mismatches = 0;
        int comparisons = 0;

        for (int s = 0; s < NUM_SOURCE_NODES; s++) {
            Link arrLink = linkList.get(rng.nextInt(n));
            double arrivalTime = 8.0 * 3600;

            chTree.calculateBackwards(arrLink, arrivalTime, null, null);
            dijkstraTree.calculateBackwards(arrLink, arrivalTime, null, null);

            for (int t = 0; t < NUM_TARGET_NODES; t++) {
                Link srcLink = linkList.get(rng.nextInt(n));
                int srcNodeIdx = srcLink.getToNode().getId().index();

                double chCost = chTree.getCost(srcNodeIdx);
                double dijCost = dijkstraTree.getCost(srcNodeIdx);

                boolean chUnreach = Double.isInfinite(chCost) || Double.isNaN(chCost);
                boolean dijUnreach = Double.isInfinite(dijCost) || Double.isNaN(dijCost);
                if (chUnreach && dijUnreach) {
                    comparisons++;
                    continue;
                }
                if (chUnreach || dijUnreach) {
                    if (!dijUnreach && chUnreach) mismatches++;
                    comparisons++;
                    continue;
                }

                comparisons++;
                if (Math.abs(chCost - dijCost) > COST_TOLERANCE) {
                    mismatches++;
                    System.err.printf("STATIC BACKWARD MISMATCH src=%s arr=%s: CH=%.6f  Dijkstra=%.6f%n",
                            srcLink.getId(), arrLink.getId(), chCost, dijCost);
                }
            }
        }

        Assertions.assertEquals(0, mismatches,
                mismatches + " static-backward cost mismatches out of " + comparisons + " comparisons.");
    }
}
