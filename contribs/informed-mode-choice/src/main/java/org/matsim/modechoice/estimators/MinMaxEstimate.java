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


	/**
	 * Create a new minimum and maximum estimate.
	 */
	public static MinMaxEstimate of(double min, double max) {
		return new MinMaxEstimate(min, max);
	}


}
