/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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


package playground.polettif.multiModalMap.osm.osm2mts;

import org.apache.log4j.Logger;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.multiModalMap.osm.core.OsmParser;

/**
 * Convert available public transit data from OSM to a MATSim Transit Schedule (bus stops, transitRoutes and routeProfiles).
 *
 * Creates an unmapped schedule with missing departures.
 *
 * @polettif
 */
public class OSM2MTS {

	private static final Logger log = Logger.getLogger(OSM2MTS.class);

	TransitSchedule schedule;

	public static void main(final String[] args) {
		String filenameOSM = "C:/Users/polettif/Desktop/basel/osmInput/germany.osm";
		String filenameMTS = "C:/Users/polettif/Desktop/basel/mts/germany.xml";

		OSM2MTS osm2mts = new OSM2MTS();
		osm2mts.run(filenameOSM, filenameMTS);
	}

	public void run(String filenameOSMinput, String filenameMTSoutput) {
		schedule = new TransitScheduleFactoryImpl().createTransitSchedule();

		OSM2MTSHandler handler = new OSM2MTSHandler(schedule);

		OsmParser parser = new OsmParser();
		parser.addHandler(handler);
		parser.readFile(filenameOSMinput);

		log.info("OSM file parsed. Writing schedule to file...");
		new TransitScheduleWriter(schedule).writeFile(filenameMTSoutput);
	}

}