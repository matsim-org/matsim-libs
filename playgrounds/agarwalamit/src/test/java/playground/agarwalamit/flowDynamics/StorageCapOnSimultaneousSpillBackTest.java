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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
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
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.testcases.MatsimTestUtils;

/**
 * 
 * This test is created to determine that which agent will move to bottleneck link 
 * if spill back occurs on two upstream link simultaneously. 
 * 
 * <p> This also depends on the capacity of the links as mentioned in the <code>QNode.doSimStep(now)</code> method. 
 * 
 * @author amit
 */

@RunWith(Parameterized.class)
public class StorageCapOnSimultaneousSpillBackTest {
	private final boolean isUsingFastCapacityUpdate;

	public StorageCapOnSimultaneousSpillBackTest(boolean isUsingFastCapacityUpdate) {
		this.isUsingFastCapacityUpdate = isUsingFastCapacityUpdate;
	}

	@Parameters(name = "{index}: isUsingfastCapacityUpdate == {0}")
	public static Collection<Object> parameterObjects () {
		Object [] capacityUpdates = new Object [] { false, true };
		return Arrays.asList(capacityUpdates);
	}

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

		Tuple<Id<Link>, Id<Link>> startLinkIds = new Tuple<Id<Link>, Id<Link>>(Id.createLinkId(1), Id.createLinkId(4)); // agent 2,4 depart on link 1
		Map<Id<Person>, Tuple<Double,Double>> person2EnterTime = getPerson2LinkEnterTime(startLinkIds);

		if (this.isUsingFastCapacityUpdate ) {
			Assert.assertEquals("Person 3 is entering on link 2 at wrong time.", 13.0, person2EnterTime.get(Id.createPersonId(3)).getFirst(),MatsimTestUtils.EPSILON);
			Assert.assertEquals("Person 3 is leaving from link 2 at wrong time.", 23.0, person2EnterTime.get(Id.createPersonId(3)).getSecond(),MatsimTestUtils.EPSILON);

			Assert.assertEquals("Person 4 is entering on link 2 at wrong time.", 23.0, person2EnterTime.get(Id.createPersonId(4)).getFirst(),MatsimTestUtils.EPSILON);
			Assert.assertEquals("Person 4 is leaving from link 2 at wrong time.", 33.0, person2EnterTime.get(Id.createPersonId(4)).getSecond(),MatsimTestUtils.EPSILON);

		} else { // here are some rounding errors --> person 1 leave, link blocked for 10 sec, person 2 leave after 11 sec, link blocked again for 10 sec, person 2 leave after 10 sec.
			Assert.assertEquals("Person 3 is entering on link 2 at wrong time.", 14.0, person2EnterTime.get(Id.createPersonId(3)).getFirst(),MatsimTestUtils.EPSILON);
			Assert.assertEquals("Person 3 is leaving from link 2 at wrong time.", 24.0, person2EnterTime.get(Id.createPersonId(3)).getSecond(),MatsimTestUtils.EPSILON);

			Assert.assertEquals("Person 4 is entering on link 2 at wrong time.", 24.0, person2EnterTime.get(Id.createPersonId(4)).getFirst(),MatsimTestUtils.EPSILON);
			Assert.assertEquals("Person 4 is leaving from link 2 at wrong time.", 34.0, person2EnterTime.get(Id.createPersonId(4)).getSecond(),MatsimTestUtils.EPSILON);
		}


		for(Id<Person> personId : person2EnterTime.keySet()){
			System.out.println("Person "+personId+ " is entering link 2 at time "+person2EnterTime.get(personId).getFirst() +" and leaving at time "+person2EnterTime.get(personId).getSecond());
		}

		//changing the links order such that agent 2,4 depart on link 4
		startLinkIds = new Tuple<Id<Link>, Id<Link>>(Id.createLinkId(4), Id.createLinkId(1));
		person2EnterTime = getPerson2LinkEnterTime(startLinkIds);

		if(this.isUsingFastCapacityUpdate) { 
			Assert.assertEquals("Person 3 is entering on link 2 at wrong time.", 23.0, person2EnterTime.get(Id.createPersonId(3)).getFirst(),MatsimTestUtils.EPSILON);
			Assert.assertEquals("Person 3 is leaving from link 2 at wrong time.", 33.0, person2EnterTime.get(Id.createPersonId(3)).getSecond(),MatsimTestUtils.EPSILON);

			Assert.assertEquals("Person 4 is entering on link 2 at wrong time.", 13.0, person2EnterTime.get(Id.createPersonId(4)).getFirst(),MatsimTestUtils.EPSILON);
			Assert.assertEquals("Person 4 is leaving from link 2 at wrong time.", 23.0, person2EnterTime.get(Id.createPersonId(4)).getSecond(),MatsimTestUtils.EPSILON);
		} else {
			Assert.assertEquals("Person 3 is entering on link 2 at wrong time.", 24.0, person2EnterTime.get(Id.createPersonId(3)).getFirst(),MatsimTestUtils.EPSILON);
			Assert.assertEquals("Person 3 is leaving from link 2 at wrong time.", 34.0, person2EnterTime.get(Id.createPersonId(3)).getSecond(),MatsimTestUtils.EPSILON);

			Assert.assertEquals("Person 4 is entering on link 2 at wrong time.", 14.0, person2EnterTime.get(Id.createPersonId(4)).getFirst(),MatsimTestUtils.EPSILON);
			Assert.assertEquals("Person 4 is leaving from link 2 at wrong time.", 24.0, person2EnterTime.get(Id.createPersonId(4)).getSecond(),MatsimTestUtils.EPSILON);
		}

	}

	private Map<Id<Person>, Tuple<Double,Double>> getPerson2LinkEnterTime(final Tuple<Id<Link>, Id<Link>> startLinkIds){

		MatsimRandom.reset(); // resetting the random nos with default seed.

		MergingNetworkAndPlans pseudoInputs = new MergingNetworkAndPlans(this.isUsingFastCapacityUpdate);
		pseudoInputs.createNetwork();
		pseudoInputs.createPopulation(startLinkIds );
		Scenario sc = pseudoInputs.scenario;

		ScenarioUtils.loadScenario(sc);
		Map<Id<Person>, Tuple<Double,Double>> person2LinkEnterTime = new HashMap<>();

		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(new PersonLinkEnterLeaveTime(person2LinkEnterTime));
		QSim sim = QSimUtils.createDefaultQSim(sc, events);
		sim.run();
		return person2LinkEnterTime;
	}

	private class PersonLinkEnterLeaveTime implements LinkEnterEventHandler, LinkLeaveEventHandler{

		Map<Id<Person>, Tuple<Double,Double>> person2linkleaveEnterTime ;

		private PersonLinkEnterLeaveTime(Map<Id<Person>, Tuple<Double,Double>> person2LinkEnterTime){
			this.person2linkleaveEnterTime = person2LinkEnterTime;
		}

		@Override
		public void reset(int iteration) {
			this.person2linkleaveEnterTime.clear();
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			if(event.getLinkId().equals(Id.createLinkId(2))){
				Tuple<Double, Double> linkEnterTime = person2linkleaveEnterTime.get(Id.createPersonId(event.getVehicleId()));
				person2linkleaveEnterTime.put(Id.createPersonId(event.getVehicleId()), new Tuple<Double, Double>(linkEnterTime.getFirst(),event.getTime()));
			}
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			if(event.getLinkId().equals(Id.createLinkId(2))){
				person2linkleaveEnterTime.put(Id.createPersonId(event.getVehicleId()), new Tuple<Double, Double>(event.getTime(), 0.));
			}
		}
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
		final Scenario scenario;
		final Config config;
		final NetworkImpl network;
		final Population population;
		Link link1;
		Link link2;
		Link link3;
		Link link4;

		private MergingNetworkAndPlans(boolean isUsingFastCapacityUpdate){
			config=ConfigUtils.createConfig();
			config.qsim().setStuckTime(3600.);
			config.global().setRandomSeed(2546);
			config.qsim().setUsingFastCapacityUpdate(isUsingFastCapacityUpdate);
			this.scenario = ScenarioUtils.loadScenario(config);
			network =  (NetworkImpl) this.scenario.getNetwork();
			population = this.scenario.getPopulation();
		}

		private void createNetwork(){

			Node node1 = network.createAndAddNode(Id.createNodeId("1"), new Coord((double) 0, (double) 0)) ;
			Node node2 = network.createAndAddNode(Id.createNodeId("2"), new Coord((double) 100, (double) 10));
			double y1 = -10;
			Node node3 = network.createAndAddNode(Id.createNodeId("3"), new Coord((double) 300, y1));
			Node node4 = network.createAndAddNode(Id.createNodeId("4"), new Coord((double) 500, (double) 20));
			double x = -10;
			double y = -200;
			Node node5 = network.createAndAddNode(Id.createNodeId("5"), new Coord(x, y));

			link1 = network.createAndAddLink(Id.createLinkId(String.valueOf("1")), node1, node2,1000.0,20.0,3600.,1,null,"7");
			link2 = network.createAndAddLink(Id.createLinkId(String.valueOf("2")), node2, node3,5.0,20.0,360.,1,null,"7");
			link3 = network.createAndAddLink(Id.createLinkId(String.valueOf("3")), node3, node4,1000.0,20.0,3600.,1,null,"7");
			link4 = network.createAndAddLink(Id.createLinkId(String.valueOf("4")), node5, node2,1000.0,20.0,3600.,1,null,"7");
		}

		private void createPopulation(final Tuple<Id<Link>, Id<Link>> startLinkIds){
			int numberOfPersonInPlan = 4;
			/*Alternative persons from different links*/
			for(int i=1;i<=numberOfPersonInPlan;i++){
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