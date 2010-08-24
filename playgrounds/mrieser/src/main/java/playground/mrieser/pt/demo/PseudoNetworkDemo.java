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

package playground.mrieser.pt.demo;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.transitSchedule.TransitScheduleReaderV1;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.vis.otfvis.OTFVisMobsimFeature;
import org.xml.sax.SAXException;

public class PseudoNetworkDemo {

	public static void main(final String[] args) {
		String networkFile = null;
		String transitScheduleFile = null;
		if (args.length == 1) {
			transitScheduleFile = args[0];
		} else {
			networkFile = "test/input/org/matsim/transitSchedule/TransitScheduleReaderTest/network.xml";
//			networkFile = "../thesis-data/application/network.oevModellZH.xml";
			transitScheduleFile = "test/input/org/matsim/transitSchedule/TransitScheduleReaderTest/transitSchedule.xml";
//			transitScheduleFile = "test/input/org/matsim/transitSchedule/TransitScheduleReaderTest/transitScheduleNoLinks.xml";
//			transitScheduleFile = "../thesis-data/examples/berta/schedule.xml";
//			transitScheduleFile = "/Users/cello/Desktop/Mohit/berlinSchedule.xml";
//			transitScheduleFile = "../thesis-data/application/zuerichSchedule.xml";
//			transitScheduleFile = "../thesis-data/application/transitSchedule.oevModellZH.xml";
		}

		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		scenario.getConfig().scenario().setUseVehicles(true);
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().getQSimConfigGroup().setSnapshotStyle("queue");

		NetworkLayer network = scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		if (networkFile != null) {
			try {
				new MatsimNetworkReader(scenario).parse(networkFile);
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		TransitSchedule schedule = scenario.getTransitSchedule();
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

		new CreatePseudoNetwork(schedule, network, "tr_").createNetwork();
//		NetworkFromTransitSchedule.createNetwork(schedule, network);

		network.getFactory().setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());
		Link link1 = network.getLinks().values().iterator().next();//getLink(scenario.createId("1"));

		Population population = scenario.getPopulation();
		Person person = population.getFactory().createPerson(new IdImpl(1));
		population.addPerson(person);
		Plan plan = population.getFactory().createPlan();
		person.addPlan(plan);
		Activity act = population.getFactory().createActivityFromLinkId("home", link1.getId());
		act.setEndTime(4*3600.0);
		plan.addActivity(act);
		Leg leg = population.getFactory().createLeg(TransportMode.walk);
		leg.setTravelTime(15*3600.0);
		leg.setRoute(network.getFactory().createRoute(TransportMode.walk, link1.getId(), link1.getId()));
		plan.addLeg(leg);
		plan.addActivity(population.getFactory().createActivityFromLinkId("home", link1.getId()));

		final EventsManagerImpl events = new EventsManagerImpl();
		EventWriterXML writer = new EventWriterXML("./output/testEvents.xml");
		events.addHandler(writer);

		final QSim sim = new QSim(scenario, events);
		new CreateVehiclesForSchedule(schedule, scenario.getVehicles()).run();
		sim.addFeature(new OTFVisMobsimFeature(sim));
		sim.run();
		writer.closeFile();
	}

}
