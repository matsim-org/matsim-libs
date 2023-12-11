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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import playground.vsp.congestion.events.CongestionEvent;
import playground.vsp.congestion.handlers.CongestionEventHandler;
import playground.vsp.congestion.handlers.CongestionHandlerImplV4;

/**
 * @author amit
 */

public class MarginalCongestionPricingTest {
	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	final void implV4Test(){

		int numberOfPersonInPlan = 10;
		createPseudoInputs pseudoInputs = new createPseudoInputs();
		pseudoInputs.createNetwork();
		pseudoInputs.createPopulation(numberOfPersonInPlan);
		Scenario sc = pseudoInputs.scenario;

		VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("car", VehicleType.class));
		car.setMaximumVelocity(20);
		car.setPcuEquivalents(1.0);
		sc.getVehicles().addVehicleType(car);

		sc.getConfig().qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

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

		events.addHandler(new CongestionHandlerImplV4(events, (MutableScenario) sc));

		PrepareForSimUtils.createDefaultPrepareForSim(sc).run();
		new QSimBuilder(sc.getConfig()).useDefaults().build(sc, events).run();

		Assertions.assertEquals(15, congestionEvents.size(), "wrong number of congestion events");

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
					Assertions.assertEquals(8, event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay.");
//					Assert.assertEquals("wrong delay.", 9, event.getDelay(), MatsimTestUtils.EPSILON);//1 sec change  is caused due to the change in the flow accumulation approach (can go to -ive). Amit Jan 17
					link3Delays++;
				} else if (event.getCausingAgentId().toString().equals("2") && event.getAffectedAgentId().toString().equals("4")) {
					Assertions.assertEquals(10, event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay.");
					link3Delays++;
				} else if (event.getCausingAgentId().toString().equals("0") && event.getAffectedAgentId().toString().equals("4")) {
					Assertions.assertEquals(6, event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay.");
//					Assert.assertEquals("wrong delay.", 7, event.getDelay(), MatsimTestUtils.EPSILON);
					link3Delays++;
				} else if (event.getCausingAgentId().toString().equals("4") && event.getAffectedAgentId().toString().equals("6")) {
					//	delays 10,5
					person6Delay+=event.getDelay();
					if(repetationPerson6Count>0){
						Assertions.assertEquals(15, person6Delay, MatsimTestUtils.EPSILON, "wrong delay.");
//						Assert.assertEquals("wrong delay.", 16, person6Delay, MatsimTestUtils.EPSILON);
					}
					repetationPerson6Count++;
					link3Delays++;
				} else if (event.getCausingAgentId().toString().equals("2") && event.getAffectedAgentId().toString().equals("6")) {
					Assertions.assertEquals(9, event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay.");
					link3Delays++;
				} else if (event.getCausingAgentId().toString().equals("6") && event.getAffectedAgentId().toString().equals("8")) {
					//	delays are 10, 10
					person8_6Delay+=event.getDelay();
					if(repetationPerson8_6Count>0){
						Assertions.assertEquals(20, person8_6Delay, MatsimTestUtils.EPSILON, "wrong delay.");
					}
					repetationPerson8_6Count++;
					link3Delays++;
				} else if (event.getCausingAgentId().toString().equals("4") && event.getAffectedAgentId().toString().equals("8")) {
					//	 delays are 3,9
					person8_4Delay+=event.getDelay();
					if(repetationPerson8_4Count>0){
						Assertions.assertEquals(12, person8_4Delay, MatsimTestUtils.EPSILON, "wrong delay.");
//						Assert.assertEquals("wrong delay.", 13, person8_4Delay, MatsimTestUtils.EPSILON);
					}
					repetationPerson8_4Count++;
					link3Delays++;
				} else if (event.getCausingAgentId().toString().equals("6") && event.getAffectedAgentId().toString().equals("9")) {
					Assertions.assertEquals(2, event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay.");
//					Assert.assertEquals("wrong delay.", 3, event.getDelay(), MatsimTestUtils.EPSILON);
					link3Delays++;
				} else if (event.getCausingAgentId().toString().equals("8") && event.getAffectedAgentId().toString().equals("9")) {
					Assertions.assertEquals(10, event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay.");
					link3Delays++;
				} else if (event.getCausingAgentId().toString().equals("6") && event.getAffectedAgentId().toString().equals("7")) {
					Assertions.assertEquals(4, event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay.");
//					Assert.assertEquals("wrong delay.", 5, event.getDelay(), MatsimTestUtils.EPSILON); //1 sec change  is caused due to the change in the flow accumulation approach (can go to -ive). Amit Jan 17
					link3Delays++;
				}

			} else if(event.getLinkId().equals(Id.createLinkId("2"))){

				if (event.getCausingAgentId().toString().equals("6") && event.getAffectedAgentId().toString().equals("7")) {
					Assertions.assertEquals(1, event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay.");
					link2Delays++;
				} else if (event.getCausingAgentId().toString().equals("8") && event.getAffectedAgentId().toString().equals("9")) {
					Assertions.assertEquals(1, event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay.");
					link2Delays++;
				}

			} else throw new RuntimeException("Delay can not occur on link id - "+event.getLinkId().toString());
		}

		// affected persons are 2,4,6,8 on link3 and 6,7,8,9 on link 2.
		Assertions.assertEquals(6, affectedPersons.size(), "wrong number of affected persons");

		//causing agents set should not have any one from 1,3,5,7,9
		for(int id :causingPersons){
			Assertions.assertEquals(0, id%2, "Wrong causing person");
		}

		Assertions.assertEquals(2, link2Delays, "some events are not checked on link 2");
		Assertions.assertEquals(13, link3Delays, "some events are not checked on link 3");
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
		Network network;
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
			network =  (Network) this.scenario.getNetwork();
			population = this.scenario.getPopulation();
		}

		private void createNetwork(){

			Node node1 = NetworkUtils.createAndAddNode(network, Id.createNodeId("1"), new Coord((double) 0, (double) 0)) ;
			Node node2 = NetworkUtils.createAndAddNode(network, Id.createNodeId("2"), new Coord((double) 100, (double) 100));
			Node node3 = NetworkUtils.createAndAddNode(network, Id.createNodeId("3"), new Coord((double) 300, (double) 90));
			Node node4 = NetworkUtils.createAndAddNode(network, Id.createNodeId("4"), new Coord((double) 500, (double) 200));
			Node node5 = NetworkUtils.createAndAddNode(network, Id.createNodeId("5"), new Coord((double) 700, (double) 150));
			Node node6 = NetworkUtils.createAndAddNode(network, Id.createNodeId("6"), new Coord((double) 500, (double) 20));
			Node node7 = NetworkUtils.createAndAddNode(network, Id.createNodeId("7"), new Coord((double) 700, (double) 100));
			final Node fromNode = node1;
			final Node toNode = node2;

			link1 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("1")), fromNode, toNode, 1000.0, 20.0, (double) 3600, (double) 1, null, (String) "7");
			final Node fromNode1 = node2;
			final Node toNode1 = node3;
			link2 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("2")), fromNode1, toNode1, 1000.0, 20.0, (double) 3600, (double) 1, null, (String) "7");
			final Node fromNode2 = node3;
			final Node toNode2 = node4;
			link3 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("3")), fromNode2, toNode2, 10.0, 20.0, (double) 360, (double) 1, null, (String) "7");
			final Node fromNode3 = node4;
			final Node toNode3 = node5;
			link4 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("4")), fromNode3, toNode3, 1000.0, 20.0, (double) 3600, (double) 1, null, (String) "7");
			final Node fromNode4 = node3;
			final Node toNode4 = node6;
			link5 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("5")), fromNode4, toNode4, 1000.0, 20.0, (double) 3600, (double) 1, null, (String) "7");
			final Node fromNode5 = node6;
			final Node toNode5 = node7;
			link6 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("6")), fromNode5, toNode5, 1000.0, 20.0, (double) 3600, (double) 1, null, (String) "7");
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
