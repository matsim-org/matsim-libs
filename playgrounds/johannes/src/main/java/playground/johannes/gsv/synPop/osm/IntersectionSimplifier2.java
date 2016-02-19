/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.osm;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;

import java.util.*;

/**
 * @author johannes
 * 
 */
public class IntersectionSimplifier2 {

	private NetworkImpl network;

	private final double maxSearchRadius = 50;

//	private final double minSearchRadius = 5;

//	private final double searchRadiusStep = 5;

	private long maxNodeId;

//	private long maxLinkId;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		NetworkReaderMatsimV1 reader = new NetworkReaderMatsimV1(scenario.getNetwork());
		reader.parse("/home/johannes/gsv/osm/network/germany-20140909.3.xml");
		
		IntersectionSimplifier2 simplifier = new IntersectionSimplifier2();
		simplifier.network = (NetworkImpl) scenario.getNetwork();
		
		simplifier.maxNodeId = Long.MIN_VALUE;
		for(Node node : scenario.getNetwork().getNodes().values()) {
			simplifier.maxNodeId = Math.max(simplifier.maxNodeId, Long.parseLong(node.getId().toString()));
		}
		
		simplifier.run();
		
		NetworkWriter writer = new NetworkWriter(simplifier.network);
		writer.write("/home/johannes/gsv/osm/network/germany-20140909.4.xml");

	}

	private void run() {
		Queue<Node> pendingNodes = new LinkedList<Node>(network.getNodes().values());

		ProgressLogger.init(pendingNodes.size(), 2, 10);
		while (!pendingNodes.isEmpty()) {
			ProgressLogger.step();
			Node node = pendingNodes.poll();

			Node nearest = getNearestConnected(node);
			if (nearest != null) {
				Tuple<Node, Node> pair = new Tuple<Node, Node>(node, nearest);
				if (pair != null) {
					List<Link> remove = new ArrayList<>();
					List<Link> keep = new ArrayList<>();

					for (Link link : pair.getFirst().getOutLinks().values()) {
						Node target = link.getToNode();
						if (target == pair.getSecond()) {
							remove.add(link);
						} else {
							keep.add(link);
						}
					}
					
					for (Link link : pair.getFirst().getInLinks().values()) {
						Node source = link.getFromNode();
						if (source == pair.getSecond()) {
							remove.add(link);
						} else {
							keep.add(link);
						}
					}
					
					for (Link link : pair.getSecond().getOutLinks().values()) {
						Node target = link.getToNode();
						if (target == pair.getFirst()) {
							remove.add(link);
						} else {
							keep.add(link);
						}
					}
					
					for (Link link : pair.getSecond().getInLinks().values()) {
						Node source = link.getFromNode();
						if (source == pair.getFirst()) {
							remove.add(link);
						} else {
							keep.add(link);
						}
					}

					network.removeNode(pair.getFirst().getId());
					network.removeNode(pair.getSecond().getId());

					Id<Node> id = Id.createNodeId(++maxNodeId);
					double x = (pair.getFirst().getCoord().getX() + pair.getSecond().getCoord().getX()) / 2.0;
					double y = (pair.getFirst().getCoord().getY() + pair.getSecond().getCoord().getY()) / 2.0;
					Node newNode = network.getFactory().createNode(id, new Coord(x, y));
					network.addNode(newNode);

					for (Link link : keep) {
						Node toNode, fromNode = null;
						Node oldNode;
						if (link.getToNode() == pair.getFirst() || link.getToNode() == pair.getSecond()) {
							toNode = newNode;
							fromNode = link.getFromNode();
							oldNode = link.getToNode();
						} else if (link.getFromNode() == pair.getFirst() || link.getFromNode() == pair.getSecond()) {
							fromNode = newNode;
							toNode = link.getToNode();
							oldNode = link.getFromNode();
						} else {
							throw new RuntimeException("oops");
						}

						Link newLink = network.getFactory().createLink(link.getId(), fromNode, toNode);
						network.addLink(newLink);
						newLink.setAllowedModes(link.getAllowedModes());
						newLink.setCapacity(link.getCapacity());
						newLink.setFreespeed(link.getFreespeed());

						double dx = x - oldNode.getCoord().getX();
						double dy = y - oldNode.getCoord().getY();
						double d = Math.sqrt(dx * dx + dy * dy);
						newLink.setLength(link.getLength() + d);

						newLink.setNumberOfLanes(link.getNumberOfLanes());
						((LinkImpl)newLink).setOrigId(((LinkImpl)link).getOrigId());
					}
				}
			}
		}
	}

	private Node getNearestConnected(Node seed) {
		Collection<Node> nodes = network.getNearestNodes(seed.getCoord(), maxSearchRadius);
		nodes.remove(seed);
		Node nearest = null;
		if (!nodes.isEmpty()) {
			
			double minDist = Double.MAX_VALUE;
			for (Node node : nodes) {
				Link link = NetworkUtils.getConnectingLink(node, seed);
				if (link == null) {
					link = NetworkUtils.getConnectingLink(seed, node);
				}

				if (link != null) {
					double d = NetworkUtils.getEuclideanDistance(node.getCoord(), seed.getCoord());
					if (d < minDist) {
						minDist = d;
						nearest = node;
					}
				}
			}

		}
		
		return nearest;
	}

	private Tuple<Node, Node> getPair(Collection<Node> candidates) {
		List<Node> list = new ArrayList<>(candidates);
		for (int i = 0; i < list.size(); i++) {
			for (int k = 0; k < list.size(); k++) {
				if (k != i) {
					if (NetworkUtils.getConnectingLink(list.get(i), list.get(k)) != null) {
						return new Tuple<Node, Node>(list.get(i), list.get(k));
					}
				}
			}
		}

		return null;
	}
}
