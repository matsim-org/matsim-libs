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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.util.Landmarker;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.misc.Counter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.TreeMap;

/**
 * @author thibautd
 */
public class CentralityBasedLandmarker implements Landmarker {
	private static final Logger log = Logger.getLogger( CentralityBasedLandmarker.class );
	private final int nSeeds = 100;
	private final Random random = MatsimRandom.getLocalInstance();
	private final TravelDisutility costFunction;

	private final Comparator<NodeInfo> centralityComparator =
			new Comparator<NodeInfo>() {
				@Override
				public int compare( NodeInfo o1, NodeInfo o2 ) {
					return Double.compare( o1.closeness , o2.closeness );
				}
			};

	public CentralityBasedLandmarker( TravelDisutility costFunction ) {
		this.costFunction = costFunction;
	}

	@Override
	public Node[] identifyLandmarks(
			final int nLandmarks,
			final Network network ) {
		final Coord center = getCenter( network );
		final Collection<Map<Id<Node>,NodeInfo>> sectors =
				calcSectors(
						nLandmarks,
						center,
						network.getNodes().values() );

		final List<Node> landmarks = new ArrayList<>( nLandmarks );
		final Counter counter = new Counter( "find centrality-based landmark for sector # " );
		for ( Map<Id<Node>, NodeInfo> sector : sectors ) {
			counter.incCounter();
			computeCloseness( sector );
			final NodeInfo best = Collections.min( sector.values() , centralityComparator );
			landmarks.add( best.node );

			if ( log.isDebugEnabled() ) {
				log.debug( "Node "+best.node+" at "+best.node.getCoord()+" was chosen as a landmark" );
			}
		}
		counter.printCounter();

		return landmarks.toArray( new Node[ 0 ] );
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

	private Collection<Map<Id<Node>, NodeInfo>> calcSectors(
			final int nLandmarks,
			final Coord center,
			final Collection<? extends Node> nodes) {
		final Collection<Map<Id<Node>, NodeInfo>> sectors = new ArrayList<>();

		ArrayList<double[]> angles = new ArrayList<>();

		// Sort nodes according to angle
		TreeMap<Double, Queue<Node>> sortedNodes = new TreeMap<>();
		for (Node node : nodes) {
			final double x = node.getCoord().getX() - center.getX();
			final double y = node.getCoord().getY() - center.getY();
			final double angle = Math.atan2(y, x) + Math.PI;
			final Queue<Node> nodeList =
					MapUtils.getArbitraryObject(
							angle,
							sortedNodes,
							new MapUtils.Factory<Queue<Node>>() {
								@Override
								public Queue<Node> create() {
									return new ArrayDeque<>( 1 );
								}
							} );
			nodeList.add( node );

			sortedNodes.put(angle, nodeList);
		}

		double lastAngle = 0;
		Iterator<Queue<Node>> it = sortedNodes.values().iterator();
		if (it.hasNext()) {
			// Fill sectors such that each sector contains on average the same number of nodes
			Queue<Node> tmpNodes = it.next();
			for (int i = 0; i < nLandmarks; i++) {

				final Map<Id<Node>, NodeInfo> sector = new HashMap<>();
				sectors.add( sector );
				Node node = null;
				for (int j = 0; j < nodes.size() / nLandmarks; j++) {
					if (tmpNodes.isEmpty()) {
						tmpNodes = it.next();
					}
					node = tmpNodes.remove();
					sector.put( node.getId() , new NodeInfo( node ) );
				}

				// Add the remaining nodes to the last sector
				if (i == nLandmarks - 1) {
					while (it.hasNext() || !tmpNodes.isEmpty() ) {
						if ( tmpNodes.isEmpty() ) {
							tmpNodes = it.next();
						}
						node = tmpNodes.remove();
						sector.put( node.getId() , new NodeInfo( node ) );
					}
				}

				if ( sector.isEmpty()) {
					log.info("There is no node in sector " + i + "!");
					sectors.remove( sector );
				}
				else {
					// Get the angle of the "rightmost" node
					double x = node.getCoord().getX() - center.getX();
					double y = node.getCoord().getY() - center.getY();
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

	private void computeCloseness( Map<Id<Node>,NodeInfo> sector ) {
		final Collection<NodeInfo> seeds = findSeeds( sector );

		for ( NodeInfo seed : seeds ) {
			final Comparator<NodeInfo> comparator = new Comparator<NodeInfo>() {
				@Override
				public int compare( NodeInfo o1, NodeInfo o2 ) {
					return Double.compare( o1.tmpCost , o2.tmpCost );
				}
			};
			final PriorityQueue<NodeInfo> pendingNodes = new PriorityQueue<>( 100, comparator );
			pendingNodes.add( seed );

			for ( NodeInfo i : sector.values() ) {
				i.tmpCost = Double.POSITIVE_INFINITY;
			}

			while ( !pendingNodes.isEmpty() ) {
				final NodeInfo node = pendingNodes.poll();
				double fromTravTime = node.tmpCost;

				for ( Link l : node.node.getOutLinks().values() ) {
					final NodeInfo toNode = sector.get( l.getToNode().getId() );
					if ( toNode == null ) {
						// not in sector
						continue;
					}
					double linkTravTime = this.costFunction.getLinkMinimumTravelDisutility( l );
					double totalTravelTime = fromTravTime + linkTravTime;

					if ( toNode.tmpCost > totalTravelTime ) {
						toNode.tmpCost = totalTravelTime;
						pendingNodes.add( toNode );
					}
				}
			}

			for ( NodeInfo i : sector.values() ) {
				i.closeness += i.tmpCost;
			}
		}
	}

	private Collection<NodeInfo> findSeeds( Map<Id<Node>, NodeInfo> network ) {
		final Collection<NodeInfo> seeds = new ArrayList<>( nSeeds );

		final List<NodeInfo> nodes = new ArrayList<>( network.values() );

		while ( seeds.size() < nSeeds && !nodes.isEmpty() ) {
			seeds.add( nodes.remove( random.nextInt( nodes.size() ) ) );
		}

		return seeds;
	}

	private static class NodeInfo {
		final Node node;
		double closeness = 0;
		double tmpCost = 0;

		private NodeInfo( Node node ) {
			this.node = node;
		}
	}
}
