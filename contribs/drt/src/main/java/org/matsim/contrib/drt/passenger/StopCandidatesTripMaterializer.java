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

package org.matsim.contrib.drt.passenger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.dvrp.router.RoutedStopCandidates;
import org.matsim.contrib.dvrp.router.StopCandidatesRoute;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Rewrites the access and egress part of an agent's trip to the pickup/dropoff stops chosen by the insertion search,
 * among the stop candidates routed into the {@link StopCandidatesRoute} of its DRT leg (see
 * {@link DrtStopCandidatesFactory}). The rewritten legs use the routed walk duration and distance of the chosen
 * candidate. In the routed candidates, access candidates are the possible pickup stops and egress candidates the
 * possible dropoff stops.
 * <p>
 * Meant for the agent's copy of the plan during the mobsim; the plan in the population keeps the routed (preferred)
 * stops, so the next iteration builds the same request again.
 *
 * @author nkuehnel / MOIA
 */
public final class StopCandidatesTripMaterializer {
	private StopCandidatesTripMaterializer() {
	}

	/**
	 * @return true if the trip of the given leg has at least one leg before and after it (the access and egress walks),
	 * so that it can be moved to other stops
	 */
	public static boolean hasAccessAndEgressLegs(Plan plan, Leg drtLeg) {
		Trip trip = TripStructureUtils.findTripAtPlanElement(drtLeg, plan);
		if (trip == null) {
			return false; // not a complete trip between two activities
		}
		List<PlanElement> elements = trip.getTripElements();
		int legIndex = Iterables.indexOf(elements, e -> e == drtLeg);
		return elements.subList(0, legIndex).stream().anyMatch(Leg.class::isInstance)
				&& elements.subList(legIndex + 1, elements.size()).stream().anyMatch(Leg.class::isInstance);
	}

	/**
	 * Rewrites the access and/or egress part of the trip of the given DRT leg to the chosen pickup/dropoff stops. The
	 * new legs are teleported legs with the routed duration and distance of the chosen candidate and the mode of the
	 * routed access/egress leg (the candidates are routed with the same access/egress router).
	 *
	 * @return true if the plan was changed
	 */
	public static boolean materialize(Plan plan, Leg drtLeg, Id<Link> pickupLinkId, Id<Link> dropoffLinkId) {
		Preconditions.checkArgument(drtLeg.getRoute() instanceof StopCandidatesRoute,
				"Leg has no route with stop candidates");
		Route route = drtLeg.getRoute();
		RoutedStopCandidates candidates = ((StopCandidatesRoute)route).getStopCandidates().orElseThrow();
		boolean changePickup = !pickupLinkId.equals(route.getStartLinkId());
		boolean changeDropoff = !dropoffLinkId.equals(route.getEndLinkId());
		if (!changePickup && !changeDropoff) {
			return false;
		}

		Trip trip = Preconditions.checkNotNull(TripStructureUtils.findTripAtPlanElement(drtLeg, plan),
				"Leg is not part of a trip between two activities");
		List<PlanElement> elements = trip.getTripElements();
		int legIndex = Iterables.indexOf(elements, e -> e == drtLeg);
		List<PlanElement> accessPart = elements.subList(0, legIndex);
		List<PlanElement> egressPart = elements.subList(legIndex + 1, elements.size());
		String mode = drtLeg.getMode();
		String routingMode = TripStructureUtils.getRoutingMode(drtLeg);

		List<PlanElement> newTrip = new ArrayList<>();
		if (changePickup) {
			RoutedStopCandidates.Candidate pickup = find(candidates.access(), pickupLinkId);
			Activity origin = trip.getOriginActivity();
			Id<Link> originLinkId = firstWalkStartLink(accessPart).orElse(origin.getLinkId());
			Leg walk = accessEgressLeg(pickup, firstLegMode(accessPart), originLinkId, pickup.linkId(), routingMode);
			origin.getEndTime().ifDefined(walk::setDepartureTime);
			newTrip.add(walk);
			newTrip.add(stageActivity(pickup, mode));
			route.setStartLinkId(pickupLinkId);
			if (walk.getDepartureTime().isDefined()) {
				drtLeg.setDepartureTime(walk.getDepartureTime().seconds() + pickup.walkDuration());
			}
		} else {
			newTrip.addAll(accessPart);
		}
		newTrip.add(drtLeg);
		if (changeDropoff) {
			RoutedStopCandidates.Candidate dropoff = find(candidates.egress(), dropoffLinkId);
			Id<Link> destinationLinkId = lastWalkEndLink(egressPart).orElse(trip.getDestinationActivity().getLinkId());
			newTrip.add(stageActivity(dropoff, mode));
			newTrip.add(accessEgressLeg(dropoff, lastLegMode(egressPart), dropoff.linkId(), destinationLinkId,
					routingMode));
			route.setEndLinkId(dropoffLinkId);
		} else {
			newTrip.addAll(egressPart);
		}

		TripRouter.insertTrip(plan, trip.getOriginActivity(), newTrip, trip.getDestinationActivity());
		return true;
	}

	/**
	 * @return the activity at which the trip of the given leg starts
	 */
	public static Activity originActivity(Plan plan, Leg drtLeg) {
		return Preconditions.checkNotNull(TripStructureUtils.findTripAtPlanElement(drtLeg, plan),
				"Leg is not part of a trip between two activities").getOriginActivity();
	}

	private static RoutedStopCandidates.Candidate find(List<RoutedStopCandidates.Candidate> candidates, Id<Link> linkId) {
		return candidates.stream()
				.filter(candidate -> candidate.linkId().equals(linkId))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Link " + linkId + " is not a stop candidate"));
	}

	private static Optional<Id<Link>> firstWalkStartLink(List<PlanElement> part) {
		return part.stream()
				.filter(Leg.class::isInstance)
				.map(element -> ((Leg)element).getRoute())
				.filter(r -> r != null && r.getStartLinkId() != null)
				.map(Route::getStartLinkId)
				.findFirst();
	}

	private static Optional<Id<Link>> lastWalkEndLink(List<PlanElement> part) {
		return Lists.reverse(part).stream()
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
	private static Activity stageActivity(RoutedStopCandidates.Candidate stop, String mode) {
		return PopulationUtils.createActivity(
				PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(stop.coord(), stop.linkId(), mode));
	}

	private static String firstLegMode(List<PlanElement> part) {
		return part.stream().filter(Leg.class::isInstance).map(e -> ((Leg)e).getMode()).findFirst()
				.orElse(TransportMode.walk);
	}

	private static String lastLegMode(List<PlanElement> part) {
		return Lists.reverse(part).stream().filter(Leg.class::isInstance).map(e -> ((Leg)e).getMode()).findFirst()
				.orElse(TransportMode.walk);
	}

	private static Leg accessEgressLeg(RoutedStopCandidates.Candidate candidate, String mode, Id<Link> startLinkId,
			Id<Link> endLinkId, String routingMode) {
		Leg walk = PopulationUtils.createLeg(mode);
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

}
