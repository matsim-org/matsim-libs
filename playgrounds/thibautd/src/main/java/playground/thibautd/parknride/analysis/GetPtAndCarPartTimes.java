/* *********************************************************************** *
 * project: org.matsim.*
 * GetPtAndCarPartTimes.java
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.pt.PtConstants;
import playground.thibautd.parknride.ParkAndRideConstants;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author thibautd
 */
public class GetPtAndCarPartTimes {

	public static void main(final String[] args) {
		String eventsFile = args[ 0 ];
		String outFile = args[ 1 ];

		EventsManager manager = EventsUtils.createEventsManager();
		EventAnalyser analyser = new EventAnalyser( outFile );
		manager.addHandler( analyser );
		(new MatsimEventsReader( manager )).readFile( eventsFile );
		analyser.close();
	}

	private static class EventAnalyser implements PersonDepartureEventHandler, PersonArrivalEventHandler, ActivityStartEventHandler {
		private Map<Id, Trip> trips = new HashMap<Id, Trip>();

		private final BufferedWriter writer;

		public EventAnalyser( final String outFile ) {
			writer = IOUtils.getBufferedWriter( outFile );

			try {
				writer.write( "carTime\tptTime" );
			} catch (IOException e) {
				throw new UncheckedIOException( e );
			}
		}

		@Override
		public void reset(final int iteration) {
			//nothing to do
		}

		@Override
		public void handleEvent(final PersonDepartureEvent event) {
			Trip trip = trips.get( event.getPersonId() );

			if (trip == null) {
				final String m = event.getLegMode();
				if (m.equals( TransportMode.car )) {
					trips.put( event.getPersonId() , new Trip( true , event.getTime() ) );
				}
				else if (m.equals( TransportMode.transit_walk )) {
					trips.put( event.getPersonId() , new Trip( false , event.getTime() ) );
				}
			}
			else {
				trip.notifyDeparture( event.getTime() , event.getLegMode() );
			}
		}

		@Override
		public void handleEvent(final ActivityStartEvent event) {
			Trip trip = trips.get( event.getPersonId() );

			if ( trip != null &&
					!trip.notifyActStarts( event.getActType() )) {
					trips.remove( event.getPersonId() );

					if (trip.isCompletePnr()) {
						try {
							writer.newLine();
							writer.write( trip.getCarTime() +"\t" + trip.getPtTime() );
						} catch (IOException e) {
							throw new UncheckedIOException( e );
						}
					}
			}		
		}

		@Override
		public void handleEvent(final PersonArrivalEvent event) {
			Trip trip = trips.get( event.getPersonId() );

			if ( trip != null ) {
				trip.notifyArrival( event.getTime() );;
			}		
		}

		public void close() {
			try {
				writer.close();
			} catch (IOException e) {
				throw new UncheckedIOException( e );
			}
		}
	}

	private static class Trip  {
		private static enum State { car , interact , pt };
		private State state;
		private final boolean fromCarToPt;

		private double carDeparture = -1;
		private double carArrival = -1;
		private double ptDeparture = -1;
		private double ptArrival = -1;

		public Trip(
				final boolean fromCarToPt,
				final double departure) {
			this.fromCarToPt = fromCarToPt;

			if (fromCarToPt)  {
				carDeparture = departure;
				state = State.car;
			}
			else {
				ptDeparture = departure;
				state = State.pt;
			}
		}

		public void notifyDeparture(final double time, final String mode) {
			switch (state) {
				case car:
					throw new IllegalStateException( ""+state );
				case interact:
					if (fromCarToPt) { 
						if (!(mode.equals( TransportMode.pt ) ||
							mode.equals( TransportMode.transit_walk)) ) {
							throw new IllegalArgumentException( mode );
						}
						state = State.pt;
						ptDeparture = time;
					}
					else {
						if (!mode.equals( TransportMode.car )) { 
							throw new IllegalArgumentException( mode );
						}
						state = State.car;
						carDeparture = time;
					}
					break;
				default:
					break;
			}
		}

		public void notifyArrival(final double time) {
			switch (state) {
				case car:
					carArrival = time;
					break;
				case pt:
					ptArrival = time;
					break;
				default:
					throw new IllegalStateException( ""+state );
			}
		}

		public boolean notifyActStarts(final String actType) {
			if ( actType.equals( ParkAndRideConstants.PARKING_ACT ) ) {
				state = State.interact;
				return true;
			}

			switch (state) {
				case car:
					// the case car -> pt is already considered above
					return false;
				case pt:
					return actType.equals( PtConstants.TRANSIT_ACTIVITY_TYPE );
				default:
					throw new IllegalStateException( ""+state );
			}
		}

		public boolean isCompletePnr() {
			return carDeparture >= 0 &&
				carArrival >= 0 &&
				ptDeparture >= 0 &&
				ptArrival >= 0;
		}

		public double getCarTime() {
			return carArrival - carDeparture;
		}

		public double getPtTime() {
			return ptArrival - ptDeparture;
		}
	}
}

