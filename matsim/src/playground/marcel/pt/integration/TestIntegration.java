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

import org.matsim.api.core.v01.ScenarioLoader;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.facilities.Facilities;
import org.matsim.core.config.Config;
import org.matsim.core.events.Events;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.run.OTFVis;
import org.xml.sax.SAXException;

import playground.marcel.pt.transitSchedule.TransitSchedule;
import playground.marcel.pt.transitSchedule.TransitScheduleReaderV1;


public class TestIntegration {

	public static void main(final String[] args) {
		final Config config = Gbl.createConfig(new String[] {"test/input/playground/marcel/pt/config.xml"});
		ScenarioImpl scenario = new ScenarioImpl(config);
		ScenarioLoader loader = new ScenarioLoader(scenario);
		loader.loadScenario();	

		final TransitSchedule schedule = new TransitSchedule();
		final Events events = new Events();
		EventWriterXML writer = new EventWriterXML("./output/testEvents.xml");
		events.addHandler(writer);
		NetworkLayer network = (NetworkLayer) scenario.getNetwork();
		Facilities facilities = scenario.getFacilities();
//		FacilityNetworkMatching.loadMapping(facilities, network, scenario.getWorld(), "../thesis-data/examples/minibln/facilityMatching.txt");
//		System.out.println(network.getLinks().size());
//		System.out.println(facilities.getFacilities().size());
		
		try {
			new TransitScheduleReaderV1(schedule, network, facilities).parse("test/input/playground/marcel/pt/transitSchedule/transitSchedule.xml");
			final TransitQueueSimulation sim = new TransitQueueSimulation((NetworkLayer) scenario.getNetwork(), scenario.getPopulation(), events);
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
	
}
