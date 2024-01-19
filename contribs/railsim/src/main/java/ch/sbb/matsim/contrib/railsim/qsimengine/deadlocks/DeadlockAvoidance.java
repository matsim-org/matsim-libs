package ch.sbb.matsim.contrib.railsim.qsimengine.deadlocks;

import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResource;
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import java.util.List;
import java.util.Map;

/**
 * Interface for deadlock prevention strategies.
 * Strategies should aim to prevent deadlocks, but is not required to guarantee it.
 */
public interface DeadlockAvoidance {

	/**
	 * Let the strategy known about the resources that are available.
	 */
	default void initResources(Map<Id<RailResource>, RailResource> resources) {
	}


	/**
	 * Check if reserving this link may produce a deadlock.
	 * @return true if the link can be reserved, false otherwise.
	 */
	boolean checkLink(double time, RailLink link, TrainPosition position);

	/**
	 * Check if performing this re-route may produce a deadlock.
	 * @param subRoute the original route to be changed
	 * @param detour new detour route
	 * @return true if the rerouting can be performed, false otherwise.
	 */
	boolean checkReroute(double time, RailLink start, RailLink end, List<RailLink> subRoute, List<RailLink> detour, TrainPosition position);

	/**
	 * Called when a resource was reserved.
	 */
	void onReserve(double time, RailResource resource, TrainPosition position);

	/**
	 * Called when a resource was released.
	 */
	void onRelease(double time, RailResource resource, MobsimDriverAgent driver);

	/**
	 * Called when a link was released.
	 */
	default void onReleaseLink(double time, RailLink link, MobsimDriverAgent driver) {
	}

}
