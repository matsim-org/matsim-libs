/* *********************************************************************** *
 * project: org.matsim.*
 * IdentifyParkAndRideFacilities.java
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
package playground.thibautd.parknride.herbiespecific;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import playground.thibautd.parknride.ParkAndRideFacilities;
import playground.thibautd.parknride.ParkAndRideFacilitiesXmlWriter;
import playground.thibautd.parknride.ParkAndRideFacility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author thibautd
 */
public class IdentifyParkAndRideFacilities {
	private static final String TRAIN = "train";
	private static final Coord CENTER = RelevantCoordinates.HAUPTBAHNHOF;
	private static final Coord BOUNDARY_POINT = RelevantCoordinates.HARDBRUECKE;
	// expand a little, so that the boundary point is excluded as well
	private static final double factor = 1.20;


	public static void main(final String[] args) {
		final String scheduleFile = args[ 0 ];
		final String networkFile = args[ 1 ];
		final String outputFile = args[ 2 ];

		final double minDist = CoordUtils.calcEuclideanDistance( CENTER , BOUNDARY_POINT ) * factor;
		final PnrIds ids = new PnrIds();

		NetworkImpl network = readNetwork( networkFile );
		TransitSchedule schedule = readSchedule( scheduleFile );
		ParkAndRideFacilities facilities = new ParkAndRideFacilities( "train stations, except "+minDist+" around Hbf" );
		RelevantStops stops = new RelevantStops();

		Counter count = new Counter( "analysing stop # " );
		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				if (route.getTransportMode().equals( TRAIN )) {
					for (TransitRouteStop stop : route.getStops()) {
						count.incCounter();
						TransitStopFacility facility = stop.getStopFacility();

						if (acceptStop( facility.getCoord() , minDist )) {
							stops.addStop( facility );
						}
					}
				}
			}
		}
		count.printCounter();

		count = new Counter( "creating pnr facility # " );
		for (TransitStopFacility stop : stops.facilities.values()) {
			count.incCounter();
			facilities.addFacility(
					new ParkAndRideFacility(
						ids.next(),
						stop.getCoord(),
						NetworkUtils.getNearestLink(network, stop.getCoord()).getId(),
						Arrays.asList( stop.getId() ) ));
		}
		count.printCounter();

		(new ParkAndRideFacilitiesXmlWriter( facilities )).write( outputFile );
	}

	private static NetworkImpl readNetwork(final String networkFile) {
		Config config = new Config();
		config.addCoreModules();
		config.network().setInputFile( networkFile );
		Scenario scenario = ScenarioUtils.loadScenario( config );
		//MatsimNetworkReader reader = new MatsimNetworkReader( scenario );
		//reader.readFile( networkFile );

		NetworkImpl network = (NetworkImpl) scenario.getNetwork();

		Collection<Link> links = new ArrayList<Link>( network.getLinks().values() );

		Counter counter = new Counter( "checking for car avail for link # " );
		for (Link link : links) {
			counter.incCounter();
			if ( !link.getAllowedModes().contains( TransportMode.car ) ) {
				network.removeLink( link.getId() );
			}
		}
		counter.printCounter();

		(new NetworkCleaner()).run( network );

		return network;
	}

	private static boolean acceptStop(final Coord coord , final double dist) {
		return CoordUtils.calcEuclideanDistance( coord , CENTER ) > dist;
	}

	private static TransitSchedule readSchedule( final String fileName ) {
		TransitSchedule schedule = new TransitScheduleFactoryImpl().createTransitSchedule(); 

		TransitScheduleReaderV1 reader =
			new TransitScheduleReaderV1(
					schedule,
					new ModeRouteFactory());

		reader.readFile( fileName );

		return schedule;
	}

	private static class RelevantStops {
		final Map<Coord, TransitStopFacility> facilities =
			new HashMap<Coord, TransitStopFacility>();

		public void addStop(final TransitStopFacility facil) {
			// avoids multiplying uselessly PnR facilities
			facilities.put( facil.getCoord() , facil );
		}
	}

	private static class PnrIds {
		private long count = Long.MIN_VALUE;

		public Id<ActivityFacility> next() {
			count++;

			if (count == Long.MAX_VALUE) {
				throw new RuntimeException( "overflow" );
			}

			return Id.create( "pnr-"+count , ActivityFacility.class );
		}
	}
}
