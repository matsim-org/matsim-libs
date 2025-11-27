package ch.sbb.matsim.contrib.railsim.qsimengine.deadlocks;

import ch.sbb.matsim.contrib.railsim.events.RailsimLinkStateChangeEvent;
import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResource;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResourceManager;
import ch.sbb.matsim.contrib.railsim.qsimengine.router.TrainRouter;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A simple deadlock avoidance strategy that backtracks conflicting points in the schedule of trains.
 * This implementation does not guarantee deadlock freedom, but can avoid many cases.
 */
public class SimpleDeadlockAvoidance implements DeadlockAvoidance {

	/**
	 * Stores conflict points of trains.
	 */
	private final Map<RailResource, Reservation> conflictPoints = new HashMap<>();
	private final Network network;
	private final EventsManager eventsManager;

	/**
	 * Stores conflict-free entry and exit links for each resource.
	 */
	private final Map<Id<RailResource>, ConflictFreeLinks> conflictFreeLinks = new HashMap<>();

	@VisibleForTesting
	public SimpleDeadlockAvoidance(Network network) {
		this(network, null);
	}

	@Inject
	public SimpleDeadlockAvoidance(Network network, EventsManager eventsManager) {
		this.network = network;
		this.eventsManager = eventsManager;
	}

	/**
	 * This strategy only safeguards sections with single capacity.
	 */
	private static boolean isConflictPoint(RailResource resource) {
		return resource.getTotalCapacity() == 1;
	}

	@Override
	public void initResources(RailResourceManager rrm) {
		conflictFreeLinks.clear();
		conflictFreeLinks.putAll(computeConflictFreeLinks(network, rrm));
	}


	/**
	 * Compute all conflict-free entry and exit links for each resource.
	 */
	static Map<Id<RailResource>, ConflictFreeLinks> computeConflictFreeLinks(Network network, RailResourceManager rrm) {

		TrainRouter router = new TrainRouter(network, rrm);

		Map<Id<RailResource>, ConflictFreeLinks> result = new LinkedHashMap<>();

		for (RailResource r : rrm.getResources()) {

			if (r.getLinks().size() == 1)
				continue;

			Set<Link> links = r.getLinks().stream().map(l -> network.getLinks().get(l.getLinkId())).collect(Collectors.toSet());

			Set<Link> firstLinks = new HashSet<>();
			Set<Link> lastLinks = new HashSet<>();

			// Collect all possible entry and exit links of a resource
			for (RailLink link : r.getLinks()) {
				Link l = network.getLinks().get(link.getLinkId());
				if (l.getFromNode().getInLinks().values().stream().anyMatch(o -> !links.contains(o)))
					firstLinks.add(l);

				if (l.getToNode().getOutLinks().values().stream().anyMatch(o -> !links.contains(o)))
					lastLinks.add(l);
			}

			// Two opposite links are never conflict free
			if (firstLinks.size() == 1 && lastLinks.size() == 1)
				continue;

			Map<LinkTuple, List<RailLink>> routes = new LinkedHashMap<>();

			// Route all combinations
			for (Link first : firstLinks) {
				for (Link last : lastLinks) {
					List<RailLink> route = router.calcRoute(rrm.getLink(first.getId()), rrm.getLink(last.getId()));
					routes.put(new LinkTuple(rrm.getLink(first.getId()), rrm.getLink(last.getId())), route);
				}
			}

			ConflictFreeLinks conflictFree = new ConflictFreeLinks(new LinkedHashMap<>());

			for (Map.Entry<LinkTuple, List<RailLink>> e : routes.entrySet()) {
				for (Map.Entry<LinkTuple, List<RailLink>> other : routes.entrySet()) {
					// Ignore self
					if (e.getKey().equals(other.getKey()))
						continue;

					// Last and first link are conflicting
					if (e.getKey().last.isOppositeLink(other.getKey().first.getLinkId()))
						continue;

					// Any link on the route is conflicting with some link on the other route
					if (e.getValue().stream().anyMatch(
						l -> other.getValue().stream().anyMatch(o -> l.isOppositeLink(o.getLinkId())))
					)
						continue;


					// Otherwise this entry and exit combination is conflict free
					conflictFree.links.computeIfAbsent(e.getKey(), k -> new LinkedHashSet<>()).add(other.getKey());
				}
			}

			if (!conflictFree.links.isEmpty())
				result.put(r.getId(), conflictFree);
		}

		return result;
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

				boolean wasNotReserved = !conflictPoints.containsKey(r);
				Reservation reservation = conflictPoints.computeIfAbsent(r, k -> new Reservation(r.getId()));

				if (reservation.entry == null)
					wasNotReserved = true;

				RailLink lastLink = findLastUsedLink(position, r, i);

				// Reserve non conflict points, otherwise stop
				if (!allowReservation(reservation, link, lastLink))
					break;

				reservation.entry = new LinkTuple(link, lastLink);
				reservation.trains.add(position.getDriver());

				// Only throw events when the resource was not currently reserved
				if (eventsManager != null && wasNotReserved)
					for (RailLink l : r.getLinks()) {
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

		if (r.entry == null)
			return true;

		if (r.entry.first == firstLink)
			return true;

		ConflictFreeLinks links = conflictFreeLinks.get(r.id);

		if (links == null)
			return false;

		Set<LinkTuple> conflictFree = links.links.get(r.entry);

		// Check if the approaching entry is conflict free with the reservation
		return conflictFree != null && conflictFree.contains(new LinkTuple(firstLink, lastLink));
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

		return r != null && r.entry != null;
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
				reservation.entry = null;
			}
		}
	}

	/**
	 * Holds the first and last link used on the resource and all trains holding this reservation.
	 */
	private static final class Reservation {

		private final Id<RailResource> id;
		private final Set<MobsimDriverAgent> trains = new LinkedHashSet<>();

		private LinkTuple entry;

		private Reservation(Id<RailResource> id) {
			this.id = id;
		}

		@Override
		public String toString() {
			return "Reservation{" +
				"entry=" + entry +
				", trains=" + trains +
				'}';
		}
	}

	record LinkTuple(RailLink first, RailLink last) {
	}

	record ConflictFreeLinks(Map<LinkTuple, Set<LinkTuple>> links) {
	}
}
