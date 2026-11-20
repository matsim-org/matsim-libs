/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.router;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.Facility;

/**
 * @author jbischoff
 * @author michalm (Michal Maciejewski)
 */
public class DvrpRoutingModule implements RoutingModule {
	private static final Logger logger = LogManager.getLogger(DvrpRoutingModule.class);

	public interface AccessEgressFacilityFinder {
		/**
		 * Finds the access and egress facilities (e.g. stops) to be used for the given routing request, or an empty
		 * {@link Optional} if the request cannot be served (e.g. because no facility is close enough, or because the
		 * service is not available at the requested departure time).
		 * <p>
		 * The whole {@link RoutingRequest} is passed such that implementations may take the departure time, the person
		 * and the trip attributes into account.
		 */
		Optional<Pair<Facility, Facility>> findFacilities(RoutingRequest request);

		/**
		 * Finds the access and egress facilities a passenger accepts for the given routing request. The first entry of
		 * each list is the preferred facility, which must be the one {@link #findFacilities} returns. By default, there
		 * are no alternatives.
		 */
		default Optional<AccessEgressFacilityCandidates> findCandidates(RoutingRequest request) {
			return findFacilities(request).map(pair -> AccessEgressFacilityCandidates.of(pair.getLeft(), pair.getRight()));
		}
	}

	private final AccessEgressFacilityFinder stopFinder;
	private final String mode;
	private final RoutingModule mainRouter;
	private final RoutingModule accessRouter;
	private final RoutingModule egressRouter;
	private final TimeInterpretation timeInterpretation;

	public DvrpRoutingModule(RoutingModule mainRouter, RoutingModule accessRouter, RoutingModule egressRouter,
			AccessEgressFacilityFinder stopFinder, String mode, TimeInterpretation timeInterpretation) {
		this.mainRouter = mainRouter;
		this.stopFinder = stopFinder;
		this.mode = mode;
		this.accessRouter = accessRouter;
		this.egressRouter = egressRouter;
		this.timeInterpretation = timeInterpretation;
	}

	@Override
	public List<? extends PlanElement> calcRoute(RoutingRequest request) {
		final Facility fromFacility = Objects.requireNonNull(request.getFromFacility(), "fromFacility is null");
		final Facility toFacility = Objects.requireNonNull(request.getToFacility(), "toFacility is null");
		final double departureTime = request.getDepartureTime();
		final Person person = request.getPerson();

		Optional<Pair<Facility, Facility>> stops = stopFinder.findFacilities(request);
		if (stops.isEmpty()) {
			logger.debug("No access/egress stops found, agent will use fallback mode as leg mode (usually "
					+ TransportMode.walk
					+ ") and routing mode "
					+ mode
					+ ". Agent Id:\t"
					+ person.getId());
			return null;
		}

		Facility accessFacility = stops.get().getLeft();
		Facility egressFacility = stops.get().getRight();
		if (accessFacility.getLinkId().equals(egressFacility.getLinkId())) {
			logger.debug("Start and end stop are the same, agent will use fallback mode as leg mode (usually "
					+ TransportMode.walk
					+ ") and routing mode "
					+ mode
					+ ". Agent Id:\t"
					+ person.getId());
			return null;
		}

		List<PlanElement> trip = new ArrayList<>();

		double now = departureTime;

		// access (sub-)trip:
		List<? extends PlanElement> accessTrip = accessRouter.calcRoute(
				DefaultRoutingRequest.of(fromFacility, accessFacility, now, person, request.getAttributes()));
		double accessEndTime = timeInterpretation.decideOnElementsEndTime(accessTrip, now).seconds();
		if (!accessTrip.isEmpty()) {
			trip.addAll(accessTrip);
			now = timeInterpretation.decideOnElementsEndTime(accessTrip, now).seconds();

			// interaction activity:
			trip.add(createDrtStageActivity(accessFacility, now));
		}

		// dvrp proper leg:
		List<? extends PlanElement> drtLeg = mainRouter.calcRoute(
				DefaultRoutingRequest.of(accessFacility, egressFacility, now, person, request.getAttributes()));
		trip.addAll(drtLeg);
		now = timeInterpretation.decideOnElementsEndTime(drtLeg, now).seconds();

		// egress (sub-)trip:
		double egressStartTime = now;
		List<? extends PlanElement> egressTrip = egressRouter.calcRoute(
				DefaultRoutingRequest.of(egressFacility, toFacility, now, person, request.getAttributes()));
		if (!egressTrip.isEmpty()) {
			// interaction activity:
			trip.add(createDrtStageActivity(egressFacility, now));

			trip.addAll(egressTrip);
		}

		// the trip itself is always built for the facilities of findFacilities; candidates are only an addition
		Optional<AccessEgressFacilityCandidates> candidates = stopFinder.findCandidates(request)
				.filter(AccessEgressFacilityCandidates::hasAlternatives)
				.filter(c -> c.preferredAccess().getLinkId().equals(accessFacility.getLinkId())
						&& c.preferredEgress().getLinkId().equals(egressFacility.getLinkId()));
		if (candidates.isPresent()) {
			attachStopCandidates(request, candidates.get(), drtLeg, accessTrip, departureTime, accessEndTime,
					egressTrip, egressStartTime);
		}

		return trip;
	}

	/**
	 * Routes the walks to/from all stop candidates with the access/egress routers and attaches them to the route of the
	 * main leg. The preferred candidates reuse the walks of the routed trip.
	 */
	private void attachStopCandidates(RoutingRequest request, AccessEgressFacilityCandidates candidates,
			List<? extends PlanElement> mainLeg, List<? extends PlanElement> accessTrip, double departureTime,
			double accessEndTime, List<? extends PlanElement> egressTrip, double egressStartTime) {
		Optional<StopCandidatesRoute> route = mainLeg.stream()
				.filter(Leg.class::isInstance)
				.map(element -> ((Leg)element).getRoute())
				.filter(StopCandidatesRoute.class::isInstance)
				.map(StopCandidatesRoute.class::cast)
				.findFirst();
		if (route.isEmpty()) {
			return; // the main leg router does not support stop candidates
		}

		List<StopCandidates.Candidate> access = new ArrayList<>();
		access.add(candidate(candidates.preferredAccess(), accessTrip, departureTime, accessEndTime));
		for (Facility facility : candidates.access().subList(1, candidates.access().size())) {
			List<? extends PlanElement> walk = accessRouter.calcRoute(
					DefaultRoutingRequest.of(request.getFromFacility(), facility, departureTime, request.getPerson(),
							request.getAttributes()));
			if (walk == null) {
				continue; // candidate not reachable
			}
			double endTime = timeInterpretation.decideOnElementsEndTime(walk, departureTime).seconds();
			access.add(candidate(facility, walk, departureTime, endTime));
		}

		List<StopCandidates.Candidate> egress = new ArrayList<>();
		double egressEndTime = timeInterpretation.decideOnElementsEndTime(egressTrip, egressStartTime).seconds();
		egress.add(candidate(candidates.preferredEgress(), egressTrip, egressStartTime, egressEndTime));
		for (Facility facility : candidates.egress().subList(1, candidates.egress().size())) {
			List<? extends PlanElement> walk = egressRouter.calcRoute(
					DefaultRoutingRequest.of(facility, request.getToFacility(), egressStartTime, request.getPerson(),
							request.getAttributes()));
			if (walk == null) {
				continue; // candidate not reachable
			}
			double endTime = timeInterpretation.decideOnElementsEndTime(walk, egressStartTime).seconds();
			egress.add(candidate(facility, walk, egressStartTime, endTime));
		}

		route.get().setStopCandidates(new StopCandidates(access, egress));
	}

	private static StopCandidates.Candidate candidate(Facility facility, List<? extends PlanElement> walk,
			double startTime, double endTime) {
		double distance = walk.stream()
				.filter(Leg.class::isInstance)
				.map(element -> ((Leg)element).getRoute())
				.filter(Objects::nonNull)
				.mapToDouble(Route::getDistance)
				.filter(Double::isFinite)
				.sum();
		return new StopCandidates.Candidate(facility.getLinkId(), facility.getCoord(), endTime - startTime, distance);
	}

	private Activity createDrtStageActivity(Facility stopFacility, double now) {
		return PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(stopFacility.getCoord(),
				stopFacility.getLinkId(), mode);
	}
}
