/* *********************************************************************** *
 * project: org.matsim.*
 * IdentifySpotsRough.java
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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;
import playground.thibautd.parknride.herbiespecific.RelevantCoordinates;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Identifies hitch hiking spots in the following way:
 * <ul>
 * <li> searches for nodes with incoming low speed links and outcoming high speed links  (i.e. probable highway entrances)
 * <li> chooses a low speed link at random near this spot.
 * </ul>
 * @author thibautd
 */
public class IdentifySpotsRough {
	private static final Coord CENTER = RelevantCoordinates.HAUPTBAHNHOF;
	private static final double MAX_DIST_TO_CENTER = 40 * 1000;
	private static final boolean INCLUDE_CLOSEST_NODE_TO_CENTER = true;

	private static final double HIGH_SPEED = 100 / 3.6;
	private static final double MIN_SPEED_SPOTS = 49 / 3.6;
	private static final double MIN_DIST = 2000;
	private static final double SEARCH_RADIUS = 500;
	private static final double RADIUS_EXTENSION = 10;

	public static void main(final String [] args) {
		String networkFile = args[0];
		String outFile = args[1];
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario( config );
		
		(new MatsimNetworkReader(scenario.getNetwork())).readFile( networkFile );

		Network network = scenario.getNetwork();

		List<Node> hhNodes = getHhNodes( network );
		pruneCloseNodes( hhNodes );
		Collection<Link> hhLinks = getHhLinks( (NetworkImpl) network , hhNodes );
		HitchHikingUtils.writeFile(
				new HitchHikingSpots( hhLinks ),
				outFile);
		writeXy( hhLinks , outFile + ".xy" );
	}

	static void writeXy(
			final Collection<Link> hhLinks,
			final String outFile) {
		try {
			Counter counter = new Counter( "writing coordinate # " );
			BufferedWriter writer = IOUtils.getBufferedWriter( outFile );
			for (Link l : hhLinks) {
				counter.incCounter();
				writer.write( l.getCoord().getX() +"\t" + l.getCoord().getY() );
				writer.newLine();
			}
			counter.printCounter();
			writer.close();
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	private static Collection<Link> getHhLinks(
			final NetworkImpl network,
			final List<Node> hhNodes) {
		Random rand = new Random( 4375692 );
		List<Link> links = new ArrayList<Link>();

		Counter counter = new Counter( "choosing spot # " );

		if (INCLUDE_CLOSEST_NODE_TO_CENTER) {
			hhNodes.add( network.getNearestNode( CENTER ) );
		}

		for (Node n : hhNodes) {
			counter.incCounter();
			Set<Link> choiceSet = new HashSet<Link>();

			double radius = SEARCH_RADIUS;
			while (choiceSet.size() < 2) {
				Collection<Node> neighbors =
					network.getNearestNodes(
							n.getCoord(),
							radius );
				radius += RADIUS_EXTENSION;

				for (Node neighbor : neighbors) {
					for (Link l : neighbor.getInLinks().values()) {
						double s = l.getFreespeed();
						if ( s >= MIN_SPEED_SPOTS && s <= HIGH_SPEED - 0.0001 ) {
							choiceSet.add( l );
						}
					}
				}
			}

			links.add( new ArrayList<Link>(choiceSet).get( rand.nextInt( choiceSet.size() ) ) );
		}
		counter.printCounter();
		
		return links;
	}

	private static List<Node> getHhNodes( final Network network ) {
		List<Node> hhNodes = new ArrayList<Node>();

		Counter counter = new Counter( "analysing node # " );
		for (Node node : network.getNodes().values()) {
			counter.incCounter();

			if ( CoordUtils.calcEuclideanDistance( node.getCoord() , CENTER ) > MAX_DIST_TO_CENTER ) {
				continue;
			}

			for (Link outLink : node.getOutLinks().values()) {
				if (outLink.getFreespeed() >= HIGH_SPEED) {
					for (Link inLink : node.getInLinks().values()) {
						if (inLink.getFreespeed() < HIGH_SPEED - 0.0001 ) {
							hhNodes.add( node );
							break;
						}
					}
					break;
				}
			}
		}
		counter.printCounter();

		return hhNodes;
	}

	private static void pruneCloseNodes(final List<Node> hhNodes) {
		List<Coord> coords = new ArrayList<Coord>();

		for (Node n : new ArrayList<Node>(hhNodes)) {
			boolean reject = false;
			for (Coord c : coords) {
				if ( CoordUtils.calcEuclideanDistance( c , n.getCoord() ) <= MIN_DIST ) {
					reject = true;
					break;
				}
			}

			if (reject) {
				hhNodes.remove( n );
			}
			else {
				coords.add( n.getCoord() );
			}
		}
	}
}

