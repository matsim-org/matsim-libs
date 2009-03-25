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

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.config.Config;
import org.matsim.core.api.facilities.Facilities;
import org.matsim.events.Events;
import org.matsim.events.algorithms.EventWriterXML;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.run.OTFVis;
import org.xml.sax.SAXException;

import playground.marcel.pt.transitSchedule.TransitSchedule;
import playground.marcel.pt.transitSchedule.TransitScheduleReader;


public class TestIntegration {

	public static void main(final String[] args) {
		final Config config = Gbl.createConfig(new String[] {"test/input/playground/marcel/pt/config.xml"});
		final ScenarioImpl scenario = new ScenarioImpl(config);
		final TransitSchedule schedule = new TransitSchedule();
		final Events events = new Events();
		EventWriterXML writer = new EventWriterXML("./output/testEvents.xml");
		events.addHandler(writer);
		NetworkLayer network = scenario.getNetwork();
		Facilities facilities = scenario.getFacilities();
//		FacilityNetworkMatching.loadMapping(facilities, network, scenario.getWorld(), "../thesis-data/examples/minibln/facilityMatching.txt");
//		System.out.println(network.getLinks().size());
//		System.out.println(facilities.getFacilities().size());
		
		try {
			new TransitScheduleReader(schedule, network, facilities).parse("test/input/playground/marcel/pt/transitSchedule/transitSchedule.xml");
		
			final TransitQueueSimulation sim = new TransitQueueSimulation(scenario.getNetwork(), scenario.getPopulation(), events);
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
