package org.matsim.modechoice;

import com.google.common.collect.Lists;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.router.TripStructureUtils;

import javax.annotation.Nullable;
import java.util.*;

/**
 * A coarse model of the daily plan containing the trips and legs for using each mode.
 */
public final class PlanModel {

	private final TripStructureUtils.Trip[] trips;

	/**
	 * Routed legs.
	 */
	private final Map<String, List<Leg>[]> legs;

	public PlanModel(List<TripStructureUtils.Trip> trips) {
		this.trips = trips.toArray(new TripStructureUtils.Trip[0]);
		this.legs = new IdentityHashMap<>();
	}

	public int trips() {
		return trips.length;
	}

	/**
	 * Number of stored modes.
	 */
	public int modes() {
		return legs.size();
	}

	/**
	 * Get ith trip of the day.
	 */
	public TripStructureUtils.Trip getTrip(int i) {
		return trips[i];
	}

	void setLegs(String mode, List<Leg>[] legs) {
		this.legs.put(mode.intern(), legs);
	}

	/**
	 * Return the legs of using a mode for ith trip. This can explicitly be null if a mode can not be used for this trip.
	 */
	@Nullable
	public List<Leg> getLegs(String mode, int i) {
		List<Leg>[] legs = this.legs.get(mode);

		// Can happen if a leg was not routed at all
		if (legs == null)
			return null;

		return legs[i];
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();

		b.append("PlanModel{").append("trips=").append(trips()).append(", legs={ ");

		// Outputs the number of legs per trip
		for (Map.Entry<String, List<Leg>[]> e : legs.entrySet()) {
			b.append(e.getKey()).append("=[");

			int length = e.getValue().length;
			for (int i = 0; i < length; i++) {

				List<Leg> v = e.getValue()[i];
				if (v == null)
					b.append("null");
				else
					b.append(v.size());

				if (i != length - 1)
					b.append(",");
			}

			b.append("] ");
		}

		b.append("}}");

		return b.toString();
	}

	/**
	 * Combination o mode and availability option.
	 */
	public static final class Combination {

		private final String mode;
		private final Enum<?> option;

		/**
		 * Whether this should be for a maximum estimate. Otherwise, minimum is assumed.
		 */
		private final boolean max;

		public Combination(String mode, Enum<?> option) {
			this.mode = mode;
			this.option = option;
			this.max = false;
		}

		public Combination(String mode, Enum<?> option, boolean max) {
			this.mode = mode;
			this.option = option;
			this.max = max;
		}

		public String getMode() {
			return mode;
		}

		public Enum<?> getOption() {
			return option;
		}

		public boolean isMax() {
			return max;
		}

		@Override
		public String toString() {
			return mode + "=" + option + (max ? " (max) " : "");
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Combination that = (Combination) o;
			return max == that.max && mode.equals(that.mode) && option == that.option;
		}

		@Override
		public int hashCode() {
			return Objects.hash(mode, option, max);
		}
	}

	/**
	 * Return all possible choice combinations.
	 */
	public static List<List<Combination>> combinations(Map<String, List<Combination>> combinations) {

		List<List<Combination>> collect = new ArrayList<>(combinations.values());
		return Lists.cartesianProduct(collect);
	}

}
