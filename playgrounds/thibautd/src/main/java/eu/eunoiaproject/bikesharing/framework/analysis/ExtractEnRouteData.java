/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractEnRouteData.java
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
package eu.eunoiaproject.bikesharing.framework.analysis;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

/**
 * @author thibautd
 */
public class ExtractEnRouteData {
	private static final Logger log =
		Logger.getLogger(ExtractEnRouteData.class);

	public static void main(final String[] args) {
		final String eventsFile = args[ 0 ];
		final String outFile = args[ 1 ];

		log.info( "Parse events..." );
		final EventsManager events = EventsUtils.createEventsManager();
		final BikeSharingEventsToNumberOfEnRouteBikes handler = new BikeSharingEventsToNumberOfEnRouteBikes(); 
		events.addHandler( handler );
		new MatsimEventsReader( events ).readFile( eventsFile );
		log.info( "Parse events... DONE" );

		handler.writeFile( outFile );

	}
}

