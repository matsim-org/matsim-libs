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
	public void onReserve(double time, RailResource resource, TrainPosition position) {

		boolean resourceFound = false;

		int idx = Math.max(0, position.getRouteIndex() - 1);
		for (int i = idx; i < position.getRouteSize(); i++) {

			RailLink link = position.getRoute(i);
			RailResource r = link.getResource();
//			// Iterate through route until requested resource is present

			if (r == resource) {
				resourceFound = true;
			}

			if (!resourceFound)
				continue;

			if (isConflictPoint(r)) {

				MobsimDriverAgent other = conflictPoints.get(r);

				// Reserve non occupied conflict points
				// If reserved by other, stop reserving more
				if (other != null && other != position.getDriver())
					break;

				conflictPoints.put(r, position.getDriver());

			} else
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
