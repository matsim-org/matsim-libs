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

package playground.polettif.boescpa.analysis.trips;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import playground.polettif.boescpa.lib.tools.NetworkUtils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Creates "trips" from events. 
 * 
 * @author boescpa
 * 
 * IMPORTANT: This is a further developed version of staheale's class 'Events2Trips'.
 * 
 */
public class EventsToTrips {
	private static Logger log = Logger.getLogger(EventsToTrips.class);

    public static void main(String[] args) {
        String eventsFile = args[0]; // Path to an events-File.
        String networkFile = args[1]; // Path to the network-File used for the simulation resulting in the events-File.
        String tripFile = args[2]; // Path to where the trip file should be written to.

        TripEventHandler.setAnonymizeTrips(true);
        List<Trip> trips = createTripsFromEvents(eventsFile, networkFile);

        log.info("Write trips...");
        new TripWriter().writeTrips(trips, tripFile);
        log.info("Write trips...done.");
    }

    public static List<Trip> createTripsFromEvents(String pathToEventsFile, String pathToNetworkFile) {
        log.info("Reading network xml file...");
        Network network = NetworkUtils.readNetwork(pathToNetworkFile);
        log.info("Reading network xml file...done.");
        return createTripsFromEvents(pathToEventsFile, network);
    }

	public static List<Trip> createTripsFromEvents(String pathToEventsFile, Network network) {
		TripEventHandler tripHandler = new TripEventHandler(network);
        tripHandler.reset(0);
        EventsManager events = EventsUtils.createEventsManager();
        events.addHandler(tripHandler);

		log.info("Reading events file...");
		if (pathToEventsFile.endsWith(".xml.gz")) { // if events-File is in the newer xml-format
			EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
			reader.parse(pathToEventsFile);
		}
		else {
			throw new IllegalArgumentException("Given events-file not of known format.");
		}
		log.info("Reading events file...done.");

        return tripHandler.getTrips();
	}
}
