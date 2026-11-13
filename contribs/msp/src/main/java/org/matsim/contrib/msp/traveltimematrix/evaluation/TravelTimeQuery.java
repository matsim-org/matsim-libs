package org.matsim.contrib.msp.traveltimematrix.evaluation;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One question put to every variant of the matrix - a node pair and a departure time - together with
 * everything that is known about that pair without asking the matrix.
 * <p>
 * This is the currency of the whole package, and the reason the studies it supports are one piece of
 * code rather than several. A query can come from a random origin-destination sample ({@link ODSample})
 * or from the trips a run really made ({@link ExperiencedLegs}); what it is <em>compared against</em> is
 * an independent choice, and a query can carry the ingredients of more than one such reference at a
 * time - see {@link MatrixEvaluation}, which is handed the references separately and does not know where
 * they came from.
 * <p>
 * Nothing in here is "the" reference travel time. Two of the fields can serve as one
 * ({@link #observedTravelTime()} and {@link #freeSpeedTravelTime()}), a third kind is routed and lives
 * in {@link RoutedTravelTimes}, and an evaluation normally reports against several of them side by side.
 *
 * @param fromNode where the trip starts, as the matrix is asked
 * @param toNode where it ends
 * @param departureTime time of day [s]
 * @param observedTravelTime a travel time this pair was <em>measured</em> to take [s], or
 * {@link Double#NaN} if none is known - which is the state a freshly sampled query is in. It is what the
 * trip really took when the query came from {@link ExperiencedLegs}, and what someone else measured when
 * it was read from a file, e.g. fetched from a routing API. Only a measurement goes in here: nothing
 * that could be recomputed from the network.
 * @param distance the length of the path [m], which the error table groups by, or {@link Double#NaN} if
 * no path is known. Always the length of a path and never a beeline distance, so that the bands mean the
 * same thing however the query was produced. The length the trip was <em>driven</em> when the query came
 * from {@link ExperiencedLegs}, and the length of the free-speed path otherwise - see
 * {@link #withRoutedPath}.
 * @param freeSpeedTravelTime the exact free-speed travel time of the pair [s], as a router computes it
 * on the full network, or {@link Double#NaN}. Filled in by {@link RoutedTravelTimes}. It is the reference
 * that isolates the <em>zone system</em> of the matrix from its congestion model: compared against it,
 * the free-speed variant of the matrix can be wrong about nothing else.
 */
public record TravelTimeQuery(Node fromNode, Node toNode, double departureTime, double observedTravelTime,
							  double distance, double freeSpeedTravelTime) {

	private static final Logger log = LogManager.getLogger(TravelTimeQuery.class);

	private static final String FROM_NODE = "from_node";
	private static final String TO_NODE = "to_node";
	private static final String DEPARTURE_TIME = "departure_time";
	private static final String OBSERVED_TRAVEL_TIME = "observed_travel_time";
	private static final String DISTANCE = "distance";
	private static final String FREE_SPEED_TRAVEL_TIME = "free_speed_travel_time";

	/**
	 * What {@link #OBSERVED_TRAVEL_TIME} was called before there was more than one reference, and the
	 * column {@code SampleValidationRoutes} users have already written travel times into. Still read.
	 */
	private static final String LEGACY_OBSERVED_TRAVEL_TIME = "reference_travel_time";

	/**
	 * A query about which nothing is known yet but the question itself.
	 */
	public static TravelTimeQuery of(Node fromNode, Node toNode, double departureTime) {
		return new TravelTimeQuery(fromNode, toNode, departureTime, Double.NaN, Double.NaN, Double.NaN);
	}

	/**
	 * The same query with a measured travel time attached - what the trip took, or what someone measured
	 * for this pair.
	 */
	public TravelTimeQuery withObserved(double observedTravelTime) {
		return new TravelTimeQuery(this.fromNode, this.toNode, this.departureTime, observedTravelTime,
			this.distance, this.freeSpeedTravelTime);
	}

	/**
	 * The same query with what routing found out about the pair: how long the free-speed path is and how
	 * long it takes. The measured travel time is <b>kept</b> - routing a query that came from a real trip
	 * must not overwrite what that trip did, because the point of doing both is to compare them.
	 * <p>
	 * A distance the query <em>already</em> carries is kept for the same reason, and that is not a detail:
	 * a query from {@link ExperiencedLegs} carries the distance the vehicle really drove, while
	 * {@code distance} here is the length of the free-speed path a router would have taken. Both are path
	 * lengths and the two are close, but only one of them is a measurement, and the distance bands of the
	 * error table should group a trip by the trip it was. It also keeps the bands stable: a trips
	 * evaluation would otherwise band its queries differently depending on whether a routed reference
	 * happened to be asked for.
	 *
	 * @param distance the length of the free-speed path [m], used only if none is known yet
	 */
	public TravelTimeQuery withRoutedPath(double distance, double freeSpeedTravelTime) {
		return new TravelTimeQuery(this.fromNode, this.toNode, this.departureTime, this.observedTravelTime,
			Double.isNaN(this.distance) ? distance : this.distance, freeSpeedTravelTime);
	}

	/**
	 * @return whether a measured travel time is known for this pair
	 */
	public boolean hasObserved() {
		return !Double.isNaN(this.observedTravelTime);
	}

	/**
	 * @return whether the free-speed travel time of this pair is known, i.e. whether it has been routed
	 */
	public boolean hasFreeSpeed() {
		return !Double.isNaN(this.freeSpeedTravelTime);
	}

	/**
	 * Writes queries as
	 * {@code from_node,to_node,departure_time,observed_travel_time,distance,free_speed_travel_time} - one
	 * row per query, the shape the results are reported in, so that the queries and the results join on
	 * their first columns.
	 * <p>
	 * The file is how an experiment is repeated on exactly the same questions: with other matrix
	 * parameters, on another day, or - which is what the measured travel time column is for - by someone
	 * who fetched the travel times of these pairs from a routing API and wants them compared against the
	 * same matrix. Unknown values are written as empty fields rather than as {@code NaN}, so that every
	 * reader of the csv treats them as missing without being told.
	 */
	public static void write(List<TravelTimeQuery> queries, String path) {

		try (Writer writer = IOUtils.getBufferedWriter(path);
			 CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
				 .setHeader(FROM_NODE, TO_NODE, DEPARTURE_TIME, OBSERVED_TRAVEL_TIME, DISTANCE,
					 FREE_SPEED_TRAVEL_TIME)
				 .build())) {

			for (TravelTimeQuery query : queries) {
				printer.printRecord(query.fromNode().getId(), query.toNode().getId(),
					format(query.departureTime(), 0), format(query.observedTravelTime(), 1),
					format(query.distance(), 1), format(query.freeSpeedTravelTime(), 1));
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		log.info("Wrote {} queries to {}.", queries.size(), path);
	}

	/**
	 * Reads back what {@link #write} wrote, over the nodes of the given network. Everything but the pair
	 * and the departure time is optional, both as a column and per row; a file that carries the measured
	 * and the free-speed travel time can be evaluated against both without reading any events or routing
	 * anything again.
	 *
	 * @throws IllegalArgumentException if a node id is not in the network, which is what happens when
	 * queries of the full network are read over the study area subnetwork, or the other way round
	 */
	public static List<TravelTimeQuery> read(URL url, Network network) {

		List<TravelTimeQuery> queries = new ArrayList<>();

		try (CSVParser parser = CSVFormat.DEFAULT.builder()
			.setHeader()
			.setSkipHeaderRecord(true)
			.build()
			.parse(IOUtils.getBufferedReader(url))) {

			String observed = parser.getHeaderMap().containsKey(OBSERVED_TRAVEL_TIME)
				? OBSERVED_TRAVEL_TIME
				: parser.getHeaderMap().containsKey(LEGACY_OBSERVED_TRAVEL_TIME) ? LEGACY_OBSERVED_TRAVEL_TIME : null;
			boolean hasDistance = parser.getHeaderMap().containsKey(DISTANCE);
			boolean hasFreeSpeed = parser.getHeaderMap().containsKey(FREE_SPEED_TRAVEL_TIME);

			for (CSVRecord record : parser) {
				queries.add(new TravelTimeQuery(
					node(network, record.get(FROM_NODE), url),
					node(network, record.get(TO_NODE), url),
					Double.parseDouble(record.get(DEPARTURE_TIME).trim()),
					observed == null ? Double.NaN : optional(record.get(observed)),
					hasDistance ? optional(record.get(DISTANCE)) : Double.NaN,
					hasFreeSpeed ? optional(record.get(FREE_SPEED_TRAVEL_TIME)) : Double.NaN));
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		if (queries.isEmpty()) {
			throw new IllegalArgumentException("The query file " + url + " holds no rows.");
		}

		log.info("Read {} queries from {}, {} of them with a measured travel time and {} with a free-speed one.",
			queries.size(), url, queries.stream().filter(TravelTimeQuery::hasObserved).count(),
			queries.stream().filter(TravelTimeQuery::hasFreeSpeed).count());
		return List.copyOf(queries);
	}

	/**
	 * @see #read(URL, Network)
	 */
	public static List<TravelTimeQuery> read(String path, Network network) {
		return read(IOUtils.resolveFileOrResource(path), network);
	}

	private static double optional(String value) {
		return value == null || value.isBlank() ? Double.NaN : Double.parseDouble(value.trim());
	}

	private static Node node(Network network, String id, URL url) {
		Node node = network.getNodes().get(Id.createNodeId(id.trim()));
		if (node == null) {
			throw new IllegalArgumentException("The query file " + url + " names node '" + id + "', which the network "
				+ "does not hold. Queries have to be read over the same network they were written for - cutting the "
				+ "study area differently drops nodes.");
		}
		return node;
	}

	/**
	 * A number for a csv: fixed decimals rather than whatever {@code Double.toString} produces, and an
	 * empty field for an unknown value.
	 */
	static String format(double value, int decimals) {
		return Double.isNaN(value) ? "" : String.format(Locale.ROOT, "%." + decimals + "f", value);
	}
}
