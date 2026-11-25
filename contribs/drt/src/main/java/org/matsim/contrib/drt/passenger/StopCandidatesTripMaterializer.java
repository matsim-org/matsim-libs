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

import org.matsim.api.core.v01.Id;
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

	// positions in a trip that can be moved to other stops
	private static final int ACCESS = 0;
	private static final int PICKUP_STOP = 1;
	private static final int DRT = 2;
	private static final int DROPOFF_STOP = 3;
	private static final int EGRESS = 4;

	/**
	 * @return true if the trip of the given DRT leg can be moved to other stops, i.e. it consists of exactly one access
	 * leg, the stage activity at the pickup stop, the DRT leg, the stage activity at the dropoff stop and one egress leg.
	 * This is the structure {@code DvrpRoutingModule} produces for trips with stop candidates; intermodal trips (e.g. with
	 * pt legs) have more elements.
	 */
	public static boolean hasSingleAccessAndEgressLeg(Plan plan, Leg drtLeg) {
		return movableTrip(plan, drtLeg) != null;
	}

	/**
	 * Rewrites the access and/or egress leg (and the stage activity at the stop) of the trip of the given DRT leg to
	 * the chosen pickup/dropoff stops. The new legs are teleported legs with the routed duration and distance of the
	 * chosen candidate and the mode of the replaced leg (the candidates are routed with the same access/egress router).
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

		Trip trip = movableTrip(plan, drtLeg);
		Preconditions.checkArgument(trip != null, "Trip of the leg is not access leg - stop - DRT leg - stop - egress leg");
		List<PlanElement> newTrip = new ArrayList<>(trip.getTripElements());
		String routingMode = TripStructureUtils.getRoutingMode(drtLeg);

		if (changePickup) {
			RoutedStopCandidates.Candidate pickup = find(candidates.access(), pickupLinkId);
			Activity origin = trip.getOriginActivity();
			Leg routedAccess = (Leg)newTrip.get(ACCESS);
			Leg access = accessEgressLeg(pickup, routedAccess.getMode(), startLinkId(routedAccess, origin),
					pickup.linkId(), routingMode);
			origin.getEndTime().ifDefined(access::setDepartureTime);
			newTrip.set(ACCESS, access);
			newTrip.set(PICKUP_STOP, stageActivity(pickup, drtLeg.getMode()));
			route.setStartLinkId(pickupLinkId);
			access.getDepartureTime().ifDefined(t -> drtLeg.setDepartureTime(t + pickup.walkDuration()));
		}
		if (changeDropoff) {
			RoutedStopCandidates.Candidate dropoff = find(candidates.egress(), dropoffLinkId);
			Leg routedEgress = (Leg)newTrip.get(EGRESS);
			newTrip.set(DROPOFF_STOP, stageActivity(dropoff, drtLeg.getMode()));
			newTrip.set(EGRESS, accessEgressLeg(dropoff, routedEgress.getMode(), dropoff.linkId(),
					endLinkId(routedEgress, trip.getDestinationActivity()), routingMode));
			route.setEndLinkId(dropoffLinkId);
		}

		TripRouter.insertTrip(plan, trip.getOriginActivity(), newTrip, trip.getDestinationActivity());
		return true;
	}

	/**
	 * @return the activity at which the trip of the given leg starts; the trip must have a single access and egress leg
	 */
	public static Activity originActivity(Plan plan, Leg drtLeg) {
		Trip trip = movableTrip(plan, drtLeg);
		Preconditions.checkArgument(trip != null, "Trip of the leg is not access leg - stop - DRT leg - stop - egress leg");
		return trip.getOriginActivity();
	}

	/**
	 * @return the trip of the given DRT leg if it is access leg - stop - DRT leg - stop - egress leg, otherwise null
	 */
	private static Trip movableTrip(Plan plan, Leg drtLeg) {
		Trip trip = TripStructureUtils.findTripAtPlanElement(drtLeg, plan);
		if (trip == null) {
			return null; // not a complete trip between two activities
		}
		List<PlanElement> elements = trip.getTripElements();
		boolean movable = elements.size() == 5
				&& elements.get(ACCESS) instanceof Leg
				&& elements.get(PICKUP_STOP) instanceof Activity
				&& elements.get(DRT) == drtLeg
				&& elements.get(DROPOFF_STOP) instanceof Activity
				&& elements.get(EGRESS) instanceof Leg;
		return movable ? trip : null;
	}

	private static Id<Link> startLinkId(Leg leg, Activity origin) {
		return leg.getRoute() != null && leg.getRoute().getStartLinkId() != null ?
				leg.getRoute().getStartLinkId() :
				origin.getLinkId();
	}

	private static Id<Link> endLinkId(Leg leg, Activity destination) {
		return leg.getRoute() != null && leg.getRoute().getEndLinkId() != null ?
				leg.getRoute().getEndLinkId() :
				destination.getLinkId();
	}

	private static RoutedStopCandidates.Candidate find(List<RoutedStopCandidates.Candidate> candidates, Id<Link> linkId) {
		return candidates.stream()
				.filter(candidate -> candidate.linkId().equals(linkId))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Link " + linkId + " is not a stop candidate"));
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
