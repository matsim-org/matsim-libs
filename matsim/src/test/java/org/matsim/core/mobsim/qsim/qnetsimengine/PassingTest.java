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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
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
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
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
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.*;


/**
 * Tests that a faster vehicle can pass slower vehicle on the same link
 */
public class PassingTest {

	@Rule public MatsimTestUtils helper = new MatsimTestUtils();
	
	/**
	 * A bike enters at t=0; and a car at t=5sec link length = 1000m
	 * Assume car speed = 20 m/s, bike speed = 5 m/s
	 * tt_car = 50 sec; tt_bike = 200 sec
	 */
	@Test 
	public void test4PassingInFreeFlowState(){

		SimpleNetwork net = new SimpleNetwork();

		//=== build plans; two persons; one with car and another with bike; car leave 5 secs after bike
		String transportModes [] = new String [] {"bike","car"};

		for(int i=0;i<2;i++){
			Id<Person> id = Id.create(i, Person.class);
			Person p = net.population.getFactory().createPerson(id);
			Plan plan = net.population.getFactory().createPlan();
			p.addPlan(plan);
			Activity a1 = net.population.getFactory().createActivityFromLinkId("h", net.link1.getId());
			a1.setEndTime(8*3600+i*5);
			Leg leg = net.population.getFactory().createLeg(transportModes[i]);
			plan.addActivity(a1);
			plan.addLeg(leg);
			LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
			NetworkRoute route = (NetworkRoute) factory.createRoute(net.link1.getId(), net.link3.getId());
			route.setLinkIds(net.link1.getId(), Arrays.asList(net.link2.getId()), net.link3.getId());
			leg.setRoute(route);

			Activity a2 = net.population.getFactory().createActivityFromLinkId("w", net.link3.getId());
			plan.addActivity(a2);
			net.population.addPerson(p);
		}

		PersonLinkTravelTimeEventHandler handler = new PersonLinkTravelTimeEventHandler();
		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(handler);

		QSim qSim = createQSim(net,manager);
		qSim.run();

		Map<Id<Person>, Map<Id<Link>, Double>> personLinkTravelTimes =  handler.getPersonId2LinkTravelTime();

		Map<Id<Link>, Double> travelTime1 = personLinkTravelTimes.get(Id.create("0", Person.class));
		Map<Id<Link>, Double> travelTime2 = personLinkTravelTimes.get(Id.create("1", Person.class));

		int bikeTravelTime = travelTime1.get(Id.create("2", Link.class)).intValue(); 
		int carTravelTime = travelTime2.get(Id.create("2", Link.class)).intValue();

		Assert.assertEquals("Wrong car travel time", 51, carTravelTime);
		Assert.assertEquals("Wrong bike travel time", 201, bikeTravelTime);
		Assert.assertEquals("Passing is not implemented", 150, bikeTravelTime-carTravelTime);

	}

	/**
	 * This is the same test as above. The only difference is way of inserting vehicle type info. 
	 * Here, vehicle types are inserted into scenario.
	 */
	@Test 
	public void test4VehicleTypesInScenario(){

		SimpleNetwork net = new SimpleNetwork();

		net.scenario.getConfig().qsim().setUseDefaultVehicles(false);

		Map<String, VehicleType> mode2VehType = getVehicleTypeInfo();

		for(String str:mode2VehType.keySet()){
			net.scenario.getVehicles().addVehicleType(mode2VehType.get(str));
		}

		//=== build plans; two persons; one with car and another with bike; car leave 5 secs after bike
		String transportModes [] = new String [] {"bike","car"};

		for(int i=0;i<2;i++){
			Id<Person> id = Id.create(i, Person.class);
			Person p = net.population.getFactory().createPerson(id);
			Plan plan = net.population.getFactory().createPlan();
			p.addPlan(plan);
			Activity a1 = net.population.getFactory().createActivityFromLinkId("h", net.link1.getId());
			a1.setEndTime(8*3600+i*5);
			Leg leg = net.population.getFactory().createLeg(transportModes[i]);
			plan.addActivity(a1);
			plan.addLeg(leg);
			LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
			NetworkRoute route = (NetworkRoute) factory.createRoute(net.link1.getId(), net.link3.getId());
			route.setLinkIds(net.link1.getId(), Arrays.asList(net.link2.getId()), net.link3.getId());
			leg.setRoute(route);

			Activity a2 = net.population.getFactory().createActivityFromLinkId("w", net.link3.getId());
			plan.addActivity(a2);
			net.population.addPerson(p);

			Id<Vehicle> vehId = Id.create(i,Vehicle.class);
			Vehicle veh = VehicleUtils.getFactory().createVehicle(vehId, mode2VehType.get(transportModes[i]));
			net.scenario.getVehicles().addVehicle(veh);

		}

		ActivityParams ap_h = new ActivityParams("h");
		ActivityParams ap_w = new ActivityParams("w");
		ap_h.setTypicalDuration(1*3600);
		ap_w.setTypicalDuration(1*3600);
		net.scenario.getConfig().planCalcScore().addActivityParams(ap_h);
		net.scenario.getConfig().planCalcScore().addActivityParams(ap_w);
		net.scenario.getConfig().controler().setWriteEventsInterval(0);
		net.scenario.getConfig().controler().setLastIteration(0);
		net.scenario.getConfig().controler().setOutputDirectory(helper.getOutputDirectory());
		
		Controler cntrlr = new Controler(net.scenario);
		cntrlr.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		cntrlr.getConfig().controler().setCreateGraphs(false);
        cntrlr.setDumpDataAtEnd(false);
		
		TravelTimeControlerListner travelTimeCntrlrListner = new TravelTimeControlerListner();

		cntrlr.addControlerListener(travelTimeCntrlrListner); 
		cntrlr.run();

		Map<Id<Person>, Map<Id<Link>, Double>> personLinkTravelTimes = travelTimeCntrlrListner.getPersonId2Time();

		Map<Id<Link>, Double> travelTime1 = personLinkTravelTimes.get(Id.create("0", Person.class));
		Map<Id<Link>, Double> travelTime2 = personLinkTravelTimes.get(Id.create("1", Person.class));

		int bikeTravelTime = travelTime1.get(Id.create("2", Link.class)).intValue(); 
		int carTravelTime = travelTime2.get(Id.create("2", Link.class)).intValue();

		Assert.assertEquals("Wrong car travel time", 51, carTravelTime);
		Assert.assertEquals("Wrong bike travel time", 201, bikeTravelTime);

		Assert.assertEquals("Passing is not implemented", 150, bikeTravelTime-carTravelTime);

	}

	private class TravelTimeControlerListner implements StartupListener, IterationEndsListener {

		Map<Id<Person>, Map<Id<Link>, Double>> personLinkTravelTimes = new HashMap<Id<Person>, Map<Id<Link>,Double>>();
		PersonLinkTravelTimeEventHandler hand;

		@Override
		public void notifyStartup(StartupEvent event) {

			EventsManager eventsManager = event.getControler().getEvents();
			hand = new PersonLinkTravelTimeEventHandler();
			eventsManager.addHandler(hand);
		}

		public Map<Id<Person>, Map<Id<Link>, Double>> getPersonId2Time(){
			return this.personLinkTravelTimes;
		}

		@Override
		public void notifyIterationEnds(IterationEndsEvent event) {
			this.personLinkTravelTimes = this.hand.getPersonId2LinkTravelTime();
		}
	}

	private QSim createQSim (SimpleNetwork net, EventsManager eventsManager){
		Scenario scenario = net.scenario;
		QSim qSim1 = new QSim(scenario, eventsManager);
		ActivityEngine activityEngine = new ActivityEngine(eventsManager, qSim1.getAgentCounter());
		qSim1.addMobsimEngine(activityEngine);
		qSim1.addActivityHandler(activityEngine);

		QNetsimEngine netsimEngine = new QNetsimEngine(qSim1);
		qSim1.addMobsimEngine(netsimEngine);
		qSim1.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine(scenario, eventsManager);
		qSim1.addMobsimEngine(teleportationEngine);
		QSim qSim = qSim1;
		

		AgentFactory agentFactory = new DefaultAgentFactory(qSim);
		PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), agentFactory, qSim);
		agentSource.setModeVehicleTypes(getVehicleTypeInfo());
		qSim.addAgentSource(agentSource);
		return qSim;
	}

	private Map<String, VehicleType> getVehicleTypeInfo() {
		Map<String, VehicleType> modeVehicleTypes = new HashMap<String, VehicleType>();

		VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("car", VehicleType.class));
		car.setMaximumVelocity(20);
		car.setPcuEquivalents(1.0);
		modeVehicleTypes.put("car", car);

		VehicleType bike = VehicleUtils.getFactory().createVehicleType(Id.create("bike", VehicleType.class));
		bike.setMaximumVelocity(5);
		bike.setPcuEquivalents(0.25);
		modeVehicleTypes.put("bike", bike);
		return modeVehicleTypes;
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
			config.qsim().setMainModes(Arrays.asList("car","bike"));
			config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ.name());

			network = (NetworkImpl) scenario.getNetwork();
			this.network.setCapacityPeriod(Time.parseTime("1:00:00"));
			double x = -100.0;
			Node node1 = network.createAndAddNode(Id.create("1", Node.class), new Coord(x, 0.0));
			Node node2 = network.createAndAddNode(Id.create("2", Node.class), new Coord(0.0, 0.0));
			Node node3 = network.createAndAddNode(Id.create("3", Node.class), new Coord(0.0, 1000.0));
			Node node4 = network.createAndAddNode(Id.create("4", Node.class), new Coord(0.0, 1100.0));

			Set<String> allowedModes = new HashSet<String>(); 
			allowedModes.addAll(Arrays.asList("pt","motorbike"));

			link1 = network.createAndAddLink(Id.create("1", Link.class), node1, node2, 100, 25, 60, 1, null, "22"); //capacity is 1 PCU per min.
			link2 = network.createAndAddLink(Id.create("2", Link.class), node2, node3, 1000, 25, 60, 1, null, "22");	
			link3 = network.createAndAddLink(Id.create("3", Link.class), node3, node4, 100, 25, 60, 1, null, "22");
			
			link1.setAllowedModes(allowedModes);
			link2.setAllowedModes(allowedModes);
			link3.setAllowedModes(allowedModes);
			
			
			population = scenario.getPopulation();
		}
	}
	private static class PersonLinkTravelTimeEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

		private final Map<Id<Person>, Map<Id<Link>, Double>> personLinkTravelTimes;

		public PersonLinkTravelTimeEventHandler() {
			this.personLinkTravelTimes = new HashMap<Id<Person>, Map<Id<Link>,Double>>();
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

		public Map<Id<Person>, Map<Id<Link>, Double>> getPersonId2LinkTravelTime(){
			return this.personLinkTravelTimes;
		}
	}
}
