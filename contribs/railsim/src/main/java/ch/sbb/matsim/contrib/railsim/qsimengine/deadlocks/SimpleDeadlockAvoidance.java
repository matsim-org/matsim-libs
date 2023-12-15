package ch.sbb.matsim.contrib.railsim.qsimengine.deadlocks;

import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResource;
import jakarta.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import javax.annotation.Nullable;
import java.util.*;

/**
 * A simple deadlock avoidance strategy that backtracks conflicting points in the schedule of trains.
 * This implementation does not guarantee deadlock freedom, but can avoid many cases.
 */
public class SimpleDeadlockAvoidance implements DeadlockAvoidance {

	/**
	 * Stores conflict points of trains.
	 */
	private final Map<RailResource, MobsimDriverAgent> conflictPoints = new HashMap<>();

	@Inject
	public SimpleDeadlockAvoidance() {
	}

	/**
	 * This strategy only safeguards sections with single capacity.
	 */
	private static boolean isConflictPoint(RailResource resource) {
		return resource.getTotalCapacity() == 1;
	}

	@Override
	public void initResources(Map<Id<RailResource>, RailResource> resources) {
	}

	@Override
	public void onReserve(double time, RailResource resource, TrainPosition position) {

		boolean resourceFound = false;

		for (int i = position.getRouteIndex(); i < position.getRouteSize(); i++) {

			RailLink link = position.getRoute(i);

//			// Iterate through route until requested resource is present
//			if (link.getResource() == resource) {
//				resourceFound = true;
//			}
//
//			if (!resourceFound)
//				continue;

			// After any non-conflict point, no route needs to be stored
			if (isConflictPoint(link.getResource()))
				conflictPoints.put(link.getResource(), position.getDriver());
			else
				break;
		}
	}

	@Override
	public boolean checkLink(double time, RailLink link, TrainPosition position) {

		// Passing points can be ignored
		if (!isConflictPoint(link.getResource()))
			return true;

		MobsimDriverAgent other = conflictPoints.get(link.getResource());

		// Not reserved, or reserved by same driver
        return other == null || other == position.getDriver();
    }

	@Override
	public void onRelease(double time, RailResource resource, MobsimDriverAgent driver) {

		if (conflictPoints.get(resource) == driver)
			conflictPoints.remove(resource);

//		MobsimDriverAgent removed = conflictPoints.remove(resource);
//		assert removed == driver : "Released resource was not reserved by driver.";
	}
}
