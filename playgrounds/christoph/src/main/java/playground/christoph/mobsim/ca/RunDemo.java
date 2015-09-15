/* *********************************************************************** *
 * project: org.matsim.*
 * RunDemo.java
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

package playground.christoph.mobsim.ca;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimNetwork;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class RunDemo {

	private static final Logger log = Logger.getLogger(RunDemo.class);
	
	public static void main(String[] args) {
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		createNetwork(scenario);
		
		createPopulation(scenario);
		
		createAndRunCA(scenario);
	}
	
	private static void createNetwork(Scenario scenario) {
		
		NetworkFactory networkFactory = scenario.getNetwork().getFactory();

		Node n0 = networkFactory.createNode(Id.create("n0", Node.class), new Coord(0.0, 0.0));
		Node n1 = networkFactory.createNode(Id.create("n1", Node.class), new Coord(1000.0, 0.0));
		Node n2 = networkFactory.createNode(Id.create("n2", Node.class), new Coord(2000.0, 0.0));
		Node n3 = networkFactory.createNode(Id.create("n3", Node.class), new Coord(3000.0, 0.0));
		
		scenario.getNetwork().addNode(n0);
		scenario.getNetwork().addNode(n1);
		scenario.getNetwork().addNode(n2);
		scenario.getNetwork().addNode(n3);
		
		Link l0 = networkFactory.createLink(Id.create("l0", Link.class), n0, n1);
		Link l1 = networkFactory.createLink(Id.create("l1", Link.class), n1, n2);
		Link l2 = networkFactory.createLink(Id.create("l2", Link.class), n2, n3);
		
		l0.setLength(1000.0);
		l1.setLength(1000.0);
		l2.setLength(1000.0);
		
		scenario.getNetwork().addLink(l0);
		scenario.getNetwork().addLink(l1);
		scenario.getNetwork().addLink(l2);
	}
	
	private static void createPopulation(Scenario scenario) {
		
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		
		Person p0 = populationFactory.createPerson(Id.create("p0", Person.class));
		Plan plan = populationFactory.createPlan();
		Activity from = populationFactory.createActivityFromLinkId("home", Id.create("l0", Link.class));
		from.setStartTime(0.0);
		from.setEndTime(8.0*3600);
		Leg leg = populationFactory.createLeg(TransportMode.car);
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		linkIds.add(Id.create("l1", Link.class));
		Route route = new LinkNetworkRouteImpl(Id.create("l0", Link.class), linkIds, Id.create("l2", Link.class));
		leg.setRoute(route);
		Activity to = populationFactory.createActivityFromLinkId("home", Id.create("l1", Link.class));
		plan.addActivity(from);
		plan.addLeg(leg);
		plan.addActivity(to);
		
		p0.addPlan(plan);
		
		scenario.getPopulation().addPerson(p0);
	}
	
	private static void createAndRunCA(Scenario scenario) {
		
		double spatialResolution = 7.5;
		double timeStep = 1.0;
		double startTime = 0.0;
		double endTime = 24.0 * 3600;
		
		CA ca = new CA(scenario.getNetwork(), spatialResolution, timeStep, startTime, endTime);
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(new EventsPrinter());
		QSim sim = new QSim(scenario, eventsManager);
		
		ca.init(createAgents(scenario, sim, ca.getNetsimNetwork()));
		ca.simulate();
	}
	
	private static Map<Id, CAAgent> createAgents(Scenario scenario, Netsim netsim, NetsimNetwork network) {
		
		Map<Id, CAAgent> agents = new LinkedHashMap<Id, CAAgent>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			MobsimAgent mobsimAgent = new PersonDriverAgentImpl(person.getSelectedPlan(), netsim);
			CAAgent caAgent = new CAAgent(mobsimAgent, network);
			agents.put(mobsimAgent.getId(), caAgent);
		}
		
		return agents;
	}
	
	private static class EventsPrinter implements BasicEventHandler {

		@Override
		public void reset(int iteration) {
			// nothing to do here
		}

		@Override
		public void handleEvent(Event event) {
			log.info(event.toString());
		}
		
	}
}
