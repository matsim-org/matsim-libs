package playground.sergioo.passivePlanning.core.network;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.FastDijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.vehicles.Vehicle;

public class ComposedNode implements Node {

	//Constants
	public static final String SEPARATOR = "-";

	//Attributes
	private Coord coord;
	private final Id id;
	private Map<Id, Link> inLinks;
	private Map<Id, Link> outLinks;
	private final Set<Node> nodes;

	public ComposedNode(Node node) {
		id = new IdImpl(node.getId().toString()+SEPARATOR);
		coord = new CoordImpl(node.getCoord().getX(), node.getCoord().getY());
		inLinks = new HashMap<Id, Link>();
		outLinks = new HashMap<Id, Link>();
		nodes = new HashSet<Node>();
		nodes.add(node);
	}
	public static ComposedNode createComposedNode(final Set<Node> nodes, String mode) {
		if(!(nodes==null) && !(nodes.size()==0) && inOutNode(nodes, mode))
			return new ComposedNode(nodes);
		else
			return null;
	}
	private static boolean inOutNode(Set<Node> nodes, String mode) {
		//Create sub-network and determine incident links
		Network subNetwork = NetworkImpl.createNetwork();
		Set<Link> inLinks = new HashSet<Link>();
		Set<Link> outLinks = new HashSet<Link>();
		for(Node node:nodes) {
			subNetwork.addNode(node);
			for(Link link:node.getInLinks().values())
				if(nodes.contains(link.getFromNode()))
					if(link.getAllowedModes().contains(mode)) {
						subNetwork.addNode(link.getFromNode());
						subNetwork.addLink(link);
					}
					else
						return false;
				else if(link.getAllowedModes().contains(mode))
					inLinks.add(link);
			for(Link link:node.getOutLinks().values())
				if(nodes.contains(link.getToNode()))
					if(link.getAllowedModes().contains(mode)) {
						subNetwork.addNode(link.getToNode());
						subNetwork.addLink(link);
					}
					else
						return false;
				else if(link.getAllowedModes().contains(mode))
					outLinks.add(link);
		}
		//Test paths for all combinations
		TravelDisutility travelMinCost =  new TravelDisutility() {
			@Override
			public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
				return getLinkMinimumTravelDisutility(link);
			}
			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return link.getLength()/link.getFreespeed();
			}
		};
		PreProcessDijkstra preProcessData = new PreProcessDijkstra();
		preProcessData.run(subNetwork);
		TravelTime timeFunction = new TravelTime() {	
			public double getLinkTravelTime(Link link, double time) {
				return link.getLength()/link.getFreespeed();
			}
		};
		LeastCostPathCalculator leastCostPathCalculator = new FastDijkstra(subNetwork, travelMinCost, timeFunction, preProcessData);
		for(Link inLink:inLinks)
			for(Link outLink:outLinks)
				if(leastCostPathCalculator.calcLeastCostPath(inLink.getToNode(), outLink.getFromNode(), 0, null, null) == null)
					return false;
		return true;
	}
	private ComposedNode(final Set<Node> nodes) {
		String idText = "";
		for(Node node:nodes)
			idText+=node.getId()+SEPARATOR;
		idText=idText.substring(0, idText.length()-1);
		id = new IdImpl(idText);
		coord = new CoordImpl(0, 0);
		for(Node node:nodes)
			coord.setXY(coord.getX()+node.getCoord().getX(), coord.getY()+node.getCoord().getY());
		coord.setXY(coord.getX()/nodes.size(), coord.getY()/nodes.size());
		inLinks = new HashMap<Id, Link>();
		outLinks = new HashMap<Id, Link>();
		this.nodes = nodes;
	}
	
	//Methods
	public boolean isConnected() {
		Set<Node> connectedNodes = new HashSet<Node>();
		fillNodes(nodes.iterator().next(), connectedNodes);
		return nodes.size()==connectedNodes.size();
	}
	private void fillNodes(Node node, Set<Node> connectedNodes) {
		connectedNodes.add(node);
		for(Link link:node.getInLinks().values())
			if(nodes.contains(link.getToNode()) && !connectedNodes.contains(link.getToNode()))
				fillNodes(link.getFromNode(), connectedNodes);
		for(Link link:node.getOutLinks().values())
			if(nodes.contains(link.getToNode()) && !connectedNodes.contains(link.getToNode()))
				fillNodes(link.getToNode(), connectedNodes);
	}
	@Override
	public Coord getCoord() {
		return coord;
	}
	@Override
	public Id getId() {
		return id;
	}
	@Override
	public boolean addInLink(Link link) {
		return inLinks.put(link.getId(), link)!=null;
	}
	@Override
	public boolean addOutLink(Link link) {
		return outLinks.put(link.getId(), link)!=null;
	}
	@Override
	public Map<Id, ? extends Link> getInLinks() {
		return inLinks;
	}
	@Override
	public Map<Id, ? extends Link> getOutLinks() {
		return outLinks;
	}
	public Set<Node> getNodes() {
		return nodes;
	}

}
