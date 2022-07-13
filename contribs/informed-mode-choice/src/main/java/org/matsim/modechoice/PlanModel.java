package org.matsim.modechoice;

import com.google.common.collect.Lists;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

/**
 * A coarse model of the daily plan containing the trips and legs for using each mode.
 */
public final class PlanModel implements Iterable<TripStructureUtils.Trip>, HasPerson {

	private final TripStructureUtils.Trip[] trips;
	private final Person person;

	/**
	 * Routed trips for each mode and all legs.
	 */
	private final Map<String, List<Leg>[]> legs;

	/**
	 * Estimates for all modes and all available {@link ModeOptions}.
	 */
	private final Map<String, List<ModeEstimate>> estimates;

	/**
	 * Original plan.
	 */
	private final Plan plan;

	/**
	 * Create a new plan model instance from an existing plan.
	 */
	public static PlanModel newInstance(Plan plan) {
		return new PlanModel(plan);
	}

	private PlanModel(Plan plan) {
		this.person = plan.getPerson();
		this.trips = TripStructureUtils.getTrips(plan).toArray(new TripStructureUtils.Trip[0]);
		this.plan = plan;
		this.legs = new HashMap<>();
		this.estimates = new HashMap<>();
	}

	@Override
	public Person getPerson() {
		return person;
	}

	public Plan getPlan() {
		return plan;
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
	 * Estimated bee line distance.
	 */
	public double distance() {

		double dist = 0;
		for (TripStructureUtils.Trip trip : trips) {
			if (trip.getOriginActivity().getCoord() == null || trip.getDestinationActivity().getCoord() == null)
				throw new IllegalStateException("No coordinates given");

			dist += CoordUtils.calcEuclideanDistance(trip.getOriginActivity().getCoord(), trip.getDestinationActivity().getCoord());
		}

		return dist;
	}

	/**
	 * Get ith trip of the day.
	 */
	public TripStructureUtils.Trip getTrip(int i) {
		return trips[i];
	}

	/**
	 * Get the mode of the ith trip.
	 */
	public String getTripMode(int i) {
		return TripStructureUtils.getRoutingMode(trips[i].getLegsOnly().get(0));
	}

	void setLegs(String mode, List<Leg>[] legs) {
		mode = mode.intern();

		List<Leg>[] existing = this.legs.putIfAbsent(mode, legs);

		if (existing != null) {

			if (legs.length != existing.length)
				throw new IllegalArgumentException(String.format("Existing legs have different length than the newly provided: %d vs. %d", existing.length, legs.length));

			// Copy existing non-null legs
			for (int i = 0; i < legs.length; i++) {
				List<Leg> l = legs[i];
				if (l != null)
					existing[i] = l;
			}
		}
	}

	void putEstimate(String mode, List<ModeEstimate> options) {
		this.estimates.put(mode, options);
	}

	/**
	 * Stored estimates.
	 */
	public Map<String, List<ModeEstimate>> getEstimates() {
		return estimates;
	}

	public Set<String> filterModes(Predicate<? super ModeEstimate> predicate) {
		Set<String> modes = new HashSet<>();
		for (Map.Entry<String, List<ModeEstimate>> e : estimates.entrySet()) {
			if (e.getValue().stream().anyMatch(predicate))
				modes.add(e.getKey());
		}

		return modes;
	}

	/**
	 * Check io estimates are present. Otherwise call {@link PlanModelService}
	 */
	public boolean hasEstimates() {
		return !this.estimates.isEmpty();
	}

	/**
	 * Return all possible choice combinations.
	 */
	public List<List<ModeEstimate>> combinations() {
		List<List<ModeEstimate>> collect = new ArrayList<>(estimates.values());
		return Lists.cartesianProduct(collect);
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

	@Override
	@Nonnull
	public Iterator<TripStructureUtils.Trip> iterator() {
		return new TripIterator();
	}

	private final class TripIterator implements Iterator<TripStructureUtils.Trip> {

		int cursor;       // index of next element to return

		@Override
		public boolean hasNext() {
			return cursor != trips.length;
		}

		@Override
		public TripStructureUtils.Trip next() {
			return getTrip(cursor++);
		}
	}

}
