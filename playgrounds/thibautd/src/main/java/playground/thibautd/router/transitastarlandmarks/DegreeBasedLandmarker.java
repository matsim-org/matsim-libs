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
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.Landmarker;
import org.matsim.core.utils.collections.MapUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

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

		final Collection<Collection<GroupedNode>> sectors = calcSectors(
				nLandmarks,
				getCenter( network ),
				groupedNodes );

		final Collection<Node> landmarks = new ArrayList<>( nLandmarks );
		for ( Collection<GroupedNode> sector : sectors ) {
			final GroupedNode biggest =
					Collections.max(
							sector,
							new Comparator<GroupedNode>() {
								@Override
								public int compare( GroupedNode o1, GroupedNode o2 ) {
									return Double.compare( o1.getDegree(), o2.getDegree() );
								}
							} );
			if ( log.isDebugEnabled() ) {
				log.debug( "Node "+biggest.getRepresentativeNode()+" at "+biggest.getRepresentativeNode().getCoord()+" was chosen as a landmark" );
			}
			landmarks.add( biggest.getRepresentativeNode() );
		}

		return landmarks.toArray( new Node[ nLandmarks ] );
	}

	private Coord getCenter( Network network ) {
		// mass center of the network.
		// alternatively, nodes could be weighted by degree (should not really matter)
		double sumX = 0;
		double sumY = 0;
		int nTerms = 0;

		for ( Node n : network.getNodes().values() ) {
			sumX += n.getCoord().getX();
			sumY += n.getCoord().getY();
			nTerms++;
		}

		return new Coord( sumX / nTerms , sumY / nTerms );
	}

	private Collection<Collection<GroupedNode>> calcSectors(
			final int nLandmarks,
			final Coord center,
			final Collection<GroupedNode> nodes) {
		final Collection<Collection<GroupedNode>> sectors = new ArrayList<>();

		ArrayList<double[]> angles = new ArrayList<double[]>();

		// Sort nodes according to angle
		TreeMap<Double, Queue<GroupedNode>> sortedNodes = new TreeMap<>();
		for (GroupedNode node : nodes) {
			final double x = node.getRepresentativeNode().getCoord().getX() - center.getX();
			final double y = node.getRepresentativeNode().getCoord().getY() - center.getY();
			final double angle = Math.atan2(y, x) + Math.PI;
			final Queue<GroupedNode> nodeList =
					MapUtils.getArbitraryObject(
							angle,
							sortedNodes,
							new MapUtils.Factory<Queue<GroupedNode>>() {
								@Override
								public Queue<GroupedNode> create() {
									return new ArrayDeque<>( 1 );
								}
							} );
			nodeList.add( node );

			sortedNodes.put(angle, nodeList);
		}

		double lastAngle = 0;
		Iterator<Queue<GroupedNode>> it = sortedNodes.values().iterator();
		if (it.hasNext()) {
			// Fill sectors such that each sector contains on average the same number of nodes
			Queue<GroupedNode> tmpNodes = it.next();
			for (int i = 0; i < nLandmarks; i++) {

				final List<GroupedNode> sector = new ArrayList<>();
				sectors.add( sector );
				GroupedNode node = null;
				for (int j = 0; j < nodes.size() / nLandmarks; j++) {
					if (tmpNodes.isEmpty()) {
						tmpNodes = it.next();
					}
					node = tmpNodes.remove();
					sector.add(node);
				}

				// Add the remaining nodes to the last sector
				if (i == nLandmarks - 1) {
					while (it.hasNext() || !tmpNodes.isEmpty() ) {
						if ( tmpNodes.isEmpty() ) {
							tmpNodes = it.next();
						}
						node = tmpNodes.remove();
						sector.add(node);
					}
				}

				if ( sector.isEmpty()) {
					log.info("There is no node in sector " + i + "!");
					sectors.remove( sector );
				}
				else {
					// Get the angle of the "rightmost" node
					double x = node.getRepresentativeNode().getCoord().getX() - center.getX();
					double y = node.getRepresentativeNode().getCoord().getY() - center.getY();
					double angle = Math.atan2(y, x) + Math.PI;
					double[] tmp = new double[2];
					tmp[0] = lastAngle;
					tmp[1] = angle;
					angles.add(tmp);
					lastAngle = angle;
				}
			}
		}

		return sectors;
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

		public Node getRepresentativeNode() {
			return nodes.get( 0 );
		}

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
