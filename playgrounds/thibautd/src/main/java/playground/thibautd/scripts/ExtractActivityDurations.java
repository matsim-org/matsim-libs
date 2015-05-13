/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractActivityDurations.java
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

/**
 * @author thibautd
 */
public class ExtractActivityDurations {
	public static void main(final String[] args) {
		final String eventFile = args[ 0 ];
		final String outFile = args[ 1 ];

		final Handler handler = new Handler( outFile );
		final EventsManager events = EventsUtils.createEventsManager();
		events.addHandler( handler );
		new EventsReaderXMLv1( events ).parse( eventFile );
		handler.close();
	}

	private static class Handler implements ActivityStartEventHandler, ActivityEndEventHandler {
		private final BufferedWriter writer;
		private final Map<Id, Double> starts = new HashMap<Id, Double>();

		public Handler(final String file) {
			this.writer = IOUtils.getBufferedWriter( file );

			try {
				writer.write( "agentId\tactType\tduration" );
			} catch (final IOException e) {
				throw new UncheckedIOException( e );
			}
		}

		public void close() {
			try {
				writer.close();
			} catch (final IOException e) {
				throw new UncheckedIOException( e );
			}
		}

		@Override
		public void reset(final int iteration) {}

		@Override
		public void handleEvent(final ActivityEndEvent event) {
			final Double start = starts.remove( event.getPersonId() );
			// first act has no start time
			if (start == null) return;

			try {
				writer.newLine();
				writer.write( event.getPersonId()+"\t"+
						event.getActType()+"\t"+
						(event.getTime() - start) );
			} catch (final IOException e) {
				throw new UncheckedIOException( e );
			}
		}

		@Override
		public void handleEvent(final ActivityStartEvent event) {
			starts.put( event.getPersonId() , event.getTime() );
		}
	}
}

