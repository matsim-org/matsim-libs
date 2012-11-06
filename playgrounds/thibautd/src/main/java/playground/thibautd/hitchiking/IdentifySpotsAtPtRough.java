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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.xml.sax.Attributes;

/**
 * chooses car links near PT stops
 * @author thibautd
 */
public class IdentifySpotsAtPtRough {
	private static final Logger log =
		Logger.getLogger(IdentifySpotsAtPtRough.class);

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
		List<Link> hhLinks = new ArrayList<Link>();
		for (Coord coord : ptStopsCoordParser.coords.getCoords()) {
			linkCounter.incCounter();
			hhLinks.add( carNetwork.getNearestLink( coord ) );
		}

		log.info( "write link Ids to "+spotsOutFile );
		HitchHikingUtils.writeFile(
				new HitchHikingSpots(
					carNetwork,
					hhLinks),
				spotsOutFile);
	}

	private static NetworkImpl getCarNetwork( final String netFile ) {
		log.info( "read network from "+netFile );
		Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader( sc ).readFile( netFile );
		NetworkImpl net = NetworkImpl.createNetwork();

		log.info( "filter network" );
		new TransportModeNetworkFilter( sc.getNetwork() ).filter(
				net,
				Collections.singleton( TransportMode.car ) );
		return net;
	}

	private static class Parser extends MatsimXmlParser {
		public final CoordTree coords = new CoordTree();
		private final Counter accCount = new Counter( "ACCEPTED Stop coord # " );
		private final Counter rejCount = new Counter( "REJECTED Stop coord # " );

		@Override
		public void startTag(
				final String name,
				final Attributes atts,
				final Stack<String> context) {
			if ( !name.equals( "stopFacility" ) ) return;
			boolean added = coords.add(
					new CoordImpl( 
						Double.parseDouble( atts.getValue( "x" ) ),
						Double.parseDouble( atts.getValue( "y" ) )));

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

	/**
	 * A simple binary tree which only adds coords at more
	 * than MIN_DIST of an already known coord.
	 */
	private static class CoordTree {
		public Coord c = null;
		public CoordTree left = null;
		public CoordTree right = null;

		public boolean add(final Coord toAdd) {
			if ( c == null ) {
				c = toAdd;
				return true;
			}

			if ( !allow( toAdd ) ) return false;

			if ( toAdd.getX() < c.getX() ) {
				if ( left == null ) left = new CoordTree();
				return left.add( toAdd );
			}

			if ( right == null ) right = new CoordTree();
			return right.add( toAdd );
		}

		private boolean allow(final Coord coord) {
			return CoordUtils.calcDistance( coord, c ) > MIN_DIST &&
				// if the "dangerous area" for the coord intersects the left or right
				// side, go down to check for a chalenger.
				(left == null || coord.getX() - MIN_DIST > c.getX() || left.allow( coord )) &&
				(right == null || coord.getX() + MIN_DIST < c.getX() || right.allow( coord ));
		}

		public Collection<Coord> getCoords() {
			List<Coord> coords = new ArrayList<Coord>();
			fill( coords );
			return coords;
		}

		private void fill(final List<Coord> coords) {
			if ( c != null) coords.add( c );
			if ( left != null ) left.fill( coords );
			if ( right != null ) right.fill( coords );
		}
	}
}

