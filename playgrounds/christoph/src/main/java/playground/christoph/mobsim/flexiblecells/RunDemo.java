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

package playground.christoph.mobsim.flexiblecells;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
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
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriter;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.christoph.mobsim.flexiblecells.events.VXYEvent;
import playground.christoph.mobsim.flexiblecells.events.VXYEventsHandler;

public class RunDemo {

	private static final Logger log = Logger.getLogger(RunDemo.class);
	
	private static String path = "../../matsim/mysimulations/CA/";
	private static int populationSize = 200;
	private static double timeStepSize = 0.25;
	
	public static void main(String[] args) {
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		createNetwork(scenario);
		
		createPopulation(scenario);
		
		createAndRunSimulation(scenario);
	}
	
	private static void createNetwork(Scenario scenario) {
		
		NetworkFactory networkFactory = scenario.getNetwork().getFactory();
		
		Node n0 = networkFactory.createNode(scenario.createId("n0"), scenario.createCoord(   0.0,    0.0));
		Node n1 = networkFactory.createNode(scenario.createId("n1"), scenario.createCoord(1000.0,    0.0));
		Node n2 = networkFactory.createNode(scenario.createId("n2"), scenario.createCoord(2000.0,    0.0));
		Node n3 = networkFactory.createNode(scenario.createId("n3"), scenario.createCoord(3000.0,    0.0));
		Node n4 = networkFactory.createNode(scenario.createId("n4"), scenario.createCoord(4000.0,    0.0));
		Node n5 = networkFactory.createNode(scenario.createId("n5"), scenario.createCoord(5000.0,    0.0));
		Node n6 = networkFactory.createNode(scenario.createId("n6"), scenario.createCoord(4000.0, 1000.0));
		
		scenario.getNetwork().addNode(n0);
		scenario.getNetwork().addNode(n1);
		scenario.getNetwork().addNode(n2);
		scenario.getNetwork().addNode(n3);
		scenario.getNetwork().addNode(n4);
		scenario.getNetwork().addNode(n5);
		scenario.getNetwork().addNode(n6);
		
		Link l0 = networkFactory.createLink(scenario.createId("l0"), n0, n1);
		Link l1 = networkFactory.createLink(scenario.createId("l1"), n1, n2);
		Link l2 = networkFactory.createLink(scenario.createId("l2"), n2, n3);
		Link l3 = networkFactory.createLink(scenario.createId("l3"), n3, n4);
		Link l4 = networkFactory.createLink(scenario.createId("l4"), n4, n5);
		Link l5 = networkFactory.createLink(scenario.createId("l5"), n4, n6);
		
		l0.setLength(1000.0);
		l1.setLength(1000.0);
		l2.setLength(1000.0);
		l3.setLength(1000.0);
		l4.setLength(1000.0);
		l5.setLength(1000.0);

		l0.setFreespeed(120.0/3.6);	// allow many vehicle to leave link in one time-step for backspill
		l1.setFreespeed(50.0/3.6);
		l2.setFreespeed(20.0/3.6);
		l3.setFreespeed(50.0/3.6);
		l4.setFreespeed(50.0/3.6);
		l5.setFreespeed(50.0/3.6);
		
		l0.setCapacity(4000.0);	// allow many vehicle to leave link in one time-step for backspill
		l1.setCapacity(2000.0);
		l2.setCapacity(500.0);
		l3.setCapacity(2000.0);
		l4.setCapacity(2000.0);
		l5.setCapacity(2000.0);
		
		scenario.getNetwork().addLink(l0);
		scenario.getNetwork().addLink(l1);
		scenario.getNetwork().addLink(l2);
		scenario.getNetwork().addLink(l3);
		scenario.getNetwork().addLink(l4);
		scenario.getNetwork().addLink(l5);
		
		new NetworkWriter(scenario.getNetwork()).write(path + "network.xml");
	}
	
	private static void createPopulation(Scenario scenario) {
		
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		
		for (int i = 0; i < populationSize; i++) {
			Id fromLinkId = scenario.createId("l0");
			Id endLinkId;
			if (MatsimRandom.getRandom().nextBoolean()) endLinkId = scenario.createId("l4");
			else endLinkId = scenario.createId("l5");
			Person p0 = populationFactory.createPerson(scenario.createId("p" + i));
			Plan plan = populationFactory.createPlan();
			Activity from = populationFactory.createActivityFromLinkId("home", fromLinkId);
			from.setStartTime(0.0);
			from.setEndTime(8.0*3600 + Math.round(MatsimRandom.getRandom().nextDouble() * 600));
			Leg leg = populationFactory.createLeg(TransportMode.walk);
			List<Id> linkIds = new ArrayList<Id>();
			linkIds.add(scenario.createId("l1"));
			linkIds.add(scenario.createId("l2"));
			linkIds.add(scenario.createId("l3"));
			Route route = new LinkNetworkRouteImpl(scenario.createId("l0"), linkIds, endLinkId);
			leg.setRoute(route);
			Activity to = populationFactory.createActivityFromLinkId("home", endLinkId);
			plan.addActivity(from);
			plan.addLeg(leg);
			plan.addActivity(to);
			
			p0.addPlan(plan);
			
			scenario.getPopulation().addPerson(p0);			
		}
		
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(path + "plans.xml");
	}
	
	private static void createAndRunSimulation(Scenario scenario) {
		
		double startTime = 0.0;
		double endTime = 24.0 * 3600;
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
//		eventsManager.addHandler(new EventsPrinter());
		eventsManager.addHandler(new EventWriterXML(path + "events.xml"));
		eventsManager.addHandler(new EventsWriterVXY(path + "eventsVXY.txt"));
		
		scenario.getConfig().addQSimConfigGroup(new QSimConfigGroup());
		scenario.getConfig().getQSimConfigGroup().setTimeStepSize(timeStepSize);
		scenario.getConfig().getQSimConfigGroup().setStartTime(startTime);
		scenario.getConfig().getQSimConfigGroup().setEndTime(endTime);
		
		QSim qSim = (QSim) new QSimFactory().createMobsim(scenario, eventsManager);
		FlexibleCellSimEngine caEngine = new FlexibleCellSimEngineFactory().createFlexibleCellSimEngine(qSim);
		qSim.addMobsimEngine(caEngine);
        qSim.addDepartureHandler(new FlexibleCellDepartureHandler(caEngine, CollectionUtils.stringToSet(TransportMode.walk)));
		
        qSim.run();
        
        // this triggers the closeFile() method in the writers
        eventsManager.resetHandlers(0);
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
	
	private static class EventsWriterVXY implements EventWriter, VXYEventsHandler {
		
		private BufferedWriter out = null;

		public EventsWriterVXY(final String filename) {
			init(filename);
		}

		@Override
		public void closeFile() {
			if (this.out != null)
				try {
					this.out.close();
					this.out = null;
				} catch (IOException e) {
					Gbl.errorMsg(e);
				}
		}

		public void init(final String outfilename) {
			closeFile();

			try {
				this.out = IOUtils.getBufferedWriter(outfilename);
				StringBuilder eventVXY = new StringBuilder(180);
				eventVXY.append("time");
				eventVXY.append("\t");
				eventVXY.append("id");
				eventVXY.append("\t");
				eventVXY.append("x");
				eventVXY.append("\t");
				eventVXY.append("y");
				eventVXY.append("\t");
				eventVXY.append("v");
				eventVXY.append("\n");
				this.out.write(eventVXY.toString());
			} catch (IOException e) {
				Gbl.errorMsg(e);
			}
		}

		@Override
		public void reset(final int iter) {
			closeFile();
		}

		@Override
		public void handleEvent(VXYEvent event) {
			try {
				StringBuilder eventVXY = new StringBuilder(180);
				eventVXY.append(event.getTime());
				eventVXY.append("\t");
				eventVXY.append(event.getPersonId().toString());
				eventVXY.append("\t");
				eventVXY.append(event.getX());
				eventVXY.append("\t");
				eventVXY.append(event.getY());
				eventVXY.append("\t");
				eventVXY.append(event.getV());
				eventVXY.append("\n");
				this.out.write(eventVXY.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
