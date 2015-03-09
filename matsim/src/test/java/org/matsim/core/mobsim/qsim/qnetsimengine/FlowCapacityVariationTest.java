/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) ${year} by the members listed in the COPYING,        *
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
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * Tests that two persons can leave a link at the same time if flow capacity permits
 * In other words, test if qsim can handle capacity more than 3600 PCU/Hr.
 * If the flow capacity is 3601 PCU/Hr it will allow the two vehicles.
 * 
 */
public class FlowCapacityVariationTest {
	
	@Test
	public void twoCarsLeavingTimes () {
		VehicleLeavingSameTime(TransportMode.car,3601);
	}

	@Test 
	public void twoMotorbikesTravelTime(){
		/* linkCapacity higher than 1PCU/sec*/
		VehicleLeavingSameTime("motorbike",3601);
		
		/*link capacuty higher than 1motorbike/sec = 0.25PCU/sec */
//		VehicleLeavingSameTime("motorbike",1800);
	}
	
	@Test 
	public void twoBikesTravelTime(){
		/* linkCapacity higher than 1PCU/sec */
		VehicleLeavingSameTime(TransportMode.bike,3601);
				
		/* link capacuty higher than 1motorbike/sec = 0.25PCU/sec */
//		VehicleLeavingSameTime(TransportMode.bike,1800);
	}
	
	private void VehicleLeavingSameTime(String travelMode, double linkCapacity){
		PseudoInputs net = new PseudoInputs(travelMode);
		net.createNetwork(linkCapacity);
		net.createPopulation();

		Map<Id<Person>, Map<Id<Link>, double[]>> personLinkTravelTimes = new HashMap<Id<Person>, Map<Id<Link>, double[]>>();

		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(new PersonLinkTravelTimeEventHandler(personLinkTravelTimes));

		QSim qSim = createQSim(net,manager);
		qSim.run();

		Map<Id<Link>, double[]> times1 = personLinkTravelTimes.get(Id.create("1", Person.class));
		Map<Id<Link>, double[]> times2 = personLinkTravelTimes.get(Id.create("2", Person.class));

		int linkEnterTime1 = (int)times1.get(Id.create("2", Link.class))[0]; 
		int linkEnterTime2 = (int)times2.get(Id.create("2", Link.class))[0];

		int linkLeaveTime1 = (int)times1.get(Id.create("2", Link.class))[1]; 
		int linkLeaveTime2 = (int)times2.get(Id.create("2", Link.class))[1];

		Assert.assertEquals(travelMode+ " entered at different time", 0, linkEnterTime1-linkEnterTime2);
		Assert.assertEquals(travelMode +" entered at same time but not leaving the link at the same time.", 0, linkLeaveTime1-linkLeaveTime2);
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


	private static final class PseudoInputs{

		final Config config;
		final Scenario scenario ;
		NetworkImpl network;
		final Population population;
		Link link1;
		Link link2;
		Link link3;
		private String travelMode;

		public PseudoInputs(String travelMode){

			this.travelMode = travelMode;
			
			scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			config = scenario.getConfig();
			config.qsim().setMainModes(Arrays.asList(travelMode));

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

			// Vehicles info			
			((ScenarioImpl)scenario).createVehicleContainer();
			scenario.getConfig().qsim().setUseDefaultVehicles(false);

			VehicleType vt = VehicleUtils.getFactory().createVehicleType(Id.create(travelMode, VehicleType.class));
			vt.setMaximumVelocity(travelMode == "bike" ? 5.0 : 20.0 );
			vt.setPcuEquivalents(travelMode == "car" ? 1.0 : 0.25);
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
				route= (NetworkRoute) factory.createRoute(link1.getId(), link3.getId());
				linkIds.add(link2.getId());
				route.setLinkIds(link1.getId(), linkIds, link3.getId());
				leg.setRoute(route);

				Activity a2 = population.getFactory().createActivityFromLinkId("w", link3.getId());
				plan.addActivity(a2);
				population.addPerson(p);

				Id<Vehicle> vehId = Id.create(i,Vehicle.class);
				Vehicle veh = VehicleUtils.getFactory().createVehicle(vehId, vt);
				scenario.getVehicles().addVehicle(veh);
			}
		}

	}

	private static class PersonLinkTravelTimeEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

		private final Map<Id<Person>, Map<Id<Link>, double[]>> personLinkEnterLeaveTimes;

		public PersonLinkTravelTimeEventHandler(Map<Id<Person>, Map<Id<Link>, double[]>> agentLinkEnterLeaveTimes) {
			this.personLinkEnterLeaveTimes = agentLinkEnterLeaveTimes;
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			Logger.getLogger(PersonLinkTravelTimeEventHandler.class).info(event.toString());
			Map<Id<Link>, double[]> times = this.personLinkEnterLeaveTimes.get(Id.createPersonId(event.getVehicleId()));
			if (times == null) {
				times = new HashMap<Id<Link>, double[]>();
				double [] linkEnterLeaveTime = {Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY};
				times.put(event.getLinkId(), linkEnterLeaveTime);
				this.personLinkEnterLeaveTimes.put(Id.createPersonId(event.getVehicleId()), times);
			}
			double linkLeaveTime;
			if(times.get(event.getLinkId())!=null){
				linkLeaveTime = times.get(event.getLinkId())[1];
			} else linkLeaveTime = Double.POSITIVE_INFINITY;

			double [] linkEnterTime = {event.getTime(),linkLeaveTime};
			times.put(event.getLinkId(), linkEnterTime);
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Logger.getLogger(PersonLinkTravelTimeEventHandler.class).info(event.toString());
			Map<Id<Link>, double[]> times = this.personLinkEnterLeaveTimes.get(Id.createPersonId(event.getVehicleId()));
			if (times != null) {
				double linkEnterTime = times.get(event.getLinkId())[0];
				double [] linkEnterLeaveTime = {linkEnterTime,event.getTime()};
				times.put(event.getLinkId(), linkEnterLeaveTime);
			}
		}

		@Override
		public void reset(int iteration) {
		}
	}
}
