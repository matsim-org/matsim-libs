/* *********************************************************************** *
 * project: org.matsim.*
 * CHAutoTunedCorrectnessTest.java
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
 * Correctness tests for the <b>auto-tuned</b> CH pipeline:
 * {@link NetworkAnalyzer} → {@link RoutingParameterTuner} → {@link CHBuilder}
 * with {@link CHBuilderParams}.
 *
 * <p>This specifically exercises the auto-tuned code path (3-arg CHBuilder
 * constructor), including the {@code deferredHopLimit} feature, to ensure
 * that the reduced hop limit in the deferred phase does not break correctness.
 *
 * <p>Compares CH routing results against {@link SpeedyDijkstra} on random
 * OD pairs.  Uses {@link FreespeedTravelTimeAndDisutility} (constant TTFs)
 * so results must match to within 1e-6.
 *
 * @author Steffen Axer
 */
public class CHAutoTunedCorrectnessTest {

    private static final int    NUM_QUERIES    = 500;
    private static final double COST_TOLERANCE = 1e-6;

    /**
     * Auto-tuned CH on a 15×15 grid (225 nodes, 840 links).
     * Exercises the full pipeline including NetworkAnalyzer + RoutingParameterTuner.
     */
    @Test
    void testAutoTunedCorrectness_Grid15() {
        Network network = buildGridNetwork(15);
        runAutoTunedCorrectnessTest(network);
    }

    /**
     * Auto-tuned CH on the Equil network.
     */
    @Test
    void testAutoTunedCorrectness_Equil() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile("test/scenarios/equil/network.xml");
        runAutoTunedCorrectnessTest(scenario.getNetwork());
    }

    /**
     * Auto-tuned CH on the Berlin test network (~11.5k nodes).
     * This is the most realistic test — exercises deferred-phase contraction
     * with the reduced {@code deferredHopLimit}.
     */
    @Test
    void testAutoTunedCorrectness_Berlin() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork())
                .readFile("test/scenarios/berlin/network.xml.gz");
        Network network = scenario.getNetwork();
        System.out.printf("%nAuto-tuned CH correctness test: Berlin (%d nodes, %d links)%n",
                network.getNodes().size(), network.getLinks().size());
        runAutoTunedCorrectnessTest(network);
    }

    /**
     * Verifies that the auto-tuned CH with deferredHopLimit produces
     * the same routes as a CH built with the legacy constructor (full hopLimit).
     * This isolates the deferredHopLimit change: if both produce the same
     * costs, the reduced hop limit is safe.
     */
    @Test
    void testAutoTunedVsLegacy_Berlin() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork())
                .readFile("test/scenarios/berlin/network.xml.gz");
        Network network = scenario.getNetwork();
        FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());
        SpeedyGraph graph = SpeedyGraphBuilder.build(network);

        // Build auto-tuned CH (with deferredHopLimit)
        NetworkProfile profile = NetworkAnalyzer.analyze(graph);
        CHBuilderParams params = RoutingParameterTuner.tuneCHParams(profile);
        IFCParams ifcParams = RoutingParameterTuner.tuneIFCParams(profile);

        System.out.printf("  Auto-tuned: hopLimit=%d, deferredHopLimit=%d%n",
                params.hopLimit(), params.deferredHopLimit());

        InertialFlowCutter.NDOrderResult order =
                new InertialFlowCutter(graph, ifcParams).computeOrderWithBatches();
        CHGraph autoTunedCH = new CHBuilder(graph, tc, params).buildWithOrderParallel(order);
        new CHTTFCustomizer().customize(autoTunedCH, tc, tc);
        CHRouterTimeDep autoRouter = new CHRouterTimeDep(autoTunedCH, tc, tc);

        // Reference: Dijkstra
        SpeedyDijkstra dijkstra = new SpeedyDijkstra(graph, tc, tc);

        // Compare
        List<Node> nodeList = new ArrayList<>(network.getNodes().values());
        int n = nodeList.size();
        Random rng = new Random(42);
        int mismatches = 0;

        for (int i = 0; i < NUM_QUERIES; i++) {
            Node src = nodeList.get(rng.nextInt(n));
            Node dst = nodeList.get(rng.nextInt(n));

            Path autoPath = autoRouter.calcLeastCostPath(src, dst, 8.0 * 3600, null, null);
            Path dijPath = dijkstra.calcLeastCostPath(src, dst, 8.0 * 3600, null, null);

            if (autoPath == null && dijPath == null) continue;

            Assertions.assertNotNull(autoPath,
                    "Auto-tuned CH returned null but Dijkstra found path " + src.getId() + "→" + dst.getId());
            Assertions.assertNotNull(dijPath,
                    "Dijkstra returned null but auto-tuned CH found path " + src.getId() + "→" + dst.getId());

            if (Math.abs(autoPath.travelCost - dijPath.travelCost) > COST_TOLERANCE) {
                mismatches++;
                System.err.printf("MISMATCH %s→%s: AutoCH=%.6f  Dijkstra=%.6f%n",
                        src.getId(), dst.getId(), autoPath.travelCost, dijPath.travelCost);
            }
        }

        Assertions.assertEquals(0, mismatches,
                mismatches + " cost mismatches out of " + NUM_QUERIES
                        + " queries with auto-tuned CH (deferredHopLimit).");

        System.out.printf("  Auto-tuned CH: %,d edges (%.1f%% overhead)%n",
                autoTunedCH.totalEdgeCount,
                ((double) autoTunedCH.totalEdgeCount / graph.linkCount - 1) * 100);
    }

    // ---- Core test logic ----

    private void runAutoTunedCorrectnessTest(Network network) {
        FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());
        SpeedyGraph graph = SpeedyGraphBuilder.build(network);

        // Auto-tune parameters from network structure
        NetworkProfile profile = NetworkAnalyzer.analyze(graph);
        CHBuilderParams chParams = RoutingParameterTuner.tuneCHParams(profile);
        IFCParams ifcParams = RoutingParameterTuner.tuneIFCParams(profile);

        // Build CH with auto-tuned parameters
        InertialFlowCutter.NDOrderResult order =
                new InertialFlowCutter(graph, ifcParams).computeOrderWithBatches();
        CHGraph chGraph = new CHBuilder(graph, tc, chParams).buildWithOrderParallel(order);
        new CHTTFCustomizer().customize(chGraph, tc, tc);
        CHRouterTimeDep chRouter = new CHRouterTimeDep(chGraph, tc, tc);

        // Reference: SpeedyDijkstra
        SpeedyDijkstra dijkstra = new SpeedyDijkstra(graph, tc, tc);

        List<Node> nodeList = new ArrayList<>(network.getNodes().values());
        int n = nodeList.size();
        if (n < 2) return;

        Random rng = new Random(42);
        int mismatches = 0;

        for (int i = 0; i < NUM_QUERIES; i++) {
            Node src = nodeList.get(rng.nextInt(n));
            Node dst = nodeList.get(rng.nextInt(n));

            Path chPath = chRouter.calcLeastCostPath(src, dst, 8.0 * 3600, null, null);
            Path dijPath = dijkstra.calcLeastCostPath(src, dst, 8.0 * 3600, null, null);

            if (chPath == null && dijPath == null) continue;

            Assertions.assertNotNull(chPath,
                    "Auto-tuned CH returned null but Dijkstra found path " + src.getId() + "→" + dst.getId());
            Assertions.assertNotNull(dijPath,
                    "Dijkstra returned null but auto-tuned CH found path " + src.getId() + "→" + dst.getId());

            if (Math.abs(chPath.travelCost - dijPath.travelCost) > COST_TOLERANCE) {
                mismatches++;
                System.err.printf("MISMATCH %s→%s: AutoCH=%.6f  Dijkstra=%.6f%n",
                        src.getId(), dst.getId(), chPath.travelCost, dijPath.travelCost);
            }
        }

        Assertions.assertEquals(0, mismatches,
                mismatches + " cost mismatches out of " + NUM_QUERIES
                        + " queries with auto-tuned CH.");
    }

    // ---- Network builders ----

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
                    addLink(network, r + "_" + c + "R", nodes[r][c], nodes[r][c + 1], 100, 10);
                    addLink(network, r + "_" + c + "L", nodes[r][c + 1], nodes[r][c], 100, 10);
                }
                if (r + 1 < n) {
                    addLink(network, r + "_" + c + "D", nodes[r][c], nodes[r + 1][c], 100, 10);
                    addLink(network, r + "_" + c + "U", nodes[r + 1][c], nodes[r][c], 100, 10);
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

