/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractLegDurations.java
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
package playground.thibautd.scripts;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author thibautd
 */
public class ExtractLegDurations {
	public static void main(final String[] args) throws IOException {
		final String eventsFile = args[ 0 ];
		final String outFile = args[ 1 ];

		final EventsManager events = EventsUtils.createEventsManager();

		final BufferedWriter writer = IOUtils.getBufferedWriter( outFile );
		writer.write( "personId\tmode\tstart\tend\tduration" );

		final Map<Id, Double> departures = new HashMap<Id, Double>();
		events.addHandler( new PersonDepartureEventHandler() {
			@Override
			public void reset(int iteration) {}

			@Override
			public void handleEvent(final PersonDepartureEvent event) {
				departures.put( event.getPersonId() , event.getTime() );
			}
		} );

		events.addHandler( new PersonArrivalEventHandler() {
			@Override
			public void reset(int iteration) {}

			@Override
			public void handleEvent(final PersonArrivalEvent event) {
				final double departure = departures.remove( event.getPersonId() );
				final double arrival = event.getTime();

				try {
					writer.newLine();
					writer.write( event.getPersonId()+"\t"+event.getLegMode()+"\t"+departure+"\t"+arrival+"\t"+(arrival-departure) );
				}
				catch (IOException e) {
					throw new UncheckedIOException( e );
				}
			}
		} );

		new MatsimEventsReader( events ).readFile( eventsFile );
		writer.close();
	}

}

