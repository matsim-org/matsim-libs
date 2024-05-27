package org.matsim.modechoice;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Combination of mode and availability option.
 */
public final class ModeEstimate {

	private final String mode;
	private final ModeAvailability option;

	private final double[] est;
	private final double[] tripEst;

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
		this.est = usable ? new double[n] : null;
		this.tripEst = storeTripEst ? new double[n] : null;
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

	public double[] getEstimates() {
		return est;
	}

	public double[] getTripEstimates() {
		return tripEst;
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
