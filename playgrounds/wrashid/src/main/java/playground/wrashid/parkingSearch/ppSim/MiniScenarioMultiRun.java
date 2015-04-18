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

package playground.wrashid.parkingSearch.ppSim;

import java.util.ArrayList;
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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.RunnableMobsim;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;

public class MiniScenarioMultiRun {

	private static final Logger log = Logger
			.getLogger(MiniScenarioMultiRun.class);

	public static void main(String[] args) {
		new MiniScenarioMultiRun();
	}

	public MiniScenarioMultiRun() {

		int runId = 0;
		int agentsPerHour = 50;
		int binSizeInSeconds = 30;
		int gapSpeed = 1;

		String caption = "runId: " + runId + ", agentsPerHour: "
				+ agentsPerHour + ", binSizeInSeconds:" + binSizeInSeconds
				+ ", gapSpeed: " + gapSpeed;
		System.out.println(caption);
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		createNetwork(scenario);
		createPopulation(scenario, agentsPerHour);
		runSimulation(scenario, Integer.toString(gapSpeed), binSizeInSeconds,
				runId, caption);

	}

	public static void createNetwork(Scenario scenario) {

		NetworkFactory factory = scenario.getNetwork().getFactory();

		Node n0 = factory.createNode(Id.create("n0", Node.class),
				scenario.createCoord(0.0, 0.0));
		Node n1 = factory.createNode(Id.create("n1", Node.class),
				scenario.createCoord(200.0, 0.0));
		Node n2 = factory.createNode(Id.create("n2", Node.class),
				scenario.createCoord(200.0, 200.0));
		Node n3 = factory.createNode(Id.create("n3", Node.class),
				scenario.createCoord(0.0, 200.0));

		Link l0 = factory.createLink(Id.create("l0", Link.class), n0, n1);
		Link l1 = factory.createLink(Id.create("l1", Link.class), n1, n2);
		Link l2 = factory.createLink(Id.create("l2", Link.class), n2, n3);
		Link l3 = factory.createLink(Id.create("l3", Link.class), n3, n0);

		l0.setFreespeed(80.0 / 3.6);
		l1.setFreespeed(80.0 / 3.6);
		l2.setFreespeed(80.0 / 3.6);
		l3.setFreespeed(80.0 / 3.6);

		l0.setLength(200.0);
		l1.setLength(200.0);
		l2.setLength(200.0);
		l3.setLength(200.0);

		l0.setCapacity(2000.0);
		l1.setCapacity(2000.0);
		l2.setCapacity(2000.0);
		l3.setCapacity(2000.0);

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

	private static void createPopulation(Scenario scenario, int agentsPerHour) {

		PopulationFactory factory = scenario.getPopulation().getFactory();

		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		for (int i = 0; i < 10; i++) {
			linkIds.add(Id.create("l0", Link.class));
			linkIds.add(Id.create("l1", Link.class));
			linkIds.add(Id.create("l2", Link.class));
			linkIds.add(Id.create("l3", Link.class));
		}
		NetworkRoute route = (NetworkRoute) new LinkNetworkRouteFactory()
				.createRoute(Id.create("l3", Link.class), Id.create("l0", Link.class));
		route.setLinkIds(Id.create("13", Link.class), linkIds,
				Id.create("l0", Link.class));

		Random random = MatsimRandom.getLocalInstance();
		int p = 0;
		for (int hour = 0; hour < 24; hour++) {
			for (int pNum = 0; pNum < agentsPerHour; pNum++) {
				Person person = factory.createPerson(Id.create(String.valueOf(p++), Person.class));
				Plan plan = factory.createPlan();
				Activity from = factory.createActivityFromLinkId("home", Id.create("l3", Link.class));
				from.setEndTime(Math.round(3600 * (hour + random.nextDouble())));
				Leg leg = factory.createLeg(TransportMode.car);
				leg.setRoute(route);
				Activity to = factory.createActivityFromLinkId("home", Id.create("l3", Link.class));
				plan.addActivity(from);
				plan.addLeg(leg);
				plan.addActivity(to);
				person.addPlan(plan);
				scenario.getPopulation().addPerson(person);
			}
		}
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork())
				.write("population.xml");
		log.info("Created " + scenario.getPopulation().getPersons().size()
				+ " persons");
	}

	private static void runSimulation(Scenario scenario, String gapSpeed,
			int binSizeInSeconds, int runId, String caption) {

		EventsManager eventsManager = EventsUtils.createEventsManager();

		EventWriterXML eventsWriter = new EventWriterXML(
				Controler.FILENAME_EVENTS_XML);
		eventsManager.addHandler(eventsWriter);

		Map<Id, Link> links = new TreeMap<Id, Link>();
		links.put(Id.create("l0", Link.class), scenario.getNetwork().getLinks()
				.get(Id.create("l0", Link.class)));
		links.put(Id.create("l1", Link.class), scenario.getNetwork().getLinks()
				.get(Id.create("l1", Link.class)));
		links.put(Id.create("l2", Link.class), scenario.getNetwork().getLinks()
				.get(Id.create("l2", Link.class)));
		links.put(Id.create("l3", Link.class), scenario.getNetwork().getLinks()
				.get(Id.create("l3", Link.class)));

		eventsManager.resetHandlers(0);
		eventsWriter.init(Controler.FILENAME_EVENTS_XML);

		RunnableMobsim sim = new PPSim(scenario, eventsManager);
		sim.run();

	}
}
