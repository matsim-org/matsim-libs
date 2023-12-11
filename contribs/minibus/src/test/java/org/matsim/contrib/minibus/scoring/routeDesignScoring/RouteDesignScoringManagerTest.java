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
 * Tests {@link RouteDesignScoringManager},
 * {@link AreaBtwStopsVsTerminiBeelinePenalty},
 * {@link Stop2StopVsTerminiBeelinePenalty}.
 *
 * The latter two are tested here, because they are also used as two example
 * RouteDesignScoringFunctions managed by RouteDesignScoringManager.
 *
 * @author gleich
 *
 */
public class RouteDesignScoringManagerTest {

	Scenario scenario;
	TransitScheduleFactory factory;

	@BeforeEach
	public void setUp() {
		scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		factory = scenario.getTransitSchedule().getFactory();
	}

	@Test
	void testRectangularLine() {
		ArrayList<TransitStopFacility> stopsToBeServed = new ArrayList<>();
		ArrayList<TransitRouteStop> stops = new ArrayList<>();

		stopsToBeServed.add(getOrCreateStopAtCoord(0, 0));
		stopsToBeServed.add(getOrCreateStopAtCoord(10, 0));
		stopsToBeServed.add(getOrCreateStopAtCoord(10, 10));
		stopsToBeServed.add(getOrCreateStopAtCoord(0, 10));

		stops.add(factory.createTransitRouteStop(getOrCreateStopAtCoord(0, 0), 0, 0));
		stops.add(factory.createTransitRouteStop(getOrCreateStopAtCoord(10, 0), 0, 0));
		stops.add(factory.createTransitRouteStop(getOrCreateStopAtCoord(10, 10), 0, 0));
		stops.add(factory.createTransitRouteStop(getOrCreateStopAtCoord(0, 10), 0, 0));
		stops.add(factory.createTransitRouteStop(getOrCreateStopAtCoord(-10, 10), 0, 0));
		stops.add(factory.createTransitRouteStop(getOrCreateStopAtCoord(-10, 0), 0, 0));

		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(Id.createLinkId("dummy1.1"), Id.createLinkId("dummy1.2"));

		PPlan pPlan1 = new PPlan(Id.create("PPlan1", PPlan.class), "creator1", Id.create("PPlanParent1", PPlan.class));
		pPlan1.setStopsToBeServed(stopsToBeServed);
		TransitLine line1 = factory.createTransitLine(Id.create("line1", TransitLine.class));
		TransitRoute route1 = factory.createTransitRoute(Id.create("TransitRoute1", TransitRoute.class), route, stops, "bus");
		line1.addRoute(route1);
		pPlan1.setLine(line1);

		/* stop2StopVsBeelinePenalty */
		/* option StopListToEvaluate.transitRouteAllStops */
		PConfigGroup pConfig = new PConfigGroup();
		RouteDesignScoreParams stop2stopVsBeeline = new RouteDesignScoreParams();
		stop2stopVsBeeline.setRouteDesignScoreFunction(RouteDesignScoreFunctionName.stop2StopVsBeelinePenalty);
		stop2stopVsBeeline.setCostFactor(-1);
		stop2stopVsBeeline.setStopListToEvaluate(StopListToEvaluate.transitRouteAllStops);
		stop2stopVsBeeline.setValueToStartScoring(1);
		pConfig.addRouteDesignScoreParams(stop2stopVsBeeline);

		RouteDesignScoringManager manager1 = new RouteDesignScoringManager();
		manager1.init(pConfig, scenario.getNetwork());
		double actual = manager1.scoreRouteDesign(pPlan1);
		// 6 stop->stop distances of 10 units each in the stops (not stopsToBeServed)
		double expected = -1 * ((6 * 10 / (10 * Math.sqrt(2))) - 1);
		Assertions.assertEquals(expected, actual, 0.001);

		/* option StopListToEvaluate.pPlanStopsToBeServed */
		stop2stopVsBeeline.setStopListToEvaluate(StopListToEvaluate.pPlanStopsToBeServed);

		manager1 = new RouteDesignScoringManager();
		manager1.init(pConfig, scenario.getNetwork());
		actual = manager1.scoreRouteDesign(pPlan1);
		// 4 stop->stop distances of 10 units each in the stops
		expected = -1 * ((4 * 10 / (10 * Math.sqrt(2))) - 1);
		Assertions.assertEquals(expected, actual, 0.001);

		pConfig.removeRouteDesignScoreParams(RouteDesignScoreFunctionName.stop2StopVsBeelinePenalty);

		/* areaVsBeelinePenalty */
		/* option StopListToEvaluate.transitRouteAllStops */
		RouteDesignScoreParams areaVsBeeline = new RouteDesignScoreParams();
		areaVsBeeline.setRouteDesignScoreFunction(RouteDesignScoreFunctionName.areaBtwStopsVsBeelinePenalty);
		areaVsBeeline.setCostFactor(-1);
		areaVsBeeline.setStopListToEvaluate(StopListToEvaluate.transitRouteAllStops);
		areaVsBeeline.setValueToStartScoring(1);
		pConfig.addRouteDesignScoreParams(areaVsBeeline);

		manager1 = new RouteDesignScoringManager();
		manager1.init(pConfig, scenario.getNetwork());
		actual = manager1.scoreRouteDesign(pPlan1);
		// x=[-10,10], y=[0,10] -> 20 X 10
		expected = -1 * ((20 * 10 / (10 * Math.sqrt(2))) - 1);
		Assertions.assertEquals(expected, actual, 0.001);

		/* option StopListToEvaluate.pPlanStopsToBeServed */
		areaVsBeeline.setStopListToEvaluate(StopListToEvaluate.pPlanStopsToBeServed);

		manager1 = new RouteDesignScoringManager();
		manager1.init(pConfig, scenario.getNetwork());
		actual = manager1.scoreRouteDesign(pPlan1);
		// x=[0,10], y=[0,10] -> 10 X 10
		expected = -1 * ((10 * 10 / (10 * Math.sqrt(2))) - 1);
		Assertions.assertEquals(expected, actual, 0.001);

		/* check summing up of both */
		pConfig.addRouteDesignScoreParams(stop2stopVsBeeline);

		manager1 = new RouteDesignScoringManager();
		manager1.init(pConfig, scenario.getNetwork());
		actual = manager1.scoreRouteDesign(pPlan1);
		// x=[0,10], y=[0,10] -> 10 X 10 ; 4 stop->stop distances of 10 units each in the stops
		expected = -1 * ((10 * 10 / (10 * Math.sqrt(2))) - 1) + (-1) * ((4 * 10 / (10 * Math.sqrt(2))) - 1);
		Assertions.assertEquals(expected, actual, 0.001);

		/* Check route with only two stops */
		stopsToBeServed = new ArrayList<>();
		stops = new ArrayList<>();

		stopsToBeServed.add(getOrCreateStopAtCoord(0, 0));
		stopsToBeServed.add(getOrCreateStopAtCoord(10, 0));

		stops.add(factory.createTransitRouteStop(getOrCreateStopAtCoord(0, 0), 0, 0));
		stops.add(factory.createTransitRouteStop(getOrCreateStopAtCoord(10, 0), 0, 0));

		route = RouteUtils.createLinkNetworkRouteImpl(Id.createLinkId("dummy2.1"), Id.createLinkId("dummy2.2"));

		PPlan pPlan2 = new PPlan(Id.create("PPlan2", PPlan.class), "creator2", Id.create("PPlanParent2", PPlan.class));
		pPlan2.setStopsToBeServed(stopsToBeServed);
		TransitLine line2 = factory.createTransitLine(Id.create("line2", TransitLine.class));
		TransitRoute route2 = factory.createTransitRoute(Id.create("TransitRoute2", TransitRoute.class), route, stops, "bus");
		line2.addRoute(route2);
		pPlan2.setLine(line2);

		manager1 = new RouteDesignScoringManager();
		manager1.init(pConfig, scenario.getNetwork());
		actual = manager1.scoreRouteDesign(pPlan2);
		// would be positive
		expected = -1;
		Assertions.assertEquals(expected, actual, 0.001);

		/* check that no subsidy emerges (no positive route design score) */
		// high valueToStartScoring -> all scores below would be positive, check that they are capped at 0
		stop2stopVsBeeline.setValueToStartScoring(10);
		areaVsBeeline.setValueToStartScoring(10);

		// add a third stop
		stops.add(factory.createTransitRouteStop(getOrCreateStopAtCoord(5, 0), 0, 0));

		route = RouteUtils.createLinkNetworkRouteImpl(Id.createLinkId("dummy2.1"), Id.createLinkId("dummy2.2"));

		PPlan pPlan3 = new PPlan(Id.create("PPlan3", PPlan.class), "creator3", Id.create("PPlanParent3", PPlan.class));
		pPlan3.setStopsToBeServed(stopsToBeServed);
		TransitLine line3 = factory.createTransitLine(Id.create("line3", TransitLine.class));
		TransitRoute route3 = factory.createTransitRoute(Id.create("TransitRoute3", TransitRoute.class), route, stops, "bus");
		line3.addRoute(route3);
		pPlan3.setLine(line3);

		manager1 = new RouteDesignScoringManager();
		manager1.init(pConfig, scenario.getNetwork());
		actual = manager1.scoreRouteDesign(pPlan3);
		// would be positive
		expected = 0;
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
