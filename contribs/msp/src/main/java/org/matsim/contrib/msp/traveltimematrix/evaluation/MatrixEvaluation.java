package org.matsim.contrib.msp.traveltimematrix.evaluation;

import jakarta.annotation.Nullable;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.util.function.ToDoubleFunction;

/**
 * Compares anything that answers {@code getTravelTime(from, to, t)} against one or more reference travel
 * times, and writes the tables a paper reports: one row per query, and the error aggregated by time of
 * day and by distance.
 * <p>
 * The result is a <b>grid</b>, {@code references x variants}, and reading it that way is the whole point
 * of this class. A variant is an approximation under test; a reference is what "right" means for that
 * row. Neither is privileged - the same free-speed matrix is nearly exact against a free-speed router
 * and badly wrong against the travel times of a congested run, and those two cells answer two different
 * questions:
 * <pre>
 *                          |  FREE_SPEED_ROUTING   |  CONGESTED_ROUTING    |  EXPERIENCED
 *                          |  (no congestion)      |  (router on the run)  |  (what trips took)
 *  ------------------------+-----------------------+-----------------------+---------------------
 *   CONGESTED_ROUTING      |          -            |  self-check, = 0      |  router vs reality
 *   LEVEL_3_FIXED_PATH     |          -            |  cost of a fixed path |          -
 *   FREE_SPEED  (matrix)   |  the zone system      |  no congestion model  |  no congestion model
 *   GLOBAL_INDEX           |          -            |  level 0              |  level 0
 *   BY_ROAD_CLASS          |          -            |  level 1              |  level 1
 * </pre>
 * The two cells of the first column and the diagonal are what make the rest readable. Against
 * {@code FREE_SPEED_ROUTING} the free-speed matrix can be wrong about <b>nothing but its own zone
 * system</b> - no congestion is involved on either side - so that cell is the discretisation error, by
 * itself, and every other cell of the {@code FREE_SPEED} row contains it too. And the
 * {@code CONGESTED_ROUTING} variant against the {@code CONGESTED_ROUTING} reference is the same
 * computation on both sides, so its error can only be the rounding of a travel time to whole seconds -
 * which is the cheap check that the comparison is wired up at all.
 * <p>
 * It serves every study of this package, and that is the point of it taking plain
 * {@link TravelTimeQuery} objects: where the pairs come from (a generated {@link ODSample} or the trips
 * of a run, {@link ExperiencedLegs}) and what the references are (what a trip really took, or
 * {@link RoutedTravelTimes}) are independent choices, and none of them reaches this class.  The
 * grouping, the metrics and the csv are the same whatever was chosen, so the tables can be read side by
 * side.
 * <p>
 * Everything compared here is a {@link TravelTimeMatrix}, including the routed rungs
 * ({@link RoutedTravelTimes#asTimeDependentMatrix()}), so this class contains no knowledge of which
 * variant is which - it asks the same three-argument question of each of them and subtracts. Adding a
 * rung to the ladder therefore means adding a map entry, not touching this.
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
	 * <p>
	 * {@link #CONGESTED_ROUTING} is deliberately the name of a reference as well, because it <em>is</em>
	 * the same computation: the cell where the two meet is the self-check of the grid.
	 */
	public static final String CONGESTED_ROUTING = "CONGESTED_ROUTING";
	public static final String FIXED_PATH = "LEVEL_3_FIXED_PATH";
	public static final String CONSTANT_INDEX = "CONSTANT_INDEX";

	/**
	 * The group a row of the error table belongs to: everything, one hour of the day, or one distance
	 * band.
	 * <p>
	 * Public because this class is not the only one that groups the same queries the same way -
	 * {@link CongestionExposure} writes a second table over them - and two csvs of one study have to
	 * label their groups identically or they cannot be joined on {@code group,group_value}.
	 */
	public static final String GROUP_ALL = "all";
	public static final String GROUP_HOUR = "hour";
	public static final String GROUP_DISTANCE = "distance_band";

	/**
	 * The group a query belongs to in each of the three groupings, as the key a table of groups uses:
	 * the group name and its value, so that "hour 8" and "distance band 8" cannot collide.
	 * <p>
	 * Shared with {@link CongestionExposure} for the same reason the constants above are.
	 *
	 * @param distanceBands lower edges of the distance bands [m]
	 */
	public static List<String> groupsOf(TravelTimeQuery query, double[] distanceBands) {
		return List.of(
			key(GROUP_ALL, GROUP_ALL),
			key(GROUP_HOUR, Integer.toString((int) (query.departureTime() / 3600))),
			key(GROUP_DISTANCE, bandOf(query.distance(), distanceBands)));
	}

	/**
	 * The key of one group in a table of groups. {@link #writeErrors} splits it again to get the two
	 * columns back, so the separator must not occur in a group name or value.
	 */
	static String key(String group, String value) {
		return group + "\t" + value;
	}

	/**
	 * reference -> variant -> group -> error.
	 */
	private final Map<String, Map<String, Map<String, TravelTimeErrorStats>>> stats;
	private final double[] distanceBands;

	private MatrixEvaluation(Map<String, Map<String, Map<String, TravelTimeErrorStats>>> stats,
		double[] distanceBands) {
		this.stats = stats;
		this.distanceBands = distanceBands;
	}

	/**
	 * Evaluates every variant against every reference over a set of queries, however the queries were
	 * produced.
	 * <p>
	 * This is the whole comparison, and it is deliberately the only one: a generated sample judged by a
	 * router and the trips of a run judged by what they really took are the same subtraction over
	 * different inputs, so they produce error tables with the same columns that can be read side by side
	 * without wondering which of them computed its MAPE differently.
	 *
	 * @param queries what to ask - see {@link ODSample} and {@link ExperiencedLegs}
	 * @param references what "right" means, by the name each is reported under: a travel time [s] per
	 * query. Pass a {@link LinkedHashMap} to fix the order of the rows. Every reference has to be known
	 * for every query - see the exception below.
	 * @param variants what to evaluate, by the name they are reported under. Pass a
	 * {@link LinkedHashMap} to fix the order of the rows.
	 * @param distanceBands lower edges of the distance bands [m], e.g.
	 * {@link ODSample#DEFAULT_DISTANCE_BANDS}
	 * @param queryFile a csv of one row per query and variant, carrying every reference as a column, or
	 * null for the aggregates alone. It is written while the queries are evaluated, so it costs no
	 * memory - but it has as many rows as there are queries times variants.
	 * @throws IllegalArgumentException if a reference is unknown for a query, i.e. nothing has determined
	 * yet what the variants should have answered there. Refused rather than skipped: a hole that shrinks
	 * one row of the grid and not the others makes two cells silently incomparable.
	 */
	public static MatrixEvaluation evaluate(Collection<TravelTimeQuery> queries,
		Map<String, ToDoubleFunction<TravelTimeQuery>> references, Map<String, TravelTimeMatrix> variants,
		double[] distanceBands, @Nullable String queryFile) {

		if (references.isEmpty()) {
			throw new IllegalArgumentException("No reference was given, so there is nothing to compare the "
				+ "variants against.");
		}
		references.forEach((name, reference) -> checkComplete(queries, name, reference));

		Map<String, Map<String, Map<String, TravelTimeErrorStats>>> stats = new LinkedHashMap<>();
		references.keySet().forEach(reference -> {
			Map<String, Map<String, TravelTimeErrorStats>> byVariant = new LinkedHashMap<>();
			variants.keySet().forEach(variant -> byVariant.put(variant, new LinkedHashMap<>()));
			stats.put(reference, byVariant);
		});

		log.info("Evaluating {} variants {} against {} references {} over {} queries.", variants.size(),
			variants.keySet(), references.size(), references.keySet(), queries.size());

		try (CSVPrinter printer = queryPrinter(queryFile, references.keySet())) {

			for (TravelTimeQuery query : queries) {

				List<String> groups = groupsOf(query, distanceBands);

				for (Map.Entry<String, TravelTimeMatrix> variant : variants.entrySet()) {

					double estimate = variant.getValue().getTravelTime(query.fromNode(), query.toNode(),
						query.departureTime());

					for (Map.Entry<String, ToDoubleFunction<TravelTimeQuery>> reference : references.entrySet()) {

						double right = reference.getValue().applyAsDouble(query);
						Map<String, TravelTimeErrorStats> byGroup = stats.get(reference.getKey())
							.get(variant.getKey());

						groups.forEach(group -> byGroup.computeIfAbsent(group, key -> new TravelTimeErrorStats())
							.add(right, estimate));
					}

					if (printer != null) {
						List<String> record = new ArrayList<>(List.of(query.fromNode().getId().toString(),
							query.toNode().getId().toString(), TravelTimeQuery.format(query.departureTime(), 0),
							TravelTimeQuery.format(query.distance(), 1)));
						references.values().forEach(reference ->
							record.add(TravelTimeQuery.format(reference.applyAsDouble(query), 1)));
						record.add(variant.getKey());
						record.add(TravelTimeQuery.format(estimate, 1));
						printer.printRecord(record);
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
		evaluation.logOverall();
		return evaluation;
	}

	/**
	 * A reference that is unknown for some query is the one thing this class cannot work around, and the
	 * message has to say which reference and how many, because the cause is always a mismatch between
	 * the reference and where the queries came from - generated pairs nobody drove, a file written
	 * without travel times in it.
	 */
	private static void checkComplete(Collection<TravelTimeQuery> queries, String name,
		ToDoubleFunction<TravelTimeQuery> reference) {

		long without = queries.stream().filter(query -> Double.isNaN(reference.applyAsDouble(query))).count();
		if (without > 0) {
			throw new IllegalArgumentException(without + " of " + queries.size() + " queries have no travel time "
				+ "for the reference '" + name + "', so there is nothing to compare the variants against there. A "
				+ "generated origin-destination pair carries no travel time of its own - something has to determine "
				+ "one, e.g. RoutedTravelTimes.");
		}
	}

	@Nullable
	private static CSVPrinter queryPrinter(@Nullable String queryFile, Collection<String> references)
		throws IOException {

		if (queryFile == null) {
			return null;
		}

		List<String> header = new ArrayList<>(List.of("from_node", "to_node", "departure_time", "distance"));
		references.forEach(reference -> header.add("tt_ref_" + reference.toLowerCase(Locale.ROOT)));
		header.addAll(List.of("variant", "tt_estimate"));

		Writer writer = IOUtils.getBufferedWriter(queryFile);
		return new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
			.setHeader(header.toArray(String[]::new))
			.build());
	}

	/**
	 * The band a distance falls into, as the label the error table groups by: {@code "2000-5000"}, or
	 * {@code "20000+"} for the open-ended last band.
	 * <p>
	 * Public because an experiment that groups by distance without going through this class - the
	 * resolution sweep accumulates its own stats, one set per configuration - still has to label its
	 * bands the same way, or two tables of the same study cannot be joined.
	 */
	public static String bandOf(double distance, double[] bandEdges) {
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
	 * @return the references the variants were compared against, in the order they are reported
	 */
	public Collection<String> getReferences() {
		return this.stats.keySet();
	}

	/**
	 * @return the error of each variant against one reference over the whole sample, by variant name
	 */
	public Map<String, TravelTimeErrorStats> getOverall(String reference) {
		Map<String, TravelTimeErrorStats> overall = new LinkedHashMap<>();
		byVariant(reference).forEach((variant, groups) ->
			overall.put(variant, groups.get(key(GROUP_ALL, GROUP_ALL))));
		return overall;
	}

	/**
	 * @return the error of one variant against one reference in one group, e.g.
	 * {@code getStats("CONGESTED_ROUTING", "GLOBAL_INDEX", "hour", "8")}
	 */
	public TravelTimeErrorStats getStats(String reference, String variant, String group, String value) {
		Map<String, TravelTimeErrorStats> groups = byVariant(reference).get(variant);
		TravelTimeErrorStats found = groups == null ? null : groups.get(key(group, value));
		if (found == null) {
			throw new IllegalArgumentException("Nothing was evaluated for variant '" + variant + "' against "
				+ "reference '" + reference + "' in group '" + group + "' = '" + value + "'.");
		}
		return found;
	}

	private Map<String, Map<String, TravelTimeErrorStats>> byVariant(String reference) {
		Map<String, Map<String, TravelTimeErrorStats>> byVariant = this.stats.get(reference);
		if (byVariant == null) {
			throw new IllegalArgumentException("Nothing was compared against a reference '" + reference + "'. The "
				+ "references of this evaluation are " + getReferences() + ".");
		}
		return byVariant;
	}

	/**
	 * Logs the grid: one block per reference, one line per variant. The number a reader looks at first,
	 * so it goes into the log and not only into a csv.
	 */
	private void logOverall() {
		this.stats.keySet().forEach(reference -> {
			log.info("Against the reference {}:", reference);
			getOverall(reference).forEach((variant, overall) -> log.info("    {}: {}", variant, overall));
		});
	}

	/**
	 * Writes the error table: one row per reference, variant and group.
	 * <pre>
	 * reference,variant,group,group_value,n,mean_reference,mean_estimate,bias,relative_bias,mape,rmse,...
	 * CONGESTED_ROUTING,GLOBAL_INDEX,all,all,28800,712.4,...
	 * CONGESTED_ROUTING,GLOBAL_INDEX,hour,8,1200,...
	 * CONGESTED_ROUTING,GLOBAL_INDEX,distance_band,2000-5000,5760,...
	 * </pre>
	 * The long format rather than one column per metric per group: the groups are what a plot facets by,
	 * and a new grouping must not change the columns of the file. The same goes for the reference - it is
	 * a column, so adding one to an experiment adds rows and leaves every existing plot working.
	 */
	public void writeErrors(String path) {

		List<String> header = new ArrayList<>(List.of("reference", "variant", "group", "group_value"));
		header.addAll(TravelTimeErrorStats.HEADER);

		try (Writer writer = IOUtils.getBufferedWriter(path);
			 CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
				 .setHeader(header.toArray(String[]::new))
				 .build())) {

			for (Map.Entry<String, Map<String, Map<String, TravelTimeErrorStats>>> reference : this.stats.entrySet()) {
				for (Map.Entry<String, Map<String, TravelTimeErrorStats>> variant : reference.getValue().entrySet()) {
					for (Map.Entry<String, TravelTimeErrorStats> group : variant.getValue().entrySet()) {
						String[] key = group.getKey().split("\t", 2);
						List<String> record = new ArrayList<>(List.of(reference.getKey(), variant.getKey(), key[0],
							key[1]));
						record.addAll(group.getValue().toRecord());
						printer.printRecord(record);
					}
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		log.info("Wrote the error of {} variants against {} references {}, by time of day and by distance band "
				+ "({} m), to {}.", this.stats.values().iterator().next().size(), this.stats.size(),
			getReferences(), Arrays.toString(this.distanceBands), path);
	}
}
