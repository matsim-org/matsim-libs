package org.matsim.modechoice;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.TripStructureUtils;

import java.util.Arrays;

/**
 * Candidate for trip modes. One candidate is compared to others by the minimum utility estimate.
 */
public final class PlanCandidate implements Comparable<PlanCandidate> {

	public static final String MIN_ESTIMATE = "min_estimate";
	public static final String MAX_ESTIMATE = "max_estimate";

	private final String[] modes;

	/**
	 * Estimated minimum utility. An estimate should never underestimate the utility.
	 */
	private double utility;

	/**
	 * Max estimate. This is the maximum expected utility that could be achieved.
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
	public PlanCandidate updateUtility(double utility) {
		this.utility = Math.min(this.utility, utility);
		this.max = Math.max(this.max, utility);
		return this;
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

	/**
	 * Applies the routing modes of this candidate to a plan.
	 *
	 * @param plan
	 */
	public void applyTo(Plan plan) {

		String id = getPlanType(modes);

		plan.setType(id);
		plan.setScore(null);

		plan.getAttributes().putAttribute(MIN_ESTIMATE, utility);
		plan.getAttributes().putAttribute(MAX_ESTIMATE, max);

		int k = 0;
		for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan)) {

			String mode = modes[k];

			// modes could be not specified
			if (mode == null)
				continue;

			// TODO: check if staging activities are correct

			for (Leg leg : trip.getLegsOnly()) {

				leg.setRoute(null);
				leg.setMode(mode);
				TripStructureUtils.setRoutingMode(leg, mode);
			}

			k++;
		}
	}

	/**
	 * Return identifier for chosen modes.
	 */
	public static String getPlanType(String[] modes) {

		StringBuilder b = new StringBuilder();
		for (int i = 0; i < modes.length; i++) {

			b.append(modes[i]);
			if (i != modes.length - 1)
				b.append("-");
		}

		return b.toString();
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
				", utility=" + utility + (utility != max ? " (max=" + max + ")" : "") +
				'}';
	}
}
