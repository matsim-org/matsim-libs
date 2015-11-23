/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractWaitForVehicleInfo.java
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
package playground.thibautd.scripts;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * can be used to identify joint travel waiting times, or pt waiting times
 * @author thibautd
 */
public class ExtractWaitForVehicleInfo {
	public static void main(final String[] args) throws Exception {
		final String inputEventsFile = args[ 0 ];
		final String inputNetworkFile = args[ 1 ];
		final String outputXyFile = args[ 2 ];

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader( sc ).readFile( inputNetworkFile );

		final BufferedWriter writer = IOUtils.getBufferedWriter( outputXyFile );
		final EventsManager events = EventsUtils.createEventsManager();
		events.addHandler( new Handler( sc.getNetwork() , writer ) );

		new MatsimEventsReader( events ).readFile( inputEventsFile );
		writer.close();
	}

	private static final class Handler implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler {
		private final Network network;
		private final BufferedWriter writer;

		private final Map<Id, String> lastDepartureModeForAgent = new HashMap<Id, String>();
		private final Map<Id, Double> lastDepartureTimeForAgent = new HashMap<Id, Double>();
		private final Map<Id, Id> lastDepartureLinkForAgent = new HashMap<Id, Id>();

		public Handler( 
				final Network network,
				final BufferedWriter writer ) throws IOException {
			this.network = network;
			this.writer = writer;

			writer.write( "personId\tlinkId\tx\ty\tmode\tvehicle\tstartWait\tendWait" );
		}

		@Override
		public void reset(int iteration) {}

		@Override
		public void handleEvent(final PersonDepartureEvent event) {
			lastDepartureModeForAgent.put( event.getPersonId() , event.getLegMode() );
			lastDepartureTimeForAgent.put( event.getPersonId() , event.getTime() );
			lastDepartureLinkForAgent.put( event.getPersonId() , event.getLinkId() );
		}
			
		@Override
		public void handleEvent(final PersonEntersVehicleEvent event) {
			try {
				writer.newLine();
				final Id linkId = lastDepartureLinkForAgent.remove( event.getPersonId() );
				writer.write( event.getPersonId()+"\t"+
						linkId+"\t"+
						network.getLinks().get( linkId ).getFromNode().getCoord().getX()+"\t"+
						network.getLinks().get( linkId ).getFromNode().getCoord().getY()+"\t"+
						lastDepartureModeForAgent.remove( event.getPersonId() )+"\t"+
						event.getVehicleId()+"\t"+
						lastDepartureTimeForAgent.remove( event.getPersonId() )+"\t"+
						event.getTime() );
			}
			catch (IOException e) {
				throw new UncheckedIOException( e );
			}
		}
	}
}

