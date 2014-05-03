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
package playground.agarwalamit.passing;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * Tests total delay on link
 * 
 */
public class CongestionTest {



	@Test 
	public void test4DelayCalculation(){

		SimpleNetwork net = new SimpleNetwork();
		
		for(int i=1;i<20;i++){
			PersonImpl p = new PersonImpl(new IdImpl(i));
			PlanImpl plan = p.createAndAddPlan(true);
			ActivityImpl a1 = plan.createAndAddActivity("h",net.link1.getId());
			a1.setEndTime(8*3600+i*10);
			
			LegImpl leg;
			
			if(i%2==0) leg=plan.createAndAddLeg("cars");
//			else if(i%3==0) leg=plan.createAndAddLeg("motorbikes");
			else leg=plan.createAndAddLeg("bicycles");
			
			LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
			NetworkRoute route = (NetworkRoute) factory.createRoute(net.link1.getId(), net.link3.getId());
			route.setLinkIds(net.link1.getId(), Arrays.asList(net.link2.getId()), net.link3.getId());
			leg.setRoute(route);
			plan.createAndAddActivity("w", net.link3.getId());
			net.population.addPerson(p);
		}

		Map<Id, Map<Id, Double>> personLinkTravelTimes = new HashMap<Id, Map<Id, Double>>();

		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(new PersonLinkTravelTimeEventHandler(personLinkTravelTimes));
		EventWriterXML eventWriterXML = new EventWriterXML("./output/events.xml");
		manager.addHandler(eventWriterXML);
		
		
		
		QSim qSim = createQSim(net,manager);
		qSim.run();
		eventWriterXML.closeFile();
		

//		Map<Id, Double> travelTime1 = personLinkTravelTimes.get(new IdImpl("0"));
//		Map<Id, Double> travelTime2 = personLinkTravelTimes.get(new IdImpl("1"));

//		int bikeTravelTime = travelTime1.get(new IdImpl("2")).intValue(); 
//		int carsTravelTime = travelTime2.get(new IdImpl("2")).intValue();

		//		Assert.assertEquals("wrong number of links.", 3, net.network.getLinks().size());
		//		Assert.assertEquals("wrong number of persons.", 2, net.population.getPersons().size());
		//		Assert.assertEquals("Passing is not implemented", 150, bikeTravelTime-carsTravelTime);

	}

	private QSim createQSim (SimpleNetwork net, EventsManager manager){
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
		Map<String, VehicleType> modeVehicleTypes = new HashMap<String, VehicleType>();

		VehicleType cars = VehicleUtils.getFactory().createVehicleType(new IdImpl("cars"));
		cars.setMaximumVelocity(16.67);
		cars.setPcuEquivalents(1.0);
		modeVehicleTypes.put("cars", cars);

		VehicleType bike = VehicleUtils.getFactory().createVehicleType(new IdImpl("bicycles"));
		bike.setMaximumVelocity(4.17);
		bike.setPcuEquivalents(0.25);
		modeVehicleTypes.put("bicycles", bike);
		
		VehicleType motorbike = VehicleUtils.getFactory().createVehicleType(new IdImpl("motorbike"));
		motorbike.setMaximumVelocity(16.67);
		motorbike.setPcuEquivalents(0.25);
		modeVehicleTypes.put("motorbikes", motorbike);
		
		agentSource.setModeVehicleTypes(modeVehicleTypes);
		qSim.addAgentSource(agentSource);
		return qSim;
	}


	private static final class SimpleNetwork{

		final Config config;
		final Scenario scenario ;
		final NetworkImpl network;
		final Population population;
		final Link link1;
		final Link link2;
		final Link link3;

		public SimpleNetwork(){

			scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			config = scenario.getConfig();
			config.qsim().setFlowCapFactor(1.0);
			config.qsim().setStorageCapFactor(1.0);
			config.qsim().setMainModes(Arrays.asList("cars","bicycles","motorbikes"));
			config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ.name());
			config.qsim().setStuckTime(50*3600);
			config.qsim().setEndTime(18*3600);
//			config.qsim().setRemoveStuckVehicles(true);

			network = (NetworkImpl) scenario.getNetwork();
			this.network.setCapacityPeriod(Time.parseTime("1:00:00"));
			Node node1 = network.createAndAddNode(scenario.createId("1"), scenario.createCoord(-100.0,0.0));
			Node node2 = network.createAndAddNode(scenario.createId("2"), scenario.createCoord( 0.0,  0.0));
			Node node3 = network.createAndAddNode(scenario.createId("3"), scenario.createCoord( 0.0,1000.0));
			Node node4 = network.createAndAddNode(scenario.createId("4"), scenario.createCoord( 0.0,1100.0));

			Set<String> allowedModes = new HashSet<String>(); allowedModes.addAll(Arrays.asList("cars","bicycles"));

			link1 = network.createAndAddLink(scenario.createId("-1"), node1, node2, 1000, 25, 600, 1, null, "22"); //capacity is 1 PCU per min.
			link2 = network.createAndAddLink(scenario.createId("0"), node2, node3, 1000, 25, 100, 1, null, "22");	
			link3 = network.createAndAddLink(scenario.createId("1"), node3, node4, 1000, 25, 600, 1, null, "22");

			population = scenario.getPopulation();
			new NetworkWriter(network).write("./output/network.xml");
			new ConfigWriter(config).write("./output/config.xml");
		}
	}
	private static class PersonLinkTravelTimeEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

		private final Map<Id, Map<Id, Double>> personLinkTravelTimes;

		public PersonLinkTravelTimeEventHandler(Map<Id, Map<Id, Double>> agentTravelTimes) {
			this.personLinkTravelTimes = agentTravelTimes;
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			System.out.println(event.toString());
			Map<Id, Double> travelTimes = this.personLinkTravelTimes.get(event.getPersonId());
			if (travelTimes == null) {
				travelTimes = new HashMap<Id, Double>();
				this.personLinkTravelTimes.put(event.getPersonId(), travelTimes);
			}
			travelTimes.put(event.getLinkId(), Double.valueOf(event.getTime()));
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			System.out.println(event.toString());
			Map<Id, Double> travelTimes = this.personLinkTravelTimes.get(event.getPersonId());
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
