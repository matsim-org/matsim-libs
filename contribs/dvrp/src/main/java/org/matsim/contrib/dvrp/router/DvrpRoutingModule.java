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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

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
import org.matsim.core.population.routes.GenericRouteImpl;
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
	private final AtomicBoolean warnedAboutRouteTypes = new AtomicBoolean();

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

		// one lookup per trip: the preferred candidates are the facilities of findFacilities
		Optional<AccessEgressFacilityCandidates> stops = stopFinder.findCandidates(request);
		if (stops.isEmpty()) {
			logger.debug("No access/egress stops found, agent will use fallback mode as leg mode (usually "
					+ TransportMode.walk
					+ ") and routing mode "
					+ mode
					+ ". Agent Id:\t"
					+ person.getId());
			return null;
		}

		Facility accessFacility = stops.get().preferredAccess();
		Facility egressFacility = stops.get().preferredEgress();
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
			now = accessEndTime;

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

		// the trip itself is always built for the preferred facilities; alternatives are only an addition
		if (stops.get().hasAlternatives()) {
			attachStopCandidates(request, stops.get(), drtLeg, accessTrip, departureTime, accessEndTime, egressTrip,
					egressStartTime);
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
		if (!isEmptyOrSingleGenericLeg(accessTrip) || !isEmptyOrSingleGenericLeg(egressTrip)) {
			// the legs to/from alternative stops can only be inserted with generic routes
			warnOnceAboutRouteTypes(isEmptyOrSingleGenericLeg(accessTrip) ? egressTrip : accessTrip);
			return;
		}

		RoutedStopCandidates.Candidate preferredAccess = candidate(candidates.preferredAccess(), accessTrip, departureTime,
				accessEndTime);
		List<RoutedStopCandidates.Candidate> access = routeAlternatives(preferredAccess,
				candidates.access().subList(1, candidates.access().size()), departureTime,
				stop -> accessRouter.calcRoute(DefaultRoutingRequest.of(request.getFromFacility(), stop, departureTime,
						request.getPerson(), request.getAttributes())));

		double egressEndTime = timeInterpretation.decideOnElementsEndTime(egressTrip, egressStartTime).seconds();
		RoutedStopCandidates.Candidate preferredEgress = candidate(candidates.preferredEgress(), egressTrip,
				egressStartTime, egressEndTime);
		List<RoutedStopCandidates.Candidate> egress = routeAlternatives(preferredEgress,
				candidates.egress().subList(1, candidates.egress().size()), egressStartTime,
				stop -> egressRouter.calcRoute(DefaultRoutingRequest.of(stop, request.getToFacility(), egressStartTime,
						request.getPerson(), request.getAttributes())));

		route.get().setStopCandidates(new RoutedStopCandidates(access, egress));
	}

	/**
	 * @return the preferred candidate followed by the alternatives that can be reached by a single leg with a generic
	 * route, each with the duration and distance of its routed walk
	 */
	private List<RoutedStopCandidates.Candidate> routeAlternatives(RoutedStopCandidates.Candidate preferred,
			List<Facility> alternatives, double startTime, Function<Facility, List<? extends PlanElement>> router) {
		List<RoutedStopCandidates.Candidate> candidates = new ArrayList<>();
		candidates.add(preferred);
		for (Facility stop : alternatives) {
			List<? extends PlanElement> walk = router.apply(stop);
			if (walk == null || !isEmptyOrSingleGenericLeg(walk)) {
				continue; // candidate not reachable, or not by a leg with a generic route
			}
			double endTime = timeInterpretation.decideOnElementsEndTime(walk, startTime).seconds();
			candidates.add(candidate(stop, walk, startTime, endTime));
		}
		return candidates;
	}

	private void warnOnceAboutRouteTypes(List<? extends PlanElement> trip) {
		if (warnedAboutRouteTypes.compareAndSet(false, true)) {
			logger.warn("Stop candidates of mode {} are not used for trips whose access or egress is not a single leg "
					+ "with a generic route (here: {}); such trips use the routed stops. Further occurrences are not "
					+ "logged.", mode, trip.stream().map(DvrpRoutingModule::describe).toList());
		}
	}

	/**
	 * @return "mode/route type" for a leg, "activity" otherwise (for log messages)
	 */
	private static String describe(PlanElement element) {
		if (!(element instanceof Leg leg)) {
			return "activity";
		}
		String routeType = leg.getRoute() == null ? "no route" : leg.getRoute().getRouteType();
		return leg.getMode() + "/" + routeType;
	}

	/**
	 * @return true if the access/egress (sub-)trip is empty or a single leg with a generic route, i.e. one that can be
	 * replaced by a leg of the same mode with a generic route of another duration and distance. This is conservative:
	 * legs with network routes are excluded even if the mobsim teleports them (e.g. network-routed modes that are no
	 * main modes of the qsim, or legs teleported by FISS), as are other route types (e.g. transit routes).
	 */
	private static boolean isEmptyOrSingleGenericLeg(List<? extends PlanElement> trip) {
		return trip.isEmpty()
				|| trip.size() == 1 && trip.getFirst() instanceof Leg leg && leg.getRoute() instanceof GenericRouteImpl;
	}

	private static RoutedStopCandidates.Candidate candidate(Facility facility, List<? extends PlanElement> walk,
			double startTime, double endTime) {
		double distance = walk.stream()
				.filter(Leg.class::isInstance)
				.map(element -> ((Leg)element).getRoute())
				.filter(Objects::nonNull)
				.mapToDouble(Route::getDistance)
				.filter(Double::isFinite)
				.sum();
		return new RoutedStopCandidates.Candidate(facility.getLinkId(), facility.getCoord(), endTime - startTime, distance);
	}

	private Activity createDrtStageActivity(Facility stopFacility, double now) {
		return PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(stopFacility.getCoord(),
				stopFacility.getLinkId(), mode);
	}
}
