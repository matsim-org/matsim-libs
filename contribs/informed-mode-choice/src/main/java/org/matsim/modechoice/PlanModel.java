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
import java.util.stream.IntStream;

/**
 * A coarse model of the daily plan containing the trips and legs for using each mode.
 */
public final class PlanModel implements Iterable<TripStructureUtils.Trip>, HasPerson {

	private final TripStructureUtils.Trip[] trips;

	private final Person person;

	/**
	 * Used modes of the input plan.
	 */
	private final String[] currentModes;

	/**
	 * Stored trip start times. Depends on the current modes.
	 * This will only be re-estimated during routing.
	 */
	private final double[] startTimes;

	/**
	 * Routed trips for each mode and all legs.
	 */
	private final Map<String, List<Leg>[]> legs;

	/**
	 * Estimates for all modes and all available {@link ModeOptions}.
	 */
	private final Map<String, List<ModeEstimate>> estimates;

	/**
	 * Create a new plan model instance from an existing plan.
	 */
	public static PlanModel newInstance(Plan plan) {
		return new PlanModel(plan);
	}

	private PlanModel(Plan plan) {
		this.person = plan.getPerson();

		List<TripStructureUtils.Trip> tripList = TripStructureUtils.getTrips(plan);

		this.trips = tripList.toArray(new TripStructureUtils.Trip[0]);
		this.legs = new HashMap<>();
		this.estimates = new HashMap<>();
		this.currentModes = new String[trips.length];
		this.startTimes = new double[trips.length];

		setCurrentModes(tripList);
	}

	@Override
	public Person getPerson() {
		return person;
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
	 * The current used modes of the original plan. Returns a copy, so it should be used sparingly.
	 *
	 * @see #getCurrentModesMutable()
	 */
	public String[] getCurrentModes() {
		return Arrays.copyOf(currentModes, currentModes.length);
	}

	/**
	 * Mutable view on the current used modes.
	 */
	public String[] getCurrentModesMutable() {
		return currentModes;
	}

	/**
	 * Return view on cached trip start times.
	 */
	public double[] getStartTimes() {
		return startTimes;
	}

	/**
	 * Update current plan an underlying modes.
	 */
	public void setPlan(Plan plan) {
		List<TripStructureUtils.Trip> newTrips = TripStructureUtils.getTrips(plan);

		if (newTrips.size() != this.trips.length)
			throw new IllegalArgumentException("New length of trip must be equal to previous trips.");

		for (int i = 0; i < trips.length; i++) {
			trips[i] = newTrips.get(i);
		}

		setCurrentModes(newTrips);
	}

	/**
	 * Update the current modes from an updated plan.
	 */
	private void setCurrentModes(List<TripStructureUtils.Trip> trips) {

		if (trips.size() != this.trips.length)
			throw new IllegalArgumentException("The given plan has a different number of trips");


		for (int i = 0; i < trips.size(); i++) {
			String routingMode = TripStructureUtils.getRoutingMode(trips.get(i).getLegsOnly().get(0));

			if (routingMode == null)
				routingMode = trips.get(i).getLegsOnly().get(0).getMode();

			currentModes[i] = routingMode != null ? routingMode.intern() : null;
		}
	}

	/**
	 * Infer plan type from existing plan.
	 */
	public static String guessPlanType(Plan plan, Collection<String> modes) {

		List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan);

		StringBuilder b = new StringBuilder();

		for (int i = 0; i < trips.size(); i++) {
			String routingMode = TripStructureUtils.getRoutingMode(trips.get(i).getLegsOnly().get(0));

			if (routingMode == null)
				routingMode = trips.get(i).getLegsOnly().get(0).getMode();

			if (!modes.contains(routingMode))
				routingMode = null;

			b.append(routingMode);
			if (i != trips.size() - 1)
				b.append("-");
		}

		return b.toString();
	}

	/**
	 * Get the mode of the ith trip.
	 */
	public String getTripMode(int i) {
		return currentModes[i];
	}

	/**
	 * Estimated bee line distance.
	 */
	public double distance() {

		double dist = 0;
		for (TripStructureUtils.Trip trip : trips) {
			if (trip.getOriginActivity().getCoord() == null || trip.getDestinationActivity().getCoord() == null)
				continue;

			dist += CoordUtils.calcEuclideanDistance(trip.getOriginActivity().getCoord(), trip.getDestinationActivity().getCoord());
		}

		return dist;
	}

	/**
	 * Estimated beeline distance of one trip. If no estimate is possible this will return 0.
	 */
	public double distance(int idx) {
		TripStructureUtils.Trip trip = trips[idx];
		if (trip.getOriginActivity().getCoord() == null || trip.getDestinationActivity().getCoord() == null)
			return 0;

		return CoordUtils.calcEuclideanDistance(trip.getOriginActivity().getCoord(), trip.getDestinationActivity().getCoord());
	}

	/**
	 * Get ith trip of the day.
	 */
	public TripStructureUtils.Trip getTrip(int i) {
		return trips[i];
	}

	/**
	 * Get all trips of the day.
	 */
	public List<TripStructureUtils.Trip> getTrips() {
		return Arrays.asList(trips);
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

	/**
	 * Iterate over estimates and collect modes that match the predicate.
	 */
	public Set<String> filterModes(Predicate<? super ModeEstimate> predicate) {
		Set<String> modes = new HashSet<>();
		for (Map.Entry<String, List<ModeEstimate>> e : estimates.entrySet()) {
			if (e.getValue().stream().anyMatch(predicate))
				modes.add(e.getKey());
		}

		return modes;
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

	@SuppressWarnings("unchecked")
	List<Leg>[] getLegs(String mode, List<Leg> def) {
		return this.legs.computeIfAbsent(mode.intern(), k -> IntStream.range(0, trips.length).mapToObj(i -> def).toArray(List[]::new));
	}

	/**
	 * Check whether a mode is available for a trip.
	 * If for instance not pt option is found in the legs this will return false.
	 */
	public boolean hasModeForTrip(String mode, int i) {

		List<Leg>[] legs = this.legs.get(mode);
		if (legs == null)
			return false;

		List<Leg> ll = legs[i];
		return ll.stream().anyMatch(l -> l.getMode().equals(mode));
	}

	/**
	 * Delete stored routes and estimates.
	 */
	public void reset() {
		legs.clear();
		estimates.clear();
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
