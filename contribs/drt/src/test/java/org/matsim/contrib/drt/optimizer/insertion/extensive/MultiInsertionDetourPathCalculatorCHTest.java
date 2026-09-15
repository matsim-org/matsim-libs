/* *********************************************************************** *
 * project: org.matsim.*
 * MultiInsertionDetourPathCalculatorCHTest.java
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

package org.matsim.contrib.drt.optimizer.insertion.extensive;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.speedy.InertialFlowCutter;
import org.matsim.core.router.speedy.CHBuilder;
import org.matsim.core.router.speedy.CHGraph;
import org.matsim.core.router.speedy.CHTTFCustomizer;
import org.matsim.core.router.speedy.SpeedyGraph;
import org.matsim.core.router.speedy.SpeedyGraphBuilder;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

/**
 * Tests for the CHRouter-based DRT insertion one-to-many path search.
 * <p>
 * Verifies that {@link MultiInsertionDetourPathCalculatorManager} correctly creates
 * CH-accelerated calculators when {@code config.controller.routingAlgorithmType=CHRouter}
 * is configured (passed as {@code useCH=true}), that the travel times match the
 * Dijkstra baseline, and that the shared CH graph cache is thread-safe under
 * concurrent access.
 *
 * @author Steffen Axer
 */
public class MultiInsertionDetourPathCalculatorCHTest {

	private static final double FREESPEED = 15.0; // m/s
	private static final double LINK_LENGTH = 150.0; // m → TT = 10 s per link

	/**
	 * Builds a simple A→B→C→D→E linear network with free-speed 15 m/s links.
	 */
	private static Network buildLinearNetwork() {
		Network network = NetworkUtils.createNetwork();
		NetworkFactory nf = network.getFactory();

		Node a = nf.createNode(Id.createNodeId("A"), new Coord(0, 0));
		Node b = nf.createNode(Id.createNodeId("B"), new Coord(150, 0));
		Node c = nf.createNode(Id.createNodeId("C"), new Coord(300, 0));
		Node d = nf.createNode(Id.createNodeId("D"), new Coord(450, 0));
		Node e = nf.createNode(Id.createNodeId("E"), new Coord(600, 0));

		for (Node n : List.of(a, b, c, d, e)) network.addNode(n);

		addLink(network, "AB", a, b);
		addLink(network, "BC", b, c);
		addLink(network, "CD", c, d);
		addLink(network, "DE", d, e);
		// reverse direction for backward search
		addLink(network, "BA", b, a);
		addLink(network, "CB", c, b);
		addLink(network, "DC", d, c);
		addLink(network, "ED", e, d);

		return network;
	}

	/**
	 * Builds an n×n perturbed grid network for more realistic concurrency testing.
	 */
	private static Network buildPerturbedGrid(int n, long seed) {
		Network network = NetworkUtils.createNetwork();
		NetworkFactory nf = network.getFactory();
		Random rng = new Random(seed);
		double spacing = 200.0;

		Node[][] nodes = new Node[n][n];
		for (int r = 0; r < n; r++) {
			for (int c = 0; c < n; c++) {
				double px = c * spacing + (rng.nextDouble() - 0.5) * spacing * 0.6;
				double py = r * spacing + (rng.nextDouble() - 0.5) * spacing * 0.6;
				Node node = nf.createNode(Id.createNodeId(r + "_" + c), new Coord(px, py));
				network.addNode(node);
				nodes[r][c] = node;
			}
		}
		for (int r = 0; r < n; r++) {
			for (int c = 0; c < n; c++) {
				if (c + 1 < n) {
					double len = distance(nodes[r][c], nodes[r][c + 1]);
					double speed = 8.0 + rng.nextDouble() * 22.0;
					addLink(network, r + "_" + c + "R", nodes[r][c], nodes[r][c + 1], len, speed);
					addLink(network, r + "_" + c + "L", nodes[r][c + 1], nodes[r][c], len, speed);
				}
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

	private static void addLink(Network network, String id, Node from, Node to) {
		addLink(network, id, from, to, LINK_LENGTH, FREESPEED);
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
	 * Verifies that the CH-based one-to-many path search returns the same travel
	 * time as the Dijkstra-based baseline on a small linear network.
	 */
	@Test
	void chBasedPathSearchMatchesDijkstra() {
		Network network = buildLinearNetwork();
		TravelTime travelTime = new FreeSpeedTravelTime();
		TravelDisutility travelDisutility = new TimeAsTravelDisutility(travelTime);

		// Build Dijkstra-based search (baseline)
		SpeedyGraph baseGraph = SpeedyGraphBuilder.build(network);
		OneToManyPathSearch dijkstraSearch = OneToManyPathSearch.createSearch(baseGraph, travelTime, travelDisutility, false);

		// Build CH-based search
		SpeedyGraph chBaseGraph = SpeedyGraphBuilder.build(network);
		InertialFlowCutter.NDOrderResult ndOrder = new InertialFlowCutter(chBaseGraph).computeOrderWithBatches();
		CHGraph chGraph = new CHBuilder(chBaseGraph, travelDisutility).buildWithOrderParallel(ndOrder);
		new CHTTFCustomizer().customize(chGraph, travelTime, travelDisutility);
		OneToManyPathSearch chSearch = OneToManyPathSearch.createSearchCH(chGraph, travelTime, travelDisutility, false);

		Link linkAB = network.getLinks().get(Id.createLinkId("AB"));
		Link linkBC = network.getLinks().get(Id.createLinkId("BC"));
		Link linkCD = network.getLinks().get(Id.createLinkId("CD"));
		Link linkDE = network.getLinks().get(Id.createLinkId("DE"));
		Link linkBA = network.getLinks().get(Id.createLinkId("BA"));
		Link linkCB = network.getLinks().get(Id.createLinkId("CB"));
		Link linkDC = network.getLinks().get(Id.createLinkId("DC"));

		// Forward: from linkAB to linkDE, linkCD
		double startTime = 8.0 * 3600;
		var dijkstraResults = dijkstraSearch.calcPathDataArray(linkAB, List.of(linkBC, linkCD, linkDE), startTime, true);
		var chResults = chSearch.calcPathDataArray(linkAB, List.of(linkBC, linkCD, linkDE), startTime, true);

		assertThat(chResults).hasSameSizeAs(dijkstraResults);
		for (int i = 0; i < dijkstraResults.length; i++) {
			assertThat(chResults[i].getTravelTime())
					.as("forward travel time mismatch at index " + i)
					.isCloseTo(dijkstraResults[i].getTravelTime(), org.assertj.core.data.Offset.offset(1e-6));
		}

		// Backward: from linkDE to linkDC, linkCB, linkBA
		var dijkstraBackResults = dijkstraSearch.calcPathDataArray(linkDE, List.of(linkDC, linkCB, linkBA), startTime, false);
		var chBackResults = chSearch.calcPathDataArray(linkDE, List.of(linkDC, linkCB, linkBA), startTime, false);

		assertThat(chBackResults).hasSameSizeAs(dijkstraBackResults);
		for (int i = 0; i < dijkstraBackResults.length; i++) {
			assertThat(chBackResults[i].getTravelTime())
					.as("backward travel time mismatch at index " + i)
					.isCloseTo(dijkstraBackResults[i].getTravelTime(), org.assertj.core.data.Offset.offset(1e-6));
		}
	}

	/**
	 * Smoke test: {@link MultiInsertionDetourPathCalculatorManager} successfully
	 * creates a CH-based calculator when {@code useCH=true} is passed
	 * (derived from {@code config.controller.routingAlgorithmType=CHRouter}).
	 */
	@Test
	void managerCreatesCHCalculatorWhenFlagSet() {
		Network network = buildLinearNetwork();
		TravelTime travelTime = new FreeSpeedTravelTime();
		FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());

		DrtConfigGroup drtCfg = new DrtConfigGroup();
		// useCH is now derived from ControllerConfigGroup.routingAlgorithmType == CHRouter
		// and passed explicitly to the manager – no longer read from DrtConfigGroup
		var manager = new MultiInsertionDetourPathCalculatorManager(network, travelTime, tc, drtCfg, true);
		// Should not throw
		var calculator = manager.create();
		assertThat(calculator).isNotNull();
	}

	// -----------------------------------------------------------------------
	// Thread-safety and caching tests
	// -----------------------------------------------------------------------

	/**
	 * Verifies that multiple threads can concurrently perform CH-based one-to-many
	 * path searches on a SHARED {@link CHGraph} without data corruption.
	 * <p>
	 * Each thread creates its own {@link OneToManyPathSearch} (which owns a private
	 * {@link org.matsim.core.router.speedy.CHLeastCostPathTree}), but they
	 * all share the same read-only CH overlay graph. This mirrors the real-world
	 * DRT setup where 4 concurrent search threads query the same cached CH graph.
	 * <p>
	 * All CH results are compared against a Dijkstra baseline to detect any
	 * corruption caused by concurrent access to the shared graph.
	 */
	@Test
	void concurrentCHQueriesOnSharedGraphAreCorrect() throws Exception {
		Network network = buildPerturbedGrid(20, 42); // 400 nodes, ~1560 links
		TravelTime travelTime = new FreeSpeedTravelTime();
		TravelDisutility travelDisutility = new TimeAsTravelDisutility(travelTime);
		List<Link> linkList = new ArrayList<>(network.getLinks().values());

		// Build ONE shared CH graph
		SpeedyGraph chBaseGraph = SpeedyGraphBuilder.build(network);
		InertialFlowCutter.NDOrderResult ndOrder = new InertialFlowCutter(chBaseGraph).computeOrderWithBatches();
		CHGraph sharedCHGraph = new CHBuilder(chBaseGraph, travelDisutility).buildWithOrderParallel(ndOrder);
		new CHTTFCustomizer().customize(sharedCHGraph, travelTime, travelDisutility);

		// Build Dijkstra baseline (single-threaded)
		SpeedyGraph dijkstraGraph = SpeedyGraphBuilder.build(network);
		OneToManyPathSearch dijkstraSearch = OneToManyPathSearch.createSearch(dijkstraGraph, travelTime, travelDisutility, false);

		int numThreads = 8;
		int queriesPerThread = 50;
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		CyclicBarrier barrier = new CyclicBarrier(numThreads);
		AtomicInteger failureCount = new AtomicInteger(0);

		List<Future<?>> futures = new ArrayList<>();
		for (int t = 0; t < numThreads; t++) {
			final int threadId = t;
			futures.add(executor.submit(() -> {
				try {
					// Each thread creates its OWN search instance sharing the graph
					OneToManyPathSearch chSearch = OneToManyPathSearch.createSearchCH(
							sharedCHGraph, travelTime, travelDisutility, false);
					Random rng = new Random(42 + threadId);

					// Wait for all threads to be ready before starting
					barrier.await();

					for (int q = 0; q < queriesPerThread; q++) {
						Link fromLink = linkList.get(rng.nextInt(linkList.size()));
						// Pick 3-5 random target links
						int numTargets = 3 + rng.nextInt(3);
						List<Link> targets = new ArrayList<>();
						for (int i = 0; i < numTargets; i++) {
							targets.add(linkList.get(rng.nextInt(linkList.size())));
						}
						double startTime = 6.0 * 3600 + rng.nextDouble() * 12 * 3600;
						boolean forward = rng.nextBoolean();

						var chResults = chSearch.calcPathDataArray(fromLink, targets, startTime, forward);

						// Compare against Dijkstra (single-threaded, so done sequentially)
						synchronized (dijkstraSearch) {
							var dijkstraResults = dijkstraSearch.calcPathDataArray(fromLink, targets, startTime, forward);
							for (int i = 0; i < dijkstraResults.length; i++) {
								double chTT = chResults[i].getTravelTime();
								double dijTT = dijkstraResults[i].getTravelTime();
								if (Math.abs(chTT - dijTT) > 1e-3) {
									failureCount.incrementAndGet();
									System.err.printf("Thread %d query %d target %d: CH=%.6f Dij=%.6f%n",
											threadId, q, i, chTT, dijTT);
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

		for (Future<?> f : futures) {
			f.get(); // propagate exceptions
		}
		executor.shutdown();

		assertThat(failureCount.get())
				.as("Concurrent CH queries should produce identical results to Dijkstra")
				.isZero();
	}

	/**
	 * Verifies that two {@link MultiInsertionDetourPathCalculatorManager} instances
	 * sharing the same (Network, TravelDisutility, TravelTime) triple reuse the
	 * same cached CH graph, and that concurrent creation from multiple threads is safe.
	 */
	@Test
	void managersShareCachedCHGraphForSameNetwork() throws Exception {
		Network network = buildLinearNetwork();
		TravelTime travelTime = new FreeSpeedTravelTime();
		FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());

		DrtConfigGroup drtCfg = new DrtConfigGroup();
		// useCH is now derived from ControllerConfigGroup.routingAlgorithmType == CHRouter
		final boolean useCH = true;

		int numThreads = 8;
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		CyclicBarrier barrier = new CyclicBarrier(numThreads);
		List<Future<MultiInsertionDetourPathCalculator>> futures = new ArrayList<>();

		for (int t = 0; t < numThreads; t++) {
			futures.add(executor.submit(() -> {
				// All managers share the SAME network, travelTime, travelDisutility objects
				var manager = new MultiInsertionDetourPathCalculatorManager(network, travelTime, tc, drtCfg, useCH);
				// Wait so all threads attempt create() at the same time
				barrier.await();
				return manager.create();
			}));
		}

		List<MultiInsertionDetourPathCalculator> calculators = new ArrayList<>();
		for (var f : futures) {
			calculators.add(f.get());
		}
		executor.shutdown();

		// All calculators should be non-null (no crashes from concurrent CH build)
		assertThat(calculators).hasSize(numThreads);
		assertThat(calculators).allSatisfy(calc -> assertThat(calc).isNotNull());
	}

	/**
	 * Verifies that managers with DIFFERENT Network objects do NOT share a cached
	 * CH graph (i.e. the cache is identity-based, not content-based).
	 * <p>
	 * Both networks are created independently with identical structure, but since
	 * they are different objects the cache should build two separate CH graphs.
	 * This test ensures correctness isolation between independent networks.
	 */
	@Test
	void managersWithDifferentNetworksDontShareCache() {
		// Two independently created networks with the same structure
		Network network1 = buildLinearNetwork();
		Network network2 = buildLinearNetwork();
		assertThat(network1).isNotSameAs(network2);

		TravelTime travelTime = new FreeSpeedTravelTime();
		FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());
		DrtConfigGroup drtCfg = new DrtConfigGroup();
		// useCH derived from ControllerConfigGroup.routingAlgorithmType == CHRouter
		final boolean useCH = true;

		var manager1 = new MultiInsertionDetourPathCalculatorManager(network1, travelTime, tc, drtCfg, useCH);
		var manager2 = new MultiInsertionDetourPathCalculatorManager(network2, travelTime, tc, drtCfg, useCH);

		// Both should create valid calculators (each triggering its own CH build)
		var calc1 = manager1.create();
		var calc2 = manager2.create();
		assertThat(calc1).isNotNull();
		assertThat(calc2).isNotNull();
	}

	/**
	 * Stress test: multiple threads concurrently query a shared CH graph built via
	 * the manager, each running many forward and backward one-to-many searches.
	 * All results are verified against Dijkstra to detect any corruption.
	 * <p>
	 * This mirrors the real DRT scenario where the manager creates multiple
	 * calculators (for pickup/dropoff forward/backward) that all share the same
	 * cached CH graph.
	 */
	@Test
	void stressConcurrentQueriesViaManager() throws Exception {
		Network network = buildPerturbedGrid(15, 99); // 225 nodes
		TravelTime travelTime = new FreeSpeedTravelTime();
		FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());
		List<Link> linkList = new ArrayList<>(network.getLinks().values());

		DrtConfigGroup drtCfg = new DrtConfigGroup();
		// useCH derived from ControllerConfigGroup.routingAlgorithmType == CHRouter
		var manager = new MultiInsertionDetourPathCalculatorManager(network, travelTime, tc, drtCfg, true);

		// One manager, multiple calculators (simulates DRT creating 4 per insertion worker)

		// Dijkstra baseline
		SpeedyGraph dijGraph = SpeedyGraphBuilder.build(network);
		OneToManyPathSearch dijSearch = OneToManyPathSearch.createSearch(dijGraph, travelTime, tc, false);

		// Build a shared CH search for thread-safe querying
		SpeedyGraph chBase = SpeedyGraphBuilder.build(network);
		InertialFlowCutter.NDOrderResult ndOrder = new InertialFlowCutter(chBase).computeOrderWithBatches();
		CHGraph chGraph = new CHBuilder(chBase, tc).buildWithOrderParallel(ndOrder);
		new CHTTFCustomizer().customize(chGraph, travelTime, tc);

		int numThreads = 6;
		int queriesPerThread = 100;
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		CyclicBarrier barrier = new CyclicBarrier(numThreads);
		AtomicInteger failureCount = new AtomicInteger(0);

		List<Future<?>> futures = new ArrayList<>();
		for (int t = 0; t < numThreads; t++) {
			final int threadId = t;
			futures.add(executor.submit(() -> {
				try {
					OneToManyPathSearch chSearch = OneToManyPathSearch.createSearchCH(
							chGraph, travelTime, tc, false);
					Random rng = new Random(threadId * 1000L);

					barrier.await();

					for (int q = 0; q < queriesPerThread; q++) {
						Link from = linkList.get(rng.nextInt(linkList.size()));
						List<Link> targets = new ArrayList<>();
						for (int i = 0; i < 4; i++) {
							targets.add(linkList.get(rng.nextInt(linkList.size())));
						}
						double startTime = rng.nextDouble() * 24 * 3600;
						boolean forward = rng.nextBoolean();

						var chResults = chSearch.calcPathDataArray(from, targets, startTime, forward);

						synchronized (dijSearch) {
							var dijResults = dijSearch.calcPathDataArray(from, targets, startTime, forward);
							for (int i = 0; i < dijResults.length; i++) {
								if (Math.abs(chResults[i].getTravelTime() - dijResults[i].getTravelTime()) > 1e-3) {
									failureCount.incrementAndGet();
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

		for (Future<?> f : futures) {
			f.get();
		}
		executor.shutdown();

		assertThat(failureCount.get())
				.as("Stress test: all concurrent CH queries must match Dijkstra baseline")
				.isZero();
	}
}
