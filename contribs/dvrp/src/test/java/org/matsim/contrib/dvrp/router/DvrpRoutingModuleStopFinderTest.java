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

package org.matsim.contrib.dvrp.router;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule.AccessEgressFacilityFinder;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.Facility;
import org.mockito.Mockito;

/**
 * How {@link DvrpRoutingModule} queries its {@link AccessEgressFacilityFinder}.
 *
 * @author nkuehnel / MOIA
 */
public class DvrpRoutingModuleStopFinderTest {

	@Test
	void testStopsAreLookedUpOncePerRoutingCall() {
		Facility from = facility("from");
		Facility to = facility("to");
		Facility access = facility("access");
		Facility egress = facility("egress");

		// a finder without own candidates: findCandidates falls back to findFacilities
		int[] lookups = {0};
		AccessEgressFacilityFinder finder = request -> {
			lookups[0]++;
			return Optional.of(Pair.of(access, egress));
		};

		DvrpRoutingModule routingModule = new DvrpRoutingModule(router("drt"), router("walk"), router("walk"), finder,
				"drt", TimeInterpretation.create(ConfigUtils.createConfig()));
		List<? extends PlanElement> trip = routingModule.calcRoute(
				DefaultRoutingRequest.withoutAttributes(from, to, 8 * 3600, null));

		assertThat(trip).hasSize(5);
		assertThat(lookups[0]).isEqualTo(1);
	}

	@Test
	void testAlternativesAreAttachedForAccessAndEgressWithGenericRoutes() {
		StopCandidatesRoute route = candidatesRoute();
		routeWithAlternatives(route, router("walk"));

		verify(route).setStopCandidates(any());
	}

	@Test
	void testNoAlternativesForNetworkRoutedAccess() {
		// the legs to/from alternative stops are inserted with generic routes, which a network-routed access cannot get
		RoutingModule networkAccess = mock(RoutingModule.class);
		when(networkAccess.calcRoute(Mockito.any())).thenAnswer(invocation -> {
			Leg leg = PopulationUtils.createLeg("car");
			leg.setDepartureTime(((RoutingRequest)invocation.getArgument(0)).getDepartureTime());
			leg.setTravelTime(60.0);
			NetworkRoute networkRoute = mock(NetworkRoute.class);
			when(networkRoute.getTravelTime()).thenReturn(OptionalTime.defined(60.0));
			leg.setRoute(networkRoute);
			return List.of(leg);
		});
		StopCandidatesRoute route = candidatesRoute();
		routeWithAlternatives(route, networkAccess);

		verify(route, never()).setStopCandidates(any());
	}

	@Test
	void testNoAlternativesForAccessWithAnotherRouteType() {
		// e.g. a single transit leg: neither a network route nor generic, so it cannot be replaced by a generic route
		RoutingModule transitAccess = mock(RoutingModule.class);
		when(transitAccess.calcRoute(Mockito.any())).thenAnswer(invocation -> {
			Leg leg = PopulationUtils.createLeg("pt");
			leg.setDepartureTime(((RoutingRequest)invocation.getArgument(0)).getDepartureTime());
			leg.setTravelTime(60.0);
			Route transitRoute = mock(Route.class);
			when(transitRoute.getTravelTime()).thenReturn(OptionalTime.defined(60.0));
			when(transitRoute.getRouteType()).thenReturn("default_pt");
			leg.setRoute(transitRoute);
			return List.of(leg);
		});
		StopCandidatesRoute route = candidatesRoute();
		routeWithAlternatives(route, transitAccess);

		verify(route, never()).setStopCandidates(any());
	}

	private static void routeWithAlternatives(StopCandidatesRoute route, RoutingModule accessRouter) {
		Facility access = facility("access");
		Facility otherAccess = facility("otherAccess");
		Facility egress = facility("egress");
		AccessEgressFacilityFinder finder = new AccessEgressFacilityFinder() {
			@Override
			public Optional<Pair<Facility, Facility>> findFacilities(RoutingRequest request) {
				return Optional.of(Pair.of(access, egress));
			}

			@Override
			public Optional<AccessEgressFacilityCandidates> findCandidates(RoutingRequest request) {
				return Optional.of(new AccessEgressFacilityCandidates(List.of(access, otherAccess), List.of(egress)));
			}
		};
		RoutingModule mainRouter = mock(RoutingModule.class);
		when(mainRouter.calcRoute(Mockito.any())).thenAnswer(invocation -> {
			Leg leg = PopulationUtils.createLeg("drt");
			leg.setDepartureTime(((RoutingRequest)invocation.getArgument(0)).getDepartureTime());
			leg.setTravelTime(600.0);
			leg.setRoute(route);
			return List.of(leg);
		});

		new DvrpRoutingModule(mainRouter, accessRouter, router("walk"), finder, "drt",
				TimeInterpretation.create(ConfigUtils.createConfig()))
				.calcRoute(DefaultRoutingRequest.withoutAttributes(facility("from"), facility("to"), 8 * 3600, null));
	}

	private static StopCandidatesRoute candidatesRoute() {
		StopCandidatesRoute route = mock(StopCandidatesRoute.class);
		when(route.getTravelTime()).thenReturn(OptionalTime.defined(600.0));
		return route;
	}

	private static Facility facility(String linkId) {
		Facility facility = mock(Facility.class);
		when(facility.getLinkId()).thenReturn(Id.createLinkId(linkId));
		when(facility.getCoord()).thenReturn(new Coord(0, 0));
		return facility;
	}

	private static RoutingModule router(String mode) {
		RoutingModule router = mock(RoutingModule.class);
		when(router.calcRoute(Mockito.any())).thenAnswer(invocation -> {
			RoutingRequest request = invocation.getArgument(0);
			Leg leg = PopulationUtils.createLeg(mode);
			leg.setDepartureTime(request.getDepartureTime());
			leg.setTravelTime(60.0);
			// teleported legs get a generic route
			Route teleported = RouteUtils.createGenericRouteImpl(request.getFromFacility().getLinkId(),
					request.getToFacility().getLinkId());
			teleported.setTravelTime(60.0);
			teleported.setDistance(50.0);
			leg.setRoute(teleported);
			return List.of(leg);
		});
		return router;
	}
}
