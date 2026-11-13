package org.matsim.contrib.msp.traveltimematrix.evaluation;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.contrib.msp.traveltimematrix.TimeVaryingTravelTimeMatrix;
import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * How much congestion the evaluated queries actually carry, against how much the matrix applies to them.
 * Two numbers per group, and their difference is the whole explanation of the error table next to it.
 * <p>
 * <b>Why this exists.</b> The congestion index is a <em>volume-weighted</em> average over the links, so
 * it answers "how much longer does the average <b>vehicle</b> take" - see
 * {@link org.matsim.contrib.msp.traveltimematrix.congestion.CongestionIndexCalculator}. The matrix then
 * applies it to whatever pair it is asked about, and a pair is not a vehicle. Ask about pairs nobody
 * drove and most of their path lies on links that carried no traffic, were therefore never slowed down in
 * the run, and contribute no excess travel time to any reference measured on that run - while the index,
 * measured where the traffic was, still says the trip should be 8 % slower. The matrix then
 * <b>overestimates</b>, and not by a little: on a uniform sample of node pairs it can be worse than
 * applying no congestion model at all, which is the finding that put
 * {@link RunEvaluateTravelTimeMatrix.ODPairs#TRIPS} in front of
 * {@link RunEvaluateTravelTimeMatrix.ODPairs#RANDOM} as the default.
 * <p>
 * That is a statement about the <em>sample</em>, not about the model, and this class is what makes it
 * visible rather than leaving it to be inferred from a MAPE that got worse. For each group it reports:
 * <ul>
 *     <li><b>{@code realized_index}</b> - the excess travel time the reference carries over free speed,
 *     over the free-speed travel time: {@code sum(TT_ref - TT_ff) / sum(TT_ff)}. Deliberately the same
 *     functional form as the congestion index itself - a ratio of two sums, not a mean of ratios - only
 *     weighted by the queries of this evaluation instead of by links and volumes. It is the index these
 *     questions <em>deserve</em>.</li>
 *     <li><b>{@code applied_index}</b> - {@code beta * CI} as the matrix reads it, weighted the same
 *     way: {@code sum((factor - 1) * TT_ff) / sum(TT_ff)}. The same denominator as above is what makes
 *     the two comparable, and what makes {@code index_bias = applied - realized} the share of the
 *     reference travel time the congestion model adds in error.</li>
 *     <li><b>{@code share_without_excess}</b> - how many of the queries carry no excess travel time at
 *     all. This is the mechanism itself, counted: on real trips it is small, on generated pairs in a city
 *     sized run it is most of them.</li>
 * </ul>
 * None of it is an error metric, and that is on purpose: {@link TravelTimeErrorStats} already says how
 * wrong the matrix is, and this says <em>why</em> - whether the congestion model is miscalibrated, or
 * whether it is being asked about pairs whose congestion the run never recorded. The two tables group
 * the same queries identically (see {@link MatrixEvaluation#groupsOf}), so they join on
 * {@code reference,group,group_value}.
 * <p>
 * <b>One free-speed travel time per query</b> serves as the denominator and as the clock the index is
 * read at: the exact routed one ({@link TravelTimeQuery#freeSpeedTravelTime()}) when it is known, and the
 * matrix's own otherwise. With an exact matrix ({@code --max-neighbor-distance inf}) the two are the same
 * number, which is the configuration this diagnostic is meant to be read in. With a zonal matrix they
 * differ by its discretisation error, so {@code applied_index} is then the index the matrix would read at
 * the midpoint of the <em>exact</em> trip - a difference of at most one bin edge, and one that keeps the
 * two reported indices over a common denominator rather than letting the zone error leak into their
 * difference.
 *
 * @see MatrixEvaluation the error table this one explains
 */
public final class CongestionExposure {

	private static final Logger log = LogManager.getLogger(CongestionExposure.class);

	/**
	 * The columns after {@code reference,group,group_value}.
	 */
	public static final List<String> HEADER = List.of("n", "share_without_excess",
		"mean_free_speed_travel_time", "mean_reference_travel_time", "realized_index", "applied_index",
		"index_bias", "index_ratio");

	/**
	 * How much longer than free speed a reference travel time has to be for the query to count as
	 * carrying congestion at all [s]. One second, because a matrix and a router both truncate a travel
	 * time to whole seconds and the free-speed travel time of a long pair is a sum of such truncations -
	 * so a pair that was never slowed down lands within a second of free speed rather than exactly on it,
	 * and counting those as congested would hide the very thing this column is for.
	 */
	private static final double EXCESS_TOLERANCE = 1.0;

	/**
	 * The two weighted sums the indices are ratios of, plus the counts, accumulated over the queries of
	 * one group. Streaming for the same reason {@link TravelTimeErrorStats} is: the grouping must not
	 * cost memory per query.
	 */
	private static final class Sums {

		private long count;
		private long withoutExcess;
		private double freeSpeed;
		private double reference;
		private double appliedExcess;

		private void add(double freeSpeedTravelTime, double referenceTravelTime, double appliedIndex) {
			this.count++;
			this.freeSpeed += freeSpeedTravelTime;
			this.reference += referenceTravelTime;
			this.appliedExcess += appliedIndex * freeSpeedTravelTime;
			if (referenceTravelTime - freeSpeedTravelTime <= EXCESS_TOLERANCE) {
				this.withoutExcess++;
			}
		}

		private double realizedIndex() {
			return this.freeSpeed == 0 ? Double.NaN : (this.reference - this.freeSpeed) / this.freeSpeed;
		}

		private double appliedIndex() {
			return this.freeSpeed == 0 ? Double.NaN : this.appliedExcess / this.freeSpeed;
		}

		private List<String> toRecord() {
			double realized = realizedIndex();
			double applied = appliedIndex();
			return List.of(
				Long.toString(this.count),
				format(this.count == 0 ? Double.NaN : (double) this.withoutExcess / this.count, 4),
				format(this.count == 0 ? Double.NaN : this.freeSpeed / this.count, 1),
				format(this.count == 0 ? Double.NaN : this.reference / this.count, 1),
				format(realized, 4),
				format(applied, 4),
				format(applied - realized, 4),
				// a ratio of a positive index to one that rounds to nothing says "infinitely too much",
				// which is not a number a csv should carry - the bias next to it says the same thing in
				// index points and stays finite
				format(Math.abs(realized) < 1e-9 ? Double.NaN : applied / realized, 3));
		}

		private static String format(double value, int decimals) {
			return Double.isNaN(value) ? "" : String.format(Locale.ROOT, "%." + decimals + "f", value);
		}
	}

	/**
	 * reference -&gt; group key -&gt; sums.
	 */
	private final Map<String, Map<String, Sums>> sums;

	private CongestionExposure(Map<String, Map<String, Sums>> sums) {
		this.sums = sums;
	}

	/**
	 * Measures the exposure of a set of queries against every reference they are judged by.
	 *
	 * @param queries the queries the error table was computed over, so that the two tables describe the
	 * same sample
	 * @param references what "right" means, by the name each is reported under - the same map
	 * {@link MatrixEvaluation#evaluate} is given, so that the two tables join on the reference name
	 * @param matrix the level 0 matrix under test. Both of its numbers are needed and neither is on the
	 * {@link org.matsim.contrib.zone.skims.TravelTimeMatrix} interface: the free-speed travel time of a
	 * pair, and the congestion factor the index gives it - see
	 * {@link TimeVaryingTravelTimeMatrix#congestionFactor}. Pass the variant whose error is being
	 * explained; a free-speed variant would report an applied index of zero, which is true and useless.
	 * @param distanceBands lower edges of the distance bands [m], the same ones the error table uses
	 */
	public static CongestionExposure measure(Collection<TravelTimeQuery> queries,
		Map<String, ToDoubleFunction<TravelTimeQuery>> references, TimeVaryingTravelTimeMatrix matrix,
		double[] distanceBands) {

		if (references.isEmpty()) {
			throw new IllegalArgumentException("No reference was given, so there is no realized congestion to "
				+ "compare the applied index against.");
		}

		Map<String, Map<String, Sums>> sums = new LinkedHashMap<>();
		references.keySet().forEach(reference -> sums.put(reference, new LinkedHashMap<>()));

		long withoutFreeSpeed = 0;

		for (TravelTimeQuery query : queries) {

			double freeSpeedTravelTime = query.hasFreeSpeed()
				? query.freeSpeedTravelTime()
				: matrix.getFreeSpeedTravelTime(query.fromNode(), query.toNode());

			if (!(freeSpeedTravelTime > 0)) {
				// a pair of a node with itself, or two nodes a rounding apart: there is no free-speed
				// travel time to be a share of, and both indices would be 0/0
				withoutFreeSpeed++;
				continue;
			}

			double appliedIndex = matrix.congestionFactor(query.fromNode(), query.toNode(), query.departureTime(),
				freeSpeedTravelTime) - 1;
			List<String> groups = MatrixEvaluation.groupsOf(query, distanceBands);

			for (Map.Entry<String, ToDoubleFunction<TravelTimeQuery>> reference : references.entrySet()) {
				double right = reference.getValue().applyAsDouble(query);
				Map<String, Sums> byGroup = sums.get(reference.getKey());
				groups.forEach(group -> byGroup.computeIfAbsent(group, key -> new Sums())
					.add(freeSpeedTravelTime, right, appliedIndex));
			}
		}

		if (withoutFreeSpeed > 0) {
			log.info("{} of {} queries have a free-speed travel time of 0 s and are not in the congestion "
				+ "exposure table: there is nothing for an index to be a share of.", withoutFreeSpeed,
				queries.size());
		}
		if (sums.values().stream().allMatch(Map::isEmpty)) {
			throw new IllegalArgumentException("Not one of the " + queries.size() + " queries has a free-speed "
				+ "travel time above 0 s, so neither index is a share of anything. Either the queries are pairs of "
				+ "a node with itself, or the matrix answers 0 s - which a travel time matrix does only for a pair "
				+ "it considers identical.");
		}

		CongestionExposure exposure = new CongestionExposure(sums);
		exposure.logOverall();
		return exposure;
	}

	/**
	 * The excess travel time the reference carries over free speed, as a share of the free-speed travel
	 * time, over the whole sample. The index these queries deserve.
	 */
	public double getRealizedIndex(String reference) {
		return overall(reference).realizedIndex();
	}

	/**
	 * {@code beta * CI} as the matrix applied it, weighted over the same sample. The index these queries
	 * got.
	 */
	public double getAppliedIndex(String reference) {
		return overall(reference).appliedIndex();
	}

	/**
	 * The share of the queries whose reference travel time is no more than {@value #EXCESS_TOLERANCE} s
	 * above free speed, i.e. that carry no congestion for any index to reproduce.
	 */
	public double getShareWithoutExcess(String reference) {
		Sums overall = overall(reference);
		return overall.count == 0 ? Double.NaN : (double) overall.withoutExcess / overall.count;
	}

	private Sums overall(String reference) {
		Map<String, Sums> byGroup = this.sums.get(reference);
		if (byGroup == null) {
			throw new IllegalArgumentException("Nothing was measured against a reference '" + reference + "'. The "
				+ "references of this exposure are " + this.sums.keySet() + ".");
		}
		Sums overall = byGroup.get(MatrixEvaluation.key(MatrixEvaluation.GROUP_ALL, MatrixEvaluation.GROUP_ALL));
		if (overall == null) {
			throw new IllegalArgumentException("Not one query went into the congestion exposure against '"
				+ reference + "', so there is nothing to report.");
		}
		return overall;
	}

	/**
	 * Writes one row per reference and group:
	 * <pre>
	 * reference,group,group_value,n,share_without_excess,mean_free_speed_travel_time,...
	 * CONGESTED_ROUTING,all,all,18432,0.6412,842.1,871.3,0.0346,0.0812,0.0466,2.347
	 * CONGESTED_ROUTING,hour,8,1536,0.4120,...
	 * </pre>
	 * The long format and the group labels of {@link MatrixEvaluation#writeErrors}, so that the two files
	 * join on {@code reference,group,group_value} and a plot can put the error of a group next to the
	 * reason for it.
	 */
	public void write(String path) {

		List<String> header = new ArrayList<>(List.of("reference", "group", "group_value"));
		header.addAll(HEADER);

		try (Writer writer = IOUtils.getBufferedWriter(path);
			 CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
				 .setHeader(header.toArray(String[]::new))
				 .build())) {

			for (Map.Entry<String, Map<String, Sums>> reference : this.sums.entrySet()) {
				for (Map.Entry<String, Sums> group : reference.getValue().entrySet()) {
					String[] key = group.getKey().split("\t", 2);
					List<String> record = new ArrayList<>(List.of(reference.getKey(), key[0], key[1]));
					record.addAll(group.getValue().toRecord());
					printer.printRecord(record);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		log.info("Wrote the congestion the queries carry against the congestion the matrix applies, per reference "
			+ "and group, to {}.", path);
	}

	/**
	 * Logs the one sentence this class is for, per reference. It belongs in the log and not only in a csv
	 * because it is what a reader needs in order to believe - or disbelieve - the error table that was
	 * logged just before it.
	 */
	private void logOverall() {
		this.sums.keySet().forEach(reference -> log.info("Congestion exposure against {}: the queries carry {}% "
				+ "excess travel time over free speed, the matrix applies {}% - {}{} percentage points. {}% of them "
				+ "carry no excess at all, so there is nothing there for an index to reproduce.",
			reference,
			String.format(Locale.ROOT, "%.2f", 100 * getRealizedIndex(reference)),
			String.format(Locale.ROOT, "%.2f", 100 * getAppliedIndex(reference)),
			getAppliedIndex(reference) >= getRealizedIndex(reference) ? "+" : "",
			String.format(Locale.ROOT, "%.2f", 100 * (getAppliedIndex(reference) - getRealizedIndex(reference))),
			String.format(Locale.ROOT, "%.1f", 100 * getShareWithoutExcess(reference))));
	}
}
