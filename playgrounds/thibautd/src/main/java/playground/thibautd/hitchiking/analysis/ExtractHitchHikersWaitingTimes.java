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
package playground.thibautd.hitchiking.analysis;

import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
		private final Map<String, Tuple<String, Double>> waitStarts = new HashMap<String, Tuple<String,Double>>();
		private final BufferedWriter writer;

		public Handler(final String outFile) {
			writer = IOUtils.getBufferedWriter( outFile );
			try {
				writer.write("agentId\tspotId\twaitTime" );
			}
			catch (IOException e) {
				throw new UncheckedIOException( e );
			}
		}

		@Override
		public void reset(final int iteration) {
		}

		@Override
		public void handleEvent(final Event event) {
			if (event.getAttributes().get( Event.ATTRIBUTE_TYPE ).equals( "passengerStartsWaiting" )) {
				waitStarts.put(
						event.getAttributes().get( ActivityStartEvent.ATTRIBUTE_PERSON ),
						new Tuple<String, Double>(
							event.getAttributes().get( ActivityEndEvent.ATTRIBUTE_LINK ),
							event.getTime()));
			}
			else if (event.getAttributes().get( Event.ATTRIBUTE_TYPE ).equals( "passengerEndsWaiting" )) {
				String p = event.getAttributes().get( ActivityEndEvent.ATTRIBUTE_PERSON );
				Tuple<String, Double> start = waitStarts.remove( p );

				try {
					writer.newLine();
					writer.write( p+"\t"+start.getFirst()+"\t"+(event.getTime() - start.getSecond()) );
				}
				catch (IOException e) {
					throw new UncheckedIOException( e );
				}
			}
		}

		public void finish() {
			for (Map.Entry<String, Tuple<String, Double>> e : waitStarts.entrySet()) {
				try {
					writer.newLine();
					writer.write( e.getKey()+"\t"+e.getValue().getFirst()+"\tInf" );
				}
				catch (IOException ex) {
					throw new UncheckedIOException( ex );
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

