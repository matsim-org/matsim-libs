/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractNonPnrParkingDurations.java
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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import playground.thibautd.parknride.ParkAndRideConstants;
import playground.thibautd.parknride.herbiespecific.RelevantCoordinates;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author thibautd
 */
public class ExtractNonPnrParkingDurations {
	private static final Coord CENTER = RelevantCoordinates.HAUPTBAHNHOF;

	public static void main(final String[] args) {
		final String networkFile = args[ 0 ];
		final String eventsFile = args[ 1 ];
		final String outFile = args[ 2 ];

		Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader(sc.getNetwork()).readFile( networkFile );

		Handler handler = new Handler( sc.getNetwork() , outFile );
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler( handler );
		new EventsReaderXMLv1( events ).parse( eventsFile );
		handler.close();
	}

	private static class Handler implements PersonArrivalEventHandler, PersonDepartureEventHandler, ActivityStartEventHandler {
		private final Network network;
		private final BufferedWriter writer;
		private final List<Id> justParkedAgents = new ArrayList<Id>();
		private final Map<Id, Double> payedParkingStarts = new HashMap<Id, Double>();

		public Handler(final Network network, final String file) {
			this.network = network;
			this.writer = IOUtils.getBufferedWriter( file );

			try {
				writer.write( "agentId\tduration\tdistanceToHb" );
			} catch (IOException e) {
				throw new UncheckedIOException( e );
			}
		}

		public void close() {
			try {
				writer.close();
			} catch (IOException e) {
				throw new UncheckedIOException( e );
			}
		}

		@Override
		public void handleEvent(final PersonArrivalEvent event) {
			if (event.getLegMode().equals( TransportMode.car )) {
				justParkedAgents.add( event.getPersonId() );
			}
		}

		@Override
		public void handleEvent(final ActivityStartEvent event) {
			boolean hasParked = justParkedAgents.remove( event.getPersonId() );
			if (event.getActType().equals( ParkAndRideConstants.PARKING_ACT )) return;
			if (hasParked) payedParkingStarts.put( event.getPersonId() , event.getTime() );
		}

		@Override
		public void handleEvent(final PersonDepartureEvent event) {
			if ( !event.getLegMode().equals( TransportMode.car ) ) return;

			Double startOfParking = payedParkingStarts.remove( event.getPersonId() );
			if (startOfParking == null) return;

			final double dist = CoordUtils.calcEuclideanDistance(
					CENTER,
					network.getLinks().get( event.getLinkId() ).getCoord());
			try {
				writer.newLine();
				writer.write( event.getPersonId()+"\t"+(event.getTime() - startOfParking)+"\t"+dist );
			} catch (IOException e) {
				throw new UncheckedIOException( e );
			}
		}

		@Override
		public void reset(final int iteration) {
			// TODO Auto-generated method stub
		}
	}
}

