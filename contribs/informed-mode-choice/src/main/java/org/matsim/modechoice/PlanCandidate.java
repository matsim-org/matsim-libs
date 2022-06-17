package org.matsim.modechoice;

import java.util.Arrays;

/**
 * Candidate for trip modes. One candidate is compared to others by the minimum utility estimate.
 */
public final class PlanCandidate implements Comparable<PlanCandidate> {

	private final String[] modes;

	/**
	 * Estimated minimum utility.
	 */
	private double utility;

	/**
	 * Max estimate
	 */
	private double max;

	public PlanCandidate(String[] modes, double utility) {
		this.modes = modes;
		this.utility = utility;
		this.max = utility;
	}

	/**
	 * Updates the minimum and maximum estimates accordingly.
	 */
	public void updateUtility(double utility) {
		this.utility = Math.min(this.utility, utility);
		this.max = Math.max(this.max, utility);
	}

	/**
	 * Total estimated utility.
	 */
	public double getUtility() {
		return utility;
	}

	/**
	 * Get mode for trip i.
	 */
	public String getMode(int i) {
		return modes[i];
	}

	@Override
	public int compareTo(PlanCandidate o) {
		return Double.compare(o.utility, utility);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PlanCandidate that = (PlanCandidate) o;
		return Arrays.equals(modes, that.modes);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(modes);
	}


	@Override
	public String toString() {
		return "PlanCandidate{" +
				"modes=" + Arrays.toString(modes) +
				", utility=" + utility + (utility != max ? "(max=" + max + " )" : "") +
				'}';
	}
}
