/* *********************************************************************** *
 * project: org.matsim.*
 * IdentifySpotsAtOvRough.java
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
package playground.thibautd.hitchiking;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.xml.sax.Attributes;
import playground.thibautd.parknride.herbiespecific.RelevantCoordinates;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

/**
 * chooses car links near PT stops.
 * It chooses the shortest link ending on nodes near PT stops,
 * as the QSim simulates the last link but not the first.
 * The "shortest" part is here to (1) identify on single link
 * (2) avoid big detours for drivers passing nearby.
 * @author thibautd
 */
public class IdentifySpotsAtPtRough {
	private static final Logger log =
		Logger.getLogger(IdentifySpotsAtPtRough.class);

	private static final Coord CENTER = RelevantCoordinates.HAUPTBAHNHOF;
	private static final double MAX_DIST_TO_CENTER = 40 * 1000;
	private static final double MIN_DIST = 1000;

	public static void main(final String[] args) {
		main(	args[ 0 ],
				args[ 1 ],
				args[ 2 ]);
	}

	private static void main(
			final String networkFile,
			final String scheduleFile,
			final String spotsOutFile) {
		// we need the impl to have the "getNearest..." methods
		NetworkImpl carNetwork = getCarNetwork( networkFile );

		log.info( "parse pt stops coordinates from "+scheduleFile );
		Parser ptStopsCoordParser = new Parser();
		ptStopsCoordParser.parse( scheduleFile );

		log.info( "search for links nearby stops" );
		Counter linkCounter = new Counter( "hitch-hiking link # " );
		Collection<Link> hhLinks = new ArrayList<Link>();
		for (Coord coord : ptStopsCoordParser.coords.coords) {
			linkCounter.incCounter();
			Node n = carNetwork.getNearestNode( coord );

			Link toAdd = null;
			for ( Link l : n.getInLinks().values() ) {
				if ( toAdd == null || toAdd.getLength() > l.getLength() ) toAdd = l;
			}
			if ( toAdd == null ) throw new RuntimeException( "TODO: search only for nodes with incoming links" );

			hhLinks.add( toAdd );
		}
		linkCounter.printCounter();
		logInfo( hhLinks );

		log.info( "write link Ids to "+spotsOutFile );
		HitchHikingUtils.writeFile(
				new HitchHikingSpots(
					hhLinks),
				spotsOutFile);
		log.info( "write link coords to "+spotsOutFile+".xy" );
		IdentifySpotsRough.writeXy( hhLinks , spotsOutFile + ".xy" );
	}

	private static void logInfo(final Collection<Link> hhLinks) {
		List<Link> known = new ArrayList<Link>();

		for (Link l : hhLinks) {
			if (known.contains( l )) log.info( l.getId()+" appears more than once" );
			else known.add( l );
		}
	}

	private static NetworkImpl getCarNetwork( final String netFile ) {
		log.info( "read network from "+netFile );
		Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader(sc.getNetwork()).readFile( netFile );
		NetworkImpl net = NetworkImpl.createNetwork();

		log.info( "filter network" );
		new TransportModeNetworkFilter( sc.getNetwork() ).filter(
				net,
				Collections.singleton( TransportMode.car ) );
		return net;
	}

	private static class Parser extends MatsimXmlParser {
		public final Coords coords = new Coords();
		private final Counter accCount = new Counter( "ACCEPTED Stop coord # " );
		private final Counter rejCount = new Counter( "REJECTED Stop coord # " );

		@Override
		public void startTag(
				final String name,
				final Attributes atts,
				final Stack<String> context) {
			if ( !name.equals( "stopFacility" ) ) return;
			boolean added = coords.add(
					new Coord(Double.parseDouble(atts.getValue("x")), Double.parseDouble(atts.getValue("y"))));

			if ( added ) accCount.incCounter();
			else rejCount.incCounter();
		}

		@Override
		public void endTag(
				final String name,
				final String content,
				final Stack<String> context) {
			if ( name.equals( "transitSchedule" ) ) {
				accCount.printCounter();
				rejCount.printCounter();
			}
		}
	}

	private static class Coords {
		private final List<Coord> coords = new ArrayList<Coord>();

		public boolean add(final Coord toAdd) {
			if ( CoordUtils.calcEuclideanDistance( CENTER , toAdd ) > MAX_DIST_TO_CENTER ) {
				return false;
			}
			for (Coord c : coords) {
				if ( CoordUtils.calcEuclideanDistance( c , toAdd ) < MIN_DIST ) {
					return false;
				}
			}
			return coords.add( toAdd );
		}
	}
}

