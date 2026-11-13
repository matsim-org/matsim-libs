package org.matsim.contrib.msp.traveltimematrix.evaluation;

import jakarta.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.trafficmonitoring.QSimFreeSpeedTravelTime;
import org.matsim.contrib.msp.traveltimematrix.TimeVaryingTravelTimeMatrix;
import org.matsim.contrib.util.ExecutorServiceWithResource;
import org.matsim.contrib.zone.skims.TravelTimeMatrix;
import org.matsim.core.router.speedy.LeastCostPathTree;
import org.matsim.core.router.speedy.SpeedyGraph;
import org.matsim.core.router.speedy.SpeedyGraphBuilder;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.OptionalTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.ToDoubleFunction;
import java.util.stream.IntStream;

/**
 * The travel times of a set of queries that only a router can give: the time-dependent optimum, and the
 * time-dependent travel time along the free-speed path. They are the top two rungs of the approximation
 * ladder, and one of the references the matrix can be measured against.
 * <pre>
 *   level 3    TT = sum over the links of the free-speed path of tt(link, clock)
 *   variant 4  TT = time-dependent shortest path, departing at t         (the reference)
 * </pre>
 * <b>Variant 4</b> is what the matrix approximates: one {@link LeastCostPathTree} per origin and
 * departure time over the congested travel times of a finished run. It is the exact answer under the
 * model the matrix lives in, which is also the limit of what it can say - it is not a measured travel
 * time, and a route the simulation would never have taken is still optimal here. The travel times the
 * trips <em>really</em> took are {@link ExperiencedLegs}' business, and the two together are what splits
 * the error of the matrix into the part the method causes and the part reality adds.
 * <p>
 * <b>Level 3</b> keeps the free-speed path and only times it under congestion. The gap between the two
 * is therefore not an error of measurement but the <em>cost of holding the path fixed</em>, which every
 * level of the matrix pays: a real driver reroutes around congestion, a fixed path does not, so level 3
 * is an upper bound of variant 4 and so is everything below it. The mean gap is logged when the
 * computation finishes.
 * <p>
 * <b>Free speed alone.</b> Passing no congested travel time computes only the free-speed part - one tree
 * per origin, no events needed at all - and fills in {@link TravelTimeQuery#freeSpeedTravelTime()}. That
 * is the reference the <em>zone system</em> of the matrix is measured against on its own: compared
 * against an exact free-speed travel time, the free-speed variant of the matrix can be wrong about
 * nothing but its own discretisation.
 * <p>
 * <b>What it costs</b> is one shortest-path tree per <em>distinct origin and departure time</em> of the
 * queries, plus one free-speed tree per origin. That is what makes the shape of the queries matter: a
 * {@link ODSample} shares its departure times between all pairs, so 200 origins at 12 times are 2400
 * trees however many destinations there are, while the trips of a run mostly depart at distinct seconds
 * and cost about one tree each. Both are supported, and the number is logged before the work starts so
 * that an impossible run can be stopped rather than waited out.
 *
 * @see MatrixEvaluation which compares the matrix against these
 */
public final class RoutedTravelTimes {

	private static final Logger log = LogManager.getLogger(RoutedTravelTimes.class);

	/**
	 * What was routed for one query. The free-speed fields are always filled in; the two congested ones
	 * are {@link Double#NaN} exactly when no congested travel time was given, i.e. in free-speed-only
	 * mode. A query that cannot be routed at all makes the computation fail rather than carry a hole.
	 */
	private record Answer(double timeDependent, double fixedPath, double freeSpeedTravelTime, double distance) {
	}

	/**
	 * A query as far as routing is concerned: the pair and the departure time, and nothing else.
	 */
	private record Key(Node fromNode, Node toNode, double departureTime) {
	}

	private final Map<Key, Answer> answers;
	private final List<TravelTimeQuery> queries;
	private final boolean congested;

	private RoutedTravelTimes(Map<Key, Answer> answers, List<TravelTimeQuery> queries, boolean congested) {
		this.answers = answers;
		this.queries = queries;
		this.congested = congested;
	}

	/**
	 * Routes every query, however it was produced.
	 *
	 * @param network the study area car subnetwork, the same one the matrix is built over
	 * @param queries what to route. Their reference travel time is ignored and replaced - which is the
	 * point: the same queries can be answered by routing and by what a trip really took, and the two
	 * compared.
	 * @param congestedTravelTime the congested link travel times of the run, e.g.
	 * {@link org.matsim.contrib.msp.traveltimematrix.prepare.MeasuredTraffic#getTravelTimes()}. The same
	 * travel times the congestion index was measured from, or the reference and the thing under test
	 * would disagree about the run rather than about the approximation. Pass {@code null} to route the
	 * free-speed paths only, which needs no events and no per-departure-time trees.
	 * @param qSimTimeStepSize {@code config.qsim().getTimeStepSize()} of the run, so that the free-speed
	 * path is the one the free-speed matrix routes and its travel time is rounded the same way
	 * @param numberOfThreads origins are routed one per thread
	 */
	public static RoutedTravelTimes compute(Network network, Collection<TravelTimeQuery> queries,
		@Nullable TravelTime congestedTravelTime, double qSimTimeStepSize, int numberOfThreads) {

		// by origin, because one tree answers every destination of that origin at once, and within an
		// origin by departure time, because one tree answers one of those
		Map<Node, Map<Double, Set<Node>>> byOrigin = new LinkedHashMap<>();
		for (TravelTimeQuery query : queries) {
			byOrigin.computeIfAbsent(query.fromNode(), origin -> new LinkedHashMap<>())
				.computeIfAbsent(query.departureTime(), time -> new LinkedHashSet<>())
				.add(query.toNode());
		}

		long trees = congestedTravelTime == null ? 0 : byOrigin.values().stream().mapToLong(Map::size).sum();
		log.info("Routing {} queries{}: {} origins, {} shortest-path trees over {} nodes (one free-speed tree per "
				+ "origin, plus one per origin and departure time when there is congestion to route on).",
			queries.size(), congestedTravelTime == null ? " at free speed only" : "", byOrigin.size(),
			trees + byOrigin.size(), network.getNodes().size());

		TravelTime freeSpeedTravelTime = new QSimFreeSpeedTravelTime(qSimTimeStepSize);
		List<Trees> resources = IntStream.range(0, numberOfThreads).mapToObj(i -> {
			// one graph per thread, shared by its two trees: the graph is immutable and read-only during
			// a search, while a tree keeps the arrays of one search, so this halves the graph memory
			SpeedyGraph graph = SpeedyGraphBuilder.build(network);
			return new Trees(
				new LeastCostPathTree(graph, freeSpeedTravelTime, new TimeAsTravelDisutility(freeSpeedTravelTime)),
				congestedTravelTime == null ? null
					: new LeastCostPathTree(graph, congestedTravelTime,
					new TimeAsTravelDisutility(congestedTravelTime)));
		}).toList();

		ExecutorServiceWithResource<Trees> executor = new ExecutorServiceWithResource<>(resources);
		Counter counter = new Counter("Routing reference: origin ", " / " + byOrigin.size());

		Map<Key, Answer> answers = new ConcurrentHashMap<>();
		// how much longer the fixed free-speed path takes than the time-dependent optimum, as a share of
		// the optimum. The headline number of the evaluation: it is the part of the error of the matrix
		// that no congestion index can remove.
		DoubleAdder fixedPathExcess = new DoubleAdder();
		LongAdder fixedPathShorter = new LongAdder();

		executor.submitRunnablesAndWait(byOrigin.entrySet().stream().map(origin -> (trees1 -> {
			counter.incCounter();
			computeForOrigin(origin.getKey(), origin.getValue(), congestedTravelTime, trees1, answers,
				fixedPathExcess, fixedPathShorter);
		})));

		counter.printCounter();
		executor.shutdown();

		if (congestedTravelTime == null) {
			log.info("Routing done: the free-speed travel time of {} pairs.", answers.size());
		} else {
			log.info("Routing done. Holding the free-speed path fixed costs on average {}% of the time-dependent "
					+ "optimum over {} queries; in {} of them the fixed path came out shorter than the optimum, "
					+ "which can only be rounding.",
				String.format(Locale.ROOT, "%.2f", 100 * fixedPathExcess.sum() / Math.max(1, answers.size())),
				answers.size(), fixedPathShorter.sum());
		}

		List<TravelTimeQuery> answered = queries.stream()
			.map(query -> {
				Answer answer = answers.get(new Key(query.fromNode(), query.toNode(), query.departureTime()));
				return query.withRoutedPath(answer.distance(), answer.freeSpeedTravelTime());
			})
			.toList();

		return new RoutedTravelTimes(Map.copyOf(answers), answered, congestedTravelTime != null);
	}

	/**
	 * The trees one thread works with. The free-speed one is calculated once per origin, the congested
	 * one once per origin and departure time - and is null in free-speed-only mode, where building a
	 * second graph search per thread would be paid for and never used.
	 */
	private record Trees(LeastCostPathTree freeSpeed, @Nullable LeastCostPathTree congested) {
	}

	private static void computeForOrigin(Node origin, Map<Double, Set<Node>> destinationsByDepartureTime,
		@Nullable TravelTime congestedTravelTime, Trees trees, Map<Key, Answer> answers,
		DoubleAdder fixedPathExcess, LongAdder fixedPathShorter) {

		// departure time 0: a free-speed path does not depend on when the trip starts, so this tree is
		// calculated once and its paths are reused for every departure time
		trees.freeSpeed().calculate(origin, 0, null, null);

		Map<Node, List<Link>> freeSpeedPaths = new HashMap<>();
		Map<Node, Double> freeSpeedTravelTimes = new HashMap<>();
		Map<Node, Double> distances = new HashMap<>();

		for (Set<Node> destinations : destinationsByDepartureTime.values()) {
			for (Node destination : destinations) {
				if (freeSpeedPaths.containsKey(destination)) {
					continue;
				}
				int nodeIndex = trees.freeSpeed().getNodeIndex(destination);
				freeSpeedTravelTimes.put(destination,
					travelTimeTo(trees.freeSpeed(), nodeIndex, 0, origin, destination));
				distances.put(destination, trees.freeSpeed().getDistance(nodeIndex));
				freeSpeedPaths.put(destination, freeSpeedPath(trees.freeSpeed(), destination));
			}
		}

		for (Map.Entry<Double, Set<Node>> departure : destinationsByDepartureTime.entrySet()) {

			double departureTime = departure.getKey();

			if (congestedTravelTime == null) {
				for (Node destination : departure.getValue()) {
					answers.put(new Key(origin, destination, departureTime), new Answer(Double.NaN, Double.NaN,
						freeSpeedTravelTimes.get(destination), distances.get(destination)));
				}
				continue;
			}

			trees.congested().calculate(origin, departureTime, null, null);

			for (Node destination : departure.getValue()) {

				double optimum = travelTimeTo(trees.congested(), trees.congested().getNodeIndex(destination),
					departureTime, origin, destination);
				double alongFreeSpeedPath = timeAlongPath(freeSpeedPaths.get(destination), departureTime,
					congestedTravelTime);

				answers.put(new Key(origin, destination, departureTime), new Answer(optimum, alongFreeSpeedPath,
					freeSpeedTravelTimes.get(destination), distances.get(destination)));

				if (optimum > 0) {
					fixedPathExcess.add((alongFreeSpeedPath - optimum) / optimum);
				}
				if (alongFreeSpeedPath < optimum - 1) {
					fixedPathShorter.increment();
				}
			}
		}
	}

	/**
	 * The travel time of a tree to one node, with the error message a disconnected network deserves.
	 * The study area network is cleaned and checked, so this should not happen; when it does, the matrix
	 * would have thrown on the same pair.
	 */
	private static double travelTimeTo(LeastCostPathTree tree, int nodeIndex, double departureTime, Node origin,
		Node destination) {
		OptionalTime arrival = tree.getTime(nodeIndex);
		if (arrival.isUndefined()) {
			throw new IllegalStateException("There is no path from node " + origin.getId() + " to node "
				+ destination.getId() + ", so the queries hold a pair the network cannot answer. Cut the study area "
				+ "network with StudyAreaNetwork.cut, which removes what cannot be routed and checks the rest.");
		}
		return arrival.seconds() - departureTime;
	}

	/**
	 * The free-speed path, in the order it is driven. {@link LeastCostPathTree.LinkPathIterator} walks
	 * from the destination back to the origin, which is the wrong way round for a clock that runs
	 * forward, so the path is reversed once per pair rather than per departure time.
	 */
	private static List<Link> freeSpeedPath(LeastCostPathTree tree, Node destination) {
		List<Link> path = new ArrayList<>();
		Iterator<Link> links = tree.getLinkPathIterator(destination);
		while (links.hasNext()) {
			path.add(links.next());
		}
		Collections.reverse(path);
		return path;
	}

	/**
	 * Level 3: the time-dependent travel time along a fixed path, accumulated link by link with the
	 * clock the trip actually arrives at each link with. This is what the matrix approximates by
	 * reading one index at the middle of the trip.
	 */
	private static double timeAlongPath(List<Link> path, double departureTime, TravelTime travelTime) {
		double clock = departureTime;
		for (Link link : path) {
			clock += travelTime.getLinkTravelTime(link, clock, null, null);
		}
		return clock - departureTime;
	}

	/**
	 * @return the queries that were routed, with the length and the free-speed travel time of the
	 * free-speed path filled in. A measured travel time a query already carried is left alone, so a
	 * trip of a run can be compared against what it took <em>and</em> against routing.
	 */
	public List<TravelTimeQuery> getQueries() {
		return this.queries;
	}

	/**
	 * @return whether congested travel times were given, i.e. whether anything but the free-speed path
	 * was routed
	 */
	public boolean hasCongestion() {
		return this.congested;
	}

	/**
	 * Variant 4: the time-dependent shortest path travel time [s].
	 */
	public double getTimeDependentTravelTime(Node origin, Node destination, double departureTime) {
		checkCongestion();
		return answer(origin, destination, departureTime).timeDependent();
	}

	/**
	 * Level 3: the time-dependent travel time along the free-speed path [s].
	 */
	public double getFixedPathTravelTime(Node origin, Node destination, double departureTime) {
		checkCongestion();
		return answer(origin, destination, departureTime).fixedPath();
	}

	private void checkCongestion() {
		if (!this.congested) {
			throw new IllegalStateException("These queries were routed at free speed only, so there is no "
				+ "time-dependent travel time to hand out. Pass the congested travel times of a run to compute(..) "
				+ "to get one.");
		}
	}

	private Answer answer(Node origin, Node destination, double departureTime) {
		Answer answer = this.answers.get(new Key(origin, destination, departureTime));
		if (answer == null) {
			throw new IllegalArgumentException("Nothing was routed from node " + origin.getId() + " to node "
				+ destination.getId() + " at " + departureTime + " s. This is a lookup table of the queries it was "
				+ "given, not a router, and an answer to a question nobody asked would compare two different "
				+ "things.");
		}
		return answer;
	}

	/**
	 * The time-dependent optimum as a {@link TravelTimeMatrix}, so that it can be compared against
	 * {@link TimeVaryingTravelTimeMatrix} through the same interface - and, for a fixed set of queries,
	 * be used in its place.
	 */
	public TravelTimeMatrix asTimeDependentMatrix() {
		checkCongestion();
		return matrixOf(Answer::timeDependent, "the time-dependent optimum (variant 4)");
	}

	/**
	 * Level 3 as a {@link TravelTimeMatrix}.
	 *
	 * @see #asTimeDependentMatrix()
	 */
	public TravelTimeMatrix asFixedPathMatrix() {
		checkCongestion();
		return matrixOf(Answer::fixedPath, "the free-speed path, timed under congestion (level 3)");
	}

	private TravelTimeMatrix matrixOf(ToDoubleFunction<Answer> value, String name) {
		return new TravelTimeMatrix() {
			@Override
			public int getTravelTime(Node fromNode, Node toNode, double departureTime) {
				return (int) Math.round(value.applyAsDouble(answer(fromNode, toNode, departureTime)));
			}

			@Override
			public String toString() {
				return "RoutedTravelTimes[" + name + ", " + RoutedTravelTimes.this.answers.size() + " queries]";
			}
		};
	}
}
