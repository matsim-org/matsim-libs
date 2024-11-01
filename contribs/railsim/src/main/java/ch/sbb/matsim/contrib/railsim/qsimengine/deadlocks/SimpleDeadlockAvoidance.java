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
	private final Map<RailResource, Reservation> conflictPoints = new HashMap<>();

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

			// all resources before requested resource can be ignored
			if (!resourceFound)
				continue;

			// check for all conflict points at and beyond the resource
			if (isConflictPoint(r)) {

				Reservation reservation = conflictPoints.computeIfAbsent(r, k -> new Reservation());

				// Reserve non conflict points, otherwise stop
				if (reservation.direction != null && reservation.direction != link)
					break;

				reservation.direction = link;
				reservation.trains.add(position.getDriver());

			} else
				// as soon as we find an avoidance point we can stop reserving
				break;

		}
	}

	@Override
	public boolean checkLink(double time, RailLink link, TrainPosition position) {

		// Passing points can be ignored
		if (!isConflictPoint(link.getResource()))
			return true;

		Reservation other = conflictPoints.get(link.getResource());

		// not reserved or reserved by same direction
		return other == null || other.direction == null || other.direction == link;
	}

	@Override
	public boolean checkReroute(double time, RailLink start, RailLink end, List<RailLink> subRoute, List<RailLink> detour, TrainPosition position) {

		// rerouting is always allowed, but reservations needs to be removed
		for (RailLink link : subRoute) {
			onRelease(time, link.getResource(), position.getDriver());
		}

		return true;
	}

	@Override
	public void onRelease(double time, RailResource resource, MobsimDriverAgent driver) {

		Reservation reservation = conflictPoints.get(resource);
		if (reservation != null) {
			reservation.trains.remove(driver);

			// this direction is free again
			if (reservation.trains.isEmpty())
				reservation.direction = null;
		}
	}

	/**
	 * Holds current direction and drivers holding a reservation.
	 */
	private static final class Reservation {

		private RailLink direction;
		private final Set<MobsimDriverAgent> trains = new LinkedHashSet<>();

	}
}
