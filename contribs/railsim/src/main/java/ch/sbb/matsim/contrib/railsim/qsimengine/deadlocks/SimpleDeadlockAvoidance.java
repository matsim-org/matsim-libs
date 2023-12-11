package ch.sbb.matsim.contrib.railsim.qsimengine.deadlocks;

import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResource;
import it.unimi.dsi.fastutil.ints.*;
import jakarta.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * A simple deadlock avoidance strategy that backtracks conflicting points in the schedule of trains.
 * This implementation does not guarantee deadlock freedom, but can avoid many cases.
 */
public class SimpleDeadlockAvoidance implements DeadlockAvoidance {

	/**
	 * Maps driver to path of resource indices.
	 */
	private final Int2ObjectMap<IntList> paths = new Int2ObjectOpenHashMap<>();

	/**
	 * Maps the index of a conflict point, to list of conflicting paths.
	 */
	private final Int2ObjectMap<IntSortedSet> conflictPoints = new Int2ObjectOpenHashMap<>();

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

		// Init relevant conflict points
		for (RailResource r : resources.values()) {
			if (isConflictPoint(r))
				conflictPoints.put(r.getId().index(), new IntAVLTreeSet());
		}
	}

	@Override
	public void onReserve(double time, RailResource resource, TrainPosition position) {

		int driverIdx = position.getDriver().getId().index();

		// stores the path, but only once
		if (paths.containsKey(driverIdx))
			return;

		IntList path = paths.computeIfAbsent(driverIdx, (k) -> new IntArrayList());
		for (RailLink link : position.getRouteUntilNextStop()) {
			path.add(link.getResource().getId().index());

			if (isConflictPoint(link.getResource()))
				conflictPoints.get(link.getResource().getId().index()).add(driverIdx);

		}
	}

	@Nullable
	@Override
	public RailLink checkSegment(double time, List<RailLink> segment, TrainPosition position) {

		int driverId = position.getDriver().getId().index();

		for (RailLink link : position.getRouteUntilNextStop()) {

			// Passing points can be ignored
			if (!isConflictPoint(link.getResource()))
				continue;

			int conflictIdx = link.getResource().getId().index();
			IntSortedSet trains = conflictPoints.get(conflictIdx);

			// First reserving train is allowed to drive through the conflict points
			if (trains.firstInt() == driverId)
				continue;

			// check all other trains with paths on this resource
			for (int driver : trains) {

				// own path can be ignored
				if (driver == driverId)
					continue;

				IntList otherPath = paths.get(driver);

				for (int r : otherPath) {

					if (r == conflictIdx)
						return link;

					// if there is a non-conflicting point before the conflict, train can continue
					if (!conflictPoints.containsKey(r))
						break;
				}
			}
		}

		return null;
	}

	@Override
	public void onRelease(double time, RailResource resource, MobsimDriverAgent driver) {

		int driverIdx = driver.getId().index();

		IntList path = paths.get(driverIdx);

		int rId = resource.getId().index();

		if (path.getInt(0) == rId)
			path.removeInt(0);

		// Remove finished path
		if (path.isEmpty())
			paths.remove(driverIdx);

		// Remove conflicting route
		if (isConflictPoint(resource))
			conflictPoints.get(rId).remove(driverIdx);

	}

}
