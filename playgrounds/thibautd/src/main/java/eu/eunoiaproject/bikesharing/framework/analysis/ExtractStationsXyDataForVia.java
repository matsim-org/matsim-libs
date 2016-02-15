/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractStationsXyDataForVia.java
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
package eu.eunoiaproject.bikesharing.framework.analysis;

import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacilities;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacilitiesReader;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Uses {@link BikeSharingEventsToXyTable} to convert events to a tabular
 * file.
 * <br>
 * usage:
 * <tt>
 * ExtractStationsXyDataForVia path/to/bikeSharingFacilities.xml path/to/events.xml path/to/outputfile.xy
 * </tt>
 * @author thibautd
 */
public class ExtractStationsXyDataForVia {
	private static final Logger log =
		Logger.getLogger(ExtractStationsXyDataForVia.class);

	public static void main(final String[] args) throws IOException {
		final String facilitiesFile = args[ 0 ];
		final String eventsFile = args[ 1 ];
		final String outFile = args[ 2 ];

		log.info( "read bike sharing facilities..." );
		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new BikeSharingFacilitiesReader( scenario ).parse( facilitiesFile );
		log.info( "read bike sharing facilities... DONE" );

		log.info( "Parse events..." );
		final BufferedWriter writer = IOUtils.getBufferedWriter( outFile );
		final EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(
				new BikeSharingEventsToXyTable(
					(BikeSharingFacilities)
						scenario.getScenarioElement(
							BikeSharingFacilities.ELEMENT_NAME ),
					writer ) );
		log.info( "Parse events... DONE" );

		new MatsimEventsReader( events ).readFile( eventsFile );
		writer.close();
	}
}

