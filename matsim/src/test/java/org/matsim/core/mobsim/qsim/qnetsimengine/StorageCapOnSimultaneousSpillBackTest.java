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
package org.matsim.core.mobsim.qsim.qnetsimengine;

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
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author amit
 */

public class StorageCapOnSimultaneousSpillBackTest {

	private final int numberOfPersonInPlan = 4;

	@Test
	public void storageCapTest4BottleneckLink (){
		/*
		 * agent 1 and 3 are departing on link 4 and agent 2 and 4 are departing on link 1. Agent 1, 2, 3, 4 are departing at an interval of 1 sec.
		 * The bottleneck link is 5 m long and thus can accomodate only one vehicle, flow cap allow one car/ 10 sec
		 * Thus, spill back occurs on both upstream links and when storage cap is available, one of the link is randomly chosen and one vehicle move to bottleneck link
		 * Importantly, if delay of the other vehicle on spill back upstream link is equal to (or more than) stuck time, irrespective of the space on bottleneck link
		 * second will also be forced to move on next link. 
		 * 
		 */
		MatsimRandom.reset();
		Tuple<Id<Link>, Id<Link>> startLinkIds = new Tuple<Id<Link>, Id<Link>>(Id.createLinkId(1), Id.createLinkId(4)); // agent 2,4 depart on link 1
		Map<Id<Person>, Tuple<Double,Double>> person2EnterTime = getPerson2LinkEnterTime(startLinkIds);

		Assert.assertEquals("Person 3 is entering on link 2 at wrong time.", 14.0, person2EnterTime.get(Id.createPersonId(3)).getFirst(),MatsimTestUtils.EPSILON);
		Assert.assertEquals("Person 3 is leaving from link 2 at wrong time.", 24.0, person2EnterTime.get(Id.createPersonId(3)).getSecond(),MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("Person 4 is entering on link 2 at wrong time.", 24.0, person2EnterTime.get(Id.createPersonId(4)).getFirst(),MatsimTestUtils.EPSILON);
		Assert.assertEquals("Person 4 is leaving from link 2 at wrong time.", 34.0, person2EnterTime.get(Id.createPersonId(4)).getSecond(),MatsimTestUtils.EPSILON);
		

		for(Id<Person> personId : person2EnterTime.keySet()){
			System.out.println("Person "+personId+ " is entering link 2 at time "+person2EnterTime.get(personId).getFirst() +" and leaving at time "+person2EnterTime.get(personId).getSecond());
		}

		//changing the links order such that agent 2,4 depart on link 4
		startLinkIds = new Tuple<Id<Link>, Id<Link>>(Id.createLinkId(4), Id.createLinkId(1));
		person2EnterTime = getPerson2LinkEnterTime(startLinkIds);

		Assert.assertEquals("Person 3 is entering on link 2 at wrong time.", 24.0, person2EnterTime.get(Id.createPersonId(3)).getFirst(),MatsimTestUtils.EPSILON);
		Assert.assertEquals("Person 3 is leaving from link 2 at wrong time.", 34.0, person2EnterTime.get(Id.createPersonId(3)).getSecond(),MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("Person 4 is entering on link 2 at wrong time.", 14.0, person2EnterTime.get(Id.createPersonId(4)).getFirst(),MatsimTestUtils.EPSILON);
		Assert.assertEquals("Person 4 is leaving from link 2 at wrong time.", 24.0, person2EnterTime.get(Id.createPersonId(4)).getSecond(),MatsimTestUtils.EPSILON);
		
	}

	private Map<Id<Person>, Tuple<Double,Double>> getPerson2LinkEnterTime(Tuple<Id<Link>, Id<Link>> startLinkIds){
		MergingNetworkAndPlans pseudoInputs = new MergingNetworkAndPlans();
		pseudoInputs.createNetwork();
		pseudoInputs.createPopulation(numberOfPersonInPlan, startLinkIds );
		Scenario sc = pseudoInputs.scenario;

		Map<Id<Person>, Tuple<Double,Double>> person2LinkEnterTime = new HashMap<>();

		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(new PersonLinkEnterLeaveTime(person2LinkEnterTime));
		QSim sim = createQSim(sc, events);
		sim.run();
		return person2LinkEnterTime;

	}

	private class PersonLinkEnterLeaveTime implements LinkEnterEventHandler, LinkLeaveEventHandler{

		Map<Id<Person>, Tuple<Double,Double>> personLinkEnterLeaveTime ;

		private PersonLinkEnterLeaveTime(Map<Id<Person>, Tuple<Double,Double>> person2LinkEnterTime){
			this.personLinkEnterLeaveTime = person2LinkEnterTime;
		}

		@Override
		public void reset(int iteration) {
			this.personLinkEnterLeaveTime.clear();
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			if(event.getLinkId().equals(Id.createLinkId(2))){
				Tuple<Double, Double> linkEnterTime = personLinkEnterLeaveTime.get(Id.createPersonId(event.getVehicleId()));
				personLinkEnterLeaveTime.put(Id.createPersonId(event.getVehicleId()), new Tuple<Double, Double>(linkEnterTime.getFirst(),event.getTime()));
			}
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			if(event.getLinkId().equals(Id.createLinkId(2))){
				personLinkEnterLeaveTime.put(Id.createPersonId(event.getVehicleId()), new Tuple<Double, Double>(event.getTime(), 0.));
			}
		}


	}


	private QSim createQSim (Scenario sc, EventsManager manager){
		QSim qSim1 = new QSim(sc, manager);
		ActivityEngine activityEngine = new ActivityEngine(manager, qSim1.getAgentCounter());
		qSim1.addMobsimEngine(activityEngine);
		qSim1.addActivityHandler(activityEngine);

		QNetsimEngine netsimEngine = new QNetsimEngine(qSim1);
		qSim1.addMobsimEngine(netsimEngine);
		qSim1.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine(sc, manager);
		qSim1.addMobsimEngine(teleportationEngine);
		QSim qSim = qSim1;
		AgentFactory agentFactory = new DefaultAgentFactory(qSim);
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);

		Map<String, VehicleType> modeVehicleTypes = new HashMap<String, VehicleType>();

		VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("car", VehicleType.class));
		car.setMaximumVelocity(20);
		car.setPcuEquivalents(1.0);
		modeVehicleTypes.put("car", car);
		agentSource.setModeVehicleTypes(modeVehicleTypes);
		qSim.addAgentSource(agentSource);
		return qSim;
	}


	private class MergingNetworkAndPlans {
		/**
		 * generates network with 3 links. 
		 *<p>			
		 *<p>  o--1---o---2---o---3---o
		 *<p> 		  |
		 *<p> 		  4
		 *<p> 		  |
		 *<p> 		  o	
		 *<p>				  
		 */
		Scenario scenario;
		Config config;
		NetworkImpl network;
		Population population;
		Link link1;
		Link link2;
		Link link3;
		Link link4;

		private MergingNetworkAndPlans(){
			config=ConfigUtils.createConfig();
			config.qsim().setStuckTime(3600.);
			this.scenario = ScenarioUtils.loadScenario(config);
			network =  (NetworkImpl) this.scenario.getNetwork();
			population = this.scenario.getPopulation();
		}

		private void createNetwork(){

			Node node1 = network.createAndAddNode(Id.createNodeId("1"), this.scenario.createCoord(0, 0)) ;
			Node node2 = network.createAndAddNode(Id.createNodeId("2"), this.scenario.createCoord(100, 10));
			Node node3 = network.createAndAddNode(Id.createNodeId("3"), this.scenario.createCoord(300, -10));
			Node node4 = network.createAndAddNode(Id.createNodeId("4"), this.scenario.createCoord(500, 20));
			Node node5 = network.createAndAddNode(Id.createNodeId("5"), this.scenario.createCoord(-10, -200));

			link1 = network.createAndAddLink(Id.createLinkId(String.valueOf("1")), node1, node2,1000.0,20.0,3600.,1,null,"7");
			link2 = network.createAndAddLink(Id.createLinkId(String.valueOf("2")), node2, node3,5.0,20.0,360.,1,null,"7");
			link3 = network.createAndAddLink(Id.createLinkId(String.valueOf("3")), node3, node4,1000.0,20.0,3600.,1,null,"7");
			link4 = network.createAndAddLink(Id.createLinkId(String.valueOf("4")), node5, node2,1000.0,20.0,3600.,1,null,"7");
		}

		private void createPopulation(int numberOfPersons, Tuple<Id<Link>, Id<Link>> startLinkIds){

			/*Alternative persons from different links*/

			for(int i=1;i<=numberOfPersons;i++){
				Id<Person> id = Id.createPersonId(i);
				Person p = population.getFactory().createPerson(id);
				Plan plan = population.getFactory().createPlan();
				p.addPlan(plan);
				Activity a1;

				Id<Link> startLinkId ;
				if (i%2==0) startLinkId = startLinkIds.getFirst();
				else startLinkId = startLinkIds.getSecond();

				a1 = population.getFactory().createActivityFromLinkId("h", startLinkId);
				a1.setEndTime(0*3600+i);
				Leg leg = population.getFactory().createLeg(TransportMode.car);
				plan.addActivity(a1);
				plan.addLeg(leg);
				LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
				NetworkRoute route;
				List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
				route= (NetworkRoute) factory.createRoute(startLinkId, link3.getId());
				linkIds.add(link2.getId());
				linkIds.add(link3.getId());
				route.setLinkIds(startLinkId, linkIds, link3.getId());
				leg.setRoute(route);

				Activity a2 = population.getFactory().createActivityFromLinkId("w", link3.getId());
				plan.addActivity(a2);
				population.addPerson(p);
			}
		}
	}

}
