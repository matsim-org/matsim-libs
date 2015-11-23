/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractRecordsFromEvents.java
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
package playground.thibautd.analysis.spacetimeprismjoinabletrips;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;
import playground.thibautd.analysis.spacetimeprismjoinabletrips.TripsPrism.PassengerRecord;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * @author thibautd
 */
public class ExtractRecordsFromEvents {
	// private static final Level LOG_LEVEL = Level.TRACE;
	private static final Level LOG_LEVEL = Level.DEBUG;

	private static final double DETOUR_FRAC = 0.5;
	private static final double TIME_WINDOW_RADIUS = 0.5 * 3600;
	private static final boolean CONSIDER_ONLY_CAR_TRIPS = true;
	private static final boolean PRUNE_ZERO_LENGTH_TRIPS = true;

	public static void main(final String[] args) {
		final String networkFile = args[0];
		final String eventFile = args[1];
		// ie the output file names will be <prefix>name.dat
		// this must include the path, and can add a "prefix"
		final String outPrefix = args[2];

		mkdirs( outPrefix );

		final Network network = getNetwork( networkFile );

		final TripsRecordsEventsHandler parser = new TripsRecordsEventsHandler();
		final TravelTimeCalculator travelTimeCalculator = new TravelTimeCalculator(
				network,
				new TravelTimeCalculatorConfigGroup());
		processEvents( parser , travelTimeCalculator , eventFile );

		final List<Record> records = extractRecords( parser );

		final TripsPrism prism = new TripsPrism( records , travelTimeCalculator.getLinkTravelTimes() , network );

		RecordsFlatFormatWriter.writeRecords( records , outPrefix+"records.dat" );
		final RecordsFlatFormatWriter writer = new RecordsFlatFormatWriter( outPrefix+"passengerTrips.dat.gz" );
		final Counter counter = new Counter( "--- searching passengers for record # " );
		for (final Record record : records) {
			counter.incCounter();
			if (record.getTripMode().equals( TransportMode.car )) {
				for (final PassengerRecord passenger : prism.getTripsInPrism( record , DETOUR_FRAC , TIME_WINDOW_RADIUS )) {
					writer.writePassengerTrip( passenger );
				}
			}
		}
		counter.printCounter();
		writer.close();
		prism.logStats();
	}

	private static void mkdirs(final String outPrefix) {
		final String dirs = outPrefix.substring( 0 , outPrefix.lastIndexOf( "/" )+1 );
		final File f = new File( dirs );
		if (!f.exists() && !f.mkdirs()) throw new UncheckedIOException( "could not create dirs for "+dirs );

		final Logger root = Logger.getRootLogger();
		root.setLevel( LOG_LEVEL );
		try {
			final FileAppender appender = new FileAppender(Controler.DEFAULTLOG4JLAYOUT, outPrefix+"logfile.log");
			appender.setName("logfile.log");
			root.addAppender(appender);
		}
		catch (final IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	private static Network getNetwork(final String networkFile) {
		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		(new MatsimNetworkReader( sc )).readFile( networkFile );
		return sc.getNetwork();
	}

	private static void processEvents(
			final TripsRecordsEventsHandler parser,
			final TravelTimeCalculator travelTime,
			final String eventFile) {
		final EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler( parser );
		manager.addHandler( travelTime );
		(new MatsimEventsReader( manager )).readFile( eventFile );
	}

	private static List<Record> extractRecords(final TripsRecordsEventsHandler parser) {
		final List<Record> records = parser.getRecords();

		if (CONSIDER_ONLY_CAR_TRIPS) {
			final Iterator<Record> it = records.iterator();

			while (it.hasNext()) {
				if (!it.next().getTripMode().equals( TransportMode.car )) {
					it.remove();
				}
			}
		}
		if (PRUNE_ZERO_LENGTH_TRIPS) {
			final Iterator<Record> it = records.iterator();

			while (it.hasNext()) {
				final Record r = it.next();
				if (r.getDestinationLink().equals( r.getOriginLink() )) {
					it.remove();
				}
			}
		}

		return records;
	}
}

