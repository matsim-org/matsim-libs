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
package playground.agarwalamit.mixedTraffic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import com.google.inject.Provider;

/**
 * @author amit
 */

public class VehicleType2ScenarioTest {

	public VehicleType2ScenarioTest() {
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		if(! IS_USING_MODIFIED_MOBSIM){
			scenario.getConfig().qsim().setVehiclesSource(VehiclesSource.fromVehiclesData);
		}
	}


	private static final boolean IS_USING_MODIFIED_MOBSIM = false;
	private Scenario scenario ;
	static String transportModes [] = new String [] {"bike","car"};
	Link link1;
	Link link2;
	Link link3;

	public static void main(String[] args) {

		VehicleType2ScenarioTest runTest = new VehicleType2ScenarioTest();
		runTest.createNetwork();
		runTest.createPlans();
		runTest.createConfig();

		final Controler cont = new Controler(runTest.scenario);
		cont.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

		if(IS_USING_MODIFIED_MOBSIM){
			cont.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindMobsim().toProvider(new Provider<Mobsim>() {
						@Override
						public Mobsim get() {
							return new modifiedMobsimFactory().createMobsim(cont.getScenario(), cont.getEvents());
						}
					});
				}
			});
		}
		cont.run();
		runTestHandler();
	}

	private void createNetwork(){
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();

		double x = -100.0;
		Node node1 = network.createAndAddNode(Id.create("1", Node.class), new Coord(x, 0.0));
		Node node2 = network.createAndAddNode(Id.create("2", Node.class), new Coord(0.0, 0.0));
		Node node3 = network.createAndAddNode(Id.create("3", Node.class), new Coord(0.0, 1000.0));
		Node node4 = network.createAndAddNode(Id.create("4", Node.class), new Coord(0.0, 1100.0));

		link1 = network.createAndAddLink(Id.create("1", Link.class), node1, node2, 100, 25, 60, 1, null, "22"); //capacity is 1 PCU per min.
		link2 = network.createAndAddLink(Id.create("2", Link.class), node2, node3, 1000, 25, 60, 1, null, "22");	
		link3 = network.createAndAddLink(Id.create("3", Link.class), node3, node4, 100, 25, 60, 1, null, "22");

		new NetworkWriter(network).write("./input/network.xml");
	}

	private void createPlans(){

		Population population = scenario.getPopulation();

		VehicleType bike = VehicleUtils.getFactory().createVehicleType(Id.create(transportModes[0], VehicleType.class));
		bike.setMaximumVelocity(5);
		bike.setPcuEquivalents(0.25);

		VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create(transportModes[1], VehicleType.class));
		car.setMaximumVelocity(20);
		car.setPcuEquivalents(1.0);

		VehicleType [] vehTypes = {bike, car};

		for(int i=0;i<2;i++){

			Id<Person> id = Id.create(i, Person.class);
			Person p = population.getFactory().createPerson(id);
			Plan plan = population.getFactory().createPlan();
			p.addPlan(plan);
			Activity a1 = population.getFactory().createActivityFromLinkId("h", link1.getId());
			a1.setEndTime(8*3600+i*5);
			Leg leg = population.getFactory().createLeg(transportModes[i]);
			plan.addActivity(a1);
			plan.addLeg(leg);
			LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
			NetworkRoute route = (NetworkRoute) factory.createRoute(link1.getId(), link3.getId());
			route.setLinkIds(link1.getId(), Arrays.asList(link2.getId()), link3.getId());
			leg.setRoute(route);

			Activity a2 = population.getFactory().createActivityFromLinkId("w", link3.getId());
			plan.addActivity(a2);
			population.addPerson(p);

			if(! IS_USING_MODIFIED_MOBSIM){
				Id<Vehicle> vId = Id.create(p.getId(),Vehicle.class);
				Vehicle v = VehicleUtils.getFactory().createVehicle(vId, vehTypes[i]);
			
				if(! scenario.getVehicles().getVehicleTypes().containsKey(vehTypes[i].getId())) {
					scenario.getVehicles().addVehicleType(vehTypes[i]);
				}
				scenario.getVehicles().addVehicle(v);
			}
		}

		new PopulationWriter(population).write("./input/plans.xml");
	}

	private void createConfig(){
		Config config = scenario.getConfig();
		config.qsim().setFlowCapFactor(1.0);
		config.qsim().setStorageCapFactor(1.0);
		config.qsim().setMainModes(Arrays.asList(transportModes));
//		config.plansCalcRoute().setNetworkModes(Arrays.asList(transportModes));
		config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ.name());

		config.network().setInputFile("./input/network.xml");
		config.plans().setInputFile("./input/plans.xml");

		config.controler().setOutputDirectory("./output/");
		config.controler().setLastIteration(0);
		config.controler().setWriteEventsInterval(1);

		ActivityParams homeAct = new ActivityParams("h");
		ActivityParams workAct = new ActivityParams("w");
		homeAct.setTypicalDuration(1*3600);
		workAct.setTypicalDuration(1*3600);

		config.planCalcScore().addActivityParams(homeAct);
		config.planCalcScore().addActivityParams(workAct);

		new ConfigWriter(config).write("./input/config.xml");
	}

	private static void runTestHandler (){
		String filename = "./output/ITERS/it.0/0.events.xml.gz";
		Map<Id<Person>, Map<Id<Link>, Double>> personLinkTravelTimes = new HashMap<Id<Person>, Map<Id<Link>, Double>>();
		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(new PersonLinkTravelTimeEventHandler(personLinkTravelTimes));
		MatsimEventsReader reader = new MatsimEventsReader(manager);
		reader.readFile(filename);

		Map<Id<Link>, Double> travelTime1 = personLinkTravelTimes.get(Id.create("0", Person.class));
		Map<Id<Link>, Double> travelTime2 = personLinkTravelTimes.get(Id.create("1", Person.class));

		int bikeTravelTime = travelTime1.get(Id.create("2", Link.class)).intValue(); 
		int carTravelTime = travelTime2.get(Id.create("2", Link.class)).intValue();

		System.out.println("bike travel time \t \t car travel time");
		System.out.println(bikeTravelTime +"\t \t " + carTravelTime);

		if(bikeTravelTime - carTravelTime !=150) Logger.getLogger(VehicleType2ScenarioTest.class).warn("Passing is not executed.");
		else Logger.getLogger(VehicleType2ScenarioTest.class).warn("Passing is executed.");
	}

	public static class modifiedMobsimFactory implements MobsimFactory {
		@Override
		public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {

			// construct the QSim:
			QSim qSim = new QSim(sc, eventsManager);

			// add the actsim engine:
			ActivityEngine activityEngine = new ActivityEngine(eventsManager, qSim.getAgentCounter());
			qSim.addMobsimEngine(activityEngine);
			qSim.addActivityHandler(activityEngine);

			QNetsimEngine netsimEngine = new QNetsimEngine(qSim) ;
			qSim.addMobsimEngine(netsimEngine);
			qSim.addDepartureHandler(netsimEngine.getDepartureHandler());

			TeleportationEngine teleportationEngine = new TeleportationEngine(sc, eventsManager);
			qSim.addMobsimEngine(teleportationEngine);

			AgentFactory agentFactory = new DefaultAgentFactory(qSim);
			PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);

			Map<String, VehicleType> modeVehicleTypes = new HashMap<String, VehicleType>();

			VehicleType bike = VehicleUtils.getFactory().createVehicleType(Id.create(transportModes[0], VehicleType.class));
			bike.setMaximumVelocity(5);
			bike.setPcuEquivalents(0.25);
			modeVehicleTypes.put(transportModes[0], bike);

			VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create(transportModes[1], VehicleType.class));
			car.setMaximumVelocity(20);
			car.setPcuEquivalents(1.0);
			modeVehicleTypes.put(transportModes[1], car);

			agentSource.setModeVehicleTypes(modeVehicleTypes);
			qSim.addAgentSource(agentSource);

			return qSim ;
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
