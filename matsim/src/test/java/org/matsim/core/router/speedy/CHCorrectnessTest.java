/* *********************************************************************** *
 * project: org.matsim.*
 * CHCorrectnessTest.java
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
 * Correctness tests for the time-dependent CATCHUp router ({@link CHRouterTimeDep}).
 *
 * <p>Uses {@link FreespeedTravelTimeAndDisutility} which makes TTFs constant over time,
 * so the CATCHUp result must agree with {@link SpeedyDijkstra} to within 1e-6 cost.
 *
 * @author Steffen Axer
 */
public class CHCorrectnessTest {

    private static final int    NUM_QUERIES     = 500;
    private static final double COST_TOLERANCE  = 1e-6;

    @Test
    void testRandomODPairsEquilNetwork() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile("test/scenarios/equil/network.xml");
        runCorrectnessTest(scenario.getNetwork());
    }

    @Test
    void testRandomODPairsSmallGrid() {
        runCorrectnessTest(buildGridNetwork(5));
    }

    private void runCorrectnessTest(Network network) {
        FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());

        SpeedyGraph baseGraph = SpeedyGraphBuilder.build(network);

        // Build time-dependent CATCHUp router.
        CHGraph chGraph = new CHBuilder(baseGraph, tc).build();
        new CHTTFCustomizer().customize(chGraph, tc, tc);
        CHRouterTimeDep chRouter = new CHRouterTimeDep(chGraph, tc, tc);

        // Reference: SpeedyDijkstra.
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
                    "CHRouterTimeDep returned null but Dijkstra found a path from "
                            + src.getId() + " to " + dst.getId());
            Assertions.assertNotNull(dijPath,
                    "SpeedyDijkstra returned null but CHRouterTimeDep found a path from "
                            + src.getId() + " to " + dst.getId());

            double chCost  = chPath.travelCost;
            double dijCost = dijPath.travelCost;

            if (Math.abs(chCost - dijCost) > COST_TOLERANCE) {
                mismatches++;
                System.err.printf("MISMATCH %s→%s: CATCHUp=%.6f  Dijkstra=%.6f%n",
                        src.getId(), dst.getId(), chCost, dijCost);
            }
        }

        Assertions.assertEquals(0, mismatches,
                mismatches + " cost mismatches out of " + NUM_QUERIES + " queries.");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Builds an N×N bidirectional grid network. */
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

