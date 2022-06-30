package org.matsim.modechoice;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A coarse model of the daily plan containing the trips and legs for using each mode.
 */
public final class PlanModel implements Iterable<TripStructureUtils.Trip> {

	private final TripStructureUtils.Trip[] trips;

	/**
	 * Routed legs.
	 */
	private final Map<String, List<Leg>[]> legs;

	public PlanModel(Plan plan) {
		this.trips = TripStructureUtils.getTrips(plan).toArray(new TripStructureUtils.Trip[0]);
		this.legs = new IdentityHashMap<>();
	}

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
