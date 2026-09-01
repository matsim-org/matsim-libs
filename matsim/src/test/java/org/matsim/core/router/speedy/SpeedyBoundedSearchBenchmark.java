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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.BeforeAll;
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
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Benchmark for the bounded-search ({@code maxCost}) overload of the speedy routers.
 *
 * <p>Disabled by default; enable with {@code -Dmatsim.benchmark=true}:
 * <pre>{@code
 *   mvn test -pl matsim -Dtest=SpeedyBoundedSearchBenchmark -Dmatsim.benchmark=true
 * }</pre>
 *
 * <h2>Measurement methodology</h2>
 * <ul>
 *   <li><b>Realistic graph size</b>: a {@value #GRID_N}x{@value #GRID_N} grid
 *       (~{@code GRID_N*GRID_N} nodes, ~{@code 4*GRID_N*(GRID_N-1)} links). Large
 *       enough that CH preprocessing and queries do non-trivial work, while still
 *       deterministic and self-contained (no test resources).</li>
 *   <li><b>Randomized queries</b>: {@value #NUM_QUERIES} random (source, target)
 *       link pairs generated with a fixed seed. Iterations cycle through this set
 *       round-robin, preventing the JIT from over-specializing on a single
 *       (src, tgt) pair and giving realistic branch/cache behavior.</li>
 *   <li><b>Per-query cutoff</b>: each query's {@code maxCost} is set to
 *       {@value #CUTOFF_FRACTION} times its <em>own</em> unbounded optimal cost
 *       (precomputed once at setup). A single global cutoff would be meaningless
 *       across queries of different lengths.</li>
 *   <li><b>Shared setup</b>: networks and routers are lazy-initialised once per
 *       JVM and shared across all benchmark methods. CH preprocessing in
 *       particular is amortized across the suite.</li>
 *   <li><b>Iteration counts vary by scenario</b>: scenarios where every
 *       unbounded call has to exhaust the full reachable component
 *       ({@link SpeedyDijkstra}/{@link SpeedyALT} against a disconnected
 *       target) take tens of milliseconds per call on the 200x200 grid, so
 *       they use {@value #WARMUP_ITERATIONS_SLOW}/{@value #MEASURE_ITERATIONS_SLOW}
 *       iterations; CH variants and all cutoff-below-optimal scenarios use
 *       {@value #WARMUP_ITERATIONS_FAST}/{@value #MEASURE_ITERATIONS_FAST}.</li>
 *   <li><b>Dead-code prevention</b>: returned paths are accumulated into a
 *       checksum that is printed, preventing JIT from eliminating the call.</li>
 * </ul>
 *
 * <h2>Scenarios</h2>
 * <ul>
 *   <li><b>Disconnected target</b>: source in the main grid, target on a tiny
 *       disconnected "island". Unbounded must exhaust the reachable component
 *       before returning {@code null}; bounded should bail after a small ball.</li>
 *   <li><b>Cutoff below optimal</b>: both endpoints in the grid;
 *       {@code maxCost = CUTOFF_FRACTION * optimal}. Unbounded runs to completion;
 *       bounded bails as soon as the queue minimum exceeds the cutoff.</li>
 * </ul>
 *
 * <p>No assertions are made on timing (results are environment-dependent).
 * Correctness assertions check that the bounded/unbounded calls agree on
 * null-ness as required by the scenario contract.
 *
 * <p><b>Note on {@link CHRouterTimeDep}</b>: its internal search optimises
 * travel <em>time</em> (required for the CATCHUp/TCH time-dependent CH
 * invariant), while {@code maxCost} is in disutility units (matching
 * {@link Path#travelCost} reconstructed by {@code constructPath}). In-loop
 * pruning on {@code maxCost} would be unit-unsound, so the contract is enforced
 * only by a final guard on the reconstructed path cost. The benchmarks below
 * therefore expect a speedup of ~1.0x for {@code CHRouterTimeDep} and exist to
 * (a) document this fact, and (b) detect future regressions or improvements.
 */
@EnabledIfSystemProperty(named = "matsim.benchmark", matches = "true")
public class SpeedyBoundedSearchBenchmark {

	private static final int GRID_N = 200;
	private static final double LINK_LENGTH = 100.0;
	private static final double LINK_FREESPEED = 10.0;
	private static final int NUM_QUERIES = 200;
	private static final long QUERY_SEED = 4242L;

	// Iteration counts are scenario-dependent: scenarios where every unbounded call
	// has to exhaust the full reachable component (SpeedyDijkstra/SpeedyALT against a
	// disconnected target) take ~50-100 ms per call on the 200x200 grid, so 5000
	// iterations would push runtime past 10 min for one benchmark alone. CH variants
	// terminate in microseconds either way.
	private static final int WARMUP_ITERATIONS_FAST = 500;
	private static final int MEASURE_ITERATIONS_FAST = 5000;
	private static final int WARMUP_ITERATIONS_SLOW = 50;
	private static final int MEASURE_ITERATIONS_SLOW = 500;

	/** True iff the router/scenario combination requires a full-component scan per unbounded call. */
	private static boolean isSlow(LeastCostPathCalculator router, boolean disconnected) {
		return disconnected && (router instanceof SpeedyDijkstra || router instanceof SpeedyALT);
	}

	/**
	 * {@code maxCost = CUTOFF_FRACTION * unbounded_optimal_cost} per query. 0.1 means
	 * "cut at 10% of the true optimum" so the bounded search should bail after a
	 * small fraction of the work.
	 */
	private static final double CUTOFF_FRACTION = 0.1;

	// -------- shared, lazily built state --------

	private static volatile Network gridNetworkShared;
	private static volatile Network gridPlusIslandNetworkShared;
	private static volatile QueryPlan[] connectedQueriesShared;
	private static volatile QueryPlan[] disconnectedQueriesShared;

	// Router instances: built lazily, one per (router-kind x network-kind).
	// They are stateless after build (no per-query mutable state visible to the API).
	private static volatile SpeedyDijkstra dijkstraGrid;
	private static volatile SpeedyDijkstra dijkstraIsland;
	private static volatile SpeedyALT altGrid;
	private static volatile SpeedyALT altIsland;
	private static volatile CHRouter chRouterGrid;
	private static volatile CHRouter chRouterIsland;
	private static volatile CHRouterTimeDep chRouterTimeDepGrid;
	private static volatile CHRouterTimeDep chRouterTimeDepIsland;

	/**
	 * One precomputed query: source link, target link, unbounded optimal cost
	 * (used only for setup/diagnostics), and per-query {@code maxCost}.
	 */
	private record QueryPlan(Link srcLink, Link tgtLink, double optimalCost, double maxCost) {}

	/**
	 * Silence the "No route was found" warnings emitted by every router on a null
	 * return. With {@value #NUM_QUERIES} disconnected queries x {@value #MEASURE_ITERATIONS}
	 * iterations x 4 routers, this would otherwise produce hundreds of thousands of
	 * log lines per run and dominate the wall-clock time of the benchmark.
	 */
	@BeforeAll
	static void silenceRouterWarnings() {
		Configurator.setLevel(CHRouter.class.getName(), Level.ERROR);
		Configurator.setLevel(CHRouterTimeDep.class.getName(), Level.ERROR);
		Configurator.setLevel(SpeedyDijkstra.class.getName(), Level.ERROR);
		Configurator.setLevel(SpeedyALT.class.getName(), Level.ERROR);
	}

	// ------------------------------------------------------------------
	// All scenarios run in one @Test method
	// ------------------------------------------------------------------
	//
	// NOTE: This benchmark intentionally collapses all 8 scenarios into a
	// single test method. Matsim registers {@code AutoResetIdCaches} as a
	// JUnit 5 {@code TestWatcher} (see META-INF/services), which calls
	// {@code Id.resetCaches()} after every successful test. Because we share
	// {@link Network} singletons across scenarios to amortise CH preprocessing,
	// a mid-suite Id cache reset invalidates the cached {@code Id.index()}
	// values stored inside our shared Node/Link references and triggers
	// {@code ArrayIndexOutOfBoundsException} the next time a graph is built.
	// Running everything inside one test keeps Id state stable for the entire
	// run.

	@Test
	void runAllScenarios() {
		runBenchmark("SpeedyDijkstra / disconnected target",
				dijkstraIsland(), disconnectedQueries(), true);
		runBenchmark("SpeedyDijkstra / cutoff below optimal (" + pct(CUTOFF_FRACTION) + " of optimal)",
				dijkstraGrid(), connectedQueries(), false);

		runBenchmark("SpeedyALT      / disconnected target",
				altIsland(), disconnectedQueries(), true);
		runBenchmark("SpeedyALT      / cutoff below optimal (" + pct(CUTOFF_FRACTION) + " of optimal)",
				altGrid(), connectedQueries(), false);

		runBenchmark("CHRouter       / disconnected target",
				chRouterIsland(), disconnectedQueries(), true);
		runBenchmark("CHRouter       / cutoff below optimal (" + pct(CUTOFF_FRACTION) + " of optimal)",
				chRouterGrid(), connectedQueries(), false);

		runBenchmark("CHRouterTimeDep/ disconnected target (no in-loop pruning, ~1.0x expected)",
				chRouterTimeDepIsland(), disconnectedQueries(), true);
		runBenchmark("CHRouterTimeDep/ cutoff below optimal (" + pct(CUTOFF_FRACTION) + " of optimal, no in-loop pruning, ~1.0x expected)",
				chRouterTimeDepGrid(), connectedQueries(), false);
	}

	// ==================================================================
	// Benchmark core
	// ==================================================================

	private static void runBenchmark(String label, LeastCostPathCalculator router,
			QueryPlan[] queries, boolean expectNull) {
		boolean slow = isSlow(router, expectNull);
		int warmupIterations = slow ? WARMUP_ITERATIONS_SLOW : WARMUP_ITERATIONS_FAST;
		int measureIterations = slow ? MEASURE_ITERATIONS_SLOW : MEASURE_ITERATIONS_FAST;

		System.out.printf("%n[bench] %s — starting (%d warmup + %d measure)%n", label, warmupIterations, measureIterations);
		System.out.flush();

		// Warmup: alternate unbounded and bounded so both JIT paths heat up.
		long warmupChecksum = 0;
		for (int i = 0; i < warmupIterations; i++) {
			QueryPlan q = queries[i % queries.length];
			Path pu = router.calcLeastCostPath(q.srcLink, q.tgtLink, 0, null, null);
			Path pb = router.calcLeastCostPath(q.srcLink, q.tgtLink, 0, null, null, q.maxCost);
			if (pu != null) warmupChecksum += pu.links.size();
			if (pb != null) warmupChecksum += pb.links.size();
		}

		// Measure unbounded.
		long unboundedChecksum = 0;
		long nullCountUnbounded = 0;
		long t0 = System.nanoTime();
		for (int i = 0; i < measureIterations; i++) {
			QueryPlan q = queries[i % queries.length];
			Path p = router.calcLeastCostPath(q.srcLink, q.tgtLink, 0, null, null);
			if (p == null) {
				nullCountUnbounded++;
			} else {
				unboundedChecksum += p.links.size();
			}
		}
		long unboundedNanos = System.nanoTime() - t0;

		// Measure bounded.
		long boundedChecksum = 0;
		long nullCountBounded = 0;
		long t1 = System.nanoTime();
		for (int i = 0; i < measureIterations; i++) {
			QueryPlan q = queries[i % queries.length];
			Path p = router.calcLeastCostPath(q.srcLink, q.tgtLink, 0, null, null, q.maxCost);
			if (p == null) {
				nullCountBounded++;
			} else {
				boundedChecksum += p.links.size();
			}
		}
		long boundedNanos = System.nanoTime() - t1;

		double unboundedUsPerCall = unboundedNanos / 1e3 / measureIterations;
		double boundedUsPerCall = boundedNanos / 1e3 / measureIterations;
		double speedup = (double) unboundedNanos / boundedNanos;

		System.out.println();
		System.out.println("=====================================================================");
		System.out.println(label);
		System.out.println("---------------------------------------------------------------------");
		System.out.printf("  iterations:  %d warmup + %d measure (cycling %d queries)%n",
				warmupIterations, measureIterations, queries.length);
		System.out.printf("  unbounded:   %9.2f us/call  (%d/%d nulls)%n",
				unboundedUsPerCall, nullCountUnbounded, measureIterations);
		System.out.printf("  bounded:     %9.2f us/call  (%d/%d nulls)%n",
				boundedUsPerCall, nullCountBounded, measureIterations);
		System.out.printf("  speedup:     %9.2fx%n", speedup);
		System.out.printf("  checksum:    warmup=%d  unbounded=%d  bounded=%d%n",
				warmupChecksum, unboundedChecksum, boundedChecksum);
		System.out.println("=====================================================================");
		System.out.flush();

		// Correctness contracts.
		if (expectNull) {
			if (nullCountUnbounded != measureIterations || nullCountBounded != measureIterations) {
				throw new AssertionError("disconnected scenario must return null for every call (got "
						+ nullCountUnbounded + " / " + nullCountBounded + " nulls)");
			}
		} else {
			if (nullCountUnbounded != 0) {
				throw new AssertionError("unbounded must find a path for every query (got "
						+ nullCountUnbounded + " nulls)");
			}
			if (nullCountBounded != measureIterations) {
				throw new AssertionError("bounded must return null for every query when cutoff is below optimal (got "
						+ nullCountBounded + " nulls / " + measureIterations + " calls)");
			}
		}
	}

	private static String pct(double fraction) {
		return String.format("%.0f%%", fraction * 100.0);
	}

	// ==================================================================
	// Lazy singletons: networks
	// ==================================================================

	private static Network gridNetwork() {
		Network local = gridNetworkShared;
		if (local == null) {
			synchronized (SpeedyBoundedSearchBenchmark.class) {
				local = gridNetworkShared;
				if (local == null) {
					long t0 = System.nanoTime();
					local = buildGrid(GRID_N);
					gridNetworkShared = local;
					System.out.printf("[setup] built %dx%d grid (%d nodes, %d links) in %.2f s%n",
							GRID_N, GRID_N, local.getNodes().size(), local.getLinks().size(),
							(System.nanoTime() - t0) / 1e9);
				}
			}
		}
		return local;
	}

	private static Network gridPlusIslandNetwork() {
		Network local = gridPlusIslandNetworkShared;
		if (local == null) {
			synchronized (SpeedyBoundedSearchBenchmark.class) {
				local = gridPlusIslandNetworkShared;
				if (local == null) {
					long t0 = System.nanoTime();
					local = buildGridPlusDisconnectedIsland(GRID_N);
					gridPlusIslandNetworkShared = local;
					System.out.printf("[setup] built %dx%d grid + island (%d nodes, %d links) in %.2f s%n",
							GRID_N, GRID_N, local.getNodes().size(), local.getLinks().size(),
							(System.nanoTime() - t0) / 1e9);
				}
			}
		}
		return local;
	}

	// ==================================================================
	// Lazy singletons: routers
	// ==================================================================

	private static SpeedyDijkstra dijkstraGrid() {
		SpeedyDijkstra local = dijkstraGrid;
		if (local == null) {
			synchronized (SpeedyBoundedSearchBenchmark.class) {
				local = dijkstraGrid;
				if (local == null) {
					local = buildDijkstra(gridNetwork());
					dijkstraGrid = local;
				}
			}
		}
		return local;
	}

	private static SpeedyDijkstra dijkstraIsland() {
		SpeedyDijkstra local = dijkstraIsland;
		if (local == null) {
			synchronized (SpeedyBoundedSearchBenchmark.class) {
				local = dijkstraIsland;
				if (local == null) {
					local = buildDijkstra(gridPlusIslandNetwork());
					dijkstraIsland = local;
				}
			}
		}
		return local;
	}

	private static SpeedyALT altGrid() {
		SpeedyALT local = altGrid;
		if (local == null) {
			synchronized (SpeedyBoundedSearchBenchmark.class) {
				local = altGrid;
				if (local == null) {
					long t0 = System.nanoTime();
					local = buildAlt(gridNetwork());
					altGrid = local;
					System.out.printf("[setup] built SpeedyALT (landmarks) for grid in %.2f s%n",
							(System.nanoTime() - t0) / 1e9);
				}
			}
		}
		return local;
	}

	private static SpeedyALT altIsland() {
		SpeedyALT local = altIsland;
		if (local == null) {
			synchronized (SpeedyBoundedSearchBenchmark.class) {
				local = altIsland;
				if (local == null) {
					long t0 = System.nanoTime();
					local = buildAlt(gridPlusIslandNetwork());
					altIsland = local;
					System.out.printf("[setup] built SpeedyALT (landmarks) for grid+island in %.2f s%n",
							(System.nanoTime() - t0) / 1e9);
				}
			}
		}
		return local;
	}

	private static CHRouter chRouterGrid() {
		CHRouter local = chRouterGrid;
		if (local == null) {
			synchronized (SpeedyBoundedSearchBenchmark.class) {
				local = chRouterGrid;
				if (local == null) {
					long t0 = System.nanoTime();
					local = buildChRouter(gridNetwork());
					chRouterGrid = local;
					System.out.printf("[setup] built CHRouter for grid in %.2f s%n",
							(System.nanoTime() - t0) / 1e9);
				}
			}
		}
		return local;
	}

	private static CHRouter chRouterIsland() {
		CHRouter local = chRouterIsland;
		if (local == null) {
			synchronized (SpeedyBoundedSearchBenchmark.class) {
				local = chRouterIsland;
				if (local == null) {
					long t0 = System.nanoTime();
					local = buildChRouter(gridPlusIslandNetwork());
					chRouterIsland = local;
					System.out.printf("[setup] built CHRouter for grid+island in %.2f s%n",
							(System.nanoTime() - t0) / 1e9);
				}
			}
		}
		return local;
	}

	private static CHRouterTimeDep chRouterTimeDepGrid() {
		CHRouterTimeDep local = chRouterTimeDepGrid;
		if (local == null) {
			synchronized (SpeedyBoundedSearchBenchmark.class) {
				local = chRouterTimeDepGrid;
				if (local == null) {
					long t0 = System.nanoTime();
					local = buildChRouterTimeDep(gridNetwork());
					chRouterTimeDepGrid = local;
					System.out.printf("[setup] built CHRouterTimeDep for grid in %.2f s%n",
							(System.nanoTime() - t0) / 1e9);
				}
			}
		}
		return local;
	}

	private static CHRouterTimeDep chRouterTimeDepIsland() {
		CHRouterTimeDep local = chRouterTimeDepIsland;
		if (local == null) {
			synchronized (SpeedyBoundedSearchBenchmark.class) {
				local = chRouterTimeDepIsland;
				if (local == null) {
					long t0 = System.nanoTime();
					local = buildChRouterTimeDep(gridPlusIslandNetwork());
					chRouterTimeDepIsland = local;
					System.out.printf("[setup] built CHRouterTimeDep for grid+island in %.2f s%n",
							(System.nanoTime() - t0) / 1e9);
				}
			}
		}
		return local;
	}

	// ==================================================================
	// Lazy singletons: query plans
	// ==================================================================

	/**
	 * Generates {@value #NUM_QUERIES} random (source, target) link pairs within the
	 * connected grid. For each pair, the unbounded optimal cost is computed once
	 * (via the reference Dijkstra router) and {@code maxCost} is set to
	 * {@value #CUTOFF_FRACTION} times that optimal.
	 */
	private static QueryPlan[] connectedQueries() {
		QueryPlan[] local = connectedQueriesShared;
		if (local == null) {
			synchronized (SpeedyBoundedSearchBenchmark.class) {
				local = connectedQueriesShared;
				if (local == null) {
					long t0 = System.nanoTime();
					local = generateConnectedQueries(gridNetwork(), dijkstraGrid());
					connectedQueriesShared = local;
					double meanOpt = Arrays.stream(local).mapToDouble(QueryPlan::optimalCost).average().orElse(0);
					double meanCut = Arrays.stream(local).mapToDouble(QueryPlan::maxCost).average().orElse(0);
					System.out.printf("[setup] generated %d connected queries in %.2f s (mean optimal=%.2f, mean cutoff=%.3f)%n",
							local.length, (System.nanoTime() - t0) / 1e9, meanOpt, meanCut);
				}
			}
		}
		return local;
	}

	/**
	 * Generates {@value #NUM_QUERIES} random sources in the main grid, each paired
	 * with the (unreachable) island target link. {@code maxCost} is set to
	 * {@value #CUTOFF_FRACTION} times the grid diameter cost so the bounded search
	 * has a meaningful, non-trivial ball to explore before bailing.
	 */
	private static QueryPlan[] disconnectedQueries() {
		QueryPlan[] local = disconnectedQueriesShared;
		if (local == null) {
			synchronized (SpeedyBoundedSearchBenchmark.class) {
				local = disconnectedQueriesShared;
				if (local == null) {
					long t0 = System.nanoTime();
					local = generateDisconnectedQueries(gridPlusIslandNetwork(), dijkstraIsland());
					disconnectedQueriesShared = local;
					double meanCut = Arrays.stream(local).mapToDouble(QueryPlan::maxCost).average().orElse(0);
					System.out.printf("[setup] generated %d disconnected queries in %.2f s (cutoff=%.3f)%n",
							local.length, (System.nanoTime() - t0) / 1e9, meanCut);
				}
			}
		}
		return local;
	}

	private static QueryPlan[] generateConnectedQueries(Network network, LeastCostPathCalculator reference) {
		Random rng = new Random(QUERY_SEED);
		List<Link> linkPool = new ArrayList<>(network.getLinks().values());
		QueryPlan[] plans = new QueryPlan[NUM_QUERIES];
		int found = 0;
		int attempts = 0;
		int maxAttempts = NUM_QUERIES * 50;
		while (found < NUM_QUERIES && attempts < maxAttempts) {
			attempts++;
			Link src = linkPool.get(rng.nextInt(linkPool.size()));
			Link tgt = linkPool.get(rng.nextInt(linkPool.size()));
			if (src == tgt) continue;
			if (src.getId().toString().startsWith("island_")) continue;
			if (tgt.getId().toString().startsWith("island_")) continue;
			Path p = reference.calcLeastCostPath(src, tgt, 0, null, null);
			if (p == null || p.travelCost <= 0 || p.links.size() < 2) continue;
			plans[found++] = new QueryPlan(src, tgt, p.travelCost, p.travelCost * CUTOFF_FRACTION);
		}
		if (found < NUM_QUERIES) {
			throw new IllegalStateException("could not generate " + NUM_QUERIES + " connected queries in "
					+ maxAttempts + " attempts (got " + found + ")");
		}
		return plans;
	}

	private static QueryPlan[] generateDisconnectedQueries(Network network, LeastCostPathCalculator reference) {
		// Use the grid diameter (corner-to-corner) cost as a representative scale,
		// then set the cutoff to a fraction of it.
		Link diameterSrc = network.getLinks().get(Id.createLinkId("g_0_0_E"));
		Link diameterTgt = network.getLinks().get(Id.createLinkId("g_" + (GRID_N - 2) + "_" + (GRID_N - 1) + "_E"));
		Path diameter = reference.calcLeastCostPath(diameterSrc, diameterTgt, 0, null, null);
		if (diameter == null) {
			throw new IllegalStateException("grid corner-to-corner diameter query returned null");
		}
		double cutoff = diameter.travelCost * CUTOFF_FRACTION;

		Link islandTarget = network.getLinks().get(Id.createLinkId("island_AB"));
		if (islandTarget == null) {
			throw new IllegalStateException("island_AB link not present in network");
		}

		Random rng = new Random(QUERY_SEED + 1);
		List<Link> gridLinks = new ArrayList<>(network.getLinks().size());
		for (Link link : network.getLinks().values()) {
			if (!link.getId().toString().startsWith("island_")) {
				gridLinks.add(link);
			}
		}
		QueryPlan[] plans = new QueryPlan[NUM_QUERIES];
		for (int i = 0; i < NUM_QUERIES; i++) {
			Link src = gridLinks.get(rng.nextInt(gridLinks.size()));
			plans[i] = new QueryPlan(src, islandTarget, Double.POSITIVE_INFINITY, cutoff);
		}
		return plans;
	}

	// ==================================================================
	// Router builders
	// ==================================================================

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

	private static CHRouter buildChRouter(Network network) {
		FreespeedTravelTimeAndDisutility td = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());
		SpeedyGraph graph = SpeedyGraphBuilder.buildWithSpatialOrdering(network);
		CHGraph chGraph = new CHBuilder(graph, td).build();
		new CHCustomizer().customize(chGraph, td);
		return new CHRouter(chGraph, td, td);
	}

	private static CHRouterTimeDep buildChRouterTimeDep(Network network) {
		FreespeedTravelTimeAndDisutility td = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());
		SpeedyGraph graph = SpeedyGraphBuilder.buildWithSpatialOrdering(network);
		CHGraph chGraph = new CHBuilder(graph, td).build();
		new CHTTFCustomizer().customize(chGraph, td, td);
		return new CHRouterTimeDep(chGraph, td, td);
	}

	// ==================================================================
	// Network builders
	// ==================================================================

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
	 * with link id {@code island_AB}. The grid is fully reachable from any grid node; the
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
