package org.matsim.modechoice.estimators;


/**
 * A minimum and maximum estimate.
 */
public final class MinMaxEstimate {

	private final double min;
	private final double max;

	private MinMaxEstimate(double min, double max) {
		this.min = min;
		this.max = max;
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	/**
	 * Create a new minimum and maximum estimate.
	 */
	public static MinMaxEstimate of(double min, double max) {
		return new MinMaxEstimate(min, max);
	}

	/**
	 * Create a new minimum estimate.
	 */
	public static MinMaxEstimate ofMin(double min) {
		return new MinMaxEstimate(min, Double.NaN);
	}

}
