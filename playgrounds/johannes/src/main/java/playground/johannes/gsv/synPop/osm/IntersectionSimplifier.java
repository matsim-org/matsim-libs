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
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.NetworkRunnable;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;

import java.util.*;

/**
 * @author johannes
 *
 */
public class IntersectionSimplifier implements NetworkRunnable {

	/* (non-Javadoc)
	 * @see org.matsim.core.api.internal.NetworkRunnable#run(org.matsim.api.core.v01.network.Network)
	 */
	@Override
	public void run(Network network) {
		Queue<Node> pendingNodes = new LinkedList<Node>(network.getNodes().values());
		
		double[] env = NetworkUtils.getBoundingBox(pendingNodes);
		QuadTree<Node> quadTree = new QuadTree<Node>(env[0], env[1], env[2], env[3]);
		for(Node node : pendingNodes) {
			quadTree.put(node.getCoord().getX(), node.getCoord().getY(), node);
		}
		
		long linkIdCounter = 100000000000L;
		
		while(!pendingNodes.isEmpty()) {
			Node node = pendingNodes.poll();
			
			double radius = 30;
			
			double minx = node.getCoord().getX() - radius;
			double miny = node.getCoord().getY() - radius;
			double maxx = node.getCoord().getX() + radius;
			double maxy = node.getCoord().getY() + radius;
			
			Set<Node> intersectionNodes = new HashSet<Node>(20);
			quadTree.getRectangle(minx, miny, maxx, maxy, intersectionNodes);
			
			if(intersectionNodes.size() > 1) {
			Set<Node> sourceNodes = new HashSet<Node>();
			Map<Node, Set<Link>> inLinks = new HashMap<Node, Set<Link>>();
			Set<Node> targetNodes = new HashSet<Node>();
			Map<Node, Set<Link>> outLinks = new HashMap<Node, Set<Link>>();
			
			for(Node intersectionNode : intersectionNodes) {
				for(Link link : intersectionNode.getInLinks().values()) {
					Node fromNode = link.getFromNode();
					
					double x = fromNode.getCoord().getX();
					double y = fromNode.getCoord().getY();
					
					if(!(x > minx && y > miny && x < maxx && y < maxy)) {
						sourceNodes.add(fromNode);
						
						Set<Link> links = inLinks.get(fromNode);
						if(links == null) {
							links = new HashSet<Link>();
							inLinks.put(fromNode, links);
						}
						links.add(link);
					}
				}
				
				for(Link link : intersectionNode.getOutLinks().values()) {
					Node toNode = link.getToNode();
					
					double x = toNode.getCoord().getX();
					double y = toNode.getCoord().getY();
					
					if(!(x > minx && y > miny && x < maxx && y < maxy)) {
						targetNodes.add(toNode);
						
						Set<Link> links = outLinks.get(toNode);
						if(links == null) {
							links = new HashSet<Link>();
							outLinks.put(toNode, links);
						}
						links.add(link);
					}
				}
			}
			
			for(Node intersectionNode : intersectionNodes) {
				network.removeNode(intersectionNode.getId());
				quadTree.remove(intersectionNode.getCoord().getX(), intersectionNode.getCoord().getY(), intersectionNode);
				pendingNodes.remove(intersectionNode);
			}
			
			NetworkFactory factory = network.getFactory();
			
			Node centerNode = factory.createNode(node.getId(), centerOfMass(intersectionNodes));
			network.addNode(centerNode);
			quadTree.put(centerNode.getCoord().getX(), centerNode.getCoord().getY(), centerNode);
			
			for(Node source : sourceNodes) {
				Link newLink = factory.createLink(Id.create(linkIdCounter++, Link.class), source, centerNode);
				network.addLink(newLink);
				Set<Link> origLinks = inLinks.get(source);
				assignProps(origLinks, newLink);
			}
			
			for(Node target : targetNodes) {
				Link newLink = factory.createLink(Id.create(linkIdCounter++, Link.class), centerNode, target);
				network.addLink(newLink);
				Set<Link> origLinks = outLinks.get(target);
				assignProps(origLinks, newLink);
			}
		}
		}
		
	}

	private Coord centerOfMass(Collection<Node> nodes) {
		double xsum = 0;
		double ysum = 0;
		
		for(Node node : nodes) {
			xsum += node.getCoord().getX();
			ysum += node.getCoord().getY();
		}
		
		double n = nodes.size();
		return new Coord(xsum / n, ysum / n);
	}
	
	private void assignProps(Collection<Link> links, Link link) {
		double capacity = 0;
		double freespeed = 0;
		double lanes = 0;
		for(Link origLink : links) {
			capacity += origLink.getCapacity();
			freespeed = Math.max(freespeed, origLink.getFreespeed());
			lanes += origLink.getNumberOfLanes();
		}
		
		link.setCapacity(capacity);
		link.setFreespeed(freespeed);
		link.setNumberOfLanes(lanes);
		link.setLength(NetworkUtils.getEuclideanDistance(link.getFromNode().getCoord(), link.getToNode().getCoord()));
	}
	
	public static void main(String args[]) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile("/home/johannes/gsv/osm/germany-network-cat5.simplified3.xml");

		IntersectionSimplifier simplifier = new IntersectionSimplifier();
		simplifier.run(network);
		
		new NetworkWriter(network).write("/home/johannes/gsv/osm/germany-network-cat5.simplified4.xml");
	}
}
