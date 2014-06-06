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

package playground.boescpa.topdad.postprocessing;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Creates "trips" from events. 
 * 
 * @author boescpa
 * 
 * IMPORTANT: This is a further developed version of staheale's class 'Events2Trips'.
 * 
 */
public class MainTripCreator {
	
	private static Logger log = Logger.getLogger(MainTripCreator.class);
	
	public static void main(String[] args) {
		
		String eventsFile = args[0]; // Path to an events-File, e.g. "run.combined.150.events.xml.gz"
		String networkFile = args[1]; // Path to the network-File used for the simulation resulting in the above events-File, e.g. "multimodalNetwork2030final.xml.gz"
		EventsManager events = EventsUtils.createEventsManager();
		
		ScenarioImpl  scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);
		
		log.info("Reading network xml file...");
		MatsimNetworkReader NetworkReader = new MatsimNetworkReader(scenario);
		NetworkReader.readFile(networkFile);
		Network network = scenario.getNetwork();
		log.info("Reading network xml file...done.");

		TripHandler tripHandler = new TripHandler();
		events.addHandler(tripHandler);
		tripHandler.reset(0);

		log.info("Reading events file...");
		if (eventsFile.endsWith(".xml.gz")) { // if events-File is in the newer xml-format
			EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
			reader.parse(eventsFile);
		}
		else if (eventsFile.endsWith(".txt.gz")) {	// if events-File is in the older txt-format
			EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
			reader.readFile(eventsFile);
		}
		else {
			throw new IllegalArgumentException("Given events-file not of known format.");
		}
		log.info("Reading events file...done.");
		
		log.info("Postprocessing trips...");
		TripProcessing.printTrips(tripHandler, network, args[2]);
		TripProcessing.analyzeTripsTopdad(tripHandler, network, args[3]);
		log.info("Postprocessing trips...done.");
		
	}
	
}
