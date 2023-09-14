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
	 * Links belonging to this resource.
	 */
	final List<RailLink> links;

	/**
	 * Agents holding this resource exclusively.
	 */
	final Set<MobsimDriverAgent> reservations;

	/**
	 * Maximum number of reservations.
	 */
	int capacity;

	public RailResource(List<RailLink> links) {
		this.links = links;
		this.reservations = new HashSet<>();
		this.capacity = links.stream().mapToInt(RailLink::getNumberOfTracks).min().orElseThrow();
	}

	/**
	 * Whether an agent is able to block this resource.
	 */
	boolean hasCapacity() {
		return reservations.size() < capacity;
	}

}
