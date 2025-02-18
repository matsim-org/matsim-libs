package org.matsim.modechoice;

import java.util.Objects;

/**
 * Combination of mode and availability option.
 */
public final class ModeEstimate {

	private final String mode;
	private final ModeAvailability option;

	private final double[] legEst;
	private final double[] tripEst;
	private final double[] actEst;

	/**
	 * Mark trips with no real usage. E.g pt trips that consist only of walk legs.
	 * These trips will not be considered during estimation.
	 */
	private final boolean[] noRealUsage;

	/**
	 * Whether this should be for a minimum estimate. Otherwise, maximum is assumed.
	 */
	private final boolean min;

	/**
	 * Whether this mode can be used at all.
	 */
	private final boolean usable;

	/**
	 * Constructor
	 *
	 * @param n            number of trips, i.e. estimates
	 * @param isMin        whether these are minimum estimates
	 * @param storeTripEst whether trip est needs to be stored
	 */
	ModeEstimate(String mode, ModeAvailability option, int n, boolean isUsable, boolean storeTripEst, boolean isMin) {
		this.mode = mode;
		this.option = option;
		this.min = isMin;
		this.usable = isUsable;
		this.legEst = usable ? new double[n] : null;
		this.tripEst = storeTripEst ? new double[n] : null;
		this.actEst = usable ? new double[n] : null;
		this.noRealUsage = usable ? new boolean[n] : null;
	}

	public String getMode() {
		return mode;
	}

	public ModeAvailability getOption() {
		return option;
	}

	public boolean isUsable() {
		return usable;
	}

	public boolean isMin() {
		return min;
	}

	public double[] getLegEstimates() {
		return legEst;
	}

	public double[] getActEst() {
		return actEst;
	}

	public double[] getTripEstimates() {
		return tripEst;
	}

	public boolean[] getNoRealUsage() {
		return noRealUsage;
	}

	@Override
	public String toString() {
		return mode + "=" + option + (min ? " (min) " : "");
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ModeEstimate that = (ModeEstimate) o;
		return min == that.min && mode.equals(that.mode) && option == that.option;
	}

	@Override
	public int hashCode() {
		return Objects.hash(mode, option, min);
	}

}
