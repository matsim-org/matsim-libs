package org.matsim.contrib.msp.traveltimematrix.evaluation;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * The travel times of an {@link ODSample} that only a router can give: the time-dependent optimum, and
 * the time-dependent travel time along the free-speed path. They are the top two rungs of the
 * approximation ladder, and the yardstick the matrix is measured against.
 * <pre>
 *   level 3    TT = sum over the links of the free-speed path of tt(link, clock)
 *   variant 4  TT = time-dependent shortest path, departing at t         (the reference)
 * </pre>
 * <b>Variant 4</b> is what the matrix approximates: one {@link LeastCostPathTree} per origin and
 * departure time over the congested travel times of a finished run. It is the exact answer under the
 * model the matrix lives in, which is also the limit of what this evaluation can say - it is not
 * measured travel times, and a route the simulation would never have taken is still optimal here.
 * <p>
 * <b>Level 3</b> keeps the free-speed path and only times it under congestion. The gap between the two
 * is therefore not an error of measurement but the <em>cost of holding the path fixed</em>, which every
 * level of the matrix pays: a real driver reroutes around congestion, a fixed path does not, so level 3
 * is an upper bound of variant 4 and so is everything below it. Reporting the two separately is what
 * splits the error of the matrix into the part a better congestion index could remove and the part that
 * is built into the method. The mean gap is logged when the computation finishes.
 * <p>
 * Both are answered for the sampled queries only, so this is a <b>lookup table and not a router</b>:
 * {@link #asTimeDependentMatrix()} and {@link #asFixedPathMatrix()} hand them out as
 * {@link TravelTimeMatrix}, the same interface the matrix under test implements, so that an evaluation
 * loops over all the rungs without knowing which of them routes. Asking either of them about a pair or
 * a departure time that was not sampled throws rather than guessing.
 *
 * @see ODSample for why the sample is grouped by origin - it is what makes this affordable
 * @see MatrixEvaluation which compares the matrix against these
 */
public final class RoutedTravelTimes {

	private static final Logger log = LogManager.getLogger(RoutedTravelTimes.class);

	/**
	 * The travel times of one origin-destination pair.
	 *
	 * @param freeSpeedTravelTime the free-speed travel time of the pair [s], i.e. what the base matrix
	 * holds, but for <b>these two nodes</b> rather than for the central nodes of their zones. The
	 * difference between the two is the discretisation error of the zone system, which is part of the
	 * error of the matrix and worth being able to separate out.
	 * @param distance the length of the free-speed path [m], which the error table groups by
	 * @param timeDependent variant 4 per departure time of the sample [s]
	 * @param fixedPath level 3 per departure time of the sample [s]
	 */
	public record PairTravelTimes(double freeSpeedTravelTime, double distance, double[] timeDependent,
								  double[] fixedPath) {
	}

	private final ODSample sample;
	private final Map<Node, Map<Node, PairTravelTimes>> byOrigin;

	/**
	 * The departure times of the sample, held here as well because
	 * {@link #indexOfDepartureTime(double)} is on the query path and {@link ODSample#getDepartureTimes()}
	 * hands out a copy.
	 */
	private final double[] departureTimes;

	private RoutedTravelTimes(ODSample sample, Map<Node, Map<Node, PairTravelTimes>> byOrigin) {
		this.sample = sample;
		this.byOrigin = byOrigin;
		this.departureTimes = sample.getDepartureTimes();
	}

	/**
	 * Computes both rungs for the whole sample.
	 *
	 * @param network the study area car subnetwork, the same one the matrix is built over
	 * @param sample the queries to answer
	 * @param congestedTravelTime the congested link travel times of the run, e.g.
	 * {@link org.matsim.contrib.msp.traveltimematrix.prepare.MeasuredTraffic#getTravelTimes()}. The same
	 * travel times the congestion index was measured from, or the reference and the thing under test
	 * would disagree about the run rather than about the approximation.
	 * @param qSimTimeStepSize {@code config.qsim().getTimeStepSize()} of the run, so that the free-speed
	 * path is the one the free-speed matrix routes and its travel time is rounded the same way
	 * @param numberOfThreads trees are computed one origin per thread
	 */
	public static RoutedTravelTimes compute(Network network, ODSample sample, TravelTime congestedTravelTime,
		double qSimTimeStepSize, int numberOfThreads) {

		double[] departureTimes = sample.getDepartureTimes();
		TravelTime freeSpeedTravelTime = new QSimFreeSpeedTravelTime(qSimTimeStepSize);

		log.info("Computing the routing reference for {} origins, {} pairs and {} departure times: {} shortest-path "
				+ "trees over {} nodes.", sample.getOrigins().size(), sample.getNumberOfPairs(),
			departureTimes.length, (long) sample.getOrigins().size() * (departureTimes.length + 1),
			network.getNodes().size());

		List<Trees> resources = IntStream.range(0, numberOfThreads).mapToObj(i -> {
			// one graph per thread, shared by its two trees: the graph is immutable and read-only during
			// a search, while a tree keeps the arrays of one search, so this halves the graph memory
			SpeedyGraph graph = SpeedyGraphBuilder.build(network);
			return new Trees(
				new LeastCostPathTree(graph, freeSpeedTravelTime, new TimeAsTravelDisutility(freeSpeedTravelTime)),
				new LeastCostPathTree(graph, congestedTravelTime, new TimeAsTravelDisutility(congestedTravelTime)));
		}).toList();

		ExecutorServiceWithResource<Trees> executor = new ExecutorServiceWithResource<>(resources);
		Counter counter = new Counter("Routing reference: origin ", " / " + sample.getOrigins().size());

		Map<Node, Map<Node, PairTravelTimes>> byOrigin = new ConcurrentHashMap<>();
		// how much longer the fixed free-speed path takes than the time-dependent optimum, as a share of
		// the optimum. The headline number of the evaluation: it is the part of the error of the matrix
		// that no congestion index can remove.
		DoubleAdder fixedPathExcess = new DoubleAdder();
		LongAdder fixedPathShorter = new LongAdder();

		executor.submitRunnablesAndWait(sample.getOrigins().stream().map(origin -> (trees -> {
			counter.incCounter();
			byOrigin.put(origin, computeForOrigin(origin, sample.getDestinations(origin), departureTimes,
				congestedTravelTime, trees, fixedPathExcess, fixedPathShorter));
		})));

		counter.printCounter();
		executor.shutdown();

		int queries = sample.getNumberOfQueries();
		log.info("Routing reference done. Holding the free-speed path fixed costs on average {}% of the "
				+ "time-dependent optimum over {} queries; in {} of them the fixed path came out shorter than the "
				+ "optimum, which can only be rounding.",
			String.format(Locale.ROOT, "%.2f", 100 * fixedPathExcess.sum() / Math.max(1, queries)), queries,
			fixedPathShorter.sum());

		return new RoutedTravelTimes(sample, byOrigin);
	}

	/**
	 * The two trees one thread works with. The free-speed one is calculated once per origin, the
	 * congested one once per origin and departure time.
	 */
	private record Trees(LeastCostPathTree freeSpeed, LeastCostPathTree congested) {
	}

	private static Map<Node, PairTravelTimes> computeForOrigin(Node origin, List<Node> destinations,
		double[] departureTimes, TravelTime congestedTravelTime, Trees trees, DoubleAdder fixedPathExcess,
		LongAdder fixedPathShorter) {

		// departure time 0: a free-speed path does not depend on when the trip starts, so this tree is
		// calculated once and its paths are reused for every departure time
		trees.freeSpeed().calculate(origin, 0, null, null);

		Map<Node, PairTravelTimes> results = new HashMap<>();
		Map<Node, List<Link>> freeSpeedPaths = new HashMap<>();

		for (Node destination : destinations) {
			int nodeIndex = trees.freeSpeed().getNodeIndex(destination);
			results.put(destination, new PairTravelTimes(
				travelTimeTo(trees.freeSpeed(), nodeIndex, 0, origin, destination),
				trees.freeSpeed().getDistance(nodeIndex),
				new double[departureTimes.length], new double[departureTimes.length]));
			freeSpeedPaths.put(destination, freeSpeedPath(trees.freeSpeed(), destination));
		}

		for (int t = 0; t < departureTimes.length; t++) {

			double departureTime = departureTimes[t];
			trees.congested().calculate(origin, departureTime, null, null);

			for (Node destination : destinations) {

				PairTravelTimes pair = results.get(destination);
				double optimum = travelTimeTo(trees.congested(), trees.congested().getNodeIndex(destination),
					departureTime, origin, destination);
				double alongFreeSpeedPath = timeAlongPath(freeSpeedPaths.get(destination), departureTime,
					congestedTravelTime);

				pair.timeDependent()[t] = optimum;
				pair.fixedPath()[t] = alongFreeSpeedPath;

				if (optimum > 0) {
					fixedPathExcess.add((alongFreeSpeedPath - optimum) / optimum);
				}
				if (alongFreeSpeedPath < optimum - 1) {
					fixedPathShorter.increment();
				}
			}
		}

		return results;
	}

	/**
	 * The travel time of a tree to one node, with the error message a disconnected network deserves.
	 * The study area network is cleaned, so this should not happen; when it does, the matrix would have
	 * thrown on the same pair.
	 */
	private static double travelTimeTo(LeastCostPathTree tree, int nodeIndex, double departureTime, Node origin,
		Node destination) {
		OptionalTime arrival = tree.getTime(nodeIndex);
		if (arrival.isUndefined()) {
			throw new IllegalStateException("There is no path from node " + origin.getId() + " to node "
				+ destination.getId() + ", so the sample holds a pair the network cannot answer. Clean the study "
				+ "area network for the mode in question - see StudyAreaNetwork.cut.");
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
	 * @return the sample these travel times were computed for, whose
	 * {@link ODSample#getDepartureTimes()} index the arrays
	 */
	public ODSample getSample() {
		return this.sample;
	}

	/**
	 * @return everything that was computed for one pair
	 * @throws IllegalArgumentException if the pair is not in the sample
	 */
	public PairTravelTimes get(Node origin, Node destination) {
		Map<Node, PairTravelTimes> destinations = this.byOrigin.get(origin);
		PairTravelTimes pair = destinations == null ? null : destinations.get(destination);
		if (pair == null) {
			throw new IllegalArgumentException("The pair " + origin.getId() + " -> " + destination.getId() + " is not "
				+ "in the sample, so no reference was computed for it. A routing reference answers the sampled "
				+ "queries only; re-compute it for the sample you want to evaluate on.");
		}
		return pair;
	}

	/**
	 * Variant 4, the reference: the time-dependent shortest path travel time [s].
	 */
	public double getTimeDependentTravelTime(Node origin, Node destination, int departureTimeIndex) {
		return get(origin, destination).timeDependent()[departureTimeIndex];
	}

	/**
	 * Level 3: the time-dependent travel time along the free-speed path [s].
	 */
	public double getFixedPathTravelTime(Node origin, Node destination, int departureTimeIndex) {
		return get(origin, destination).fixedPath()[departureTimeIndex];
	}

	/**
	 * The reference as a {@link TravelTimeMatrix}, so that it can be compared against
	 * {@link TimeVaryingTravelTimeMatrix} through the same interface - and, for a sampled study, be used
	 * in its place.
	 */
	public TravelTimeMatrix asTimeDependentMatrix() {
		return matrixOf(PairTravelTimes::timeDependent, "the time-dependent reference (variant 4)");
	}

	/**
	 * Level 3 as a {@link TravelTimeMatrix}.
	 *
	 * @see #asTimeDependentMatrix()
	 */
	public TravelTimeMatrix asFixedPathMatrix() {
		return matrixOf(PairTravelTimes::fixedPath, "the free-speed path, timed under congestion (level 3)");
	}

	private TravelTimeMatrix matrixOf(Function<PairTravelTimes, double[]> values, String name) {
		return new TravelTimeMatrix() {
			@Override
			public int getTravelTime(Node fromNode, Node toNode, double departureTime) {
				return (int) Math.round(values.apply(get(fromNode, toNode))[indexOfDepartureTime(departureTime)]);
			}

			@Override
			public String toString() {
				return "RoutedTravelTimes[" + name + ", " + RoutedTravelTimes.this.sample + "]";
			}
		};
	}

	/**
	 * The index of a departure time of the sample. A linear scan over a handful of values, and an exact
	 * comparison: the only legal arguments are the sampled times themselves, which are handed around as
	 * the very same doubles, so a near miss is a caller asking about a time that was never computed -
	 * and answering the nearest one instead would silently compare two different questions.
	 */
	private int indexOfDepartureTime(double departureTime) {
		for (int t = 0; t < this.departureTimes.length; t++) {
			if (this.departureTimes[t] == departureTime) {
				return t;
			}
		}
		throw new IllegalArgumentException("Departure time " + departureTime + " s is not one of the sampled times "
			+ Arrays.toString(this.departureTimes) + ". A routing reference is a lookup table, not a router.");
	}
}
