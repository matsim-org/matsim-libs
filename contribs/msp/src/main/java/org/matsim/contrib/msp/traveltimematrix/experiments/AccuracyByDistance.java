package org.matsim.contrib.msp.traveltimematrix.experiments;

import org.matsim.contrib.msp.traveltimematrix.evaluation.MatrixEvaluation;
import org.matsim.contrib.msp.traveltimematrix.evaluation.TravelTimeErrorStats;
import org.matsim.contrib.msp.traveltimematrix.evaluation.TravelTimeQuery;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;

/**
 * The error of one answerer against the exact free-speed travel time, accumulated overall and per
 * distance band.
 * <p>
 * It exists because an experiment cannot use {@link MatrixEvaluation} for this. That class produces one
 * table of {@code references x variants} and knows nothing about a study area or a cell size, while every
 * row here has to carry both - so a sweep accumulates its own statistics. What it does <b>not</b> do is
 * compute them differently: the band labels come from {@link MatrixEvaluation#bandOf} and the metrics from
 * {@link TravelTimeErrorStats}, which are the same classes the evaluation uses, so a MAPE here and a MAPE
 * there are the same number.
 * <p>
 * The bands are keyed by their <b>index</b> and not by their label, so that the rows come out in the order
 * a reader expects: a sample is drawn origin by origin, so insertion order is not distance order, and
 * sorting the labels as strings would put {@code "10000-20000"} before {@code "2000-5000"}.
 */
final class AccuracyByDistance {

	/**
	 * Where a pair whose path nobody computed sorts: last. It belongs at the bottom of a table ordered by
	 * distance rather than in the middle of it, and {@link MatrixEvaluation#bandOf} labels it
	 * {@code "unknown"}.
	 */
	private static final int UNKNOWN_BAND = Integer.MAX_VALUE;

	private final double[] bands;
	private final TravelTimeErrorStats overall = new TravelTimeErrorStats();
	private final Map<Integer, TravelTimeErrorStats> byBand = new TreeMap<>();
	private final Map<Integer, String> labels = new TreeMap<>();

	/**
	 * @param bands lower edges of the distance bands [m], ascending
	 */
	AccuracyByDistance(double[] bands) {
		this.bands = bands.clone();
	}

	/**
	 * Adds one answered query.
	 *
	 * @param query carrying the exact free-speed travel time, which is the reference, and the length of
	 * the path, which decides the band
	 * @param estimate what the answerer said [s]
	 */
	void add(TravelTimeQuery query, double estimate) {

		this.overall.add(query.freeSpeedTravelTime(), estimate);

		int band = bandIndex(query.distance());
		this.labels.put(band, MatrixEvaluation.bandOf(query.distance(), this.bands));
		this.byBand.computeIfAbsent(band, index -> new TravelTimeErrorStats())
			.add(query.freeSpeedTravelTime(), estimate);
	}

	/**
	 * @return the error over the whole sample
	 */
	TravelTimeErrorStats getOverall() {
		return this.overall;
	}

	/**
	 * Hands out the overall row first and then one row per band, in ascending distance order - which is
	 * the order they are to be written in.
	 */
	void forEachGroup(BiConsumer<String, TravelTimeErrorStats> row) {
		Map<String, TravelTimeErrorStats> groups = new LinkedHashMap<>();
		groups.put(MatrixEvaluation.GROUP_ALL, this.overall);
		this.byBand.forEach((band, stats) -> groups.put(this.labels.get(band), stats));
		groups.forEach(row);
	}

	/**
	 * Where a distance sorts among the bands: below the first, inside band {@code i}, or unknown.
	 */
	private int bandIndex(double distance) {
		if (Double.isNaN(distance)) {
			return UNKNOWN_BAND;
		}
		for (int band = this.bands.length - 1; band >= 0; band--) {
			if (distance >= this.bands[band]) {
				return band;
			}
		}
		return -1;
	}
}
