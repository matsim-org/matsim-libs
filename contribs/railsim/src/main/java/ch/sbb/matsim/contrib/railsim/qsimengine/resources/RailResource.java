package ch.sbb.matsim.contrib.railsim.qsimengine.resources;

import ch.sbb.matsim.contrib.railsim.qsimengine.RailLink;
import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import java.util.List;

/**
 * A resource representing multiple {@link RailLink}.
 */
public interface RailResource {

	/**
	 * The links that are represented by this resource.
	 */
	List<RailLink> getLinks();

	/**
	 * Whether an agent is able to block this resource.
	 */
	boolean hasCapacity(double time, TrainPosition position);

	/**
	 * Whether an agent is able to block this resource.
	 */
	boolean isReservedBy(MobsimDriverAgent driver);

	/**
	 * Reserves this resource for the given agent.
	 */
	void reserve(TrainPosition position);

	/**
	 * Releases this resource for the given agent.
	 */
	void release(MobsimDriverAgent driver);

}
