package ch.sbb.matsim.contrib.railsim.qsimengine.resources;

import ch.sbb.matsim.contrib.railsim.RailsimUtils;
import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import ch.sbb.matsim.contrib.railsim.qsimengine.deadlocks.DeadlockAvoidance;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import java.util.Collection;
import java.util.List;

public interface RailResourceManager {
	/**
	 * Constant that can be used as track number to indicate that any track is allowed.
	 */
	int ANY_TRACK = -1;
	/**
	 * Constant to indicate that any track is allowed as long as the opposing direction is not blocked.
	 */
	int ANY_TRACK_NON_BLOCKING = -2;

	/**
	 * Retrieve source id of a link.
	 */
	static Id<RailResource> getResourceId(Link link) {
		String id = RailsimUtils.getResourceId(link);
		if (id == null)
			return Id.create(link.getId().toString(), RailResource.class);
		else
			return Id.create(id, RailResource.class);
	}

	/**
	 * All available resources.
	 */
	Collection<RailResource> getResources();

	/**
	 * Get single link that belongs to an id.
	 */
	RailLink getLink(Id<Link> id);

	/**
	 * Try to block a track and the underlying resource and return the allowed distance.
	 */
	double tryBlockLink(double time, RailLink link, int track, TrainPosition position);

	/**
	 * Checks whether a link or underlying resource has remaining capacity.
	 */
	boolean hasCapacity(double time, Id<Link> link, int track, TrainPosition position);

	/**
	 * Set the capacity of a link or underlying resource.
	 */
	void setCapacity(Id<Link> link, int newCapacity);

	/**
	 * Whether a driver already reserved a link.
	 */
	boolean isBlockedBy(RailLink link, TrainPosition position);

	/**
	 * Release a non-free track to be free again.
	 */
	void releaseLink(double time, RailLink link, MobsimDriverAgent driver);

	/**
	 * Check if a re-route is allowed.
	 *
	 * @see DeadlockAvoidance#checkReroute(double, RailLink, RailLink, List, List, TrainPosition)
	 */
	boolean checkReroute(double time, RailLink start, RailLink end, List<RailLink> subRoute, List<RailLink> detour, TrainPosition position);
}
