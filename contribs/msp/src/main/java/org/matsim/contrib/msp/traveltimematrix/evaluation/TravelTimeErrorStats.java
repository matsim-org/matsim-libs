package org.matsim.contrib.msp.traveltimematrix.evaluation;

import java.util.List;
import java.util.Locale;

/**
 * The error of one approximation against the reference, accumulated over queries: MAPE, bias, RMSE.
 * <p>
 * Streaming on purpose - it keeps seven numbers, not the queries - so that an evaluation can group by
 * anything it likes (time of day, distance band, road class, all of them at once) without the memory
 * of the grouping growing with the sample.
 * <p>
 * <b>Why both MAPE and bias.</b> They answer different questions and the matrix is interesting in
 * exactly their difference. {@link #getMape()} is the mean of {@code |error| / reference} and says how
 * far off a single travel time typically is. {@link #getRelativeBias()} is the mean of the
 * <em>signed</em> {@code error / reference} and says whether the approximation is systematically too
 * high or too low - which is what matters when travel times are summed, as in an accessibility or
 * capability analysis, because a bias accumulates where a scatter cancels out. A bias also has a
 * remedy: it is what the calibration factor {@code beta} of
 * {@link org.matsim.contrib.msp.traveltimematrix.TravelTimeMatrixConfigGroup#getCalibrationFactor()} is
 * fitted on.
 * <p>
 * Percentages are shares, not percent: {@code 0.05} is 5%.
 */
public final class TravelTimeErrorStats {

	/**
	 * The header of {@link #toRecord()}, for a csv of one row per group.
	 */
	public static final List<String> HEADER = List.of("n", "mean_reference", "mean_estimate", "bias",
		"error_std_dev", "mean_absolute_error", "relative_bias", "mape", "rmse", "max_absolute_percentage_error");

	private long count;
	private long countWithoutPercentage;
	private double sumReference;
	private double sumEstimate;
	private double sumError;
	private double sumAbsoluteError;
	private double sumSquaredError;
	private double sumRelativeError;
	private double sumAbsoluteRelativeError;
	private double maxAbsoluteRelativeError;

	/**
	 * Adds one query.
	 *
	 * @param reference the reference travel time [s], see {@link RoutedTravelTimes}
	 * @param estimate what the approximation under test answered [s]
	 */
	public void add(double reference, double estimate) {

		double error = estimate - reference;

		this.count++;
		this.sumReference += reference;
		this.sumEstimate += estimate;
		this.sumError += error;
		this.sumAbsoluteError += Math.abs(error);
		this.sumSquaredError += error * error;

		if (reference > 0) {
			double relativeError = error / reference;
			this.sumRelativeError += relativeError;
			this.sumAbsoluteRelativeError += Math.abs(relativeError);
			this.maxAbsoluteRelativeError = Math.max(this.maxAbsoluteRelativeError, Math.abs(relativeError));
		} else {
			// a reference of 0 s - the pair of a node with itself, or two nodes a rounding apart - has no
			// percentage error at all, and counting it as 0% would quietly improve every mean it is in
			this.countWithoutPercentage++;
		}
	}

	/**
	 * @return how many queries went in
	 */
	public long getCount() {
		return this.count;
	}

	/**
	 * @return how many of them had a reference travel time of 0 s and are therefore in the absolute
	 * statistics but not in the percentages
	 */
	public long getCountWithoutPercentage() {
		return this.countWithoutPercentage;
	}

	/**
	 * @return the mean reference travel time [s], which says what the percentages below are a percentage
	 * of
	 */
	public double getMeanReference() {
		return mean(this.sumReference, this.count);
	}

	/**
	 * @return the mean travel time of the approximation [s]
	 */
	public double getMeanEstimate() {
		return mean(this.sumEstimate, this.count);
	}

	/**
	 * @return the mean signed error [s]. Positive means the approximation is too slow, which is the
	 * expected direction: it holds the free-speed path fixed under congestion.
	 */
	public double getBias() {
		return mean(this.sumError, this.count);
	}

	/**
	 * @return the mean absolute error [s]. The companion of {@link #getBias()}: the bias says which way
	 * the approximation leans, this says how far off it is regardless of direction, in seconds rather
	 * than as a share - which is what matters when the reference travel times span a factor of twenty, as
	 * they do across distance bands, and a percentage of a two-minute trip is not comparable to a
	 * percentage of a forty-minute one.
	 */
	public double getMeanAbsoluteError() {
		return mean(this.sumAbsoluteError, this.count);
	}

	/**
	 * @return the standard deviation of the signed error [s]: the part of the error that does <em>not</em>
	 * cancel out over many pairs, as opposed to {@link #getBias()}, which is the part that does.
	 * <p>
	 * The two together are the whole of {@link #getRmse()}, since
	 * {@code rmse^2 = bias^2 + stdDev^2}. That identity is why both are reported: a calibration factor
	 * can remove a bias and can do nothing at all about a standard deviation, so the two halves say
	 * different things about whether an approximation is fixable.
	 */
	public double getStandardDeviation() {
		double bias = getBias();
		return Math.sqrt(Math.max(0, mean(this.sumSquaredError, this.count) - bias * bias));
	}

	/**
	 * @return the mean signed relative error, as a share of the reference
	 */
	public double getRelativeBias() {
		return mean(this.sumRelativeError, this.count - this.countWithoutPercentage);
	}

	/**
	 * @return the mean absolute percentage error, as a share
	 */
	public double getMape() {
		return mean(this.sumAbsoluteRelativeError, this.count - this.countWithoutPercentage);
	}

	/**
	 * @return the root mean squared error [s], which weighs the large errors the MAPE averages away
	 */
	public double getRmse() {
		return Math.sqrt(mean(this.sumSquaredError, this.count));
	}

	/**
	 * @return the worst single relative error, as a share. The number that says whether a good MAPE
	 * hides a pair that is wrong by a factor.
	 */
	public double getMaxAbsolutePercentageError() {
		return this.maxAbsoluteRelativeError;
	}

	private static double mean(double sum, long count) {
		return count == 0 ? Double.NaN : sum / count;
	}

	/**
	 * One csv row, in the order of {@link #HEADER}. Travel times to the second, shares to four decimals -
	 * a MAPE is not meaningful beyond that and a fixed format keeps the file diffable.
	 */
	public List<String> toRecord() {
		return List.of(
			Long.toString(this.count),
			format(getMeanReference(), 1),
			format(getMeanEstimate(), 1),
			format(getBias(), 1),
			format(getStandardDeviation(), 1),
			format(getMeanAbsoluteError(), 1),
			format(getRelativeBias(), 4),
			format(getMape(), 4),
			format(getRmse(), 1),
			format(getMaxAbsolutePercentageError(), 4));
	}

	private static String format(double value, int decimals) {
		return String.format(Locale.ROOT, "%." + decimals + "f", value);
	}

	@Override
	public String toString() {
		return String.format(Locale.ROOT, "n = %d, MAPE = %.1f%%, bias = %+.1f s (%+.1f%%), sd = %.1f s, "
				+ "MAE = %.1f s, RMSE = %.1f s", this.count, 100 * getMape(), getBias(), 100 * getRelativeBias(),
			getStandardDeviation(), getMeanAbsoluteError(), getRmse());
	}
}
