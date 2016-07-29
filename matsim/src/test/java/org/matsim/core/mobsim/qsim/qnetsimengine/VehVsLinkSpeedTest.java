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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;


/**
 * Tests that minimum of link speed and vehicle speed is used in the simulation.
 * @author amit
 */

@RunWith(Parameterized.class)
public class VehVsLinkSpeedTest {
	
	public VehVsLinkSpeedTest(final double vehSpeed) {
		this.vehSpeed = vehSpeed;
	}
	
	private double vehSpeed ;
	private final static double MAX_SPEED_ON_LINK = 25; //in m/s

	@Parameters(name = "{index}: vehicleSpeed == {0};")
	public static Collection<Object> createFds() {
		Object [] vehSpeeds = new Object [] { 
				30, 20
		};
		return Arrays.asList(vehSpeeds);
	}
	
	@Test 
	public void testVehicleSpeed(){
		SimpleNetwork net = new SimpleNetwork();
		
		Id<Person> id = Id.createPersonId(0);
		Person p = net.population.getFactory().createPerson(id);
		Plan plan = net.population.getFactory().createPlan();
		p.addPlan(plan);
		Activity a1 = net.population.getFactory().createActivityFromLinkId("h", net.link1.getId());
		a1.setEndTime(8*3600);
		Leg leg = net.population.getFactory().createLeg(TransportMode.car);
		plan.addActivity(a1);
		plan.addLeg(leg);
		
		LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
		NetworkRoute route = (NetworkRoute) factory.createRoute(net.link1.getId(), net.link3.getId());
		route.setLinkIds(net.link1.getId(), Arrays.asList(net.link2.getId()), net.link3.getId());
		leg.setRoute(route);
		Activity a2 = net.population.getFactory().createActivityFromLinkId("w", net.link3.getId());
		plan.addActivity(a2);
		net.population.addPerson(p);

		Map<Id<Person>, Map<Id<Link>, Double>> personLinkTravelTimes = new HashMap<Id<Person>, Map<Id<Link>, Double>>();

		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(new PersonLinkTravelTimeEventHandler(personLinkTravelTimes));

		QSim qSim = createQSim(net, manager, this.vehSpeed);
		qSim.run();

		Map<Id<Link>, Double> travelTime1 = personLinkTravelTimes.get(Id.create("0", Person.class));
	
		Link desiredLink = net.scenario.getNetwork().getLinks().get(Id.createLinkId(2));
		
		double carTravelTime = travelTime1.get(desiredLink.getId()); // 1000 / min(25, vehSpeed)
		double speedUsedInSimulation = Math.round( desiredLink.getLength() / (carTravelTime - 1) );

		Assert.assertEquals("In the simulation minimum of vehicle speed and link speed should be used.", 
				Math.min(vehSpeed, MAX_SPEED_ON_LINK), speedUsedInSimulation, MatsimTestUtils.EPSILON);
	}

	private QSim createQSim (SimpleNetwork net, EventsManager manager, double maxVelocity){
		Scenario sc = net.scenario;
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
		car.setMaximumVelocity(maxVelocity);
		car.setPcuEquivalents(1.0);
		modeVehicleTypes.put("car", car);
		agentSource.setModeVehicleTypes(modeVehicleTypes);
		qSim.addAgentSource(agentSource);
		return qSim;
	}


	private static final class SimpleNetwork{
		final Config config;
		final Scenario scenario ;
		final Network network;
		final Population population;
		final Link link1;
		final Link link2;
		final Link link3;

		public SimpleNetwork(){

			scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			config = scenario.getConfig();
			config.qsim().setFlowCapFactor(1.0);
			config.qsim().setStorageCapFactor(1.0);
			config.qsim().setMainModes(Arrays.asList("car","bike"));
			config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ.name());

			network = (Network) scenario.getNetwork();
			this.network.setCapacityPeriod(Time.parseTime("1:00:00"));
			double x = -100.0;
			Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord(x, 0.0));
			Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord(0.0, 0.0));
			Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord(0.0, 1000.0));
			Node node4 = NetworkUtils.createAndAddNode(network, Id.create("4", Node.class), new Coord(0.0, 1100.0));

			Set<String> allowedModes = new HashSet<String>(); allowedModes.addAll(Arrays.asList("car","bike"));
			final Node fromNode = node1;
			final Node toNode = node2;

			link1 = NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode, toNode, (double) 100, MAX_SPEED_ON_LINK, (double) 60, (double) 1, null, "22");
			final Node fromNode1 = node2;
			final Node toNode1 = node3; 
			link2 = NetworkUtils.createAndAddLink(network,Id.create("2", Link.class), fromNode1, toNode1, (double) 1000, MAX_SPEED_ON_LINK, (double) 60, (double) 1, null, "22");
			final Node fromNode2 = node3;
			final Node toNode2 = node4;	
			link3 = NetworkUtils.createAndAddLink(network,Id.create("3", Link.class), fromNode2, toNode2, (double) 100, MAX_SPEED_ON_LINK, (double) 60, (double) 1, null, "22");

			population = scenario.getPopulation();
		}
	}
	private static class PersonLinkTravelTimeEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

		private final Map<Id<Person>, Map<Id<Link>, Double>> personLinkTravelTimes;

		public PersonLinkTravelTimeEventHandler(Map<Id<Person>, Map<Id<Link>, Double>> agentTravelTimes) {
			this.personLinkTravelTimes = agentTravelTimes;
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			Map<Id<Link>, Double> travelTimes = this.personLinkTravelTimes.get(Id.createPersonId(event.getVehicleId()));
			if (travelTimes == null) {
				travelTimes = new HashMap<Id<Link>, Double>();
				this.personLinkTravelTimes.put(Id.createPersonId(event.getVehicleId()), travelTimes);
			}
			travelTimes.put(event.getLinkId(), Double.valueOf(event.getTime()));
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Map<Id<Link>, Double> travelTimes = this.personLinkTravelTimes.get(Id.createPersonId(event.getVehicleId()));
			if (travelTimes != null) {
				Double d = travelTimes.get(event.getLinkId());
				if (d != null) {
					double time = event.getTime() - d.doubleValue();
					travelTimes.put(event.getLinkId(), Double.valueOf(time));
				}
			}
		}

		@Override
		public void reset(int iteration) {
		}
	}
}