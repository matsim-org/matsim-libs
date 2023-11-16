package ch.sbb.matsim.contrib.railsim.qsimengine.resources;

import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

/**
 * Internal rail resource interface, which allows modifying the state.
 * Disposition should only interact with resources via {@link RailResourceManager}.
 */
interface RailResourceInternal extends RailResource {

	/**
	 * Whether an agent is able to block this resource.
	 */
	boolean hasCapacity(RailLink link, TrainPosition position);

	/**
	 * Whether an agent is able to block this resource.
	 */
	boolean isReservedBy(RailLink link, MobsimDriverAgent driver);

	/**
	 * Reserves this resource for the given agent.
	 */
	int reserve(RailLink link, TrainPosition position);

	/**
	 * Releases this resource for the given agent.
	 */
	int release(RailLink link, MobsimDriverAgent driver);

}
