/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractExecutedWaitingTimes.java
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
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.qsim.PassengerDepartsWithDriverEvent;

/**
 * @author thibautd
 */
public class ExtractExecutedWaitingTimes {

	public static void main(final String[] args) {
		String eventFile = args[ 0 ];
		String outFile = args[ 1 ];

		EventsManager manager = EventsUtils.createEventsManager();
		Handler handler = new Handler( outFile );
		manager.addHandler( handler );

		(new MatsimEventsReader( manager )).readFile( eventFile );

		handler.close();
	}

	private static class Handler implements BasicEventHandler {
		private final BufferedWriter writer;

		private Map<Id, Double> passengerWaitStarts = new HashMap<Id, Double>();
		private Map<Id, Double> driverWaitStarts = new HashMap<Id, Double>();

		public Handler(final String file) {
			this.writer = IOUtils.getBufferedWriter( file );
			try {
				writer.write( "id\tstatus\twaiting_s" );
			} catch (IOException e) {
				throw new UncheckedIOException( e );
			}
		}

		public void close() {
			try {
				writer.close();
			} catch (IOException e) {
				throw new RuntimeException( e );
			}
		}

		@Override
		public void reset(int iteration) {}

		@Override
		public void handleEvent(final Event event) {
			if (handlePassengerDeparture( event )) return;
			if (handleDriverDeparture( event )) return;
			if (handleJointDeparture( event )) return;
		}

		private boolean handlePassengerDeparture(final Event event) {
			if (event instanceof AgentDepartureEvent &&
					((AgentDepartureEvent) event).getLegMode().equals( JointActingTypes.PASSENGER )) {
				passengerWaitStarts.put(
						((AgentDepartureEvent) event).getPersonId(),
						event.getTime());
				return true;
			}
			return false;
		}

		private boolean handleDriverDeparture(final Event event) {
			if (event instanceof AgentDepartureEvent &&
					((AgentDepartureEvent) event).getLegMode().equals( JointActingTypes.DRIVER )) {
				driverWaitStarts.put(
						((AgentDepartureEvent) event).getPersonId(),
						event.getTime());
				return true;
			}
			return false;
		}

		private boolean handleJointDeparture(final Event event) {
			if ( event.getAttributes().get( Event.ATTRIBUTE_TYPE ).equals( PassengerDepartsWithDriverEvent.EVENT_TYPE ) ) {
				String driverId = event.getAttributes().get( PassengerDepartsWithDriverEvent.ATTRIBUTE_DRIVER );
				String passengerId = event.getAttributes().get( PassengerDepartsWithDriverEvent.ATTRIBUTE_PASSENGER );

				Double driverStartWaitingTime = driverWaitStarts.remove( new IdImpl( driverId ) );
				Double passengerStartWaitingTime = passengerWaitStarts.remove( new IdImpl( passengerId ) );

				try {
					if (driverStartWaitingTime != null) {
						writer.newLine();
						writer.write( driverId+"\tdriver\t"+(event.getTime() - driverStartWaitingTime) );
					}
					writer.newLine();
					writer.write( passengerId+"\tpassenger\t"+(event.getTime() - passengerStartWaitingTime) );
				}
				catch (IOException e) {
					throw new UncheckedIOException( e );
				}


				return true;
			}
			return false;
		}
	}
}

