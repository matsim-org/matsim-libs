package org.matsim.modechoice;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.modechoice.estimators.PlanBasedLegEstimator;

import java.util.List;

/**
 * A rough model of the daily plan containing trip departure times and distances.
 * This information can be useful for rough estimate using {@link PlanBasedLegEstimator}.
 */
public final class PlanModel {

	private final TripStructureUtils.Trip[] trips;

	private final String[] modes;
	public final List<Leg>[] legs;

	public PlanModel(List<TripStructureUtils.Trip> trips) {
		this.trips = trips.toArray(new TripStructureUtils.Trip[0]);
		this.modes = new String[trips.size()];
		this.legs = new List[trips.size()];
	}

	public int size() {
		return trips.length;
	}

	/**
	 * Get ith trip of the day.
	 */
	public TripStructureUtils.Trip getTrip(int i) {
		return trips[i];
	}

	/**
	 * Get ith trip of the day.
	 */
	public String getMode(int i) {
		return modes[i];
	}

	/**
	 * Set the used modes.
	 */
	void setModes(List<String> modes) {
		modes.toArray(this.modes);
	}

	// TODO: estimated distances of these trips

	// can also contain the mode selection
	// list of all legs


	// TODO: custom iterator that allows accessing all modes

}
