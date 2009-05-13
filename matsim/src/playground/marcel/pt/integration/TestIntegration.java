/* *********************************************************************** *
 * project: org.matsim.*
 * TestIntegration.java
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

package playground.marcel.pt.integration;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioLoader;
import org.matsim.core.api.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.events.Events;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.NetworkLayer;
import org.matsim.run.OTFVis;
import org.xml.sax.SAXException;

import playground.marcel.pt.transitSchedule.TransitSchedule;
import playground.marcel.pt.transitSchedule.TransitScheduleReaderBerta;
import playground.marcel.pt.transitSchedule.TransitScheduleReaderV1;
import playground.marcel.pt.tryout.CreatePseudoNetwork;


public class TestIntegration {

	public static void main1(final String[] args) {
		try {
			TransitScheduleReaderBerta.main(null);
		} catch (SAXException e1) {
			e1.printStackTrace();
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		new CreatePseudoNetwork().run();
		
		ScenarioLoader sl = new ScenarioLoader("test/input/playground/marcel/pt/config.xml");
		Scenario scenario = sl.getScenario();
		final Config config = scenario.getConfig();
		
		config.network().setInputFile("../thesis-data/examples/berta/pseudoNetwork.xml");
		config.facilities().setInputFile("../thesis-data/examples/berta/facilities.xml");
		config.plans().setInputFile("../thesis-data/examples/berta/pseudoPerson.xml");
		config.simulation().setSnapshotPeriod(60);
		config.simulation().setEndTime(12.0*3600);
		
		NetworkLayer network = (NetworkLayer) scenario.getNetwork();
		network.getFactory().setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());

		sl.loadScenario();	

		final TransitSchedule schedule = new TransitSchedule();
		final Events events = new Events();
		EventWriterXML writer = new EventWriterXML("./output/testEvents.xml");
		events.addHandler(writer);
		ActivityFacilities facilities = scenario.getActivityFacilities();
//		FacilityNetworkMatching.loadMapping(facilities, network, scenario.getWorld(), "../thesis-data/examples/minibln/facilityMatching.txt");
//		System.out.println(network.getLinks().size());
//		System.out.println(facilities.getFacilities().size());
		
		try {
//			new TransitScheduleReaderV1(schedule, network, facilities).parse("test/input/playground/marcel/pt/transitSchedule/transitSchedule.xml");
			new TransitScheduleReaderV1(schedule, network, facilities).parse("../thesis-data/examples/berta/pseudoSchedule.xml");
			final TransitQueueSimulation sim = new TransitQueueSimulation((NetworkLayer) scenario.getNetwork(), scenario.getPopulation(), events, facilities);
			sim.setTransitSchedule(schedule);
			sim.run();
			OTFVis.playMVI(new String[] {"./otfvis.mvi"});
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		writer.closeFile();
	}
	
	public static void main(final String[] args) {
		ScenarioLoader sl = new ScenarioLoader("test/input/playground/marcel/pt/config.xml");
		Scenario scenario = sl.getScenario();
//		final Config config = scenario.getConfig();
//		
//		config.network().setInputFile("../thesis-data/examples/berta/pseudoNetwork.xml");
//		config.facilities().setInputFile("../thesis-data/examples/berta/facilities.xml");
//		config.plans().setInputFile("../thesis-data/examples/berta/pseudoPerson.xml");
//		config.simulation().setSnapshotPeriod(60);
//		config.simulation().setEndTime(12.0*3600);
		
		NetworkLayer network = (NetworkLayer) scenario.getNetwork();
		network.getFactory().setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());

		sl.loadScenario();	

		final TransitSchedule schedule = new TransitSchedule();
		final Events events = new Events();
		EventWriterXML writer = new EventWriterXML("./output/testEvents.xml");
		events.addHandler(writer);
		ActivityFacilities facilities = scenario.getActivityFacilities();
//		FacilityNetworkMatching.loadMapping(facilities, network, scenario.getWorld(), "../thesis-data/examples/minibln/facilityMatching.txt");
//		System.out.println(network.getLinks().size());
//		System.out.println(facilities.getFacilities().size());
		
		try {
			new TransitScheduleReaderV1(schedule, network, facilities).parse("test/input/playground/marcel/pt/transitSchedule/transitSchedule.xml");
//			new TransitScheduleReaderV1(schedule, network, facilities).parse("../thesis-data/examples/berta/pseudoSchedule.xml");
			final TransitQueueSimulation sim = new TransitQueueSimulation((NetworkLayer) scenario.getNetwork(), scenario.getPopulation(), events, facilities);
			sim.setTransitSchedule(schedule);
			sim.run();
//			OTFVis.playMVI(new String[] {"./otfvis.mvi"});
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		writer.closeFile();
	}

}
