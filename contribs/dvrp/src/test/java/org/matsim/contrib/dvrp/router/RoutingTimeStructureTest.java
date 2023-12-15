/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule.AccessEgressFacilityFinder;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.Facility;
import org.mockito.Mockito;

/**
 * @author Sebastian Hörl (sebhoerl)
 */
public class RoutingTimeStructureTest {
	@Test
	void testTimingWithSimpleAccess() {
		TimeInterpretation timeInterpretation = TimeInterpretation.create(ConfigUtils.createConfig());

		Facility fromFacility = mock(Facility.class);
		Facility toFacility = mock(Facility.class);
		Facility accessFacility = mock(Facility.class);
		Facility egressFacility = mock(Facility.class);

		when(accessFacility.getLinkId()).thenReturn(Id.createLinkId("access"));
		when(egressFacility.getLinkId()).thenReturn(Id.createLinkId("egress"));

		AccessEgressFacilityFinder stopFinder = mock(AccessEgressFacilityFinder.class);
		when(stopFinder.findFacilities(eq(fromFacility), eq(toFacility), any())).thenReturn(
				Optional.of(Pair.of(accessFacility, egressFacility)));

		RoutingModule accessRouter = mock(RoutingModule.class);
		when(accessRouter.calcRoute(Mockito.any())).thenAnswer(iv -> {
			Leg leg = PopulationUtils.createLeg("walk");
			leg.setDepartureTime(((RoutingRequest)iv.getArgument(0)).getDepartureTime());
			leg.setTravelTime(120.0);
			return Arrays.asList(leg);
		});

		RoutingModule mainRouter = mock(RoutingModule.class);
		when(mainRouter.calcRoute(Mockito.any())).thenAnswer(iv -> {
			Leg leg = PopulationUtils.createLeg("drt");
			leg.setDepartureTime(((RoutingRequest)iv.getArgument(0)).getDepartureTime());
			leg.setTravelTime(800.0);
			return Arrays.asList(leg);
		});

		RoutingModule egressRouter = mock(RoutingModule.class);
		when(egressRouter.calcRoute(Mockito.any())).thenAnswer(iv -> {
			Leg leg = PopulationUtils.createLeg("walk");
			leg.setDepartureTime(((RoutingRequest)iv.getArgument(0)).getDepartureTime());
			leg.setTravelTime(45.0);
			return Arrays.asList(leg);
		});

		DvrpRoutingModule routingModule = new DvrpRoutingModule(mainRouter, accessRouter, egressRouter, stopFinder,
				"drt", timeInterpretation);
		List<? extends PlanElement> result = routingModule.calcRoute(
				DefaultRoutingRequest.withoutAttributes(fromFacility, toFacility, 500.0, null));

		assertThat(result.get(0)).isInstanceOf(Leg.class);
		assertThat(result.get(1)).isInstanceOf(Activity.class);
		assertThat(result.get(2)).isInstanceOf(Leg.class);
		assertThat(result.get(3)).isInstanceOf(Activity.class);
		assertThat(result.get(4)).isInstanceOf(Leg.class);

		assertThat(((Leg)result.get(0)).getDepartureTime().seconds()).isEqualTo(500.0);
		assertThat(((Activity)result.get(1)).getMaximumDuration().seconds()).isEqualTo(0.0);
		assertThat(((Activity)result.get(1)).getEndTime().isUndefined()).isTrue();
		assertThat(((Leg)result.get(2)).getDepartureTime().seconds()).isEqualTo(500.0 + 120.0);
		assertThat(((Activity)result.get(3)).getMaximumDuration().seconds()).isEqualTo(0.0);
		assertThat(((Activity)result.get(3)).getEndTime().isUndefined()).isTrue();
		assertThat(((Leg)result.get(4)).getDepartureTime().seconds()).isEqualTo(500.0 + 120.0 + 800.0);
	}

	@Test
	void testTimingWithComplexAccess() {
		TimeInterpretation timeInterpretation = TimeInterpretation.create(ConfigUtils.createConfig());

		Facility fromFacility = mock(Facility.class);
		Facility toFacility = mock(Facility.class);
		Facility accessFacility = mock(Facility.class);
		Facility egressFacility = mock(Facility.class);

		when(accessFacility.getLinkId()).thenReturn(Id.createLinkId("access"));
		when(egressFacility.getLinkId()).thenReturn(Id.createLinkId("egress"));

		AccessEgressFacilityFinder stopFinder = mock(AccessEgressFacilityFinder.class);
		when(stopFinder.findFacilities(eq(fromFacility), eq(toFacility), any())).thenReturn(
				Optional.of(Pair.of(accessFacility, egressFacility)));

		RoutingModule accessRouter = mock(RoutingModule.class);
		when(accessRouter.calcRoute(Mockito.any())).thenAnswer(iv -> {
			Leg leg1 = PopulationUtils.createLeg("walk");
			leg1.setDepartureTime(((RoutingRequest)iv.getArgument(0)).getDepartureTime());
			leg1.setTravelTime(120.0);

			Activity activity = PopulationUtils.createActivityFromCoord("walk interaction", new Coord(0.0, 0.0));
			activity.setMaximumDuration(0.0);
			activity.setEndTimeUndefined();

			Leg leg2 = PopulationUtils.createLeg("walk");
			leg2.setTravelTime(400.0);

			return Arrays.asList(leg1, activity, leg2);
		});

		RoutingModule mainRouter = mock(RoutingModule.class);
		when(mainRouter.calcRoute(Mockito.any())).thenAnswer(iv -> {
			Leg leg = PopulationUtils.createLeg("drt");
			leg.setDepartureTime(((RoutingRequest)iv.getArgument(0)).getDepartureTime());
			leg.setTravelTime(800.0);
			return Arrays.asList(leg);
		});

		RoutingModule egressRouter = mock(RoutingModule.class);
		when(egressRouter.calcRoute(Mockito.any())).thenAnswer(iv -> {
			Leg leg = PopulationUtils.createLeg("walk");
			leg.setDepartureTime(((RoutingRequest)iv.getArgument(0)).getDepartureTime());
			leg.setTravelTime(45.0);
			return Arrays.asList(leg);
		});

		DvrpRoutingModule routingModule = new DvrpRoutingModule(mainRouter, accessRouter, egressRouter, stopFinder,
				"drt", timeInterpretation);
		List<? extends PlanElement> result = routingModule.calcRoute(
				DefaultRoutingRequest.withoutAttributes(fromFacility, toFacility, 500.0, null));

		assertThat(result.get(0)).isInstanceOf(Leg.class);
		assertThat(result.get(1)).isInstanceOf(Activity.class);
		assertThat(result.get(2)).isInstanceOf(Leg.class);
		assertThat(result.get(3)).isInstanceOf(Activity.class);
		assertThat(result.get(4)).isInstanceOf(Leg.class);
		assertThat(result.get(5)).isInstanceOf(Activity.class);
		assertThat(result.get(6)).isInstanceOf(Leg.class);

		assertThat(((Leg)result.get(0)).getDepartureTime().seconds()).isEqualTo(500.0);
		assertThat(((Activity)result.get(3)).getMaximumDuration().seconds()).isEqualTo(0.0);
		assertThat(((Activity)result.get(3)).getEndTime().isUndefined()).isTrue();
		assertThat(((Leg)result.get(4)).getDepartureTime().seconds()).isEqualTo(500.0 + 120.0 + 400.0);
		assertThat(((Activity)result.get(5)).getMaximumDuration().seconds()).isEqualTo(0.0);
		assertThat(((Activity)result.get(5)).getEndTime().isUndefined()).isTrue();
		assertThat(((Leg)result.get(6)).getDepartureTime().seconds()).isEqualTo(500.0 + 120.0 + 400.0 + 800.0);
	}
}
