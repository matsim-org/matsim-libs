package org.matsim.contrib.msp.traveltimematrix.evaluation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Randomly generated origin-destination queries: the other way to get the pairs an evaluation runs on,
 * next to the trips a run really made ({@link ExperiencedLegs}).
 * <p>
 * <b>Why generate pairs at all</b>, when a run comes with hundreds of thousands of real ones. Because
 * the real ones are not a statement about the matrix but about the demand: they cluster where people
 * live and travel, at the hours they travel, and they say nothing about the pairs an analysis will ask
 * about that nobody drove in this scenario - which, for a capability analysis asking "could this person
 * have reached that shop at 19:00", is most of them. A generated sample covers the matrix evenly and on
 * purpose; the real trips weight the error by what actually happens. The two answer different questions
 * and both are worth a table.
 * <p>
 * The pairs are returned as {@link TravelTimeQuery} with <b>no reference travel time</b>: what a
 * generated pair should have taken is not known until something computes it, which is
 * {@link RoutedTravelTimes}' job - or a routing API's.
 *
 * @see #byDistanceBands(Network, int, double[], double[], long) the sampling an error-by-distance table needs
 */
public final class ODSample {

	private static final Logger log = LogManager.getLogger(ODSample.class);

	/**
	 * Distance bands that cover an urban study area reasonably: up to 2 km, to 5, to 10, to 20 and
	 * beyond [m]. The upper end of the last band is left open - how far apart two nodes of the study
	 * area can be is a property of the area, not of the sampling.
	 */
	public static final double[] DEFAULT_DISTANCE_BANDS = {0, 2000, 5000, 10000, 20000};

	/**
	 * Departure times that resolve the shape of the day at one per hour. The matrix is cheapest exactly
	 * where a reference is expensive, so a finer grid is affordable for the matrix and not for the
	 * reference.
	 */
	public static final double[] HOURLY_DEPARTURE_TIMES = hourly(0, 24);

	/**
	 * How often a destination is drawn for one band before the band is given up on.
	 */
	private static final int MAX_DRAWS_PER_DESTINATION = 20;

	private ODSample() {
	}

	/**
	 * Uniformly random node pairs: the plainest sample there is, and the wrong one for an
	 * error-by-distance table.
	 * <p>
	 * Nodes are not spread evenly over a network - a city centre has many more of them per square
	 * kilometre than a motorway corridor - and the number of pairs at a given distance grows with that
	 * distance, so uniform pairs are overwhelmingly medium-range trips through dense areas. That is a
	 * fair picture of "a random pair of nodes" and a poor one of "a trip", which is what
	 * {@link #byDistanceBands} is for.
	 *
	 * @param destinationsPerOrigin how many destinations to draw per origin, at most one less than the
	 * number of nodes - there are no more distinct ones to draw
	 * @param departureTimes the times of day every pair is asked about [s]
	 * @param seed of the random number generator, so that a sample is reproducible
	 */
	public static List<TravelTimeQuery> uniform(Network network, int numberOfOrigins, int destinationsPerOrigin,
		double[] departureTimes, long seed) {

		SplittableRandom random = new SplittableRandom(seed);
		List<Node> nodes = shuffled(network, random);
		List<TravelTimeQuery> queries = new ArrayList<>();

		for (Node origin : nodes.subList(0, Math.min(numberOfOrigins, nodes.size()))) {
			List<Node> candidates = shuffled(network, random);
			candidates.remove(origin);
			for (Node destination : candidates.subList(0, Math.min(destinationsPerOrigin, candidates.size()))) {
				add(queries, origin, destination, departureTimes);
			}
		}

		log.info("Sampled {} uniformly random origin-destination pairs at {} departure times, giving {} queries.",
			queries.size() / departureTimes.length, departureTimes.length, queries.size());
		return List.copyOf(queries);
	}

	/**
	 * One destination per distance band and origin, so that every band gets the same number of pairs.
	 * <p>
	 * This is the sample an error table by distance needs: the interesting question is whether the
	 * approximation is worse for short or for long trips, and that question cannot be answered from a
	 * sample in which the short trips are a handful of the rows. Balancing the bands by construction
	 * also means no band is empty by bad luck, which a stratified sample of a fixed total would risk.
	 * <p>
	 * A destination is drawn geometrically - a random direction at a random distance inside the band,
	 * snapped to the nearest node by {@link NetworkUtils#getNearestNode} - and the band it lands in is
	 * then re-checked on the <b>snapped</b> node, because the snapping moves it. A draw that leaves the
	 * network, or lands outside its band after snapping, is retried; after
	 * {@value #MAX_DRAWS_PER_DESTINATION} failures the band is skipped for that origin and a warning
	 * says how often that happened. That is the honest outcome for an origin at the edge of a small
	 * study area, where no node is 20 km away in any direction at all.
	 * <p>
	 * The direction is drawn uniformly, unlike {@code SampleNetwork.rndCoord} of
	 * {@code contribs/application}, which draws it within ±72° of the origin link's direction. That
	 * bias is right for its purpose - sampling a route that follows a road - and wrong here, where a
	 * matrix is asked about every direction equally.
	 *
	 * @param bandEdges the lower edges of the bands [m], ascending, e.g.
	 * {@link #DEFAULT_DISTANCE_BANDS}. The last band is open-ended upwards, but a destination for it is
	 * drawn within twice its lower edge - a sample has to stay inside the study area.
	 * @throws IllegalArgumentException if not a single pair could be drawn, which means the bands do not
	 * fit the network rather than that the sampling was unlucky
	 */
	public static List<TravelTimeQuery> byDistanceBands(Network network, int numberOfOrigins, double[] bandEdges,
		double[] departureTimes, long seed) {

		if (bandEdges.length == 0) {
			throw new IllegalArgumentException("A sample by distance bands needs at least one band.");
		}
		for (int i = 1; i < bandEdges.length; i++) {
			if (bandEdges[i] <= bandEdges[i - 1]) {
				throw new IllegalArgumentException("The distance band edges have to ascend, but "
					+ Arrays.toString(bandEdges) + " does not.");
			}
		}

		SplittableRandom random = new SplittableRandom(seed);
		List<Node> nodes = shuffled(network, random);

		List<TravelTimeQuery> queries = new ArrayList<>();
		int origins = 0;
		int skippedBands = 0;

		// over a shuffled list of candidates rather than drawing an origin until enough of them worked:
		// a band that no node of the network can fill - 20 km in a study area of 10 - would otherwise
		// turn "sample 200 origins" into a loop that never ends
		for (Node origin : nodes) {

			if (origins == numberOfOrigins) {
				break;
			}

			List<Node> destinations = new ArrayList<>();
			for (int band = 0; band < bandEdges.length; band++) {
				double lower = bandEdges[band];
				double upper = band + 1 < bandEdges.length ? bandEdges[band + 1] : Math.max(2 * lower, lower + 1000);
				Node destination = drawInBand(network, origin, lower, upper, destinations, random);
				if (destination == null) {
					skippedBands++;
				} else {
					destinations.add(destination);
				}
			}

			if (!destinations.isEmpty()) {
				origins++;
				destinations.forEach(destination -> add(queries, origin, destination, departureTimes));
			}
		}

		if (queries.isEmpty()) {
			throw new IllegalArgumentException("No node of the network lies in any of the distance bands "
				+ Arrays.toString(bandEdges) + " m of any other node, so there is nothing to evaluate. The bands "
				+ "have to fit the study area: a band of 0 to 1000 m finds nothing in a network whose nodes are "
				+ "kilometres apart, because the nearest node to a point 500 m away is then the origin itself.");
		}

		log.info("Sampled {} origin-destination pairs over {} of the {} requested origins, bands {} m, at {} "
				+ "departure times, giving {} queries; {} (origin, band) combinations found no node in the band "
				+ "and were skipped.", queries.size() / departureTimes.length, origins, numberOfOrigins,
			Arrays.toString(bandEdges), departureTimes.length, queries.size(), skippedBands);
		return List.copyOf(queries);
	}

	/**
	 * One query per departure time. The departure times are shared by every pair, which is what keeps a
	 * routed reference over a generated sample cheap: {@link RoutedTravelTimes} needs one shortest-path
	 * tree per origin <em>and</em> departure time, so few origins times few times is few trees.
	 */
	private static void add(List<TravelTimeQuery> queries, Node origin, Node destination, double[] departureTimes) {
		if (departureTimes.length == 0) {
			throw new IllegalArgumentException("A sample needs at least one departure time.");
		}
		for (double departureTime : departureTimes) {
			queries.add(TravelTimeQuery.of(origin, destination, departureTime));
		}
	}

	/**
	 * One departure time per hour, as seconds after midnight.
	 *
	 * @param fromHour inclusive, {@code toHour} exclusive
	 */
	public static double[] hourly(int fromHour, int toHour) {
		if (toHour <= fromHour) {
			throw new IllegalArgumentException("The last hour (" + toHour + ") has to come after the first ("
				+ fromHour + ").");
		}
		double[] times = new double[toHour - fromHour];
		for (int i = 0; i < times.length; i++) {
			times[i] = (fromHour + i) * 3600.0;
		}
		return times;
	}

	/**
	 * The nodes of the network in a random order, as a modifiable list. Fisher-Yates with the sample's
	 * own {@link SplittableRandom}, so that the order is reproducible from the seed and the iteration
	 * order of the network - which is itself not guaranteed - does not leak into the sample.
	 */
	private static List<Node> shuffled(Network network, SplittableRandom random) {
		List<Node> nodes = new ArrayList<>(network.getNodes().values());
		for (int i = nodes.size() - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			nodes.set(i, nodes.set(j, nodes.get(i)));
		}
		return nodes;
	}

	/**
	 * A node whose beeline distance from the origin lies in {@code [lower, upper)}, or null if none was
	 * found in {@value #MAX_DRAWS_PER_DESTINATION} draws.
	 */
	private static Node drawInBand(Network network, Node origin, double lower, double upper, List<Node> taken,
		SplittableRandom random) {

		for (int draw = 0; draw < MAX_DRAWS_PER_DESTINATION; draw++) {

			double distance = lower + random.nextDouble() * (upper - lower);
			double direction = random.nextDouble() * 2 * Math.PI;
			Coord target = new Coord(origin.getCoord().getX() + Math.cos(direction) * distance,
				origin.getCoord().getY() + Math.sin(direction) * distance);

			Node candidate = NetworkUtils.getNearestNode(network, target);
			if (candidate == null || candidate == origin || taken.contains(candidate)) {
				continue;
			}

			// on the snapped node, not on the drawn coordinate: snapping can move a point by kilometres
			// at the edge of the network, and a pair in the wrong band would quietly blur the table the
			// whole sampling exists to produce
			double snapped = CoordUtils.calcEuclideanDistance(origin.getCoord(), candidate.getCoord());
			if (snapped >= lower && snapped < upper) {
				return candidate;
			}
		}
		return null;
	}
}
