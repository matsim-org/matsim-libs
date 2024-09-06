/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.scoring.routeDesignScoring;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.PConfigGroup.RouteDesignScoreParams;
import org.matsim.contrib.minibus.PConfigGroup.RouteDesignScoreParams.StopListToEvaluate;
import org.matsim.contrib.minibus.hook.PPlan;
import org.matsim.contrib.minibus.scoring.routeDesignScoring.RouteDesignScoringManager.RouteDesignScoreFunctionName;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 *
 * Tests {@link StopServedMultipleTimesPenalty}
 *
 * @author gleich
 *
 */
public class StopServedMultipleTimesPenaltyTest {

	Scenario scenario;
	TransitScheduleFactory factory;

	@BeforeEach
	public void setUp() {
		scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		factory = scenario.getTransitSchedule().getFactory();
	}

	@Test
	void testRouteServingSameStopTwice() {
		ArrayList<TransitStopFacility> stopsToBeServed = new ArrayList<>();
		ArrayList<TransitRouteStop> stops = new ArrayList<>();

		stopsToBeServed.add(getOrCreateStopAtCoord(0, 0));
		stopsToBeServed.add(getOrCreateStopAtCoord(10, 0));

		stops.add(factory.createTransitRouteStop(getOrCreateStopAtCoord(0, 0), 0, 0));
		stops.add(factory.createTransitRouteStop(getOrCreateStopAtCoord(10, 0), 0, 0));
		stops.add(factory.createTransitRouteStop(getOrCreateStopAtCoord(10, 10), 0, 0));
		stops.add(factory.createTransitRouteStop(getOrCreateStopAtCoord(0, 0), 0, 0));

		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(Id.createLinkId("dummy1.1"), Id.createLinkId("dummy1.2"));

		PPlan pPlan1 = new PPlan(Id.create("PPlan1", PPlan.class), "creator1", Id.create("PPlanParent1", PPlan.class));
		pPlan1.setStopsToBeServed(stopsToBeServed);
		TransitLine line1 = factory.createTransitLine(Id.create("line1", TransitLine.class));
		TransitRoute route1 = factory.createTransitRoute(Id.create("TransitRoute1", TransitRoute.class), route, stops, "bus");
		line1.addRoute(route1);
		pPlan1.setLine(line1);

		/* option StopListToEvaluate.transitRouteAllStops */
		PConfigGroup pConfig = new PConfigGroup();
		RouteDesignScoreParams params = new RouteDesignScoreParams();
		params.setRouteDesignScoreFunction(RouteDesignScoreFunctionName.stopServedMultipleTimesPenalty);
		params.setCostFactor(-1);
		params.setStopListToEvaluate(StopListToEvaluate.transitRouteAllStops);
		params.setValueToStartScoring(1);
		pConfig.addRouteDesignScoreParams(params);

		StopServedMultipleTimesPenalty penalty = new StopServedMultipleTimesPenalty(params);
		double actual = penalty.getScore(pPlan1, route1);
		// 4 stops served, but only 3 different stop ids
		double expected = -1 * ((4.0 / 3) - 1);
		Assertions.assertEquals(expected, actual, 0.001);
	}

	private TransitStopFacility getOrCreateStopAtCoord(int x, int y) {
		Id<TransitStopFacility> stopId = getStopId(x, y);
		if (scenario.getTransitSchedule().getFacilities().containsKey(stopId)) {
			return scenario.getTransitSchedule().getFacilities().get(stopId);
		} else {
			return factory.createTransitStopFacility(
					stopId, CoordUtils.createCoord(x, y), false);
		}
	}

	private Id<TransitStopFacility> getStopId(int x, int y) {
		return Id.create(x + "," + y, TransitStopFacility.class);
	}
}
