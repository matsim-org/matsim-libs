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

import java.io.File;
import java.io.IOException;

import java.util.List;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;

import playground.thibautd.analysis.spacetimeprismjoinabletrips.TripsPrism.PassengerRecord;

/**
 * @author thibautd
 */
public class ExtractRecordsFromEvents {
	// private static final Level LOG_LEVEL = Level.TRACE;
	private static final Level LOG_LEVEL = Level.DEBUG;

	private static final double DETOUR_FRAC = 0.25;
	private static final double TIME_WINDOW_RADIUS = 0.25 * 3600;

	public static void main(final String[] args) {
		String networkFile = args[0];
		String eventFile = args[1];
		// ie the output file names will be <prefix>name.dat
		// this must include the path, and can add a "prefix"
		String outPrefix = args[2];

		mkdirs( outPrefix );

		List<Record> records = extractRecords( eventFile );
		Network network = getNetwork( networkFile );
		TripsPrism prism = new TripsPrism( records , network );

		RecordsFlatFormatWriter.writeRecords( records , outPrefix+"records.dat.gz" );
		RecordsFlatFormatWriter writer = new RecordsFlatFormatWriter( outPrefix+"passengerTrips.dat.gz" );
		Counter counter = new Counter( "--- searching passengers for record # " );
		for (Record record : records) {
			counter.incCounter();
			if (record.getTripMode().equals( TransportMode.car )) {
				for (PassengerRecord passenger : prism.getTripsInPrism( record , DETOUR_FRAC , TIME_WINDOW_RADIUS )) {
					writer.writePassengerTrip( passenger );
				}
			}
		}
		counter.printCounter();
		writer.close();
	}

	private static void mkdirs(final String outPrefix) {
		String dirs = outPrefix.substring( 0 , outPrefix.lastIndexOf( "/" )+1 );
		File f = new File( dirs );
		if (!f.exists()) f.mkdirs();

		Logger root = Logger.getRootLogger();
		root.setLevel( LOG_LEVEL );
		try {
			FileAppender appender = new FileAppender(Controler.DEFAULTLOG4JLAYOUT, outPrefix+"logfile.log");
			appender.setName("logfile.log");
			root.addAppender(appender);
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	private static Network getNetwork(final String networkFile) {
		Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		(new MatsimNetworkReader( sc )).readFile( networkFile );
		return sc.getNetwork();
	}

	private static List<Record> extractRecords(final String eventFile) {
		EventsManager manager = EventsUtils.createEventsManager();
		TripsRecordsEventsHandler parser = new TripsRecordsEventsHandler();
		manager.addHandler( parser );
		(new MatsimEventsReader( manager )).readFile( eventFile );
		return parser.getRecords();
	}
}

