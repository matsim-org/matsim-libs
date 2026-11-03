package org.matsim.contrib.msp.traveltimematrix.evaluation;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * The queries an evaluation of the matrix is carried out on: origin-destination node pairs, times a set
 * of departure times.
 * <p>
 * <b>Why a sample and not the whole matrix.</b> The reference the matrix is judged against is
 * time-dependent routing, which costs one shortest-path tree per origin <em>and departure time</em>.
 * Computing it for every pair of a city-sized zone system at every time of day is the very thing the
 * matrix exists to avoid, so the error is estimated on a sample instead - which is all a MAPE needs.
 * <p>
 * <b>Why the pairs are grouped by origin.</b> A tree gives the travel times from one origin to
 * <em>every</em> node at once, so sampling a few destinations per origin costs the same as sampling one.
 * The grouping is therefore not a detail of the data structure but the reason the reference is
 * affordable at all: 200 origins with 6 destinations each and 24 departure times are 4800 trees and
 * 28800 query results, not 28800 trees.
 * <p>
 * A sample is written and read as a csv so that an experiment can be repeated, and so that the
 * <em>same</em> sample can be handed to something else - a route API validation such as
 * {@code SampleValidationRoutes} of {@code contribs/application} reads {@code from_node,to_node} pairs
 * in this shape.
 *
 * @see #byDistanceBands(Network, int, double[], double[], long) the sampling an error-by-distance table needs
 * @see RoutedTravelTimes what is computed for it
 */
public final class ODSample {

	private static final Logger log = LogManager.getLogger(ODSample.class);

	private static final String FROM_NODE = "from_node";
	private static final String TO_NODE = "to_node";
	private static final String DEPARTURE_TIME = "departure_time";

	/**
	 * Distance bands that cover an urban study area reasonably: up to 2 km, to 5, to 10, to 20 and
	 * beyond [m]. The upper end of the last band is left open - how far apart two nodes of the study
	 * area can be is a property of the area, not of the sampling.
	 */
	public static final double[] DEFAULT_DISTANCE_BANDS = {0, 2000, 5000, 10000, 20000};

	/**
	 * Departure times that resolve the shape of the day at one per hour. The matrix is cheapest exactly
	 * where this is expensive, so a finer grid is affordable for the matrix and not for the reference.
	 */
	public static final double[] HOURLY_DEPARTURE_TIMES = hourly(0, 24);

	/**
	 * How often a destination is drawn for one band before the band is given up on.
	 */
	private static final int MAX_DRAWS_PER_DESTINATION = 20;

	private final List<Node> origins;
	private final Map<Node, List<Node>> destinationsByOrigin;
	private final double[] departureTimes;

	private ODSample(Map<Node, List<Node>> destinationsByOrigin, double[] departureTimes) {

		if (destinationsByOrigin.isEmpty()) {
			throw new IllegalArgumentException("An origin-destination sample needs at least one origin.");
		}
		if (departureTimes.length == 0) {
			throw new IllegalArgumentException("An origin-destination sample needs at least one departure time.");
		}

		Map<Node, List<Node>> copy = new LinkedHashMap<>();
		destinationsByOrigin.forEach((origin, destinations) -> {
			if (destinations.isEmpty()) {
				throw new IllegalArgumentException("Origin " + origin.getId() + " has no destination. An origin "
					+ "without destinations would cost a shortest-path tree and answer nothing.");
			}
			copy.put(origin, List.copyOf(destinations));
		});

		this.destinationsByOrigin = copy;
		this.origins = List.copyOf(copy.keySet());
		this.departureTimes = departureTimes.clone();
	}

	/**
	 * A sample of given pairs, e.g. the origin-destination pairs of a real plans file.
	 *
	 * @param destinationsByOrigin the destinations to query per origin. Iteration order is kept, so a
	 * written sample reads in the order it was built.
	 * @param departureTimes times of day to query every pair at [s]
	 */
	public static ODSample of(Map<Node, List<Node>> destinationsByOrigin, double[] departureTimes) {
		return new ODSample(destinationsByOrigin, departureTimes);
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
	 * @param seed of the random number generator, so that a sample is reproducible
	 */
	public static ODSample uniform(Network network, int numberOfOrigins, int destinationsPerOrigin,
		double[] departureTimes, long seed) {

		SplittableRandom random = new SplittableRandom(seed);
		List<Node> nodes = shuffled(network, random);

		Map<Node, List<Node>> destinationsByOrigin = new LinkedHashMap<>();
		for (Node origin : nodes.subList(0, Math.min(numberOfOrigins, nodes.size()))) {
			List<Node> candidates = shuffled(network, random);
			candidates.remove(origin);
			destinationsByOrigin.put(origin,
				candidates.subList(0, Math.min(destinationsPerOrigin, candidates.size())));
		}

		ODSample sample = new ODSample(destinationsByOrigin, departureTimes);
		log.info("Sampled {} uniformly random origin-destination pairs at {} departure times.",
			sample.getNumberOfPairs(), departureTimes.length);
		return sample;
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
	public static ODSample byDistanceBands(Network network, int numberOfOrigins, double[] bandEdges,
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

		Map<Node, List<Node>> destinationsByOrigin = new LinkedHashMap<>();
		int skippedBands = 0;

		// over a shuffled list of candidates rather than drawing an origin until enough of them worked:
		// a band that no node of the network can fill - 20 km in a study area of 10 - would otherwise
		// turn "sample 200 origins" into a loop that never ends
		for (Node origin : nodes) {

			if (destinationsByOrigin.size() == numberOfOrigins) {
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
				destinationsByOrigin.put(origin, destinations);
			}
		}

		if (destinationsByOrigin.isEmpty()) {
			throw new IllegalArgumentException("No node of the network lies in any of the distance bands "
				+ Arrays.toString(bandEdges) + " m of any other node, so there is nothing to evaluate. The bands "
				+ "have to fit the study area: a band of 0 to 1000 m finds nothing in a network whose nodes are "
				+ "kilometres apart, because the nearest node to a point 500 m away is then the origin itself.");
		}

		ODSample sample = new ODSample(destinationsByOrigin, departureTimes);
		log.info("Sampled {} origin-destination pairs over {} of the {} requested origins, bands {} m, at {} "
				+ "departure times; {} (origin, band) combinations found no node in the band and were skipped.",
			sample.getNumberOfPairs(), destinationsByOrigin.size(), numberOfOrigins, Arrays.toString(bandEdges),
			departureTimes.length, skippedBands);
		return sample;
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
	 * @return the origins, in sampling order. One shortest-path tree per entry and departure time.
	 */
	public List<Node> getOrigins() {
		return this.origins;
	}

	/**
	 * @return the destinations sampled for one origin
	 * @throws IllegalArgumentException if the node is not an origin of this sample
	 */
	public List<Node> getDestinations(Node origin) {
		List<Node> destinations = this.destinationsByOrigin.get(origin);
		if (destinations == null) {
			throw new IllegalArgumentException("Node " + origin.getId() + " is not an origin of this sample.");
		}
		return destinations;
	}

	/**
	 * @return the departure times [s]. A copy - a sample is immutable.
	 */
	public double[] getDepartureTimes() {
		return this.departureTimes.clone();
	}

	/**
	 * @return how many departure times every pair is queried at, i.e. the length of the arrays
	 * {@link RoutedTravelTimes} stores per pair
	 */
	public int getNumberOfDepartureTimes() {
		return this.departureTimes.length;
	}

	/**
	 * @return the number of origin-destination pairs, summed over the origins
	 */
	public int getNumberOfPairs() {
		return this.destinationsByOrigin.values().stream().mapToInt(List::size).sum();
	}

	/**
	 * @return the number of query results a full evaluation produces per variant, i.e. pairs times
	 * departure times
	 */
	public int getNumberOfQueries() {
		return getNumberOfPairs() * this.departureTimes.length;
	}

	/**
	 * Writes the sample as {@code from_node,to_node,departure_time} - one row per query, the shape the
	 * results are reported in, so that the sample and the results join on their first columns.
	 */
	public void write(String path) {
		try (Writer writer = IOUtils.getBufferedWriter(path);
			 CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
				 .setHeader(FROM_NODE, TO_NODE, DEPARTURE_TIME)
				 .build())) {

			for (Node origin : this.origins) {
				for (Node destination : this.destinationsByOrigin.get(origin)) {
					for (double departureTime : this.departureTimes) {
						printer.printRecord(origin.getId(), destination.getId(),
							String.format(Locale.ROOT, "%.0f", departureTime));
					}
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		log.info("Wrote an origin-destination sample of {} pairs times {} departure times to {}.",
			getNumberOfPairs(), this.departureTimes.length, path);
	}

	/**
	 * Reads back what {@link #write(String)} wrote, over the nodes of the given network.
	 *
	 * @throws IllegalArgumentException if a node id is not in the network, which is what happens when a
	 * sample of the full network is read over the study area subnetwork, or the other way round
	 */
	public static ODSample read(URL url, Network network) {

		Map<Node, List<Node>> destinationsByOrigin = new LinkedHashMap<>();
		List<Double> departureTimes = new ArrayList<>();

		try (CSVParser parser = CSVFormat.DEFAULT.builder()
			.setHeader()
			.setSkipHeaderRecord(true)
			.build()
			.parse(IOUtils.getBufferedReader(url))) {

			for (CSVRecord record : parser) {
				Node origin = node(network, record.get(FROM_NODE), url);
				Node destination = node(network, record.get(TO_NODE), url);
				double departureTime = Double.parseDouble(record.get(DEPARTURE_TIME).trim());

				List<Node> destinations = destinationsByOrigin.computeIfAbsent(origin, o -> new ArrayList<>());
				if (!destinations.contains(destination)) {
					destinations.add(destination);
				}
				if (!departureTimes.contains(departureTime)) {
					departureTimes.add(departureTime);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		ODSample sample = new ODSample(destinationsByOrigin,
			departureTimes.stream().mapToDouble(Double::doubleValue).toArray());
		log.info("Read an origin-destination sample of {} pairs times {} departure times from {}.",
			sample.getNumberOfPairs(), sample.departureTimes.length, url);
		return sample;
	}

	/**
	 * @see #read(URL, Network)
	 */
	public static ODSample read(String path, Network network) {
		return read(IOUtils.resolveFileOrResource(path), network);
	}

	private static Node node(Network network, String id, URL url) {
		Node node = network.getNodes().get(Id.createNodeId(id.trim()));
		if (node == null) {
			throw new IllegalArgumentException("The sample " + url + " names node '" + id + "', which the network "
				+ "does not hold. A sample has to be read over the same network it was written for - cutting the "
				+ "study area differently drops nodes.");
		}
		return node;
	}

	@Override
	public String toString() {
		return "ODSample[" + this.origins.size() + " origins, " + getNumberOfPairs() + " pairs, "
			+ this.departureTimes.length + " departure times, " + getNumberOfQueries() + " queries]";
	}
}
