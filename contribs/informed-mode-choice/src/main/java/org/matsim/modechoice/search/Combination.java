package org.matsim.modechoice.search;

import com.google.common.collect.Lists;
import org.matsim.modechoice.PlanModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Combination o mode and availability option.
 */
final class Combination {

	private final String mode;
	private final Enum<?> option;

	private final double[] est;
	private final double[] tripEst;

	/**
	 * Whether this should be for a minimum estimate. Otherwise, maximum is assumed.
	 */
	private final boolean min;

	/**
	 * Constructor
	 * @param n number of trips, i.e. estimates
	 * @param isMin whether these are minimum estimates
	 * @param storeTripEst whether trip est needs to be stored
	 */
	Combination(String mode, Enum<?> option, int n, boolean storeTripEst, boolean isMin) {
		this.mode = mode;
		this.option = option;
		this.min = isMin;
		this.est = new double[n];
		this.tripEst = storeTripEst ? new double[n] : null;
	}

	public String getMode() {
		return mode;
	}

	public Enum<?> getOption() {
		return option;
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
		Combination that = (Combination) o;
		return min == that.min && mode.equals(that.mode) && option == that.option;
	}

	@Override
	public int hashCode() {
		return Objects.hash(mode, option, min);
	}

	/**
	 * Return all possible choice combinations.
	 */
	public static List<List<Combination>> combinations(Map<String, List<Combination>> combinations) {

		List<List<Combination>> collect = new ArrayList<>(combinations.values());
		return Lists.cartesianProduct(collect);
	}

}
