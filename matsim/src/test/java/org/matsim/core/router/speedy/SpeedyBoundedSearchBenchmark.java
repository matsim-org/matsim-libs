/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2026 by the members listed in the COPYING,        *
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Benchmark demonstrating the speedup of the bounded-search (maxCost) overload
 * on the speedy routers vs. the unbounded variant.
 *
 * <p>Disabled by default; enable with {@code -Dmatsim.benchmark=true}, e.g.
 * <pre>{@code
 *   mvn test -pl matsim -Dtest=SpeedyBoundedSearchBenchmark -Dmatsim.benchmark=true
 * }</pre>
 *
 * <p>Two scenarios are measured:
 * <ol>
 *   <li><b>Disconnected target</b>: a large NxN grid (reachable from source) plus
 *       a tiny separate component (where the target lives). The unbounded search
 *       must exhaust the full reachable component before returning {@code null};
 *       the bounded search returns {@code null} after exploring only the local
 *       ball of cost {@code maxCost}.</li>
 *   <li><b>Connected with cutoff below optimal</b>: source and target are both
 *       in the same NxN grid but at far corners; {@code maxCost} is set well
 *       below the optimal cost (the realistic pt2matsim case where a candidate
 *       pair exceeds {@code maxAllowedTravelCost}). The unbounded search
 *       explores up to the optimal cost before returning; the bounded search
 *       stops once {@code peek > maxCost}.</li>
 * </ol>
 *
 * <p>Each measurement runs {@code WARMUP_ITERATIONS} warmup calls + {@code MEASURE_ITERATIONS}
 * measured calls and reports mean time per call and the bounded/unbounded speedup factor.
 * No assertions are made on timing (results are environment-dependent); failures here
 * indicate a correctness regression, not a perf regression.
 */
@EnabledIfSystemProperty(named = "matsim.benchmark", matches = "true")
public class SpeedyBoundedSearchBenchmark {

	private static final int GRID_N = 80; // 80x80 = 6400 nodes in the main component
	private static final double LINK_LENGTH = 100.0;
	private static final double LINK_FREESPEED = 10.0;
	private static final int WARMUP_ITERATIONS = 200;
	private static final int MEASURE_ITERATIONS = 1000;

	/**
	 * Bounded {@code maxCost} expressed as a fraction of the unbounded optimal cost.
	 * 0.1 means "set the cutoff at 10% of the true optimum" so bounded should bail
	 * after exploring only a small fraction of the reachable component.
	 */
	private static final double CUTOFF_FRACTION = 0.1;

	@Test
	void disconnectedTarget_dijkstra() {
		Network network = buildGridPlusDisconnectedIsland(GRID_N);
		Link sourceLink = network.getLinks().get(Id.createLinkId("g_0_0_E"));
		Link reachableFarLink = network.getLinks().get(Id.createLinkId("g_" + (GRID_N - 2) + "_" + (GRID_N - 1) + "_E"));
		Link targetLink = network.getLinks().get(Id.createLinkId("island_AB"));

		SpeedyDijkstra router = buildDijkstra(network);
		// Sample a representative cost (diameter of the reachable component) and cut at a fraction of it.
		Path reference = router.calcLeastCostPath(sourceLink, reachableFarLink, 0, null, null);
		double maxCost = reference.travelCost * CUTOFF_FRACTION;
		runBenchmark("SpeedyDijkstra / disconnected target / grid " + GRID_N + "x" + GRID_N
				+ " / cutoff=" + String.format("%.1f", maxCost),
				() -> router.calcLeastCostPath(sourceLink, targetLink, 0, null, null),
				() -> router.calcLeastCostPath(sourceLink, targetLink, 0, null, null, maxCost),
				true);
	}

	@Test
	void disconnectedTarget_alt() {
		Network network = buildGridPlusDisconnectedIsland(GRID_N);
		Link sourceLink = network.getLinks().get(Id.createLinkId("g_0_0_E"));
		Link reachableFarLink = network.getLinks().get(Id.createLinkId("g_" + (GRID_N - 2) + "_" + (GRID_N - 1) + "_E"));
		Link targetLink = network.getLinks().get(Id.createLinkId("island_AB"));

		SpeedyALT router = buildAlt(network);
		Path reference = router.calcLeastCostPath(sourceLink, reachableFarLink, 0, null, null);
		double maxCost = reference.travelCost * CUTOFF_FRACTION;
		runBenchmark("SpeedyALT      / disconnected target / grid " + GRID_N + "x" + GRID_N
				+ " / cutoff=" + String.format("%.1f", maxCost),
				() -> router.calcLeastCostPath(sourceLink, targetLink, 0, null, null),
				() -> router.calcLeastCostPath(sourceLink, targetLink, 0, null, null, maxCost),
				true);
	}

	@Test
	void cutoffBelowOptimal_dijkstra() {
		Network network = buildGrid(GRID_N);
		Link sourceLink = network.getLinks().get(Id.createLinkId("g_0_0_E"));
		// Target on far side of grid.
		Link targetLink = network.getLinks().get(Id.createLinkId("g_" + (GRID_N - 2) + "_" + (GRID_N - 1) + "_E"));

		SpeedyDijkstra router = buildDijkstra(network);
		Path reference = router.calcLeastCostPath(sourceLink, targetLink, 0, null, null);
		double maxCost = reference.travelCost * CUTOFF_FRACTION;
		runBenchmark("SpeedyDijkstra / cutoff below optimal / grid " + GRID_N + "x" + GRID_N
				+ " / cutoff=" + String.format("%.1f", maxCost) + " (optimal=" + String.format("%.1f", reference.travelCost) + ")",
				() -> router.calcLeastCostPath(sourceLink, targetLink, 0, null, null),
				() -> router.calcLeastCostPath(sourceLink, targetLink, 0, null, null, maxCost),
				false);
	}

	@Test
	void cutoffBelowOptimal_alt() {
		Network network = buildGrid(GRID_N);
		Link sourceLink = network.getLinks().get(Id.createLinkId("g_0_0_E"));
		Link targetLink = network.getLinks().get(Id.createLinkId("g_" + (GRID_N - 2) + "_" + (GRID_N - 1) + "_E"));

		SpeedyALT router = buildAlt(network);
		Path reference = router.calcLeastCostPath(sourceLink, targetLink, 0, null, null);
		double maxCost = reference.travelCost * CUTOFF_FRACTION;
		runBenchmark("SpeedyALT      / cutoff below optimal / grid " + GRID_N + "x" + GRID_N
				+ " / cutoff=" + String.format("%.1f", maxCost) + " (optimal=" + String.format("%.1f", reference.travelCost) + ")",
				() -> router.calcLeastCostPath(sourceLink, targetLink, 0, null, null),
				() -> router.calcLeastCostPath(sourceLink, targetLink, 0, null, null, maxCost),
				false);
	}

	/**
	 * Tight-cutoff scenario: cutoff is set at 95% of the optimal cost. Both the unbounded
	 * search and the bounded search end up exploring most of the relevant graph, so the
	 * dominant cost is the inner loop. This is the regime where edge-level pruning
	 * (skipping the heap insert when {@code newCost + h > maxCost}) is most visible.
	 */
	@Test
	void tightCutoff_dijkstra() {
		Network network = buildGrid(GRID_N);
		Link sourceLink = network.getLinks().get(Id.createLinkId("g_0_0_E"));
		Link targetLink = network.getLinks().get(Id.createLinkId("g_" + (GRID_N - 2) + "_" + (GRID_N - 1) + "_E"));

		SpeedyDijkstra router = buildDijkstra(network);
		Path reference = router.calcLeastCostPath(sourceLink, targetLink, 0, null, null);
		// 95% of optimal: bounded will explore most of the graph but still bail just short of the target.
		double maxCost = reference.travelCost * 0.95;
		runBenchmark("SpeedyDijkstra / tight cutoff (95%)    / grid " + GRID_N + "x" + GRID_N
				+ " / cutoff=" + String.format("%.2f", maxCost) + " (optimal=" + String.format("%.2f", reference.travelCost) + ")",
				() -> router.calcLeastCostPath(sourceLink, targetLink, 0, null, null),
				() -> router.calcLeastCostPath(sourceLink, targetLink, 0, null, null, maxCost),
				false);
	}

	@Test
	void tightCutoff_alt() {
		Network network = buildGrid(GRID_N);
		Link sourceLink = network.getLinks().get(Id.createLinkId("g_0_0_E"));
		Link targetLink = network.getLinks().get(Id.createLinkId("g_" + (GRID_N - 2) + "_" + (GRID_N - 1) + "_E"));

		SpeedyALT router = buildAlt(network);
		Path reference = router.calcLeastCostPath(sourceLink, targetLink, 0, null, null);
		double maxCost = reference.travelCost * 0.95;
		runBenchmark("SpeedyALT      / tight cutoff (95%)    / grid " + GRID_N + "x" + GRID_N
				+ " / cutoff=" + String.format("%.2f", maxCost) + " (optimal=" + String.format("%.2f", reference.travelCost) + ")",
				() -> router.calcLeastCostPath(sourceLink, targetLink, 0, null, null),
				() -> router.calcLeastCostPath(sourceLink, targetLink, 0, null, null, maxCost),
				false);
	}

	// ------------------------------------------------------------------
	// helpers
	// ------------------------------------------------------------------

	private static void runBenchmark(String label, java.util.function.Supplier<Path> unbounded,
			java.util.function.Supplier<Path> bounded, boolean expectNull) {
		// Warmup
		for (int i = 0; i < WARMUP_ITERATIONS; i++) {
			unbounded.get();
			bounded.get();
		}

		// Measure unbounded
		long t0 = System.nanoTime();
		Path lastUnbounded = null;
		for (int i = 0; i < MEASURE_ITERATIONS; i++) {
			lastUnbounded = unbounded.get();
		}
		long unboundedNanos = System.nanoTime() - t0;

		// Measure bounded
		long t1 = System.nanoTime();
		Path lastBounded = null;
		for (int i = 0; i < MEASURE_ITERATIONS; i++) {
			lastBounded = bounded.get();
		}
		long boundedNanos = System.nanoTime() - t1;

		double unboundedMsPerCall = unboundedNanos / 1e6 / MEASURE_ITERATIONS;
		double boundedMsPerCall = boundedNanos / 1e6 / MEASURE_ITERATIONS;
		double speedup = (double) unboundedNanos / boundedNanos;

		System.out.println();
		System.out.println("====================================================================");
		System.out.println(label);
		System.out.println("--------------------------------------------------------------------");
		System.out.printf("  unbounded: %8.3f ms/call (returned %s)%n",
				unboundedMsPerCall, lastUnbounded == null ? "null" : "path");
		System.out.printf("  bounded:   %8.3f ms/call (returned %s)%n",
				boundedMsPerCall, lastBounded == null ? "null" : "path");
		System.out.printf("  speedup:   %8.1fx%n", speedup);
		System.out.println("====================================================================");

		// Correctness check: both must agree on null-ness in the disconnected case;
		// in the cutoff-below-optimal case bounded must be null and unbounded must be non-null.
		if (expectNull) {
			if (lastUnbounded != null || lastBounded != null) {
				throw new AssertionError("disconnected target must produce null on both calls");
			}
		} else {
			if (lastUnbounded == null) {
				throw new AssertionError("unbounded must find the path");
			}
			if (lastBounded != null) {
				throw new AssertionError("bounded must return null when cutoff is below optimal");
			}
		}
	}

	private static SpeedyDijkstra buildDijkstra(Network network) {
		FreespeedTravelTimeAndDisutility td = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());
		SpeedyGraph graph = SpeedyGraphBuilder.build(network);
		return new SpeedyDijkstra(graph, td, td);
	}

	private static SpeedyALT buildAlt(Network network) {
		FreespeedTravelTimeAndDisutility td = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());
		SpeedyGraph graph = SpeedyGraphBuilder.build(network);
		SpeedyALTData altData = new SpeedyALTData(graph, 8, td, 4);
		return new SpeedyALT(altData, td, td);
	}

	/**
	 * Builds an NxN grid graph. Each interior node has 4 outgoing links (N/S/E/W). All
	 * links have {@link #LINK_LENGTH}m length and {@link #LINK_FREESPEED}m/s freespeed,
	 * so each link costs 10s of travel time under {@link FreespeedTravelTimeAndDisutility}.
	 * <p>
	 * Node ids: {@code "g_<x>_<y>"}. Eastbound link ids: {@code "g_<x>_<y>_E"}; westbound
	 * {@code _W}; northbound {@code _N}; southbound {@code _S}.
	 */
	private static Network buildGrid(int n) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		for (int x = 0; x < n; x++) {
			for (int y = 0; y < n; y++) {
				NetworkUtils.createAndAddNode(network, Id.createNodeId("g_" + x + "_" + y),
						new Coord(x * LINK_LENGTH, y * LINK_LENGTH));
			}
		}
		for (int x = 0; x < n; x++) {
			for (int y = 0; y < n; y++) {
				Node here = network.getNodes().get(Id.createNodeId("g_" + x + "_" + y));
				if (x + 1 < n) {
					Node east = network.getNodes().get(Id.createNodeId("g_" + (x + 1) + "_" + y));
					NetworkUtils.createAndAddLink(network, Id.createLinkId("g_" + x + "_" + y + "_E"),
							here, east, LINK_LENGTH, LINK_FREESPEED, 1000.0, 1.0);
					NetworkUtils.createAndAddLink(network, Id.createLinkId("g_" + (x + 1) + "_" + y + "_W"),
							east, here, LINK_LENGTH, LINK_FREESPEED, 1000.0, 1.0);
				}
				if (y + 1 < n) {
					Node north = network.getNodes().get(Id.createNodeId("g_" + x + "_" + (y + 1)));
					NetworkUtils.createAndAddLink(network, Id.createLinkId("g_" + x + "_" + y + "_N"),
							here, north, LINK_LENGTH, LINK_FREESPEED, 1000.0, 1.0);
					NetworkUtils.createAndAddLink(network, Id.createLinkId("g_" + x + "_" + (y + 1) + "_S"),
							north, here, LINK_LENGTH, LINK_FREESPEED, 1000.0, 1.0);
				}
			}
		}
		return network;
	}

	/**
	 * Grid as in {@link #buildGrid(int)} plus a tiny disconnected 2-node "island" component
	 * with link id {@code island_AB}. The grid is fully reachable from {@code g_0_0}; the
	 * island is unreachable from the grid (and vice versa).
	 */
	private static Network buildGridPlusDisconnectedIsland(int n) {
		Network network = buildGrid(n);
		Node islandA = NetworkUtils.createAndAddNode(network, Id.createNodeId("island_A"),
				new Coord(-10000.0, -10000.0));
		Node islandB = NetworkUtils.createAndAddNode(network, Id.createNodeId("island_B"),
				new Coord(-10000.0 + LINK_LENGTH, -10000.0));
		NetworkUtils.createAndAddLink(network, Id.createLinkId("island_AB"),
				islandA, islandB, LINK_LENGTH, LINK_FREESPEED, 1000.0, 1.0);
		return network;
	}
}
