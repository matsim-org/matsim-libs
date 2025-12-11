package ch.sbb.matsim.contrib.railsim.qsimengine.disposition;

import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;

import java.util.List;

/**
 * Planned arrival at a rail link.
 * @param time time of arrival in seconds
 * @param route the route to be followed, including the arrival link
 */
public record PlannedArrival(double time, List<RailLink> route) {
	/**
	 * Undefined planned arrival, used to indicate that no arrival is planned.
	 */
	public static PlannedArrival UNDEFINED = new PlannedArrival(0, null);

}
