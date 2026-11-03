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
 * One question put to every variant of the matrix: a node pair, a departure time, and - once it is
 * known - the travel time the variant is to be compared against.
 * <p>
 * This is the currency of the whole package, and the reason the two studies it supports are one piece of
 * code rather than two. A query can come from a random origin-destination sample ({@link ODSample}) or
 * from the trips a run really made ({@link ExperiencedLegs}); its reference travel time can be what the
 * trip really took, or what a time-dependent router computes for it ({@link RoutedTravelTimes}), or one
 * day what a routing API answers. Those two choices are independent, and
 * {@link MatrixEvaluation} does not know which was made.
 *
 * @param fromNode where the trip starts, as the matrix is asked
 * @param toNode where it ends
 * @param departureTime time of day [s]
 * @param referenceTravelTime what the variant should have answered [s], or {@link Double#NaN} while it is
 * still to be determined - which is the state a freshly sampled query is in
 * @param distance the length of the path [m], which the error table groups by, or {@link Double#NaN} if
 * no path is known. Always the length of a path and never a beeline distance, so that the bands mean the
 * same thing however the query was produced.
 * @param freeSpeedTravelTime the exact free-speed travel time of the pair [s], or {@link Double#NaN}. It
 * is reported next to the answers so that the discretisation of the zone system can be told from the
 * congestion model; only routing knows it.
 */
public record TravelTimeQuery(Node fromNode, Node toNode, double departureTime, double referenceTravelTime,
							  double distance, double freeSpeedTravelTime) {

	private static final Logger log = LogManager.getLogger(TravelTimeQuery.class);

	private static final String FROM_NODE = "from_node";
	private static final String TO_NODE = "to_node";
	private static final String DEPARTURE_TIME = "departure_time";
	private static final String REFERENCE_TRAVEL_TIME = "reference_travel_time";
	private static final String DISTANCE = "distance";

	/**
	 * A query whose reference travel time is still to be determined.
	 */
	public static TravelTimeQuery of(Node fromNode, Node toNode, double departureTime) {
		return new TravelTimeQuery(fromNode, toNode, departureTime, Double.NaN, Double.NaN, Double.NaN);
	}

	/**
	 * The same query with the reference filled in.
	 */
	public TravelTimeQuery withReference(double referenceTravelTime, double distance, double freeSpeedTravelTime) {
		return new TravelTimeQuery(this.fromNode, this.toNode, this.departureTime, referenceTravelTime, distance,
			freeSpeedTravelTime);
	}

	/**
	 * @return whether a reference travel time is known, i.e. whether this query can be evaluated
	 */
	public boolean hasReference() {
		return !Double.isNaN(this.referenceTravelTime);
	}

	/**
	 * Writes queries as {@code from_node,to_node,departure_time,reference_travel_time,distance} - one row
	 * per query, the shape the results are reported in, so that the queries and the results join on their
	 * first columns.
	 * <p>
	 * The file is how an experiment is repeated on exactly the same questions: with other matrix
	 * parameters, on another day, or - which is what the reference travel time column is for - by someone
	 * who fetched the travel times of these pairs from a routing API and wants them compared against the
	 * same matrix. Unknown values are written as empty fields rather than as {@code NaN}, so that every
	 * reader of the csv treats them as missing without being told.
	 */
	public static void write(List<TravelTimeQuery> queries, String path) {

		try (Writer writer = IOUtils.getBufferedWriter(path);
			 CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
				 .setHeader(FROM_NODE, TO_NODE, DEPARTURE_TIME, REFERENCE_TRAVEL_TIME, DISTANCE)
				 .build())) {

			for (TravelTimeQuery query : queries) {
				printer.printRecord(query.fromNode().getId(), query.toNode().getId(),
					format(query.departureTime(), 0), format(query.referenceTravelTime(), 1),
					format(query.distance(), 1));
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		log.info("Wrote {} queries to {}.", queries.size(), path);
	}

	/**
	 * Reads back what {@link #write} wrote, over the nodes of the given network. The reference travel time
	 * and the distance are optional, both as columns and per row.
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

			boolean hasReference = parser.getHeaderMap().containsKey(REFERENCE_TRAVEL_TIME);
			boolean hasDistance = parser.getHeaderMap().containsKey(DISTANCE);

			for (CSVRecord record : parser) {
				queries.add(new TravelTimeQuery(
					node(network, record.get(FROM_NODE), url),
					node(network, record.get(TO_NODE), url),
					Double.parseDouble(record.get(DEPARTURE_TIME).trim()),
					hasReference ? optional(record.get(REFERENCE_TRAVEL_TIME)) : Double.NaN,
					hasDistance ? optional(record.get(DISTANCE)) : Double.NaN,
					Double.NaN));
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		if (queries.isEmpty()) {
			throw new IllegalArgumentException("The query file " + url + " holds no rows.");
		}

		log.info("Read {} queries from {}, {} of them with a reference travel time.", queries.size(), url,
			queries.stream().filter(TravelTimeQuery::hasReference).count());
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
