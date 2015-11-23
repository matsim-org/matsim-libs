/* *********************************************************************** *
 * project: org.matsim.*
 * FromParkedVehiclesEventsToNumberOfParkedVehicles.java
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
package playground.thibautd.parknride.analysis;

import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * @author thibautd
 */
public class FromParkedVehiclesEventsToNumberOfParkedVehicles {
	public static void main(final String[] args) {
		String inFile = args[ 0 ];
		String outFile = args[ 1 ];

		EventsManager events = EventsUtils.createEventsManager();
		EventsInterpreter i = new EventsInterpreter( outFile );
		events.addHandler( i );
		(new MatsimEventsReader( events )).readFile( inFile );
		i.close();
	}
	
	private static class EventsInterpreter implements ActivityStartEventHandler, ActivityEndEventHandler {
		private int count = 0;
		private BufferedWriter writer;
		private double currentTime = Double.NEGATIVE_INFINITY;

		public EventsInterpreter(final String outFile) {
			writer = IOUtils.getBufferedWriter( outFile );
			try {
				writer.write( "\"Time (h)\"\t\"n\"" );
			} catch (IOException e) {
				throw new RuntimeException( e );
			}
		}

		@Override
		public void reset(int iteration) {}

		@Override
		public void handleEvent(final ActivityStartEvent event) {
			if (event.getActType().equals( GenerateActivityEventsForParkedVehicles.ACTIVITY_TYPE )) {
				count++;
				writeCount( event.getTime() );
			}
		}

		@Override
		public void handleEvent(final ActivityEndEvent event) {
			if (event.getActType().equals( GenerateActivityEventsForParkedVehicles.ACTIVITY_TYPE )) {
				count--;
				writeCount( event.getTime() );
			}
		}

		private void writeCount(final double time) {
			if (time < currentTime) throw new RuntimeException( "wrong time sequence!" );
			currentTime = time;
			try {
				writer.newLine();
				writer.write( (time / 3600d)+"\t"+count );
			} catch (IOException e) {
				throw new RuntimeException( e );
			}
		}

		public void close() {
			try {
				writer.close();
			} catch (IOException e) {
				throw new RuntimeException( e );
			}
		}
	}

}
