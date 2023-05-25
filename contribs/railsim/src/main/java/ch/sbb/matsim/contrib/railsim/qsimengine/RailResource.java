package ch.sbb.matsim.contrib.railsim.qsimengine;

import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * A resource representing multiple {@link RailLink}.
 */
public class RailResource {

	/**
	 * Links beloning to this resource.
	 */
	final List<RailLink> links;

	/**
	 * Maximum number of reservations.
	 */
	int capacity;

	/**
	 * Agents holding this resource exclusively.
	 */
	Set<MobsimDriverAgent> reservations;

	public RailResource(List<RailLink> links) {
		capacity = links.stream().mapToInt(RailLink::getNumberOfTracks).min().orElseThrow();
		this.links = links;
		reservations = new HashSet<>();
	}

	/**
	 * Whether an agent is able to block this resource.
	 */
	boolean hasCapacity() {
		return reservations.size() < capacity;
	}

}
