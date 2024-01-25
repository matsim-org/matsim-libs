/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

package org.matsim.contrib.minibus.routeProvider;

import java.util.ArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.PConstants;
import org.matsim.contrib.minibus.hook.Operator;
import org.matsim.contrib.minibus.hook.PPlan;
import org.matsim.contrib.minibus.schedule.CreateStopsForAllCarLinks;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;

public class SimpleCircleScheduleProviderTest {

@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	final void testCreateTransitLine() {

		Scenario scenario = PScenarioHelper.createTestNetwork();
		PConfigGroup pC = new PConfigGroup();


		TransitSchedule tS = CreateStopsForAllCarLinks.createStopsForAllCarLinks(scenario.getNetwork(), pC);

		SimpleCircleScheduleProvider prov = new SimpleCircleScheduleProvider(pC.getPIdentifier(), tS, scenario.getNetwork(), null, pC.getVehicleMaximumVelocity(), pC.getDriverRestTime(), pC.getMode());

		Id<PPlan> lineId = Id.create("line1", PPlan.class);
		Id<PPlan> routeId = Id.create("route1", PPlan.class);

		PPlan plan = new PPlan(routeId, "noCreator", PConstants.founderPlanId);
		plan.setStartTime(7.0 * 3600.0);
		plan.setEndTime(9.0 * 3600.0);
		plan.setNVehicles(2);
		TransitStopFacility startStop = tS.getFacilities().get(Id.create(pC.getPIdentifier() + "1424", TransitStopFacility.class));
		TransitStopFacility endStop = tS.getFacilities().get(Id.create(pC.getPIdentifier() + "4434", TransitStopFacility.class));
		ArrayList<TransitStopFacility> stopsToBeServed = new ArrayList<>();
		stopsToBeServed.add(startStop);
		stopsToBeServed.add(endStop);
		plan.setStopsToBeServed(stopsToBeServed);

		ArrayList<Id<Link>> refIds = new ArrayList<>();
		refIds.add(Id.create("1424", Link.class)); refIds.add(Id.create("2434", Link.class));
		refIds.add(Id.create("3444", Link.class)); refIds.add(Id.create("4434", Link.class));
		refIds.add(Id.create("3424", Link.class)); refIds.add(Id.create("2414", Link.class));
		refIds.add(Id.create("1424", Link.class));

		TransitLine line = prov.createTransitLineFromOperatorPlan(Id.create(lineId, Operator.class), plan);

		Assertions.assertEquals(Id.create(lineId, TransitLine.class), line.getId(), "Transit line ids have to be the same");

		for (TransitRoute route : line.getRoutes().values()) {
			Assertions.assertEquals(Id.create(lineId + "-" + routeId, TransitRoute.class), route.getId(), "Route id have to be the same");
			Assertions.assertEquals(14.0, route.getDepartures().size(), MatsimTestUtils.EPSILON, "Number of departures");

			// check stops
			int i = 0;
			for (TransitRouteStop stop : route.getStops()) {
				Assertions.assertEquals(Id.create(pC.getPIdentifier() + refIds.get(i), TransitStopFacility.class), stop.getStopFacility().getId(), "Route stop ids have to be the same");
				i++;
			}

			// check links
			Assertions.assertEquals(refIds.get(0), route.getRoute().getStartLinkId(), "Start link id has to be the same");

			i = 1;
			for (Id<Link> linkId : route.getRoute().getLinkIds()) {
				Assertions.assertEquals(refIds.get(i), linkId, "Route link ids have to be the same");
				i++;
			}

			Assertions.assertEquals(refIds.get(refIds.size() - 1), route.getRoute().getEndLinkId(), "End link id has to be the same");
		}
	}

	@Test
	final void testGetRandomTransitStop() {

		MutableScenario scenario = (MutableScenario) PScenarioHelper.createTestNetwork();
		PConfigGroup pC = new PConfigGroup();
		RandomStopProvider randomStopProvider = new RandomStopProvider(pC, scenario.getPopulation(), scenario.getTransitSchedule(), null);

		SimpleCircleScheduleProvider prov = new SimpleCircleScheduleProvider(pC.getPIdentifier(), scenario.getTransitSchedule(), scenario.getNetwork(), randomStopProvider, pC.getVehicleMaximumVelocity(), pC.getDriverRestTime(), pC.getMode());

		for (int i = 0; i < 5; i++) {
			TransitStopFacility stop1 = prov.getRandomTransitStop(0);
			TransitStopFacility stop2 = prov.getRandomTransitStop(0);
			Assertions.assertNotSame(stop1.getId(), stop2.getId(), "Stop should not be the same");
		}
	}

	@Test
	final void testCreateEmptyLine() {

		MutableScenario scenario = (MutableScenario) PScenarioHelper.createTestNetwork();
		PConfigGroup pC = new PConfigGroup();

		Id<Operator> lineId = Id.create("1", Operator.class);

		SimpleCircleScheduleProvider prov = new SimpleCircleScheduleProvider(pC.getPIdentifier(), scenario.getTransitSchedule(), scenario.getNetwork(), null, pC.getVehicleMaximumVelocity(), pC.getDriverRestTime(), pC.getMode());
		TransitLine line = prov.createEmptyLineFromOperator(lineId);

		Assertions.assertEquals(Id.create(lineId, TransitLine.class), line.getId(), "Transit line ids have to be the same");
	}
}
