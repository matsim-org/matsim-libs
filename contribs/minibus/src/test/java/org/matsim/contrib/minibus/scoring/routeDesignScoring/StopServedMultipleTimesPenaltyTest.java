package org.matsim.contrib.minibus.scoring.routeDesignScoring;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.PConfigGroup.RouteDesignScoreParams;
import org.matsim.contrib.minibus.PConfigGroup.RouteDesignScoreParams.StopListToEvaluate;
import org.matsim.contrib.minibus.operator.PPlan;
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

public class StopServedMultipleTimesPenaltyTest {
	
	Scenario scenario;
	TransitScheduleFactory factory;
	
	@Before
	public void setUp() {
		scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		factory = scenario.getTransitSchedule().getFactory();
	}
	
	@Test
	public void testRouteServingSameStopTwice() {
		ArrayList<TransitStopFacility> stopsToBeServed = new ArrayList<>();
		ArrayList<TransitRouteStop> stops = new ArrayList<>();
		
		stopsToBeServed.add(getOrCreateStopAtCoord(0, 0));
		stopsToBeServed.add(getOrCreateStopAtCoord(10, 0));
		
		stops.add(factory.createTransitRouteStop(getOrCreateStopAtCoord(0, 0), 0, 0));
		stops.add(factory.createTransitRouteStop(getOrCreateStopAtCoord(10, 0), 0, 0));
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
		// 3 stops served, but only 2 different stop ids
		double expected = -1 * ((3 / 2) - 1);
		Assert.assertEquals(expected, actual, 0.001);
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
