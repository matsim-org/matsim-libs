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
import org.matsim.core.router.TripStructureUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;

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
	 * @return true if the trip of the given DRT leg can be moved to other stops, i.e. it consists of exactly one access
	 * leg, the stage activity at the pickup stop, the DRT leg, the stage activity at the dropoff stop and one egress leg
	 * between two (non-stage) activities. This is the structure {@code DvrpRoutingModule} produces for trips with stop
	 * candidates.
	 */
	public static boolean hasSingleAccessAndEgressLeg(Plan plan, Leg drtLeg) {
		return indexOfMovableLeg(plan, drtLeg) >= 0;
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

		int i = indexOfMovableLeg(plan, drtLeg);
		Preconditions.checkArgument(i >= 0, "Trip of the leg is not access leg - stop - DRT leg - stop - egress leg");
		List<PlanElement> elements = plan.getPlanElements();
		String routingMode = TripStructureUtils.getRoutingMode(drtLeg);

		if (changePickup) {
			RoutedStopCandidates.Candidate pickup = find(candidates.access(), pickupLinkId);
			Activity origin = (Activity)elements.get(i - 3);
			Leg routedAccess = (Leg)elements.get(i - 2);
			Leg access = accessEgressLeg(pickup, routedAccess.getMode(), startLinkId(routedAccess, origin),
					pickup.linkId(), routingMode);
			origin.getEndTime().ifDefined(access::setDepartureTime);
			elements.set(i - 2, access);
			elements.set(i - 1, stageActivity(pickup, drtLeg.getMode()));
			route.setStartLinkId(pickupLinkId);
			access.getDepartureTime().ifDefined(t -> drtLeg.setDepartureTime(t + pickup.walkDuration()));
		}
		if (changeDropoff) {
			RoutedStopCandidates.Candidate dropoff = find(candidates.egress(), dropoffLinkId);
			Activity destination = (Activity)elements.get(i + 3);
			Leg routedEgress = (Leg)elements.get(i + 2);
			elements.set(i + 1, stageActivity(dropoff, drtLeg.getMode()));
			elements.set(i + 2, accessEgressLeg(dropoff, routedEgress.getMode(), dropoff.linkId(),
					endLinkId(routedEgress, destination), routingMode));
			route.setEndLinkId(dropoffLinkId);
		}
		return true;
	}

	/**
	 * @return the activity at which the trip of the given leg starts; the trip must have a single access and egress leg
	 */
	public static Activity originActivity(Plan plan, Leg drtLeg) {
		int i = indexOfMovableLeg(plan, drtLeg);
		Preconditions.checkArgument(i >= 0, "Trip of the leg is not access leg - stop - DRT leg - stop - egress leg");
		return (Activity)plan.getPlanElements().get(i - 3);
	}

	/**
	 * @return the index of the DRT leg in the plan if its trip is activity - access leg - stop - DRT leg - stop - egress
	 * leg - activity, otherwise -1
	 */
	private static int indexOfMovableLeg(Plan plan, Leg drtLeg) {
		List<PlanElement> elements = plan.getPlanElements();
		int i = Iterables.indexOf(elements, e -> e == drtLeg);
		if (i < 3 || i + 3 >= elements.size()) {
			return -1;
		}
		boolean movable = isActivity(elements.get(i - 3)) && elements.get(i - 2) instanceof Leg
				&& isStageActivity(elements.get(i - 1)) && isStageActivity(elements.get(i + 1))
				&& elements.get(i + 2) instanceof Leg && isActivity(elements.get(i + 3));
		return movable ? i : -1;
	}

	private static boolean isActivity(PlanElement element) {
		return element instanceof Activity activity && !TripStructureUtils.isStageActivityType(activity.getType());
	}

	private static boolean isStageActivity(PlanElement element) {
		return element instanceof Activity activity && TripStructureUtils.isStageActivityType(activity.getType());
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
