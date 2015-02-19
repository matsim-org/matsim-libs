/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
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
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;


/**
 * This test is to show that qsim returns actualTravelTime+1 because of java rounding errors.
 * This is more predominant when flow capacity of link is not multiple of 3600 Veh/h.
 * In such scenarios, flowCapacityFraction is accumulated in every second and that's where
 * problem starts. For e.g. 0.2+0.1 = 0.30000000004 also 0.6+0.1=0.79999999999999999
 * See, nice article http://floating-point-gui.de/basic/
 * 
 * See small numerical test also
 * @author amit
 */

public class JavaRoundingErrorInQsim {

	@Test
	public void printDecimalSum(){
		double a = 0.1;
		double sum =0;
		double counter = 0;
		
		for(int i=0; i<10;i++){
			sum += a;
			counter++;
			System.out.println("Sum at counter "+counter+" is "+sum);	
		}
	}
	
	@Test
	public void test4WrongTravelTime () {
		// 2 cars depart on same time, central (bottleneck) link allow only 1 agent / 10 sec.
		PseudoInputs net = new PseudoInputs();
		net.createNetwork(360);
		net.createPopulation();

		Map<Id<Person>, Double> personLinkTravelTimes = new HashMap<Id<Person>, Double>();

		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(new PersonLinkTravelTimeEventHandler(personLinkTravelTimes));

		QSim qSim = createQSim(net,manager);
		qSim.run();

		//agent 2 is departed first so will have free speed time = 1000/25 +1 = 41 sec
		Assert.assertEquals( "Wrong travel time for on link 2 for person 2" , 41.0 , personLinkTravelTimes.get(Id.createPersonId(2))  , MatsimTestUtils.EPSILON);

		// agent 1 should have 1000/25 +1 + 10 = 51 but, it have 52 sec due to rounding errors in java
		Assert.assertEquals( "Wrong travel time for on link 2 for person 1" , 52.0 , personLinkTravelTimes.get(Id.createPersonId(1))  , MatsimTestUtils.EPSILON);
		Logger.getLogger(JavaRoundingErrorInQsim.class).warn("Although the test is passing instead of failing for person 1. This is done intentionally in order to keep this in mind for future.");
	}

	private QSim createQSim (PseudoInputs net, EventsManager manager){
		Scenario sc = net.scenario;
		QSim qSim1 = new QSim(sc, manager);
		ActivityEngine activityEngine = new ActivityEngine();
		qSim1.addMobsimEngine(activityEngine);
		qSim1.addActivityHandler(activityEngine);
		QNetsimEngine netsimEngine = new QNetsimEngine(qSim1);
		qSim1.addMobsimEngine(netsimEngine);
		qSim1.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim1.addMobsimEngine(teleportationEngine);
		QSim qSim = qSim1;
		AgentFactory agentFactory = new DefaultAgentFactory(qSim);
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
		qSim.addAgentSource(agentSource);
		return qSim;
	}

	private static class PersonLinkTravelTimeEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

		private final Map<Id<Person>, Double> personTravelTime;

		public PersonLinkTravelTimeEventHandler(Map<Id<Person>, Double> agentTravelTime) {
			this.personTravelTime = agentTravelTime;
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {

			Id<Person> personId = Id.createPersonId(event.getVehicleId());

			if( event.getLinkId().equals(Id.createLinkId(2))){
				personTravelTime.put(personId, - event.getTime());	
			}

		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Id<Person> personId = Id.createPersonId(event.getVehicleId());

			if( event.getLinkId().equals(Id.createLinkId(2)) ){
				personTravelTime.put(personId, personTravelTime.get(personId) + event.getTime());	
			}
		}

		@Override
		public void reset(int iteration) {
		}
	}

	private static final class PseudoInputs{

		final Scenario scenario ;
		NetworkImpl network;
		final Population population;
		Link link1;
		Link link2;
		Link link3;

		public PseudoInputs(){
			scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

			population = scenario.getPopulation();
		}

		private void createNetwork(double linkCapacity){

			network = (NetworkImpl) scenario.getNetwork();

			Node node1 = network.createAndAddNode(Id.create("1", Node.class), scenario.createCoord(-100.0,0.0));
			Node node2 = network.createAndAddNode(Id.create("2", Node.class), scenario.createCoord( 0.0,  0.0));
			Node node3 = network.createAndAddNode(Id.create("3", Node.class), scenario.createCoord( 0.0,1000.0));
			Node node4 = network.createAndAddNode(Id.create("4", Node.class), scenario.createCoord( 0.0,1100.0));

			link1 = network.createAndAddLink(Id.create("1", Link.class), node1, node2, 1000, 25, 7200, 1, null, "22"); 
			link2 = network.createAndAddLink(Id.create("2", Link.class), node2, node3, 1000, 25, linkCapacity, 1, null, "22");	
			link3 = network.createAndAddLink(Id.create("3", Link.class), node3, node4, 1000, 25, 7200, 1, null, "22");

		}

		private void createPopulation(){

			for(int i=1;i<3;i++){
				Id<Person> id = Id.createPersonId(i);
				Person p = population.getFactory().createPerson(id);
				Plan plan = population.getFactory().createPlan();
				p.addPlan(plan);
				Activity a1 = population.getFactory().createActivityFromLinkId("h", link1.getId());

				a1.setEndTime(0*3600);
				Leg leg = population.getFactory().createLeg(TransportMode.car);
				plan.addActivity(a1);
				plan.addLeg(leg);
				LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
				NetworkRoute route;
				List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
				route= (NetworkRoute) factory.createRoute(link1.getId(), link3.getId());
				linkIds.add(link2.getId());
				route.setLinkIds(link1.getId(), linkIds, link3.getId());
				leg.setRoute(route);

				Activity a2 = population.getFactory().createActivityFromLinkId("w", link3.getId());
				plan.addActivity(a2);
				population.addPerson(p);
			}
		}
	}
}
