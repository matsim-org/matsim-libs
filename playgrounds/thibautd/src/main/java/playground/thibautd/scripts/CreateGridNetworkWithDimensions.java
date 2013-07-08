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
package playground.thibautd.scripts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.thibautd.utils.UniqueIdFactory;

/**
 * @author thibautd
 */
public class CreateGridNetworkWithDimensions {
	private static final double FREESPEED = 75 * 1000 / 3600;
	private static final double LINK_LENGTH = 1000;
	private static final double LINK_CAPACITY = 100;

	public static void main(final String[] args) {
		final int width = Integer.valueOf( args[ 0 ] ).intValue();
		final int height = Integer.valueOf( args[ 1 ] ).intValue();
		final String outFile = args[ 2 ];

		final Network network = ScenarioUtils.createScenario( ConfigUtils.createConfig() ).getNetwork();
		List<Node> lastHorizontalLine = Collections.emptyList();

		final UniqueIdFactory nodeIdFactory = new UniqueIdFactory( "" );
		for ( int i = 0; i < height; i++ ) {
			final List<Node> newHorizontalLine = createNodes( nodeIdFactory , i * LINK_LENGTH , width , network.getFactory());
			final List<Link> horizontalLinks = createLinks( newHorizontalLine , network.getFactory() );
			final List<Link> verticalLinks = linkLines( lastHorizontalLine , newHorizontalLine , network.getFactory() );

			for ( Node n : newHorizontalLine ) network.addNode( n );
			for ( Link l : horizontalLinks ) network.addLink( l ) ;
			for ( Link l : verticalLinks ) network.addLink( l ) ;

			lastHorizontalLine = newHorizontalLine;
		}

		new NetworkWriter( network ).write( outFile );
	}

	private static List<Link> linkLines(
			final List<Node> lastHorizontalLine,
			final List<Node> newHorizontalLine,
			final NetworkFactory fact) {
		if ( lastHorizontalLine.isEmpty() ) return Collections.emptyList();
		if ( lastHorizontalLine.size() != newHorizontalLine.size() ) throw new IllegalArgumentException();

		final List<Link> links = new ArrayList<Link>();
		for ( int i=0; i < lastHorizontalLine.size(); i++ ) {
			final Node n1 = lastHorizontalLine.get( i );
			final Node n2 = newHorizontalLine.get( i );
			links.add( createLink( fact , n1 , n2 ) );
			links.add( createLink( fact , n2 , n1 ) );
		}

		return links;
	}

	private static List<Link> createLinks(
			final List<Node> newHorizontalLine,
			final NetworkFactory fact) {
		final List<Link> links = new ArrayList<Link>();

		final Iterator<Node> nodeIterator = newHorizontalLine.iterator();
		Node lastNode = nodeIterator.next();

		while ( nodeIterator.hasNext() ) {
			final Node newNode = nodeIterator.next();

			links.add( createLink( fact , lastNode , newNode ) );
			links.add( createLink( fact , newNode , lastNode ) );

			lastNode = newNode;
		}

		return links;
	}

	private static Link createLink(
			final NetworkFactory fact,
			final Node o,
			final Node d) {
		final Link link = fact.createLink(
				new IdImpl( o.getId() +"--"+ d.getId() ),
				o,
				d );
		link.setLength( LINK_LENGTH );
		link.setFreespeed( FREESPEED );
		link.setCapacity( LINK_CAPACITY );

		return link;
	}

	private static List<Node> createNodes(
			final UniqueIdFactory nodeIdFactory,
			final double y,
			final int width,
			final NetworkFactory factory) {
		final List<Node> nodes = new ArrayList<Node>();
		for ( int i=0; i < width; i++ ) {
			nodes.add(
					factory.createNode(
						nodeIdFactory.createNextId(),
						new CoordImpl( i * LINK_LENGTH , y ) ) );
		}
		return nodes;
	}
}

