/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractHitchHikersWaitingTimes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

/**
 * @author thibautd
 */
public class ExtractHitchHikersWaitingTimes {
	public static void main(final String[] args) {
		String eventsFile = args[ 0 ];
		String outFile = args[ 1 ];

		Handler handler = new Handler( outFile );
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler( handler );
		(new MatsimEventsReader( events )).readFile( eventsFile );
		handler.finish();
	}

	private static class Handler implements BasicEventHandler {
		private final Map<String, Double> waitStarts = new HashMap<String, Double>();
		private final BufferedWriter writer;

		public Handler(final String outFile) {
			writer = IOUtils.getBufferedWriter( outFile );
		}

		@Override
		public void reset(final int iteration) {
		}

		@Override
		public void handleEvent(final Event event) {
			if (event.getAttributes().get( EventImpl.ATTRIBUTE_TYPE ).equals( "passengerStartsWaiting" )) {
				waitStarts.put(
						event.getAttributes().get( ActivityStartEvent.ATTRIBUTE_PERSON ),
						event.getTime());
			}
			else if (event.getAttributes().get( EventImpl.ATTRIBUTE_TYPE ).equals( "passengerEndsWaiting" )) {
				Double start = waitStarts.remove( event.getAttributes().get( ActivityEndEvent.ATTRIBUTE_PERSON ) );

				try {
					writer.write( ""+(event.getTime() - start) );
					writer.newLine();
				} catch (IOException e) {
					throw new UncheckedIOException( e );
				}
			}
		}

		public void finish() {
			final int n = waitStarts.size();
			for (int i=0; i < n; i++) {
				try {
					writer.write( "inf" );
					writer.newLine();
				} catch (IOException e) {
					throw new UncheckedIOException( e );
				}
			}
			try {
				writer.close();
			} catch (IOException e) {
				throw new UncheckedIOException( e );
			}

		}
	}
}

