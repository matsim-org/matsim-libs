package ch.sbb.matsim.contrib.railsim.qsimengine.resources;

import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

/**
 * Internal rail resource interface, which allows modifying the state.
 * Disposition should only interact with resources via {@link RailResourceManagerImpl}.
 */
interface RailResourceInternal extends RailResource {

	/**
	 * Modify the capacity of this resource. This is an internal method and should only be used by simulation engine, in case the capacity is changed externally.
	 */
	void setCapacity(int capacity);

	/**
	 * Whether an agent is able to block this resource.
	 */
	boolean hasCapacity(double time, RailLink link, int track, TrainPosition position);

	/**
	 * The reserved distance on this link for an agent. Returns 0 if the agent has no reservation.
	 *
	 * @return the reserved distance, -1 if there is no reservation. A reservation with 0 dist could be possible.
	 */
	double getReservedDist(RailLink link, TrainPosition position);

	/**
	 * Reserves this resource for the given agent.
	 *
	 * @see #reserve(double, RailLink, int, TrainPosition, boolean)
	 */
	default double reserve(double time, RailLink link, int track, TrainPosition position) {
		return reserve(time, link, track, position, false);
	}

	/**
	 * Reserves this resource for the given agent.
	 *
	 * @param force force reservation even when no capacity is available.
	 * @return the reserved distance on this link
	 */
	double reserve(double time, RailLink link, int track, TrainPosition position, boolean force);

	/**
	 * Releases the link on this resource for the given agent.
	 *
	 * @return if the resource was released, i.e. no more links are occupied.
	 */
	boolean release(RailLink link, MobsimDriverAgent driver);

}
