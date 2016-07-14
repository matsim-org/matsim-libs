package playground.toronto.gtfsutils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * modified from mRiser's CreatePseudoNetwork
 * @author Yi
 *
 */

public class CreateNetworkFromTransitSchedule {

	private final TransitSchedule schedule;
	private final Network network;
	private final String prefix;

	private final Map<Tuple<Node, Node>, Link> links = new HashMap<Tuple<Node, Node>, Link>();
	private final Map<Tuple<Node, Node>, TransitStopFacility> stopFacilities = new HashMap<Tuple<Node, Node>, TransitStopFacility>();
	private final Map<TransitStopFacility, Node> nodes = new HashMap<TransitStopFacility, Node>();
	private final Map<TransitStopFacility, Node> startNodes = new HashMap<TransitStopFacility, Node>();
	private final Map<TransitStopFacility, List<TransitStopFacility>> facilityCopies = new HashMap<TransitStopFacility, List<TransitStopFacility>>();

	//ivy's code
	private final ArrayList<Id<Node>> Stop_Nodes = new ArrayList<>();
	private final Map<Tuple<Id<Node>,Id<Node>>,Link> Connection_Link = new HashMap<>();
	
	private long linkIdCounter = 0;
	private long nodeIdCounter = 0;

	private final Set<String> transitModes = Collections.singleton(TransportMode.pt);
	

	public CreateNetworkFromTransitSchedule(final TransitSchedule schedule, final Network network, final String networkIdPrefix) {
		this.schedule = schedule;
		this.network = network;
		this.prefix = networkIdPrefix;
	}

	public void createNetwork(HashMap<Id<Node>,Coord> StopAndCoordinates) {

		List<Tuple<TransitLine, TransitRoute>> toBeRemoved = new LinkedList<Tuple<TransitLine, TransitRoute>>();
		 
		//HashMap<Tuple<Node,Node>,Link> Connection_Link = new HashMap<Tuple<Node,Node>, Link>();

		for (TransitLine tLine : this.schedule.getTransitLines().values()) {
			for (TransitRoute tRoute : tLine.getRoutes().values()) {
				ArrayList<Id<Link>> routeLinks = new ArrayList<Id<Link>>();
				//TransitRouteStop prevStop = null; 
				TransitRouteStop prevStop =  tRoute.getStops().get(0);
				for (int i = 1; i < tRoute.getStops().size();i++) {
					TransitRouteStop stop = tRoute.getStops().get(i);
					///////////////ivy's//////////////////////
					Link link = getNetworkStop(prevStop, stop, StopAndCoordinates);
					routeLinks.add(link.getId());
					prevStop = stop;
				}
                
				if (routeLinks.size() > 0) {
					NetworkRoute route = RouteUtils.createNetworkRoute(routeLinks, this.network);
					tRoute.setRoute(route);
				} else {
					System.err.println("Line " + tLine.getId() + " route " + tRoute.getId() + " has less than two stops. Removing this route from schedule.");
					toBeRemoved.add(new Tuple<TransitLine, TransitRoute>(tLine, tRoute));
				}
			}
		}

		for (Tuple<TransitLine, TransitRoute> remove : toBeRemoved) {
			remove.getFirst().removeRoute(remove.getSecond());
		}
	}
	
	/* modified to make the stop as a node and create link in between
	 * 
	 * */
	////ivy's code////////////////////////////////////////////////////////////////////
	public Link getNetworkStop(final TransitRouteStop fromStop, final TransitRouteStop toStop, HashMap<Id<Node>,Coord> StopAndCoordinates) {
		Node fromNode;
		Node toNode;
		
		Id<Node> fromNodeId = Id.create(fromStop.getStopFacility().getId().toString(), Node.class);
		//IdImpl fromNodeIdfinal = Id.create(fromStop.getStopFacility().getId().toString() + "_STOP");
		Id<Node> fromNodeIdfinal = Id.create(fromStop.getStopFacility().getId().toString(), Node.class);
		Coord fromNodeCoord = StopAndCoordinates.get(fromNodeId);
		fromNode = this.network.getFactory().createNode(fromNodeIdfinal, fromNodeCoord);
		if (this.Stop_Nodes.contains(fromNodeIdfinal) != true){
			this.network.addNode(fromNode);
			this.Stop_Nodes.add(fromNodeIdfinal);
		}
		
		
		Id<Node> toNodeId = Id.create(toStop.getStopFacility().getId().toString(), Node.class);
		//IdImpl toNodeIdfinal = Id.create(toStop.getStopFacility().getId().toString()+"_STOP");
		Id<Node> toNodeIdfinal = Id.create(toStop.getStopFacility().getId().toString(), Node.class);
		Coord toNodeCoord = StopAndCoordinates.get(toNodeId);
		toNode = this.network.getFactory().createNode(toNodeIdfinal, toNodeCoord);
		if (this.Stop_Nodes.contains(toNodeIdfinal) != true){
			this.network.addNode(toNode);
			this.Stop_Nodes.add(toNodeIdfinal);
		}
		
		
		Tuple<Node, Node> connection = new Tuple<Node, Node>(fromNode, toNode);
		Tuple<Id<Node>, Id<Node>> Node_Link = new Tuple<>(fromNodeIdfinal,toNodeIdfinal);
		//System.out.println(connection);
		//System.out.println(connection.hashCode());
		//check for repeated links
		Link link = this.Connection_Link.get(Node_Link);
		//System.out.println(link);
		if (link == null){
			link = createAndAddLink(fromNode,toNode,new Tuple<Node, Node>(toNode, fromNode));
			this.Connection_Link.put(Node_Link, link);
			//this.links.put(connection, link);
		}
		
		return link;
		
	}
	////////////////////////////////////////////////////////////////////////////////

	private Link getNetworkLink(final TransitRouteStop fromStop, final TransitRouteStop toStop) {
		TransitStopFacility fromFacility = (fromStop == null) ? null : fromStop.getStopFacility();
		TransitStopFacility toFacility = toStop.getStopFacility();

		Node fromNode;
		if (fromStop == null) {
			fromNode = this.startNodes.get(toFacility);
			if (fromNode == null) {
				Coord coord = new Coord(toFacility.getCoord().getX() + 50, toFacility.getCoord().getY() + 50);
				fromNode = this.network.getFactory().createNode(Id.create("startnode_" + toFacility.getId(), Node.class), coord);
				this.network.addNode(fromNode);
				++nodeIdCounter;
				this.startNodes.put(toFacility, fromNode);
			}
		} else {
			fromNode = this.nodes.get(fromFacility);
		}

		Node toNode = this.nodes.get(toFacility);
		if (toNode == null) {
			toNode = this.network.getFactory().createNode(Id.create(this.prefix + toFacility.getId(), Node.class), toFacility.getCoord());
			this.network.addNode(toNode);
			++nodeIdCounter;
			this.nodes.put(toFacility, toNode);
		}

		Tuple<Node, Node> connection = new Tuple<Node, Node>(fromNode, toNode);
		Link link = this.links.get(connection);
		if (link == null) {
			link = createAndAddLink(fromNode, toNode, connection);
			if (fromStop == null) {
				createAndAddLink(toNode, fromNode, new Tuple<Node, Node>(toNode, fromNode));
			}

			if (toFacility.getLinkId() == null) {
				toFacility.setLinkId(link.getId());
				this.stopFacilities.put(connection, toFacility);
			} else {
				List<TransitStopFacility> copies = this.facilityCopies.get(toFacility);
				if (copies == null) {
					copies = new ArrayList<TransitStopFacility>();
					this.facilityCopies.put(toFacility, copies);
				}
				Id<TransitStopFacility> newId = Id.create(toFacility.getId().toString() + "." + Integer.toString(copies.size() + 1), TransitStopFacility.class);
				TransitStopFacility newFacility = this.schedule.getFactory().createTransitStopFacility(newId, toFacility.getCoord(), toFacility.getIsBlockingLane());
				newFacility.setStopPostAreaId(toFacility.getId().toString());
				newFacility.setLinkId(link.getId());
				newFacility.setName(toFacility.getName());
				copies.add(newFacility);
				this.nodes.put(newFacility, toNode);
				this.schedule.addStopFacility(newFacility);
				toStop.setStopFacility(newFacility);
				this.stopFacilities.put(connection, newFacility);
			}
		} else {
			toStop.setStopFacility(this.stopFacilities.get(connection));
		}
		return link;
	}

	private Link createAndAddLink(Node fromNode, Node toNode,
			Tuple<Node, Node> connection) {
		Link link;
		//need to change link id naming
		Long LinkId = Integer.parseInt(this.prefix) * (++this.linkIdCounter);
		link = this.network.getFactory().createLink(Id.create(LinkId, Link.class), fromNode, toNode);
		link.setLength(CoordUtils.calcEuclideanDistance(fromNode.getCoord(), toNode.getCoord()));
		//need to change it!
		link.setFreespeed(30.0 / 3.6);
		link.setCapacity(9999);
		link.setNumberOfLanes(1);
		this.network.addLink(link);
		link.setAllowedModes(this.transitModes);
		this.links.put(connection, link);
		return link;
	}

	public Link getLinkBetweenStops(final TransitStopFacility fromStop, final TransitStopFacility toStop) {
		Node fromNode = this.nodes.get(fromStop);
		Node toNode = this.nodes.get(toStop);
		Tuple<Node, Node> connection = new Tuple<Node, Node>(fromNode, toNode);
		return this.links.get(connection);
	}

}
