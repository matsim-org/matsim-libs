/* *********************************************************************** *
 * project: org.matsim.*
 * CHBuilderNDTest.java
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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Tests for nested-dissection-ordered CH contraction ({@link InertialFlowCutter}
 * + {@link CHBuilder#buildWithOrder}).
 *
 * <p>Verifies that the ND-ordered CH produces correct shortest paths by comparing
 * against {@link SpeedyDijkstra} on random OD pairs.
 *
 * @author Steffen Axer
 */
public class CHBuilderNDTest {

    private static final int    NUM_QUERIES    = 500;
    private static final double COST_TOLERANCE = 1e-6;

    // ---- correctness tests ----

    @Test
    void testNDOrderLinearNetwork() {
        Network network = buildLinearNetwork(10);
        FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());

        SpeedyGraph g = SpeedyGraphBuilder.build(network);
        int[] order = new InertialFlowCutter(g).computeOrder();
        CHGraph ch = new CHBuilder(g, tc).buildWithOrder(order);

        Assertions.assertNotNull(ch);
        Assertions.assertEquals(g.nodeCount, ch.nodeCount);
        Assertions.assertTrue(ch.totalEdgeCount >= g.linkCount,
                "CH should have at least as many edges as the base graph");
    }

    @Test
    void testNDOrderTriangleNetwork() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Network network = scenario.getNetwork();
        NetworkFactory nf = network.getFactory();

        Node nA = nf.createNode(Id.createNodeId("A"), new Coord(0, 0));
        Node nB = nf.createNode(Id.createNodeId("B"), new Coord(100, 0));
        Node nC = nf.createNode(Id.createNodeId("C"), new Coord(200, 0));
        network.addNode(nA); network.addNode(nB); network.addNode(nC);

        addLink(network, "AB", nA, nB, 100, 10);
        addLink(network, "BC", nB, nC, 100, 10);
        addLink(network, "AC", nA, nC, 300, 10);

        FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());
        SpeedyGraph g = SpeedyGraphBuilder.build(network);
        int[] order = new InertialFlowCutter(g).computeOrder();
        CHGraph ch = new CHBuilder(g, tc).buildWithOrder(order);
        new CHTTFCustomizer().customize(ch, tc, tc);

        CHRouterTimeDep router = new CHRouterTimeDep(ch, tc, tc);
        Path path = router.calcLeastCostPath(nA, nC, 0, null, null);

        Assertions.assertNotNull(path, "Path should not be null");
        Assertions.assertEquals(2, path.links.size(), "Expected path A→B→C (2 links)");
    }

    @Test
    void testNDOrderCorrectnessSmallGrid() {
        Network network = buildGridNetwork(5);
        runCorrectnessTest(network);
    }

    @Test
    void testNDOrderCorrectnessEquilNetwork() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile("test/scenarios/equil/network.xml");
        runCorrectnessTest(scenario.getNetwork());
    }

    @Test
    void testNDOrderCorrectnessLargerGrid() {
        Network network = buildGridNetwork(10);
        runCorrectnessTest(network);
    }

    // ---- parallel contraction tests ----

    @Test
    void testParallelContractionSmallGrid() {
        Network network = buildGridNetwork(5);
        runParallelCorrectnessTest(network);
    }

    @Test
    void testParallelContractionLargerGrid() {
        Network network = buildGridNetwork(15);
        runParallelCorrectnessTest(network);
    }

    @Test
    void testParallelContractionEquilNetwork() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile("test/scenarios/equil/network.xml");
        runParallelCorrectnessTest(scenario.getNetwork());
    }

    @Test
    void testParallelContractionBerlinNetwork() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork())
                .readFile("test/scenarios/berlin/network.xml.gz");
        Network network = scenario.getNetwork();
        System.out.printf("%nParallel contraction test: Berlin network (%d nodes, %d links)%n",
                network.getNodes().size(), network.getLinks().size());
        runParallelCorrectnessTest(network);
    }

    // ---- benchmark test ----

    @Test
    void benchmarkNDvsWitnessBasedOrdering() {
        // Use a moderately large grid to see timing differences
        int gridSize = 20;
        Network network = buildGridNetwork(gridSize);
        FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());
        int nodeCount = network.getNodes().size();

        System.out.printf("%n=== CH Build Benchmark (%dx%d grid, %d nodes) ===%n",
                gridSize, gridSize, nodeCount);

        // Warm up JVM
        {
            SpeedyGraph g = SpeedyGraphBuilder.build(network);
            new CHBuilder(g, tc).build();
        }

        // Benchmark witness-based ordering
        long witnessStart = System.nanoTime();
        SpeedyGraph g1 = SpeedyGraphBuilder.build(network);
        CHGraph ch1 = new CHBuilder(g1, tc).build();
        long witnessMs = (System.nanoTime() - witnessStart) / 1_000_000;

        // Benchmark ND ordering
        long ndStart = System.nanoTime();
        SpeedyGraph g2 = SpeedyGraphBuilder.build(network);
        long ndOrderStart = System.nanoTime();
        int[] order = new InertialFlowCutter(g2).computeOrder();
        long ndOrderMs = (System.nanoTime() - ndOrderStart) / 1_000_000;
        CHGraph ch2 = new CHBuilder(g2, tc).buildWithOrder(order);
        long ndTotalMs = (System.nanoTime() - ndStart) / 1_000_000;

        System.out.printf("  Witness-based: %d ms, %d total edges%n", witnessMs, ch1.totalEdgeCount);
        System.out.printf("  ND ordering:   %d ms total (%d ms ordering + %d ms contraction), %d total edges%n",
                ndTotalMs, ndOrderMs, ndTotalMs - ndOrderMs, ch2.totalEdgeCount);
        System.out.printf("  Speedup:       %.2fx%n", (double) witnessMs / Math.max(1, ndTotalMs));
        System.out.printf("  Edge overhead: %.1f%%%n",
                ((double) ch2.totalEdgeCount / Math.max(1, ch1.totalEdgeCount) - 1) * 100);
        System.out.println();

        // Both should produce valid CH
        Assertions.assertTrue(ch1.totalEdgeCount >= g1.linkCount);
        Assertions.assertTrue(ch2.totalEdgeCount >= g2.linkCount);
    }

    // ---- large natural road network tests ----

    /**
     * Correctness test on the Berlin road network (~11.5k nodes, ~27.6k links).
     * Runs 500 random OD pairs comparing ND-ordered CH against Dijkstra.
     */
    @Test
    void testNDOrderCorrectnessBerlinNetwork() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork())
                .readFile("test/scenarios/berlin/network.xml.gz");
        Network network = scenario.getNetwork();
        System.out.printf("%nBerlin network: %d nodes, %d links%n",
                network.getNodes().size(), network.getLinks().size());
        runCorrectnessTest(network);
    }

    /**
     * Benchmark ND vs witness-based ordering on the Berlin road network.
     * This is a realistic-scale test with a natural road graph topology.
     */
    @Test
    void benchmarkNDvsWitnessBasedBerlinNetwork() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork())
                .readFile("test/scenarios/berlin/network.xml.gz");
        Network network = scenario.getNetwork();
        int nodeCount = network.getNodes().size();
        int linkCount = network.getLinks().size();
        FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());

        System.out.printf("%n=== CH Build Benchmark (Berlin network: %d nodes, %d links) ===%n",
                nodeCount, linkCount);

        // Warm up JVM with a smaller build
        {
            SpeedyGraph g = SpeedyGraphBuilder.build(network);
            new CHBuilder(g, tc).build();
        }

        // Benchmark witness-based ordering
        long witnessStart = System.nanoTime();
        SpeedyGraph g1 = SpeedyGraphBuilder.build(network);
        CHGraph ch1 = new CHBuilder(g1, tc).build();
        long witnessMs = (System.nanoTime() - witnessStart) / 1_000_000;

        // Benchmark ND ordering
        long ndStart = System.nanoTime();
        SpeedyGraph g2 = SpeedyGraphBuilder.build(network);
        long ndOrderStart = System.nanoTime();
        int[] order = new InertialFlowCutter(g2).computeOrder();
        long ndOrderMs = (System.nanoTime() - ndOrderStart) / 1_000_000;
        long ndContractStart = System.nanoTime();
        CHGraph ch2 = new CHBuilder(g2, tc).buildWithOrder(order);
        long ndContractMs = (System.nanoTime() - ndContractStart) / 1_000_000;
        long ndTotalMs = (System.nanoTime() - ndStart) / 1_000_000;

        System.out.printf("  Witness-based:  %,d ms, %,d total edges%n", witnessMs, ch1.totalEdgeCount);
        System.out.printf("  ND total:       %,d ms, %,d total edges%n", ndTotalMs, ch2.totalEdgeCount);
        System.out.printf("    ND ordering:  %,d ms%n", ndOrderMs);
        System.out.printf("    ND contract:  %,d ms%n", ndContractMs);
        System.out.printf("  Speedup:        %.2fx%n", (double) witnessMs / Math.max(1, ndTotalMs));
        System.out.printf("  Edge overhead:  %.1f%%%n",
                ((double) ch2.totalEdgeCount / Math.max(1, ch1.totalEdgeCount) - 1) * 100);
        System.out.println();

        // Verify correctness of ND-built CH on random queries
        new CHTTFCustomizer().customize(ch2, tc, tc);
        CHRouterTimeDep ndRouter = new CHRouterTimeDep(ch2, tc, tc);
        SpeedyDijkstra dijkstra = new SpeedyDijkstra(g2, tc, tc);

        List<Node> nodeList = new ArrayList<>(network.getNodes().values());
        int n = nodeList.size();
        Random rng = new Random(42);
        int mismatches = 0;

        for (int i = 0; i < NUM_QUERIES; i++) {
            Node src = nodeList.get(rng.nextInt(n));
            Node dst = nodeList.get(rng.nextInt(n));

            Path ndPath  = ndRouter.calcLeastCostPath(src, dst, 8.0 * 3600, null, null);
            Path dijPath = dijkstra.calcLeastCostPath(src, dst, 8.0 * 3600, null, null);

            if (ndPath == null && dijPath == null) continue;

            Assertions.assertNotNull(ndPath,
                    "ND-CH returned null but Dijkstra found path " + src.getId() + "→" + dst.getId());
            Assertions.assertNotNull(dijPath,
                    "Dijkstra returned null but ND-CH found path " + src.getId() + "→" + dst.getId());

            if (Math.abs(ndPath.travelCost - dijPath.travelCost) > COST_TOLERANCE) {
                mismatches++;
                System.err.printf("MISMATCH %s→%s: ND-CH=%.6f  Dijkstra=%.6f%n",
                        src.getId(), dst.getId(), ndPath.travelCost, dijPath.travelCost);
            }
        }

        Assertions.assertEquals(0, mismatches,
                mismatches + "/" + NUM_QUERIES + " Berlin queries had cost mismatches.");

        // Both should produce valid CH
        Assertions.assertTrue(ch1.totalEdgeCount >= g1.linkCount);
        Assertions.assertTrue(ch2.totalEdgeCount >= g2.linkCount);
    }

    // ---- helpers ----

    private void runCorrectnessTest(Network network) {
        FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());

        SpeedyGraph baseGraph = SpeedyGraphBuilder.build(network);

        // Build ND-ordered CATCHUp router
        int[] order = new InertialFlowCutter(baseGraph).computeOrder();
        CHGraph chGraph = new CHBuilder(baseGraph, tc).buildWithOrder(order);
        new CHTTFCustomizer().customize(chGraph, tc, tc);
        CHRouterTimeDep chRouter = new CHRouterTimeDep(chGraph, tc, tc);

        // Reference: SpeedyDijkstra
        SpeedyDijkstra dijkstra = new SpeedyDijkstra(baseGraph, tc, tc);

        List<Node> nodeList = new ArrayList<>(network.getNodes().values());
        int n = nodeList.size();
        if (n < 2) return;

        Random rng = new Random(42);
        int mismatches = 0;

        for (int i = 0; i < NUM_QUERIES; i++) {
            Node src = nodeList.get(rng.nextInt(n));
            Node dst = nodeList.get(rng.nextInt(n));

            Path chPath  = chRouter.calcLeastCostPath(src, dst, 8.0 * 3600, null, null);
            Path dijPath = dijkstra.calcLeastCostPath(src, dst, 8.0 * 3600, null, null);

            if (chPath == null && dijPath == null) continue;

            Assertions.assertNotNull(chPath,
                    "ND-CH returned null but Dijkstra found a path from "
                            + src.getId() + " to " + dst.getId());
            Assertions.assertNotNull(dijPath,
                    "Dijkstra returned null but ND-CH found a path from "
                            + src.getId() + " to " + dst.getId());

            double chCost  = chPath.travelCost;
            double dijCost = dijPath.travelCost;

            if (Math.abs(chCost - dijCost) > COST_TOLERANCE) {
                mismatches++;
                System.err.printf("MISMATCH %s→%s: ND-CH=%.6f  Dijkstra=%.6f%n",
                        src.getId(), dst.getId(), chCost, dijCost);
            }
        }

        Assertions.assertEquals(0, mismatches,
                mismatches + " cost mismatches out of " + NUM_QUERIES
                        + " queries with ND-ordered CH.");
    }

    /**
     * Correctness test using the <b>parallel</b> contraction path
     * ({@link CHBuilder#buildWithOrderParallel}).
     * Compares 500 random OD pairs against Dijkstra.
     */
    private void runParallelCorrectnessTest(Network network) {
        FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());

        SpeedyGraph baseGraph = SpeedyGraphBuilder.build(network);

        // Build ND-ordered CH using PARALLEL contraction
        InertialFlowCutter.NDOrderResult orderResult =
                new InertialFlowCutter(baseGraph).computeOrderWithBatches();
        CHGraph chGraph = new CHBuilder(baseGraph, tc).buildWithOrderParallel(orderResult);
        new CHTTFCustomizer().customize(chGraph, tc, tc);
        CHRouterTimeDep chRouter = new CHRouterTimeDep(chGraph, tc, tc);

        // Reference: SpeedyDijkstra
        SpeedyDijkstra dijkstra = new SpeedyDijkstra(baseGraph, tc, tc);

        List<Node> nodeList = new ArrayList<>(network.getNodes().values());
        int n = nodeList.size();
        if (n < 2) return;

        Random rng = new Random(42);
        int mismatches = 0;

        for (int i = 0; i < NUM_QUERIES; i++) {
            Node src = nodeList.get(rng.nextInt(n));
            Node dst = nodeList.get(rng.nextInt(n));

            Path chPath  = chRouter.calcLeastCostPath(src, dst, 8.0 * 3600, null, null);
            Path dijPath = dijkstra.calcLeastCostPath(src, dst, 8.0 * 3600, null, null);

            if (chPath == null && dijPath == null) continue;

            Assertions.assertNotNull(chPath,
                    "Parallel-CH returned null but Dijkstra found a path from "
                            + src.getId() + " to " + dst.getId());
            Assertions.assertNotNull(dijPath,
                    "Dijkstra returned null but Parallel-CH found a path from "
                            + src.getId() + " to " + dst.getId());

            double chCost  = chPath.travelCost;
            double dijCost = dijPath.travelCost;

            if (Math.abs(chCost - dijCost) > COST_TOLERANCE) {
                mismatches++;
                System.err.printf("MISMATCH (parallel) %s→%s: CH=%.6f  Dijkstra=%.6f%n",
                        src.getId(), dst.getId(), chCost, dijCost);
            }
        }

        Assertions.assertEquals(0, mismatches,
                mismatches + " cost mismatches out of " + NUM_QUERIES
                        + " queries with parallel-contracted CH.");
    }

    private static Network buildLinearNetwork(int nodeCount) {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Network network = scenario.getNetwork();
        NetworkFactory nf = network.getFactory();

        Node[] nodes = new Node[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            nodes[i] = nf.createNode(Id.createNodeId(String.valueOf(i)), new Coord(i * 100, 0));
            network.addNode(nodes[i]);
        }
        for (int i = 0; i < nodeCount - 1; i++) {
            addLink(network, i + "->" + (i + 1), nodes[i], nodes[i + 1], 100, 10);
        }
        return network;
    }

    private static Network buildGridNetwork(int n) {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Network network = scenario.getNetwork();
        NetworkFactory nf = network.getFactory();

        Node[][] nodes = new Node[n][n];
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                Node node = nf.createNode(Id.createNodeId(r + "_" + c), new Coord(c * 100, r * 100));
                network.addNode(node);
                nodes[r][c] = node;
            }
        }
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                if (c + 1 < n) {
                    addLink(network, r + "_" + c + "R", nodes[r][c],     nodes[r][c + 1], 100, 10);
                    addLink(network, r + "_" + c + "L", nodes[r][c + 1], nodes[r][c],     100, 10);
                }
                if (r + 1 < n) {
                    addLink(network, r + "_" + c + "D", nodes[r][c],     nodes[r + 1][c], 100, 10);
                    addLink(network, r + "_" + c + "U", nodes[r + 1][c], nodes[r][c],     100, 10);
                }
            }
        }
        return network;
    }

    private static void addLink(Network network, String id, Node from, Node to,
                                double length, double freespeed) {
        NetworkFactory nf = network.getFactory();
        Link link = nf.createLink(Id.createLinkId(id), from, to);
        link.setLength(length);
        link.setFreespeed(freespeed);
        link.setCapacity(1800);
        link.setNumberOfLanes(1);
        network.addLink(link);
    }
}
