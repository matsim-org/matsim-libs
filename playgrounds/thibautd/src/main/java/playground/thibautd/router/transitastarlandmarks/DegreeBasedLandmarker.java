/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.thibautd.router.transitastarlandmarks;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.Landmarker;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

/**
 * A Landmarker that tries to maximize the degree of the nodes chosen as landmarks.
 * Designed for public transport networks, where choosing hubs as landmarks is natural.
 *
 * @author thibautd
 */
public class DegreeBasedLandmarker implements Landmarker {
	private static final Logger log = Logger.getLogger( DegreeBasedLandmarker.class );
	
	@Override
	public Node[] identifyLandmarks( final int nLandmarks, final Network network ) {
		// First, "aggregate" nodes, so that nodes close enough are considered the same.
		final Collection<GroupedNode> groupedNodes = groupNodes( network , 10 );

		// very simple approach: just get one node from each of the n biggest groups
		// could be improved by looking at geography as well (for instance, avoid having Zurich HB, Oerlikon and Altstetten
		// as landmarks when routing through Switzerland)
		final Queue<GroupedNode> queue =
				new PriorityQueue<>(
						groupedNodes.size(),
						new Comparator<GroupedNode>() {
							@Override
							public int compare( GroupedNode o1, GroupedNode o2 ) {
								return Double.compare( o1.getDegree() , o2.getDegree() );
							}
						} );
		queue.addAll( groupedNodes );

		final Node[] landmarks = new Node[ nLandmarks ];
		for ( int i = 0; i < nLandmarks; i++ ) {
			landmarks[ i ] = queue.poll().nodes.get( 0 );
			if ( log.isDebugEnabled() ) {
				log.debug( "identified node with id"+landmarks[ i ].getId()+" located at "+landmarks[ i ].getCoord()+" as a landmark" );
			}
		}
		return landmarks;
	}

	private Collection<GroupedNode> groupNodes(
			final Network network,
			final double distance ) {
		final Set<Node> remainingNodes = new HashSet<>( network.getNodes().values() );

		final Collection<GroupedNode> grouped = new ArrayList<>();

		for ( Node n : network.getNodes().values() ) {
			if ( remainingNodes.isEmpty() ) break;
			if ( !remainingNodes.remove( n ) ) continue;

			final GroupedNode group = new GroupedNode();
			grouped.add( group );
			group.nodes.add( n );
			final Queue<Node> seeds = new ArrayDeque<>(  );
			seeds.add( n );
			while ( !seeds.isEmpty() ) {
				final Node seed = seeds.remove();
				for ( Link l : seed.getOutLinks().values() ) {
					if ( !remainingNodes.remove( l.getToNode() ) ) continue;
					if ( l.getLength() < distance ) {
						seeds.add( l.getToNode() );
						group.nodes.add( l.getToNode() );
					}
				}
				for ( Link l : seed.getInLinks().values() ) {
					if ( !remainingNodes.remove( l.getFromNode() ) ) continue;
					if ( l.getLength() < distance ) {
						seeds.add( l.getFromNode() );
						group.nodes.add( l.getFromNode() );
					}
				}
			}
		}

		return grouped;
	}

	private static class GroupedNode {
		final List<Node> nodes = new ArrayList<>();

		public int getOutDegree() {
			int d = 0;
			for ( Node n : nodes ) {
				d += n.getOutLinks().size();
			}
			return d;
		}

		public int getInDegree() {
			int d = 0;
			for ( Node n : nodes ) {
				d += n.getInLinks().size();
			}
			return d;
		}

		public int getDegree() {
			// alternatively, could be the number of neighbors or the number of neighbors doubly connected
			return getInDegree() + getOutDegree();
		}
	}
}
