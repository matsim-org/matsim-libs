package ch.sbb.matsim.contrib.railsim.qsimengine.deadlocks;

import ch.sbb.matsim.contrib.railsim.events.RailsimLinkStateChangeEvent;
import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResource;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

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
	private final EventsManager eventsManager;

	@VisibleForTesting
	public SimpleDeadlockAvoidance() {
		this(null);
	}

	@Inject
	public SimpleDeadlockAvoidance(EventsManager eventsManager) {
		this.eventsManager = eventsManager;
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

				RailLink lastLink = findLastUsedLink(position, r, i);

				// Reserve non conflict points, otherwise stop
				if (!allowReservation(reservation, link, lastLink))
					break;

				reservation.firstLink = link;
				reservation.lastLink = lastLink;
				reservation.trains.add(position.getDriver());

				for (RailLink l : resource.getLinks()) {
					if (eventsManager != null)
						eventsManager.processEvent(
							new RailsimLinkStateChangeEvent(Math.ceil(time), l.getLinkId(), position.getDriver().getPlannedVehicleId(),
								resource.getState(l), true)
						);
				}

			} else
				// as soon as we find an avoidance point we can stop reserving
				break;

		}
	}

	/**
	 * Find the last used link on the resource.
	 */
	private RailLink findLastUsedLink(TrainPosition position, RailResource resource, int idx) {

		// Find the index of the resource
		if (idx == -1) {
			idx = Math.max(0, position.getRouteIndex() - 1);
			for (; idx < position.getRouteSize(); idx++) {
				if (position.getRoute(idx).getResource() == resource) {
					break;
				}
			}
		}

		RailLink last = position.getRoute(idx);

		for (int i = idx; i < position.getRouteSize(); i++) {
			RailLink link = position.getRoute(i);

			if (link.getResource() == resource) {
				last = link;
			} else
				break;
		}

		return last;
	}

	/**
	 * Check if the reservation is allowed considering existing ones.
	 */
	private boolean allowReservation(Reservation r, RailLink firstLink, RailLink lastLink) {

		if (r.firstLink == null && r.lastLink == null)
			return true;


		// TODO: simplication for now, need to preprocess information of allowed first and last link combinations
		return r.firstLink == firstLink || r.lastLink == lastLink;
	}

	@Override
	public boolean checkLink(double time, RailLink link, TrainPosition position) {

		// Passing points can be ignored
		if (!isConflictPoint(link.getResource()))
			return true;

		Reservation other = conflictPoints.get(link.getResource());

		RailLink last = findLastUsedLink(position, link.getResource(), -1);

		if (other == null) return true;

		boolean allowReservation = allowReservation(other, link, last);
		return allowReservation || other.trains.contains(position.getDriver());
	}

	@Override
	public boolean isReserved(RailResource resource) {
		if (resource == null || !isConflictPoint(resource))
			return false;

		Reservation r = conflictPoints.get(resource);

		// first and last link should always be both null or both not null
		return r != null && (r.firstLink != null || r.lastLink != null);
	}

	@Override
	public boolean checkLinks(double time, List<RailLink> links, TrainPosition position) {
		// This strategy cannot handle cases where one train reserves a segment of links efficiently.
		// The non-blocking areas should already be designed such that trains don't block each other.
		// Checking each link individually is possible but too restrictive,
		// Instead it checks only the link used for entering the segment.
		return checkLink(time, links.getLast(), position);
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
			if (reservation.trains.isEmpty()) {
				reservation.firstLink = null;
				reservation.lastLink = null;
			}
		}
	}

	/**
	 * Holds the first and last link used on the resource and all trains holding this reservation.
	 */
	private static final class Reservation {

		private final Set<MobsimDriverAgent> trains = new LinkedHashSet<>();
		private RailLink firstLink;
		private RailLink lastLink;

		@Override
		public String toString() {
			return "Reservation{" +
				"firstLink=" + firstLink +
				", lastLink=" + lastLink +
				", trains=" + trains +
				'}';
		}
	}
}
