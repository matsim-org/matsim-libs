/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2026 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.prebooking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.passenger.DrtStopCandidate;
import org.matsim.contrib.drt.passenger.DrtStopCandidates;
import org.matsim.contrib.dvrp.router.StopCandidates;
import org.matsim.contrib.dvrp.router.StopCandidatesRoute;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.TripStructureUtils;

import com.google.common.base.Preconditions;

/**
 * Connects the stop candidates routed into a {@link StopCandidatesRoute} with prebooking:
 * <ul>
 * <li>{@link #toDrtStopCandidates} turns them into the {@link DrtStopCandidates} of a request, so that the insertion
 * search may choose another pickup/dropoff stop than the routed one;</li>
 * <li>{@link #materialize} rewrites the access and egress part of the agent's trip to the chosen stops once the request
 * is scheduled, using the routed walk duration and distance of the chosen candidate.</li>
 * </ul>
 * Only the agent's copy of the plan is changed during the mobsim; the plan in the population keeps the routed
 * (preferred) stops, so the next iteration builds the same request again.
 *
 * @author nkuehnel / MOIA
 */
public final class PrebookingStopCandidates {
	private PrebookingStopCandidates() {
	}

	/**
	 * Single-person variant of {@link #toDrtStopCandidates(List, Network, boolean)}.
	 */
	public static Optional<DrtStopCandidates> toDrtStopCandidates(Route route, Network network,
			boolean includeAccess) {
		return toDrtStopCandidates(List.of(route), network, includeAccess);
	}

	/**
	 * Combines the stop candidates of the routes of all passengers of one request. A stop is a candidate if it is a
	 * candidate of every passenger; its walk duration is the longest one among the passengers, since the group boards
	 * together. The order is that of the first route.
	 *
	 * @param includeAccess whether alternative pickup stops may be offered (false if a passenger may already be on the
	 *                      way to the routed stop)
	 * @return the candidates for a request with the routed (preferred) stops as its own from/to link, or empty if there
	 * are no common alternatives or a route has no candidates or does not start/end at its preferred stops
	 */
	public static Optional<DrtStopCandidates> toDrtStopCandidates(List<Route> routes, Network network,
			boolean includeAccess) {
		Preconditions.checkArgument(!routes.isEmpty(), "at least one route is required");
		List<StopCandidates> all = new ArrayList<>();
		for (Route route : routes) {
			if (!(route instanceof StopCandidatesRoute candidatesRoute)) {
				return Optional.empty();
			}
			Optional<StopCandidates> candidates = candidatesRoute.getStopCandidates();
			if (candidates.isEmpty()
					|| !candidates.get().preferredAccess().linkId().equals(route.getStartLinkId())
					|| !candidates.get().preferredEgress().linkId().equals(route.getEndLinkId())) {
				return Optional.empty(); // no candidates, or the route was changed after routing
			}
			all.add(candidates.get());
		}
		Id<Link> preferredAccess = all.getFirst().preferredAccess().linkId();
		Id<Link> preferredEgress = all.getFirst().preferredEgress().linkId();
		if (all.stream().anyMatch(c -> !c.preferredAccess().linkId().equals(preferredAccess)
				|| !c.preferredEgress().linkId().equals(preferredEgress))) {
			return Optional.empty(); // the group does not share its routed stops
		}

		List<DrtStopCandidate> access = includeAccess ?
				alternatives(common(all, StopCandidates::access), network) :
				List.of();
		List<DrtStopCandidate> egress = alternatives(common(all, StopCandidates::egress), network);
		if (access.isEmpty() && egress.isEmpty()) {
			return Optional.empty();
		}
		double accessWalk = all.stream().mapToDouble(c -> c.preferredAccess().walkDuration()).max().orElseThrow();
		double egressWalk = all.stream().mapToDouble(c -> c.preferredEgress().walkDuration()).max().orElseThrow();
		return Optional.of(new DrtStopCandidates(accessWalk, egressWalk, access, egress));
	}

	/**
	 * @return the candidates (preferred first) that all passengers share, each with the longest walk duration
	 */
	private static List<StopCandidates.Candidate> common(List<StopCandidates> all,
			Function<StopCandidates, List<StopCandidates.Candidate>> side) {
		List<StopCandidates.Candidate> result = new ArrayList<>();
		for (StopCandidates.Candidate candidate : side.apply(all.getFirst())) {
			double walkDuration = candidate.walkDuration();
			boolean shared = true;
			for (StopCandidates other : all.subList(1, all.size())) {
				Optional<StopCandidates.Candidate> match = side.apply(other)
						.stream()
						.filter(c -> c.linkId().equals(candidate.linkId()))
						.findFirst();
				if (match.isEmpty()) {
					shared = false;
					break;
				}
				walkDuration = Math.max(walkDuration, match.get().walkDuration());
			}
			if (shared) {
				result.add(new StopCandidates.Candidate(candidate.linkId(), candidate.coord(), walkDuration,
						candidate.walkDistance()));
			}
		}
		return result;
	}

	private static List<DrtStopCandidate> alternatives(List<StopCandidates.Candidate> candidates, Network network) {
		return candidates.subList(1, candidates.size())
				.stream()
				.map(candidate -> new DrtStopCandidate(
						Preconditions.checkNotNull(network.getLinks().get(candidate.linkId()),
								"Stop candidate link %s not in network", candidate.linkId()),
						candidate.walkDuration()))
				.toList();
	}

	/**
	 * @return true if every route carries stop candidates with alternatives (cheap check before looking at the plans)
	 */
	public static boolean haveStopCandidates(List<Route> routes) {
		return !routes.isEmpty() && routes.stream()
				.allMatch(route -> route instanceof StopCandidatesRoute candidatesRoute
						&& candidatesRoute.getStopCandidates().map(StopCandidates::hasAlternatives).orElse(false));
	}

	/**
	 * @return true if the trip of the given leg has at least one leg before and after it (the access and egress walks),
	 * so that it can be moved to other stops
	 */
	public static boolean hasAccessAndEgressLegs(Plan plan, Leg drtLeg) {
		List<PlanElement> elements = plan.getPlanElements();
		int legIndex = indexOf(elements, drtLeg);
		int originIndex = legIndex - 1;
		while (originIndex >= 0 && !isMainActivity(elements.get(originIndex))) {
			originIndex--;
		}
		int destinationIndex = legIndex + 1;
		while (destinationIndex < elements.size() && !isMainActivity(elements.get(destinationIndex))) {
			destinationIndex++;
		}
		if (originIndex < 0 || destinationIndex >= elements.size()) {
			return false; // not a complete trip between two activities
		}
		return elements.subList(originIndex + 1, legIndex).stream().anyMatch(Leg.class::isInstance)
				&& elements.subList(legIndex + 1, destinationIndex).stream().anyMatch(Leg.class::isInstance);
	}

	/**
	 * @return the request with the given stop candidates (or the request itself if there are none)
	 */
	public static DrtRequest withStopCandidates(DrtRequest request, Optional<DrtStopCandidates> candidates) {
		return candidates.map(c -> DrtRequest.newBuilder(request).stopCandidates(c).build()).orElse(request);
	}

	/**
	 * Rewrites the access and/or egress part of the trip of the given DRT leg to the chosen pickup/dropoff stops. The
	 * new walk legs are teleported legs with the routed duration and distance of the chosen candidate.
	 *
	 * @return true if the plan was changed
	 */
	public static boolean materialize(Plan plan, Leg drtLeg, Id<Link> pickupLinkId, Id<Link> dropoffLinkId) {
		Preconditions.checkArgument(drtLeg.getRoute() instanceof StopCandidatesRoute,
				"Leg has no route with stop candidates");
		Route route = drtLeg.getRoute();
		StopCandidates candidates = ((StopCandidatesRoute)route).getStopCandidates().orElseThrow();
		String mode = drtLeg.getMode();
		String routingMode = TripStructureUtils.getRoutingMode(drtLeg);

		boolean changePickup = !pickupLinkId.equals(route.getStartLinkId());
		boolean changeDropoff = !dropoffLinkId.equals(route.getEndLinkId());
		if (!changePickup && !changeDropoff) {
			return false;
		}

		List<PlanElement> elements = plan.getPlanElements();
		int legIndex = indexOf(elements, drtLeg);
		int originIndex = legIndex - 1;
		while (!isMainActivity(elements.get(originIndex))) {
			originIndex--;
		}
		int destinationIndex = legIndex + 1;
		while (!isMainActivity(elements.get(destinationIndex))) {
			destinationIndex++;
		}

		if (changeDropoff) { // first the egress part, so that the indices of the access part stay valid
			StopCandidates.Candidate dropoff = find(candidates.egress(), dropoffLinkId);
			Activity destination = (Activity)elements.get(destinationIndex);
			Id<Link> destinationLinkId = lastWalkEndLink(elements, legIndex + 1, destinationIndex)
					.orElse(destination.getLinkId());
			List<PlanElement> egress = List.of(stageActivity(dropoff, mode),
					walkLeg(dropoff, dropoff.linkId(), destinationLinkId, routingMode));
			replace(elements, legIndex + 1, destinationIndex, egress);
			route.setEndLinkId(dropoffLinkId);
		}

		if (changePickup) {
			StopCandidates.Candidate pickup = find(candidates.access(), pickupLinkId);
			Activity origin = (Activity)elements.get(originIndex);
			Id<Link> originLinkId = firstWalkStartLink(elements, originIndex + 1, legIndex).orElse(origin.getLinkId());
			Leg walk = walkLeg(pickup, originLinkId, pickup.linkId(), routingMode);
			origin.getEndTime().ifDefined(walk::setDepartureTime);
			List<PlanElement> access = List.of(walk, stageActivity(pickup, mode));
			replace(elements, originIndex + 1, legIndex, access);
			route.setStartLinkId(pickupLinkId);
			if (walk.getDepartureTime().isDefined()) {
				drtLeg.setDepartureTime(walk.getDepartureTime().seconds() + pickup.walkDuration());
			}
		}
		return true;
	}

	/**
	 * @return the candidate of the given route for the given stop link
	 */
	public static StopCandidates.Candidate candidate(Route route, Id<Link> linkId, boolean access) {
		StopCandidates candidates = ((StopCandidatesRoute)route).getStopCandidates().orElseThrow();
		return find(access ? candidates.access() : candidates.egress(), linkId);
	}

	/**
	 * @return the index of the activity at which the trip of the given leg starts
	 */
	public static int originActivityIndex(Plan plan, Leg drtLeg) {
		List<PlanElement> elements = plan.getPlanElements();
		int index = indexOf(elements, drtLeg) - 1;
		while (!isMainActivity(elements.get(index))) {
			index--;
		}
		return index;
	}

	private static boolean isMainActivity(PlanElement element) {
		return element instanceof Activity activity && !TripStructureUtils.isStageActivityType(activity.getType());
	}

	private static int indexOf(List<PlanElement> elements, Leg leg) {
		for (int i = 0; i < elements.size(); i++) {
			if (elements.get(i) == leg) {
				return i;
			}
		}
		throw new IllegalArgumentException("Leg is not part of the plan");
	}

	private static StopCandidates.Candidate find(List<StopCandidates.Candidate> candidates, Id<Link> linkId) {
		return candidates.stream()
				.filter(candidate -> candidate.linkId().equals(linkId))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Link " + linkId + " is not a stop candidate"));
	}

	private static Optional<Id<Link>> firstWalkStartLink(List<PlanElement> elements, int from, int to) {
		return elements.subList(from, to)
				.stream()
				.filter(Leg.class::isInstance)
				.map(element -> ((Leg)element).getRoute())
				.filter(r -> r != null && r.getStartLinkId() != null)
				.map(Route::getStartLinkId)
				.findFirst();
	}

	private static Optional<Id<Link>> lastWalkEndLink(List<PlanElement> elements, int from, int to) {
		List<PlanElement> part = new ArrayList<>(elements.subList(from, to));
		Collections.reverse(part);
		return part.stream()
				.filter(Leg.class::isInstance)
				.map(element -> ((Leg)element).getRoute())
				.filter(r -> r != null && r.getEndLinkId() != null)
				.map(Route::getEndLinkId)
				.findFirst();
	}

	/**
	 * The agent's copy of a plan holds stage activities as regular activities (see
	 * {@link PopulationUtils#copyFromTo(Plan, Plan)}), which the mobsim may modify; an {@code InteractionActivity}
	 * would not allow setting the start time.
	 */
	private static Activity stageActivity(StopCandidates.Candidate stop, String mode) {
		return PopulationUtils.createActivity(
				PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(stop.coord(), stop.linkId(), mode));
	}

	private static Leg walkLeg(StopCandidates.Candidate candidate, Id<Link> startLinkId, Id<Link> endLinkId,
			String routingMode) {
		Leg walk = PopulationUtils.createLeg(TransportMode.walk);
		Route route = RouteUtils.createGenericRouteImpl(startLinkId, endLinkId);
		route.setTravelTime(candidate.walkDuration());
		route.setDistance(candidate.walkDistance());
		walk.setRoute(route);
		walk.setTravelTime(candidate.walkDuration());
		if (routingMode != null) {
			TripStructureUtils.setRoutingMode(walk, routingMode);
		}
		return walk;
	}

	private static void replace(List<PlanElement> elements, int from, int to, List<PlanElement> replacement) {
		elements.subList(from, to).clear();
		elements.addAll(from, replacement);
	}
}
