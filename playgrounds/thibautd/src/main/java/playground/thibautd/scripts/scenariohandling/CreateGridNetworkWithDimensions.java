/* *********************************************************************** *
 * project: org.matsim.*
 * CreateGridNetworkWithDimensions.java
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
package playground.thibautd.scripts.scenariohandling;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import playground.ivt.utils.ArgParser;
import playground.ivt.utils.ArgParser.Args;
import playground.thibautd.utils.UniqueIdFactory;

import java.util.*;

/**
 * @author thibautd
 */
public class CreateGridNetworkWithDimensions {

	public static void main(final String[] args) {
		final ArgParser argParser = new ArgParser();
		argParser.setDefaultValue( "--freespeed" , ""+(75d * 1000 / 3600) );
		argParser.setDefaultValue( "--link-length" , "1000" );
		argParser.setDefaultValue( "--link-capacity" , "100" );
		argParser.setDefaultValue( "--width" , "100" );
		argParser.setDefaultValue( "--height" , "100" );

		final Args args1 = argParser.parseArgs( args );
		final double freespeed = Double.parseDouble( args1.getValue( "--freespeed" ) );
		final double linkLength = Double.parseDouble( args1.getValue( "--link-length" ) );
		final double linkCapacity = Double.parseDouble( args1.getValue( "--link-capacity" ) );
		final int width = Integer.parseInt( args1.getValue( "--width" ) );
		final int height = Integer.parseInt( args1.getValue( "--height" ) );

		final String outFile = args1.getNonSwitchedArgs()[ 0 ];

		final Network network = ScenarioUtils.createScenario( ConfigUtils.createConfig() ).getNetwork();
		createNetwork(
				network,
				freespeed,
				linkLength,
				linkCapacity,
				width,
				height );

		// metadata
		final StringBuilder meta = new StringBuilder();
		meta.append( "generated with " );
		meta.append( CreateGridNetworkWithDimensions.class.getName() );
		for ( String arg : args ) meta.append( " "+arg );
		meta.append( " on the " );
		meta.append( new Date().toString() );

		((NetworkImpl) network).setName( meta.toString() );

		new NetworkWriter( network ).write( outFile );
	}

	public static void createNetwork(
			final Network network,
			final double freespeed,
			final double length,
			final double capacity,
			final int width,
			final int height) {
		List<Node> lastHorizontalLine = Collections.emptyList();

		final UniqueIdFactory nodeIdFactory = new UniqueIdFactory( "" );
		for ( int i = 0; i < height; i++ ) {
			final List<Node> newHorizontalLine =
				createNodes(
						nodeIdFactory,
						i * length,
						width,
						length,
						network.getFactory());
			final List<Link> horizontalLinks =
				createLinks(
						newHorizontalLine,
						length,
						freespeed,
						capacity,
						network.getFactory() );
			final List<Link> verticalLinks =
				linkLines(
						lastHorizontalLine,
						newHorizontalLine,
						length,
						freespeed,
						capacity,
						network.getFactory() );

			for ( Node n : newHorizontalLine ) network.addNode( n );
			for ( Link l : horizontalLinks ) network.addLink( l ) ;
			for ( Link l : verticalLinks ) network.addLink( l ) ;

			lastHorizontalLine = newHorizontalLine;
		}
	}

	private static List<Link> linkLines(
			final List<Node> lastHorizontalLine,
			final List<Node> newHorizontalLine,
			final double length,
			final double freespeed,
			final double capacity,
			final NetworkFactory fact) {
		if ( lastHorizontalLine.isEmpty() ) return Collections.emptyList();
		if ( lastHorizontalLine.size() != newHorizontalLine.size() ) throw new IllegalArgumentException();

		final List<Link> links = new ArrayList<Link>();
		for ( int i=0; i < lastHorizontalLine.size(); i++ ) {
			final Node n1 = lastHorizontalLine.get( i );
			final Node n2 = newHorizontalLine.get( i );
			links.add( createLink( fact , length , freespeed , capacity , n1 , n2 ) );
			links.add( createLink( fact , length , freespeed , capacity , n2 , n1 ) );
		}

		return links;
	}

	private static List<Link> createLinks(
			final List<Node> newHorizontalLine,
			final double length,
			final double freespeed,
			final double capacity,
			final NetworkFactory fact) {
		final List<Link> links = new ArrayList<Link>();

		final Iterator<Node> nodeIterator = newHorizontalLine.iterator();
		Node lastNode = nodeIterator.next();

		while ( nodeIterator.hasNext() ) {
			final Node newNode = nodeIterator.next();

			links.add( createLink( fact , length , freespeed , capacity , lastNode , newNode ) );
			links.add( createLink( fact , length , freespeed , capacity , newNode , lastNode ) );

			lastNode = newNode;
		}

		return links;
	}

	private static Link createLink(
			final NetworkFactory fact,
			final double length,
			final double freespeed,
			final double capacity,
			final Node o,
			final Node d) {
		final Link link = fact.createLink(
				Id.create( o.getId() +"--"+ d.getId() , Link.class ),
				o,
				d );
		link.setLength( length );
		link.setFreespeed( freespeed );
		link.setCapacity( capacity );

		return link;
	}

	private static List<Node> createNodes(
			final UniqueIdFactory nodeIdFactory,
			final double y,
			final int width,
			final double length,
			final NetworkFactory factory) {
		final List<Node> nodes = new ArrayList<Node>();
		for ( int i=0; i < width; i++ ) {
			nodes.add(
					factory.createNode(
						nodeIdFactory.createNextId(Node.class),
							new Coord(i * length, y)) );
		}
		return nodes;
	}
}

