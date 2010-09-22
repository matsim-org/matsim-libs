package playground.mzilske.osm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

public class Stitcher {

	boolean broken = false;

	private Network network;

	private NetworkImpl networkForThisRoute = NetworkImpl.createNetwork();

	private LinkedList<Id> forwardStops = new LinkedList<Id>();

	private LinkedList<Id> backwardStops = new LinkedList<Id>();

	private LinkedList<Id> forwardStopLinks = new LinkedList<Id>();

	private LinkedList<Id> backwardStopLinks = new LinkedList<Id>();

	private List<Double> forwardTravelTimes = new LinkedList<Double>();

	private List<Double> backwardTravelTimes = new LinkedList<Double>();

	public Stitcher(Network network) {
		this.network = network;
	}

	public void addForwardStop(org.openstreetmap.osmosis.core.domain.v0_6.Node stop) {
		for (Tag tag : stop.getTags()) {
			if (tag.getKey().startsWith("matsim:node-id")) {
				System.out.println(tag);
				Id nodeId = new IdImpl(tag.getValue());
				if (!networkForThisRoute.getNodes().containsKey(nodeId)) {
					Node nearestNode = (networkForThisRoute).getNearestNode(network.getNodes().get(nodeId).getCoord());
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
				Id nodeId = new IdImpl(tag.getValue());
				if (!networkForThisRoute.getNodes().containsKey(nodeId)) {
					nodeId = (networkForThisRoute).getNearestNode(network.getNodes().get(nodeId).getCoord()).getId();
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

	public List<Id> getForwardRoute() {
		return route(forwardStops, forwardStopLinks, forwardTravelTimes);
	}

	public List<Id> getForwardStopLinks() {
		return forwardStopLinks;
	}

	public List<Id> getBackwardStopLinks() {
		return backwardStopLinks;
	}

	private List<Id> route(List<Id> stopNodes, List<Id> outStopLinks, List<Double> outTravelTimes) {
		if (stopNodes.isEmpty()) {
			return Collections.emptyList();
		}
		List<Id> links = new ArrayList<Id>();
		FreespeedTravelTimeCost cost = new FreespeedTravelTimeCost(-1, 0, 0);
		Dijkstra router = new Dijkstra(networkForThisRoute, cost, cost);
		Iterator<Id> i = stopNodes.iterator();
		Node previous = network.getNodes().get(i.next());
		while (i.hasNext()) {
			Node next = network.getNodes().get(i.next());
			Path leastCostPath = router.calcLeastCostPath(previous, next, 0);
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

	public List<Id> getBackwardRoute() {
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
		Id linkId = new IdImpl(linkIdString);
		Link link = network.getLinks().get(linkId);
		addNode(link.getFromNode());
		addNode(link.getToNode());
		addLink(link);
	}

	private void addLink(Link link) {
		if (!networkForThisRoute.getLinks().containsKey(link.getId())) {
			networkForThisRoute.addLink(networkForThisRoute.getFactory().createLink(link.getId(), link.getFromNode().getId(), link.getToNode().getId()));
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
