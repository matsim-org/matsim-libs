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
package playground.vsp.congestion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
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
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import playground.vsp.congestion.events.CongestionEvent;
import playground.vsp.congestion.handlers.CongestionEventHandler;
import playground.vsp.congestion.handlers.CongestionHandlerImplV4;
import playground.vsp.congestion.handlers.CongestionHandlerImplV6;

/**
 * @author amit
 */

public class MarginalCongestionPricingTest {
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	public final void implV6Test(){

		int numberOfPersonInPlan = 10;
		createPseudoInputs pseudoInputs = new createPseudoInputs();
		pseudoInputs.createNetwork();
		pseudoInputs.createPopulation(numberOfPersonInPlan);
		Scenario sc = pseudoInputs.scenario;

		EventsManager events = EventsUtils.createEventsManager();

		final List<CongestionEvent> congestionEvents = new ArrayList<CongestionEvent>();

		events.addHandler( new CongestionEventHandler() {

			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(CongestionEvent event) {
				congestionEvents.add(event);
			}

		});

		events.addHandler(new CongestionHandlerImplV6(events, (ScenarioImpl) sc));

		QSim sim = createQSim(sc, events);
		sim.run();

		Assert.assertEquals("wrong number of congestion events" , 8, congestionEvents.size());

		Set<String> affectedPersons = new HashSet<>();
		Set<Integer> causingPersons = new HashSet<>();
		int link2Delays=0;
		int link3Delays=0;
		double person6Delay=0;
		double person8Delay=0;
		double repetationPerson6Count=0;
		double repetationPerson8Count=0;

		for (CongestionEvent event : congestionEvents) {

			affectedPersons.add(event.getAffectedAgentId().toString());
			causingPersons.add(Integer.valueOf(event.getCausingAgentId().toString()));

			if(event.getLinkId().equals(Id.createLinkId("3"))){

				if (event.getCausingAgentId().toString().equals("0") && event.getAffectedAgentId().toString().equals("2")) {
					Assert.assertEquals("wrong delay.", 9, event.getDelay(), MatsimTestUtils.EPSILON);
					link3Delays++;
				} else if (event.getCausingAgentId().toString().equals("2") && event.getAffectedAgentId().toString().equals("4")) {
					Assert.assertEquals("wrong delay.", 17, event.getDelay(), MatsimTestUtils.EPSILON);
					link3Delays++;
				} else if (event.getCausingAgentId().toString().equals("4") && event.getAffectedAgentId().toString().equals("6")) {
					// delays 6, 19
					person6Delay+=event.getDelay();
					if(repetationPerson6Count>0){
						Assert.assertEquals("wrong delay.", 25, person6Delay, MatsimTestUtils.EPSILON);	
					}
					repetationPerson6Count++;
					link3Delays++;
				} else if (event.getCausingAgentId().toString().equals("4") && event.getAffectedAgentId().toString().equals("6")) {
					Assert.assertEquals("wrong delay.", 6, event.getDelay(), MatsimTestUtils.EPSILON);
					link3Delays++;
				} else if (event.getCausingAgentId().toString().equals("6") && event.getAffectedAgentId().toString().equals("8")) {
					// here delays are 14 and 19
					person8Delay+=event.getDelay();
					if(repetationPerson8Count>0){
						Assert.assertEquals("wrong delay.", 33, person8Delay, MatsimTestUtils.EPSILON);	
					}
					repetationPerson8Count++;
					link3Delays++;
				}

			} else if(event.getLinkId().equals(Id.createLinkId("2"))){

				if (event.getCausingAgentId().toString().equals("6") && event.getAffectedAgentId().toString().equals("7")) {
					Assert.assertEquals("wrong delay.", 6, event.getDelay(), MatsimTestUtils.EPSILON);
					link2Delays++;
				}  else if (event.getCausingAgentId().toString().equals("8") && event.getAffectedAgentId().toString().equals("9")) {
					Assert.assertEquals("wrong delay.", 14, event.getDelay(), MatsimTestUtils.EPSILON);
					link2Delays++;
				}

			} else throw new RuntimeException("Delay can not occur on this link - "+event.getLinkId().toString());

		}

		// affected persons are 2,4,6,8 on link3 and 6,7,8,9 on link 2.
		Assert.assertEquals("wrong number of affected persons" , 6, affectedPersons.size());

		//causing agents set should not have any one from 1,3,5,7,9
		for(int id :causingPersons){
			Assert.assertEquals("Wrong causing person", 0, id%2);
		}

		Assert.assertEquals("some events are not checked on link 2" , 2, link2Delays);
		Assert.assertEquals("some events are not checked on link 3" , 6, link3Delays);

	}

	@Test
	public final void implV4Test(){

		int numberOfPersonInPlan = 10;
		createPseudoInputs pseudoInputs = new createPseudoInputs();
		pseudoInputs.createNetwork();
		pseudoInputs.createPopulation(numberOfPersonInPlan);
		Scenario sc = pseudoInputs.scenario;

		EventsManager events = EventsUtils.createEventsManager();

		final List<CongestionEvent> congestionEvents = new ArrayList<CongestionEvent>();

		events.addHandler( new CongestionEventHandler() {

			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(CongestionEvent event) {
				congestionEvents.add(event);
			}

		});

		events.addHandler(new CongestionHandlerImplV4(events, (ScenarioImpl) sc));

		QSim sim = createQSim(sc, events);
		sim.run();

		Assert.assertEquals("wrong number of congestion events" , 15, congestionEvents.size());

		Set<String> affectedPersons = new HashSet<>();
		Set<Integer> causingPersons = new HashSet<>();
		int link2Delays=0;
		int link3Delays=0;

		double person6Delay=0;
		double person8_6Delay=0;
		double person8_4Delay=0;
		
		int repetationPerson6Count=0;
		int repetationPerson8_6Count=0;
		int repetationPerson8_4Count=0;

		for (CongestionEvent event : congestionEvents) {

			affectedPersons.add(event.getAffectedAgentId().toString());
			causingPersons.add(Integer.valueOf(event.getCausingAgentId().toString()));

			if(event.getLinkId().equals(Id.createLinkId("3"))){

				if (event.getCausingAgentId().toString().equals("0") && event.getAffectedAgentId().toString().equals("2")) {
					Assert.assertEquals("wrong delay.", 9, event.getDelay(), MatsimTestUtils.EPSILON);
					link3Delays++;
				} else if (event.getCausingAgentId().toString().equals("2") && event.getAffectedAgentId().toString().equals("4")) {
					Assert.assertEquals("wrong delay.", 10, event.getDelay(), MatsimTestUtils.EPSILON);
					link3Delays++;
				} else if (event.getCausingAgentId().toString().equals("0") && event.getAffectedAgentId().toString().equals("4")) {
					Assert.assertEquals("wrong delay.", 7, event.getDelay(), MatsimTestUtils.EPSILON);
					link3Delays++;
				} else if (event.getCausingAgentId().toString().equals("4") && event.getAffectedAgentId().toString().equals("6")) {
					//	delays 10,6
					person6Delay+=event.getDelay();
					if(repetationPerson6Count>0){
						Assert.assertEquals("wrong delay.", 16, person6Delay, MatsimTestUtils.EPSILON);	
					}
					repetationPerson6Count++;
					link3Delays++;
				} else if (event.getCausingAgentId().toString().equals("2") && event.getAffectedAgentId().toString().equals("6")) {
					Assert.assertEquals("wrong delay.", 9, event.getDelay(), MatsimTestUtils.EPSILON);
					link3Delays++;
				} else if (event.getCausingAgentId().toString().equals("6") && event.getAffectedAgentId().toString().equals("8")) {
					//	delays are 10, 10
					person8_6Delay+=event.getDelay();
					if(repetationPerson8_6Count>0){
						Assert.assertEquals("wrong delay.", 20, person8_6Delay, MatsimTestUtils.EPSILON);	
					}
					repetationPerson8_6Count++;
					link3Delays++;
				} else if (event.getCausingAgentId().toString().equals("4") && event.getAffectedAgentId().toString().equals("8")) {
					//	 delays are 4,9
					person8_4Delay+=event.getDelay();
					if(repetationPerson8_4Count>0){
						Assert.assertEquals("wrong delay.", 13, person8_4Delay, MatsimTestUtils.EPSILON);	
					}
					repetationPerson8_4Count++;
					link3Delays++;
				} else if (event.getCausingAgentId().toString().equals("6") && event.getAffectedAgentId().toString().equals("9")) {
					Assert.assertEquals("wrong delay.", 3, event.getDelay(), MatsimTestUtils.EPSILON);
					link3Delays++;
				} else if (event.getCausingAgentId().toString().equals("8") && event.getAffectedAgentId().toString().equals("9")) {
					Assert.assertEquals("wrong delay.", 10, event.getDelay(), MatsimTestUtils.EPSILON);
					link3Delays++;
				} else if (event.getCausingAgentId().toString().equals("6") && event.getAffectedAgentId().toString().equals("7")) {
					Assert.assertEquals("wrong delay.", 5, event.getDelay(), MatsimTestUtils.EPSILON);
					link3Delays++;
				}

			} else if(event.getLinkId().equals(Id.createLinkId("2"))){

				if (event.getCausingAgentId().toString().equals("6") && event.getAffectedAgentId().toString().equals("7")) {
					Assert.assertEquals("wrong delay.", 1, event.getDelay(), MatsimTestUtils.EPSILON);
					link2Delays++;
				} else if (event.getCausingAgentId().toString().equals("8") && event.getAffectedAgentId().toString().equals("9")) {
					Assert.assertEquals("wrong delay.", 1, event.getDelay(), MatsimTestUtils.EPSILON);
					link2Delays++;
				} 

			} else throw new RuntimeException("Delay can not occur on link id - "+event.getLinkId().toString());
		}

		// affected persons are 2,4,6,8 on link3 and 6,7,8,9 on link 2.
		Assert.assertEquals("wrong number of affected persons" , 6, affectedPersons.size());

		//causing agents set should not have any one from 1,3,5,7,9
		for(int id :causingPersons){
			Assert.assertEquals("Wrong causing person", 0, id%2);
		}

		Assert.assertEquals("some events are not checked on link 2" , 2, link2Delays);
		Assert.assertEquals("some events are not checked on link 3" , 13, link3Delays);
	}

	@Test
	public void compareTwoImplementations(){

		Map<Id<Person>, Double> personId2affectedDelay_v4  = getAffectedPersonId2Delays("v4");
		Map<Id<Person>, Double> personId2affectedDelay_v6  = getAffectedPersonId2Delays("v6");

		for(Id<Person> personId : personId2affectedDelay_v4.keySet()){
			Assert.assertEquals("wrong delay for person "+personId, personId2affectedDelay_v4.get(personId), personId2affectedDelay_v6.get(personId), MatsimTestUtils.EPSILON);
		}
	}

	private Map<Id<Person>, Double> getAffectedPersonId2Delays(String congestionPricingImpl){

		int numberOfPersonInPlan = 10;
		createPseudoInputs pseudoInputs = new createPseudoInputs();
		pseudoInputs.createNetwork();
		pseudoInputs.createPopulation(numberOfPersonInPlan);
		Scenario sc = pseudoInputs.scenario;

		EventsManager events = EventsUtils.createEventsManager();

		Map<Id<Person>, Double> personId2affectedDelay = new HashMap<Id<Person>, Double>();

		final List<CongestionEvent> congestionEvents = new ArrayList<CongestionEvent>();

		events.addHandler( new CongestionEventHandler() {

			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(CongestionEvent event) {
				congestionEvents.add(event);
			}

		});

		if(congestionPricingImpl.equalsIgnoreCase("v4")) events.addHandler(new CongestionHandlerImplV4(events, sc));
		else if(congestionPricingImpl.equalsIgnoreCase("v6")) events.addHandler(new CongestionHandlerImplV6(events, sc));

		QSim sim = createQSim(sc, events);
		sim.run();

		for (CongestionEvent event : congestionEvents) {
			if(personId2affectedDelay.containsKey(event.getAffectedAgentId())){
				double delaySoFar = personId2affectedDelay.get(event.getAffectedAgentId());
				personId2affectedDelay.put(event.getAffectedAgentId(), event.getDelay()+delaySoFar);
			} else personId2affectedDelay.put(event.getAffectedAgentId(), event.getDelay());
		}

		return personId2affectedDelay;
	}

	private QSim createQSim (Scenario sc, EventsManager manager){
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

		Map<String, VehicleType> modeVehicleTypes = new HashMap<String, VehicleType>();

		VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("car", VehicleType.class));
		car.setMaximumVelocity(20);
		car.setPcuEquivalents(1.0);
		modeVehicleTypes.put("car", car);
		agentSource.setModeVehicleTypes(modeVehicleTypes);
		qSim.addAgentSource(agentSource);
		return qSim;
	}

	/**
	 * generates network with 6 links. Even persons will go on one branch (up) and odd persons will go on other (down).
	 *<p>				  o----4----o
	 *<p> 				  |
	 *<p>				  3 
	 *<p>				  |
	 *<p>				  |
	 *<p>  o--1---o---2---o
	 *<p>				  |
	 *<p>				  |
	 *<p>				  5
	 *<p>				  |
	 *<p>				  o----6----o
	 */
	private class createPseudoInputs {
		Scenario scenario;
		Config config;
		NetworkImpl network;
		Population population;
		Link link1;
		Link link2;
		Link link3;
		Link link4;
		Link link5;
		Link link6;
		public createPseudoInputs(){
			config=ConfigUtils.createConfig();
			this.scenario = ScenarioUtils.loadScenario(config);
			network =  (NetworkImpl) this.scenario.getNetwork();
			population = this.scenario.getPopulation();
		}

		private void createNetwork(){
			
			Node node1 = network.createAndAddNode(Id.createNodeId("1"), this.scenario.createCoord(0, 0)) ;
			Node node2 = network.createAndAddNode(Id.createNodeId("2"), this.scenario.createCoord(100, 100));
			Node node3 = network.createAndAddNode(Id.createNodeId("3"), this.scenario.createCoord(300, 90));
			Node node4 = network.createAndAddNode(Id.createNodeId("4"), this.scenario.createCoord(500, 200));
			Node node5 = network.createAndAddNode(Id.createNodeId("5"), this.scenario.createCoord(700, 150));
			Node node6 = network.createAndAddNode(Id.createNodeId("6"), this.scenario.createCoord(500, 20));
			Node node7 = network.createAndAddNode(Id.createNodeId("7"), this.scenario.createCoord(700, 100));

			link1 = network.createAndAddLink(Id.createLinkId(String.valueOf("1")), node1, node2,1000.0,20.0,3600,1,null,"7");
			link2 = network.createAndAddLink(Id.createLinkId(String.valueOf("2")), node2, node3,1000.0,20.0,3600,1,null,"7");
			link3 = network.createAndAddLink(Id.createLinkId(String.valueOf("3")), node3, node4,10.0,20.0,360,1,null,"7");
			link4 = network.createAndAddLink(Id.createLinkId(String.valueOf("4")), node4, node5,1000.0,20.0,3600,1,null,"7");
			link5 = network.createAndAddLink(Id.createLinkId(String.valueOf("5")), node3, node6,1000.0,20.0,3600,1,null,"7");
			link6 = network.createAndAddLink(Id.createLinkId(String.valueOf("6")), node6, node7,1000.0,20.0,3600,1,null,"7");
		}

		private void createPopulation(int numberOfPersons){

			for(int i=0;i<numberOfPersons;i++){
				Id<Person> id = Id.createPersonId(i);
				Person p = population.getFactory().createPerson(id);
				Plan plan = population.getFactory().createPlan();
				p.addPlan(plan);
				Activity a1 = population.getFactory().createActivityFromLinkId("h", link1.getId());
				a1.setEndTime(8*3600+i);
				Leg leg = population.getFactory().createLeg(TransportMode.car);
				plan.addActivity(a1);
				plan.addLeg(leg);
				LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
				NetworkRoute route;
				List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
				if(i%2==0) {
					route= (NetworkRoute) factory.createRoute(link1.getId(), link4.getId());
					linkIds.add(link2.getId());
					linkIds.add(link3.getId());
					route.setLinkIds(link1.getId(), linkIds, link4.getId());
					leg.setRoute(route);
					Activity a2 = population.getFactory().createActivityFromLinkId("w", link4.getId());
					plan.addActivity(a2);
				} else {
					route = (NetworkRoute) factory.createRoute(link1.getId(), link6.getId());
					linkIds.add(link2.getId());
					linkIds.add(link5.getId());
					route.setLinkIds(link1.getId(), linkIds, link6.getId());
					leg.setRoute(route);
					Activity a2 = population.getFactory().createActivityFromLinkId("w", link6.getId());
					plan.addActivity(a2);
				}
				population.addPerson(p);
			}
		}
	}
}
