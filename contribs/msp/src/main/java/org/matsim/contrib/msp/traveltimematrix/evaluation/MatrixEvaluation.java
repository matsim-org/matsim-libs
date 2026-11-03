package org.matsim.contrib.msp.traveltimematrix.evaluation;

import jakarta.annotation.Nullable;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.msp.traveltimematrix.TimeVaryingTravelTimeMatrix;
import org.matsim.contrib.zone.skims.TravelTimeMatrix;
import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Compares anything that answers {@code getTravelTime(from, to, t)} against a reference travel time, and
 * writes the two tables a paper reports: one row per query, and the error aggregated by time of day and
 * by distance.
 * <p>
 * It serves both studies of this package, and that is the point of it taking plain {@link Query}
 * objects: the reference is the time-dependent optimum over an {@link ODSample} in one of them
 * ({@link RoutedTravelTimes}) and the experienced travel time of a car leg of the run in the other
 * ({@link ExperiencedLegs}), and the grouping, the metrics and the csv are the same either way - so the
 * two error tables can be read side by side without wondering which of them computed its MAPE
 * differently.
 * <p>
 * Everything compared here is a {@link TravelTimeMatrix}, the reference included
 * ({@link RoutedTravelTimes#asTimeDependentMatrix()}), so this class contains no knowledge of which
 * variant is which - it asks the same three-argument question of each of them and subtracts. Adding a
 * rung to the ladder therefore means adding a map entry, not touching this.
 * <p>
 * <b>What the error contains.</b> Two things, and they are worth keeping apart when reading the result.
 * The congestion model is one: a single index curve cannot know where the queue is. The zone system is
 * the other: for a long pair the base matrix answers with the travel time between the <em>central
 * nodes</em> of the two zones, not between the two nodes asked about. The query table therefore carries
 * the exact free-speed travel time of each pair ({@code tt_free_speed_path}) next to what the variants
 * answered, so the discretisation can be told from the approximation - and the {@code FREE_SPEED}
 * variant, whose only error is discretisation plus the absence of congestion, is the row that anchors
 * the comparison.
 *
 * @see RunEvaluateTravelTimeMatrix against a time-dependent router, on sampled pairs
 * @see RunValidateTravelTimeMatrix against the legs a run actually drove
 */
public final class MatrixEvaluation {

	private static final Logger log = LogManager.getLogger(MatrixEvaluation.class);

	/**
	 * The names the rungs that are not a {@link TimeVaryingTravelTimeMatrix} variant are reported under,
	 * so that the command and a test agree on them without either of them owning the strings. The
	 * variants themselves are reported under the name of their
	 * {@link org.matsim.contrib.msp.traveltimematrix.TravelTimeMatrixConfigGroup.Variant}.
	 */
	public static final String REFERENCE = "REFERENCE_TIME_DEPENDENT";
	public static final String FIXED_PATH = "LEVEL_3_FIXED_PATH";
	public static final String CONSTANT_INDEX = "CONSTANT_INDEX";

	/**
	 * The group a row of the error table belongs to: everything, one hour of the day, or one distance
	 * band.
	 */
	private static final String GROUP_ALL = "all";
	private static final String GROUP_HOUR = "hour";
	private static final String GROUP_DISTANCE = "distance_band";

	private final Map<String, Map<String, TravelTimeErrorStats>> stats;
	private final double[] distanceBands;

	private MatrixEvaluation(Map<String, Map<String, TravelTimeErrorStats>> stats,
		double[] distanceBands) {
		this.stats = stats;
		this.distanceBands = distanceBands;
	}

	/**
	 * One question put to every variant: a node pair, a departure time, and the travel time the variant
	 * is to be compared against.
	 *
	 * @param referenceTravelTime what the variant should have answered [s] - the time-dependent optimum
	 * of {@link RoutedTravelTimes} in the sampled study, the experienced travel time of
	 * {@link ExperiencedLegs} in the validation against a run
	 * @param distance the length of the path [m], which the error table groups by
	 * @param freeSpeedTravelTime the exact free-speed travel time of the pair [s], or
	 * {@link Double#NaN} when it is not known separately. It is reported next to the answers so that the
	 * discretisation of the zone system can be told from the congestion model.
	 */
	public record Query(Node fromNode, Node toNode, double departureTime, double referenceTravelTime,
						double distance, double freeSpeedTravelTime) {
	}

	/**
	 * Evaluates every variant over the sampled queries, against the time-dependent routing reference.
	 *
	 * @param reference the routing reference, which also carries the sample and the distances
	 * @param variants what to evaluate, by the name they are reported under. Pass a
	 * {@link java.util.LinkedHashMap} to fix the order of the tables; the reference itself may be among
	 * them and then comes out with the rounding of a travel time to whole seconds as its only error,
	 * which is a cheap check that the comparison is wired up correctly.
	 * @param distanceBands lower edges of the distance bands [m], e.g.
	 * {@link ODSample#DEFAULT_DISTANCE_BANDS}
	 * @param queryFile a csv of one row per query and variant, or null for the aggregates alone. It is
	 * written while the queries are evaluated, so it costs no memory - but it has as many rows as the
	 * sample has queries times variants.
	 */
	public static MatrixEvaluation evaluate(RoutedTravelTimes reference, Map<String, TravelTimeMatrix> variants,
		double[] distanceBands, @Nullable String queryFile) {

		ODSample sample = reference.getSample();
		double[] departureTimes = sample.getDepartureTimes();
		List<Query> queries = new ArrayList<>(sample.getNumberOfQueries());

		for (Node origin : sample.getOrigins()) {
			for (Node destination : sample.getDestinations(origin)) {
				RoutedTravelTimes.PairTravelTimes pair = reference.get(origin, destination);
				for (int t = 0; t < departureTimes.length; t++) {
					queries.add(new Query(origin, destination, departureTimes[t], pair.timeDependent()[t],
						pair.distance(), pair.freeSpeedTravelTime()));
				}
			}
		}

		return evaluate(queries, variants, distanceBands, queryFile);
	}

	/**
	 * Evaluates every variant over any set of queries - the sampled ones above, or the car legs a run
	 * actually drove ({@link ExperiencedLegs#asQueries()}).
	 * <p>
	 * This is the whole comparison, and it is the same code for both studies on purpose: the two error
	 * tables then have the same columns and the same grouping, and can be read side by side without
	 * asking which of them computed its MAPE differently.
	 *
	 * @see #evaluate(RoutedTravelTimes, Map, double[], String) for the parameters
	 */
	public static MatrixEvaluation evaluate(Collection<Query> queries, Map<String, TravelTimeMatrix> variants,
		double[] distanceBands, @Nullable String queryFile) {

		Map<String, Map<String, TravelTimeErrorStats>> stats = new LinkedHashMap<>();
		variants.keySet().forEach(variant -> stats.put(variant, new LinkedHashMap<>()));

		log.info("Evaluating {} variants {} over {} queries.", variants.size(), variants.keySet(), queries.size());

		try (CSVPrinter printer = queryPrinter(queryFile)) {

			for (Query query : queries) {

				String distanceBand = bandOf(query.distance(), distanceBands);
				String hour = Integer.toString((int) (query.departureTime() / 3600));

				for (Map.Entry<String, TravelTimeMatrix> variant : variants.entrySet()) {

					double estimate = variant.getValue().getTravelTime(query.fromNode(), query.toNode(),
						query.departureTime());
					Map<String, TravelTimeErrorStats> groups = stats.get(variant.getKey());

					add(groups, GROUP_ALL, GROUP_ALL, query.referenceTravelTime(), estimate);
					add(groups, GROUP_HOUR, hour, query.referenceTravelTime(), estimate);
					add(groups, GROUP_DISTANCE, distanceBand, query.referenceTravelTime(), estimate);

					if (printer != null) {
						printer.printRecord(query.fromNode().getId(), query.toNode().getId(),
							format(query.departureTime(), 0), format(query.distance(), 1),
							format(query.freeSpeedTravelTime(), 1), format(query.referenceTravelTime(), 1),
							variant.getKey(), format(estimate, 1));
					}
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		if (queryFile != null) {
			log.info("Wrote {} query results to {}.", (long) queries.size() * variants.size(), queryFile);
		}

		MatrixEvaluation evaluation = new MatrixEvaluation(stats, distanceBands.clone());
		evaluation.getOverall().forEach((variant, overall) -> log.info("{}: {}", variant, overall));
		return evaluation;
	}

	/**
	 * The group key is {@code group} plus its value, so that "hour 8" and "distance band 8" cannot
	 * collide.
	 */
	private static void add(Map<String, TravelTimeErrorStats> groups, String group, String value, double reference,
		double estimate) {
		groups.computeIfAbsent(group + "\t" + value, key -> new TravelTimeErrorStats()).add(reference, estimate);
	}

	@Nullable
	private static CSVPrinter queryPrinter(@Nullable String queryFile) throws IOException {
		if (queryFile == null) {
			return null;
		}
		Writer writer = IOUtils.getBufferedWriter(queryFile);
		return new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
			.setHeader("from_node", "to_node", "departure_time", "distance", "tt_free_speed_path", "tt_reference",
				"variant", "tt_estimate")
			.build());
	}

	/**
	 * A number for the query csv. Fixed decimals rather than whatever {@code Double.toString} produces,
	 * so that the file stays diffable and does not carry an accuracy a travel time does not have.
	 */
	private static String format(double value, int decimals) {
		// an unknown free-speed travel time is an empty field and not the string "NaN", which every
		// reader of the csv would then have to know to filter out
		return Double.isNaN(value) ? "" : String.format(Locale.ROOT, "%." + decimals + "f", value);
	}

	/**
	 * The band a distance falls into, as the label the error table groups by: {@code "2000-5000"}, or
	 * {@code "20000+"} for the open-ended last band.
	 */
	static String bandOf(double distance, double[] bandEdges) {
		for (int band = bandEdges.length - 1; band >= 0; band--) {
			if (distance >= bandEdges[band]) {
				return band + 1 < bandEdges.length
					? (long) bandEdges[band] + "-" + (long) bandEdges[band + 1]
					: (long) bandEdges[band] + "+";
			}
		}
		return "<" + (long) bandEdges[0];
	}

	/**
	 * @return the error of each variant over the whole sample, by variant name
	 */
	public Map<String, TravelTimeErrorStats> getOverall() {
		Map<String, TravelTimeErrorStats> overall = new LinkedHashMap<>();
		this.stats.forEach((variant, groups) -> overall.put(variant, groups.get(GROUP_ALL + "\t" + GROUP_ALL)));
		return overall;
	}

	/**
	 * @return the error of one variant in one group, e.g. {@code getStats("GLOBAL_INDEX", "hour", "8")}
	 */
	public TravelTimeErrorStats getStats(String variant, String group, String value) {
		Map<String, TravelTimeErrorStats> groups = this.stats.get(variant);
		TravelTimeErrorStats found = groups == null ? null : groups.get(group + "\t" + value);
		if (found == null) {
			throw new IllegalArgumentException("Nothing was evaluated for variant '" + variant + "' in group '"
				+ group + "' = '" + value + "'.");
		}
		return found;
	}

	/**
	 * Writes the error table: one row per variant and group.
	 * <pre>
	 * variant,group,group_value,n,mean_reference,mean_estimate,bias,relative_bias,mape,rmse,max_absolute_percentage_error
	 * GLOBAL_INDEX,all,all,28800,712.4,...
	 * GLOBAL_INDEX,hour,8,1200,...
	 * GLOBAL_INDEX,distance_band,2000-5000,5760,...
	 * </pre>
	 * The long format rather than one column per metric per group: the groups are what a plot facets by,
	 * and a new grouping must not change the columns of the file.
	 */
	public void writeErrors(String path) {

		List<String> header = new ArrayList<>(List.of("variant", "group", "group_value"));
		header.addAll(TravelTimeErrorStats.HEADER);

		try (Writer writer = IOUtils.getBufferedWriter(path);
			 CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
				 .setHeader(header.toArray(String[]::new))
				 .build())) {

			for (Map.Entry<String, Map<String, TravelTimeErrorStats>> variant : this.stats.entrySet()) {
				for (Map.Entry<String, TravelTimeErrorStats> group : variant.getValue().entrySet()) {
					String[] key = group.getKey().split("\t", 2);
					List<String> record = new ArrayList<>(List.of(variant.getKey(), key[0], key[1]));
					record.addAll(group.getValue().toRecord());
					printer.printRecord(record);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		log.info("Wrote the error of {} variants by time of day and by distance band ({} m) to {}.",
			this.stats.size(), Arrays.toString(this.distanceBands), path);
	}
}
