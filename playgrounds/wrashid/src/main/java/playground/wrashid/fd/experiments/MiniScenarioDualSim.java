/* *********************************************************************** *
 * project: org.matsim.*
 * MiniScenario.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.wrashid.fd.experiments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.ParallelEventsManagerImpl;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.jdeqsim.JDEQSimulationFactory;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;

import playground.wrashid.fd.DensityInfoCollectorDualSim;
import playground.wrashid.fd.MainFundamentalDiagram;
import playground.wrashid.fd.OutFlowInfoCollectorDualSim;

public class MiniScenarioDualSim {

	private static final Logger log = Logger.getLogger(MiniScenarioDualSim.class);
	
	public static void main(String[] args) {
		new MiniScenarioDualSim();
	}
	
	public MiniScenarioDualSim() {
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		createNetwork(scenario);
		createPopulation(scenario,100,0);
		runSimulation(scenario,300,0);
	}
	
	public static void createNetwork(Scenario scenario) {
		
		NetworkFactory factory = scenario.getNetwork().getFactory();
		
		Node n0 = factory.createNode(scenario.createId("n0"), scenario.createCoord(0.0, 0.0));
		Node n1 = factory.createNode(scenario.createId("n1"), scenario.createCoord(5000.0, 0.0));
		Node n2 = factory.createNode(scenario.createId("n2"), scenario.createCoord(5000.0, 5000.0));
		Node n3 = factory.createNode(scenario.createId("n3"), scenario.createCoord(0.0, 5000.0));
		
		Link l0 = factory.createLink(scenario.createId("l0"), n0, n1);
		Link l1 = factory.createLink(scenario.createId("l1"), n1, n2);
		Link l2 = factory.createLink(scenario.createId("l2"), n2, n3);
		Link l3 = factory.createLink(scenario.createId("l3"), n3, n0);
		
		l0.setFreespeed(80.0 / 3.6);
		l1.setFreespeed(80.0 / 3.6);
		l2.setFreespeed(80.0 / 3.6);
		l3.setFreespeed(80.0 / 3.6);
		
		l0.setLength(5000.0);
		l1.setLength(5000.0);
		l2.setLength(5000.0);
		l3.setLength(5000.0);
		
		l0.setCapacity(Double.MAX_VALUE);
		l1.setCapacity(2000.0);
		l2.setCapacity(Double.MAX_VALUE);
		l3.setCapacity(Double.MAX_VALUE);
				
		scenario.getNetwork().addNode(n0);
		scenario.getNetwork().addNode(n1);
		scenario.getNetwork().addNode(n2);
		scenario.getNetwork().addNode(n3);
		
		scenario.getNetwork().addLink(l0);
		scenario.getNetwork().addLink(l1);
		scenario.getNetwork().addLink(l2);
		scenario.getNetwork().addLink(l3);
		
		new NetworkWriter(scenario.getNetwork()).write("network.xml");
	}
	
	
	public static void createPopulation(Scenario scenario, int initialAgents,int agentIncrementPerHour) {
		
		PopulationFactory factory = scenario.getPopulation().getFactory();
		
		List<Id> linkIds = new ArrayList<Id>();
		for (int i = 0; i < 1000; i++) {
			linkIds.add(scenario.createId("l0"));
			linkIds.add(scenario.createId("l1"));
			linkIds.add(scenario.createId("l2"));
			linkIds.add(scenario.createId("l3"));
		}
		NetworkRoute route = (NetworkRoute) new LinkNetworkRouteFactory().createRoute(scenario.createId("l3"), scenario.createId("l0"));
		route.setLinkIds(scenario.createId("l3"), linkIds, scenario.createId("l0"));
		
		Random random = MatsimRandom.getLocalInstance();
		int p = 0;
		for (int hour = 0; hour < 24; hour++) {
			for (int pNum = 0; pNum < initialAgents+agentIncrementPerHour*hour; pNum++) {
				Person person = factory.createPerson(scenario.createId(String.valueOf(p++)));
				Plan plan = factory.createPlan();
				Activity from = factory.createActivityFromLinkId("home", scenario.createId("l3"));
				from.setEndTime(Math.round(3600*(hour + random.nextDouble())));
				Leg leg = factory.createLeg(TransportMode.car);
				leg.setRoute(route);
				Activity to = factory.createActivityFromLinkId("home", scenario.createId("l3"));
				plan.addActivity(from);
				plan.addLeg(leg);
				plan.addActivity(to);
				person.addPlan(plan);
				scenario.getPopulation().addPerson(person);
			}
		}
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write("population.xml");
		log.info("Created " + scenario.getPopulation().getPersons().size() + " persons");
	}
	
	public static void runSimulation(Scenario scenario,int binSizeInSeconds, int runId) {
		
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		//EventsManager eventsManager = new ParallelEventsManagerImpl(4);
		eventsManager.initProcessing();
		
		EventWriterXML eventsWriter = new EventWriterXML(Controler.FILENAME_EVENTS_XML);
		eventsManager.addHandler(eventsWriter);
		
		Map<Id, Link> links = new TreeMap<Id, Link>();
		links.put(scenario.createId("l0"), scenario.getNetwork().getLinks().get(scenario.createId("l0")));
		links.put(scenario.createId("l1"), scenario.getNetwork().getLinks().get(scenario.createId("l1")));
		links.put(scenario.createId("l2"), scenario.getNetwork().getLinks().get(scenario.createId("l2")));
		links.put(scenario.createId("l3"), scenario.getNetwork().getLinks().get(scenario.createId("l3")));
		
		
		

		boolean isJDEQSim=true;
		DensityInfoCollectorDualSim densityHandler = new DensityInfoCollectorDualSim(
				links, binSizeInSeconds,isJDEQSim);
		OutFlowInfoCollectorDualSim outflowHandler = new OutFlowInfoCollectorDualSim(
				links, binSizeInSeconds,isJDEQSim);
		eventsManager.addHandler(densityHandler);
		eventsManager.addHandler(outflowHandler);

		eventsManager.resetHandlers(0);
		eventsWriter.init(Controler.FILENAME_EVENTS_XML);
		
		
		
		
		scenario.getConfig().setParam("JDEQSim", "endTime", "96:00:00");
		scenario.getConfig().setParam("JDEQSim", "gapTravelSpeed", "5.0");	// instead of 15m/s
//		scenario.getConfig().setParam("JDEQSim", "squeezeTime", "10.0");// instead of 1800.0
//		scenario.getConfig().setParam("JDEQSim", "minimumInFlowCapacity", "100.0");	// instead of 1800.0
//		scenario.getConfig().setParam("JDEQSim", "storageCapacityFactor", "5.0");	// instead of 1.0
		Mobsim sim = new JDEQSimulationFactory().createMobsim(scenario, eventsManager);
		sim.run();
		
		/*
		QSimConfigGroup conf = new QSimConfigGroup();
		conf.setStartTime(0.0);
		conf.setEndTime(48*3600);
		//conf.setTrafficDynamics(QSimConfigGroup.TRAFF_DYN_W_HOLES);
		scenario.getConfig().addQSimConfigGroup(conf);
		Mobsim sim = new QSimFactory().createMobsim(scenario, eventsManager);
		sim.run();
		*/
		eventsManager.finishProcessing();
		eventsWriter.closeFile();
		
		HashMap<Id, double[]> densities = MainFundamentalDiagram.calculateDensities(links,
				densityHandler, binSizeInSeconds);

		MainFundamentalDiagram.printDensityAndOutFlow(densities, links, outflowHandler,false,runId);
	}
}
