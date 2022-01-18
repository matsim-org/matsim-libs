/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.vsp.andreas.mzilske.osm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

public class Stitcher {

	boolean broken = false;

	private Network network;

	private Network networkForThisRoute = NetworkUtils.createNetwork();

	private LinkedList<Id<Node>> forwardStops = new LinkedList<>();

	private LinkedList<Id<Node>> backwardStops = new LinkedList<>();

	private LinkedList<Id<Link>> forwardStopLinks = new LinkedList<>();

	private LinkedList<Id<Link>> backwardStopLinks = new LinkedList<>();

	private List<Double> forwardTravelTimes = new LinkedList<Double>();

	private List<Double> backwardTravelTimes = new LinkedList<Double>();

	public Stitcher(Network network) {
		this.network = network;
	}

	public void addForwardStop(org.openstreetmap.osmosis.core.domain.v0_6.Node stop) {
		for (Tag tag : stop.getTags()) {
			if (tag.getKey().startsWith("matsim:node-id")) {
				System.out.println(tag);
				Id<Node> nodeId = Id.create(tag.getValue(), Node.class);
				if (!networkForThisRoute.getNodes().containsKey(nodeId)) {
					Node nearestNode = NetworkUtils.getNearestNode((networkForThisRoute),network.getNodes().get(nodeId).getCoord());
					nodeId = nearestNode.getId();
					System.out.println("  --> " + nodeId);
				}
				forwardStops.add(nodeId);
				return;
			}
		}
		throw new RuntimeException();
	}

	public void addBackwardStop(org.openstreetmap.osmosis.core.domain.v0_6.Node stop) {
		for (Tag tag : stop.getTags()) {
			if (tag.getKey().startsWith("matsim:node-id")) {
				System.out.println(tag);
				Id<Node> nodeId = Id.create(tag.getValue(), Node.class);
				if (!networkForThisRoute.getNodes().containsKey(nodeId)) {
					nodeId = NetworkUtils.getNearestNode((networkForThisRoute),network.getNodes().get(nodeId).getCoord()).getId();
					System.out.println("  --> " + nodeId);
				}
				backwardStops.add(nodeId);
				return;
			}
		}
		throw new RuntimeException();
	}

	public void addBoth(Way way) {
		System.out.println("WayBoth: " + way.getWayNodes().get(0) + " " + way.getWayNodes().get(way.getWayNodes().size()-1));
		addForwardLinks(way);
		addBackwardLinks(way);
	}

	public void addForward(Way way) {
		System.out.println("WayForward: " + way.getWayNodes().get(0) + " " + way.getWayNodes().get(way.getWayNodes().size()-1));
		addForwardLinks(way);
	}

	public void addBackward(Way way) {
		System.out.println("WayBackward: " + way.getWayNodes().get(0) + " " + way.getWayNodes().get(way.getWayNodes().size()-1));
		addBackwardLinks(way);
	}

	public List<Id<Link>> getForwardRoute() {
		return route(forwardStops, forwardStopLinks, forwardTravelTimes);
	}

	public List<Id<Link>> getForwardStopLinks() {
		return forwardStopLinks;
	}

	public List<Id<Link>> getBackwardStopLinks() {
		return backwardStopLinks;
	}

	private List<Id<Link>> route(List<Id<Node>> stopNodes, List<Id<Link>> outStopLinks, List<Double> outTravelTimes) {
		if (stopNodes.isEmpty()) {
			return Collections.emptyList();
		}
		List<Id<Link>> links = new ArrayList<Id<Link>>();
		FreespeedTravelTimeAndDisutility cost = new FreespeedTravelTimeAndDisutility(-1, 0, 0);
		//		LeastCostPathCalculator router = new DijkstraFactory().createPathCalculator(networkForThisRoute, cost, cost);
		LeastCostPathCalculator router = new DijkstraFactory().createPathCalculator(network, cost, cost);
		Iterator<Id<Node>> i = stopNodes.iterator();
		Node previous = network.getNodes().get(i.next());

//		final Node prev2 = networkForThisRoute.getNodes().get( previous.getId() );
//		Gbl.assertNotNull( prev2 );

		while (i.hasNext()) {
			Node next = network.getNodes().get(i.next());

//			final Node next2 = networkForThisRoute.getNodes().get( next.getId() );
//			Gbl.assertNotNull( next2 );

			// This may have been a bug.  The old router would use the network that was connected to the origin node, _not_ the one that was given by the constructor.
			// This evidently started being a problem with mode-specific sub-networks.  Christoph found this at some point and put in a test.  Now (many years later) the
			// code failed that test.  I first tried to repair the nodes, but it turns out that I should not pass the networkForThisRoute but instead the full network to
			// the router.  kai, dec'18

			Path leastCostPath = router.calcLeastCostPath(previous, next, 0, null, null);
			if (leastCostPath == null) {
				System.out.println("No route.");
				return Collections.emptyList();
			}
			for (Link link : leastCostPath.links) {
				links.add(link.getId());
			}
			Link linkForStop;
			double travelTime;
			if (leastCostPath.links.isEmpty()) {
				linkForStop = null;
				travelTime = 0;
				outStopLinks.add(null);
				outTravelTimes.add(travelTime);
			} else {
				linkForStop = leastCostPath.links.get(leastCostPath.links.size()-1);
				travelTime = leastCostPath.travelTime;
				outStopLinks.add(linkForStop.getId());
				outTravelTimes.add(travelTime);
			}
			previous = next;
		}
		return links;
	}

	public List<Id<Link>> getBackwardRoute() {
		return route(backwardStops, backwardStopLinks, backwardTravelTimes);
	}

	private void addForwardLinks(Way way) {
		for (Tag tag : way.getTags()) {
			if (tag.getKey().startsWith("matsim:forward:link-id")) {
				System.out.println(tag);
				addToRoute(tag.getValue());
			}
		}
	}

	private void addBackwardLinks(Way way) {
		for (Tag tag : way.getTags()) {
			if (tag.getKey().startsWith("matsim:backward:link-id")) {
				System.out.println(tag);
				addToRoute(tag.getValue());
			}
		}
	}

	private void addToRoute(String linkIdString) {
		Id<Link> linkId = Id.create(linkIdString, Link.class);
		Link link = network.getLinks().get(linkId);
		addNode(link.getFromNode());
		addNode(link.getToNode());
		addLink(link);
	}

	private void addLink(Link link) {
		if (!networkForThisRoute.getLinks().containsKey(link.getId())) {
			networkForThisRoute.addLink(networkForThisRoute.getFactory().createLink(link.getId(), link.getFromNode(), link.getToNode()));
		}
	}

	private void addNode(Node fromNode) {
		if (!networkForThisRoute.getNodes().containsKey(fromNode.getId())) {
			networkForThisRoute.addNode(networkForThisRoute.getFactory().createNode(fromNode.getId(), fromNode.getCoord()));
		}
	}

	public List<Double> getForwardTravelTimes() {
		return forwardTravelTimes;
	}

	public List<Double> getBackwardTravelTimes() {
		return backwardTravelTimes;
	}

}
