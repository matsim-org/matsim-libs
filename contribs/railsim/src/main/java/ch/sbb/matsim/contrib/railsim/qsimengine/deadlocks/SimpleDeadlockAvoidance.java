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
	private Int2ObjectMap<IntList> paths = new Int2ObjectOpenHashMap<>();

	/**
	 * Maps the index of a conflict point, to list of conflicting paths.
	 */
	private Int2ObjectMap<IntSet> conflictPoints = new Int2ObjectOpenHashMap<>();

	@Inject
	public SimpleDeadlockAvoidance() {
	}

	@Override
	public void initResources(Map<Id<RailResource>, RailResource> resources) {

		// Init relevant conflict points
		for (RailResource r : resources.values()) {
			if (r.getTotalCapacity() == 1)
				conflictPoints.put(r.getId().index(), new IntOpenHashSet());
		}
	}

	@Override
	public void onReserve(double time, RailResource resource, TrainPosition position) {

		// stores the path
		int driverIdx = position.getDriver().getId().index();

		if (!paths.containsKey(driverIdx))
			return;

		IntList path = paths.computeIfAbsent(driverIdx, (k) -> new IntArrayList());
		for (RailLink link : position.getRouteUntilNextStop()) {
			path.add(link.getLinkId().index());

			if (link.getResource().getTotalCapacity() == 1)
				conflictPoints.get(link.getResource().getId().index()).add(driverIdx);

		}
	}

	@Nullable
	@Override
	public RailLink check(double time, List<RailLink> segment, TrainPosition position) {

		int driverId = position.getDriver().getId().index();

		for (RailLink link : position.getRouteUntilNextStop()) {

			// Passing points can be ignored
			if (link.getResource().getTotalCapacity() > 1)
				continue;

			int conflictIdx = link.getResource().getId().index();
			IntSet trains = conflictPoints.get(conflictIdx);

			// check all other trains with paths on this resource
			for (int driver : trains) {

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

		// reset conflicting route
		conflictPoints.put(resource.getId().index(), null);

	}

}
