/* *********************************************************************** *
 * project: org.matsim.*
 * PseudoNetworkDemo.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.marcel.pt.demo;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.experimental.Scenario;
import org.matsim.core.api.experimental.ScenarioImpl;
import org.matsim.core.api.experimental.population.Population;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.Events;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

import org.xml.sax.SAXException;

import playground.marcel.OTFDemo;
import playground.marcel.pt.integration.ExperimentalTransitRouteFactory;
import playground.marcel.pt.integration.TransitQueueSimulation;
import playground.marcel.pt.transitSchedule.TransitScheduleImpl;
import playground.marcel.pt.transitSchedule.TransitScheduleReaderV1;
import playground.marcel.pt.transitSchedule.api.TransitSchedule;
import playground.marcel.pt.tryout.CreatePseudoNetwork;

public class PseudoNetworkDemo {

	private static final String SERVERNAME = "pseudoNetworkDemo";
	
	public static void main(String[] args) {
		String networkFile = null;
		String transitScheduleFile = null;
		if (args.length == 1) {
			transitScheduleFile = args[0]	;
		} else {
			networkFile = "test/input/playground/marcel/pt/transitSchedule/network.xml";
			transitScheduleFile = "test/input/playground/marcel/pt/transitSchedule/transitSchedule.xml";
//			transitScheduleFile = "../thesis-data/examples/berta/schedule.xml";
//			transitScheduleFile = "/Users/cello/Desktop/transitScheduleF.xml";
		}
		
		Scenario scenario = new ScenarioImpl();
		
		scenario.getConfig().simulation().setSnapshotStyle("queue");
		
		Network network = scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		if (networkFile != null) {
			try {
				new MatsimNetworkReader(network).parse(networkFile);
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		TransitSchedule schedule = new TransitScheduleImpl();
		try {
			new TransitScheduleReaderV1(schedule, network).readFile(transitScheduleFile);
		} catch (SAXException e1) {
			e1.printStackTrace();
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		network.getLinks().clear();
		network.getNodes().clear();
		
		new CreatePseudoNetwork(schedule, (NetworkLayer) network).run();
		
		network.getFactory().setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());
		Link link1 = network.getLink(scenario.createId("1"));

		Population population = scenario.getPopulation();
		PersonImpl person = population.getPopulationBuilder().createPerson(new IdImpl(1));
		population.getPersons().put(person.getId(), person);
		PlanImpl plan = population.getPopulationBuilder().createPlan(person);
		person.addPlan(plan);
		ActivityImpl act = population.getPopulationBuilder().createActivityFromLinkId("home", link1.getId());
		act.setEndTime(4*3600.0);
		plan.addActivity(act);
		LegImpl leg = population.getPopulationBuilder().createLeg(TransportMode.walk);
		leg.setTravelTime(15*3600.0);
		leg.setRoute(network.getFactory().createRoute(TransportMode.walk, link1, link1));
		plan.addLeg(leg);
		plan.addActivity(population.getPopulationBuilder().createActivityFromLinkId("home", link1.getId()));
		
		final Events events = new Events();
		EventWriterXML writer = new EventWriterXML("./output/testEvents.xml");
		events.addHandler(writer);
		
		final TransitQueueSimulation sim = new TransitQueueSimulation(network, population, events);
		sim.setTransitSchedule(schedule);
		sim.startOTFServer(SERVERNAME);
		OTFDemo.ptConnect(SERVERNAME);
		sim.run();
		writer.closeFile();
	}
	
}
