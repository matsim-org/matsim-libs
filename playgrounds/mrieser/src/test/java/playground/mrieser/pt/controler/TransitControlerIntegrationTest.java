/* *********************************************************************** *
 * project: org.matsim.*
 * TransitControlerIntegrationTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mrieser.pt.controler;

import java.util.ArrayList;
import java.util.Collections;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleFactory;
import org.matsim.transitSchedule.api.TransitStopFacility;

public class TransitControlerIntegrationTest extends MatsimTestCase {

	public void testTransitRouteCopy() {
		Config config = super.loadConfig(null);
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		ScenarioImpl scenario = new ScenarioImpl(config);

		Id id1 = scenario.createId("1");
		Id id2 = scenario.createId("2");
		Id id3 = scenario.createId("3");

		// build network
		Network network = scenario.getNetwork();
		NetworkFactory nBuilder = network.getFactory();
		Node node1 = nBuilder.createNode(id1, scenario.createCoord(0, 0));
		Node node2 = nBuilder.createNode(id2, scenario.createCoord(1000, 0));
		Node node3 = nBuilder.createNode(id3, scenario.createCoord(2000, 0));
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		Link link1 = nBuilder.createLink(id1, id1, id2);
		Link link2 = nBuilder.createLink(id2, id2, id3);
		network.addLink(link1);
		network.addLink(link2);

		// build schedule
		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory sBuilder = schedule.getFactory();

		TransitStopFacility stopF1 = sBuilder.createTransitStopFacility(id1, scenario.createCoord(1000.0, 0), false);
		TransitStopFacility stopF2 = sBuilder.createTransitStopFacility(id2, scenario.createCoord(2000.0, 0), false);
		stopF1.setLinkId(link1.getId());
		stopF2.setLinkId(link2.getId());
		schedule.addStopFacility(stopF1);
		schedule.addStopFacility(stopF2);

		TransitLine tLine1 = sBuilder.createTransitLine(id1);

		TransitRouteStop stop1 = sBuilder.createTransitRouteStop(stopF1, 0, 0);
		TransitRouteStop stop2 = sBuilder.createTransitRouteStop(stopF2, 100, 100);
		ArrayList<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		stops.add(stop1);
		stops.add(stop2);

		NetworkRoute netRoute = new LinkNetworkRouteImpl(link1.getId(), link2.getId());
		netRoute.setLinkIds(link1.getId(), Collections.<Id>emptyList(), link2.getId());
		TransitRoute tRoute1 = sBuilder.createTransitRoute(id1, netRoute, stops, "bus");

		tRoute1.addDeparture(sBuilder.createDeparture(id1, 7.0*3600));
		tLine1.addRoute(tRoute1);
		schedule.addTransitLine(tLine1);

		// build vehicles
		new CreateVehiclesForSchedule(schedule, scenario.getVehicles()).run();

		// build population
		Population population = scenario.getPopulation();
		PopulationFactory pBuilder = population.getFactory();
		Person person1 = pBuilder.createPerson(id1);
		Plan plan = pBuilder.createPlan();
		Activity homeAct = pBuilder.createActivityFromLinkId("h", id1);
		homeAct.setEndTime(7.0*3600);
		plan.addActivity(homeAct);
		Leg leg = pBuilder.createLeg(TransportMode.pt);
		ExperimentalTransitRoute tRoute = new ExperimentalTransitRoute(stopF1, tLine1, tRoute1, stopF2);
		leg.setRoute(tRoute);
		plan.addLeg(leg);
		plan.addActivity(pBuilder.createActivityFromLinkId("w", id2));
		person1.addPlan(plan);
		population.addPerson(person1);

		// prepare config
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(1);

		config.simulation().setEndTime(8.0 * 3600);

		ActivityParams params = new ActivityParams("h");
		params.setTypicalDuration(16.0*3600);
		config.charyparNagelScoring().addActivityParams(params);
		params = new ActivityParams("w");
		params.setTypicalDuration(8.0*3600);
		config.charyparNagelScoring().addActivityParams(params);

		StrategySettings tam = new StrategySettings(new IdImpl(1));
		tam.setModuleName("TimeAllocationMutator");
		tam.setProbability(1.0);
		config.strategy().addStrategySettings(tam);

		// run
		TransitControler controler = new TransitControler(scenario);
		controler.setWriteEventsInterval(0);
		controler.setCreateGraphs(false);
		controler.run();

		// checks
		assertEquals(1, population.getPersons().size());
		assertEquals(2, person1.getPlans().size());
		assertEquals(ExperimentalTransitRoute.class, ((Leg) person1.getPlans().get(0).getPlanElements().get(1)).getRoute().getClass());
		assertEquals(ExperimentalTransitRoute.class, ((Leg) person1.getPlans().get(1).getPlanElements().get(1)).getRoute().getClass());
	}
}
