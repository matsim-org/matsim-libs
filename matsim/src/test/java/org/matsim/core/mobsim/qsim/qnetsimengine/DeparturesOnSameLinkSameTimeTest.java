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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
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
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * @author amit
 */

public class DeparturesOnSameLinkSameTimeTest {

	/**
	 * Two cases where two motorbikes or two cars depart at the same time on link l_1.
	 * <p> Since, the flow capacity of link l_1 is 1 PCU per sec, both motorbike should be able to leave the link l_1 at the same time
	 * whereas cars should leave at a gap of one second.
	 */
	@Test 
	public void test4LinkEnterTimeOfCarAndBike () {
		
		Id<Person> firstAgent = Id.createPersonId(1);
		Id<Person> secondAgent = Id.createPersonId(2);
		
		Id<Link> departureLink = Id.createLinkId(1);
		
		Map<Id<Person>,Map<Id<Link>, Double>> motorbikeLinkLeaveTime = getLinkEnterTime("motorbike",3600);
		Map<Id<Person>,Map<Id<Link>, Double>> carLinkLeaveTime = getLinkEnterTime(TransportMode.car,3600);
		
		double diff_carAgents_departureLink_LeaveTimes = carLinkLeaveTime.get(secondAgent).get(departureLink) - carLinkLeaveTime.get(firstAgent).get(departureLink);
		Assert.assertEquals("Both car agents should leave at the gap of 1 sec.", 1., Math.abs(diff_carAgents_departureLink_LeaveTimes), MatsimTestUtils.EPSILON );
		
		double diff_motorbikeAgents_departureLink_LeaveTimes = motorbikeLinkLeaveTime.get(secondAgent).get(departureLink) - motorbikeLinkLeaveTime.get(firstAgent).get(departureLink);
		Assert.assertEquals("Both motorbike agents should leave at the same time.", 0., diff_motorbikeAgents_departureLink_LeaveTimes, MatsimTestUtils.EPSILON );
		
		// for flow cap more than 3600, both cars also should leave link l_1 at the same time.
		carLinkLeaveTime = getLinkEnterTime(TransportMode.car,3601);
	
		diff_carAgents_departureLink_LeaveTimes = carLinkLeaveTime.get(secondAgent).get(departureLink) - carLinkLeaveTime.get(firstAgent).get(departureLink);
		Assert.assertEquals("Both car agents should leave at the same time", 0., diff_carAgents_departureLink_LeaveTimes, MatsimTestUtils.EPSILON );
	}

	private Map<Id<Person>,Map<Id<Link>, Double>> getLinkEnterTime (String travelMode, double departureLinkCapacity){
		
		PseudoInputs inputs = new PseudoInputs(travelMode);
		inputs.createNetwork(departureLinkCapacity);
		inputs.createPopulation();

		final Map<Id<Person>,Map<Id<Link>, Double>> linkLeaveTimes = new HashMap<>() ;

		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(new LinkLeaveEventHandler() {

			@Override
			public void reset(int iteration) {
				linkLeaveTimes.clear();
			}

			@Override
			public void handleEvent(LinkLeaveEvent event) {
			
				Id<Person> personId = Id.createPersonId(event.getVehicleId());
				
				if(linkLeaveTimes.containsKey(personId)){
				
					Map<Id<Link>, Double> times = linkLeaveTimes.get(personId);
					times.put(event.getLinkId(), event.getTime());
					linkLeaveTimes.put(personId, times);
				
				} else {
					
					Map<Id<Link>, Double> times = new HashMap<Id<Link>, Double>();
					times.put(event.getLinkId(), event.getTime());
					linkLeaveTimes.put(personId, times);
					
				}
			}
		});
		
		QSim qsim = createQSim(inputs.scenario, events);
		qsim.run();

		return linkLeaveTimes;
	}


	private QSim createQSim (Scenario scenario, EventsManager eventsManager){
		QSim qSim = new QSim(scenario, eventsManager);
		ActivityEngine activityEngine = new ActivityEngine(eventsManager, qSim.getAgentCounter());
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
		QNetsimEngine netsimEngine = new QNetsimEngine(qSim);
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine(scenario, eventsManager);
		qSim.addMobsimEngine(teleportationEngine);
		AgentFactory agentFactory = new DefaultAgentFactory(qSim);
		PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), agentFactory, qSim);
		qSim.addAgentSource(agentSource);
		return qSim;
	}

	
	/**
	 * Corridor link
	 * <p> o-----o------o
	 *
	 */
	private class PseudoInputs {

		Scenario scenario;
		Config config;
		NetworkImpl network;
		Population population;
		Link link1;
		Link link2;
		private String travelMode;

		public PseudoInputs(String travelMode) {
			this.travelMode = travelMode;
			config=ConfigUtils.createConfig();
			this.scenario = ScenarioUtils.loadScenario(config);
			config.qsim().setMainModes(Arrays.asList(travelMode));
			network =  (NetworkImpl) this.scenario.getNetwork();
			population = this.scenario.getPopulation();
		}

		private void createNetwork(double departureLinkCapacity){

			Node node1 = network.createAndAddNode(Id.createNodeId("1"), this.scenario.createCoord(0, 0)) ;
			Node node2 = network.createAndAddNode(Id.createNodeId("2"), this.scenario.createCoord(100, 10));
			Node node3 = network.createAndAddNode(Id.createNodeId("3"), this.scenario.createCoord(300, -10));

			link1 = network.createAndAddLink(Id.createLinkId(String.valueOf("1")), node1, node2,1000.0,20.0,departureLinkCapacity,1,null,"7");
			link2 = network.createAndAddLink(Id.createLinkId(String.valueOf("2")), node2, node3,1000.0,20.0,3600,1,null,"7");
		}

		private void createPopulation(){

			// Vehicles info			
			scenario.getConfig().qsim().setUseDefaultVehicles(false);
			scenario.getConfig().qsim().setUsingFastCapacityUpdate(true);

			VehicleType vt = VehicleUtils.getFactory().createVehicleType(Id.create(travelMode, VehicleType.class));
			vt.setMaximumVelocity(20);
			vt.setPcuEquivalents(travelMode == "motorbike" ? 0.25 : 1.0);
			scenario.getVehicles().addVehicleType(vt);

			for(int i=1;i<3;i++){
				Id<Person> id = Id.createPersonId(i);
				Person p = population.getFactory().createPerson(id);
				Plan plan = population.getFactory().createPlan();
				p.addPlan(plan);
				Activity a1 = population.getFactory().createActivityFromLinkId("h", link1.getId());

				a1.setEndTime(0*3600);
				Leg leg = population.getFactory().createLeg(travelMode);
				plan.addActivity(a1);
				plan.addLeg(leg);
				LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
				NetworkRoute route;
				List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
				route= (NetworkRoute) factory.createRoute(link1.getId(), link2.getId());
				linkIds.add(link2.getId());
				route.setLinkIds(link1.getId(), linkIds, link2.getId());
				leg.setRoute(route);

				Activity a2 = population.getFactory().createActivityFromLinkId("w", link2.getId());
				plan.addActivity(a2);
				population.addPerson(p);

				Id<Vehicle> vehId = Id.create(i,Vehicle.class);
				Vehicle veh = VehicleUtils.getFactory().createVehicle(vehId, vt);
				scenario.getVehicles().addVehicle(veh);
			}
		}
	}
}
