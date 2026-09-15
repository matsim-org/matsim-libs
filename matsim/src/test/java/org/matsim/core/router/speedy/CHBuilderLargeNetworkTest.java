/* *********************************************************************** *
 * project: org.matsim.*
 * CHBuilderLargeNetworkTest.java
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
 * Large-scale tests for nested-dissection-ordered CH contraction
 * ({@link InertialFlowCutter} + {@link CHBuilder#buildWithOrder}).
 *
 * <p>Tests on real and large synthetic networks to evaluate correctness
 * at realistic scales:
 * <ul>
 *   <li><b>Real networks</b>: Berlin (11.5k nodes)</li>
 *   <li><b>Synthetic large grids</b>: up to 22,500 nodes with perturbed coordinates</li>
 *   <li><b>Synthetic hub networks</b>: grids with high-degree PT hub nodes</li>
 * </ul>
 *
 * @author Steffen Axer
 */
public class CHBuilderLargeNetworkTest {

    private static final int    NUM_QUERIES    = 500;
    private static final double COST_TOLERANCE = 1e-6;

    // -----------------------------------------------------------------------
    // Correctness tests on real natural road networks
    // -----------------------------------------------------------------------

    /**
     * Berlin road network: 11,566 nodes, 27,664 links.
     * Same network used in {@link CHBuilderNDTest} but tested here
     * as part of the large-network suite.
     */
    @Test
    void testNDCorrectnessBerlinNetwork() {
        Network network = loadNetwork("test/scenarios/berlin/network.xml.gz");
        System.out.printf("Berlin network: %,d nodes, %,d links%n",
                network.getNodes().size(), network.getLinks().size());
        runCorrectnessTest(network);
    }

    // -----------------------------------------------------------------------
    // Large synthetic network tests
    // -----------------------------------------------------------------------

    /**
     * 100×100 perturbed grid (10,000 nodes, ~39,600 links).
     * Coordinates are randomly shifted to break regularity – closer to a real road network.
     */
    @Test
    void testNDCorrectnessLargeGrid100x100() {
        Network network = buildPerturbedGrid(100, 42);
        System.out.printf("100x100 perturbed grid: %,d nodes, %,d links%n",
                network.getNodes().size(), network.getLinks().size());
        runCorrectnessTest(network);
    }

    /**
     * 150×150 perturbed grid (22,500 nodes, ~89,400 links) — large synthetic network.
     * Tests correctness with 500 random OD pairs.
     */
    @Test
    void testNDCorrectnessLargeGrid150x150() {
        Network network = buildPerturbedGrid(150, 42);
        System.out.printf("150x150 perturbed grid: %,d nodes, %,d links%n",
                network.getNodes().size(), network.getLinks().size());
        runCorrectnessTest(network);
    }

    /**
     * Grid with high-degree hub nodes simulating PT stops.
     *
     * <p>This test targets the edge explosion scenario observed on the 532k-node
     * Metropole Ruhr network: high-degree hub nodes (PT stops) in separator cells
     * caused cascading edge inflation (1.2M → 4M edges in one ND round) before
     * the adaptive contraction fix with lazy priority updates.
     *
     * <p>The test builds a 50×50 grid (~2,500 road nodes) with 20 hub nodes
     * that each connect to 10 surrounding road nodes (simulating PT stops).
     * This creates local clusters of high-degree nodes that stress the ND
     * separator contraction.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>The CH builds without edge explosion (edge count stays reasonable)</li>
     *   <li>500 random OD pair correctness checks pass vs Dijkstra</li>
     * </ul>
     */
    @Test
    void testNDCorrectnessGridWithHubs() {
        Network network = buildGridWithHubs(50, 20, 10, 42);
        System.out.printf("50x50 grid with hubs: %,d nodes, %,d links%n",
                network.getNodes().size(), network.getLinks().size());

        FreespeedTravelTimeAndDisutility tc =
                new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());
        SpeedyGraph baseGraph = SpeedyGraphBuilder.build(network);

        // Build ND-ordered CH and verify edge count is reasonable
        int[] order = new InertialFlowCutter(baseGraph).computeOrder();
        CHGraph chGraph = new CHBuilder(baseGraph, tc).buildWithOrder(order);
        new CHTTFCustomizer().customize(chGraph, tc, tc);

        // The base graph has linkCount links. A well-built CH should have
        // at most ~8× the original edges. Without the adaptive fix, hub
        // networks can explode to 20×+ or worse.
        int maxEdges = baseGraph.linkCount * 8;
        System.out.printf("  Base links: %,d  CH edges: %,d  ratio: %.1fx%n",
                baseGraph.linkCount, chGraph.totalEdgeCount,
                (double) chGraph.totalEdgeCount / baseGraph.linkCount);
        Assertions.assertTrue(chGraph.totalEdgeCount < maxEdges,
                "Edge explosion detected: " + chGraph.totalEdgeCount + " CH edges for "
                        + baseGraph.linkCount + " base links (ratio: "
                        + String.format("%.1f", (double) chGraph.totalEdgeCount / baseGraph.linkCount) + "×)");

        // Correctness test
        runCorrectnessTest(network);
    }

    /**
     * Parallel ND contraction variant of the hub test.
     * Verifies that parallel contraction also handles hubs correctly.
     */
    @Test
    void testNDCorrectnessGridWithHubsParallel() {
        Network network = buildGridWithHubs(50, 20, 10, 42);
        System.out.printf("50x50 grid with hubs (parallel): %,d nodes, %,d links%n",
                network.getNodes().size(), network.getLinks().size());

        FreespeedTravelTimeAndDisutility tc =
                new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());
        SpeedyGraph baseGraph = SpeedyGraphBuilder.build(network);

        // Build parallel ND-ordered CH
        InertialFlowCutter.NDOrderResult orderResult =
                new InertialFlowCutter(baseGraph).computeOrderWithBatches();
        CHGraph chGraph = new CHBuilder(baseGraph, tc).buildWithOrderParallel(orderResult);
        new CHTTFCustomizer().customize(chGraph, tc, tc);

        int maxEdges = baseGraph.linkCount * 8;
        System.out.printf("  Base links: %,d  CH edges: %,d  ratio: %.1fx%n",
                baseGraph.linkCount, chGraph.totalEdgeCount,
                (double) chGraph.totalEdgeCount / baseGraph.linkCount);
        Assertions.assertTrue(chGraph.totalEdgeCount < maxEdges,
                "Edge explosion detected (parallel): " + chGraph.totalEdgeCount
                        + " CH edges for " + baseGraph.linkCount + " base links");

        // Correctness: compare against Dijkstra
        CHRouterTimeDep chRouter = new CHRouterTimeDep(chGraph, tc, tc);
        SpeedyDijkstra dijkstra = new SpeedyDijkstra(baseGraph, tc, tc);
        List<Node> nodeList = new ArrayList<>(network.getNodes().values());
        int n = nodeList.size();
        Random rng = new Random(42);
        int mismatches = 0;
        for (int i = 0; i < NUM_QUERIES; i++) {
            Node src = nodeList.get(rng.nextInt(n));
            Node dst = nodeList.get(rng.nextInt(n));
            Path chPath  = chRouter.calcLeastCostPath(src, dst, 8.0 * 3600, null, null);
            Path dijPath = dijkstra.calcLeastCostPath(src, dst, 8.0 * 3600, null, null);
            if (chPath == null && dijPath == null) continue;
            Assertions.assertNotNull(chPath, "CH null but Dijkstra found path");
            Assertions.assertNotNull(dijPath, "Dijkstra null but CH found path");
            if (Math.abs(chPath.travelCost - dijPath.travelCost) > COST_TOLERANCE) {
                mismatches++;
            }
        }
        Assertions.assertEquals(0, mismatches,
                mismatches + " cost mismatches out of " + NUM_QUERIES + " queries (parallel).");
    }

    // -----------------------------------------------------------------------
    // Helper: run correctness test (compare ND-CH against Dijkstra)
    // -----------------------------------------------------------------------

    private void runCorrectnessTest(Network network) {
        FreespeedTravelTimeAndDisutility tc =
                new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());

        SpeedyGraph baseGraph = SpeedyGraphBuilder.build(network);

        // Build ND-ordered CH
        int[] order = new InertialFlowCutter(baseGraph).computeOrder();
        CHGraph chGraph = new CHBuilder(baseGraph, tc).buildWithOrder(order);
        new CHTTFCustomizer().customize(chGraph, tc, tc);
        CHRouterTimeDep chRouter = new CHRouterTimeDep(chGraph, tc, tc);

        // Reference: Dijkstra (exact baseline for correctness)
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
                    "ND-CH null but Dijkstra found path " + src.getId() + "→" + dst.getId());
            Assertions.assertNotNull(dijPath,
                    "Dijkstra null but ND-CH found path " + src.getId() + "→" + dst.getId());

            if (Math.abs(chPath.travelCost - dijPath.travelCost) > COST_TOLERANCE) {
                mismatches++;
                System.err.printf("MISMATCH %s→%s: ND-CH=%.6f  Dijkstra=%.6f%n",
                        src.getId(), dst.getId(), chPath.travelCost, dijPath.travelCost);
            }
        }

        Assertions.assertEquals(0, mismatches,
                mismatches + " cost mismatches out of " + NUM_QUERIES + " queries.");
    }

    // -----------------------------------------------------------------------
    // Helper: load network from file
    // -----------------------------------------------------------------------

    private static Network loadNetwork(String path) {
        Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(s.getNetwork()).readFile(path);
        return s.getNetwork();
    }

    // -----------------------------------------------------------------------
    // Helper: build large perturbed grid network
    // -----------------------------------------------------------------------

    /**
     * Builds an n×n grid with randomly perturbed coordinates and variable link speeds.
     * This is much more realistic than a regular grid for testing graph partitioning.
     *
     * @param n    grid dimension (n×n nodes)
     * @param seed random seed for reproducibility
     */
    private static Network buildPerturbedGrid(int n, long seed) {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Network network = scenario.getNetwork();
        NetworkFactory nf = network.getFactory();
        Random rng = new Random(seed);

        double spacing = 200.0; // 200m base spacing

        Node[][] nodes = new Node[n][n];
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                // Perturb coordinates by up to 30% of spacing
                double px = c * spacing + (rng.nextDouble() - 0.5) * spacing * 0.6;
                double py = r * spacing + (rng.nextDouble() - 0.5) * spacing * 0.6;
                Node node = nf.createNode(Id.createNodeId(r + "_" + c), new Coord(px, py));
                network.addNode(node);
                nodes[r][c] = node;
            }
        }

        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                // Horizontal links (right + left)
                if (c + 1 < n) {
                    double len = distance(nodes[r][c], nodes[r][c + 1]);
                    double speed = 8.0 + rng.nextDouble() * 22.0; // 8-30 m/s
                    addLink(network, r + "_" + c + "R", nodes[r][c], nodes[r][c + 1], len, speed);
                    addLink(network, r + "_" + c + "L", nodes[r][c + 1], nodes[r][c], len, speed);
                }
                // Vertical links (down + up)
                if (r + 1 < n) {
                    double len = distance(nodes[r][c], nodes[r + 1][c]);
                    double speed = 8.0 + rng.nextDouble() * 22.0;
                    addLink(network, r + "_" + c + "D", nodes[r][c], nodes[r + 1][c], len, speed);
                    addLink(network, r + "_" + c + "U", nodes[r + 1][c], nodes[r][c], len, speed);
                }
            }
        }
        return network;
    }

    private static double distance(Node a, Node b) {
        double dx = a.getCoord().getX() - b.getCoord().getX();
        double dy = a.getCoord().getY() - b.getCoord().getY();
        return Math.max(1.0, Math.sqrt(dx * dx + dy * dy));
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


    /**
     * Builds an n×n perturbed grid with additional high-degree hub nodes
     * simulating PT (public transport) stops.
     *
     * <p>Hub nodes are placed at evenly spaced grid positions and connected
     * to nearby road nodes with bidirectional links at lower speed (simulating
     * PT access links).  This creates localized clusters of high-degree nodes
     * that stress the ND separator contraction — similar to the structure
     * of the Metropole Ruhr network where PT hub stops caused edge explosion.
     *
     * @param gridSize    grid dimension (gridSize×gridSize road nodes)
     * @param numHubs     number of hub nodes to add
     * @param hubDegree   number of road nodes each hub connects to
     * @param seed        random seed
     */
    private static Network buildGridWithHubs(int gridSize, int numHubs, int hubDegree, long seed) {
        // Build base grid
        Network network = buildPerturbedGrid(gridSize, seed);
        NetworkFactory nf = network.getFactory();
        Random rng = new Random(seed + 7919);

        List<Node> gridNodes = new ArrayList<>(network.getNodes().values());
        double spacing = 200.0;

        // Add hub nodes evenly distributed across the grid
        int hubSpacing = Math.max(1, gridSize / (int) Math.ceil(Math.sqrt(numHubs)));
        int hubIdx = 0;
        for (int r = hubSpacing / 2; r < gridSize && hubIdx < numHubs; r += hubSpacing) {
            for (int c = hubSpacing / 2; c < gridSize && hubIdx < numHubs; c += hubSpacing) {
                // Hub position near grid point (r,c) with small offset
                double hx = c * spacing + (rng.nextDouble() - 0.5) * spacing * 0.3;
                double hy = r * spacing + (rng.nextDouble() - 0.5) * spacing * 0.3;
                Node hub = nf.createNode(Id.createNodeId("hub_" + hubIdx), new Coord(hx, hy));
                network.addNode(hub);

                // Connect hub to nearby grid nodes — sort by distance and take closest hubDegree
                gridNodes.sort((a, b) -> {
                    double da = Math.pow(a.getCoord().getX() - hx, 2) + Math.pow(a.getCoord().getY() - hy, 2);
                    double db = Math.pow(b.getCoord().getX() - hx, 2) + Math.pow(b.getCoord().getY() - hy, 2);
                    return Double.compare(da, db);
                });

                int connections = Math.min(hubDegree, gridNodes.size());
                for (int k = 0; k < connections; k++) {
                    Node target = gridNodes.get(k);
                    double len = distance(hub, target);
                    double speed = 3.0 + rng.nextDouble() * 5.0; // 3-8 m/s (walking/PT access speed)
                    String linkId = "hub" + hubIdx + "_" + k;
                    addLink(network, linkId + "F", hub, target, len, speed);
                    addLink(network, linkId + "B", target, hub, len, speed);
                }

                // Add inter-hub links (simulating PT lines connecting stops)
                // Connect to 2-3 other hubs to create PT corridors
                hubIdx++;
            }
        }

        // Connect hubs to each other in a chain and with some cross-links
        List<Node> hubs = new ArrayList<>();
        for (int h = 0; h < hubIdx; h++) {
            hubs.add(network.getNodes().get(Id.createNodeId("hub_" + h)));
        }
        for (int h = 0; h < hubs.size() - 1; h++) {
            Node a = hubs.get(h);
            Node b = hubs.get(h + 1);
            double len = distance(a, b);
            addLink(network, "hubline_" + h + "F", a, b, len, 15.0);
            addLink(network, "hubline_" + h + "B", b, a, len, 15.0);
            // Cross-link every 3rd hub to a hub 2 positions ahead
            if (h + 2 < hubs.size() && h % 3 == 0) {
                Node c = hubs.get(h + 2);
                double len2 = distance(a, c);
                addLink(network, "hubcross_" + h + "F", a, c, len2, 12.0);
                addLink(network, "hubcross_" + h + "B", c, a, len2, 12.0);
            }
        }

        return network;
    }
}
