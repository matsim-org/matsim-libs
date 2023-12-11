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
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
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
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.vsp.congestion.events.CongestionEvent;
import playground.vsp.congestion.handlers.CongestionEventHandler;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.CongestionHandlerImplV4;

/**
 * @author amit
 */

public class CorridorNetworkTest {

	@Test
	void v3Test(){
		CorridorNetworkAndPlans inputs = new CorridorNetworkAndPlans();
		Scenario sc = inputs.getDesiredScenario();
		
		List<CongestionEvent> v3_events = getCongestionEvents("v3", sc);

		Assertions.assertEquals(6, v3_events.size(), MatsimTestUtils.EPSILON, "wrong number of congestion events");

		for ( CongestionEvent event : v3_events ){

			if(event.getAffectedAgentId().equals(Id.createPersonId(2))){ // agent 2 is delayed on link 2 (bottleneck link) due to agent 1

				Assertions.assertEquals("1", event.getCausingAgentId().toString(), "wrong causing agent");
				Assertions.assertEquals(3, event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay");

			} else if ( event.getAffectedAgentId().equals(Id.createPersonId(3)) ) { // agent 3 is delayed on link 2 due to agent 2, 1

				if(event.getCausingAgentId().equals(Id.createPersonId(2))) {
					
					Assertions.assertEquals(4, event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay");
				
				} else {
				
					Assertions.assertEquals(2, event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay");
				
				}

			} else if(event.getAffectedAgentId().equals(Id.createPersonId(4))){ // agent 4 is first delayed due to spill back on link 1 (3 sec) and then on link 2 (6sec) 

				if(event.getCausingAgentId().equals(Id.createPersonId(3))){
				
					Assertions.assertEquals(4, event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay");
				
				} else if (event.getCausingAgentId().equals(Id.createPersonId(2))){
					
					Assertions.assertEquals(4, event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay");
				
				} else {
					
					Assertions.assertEquals("1", event.getCausingAgentId().toString(), "wrong causing agent");
					Assertions.assertEquals(1, event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay");
				}
			}
		}
	}

	@Test
	void v4Test(){
		CorridorNetworkAndPlans inputs = new CorridorNetworkAndPlans();
		Scenario sc = inputs.getDesiredScenario();

		List<CongestionEvent> v4_events = getCongestionEvents("v4", sc);

		Assertions.assertEquals(6, v4_events.size(), MatsimTestUtils.EPSILON, "wrong number of congestion events");

		for ( CongestionEvent event : v4_events ){

			if(event.getAffectedAgentId().equals(Id.createPersonId(2))){ // agent 2 is delayed on link 2 (bottleneck link) due to agent 1

				Assertions.assertEquals("1", event.getCausingAgentId().toString(), "wrong causing agent");
				Assertions.assertEquals(3, event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay");

			} else if ( event.getAffectedAgentId().equals(Id.createPersonId(3)) ) { // agent 3 is delayed on link 2 due to agent 2, 1

				if(event.getCausingAgentId().equals(Id.createPersonId(2))) {
					
					Assertions.assertEquals(4, event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay");
				
				} else {
				
					Assertions.assertEquals(2, event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay");
				
				}

			} else if(event.getAffectedAgentId().equals(Id.createPersonId(4))){ // agent 4 is first delayed due to spill back on link 1 (3 sec) and then on link 2 (6sec) 

				if(event.getCausingAgentId().equals(Id.createPersonId(3)) ){
					if ( event.getTime() == 10.0 ) {
						
						Assertions.assertEquals(3, event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay");
					
					} else {
					
						Assertions.assertEquals(18.0, event.getTime(), MatsimTestUtils.EPSILON, "wrong congestion event time");
						Assertions.assertEquals(4, event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay");
					
					}
					
				} else {
					
					Assertions.assertEquals("2", event.getCausingAgentId().toString(), "wrong causing agent");
					Assertions.assertEquals(2, event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay");
				
				} 
			}
		}
	}

	private List<CongestionEvent> getCongestionEvents (String congestionPricingImpl, Scenario sc) {
		sc.getConfig().qsim().setStuckTime(3600);

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

		events.addHandler( new BasicEventHandler(){
			@Override public void reset(int iteration) { }
			@Override public void handleEvent(Event event) {
				LogManager.getLogger( CorridorNetworkTest.class ).warn( event );
			}
		});
		
		if(congestionPricingImpl.equalsIgnoreCase("v3")) {
			events.addHandler(new CongestionHandlerImplV3(events, (MutableScenario)sc));
		}
		else if(congestionPricingImpl.equalsIgnoreCase("v4")) events.addHandler(new CongestionHandlerImplV4(events, sc));

		PrepareForSimUtils.createDefaultPrepareForSim(sc).run();
		new QSimBuilder(sc.getConfig()).useDefaults().build(sc, events).run();

		return congestionEvents;
	}


	private class CorridorNetworkAndPlans {

		/**
		 * generates network with 3 links. 
		 *<p>			
		 *<p>  o--0---o---1---o---2---o---3---o
		 *<p>				  
		 */
		Scenario scenario;
		Config config;
		Network network;
		Population population;

		Link link0;
		Link link1;
		Link link2;
		Link link3;

		CorridorNetworkAndPlans(){
			config=ConfigUtils.createConfig();
			this.scenario = ScenarioUtils.loadScenario(config);
			network =  (Network) this.scenario.getNetwork();
			population = this.scenario.getPopulation();
		}

		void createNetwork(){

			Node node1 = NetworkUtils.createAndAddNode(network, Id.createNodeId("1"), new Coord((double) 0, (double) 0)) ;
			Node node2 = NetworkUtils.createAndAddNode(network, Id.createNodeId("2"), new Coord((double) 100, (double) 10));
			double y = -10;
			Node node3 = NetworkUtils.createAndAddNode(network, Id.createNodeId("3"), new Coord((double) 300, y));
			Node node4 = NetworkUtils.createAndAddNode(network, Id.createNodeId("4"), new Coord((double) 500, (double) 20));
			Node node5 = NetworkUtils.createAndAddNode(network, Id.createNodeId("5"), new Coord((double) 700, (double) 0));
			final Node fromNode = node1;
			final Node toNode = node2;

			link0 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("0")), fromNode, toNode, 1000.0, 20.0, 3600., (double) 1, null, (String) "7");
			final Node fromNode1 = node2;
			final Node toNode1 = node3;
			link1 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("1")), fromNode1, toNode1, 100.0, 40.0, 3600., (double) 1, null, (String) "7");
			final Node fromNode2 = node3;
			final Node toNode2 = node4;
			link2 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("2")), fromNode2, toNode2, 10.0, 9.0, 900., (double) 1, null, (String) "7");
			final Node fromNode3 = node4;
			final Node toNode3 = node5;
			link3 = NetworkUtils.createAndAddLink(network,Id.createLinkId(String.valueOf("3")), fromNode3, toNode3, 1000.0, 20.0, 3600., (double) 1, null, (String) "7");
		}

		void createPopulation(){

			for(int i=1;i<=4;i++){
				Id<Person> id = Id.createPersonId(i);
				Person p = population.getFactory().createPerson(id);
				Plan plan = population.getFactory().createPlan();
				p.addPlan(plan);
				Activity a1 = population.getFactory().createActivityFromLinkId("h", link0.getId());
				a1.setEndTime(0*3600+i-1);
				Leg leg = population.getFactory().createLeg(TransportMode.car);
				plan.addActivity(a1);
				plan.addLeg(leg);
				LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
				NetworkRoute route;
				List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
				route= (NetworkRoute) factory.createRoute(link0.getId(), link3.getId());
				linkIds.add(link1.getId());
				linkIds.add(link2.getId());
				linkIds.add(link3.getId());
				route.setLinkIds(link0.getId(), linkIds, link3.getId());
				leg.setRoute(route);
				Activity a2 = population.getFactory().createActivityFromLinkId("w", link3.getId());
				plan.addActivity(a2);
				population.addPerson(p);
			}
		}

		Scenario getDesiredScenario(){
			createNetwork();
			createPopulation();
			return this.scenario;
		}
	}

}


