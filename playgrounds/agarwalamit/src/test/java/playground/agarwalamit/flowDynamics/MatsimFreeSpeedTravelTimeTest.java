/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
package playground.agarwalamit.flowDynamics;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author amit
 */

public class MatsimFreeSpeedTravelTimeTest {
	@Test
	public void testFreeSpeedTime(){
		// pass whole (or natural) number as link speed (in m/s), this will return travelTime + 1(matsim sec).
		double travelTime = runAndGetTravelTime(20); // --> 1000/20 + 1 = 51
		Assert.assertEquals("Wrong free speed travel time.", 51, travelTime, MatsimTestUtils.EPSILON);

		// now, pass the speed (rational number) in kph 
		travelTime = runAndGetTravelTime(60/3.6); //--> 1000*3.6/60 = 60
		Assert.assertEquals("Wrong free speed travel time.", 60, travelTime, MatsimTestUtils.EPSILON);
		
		/*
		 * previously (before Jan'16), the later was returning 61.0 due to java double precision.
		 * for e.g. at timeStep = 100
		 * previously, it was -->  Math.floor( currentTimeStep + earliestLinkTime ) + 1 = 161
		 * now --> currentTimeStep + Math.floor(earliestLinkTime) + 1 = 160
		 * This will eliminate some of the errors in the post processing.
		 */
		System.out.println( Math.floor( 100+1000/(60/3.6) ) + 1 );
		System.out.println( 100 + Math.floor(1000/(60/3.6)) + 1);
	}

	private double runAndGetTravelTime(double maxLinkSpeed){ // returns travel time on link 2 only.
		SimpleNetwork net = new SimpleNetwork(maxLinkSpeed);
		EventsManager manager = EventsUtils.createEventsManager();
		TravelTimeHandler tth = new TravelTimeHandler();
		manager.addHandler(tth);
		QSim qsim = QSimUtils.createDefaultQSim(net.scenario, manager);
		qsim.run();
		return tth.travelTime;
	}

	private static final class TravelTimeHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {
		double travelTime = 0;
		@Override
		public void reset(int iteration) {}
		@Override
		public void handleEvent(LinkLeaveEvent event) {
			if(event.getLinkId().toString().equals("2")) {
				travelTime += event.getTime();
			}
		}
		@Override
		public void handleEvent(LinkEnterEvent event) {
			if(event.getLinkId().toString().equals("2")) {
				travelTime -= event.getTime();	
			}
		}
	}

	private static final class SimpleNetwork{

		final Config config;
		final Scenario scenario ;
		final NetworkImpl network;
		final Population population;
		final Link link1;
		final Link link2;
		final Link link3;

		public SimpleNetwork(double maxLinkSpeed){

			scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			config = scenario.getConfig();
			config.qsim().setFlowCapFactor(1.0);
			config.qsim().setStorageCapFactor(1.0);

			network = (NetworkImpl) scenario.getNetwork();

			Node node1 = network.createAndAddNode(Id.createNodeId("1"), new Coord(-100., -100.0));
			Node node2 = network.createAndAddNode(Id.createNodeId("2"), new Coord(0.0, 0.0));
			Node node3 = network.createAndAddNode(Id.createNodeId("3"), new Coord(1000.0, 0.0));
			Node node4 = network.createAndAddNode(Id.createNodeId("4"), new Coord(1000.0, 100.0));

			Set<String> allowedModes = new HashSet<String>(); allowedModes.addAll(Arrays.asList(TransportMode.car,TransportMode.walk));

			link1 = network.createAndAddLink(Id.createLinkId("1"), node1, node2, 1000, 25, 3600, 1, null, "22"); 
			link2 = network.createAndAddLink(Id.createLinkId("2"), node2, node3, 1000, maxLinkSpeed, 3600, 1, null, "22");	//flow capacity is 1 PCU per min.
			link3 = network.createAndAddLink(Id.createLinkId("3"), node3, node4, 1000, 25, 3600, 1, null, "22");

			population = scenario.getPopulation();

			Id<Person> id = Id.createPersonId(0);
			Person p = population.getFactory().createPerson(id);
			Plan plan = population.getFactory().createPlan();
			p.addPlan(plan);
			Activity a1 = population.getFactory().createActivityFromLinkId("h", link1.getId());
			Leg leg  = population.getFactory().createLeg(TransportMode.car);
			a1.setEndTime(99);

			plan.addActivity(a1);
			plan.addLeg(leg);
			LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
			NetworkRoute route = (NetworkRoute) factory.createRoute(link1.getId(), link3.getId());
			route.setLinkIds(link1.getId(), Arrays.asList(link2.getId()), link3.getId());
			leg.setRoute(route);
			Activity a2 = population.getFactory().createActivityFromLinkId("w", link3.getId());
			plan.addActivity(a2);
			population.addPerson(p);
		}
	}
}