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
 * It serves every study of this package, and that is the point of it taking plain
 * {@link TravelTimeQuery} objects: where the pairs come from (a generated {@link ODSample} or the trips
 * of a run, {@link ExperiencedLegs}) and what determines the reference travel time (what the trip really
 * took, or {@link RoutedTravelTimes}) are two independent choices, and none of them reaches this class.
 * The grouping, the metrics and the csv are the same whatever was chosen, so the tables can be read side
 * by side.
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
 * @see RunEvaluateTravelTimeMatrix the command that composes the choices
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
	 * Evaluates every variant over a set of queries, however they were produced and whatever filled in
	 * their reference travel time.
	 * <p>
	 * This is the whole comparison, and it is deliberately the only one: a generated sample judged by a
	 * router and the trips of a run judged by what they really took are the same subtraction over
	 * different inputs, so they produce error tables with the same columns that can be read side by side
	 * without wondering which of them computed its MAPE differently.
	 *
	 * @param queries what to ask, each with a reference travel time - see {@link ODSample},
	 * {@link ExperiencedLegs} and {@link RoutedTravelTimes}
	 * @param variants what to evaluate, by the name they are reported under. Pass a
	 * {@link java.util.LinkedHashMap} to fix the order of the tables; the reference itself may be among
	 * them and then comes out with the rounding of a travel time to whole seconds as its only error,
	 * which is a cheap check that the comparison is wired up correctly.
	 * @param distanceBands lower edges of the distance bands [m], e.g.
	 * {@link ODSample#DEFAULT_DISTANCE_BANDS}
	 * @param queryFile a csv of one row per query and variant, or null for the aggregates alone. It is
	 * written while the queries are evaluated, so it costs no memory - but it has as many rows as there
	 * are queries times variants.
	 * @throws IllegalArgumentException if a query has no reference travel time, i.e. nothing has
	 * determined yet what the variants should have answered
	 */
	public static MatrixEvaluation evaluate(Collection<TravelTimeQuery> queries,
		Map<String, TravelTimeMatrix> variants, double[] distanceBands, @Nullable String queryFile) {

		long withoutReference = queries.stream().filter(query -> !query.hasReference()).count();
		if (withoutReference > 0) {
			throw new IllegalArgumentException(withoutReference + " of " + queries.size() + " queries have no "
				+ "reference travel time, so there is nothing to compare the variants against. A generated "
				+ "origin-destination pair carries no travel time of its own - something has to determine one, "
				+ "e.g. RoutedTravelTimes.");
		}

		Map<String, Map<String, TravelTimeErrorStats>> stats = new LinkedHashMap<>();
		variants.keySet().forEach(variant -> stats.put(variant, new LinkedHashMap<>()));

		log.info("Evaluating {} variants {} over {} queries.", variants.size(), variants.keySet(), queries.size());

		try (CSVPrinter printer = queryPrinter(queryFile)) {

			for (TravelTimeQuery query : queries) {

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
							TravelTimeQuery.format(query.departureTime(), 0),
							TravelTimeQuery.format(query.distance(), 1),
							TravelTimeQuery.format(query.freeSpeedTravelTime(), 1),
							TravelTimeQuery.format(query.referenceTravelTime(), 1),
							variant.getKey(), TravelTimeQuery.format(estimate, 1));
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
	 * The band a distance falls into, as the label the error table groups by: {@code "2000-5000"}, or
	 * {@code "20000+"} for the open-ended last band.
	 */
	static String bandOf(double distance, double[] bandEdges) {
		if (Double.isNaN(distance)) {
			// a query whose path nobody computed - an API reference, say. Its own group rather than a
			// guess, so that it neither disappears from the table nor pollutes a band it may not be in.
			return "unknown";
		}
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
