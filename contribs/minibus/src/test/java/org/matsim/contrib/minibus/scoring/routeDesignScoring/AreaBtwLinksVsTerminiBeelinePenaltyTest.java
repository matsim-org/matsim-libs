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
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.PConfigGroup.RouteDesignScoreParams;
import org.matsim.contrib.minibus.PConfigGroup.RouteDesignScoreParams.StopListToEvaluate;
import org.matsim.contrib.minibus.hook.PPlan;
import org.matsim.contrib.minibus.scoring.routeDesignScoring.RouteDesignScoringManager.RouteDesignScoreFunctionName;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
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
 * Tests {@link AreaBtwLinksVsTerminiBeelinePenalty}.
 *
 * @author gleich
 *
 */
public class AreaBtwLinksVsTerminiBeelinePenaltyTest {

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

		stops.add(factory.createTransitRouteStop(getOrCreateStopAtCoord(0, 0), 0, 0));
		stops.add(factory.createTransitRouteStop(getOrCreateStopAtCoord(10, 0), 0, 0));
		stops.add(factory.createTransitRouteStop(getOrCreateStopAtCoord(10, 10), 0, 0));

		// Nodes a-d are passed by TransitRoute, Node e not
		Id<Node> idNodeA = Id.createNodeId("a");
		Id<Node> idNodeB = Id.createNodeId("b");
		Id<Node> idNodeC = Id.createNodeId("c");
		Id<Node> idNodeD = Id.createNodeId("d");
		Id<Node> idNodeE = Id.createNodeId("e");
		NetworkUtils.createAndAddNode(scenario.getNetwork(), idNodeA, CoordUtils.createCoord(0, 0));
		NetworkUtils.createAndAddNode(scenario.getNetwork(), idNodeB, CoordUtils.createCoord(10, 0));
		NetworkUtils.createAndAddNode(scenario.getNetwork(), idNodeC, CoordUtils.createCoord(10, 10));
		NetworkUtils.createAndAddNode(scenario.getNetwork(), idNodeD, CoordUtils.createCoord(0, 10));
		NetworkUtils.createAndAddNode(scenario.getNetwork(), idNodeE, CoordUtils.createCoord(5, 5));

		Id<Link> idLinkAB = Id.createLinkId("a>b");
		Id<Link> idLinkBC = Id.createLinkId("b>c");
		Id<Link> idLinkCD = Id.createLinkId("c>d");
		Id<Link> idLinkDA = Id.createLinkId("d>a");
		Id<Link> idLinkCE = Id.createLinkId("c>e");
		Id<Link> idLinkEA = Id.createLinkId("e>a");
		NetworkUtils.createAndAddLink(scenario.getNetwork(), idLinkAB, scenario.getNetwork().getNodes().get(idNodeA),
				scenario.getNetwork().getNodes().get(idNodeB), 10.0, 1.0, 1.0, 1.0, "", "");
		NetworkUtils.createAndAddLink(scenario.getNetwork(), idLinkBC, scenario.getNetwork().getNodes().get(idNodeB),
				scenario.getNetwork().getNodes().get(idNodeC), 10.0, 1.0, 1.0, 1.0, "", "");
		NetworkUtils.createAndAddLink(scenario.getNetwork(), idLinkCD, scenario.getNetwork().getNodes().get(idNodeC),
				scenario.getNetwork().getNodes().get(idNodeD), 10.0, 1.0, 1.0, 1.0, "", "");
		NetworkUtils.createAndAddLink(scenario.getNetwork(), idLinkDA, scenario.getNetwork().getNodes().get(idNodeD),
				scenario.getNetwork().getNodes().get(idNodeA), 10.0, 1.0, 1.0, 1.0, "", "");
		NetworkUtils.createAndAddLink(scenario.getNetwork(), idLinkCE, scenario.getNetwork().getNodes().get(idNodeC),
				scenario.getNetwork().getNodes().get(idNodeE), 10.0, 1.0, 1.0, 1.0, "", "");
		NetworkUtils.createAndAddLink(scenario.getNetwork(), idLinkEA, scenario.getNetwork().getNodes().get(idNodeE),
				scenario.getNetwork().getNodes().get(idNodeA), 10.0, 1.0, 1.0, 1.0, "", "");

		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(idLinkAB, idLinkDA);
		List<Id<Link>> linkIds = new ArrayList<>();
		linkIds.add(idLinkBC);
		linkIds.add(idLinkCD);
		route.setLinkIds(idLinkAB, linkIds, idLinkDA);

		PPlan pPlan1 = new PPlan(Id.create("PPlan1", PPlan.class), "creator1", Id.create("PPlanParent1", PPlan.class));
		pPlan1.setStopsToBeServed(stopsToBeServed);
		TransitLine line1 = factory.createTransitLine(Id.create("line1", TransitLine.class));
		TransitRoute route1 = factory.createTransitRoute(Id.create("TransitRoute1", TransitRoute.class), route, stops, "bus");
		line1.addRoute(route1);
		pPlan1.setLine(line1);

		/* option StopListToEvaluate.transitRouteAllStops */
		PConfigGroup pConfig = new PConfigGroup();
		RouteDesignScoreParams params = new RouteDesignScoreParams();
		params.setRouteDesignScoreFunction(RouteDesignScoreFunctionName.areaBtwLinksVsBeelinePenalty);
		params.setCostFactor(-1);
		params.setStopListToEvaluate(StopListToEvaluate.transitRouteAllStops);
		params.setValueToStartScoring(1);
		pConfig.addRouteDesignScoreParams(params);

		AreaBtwLinksVsTerminiBeelinePenalty penalty = new AreaBtwLinksVsTerminiBeelinePenalty(params, scenario.getNetwork());
		double actual = penalty.getScore(pPlan1, route1);
		// area 10 x 10 = 100; beeline termini ({0,0}, {10,0}) = 10
		double expected = -1 * ((10.0 * 10.0 / 10.0) - 1);
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
