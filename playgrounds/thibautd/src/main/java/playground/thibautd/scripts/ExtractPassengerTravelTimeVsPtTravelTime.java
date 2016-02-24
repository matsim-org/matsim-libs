/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractPassengerTravelTimeVsPtTravelTime.java
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author thibautd
 */
public class ExtractPassengerTravelTimeVsPtTravelTime {
	private static final Logger log =
		Logger.getLogger(ExtractPassengerTravelTimeVsPtTravelTime.class);

	private static final double BEEFLY_FACTOR = 1.3;
	private static final double PT_SPEED = 6.944444444444445;

	public static void main(final String[] args) {
		log.warn( "parameters for the computation of pt travel time are hard coded" );
		log.warn( "process is valid only if access and egress is instantaneous for passengers" );

		String networkFile = args[ 0 ];
		String eventsFile = args[ 1 ];
		String outFile = args[ 2 ];

		Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		(new MatsimNetworkReader(scenario.getNetwork())).readFile( networkFile );
		Handler handler = new Handler( scenario.getNetwork() , outFile );
		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler( handler );
		(new MatsimEventsReader( manager )).readFile( eventsFile );
		handler.close();
	}

	private static class Handler implements PersonDepartureEventHandler , PersonArrivalEventHandler {
		private final Network network;
		private final BufferedWriter writer;
		private Map<Id, PersonDepartureEvent> passengerDepartures = new HashMap<Id, PersonDepartureEvent>();

		public Handler(
				final Network network,
				final String outFile) {
			this.network = network;
			this.writer = IOUtils.getBufferedWriter( outFile );

			try {
				writer.write("id\tdistance\tcpTravelTime\tptTravelTime" );
			} catch (IOException e) {
				throw new UncheckedIOException( e );
			}
		}

		@Override
		public void reset(final int iteration) {
		}

		@Override
		public void handleEvent(final PersonDepartureEvent event) {
			if (event.getLegMode().equals( JointActingTypes.PASSENGER )) {
				passengerDepartures.put( event.getPersonId() , event );
			}
		}

		@Override
		public void handleEvent(final PersonArrivalEvent event) {
			if (event.getLegMode().equals( JointActingTypes.PASSENGER )) {
				PersonDepartureEvent departureEvent = passengerDepartures.remove( event.getPersonId() );
				double dep = departureEvent.getTime();

				double dist = calcDist( departureEvent , event );
				double ptTravelTime = calcPtTime( dist );

				try {
					writer.newLine();
					writer.write( event.getPersonId()+"\t"+dist+"\t"+(event.getTime() - dep)+"\t"+ptTravelTime );
				} catch (IOException e) {
					throw new UncheckedIOException( e );
				}
			}
		}

		private double calcDist(
				final PersonDepartureEvent departureEvent,
				final PersonArrivalEvent arrivalEvent) {
			Coord o = network.getLinks().get( departureEvent.getLinkId() ).getCoord();
			Coord d = network.getLinks().get( arrivalEvent.getLinkId() ).getCoord();

			return CoordUtils.calcEuclideanDistance( o , d );
		}

		private double calcPtTime(final double dist) {
			return (dist * BEEFLY_FACTOR) / PT_SPEED;
		}

		public void close() {
			try {
				writer.close();
			} catch (IOException e) {
				throw new UncheckedIOException( e );
			}
		}
	}
}

