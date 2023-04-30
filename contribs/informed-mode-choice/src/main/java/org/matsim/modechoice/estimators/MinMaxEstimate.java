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
	 * Create a new maximum estimate. This is the default when only one estimate can be provided.
	 * An estimate should never overestimate the costs, i.e. never underestimate the utility.
	 * This should therefore return the maximum achievable utility.
	 */
	public static MinMaxEstimate ofMax(double max) {
		return new MinMaxEstimate(Double.NaN, max);
	}

	@Override
	public String toString() {
		return "Estimate{" +
				"min=" + min +
				", max=" + max +
				'}';
	}
}
