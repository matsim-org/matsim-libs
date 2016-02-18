package playground.sergioo.visualizer2D2012.networkVisualizer.networkPainters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.sergioo.visualizer2D2012.Camera;

public class DynamicNetworkPainterManager {
	
	//Attributes
	protected Network network;
	protected Id<Link> selectedLinkId;
	protected Id<Node> selectedNodeId;
	protected SortedMap<Double, Set<Link>> selectedLinks = new TreeMap<Double, Set<Link>>();
	protected SortedMap<Double, Set<Node>> selectedNodes = new TreeMap<Double, Set<Node>>();
	private double time;
	private double timeStep = 900;
	private double totalTime = 30*3600;
	
	//Methods
	/**
	 * @param network
	 */
	public DynamicNetworkPainterManager(Network network) {
		super();
		this.network = network;
	}
	public DynamicNetworkPainterManager(Network network, double timeStep, double totalTime) {
		super();
		this.network = network;
		this.timeStep = timeStep;
		this.totalTime = totalTime;
	}
	public Network getNetwork() {
		return network;
	}
	public void setNetwork(Network network) {
		this.network = network;
	}
	public Collection<? extends Link> getNetworkLinks(Camera camera) throws Exception {
		if(camera!=null) {
			Collection<Link> links =  new HashSet<Link>();
			double[] from = new double[2];
			double[] to = new double[2];
			double[] center = new double[2];
			for(Link link:network.getLinks().values()) {
				from[0] = link.getFromNode().getCoord().getX();
				from[1] = link.getFromNode().getCoord().getY();
				to[0] = link.getToNode().getCoord().getX();
				to[1] = link.getToNode().getCoord().getY();
				center[0] = link.getCoord().getX();
				center[1] = link.getCoord().getY();
				if(camera.isInside(from) || camera.isInside(to) || camera.isInside(center))
					links.add(link);
			}
			return links;
		}
		else
			throw new Exception("No camera defined");
	}
	public Collection<? extends Node> getNetworkNodes(Camera camera) throws Exception {
		if(camera!=null) {
			Collection<Node> nodes =  new HashSet<Node>();
			for(Node node:network.getNodes().values()) {
				double[] point = new double[]{node.getCoord().getX(), node.getCoord().getY()};
				if(camera.isInside(point))
					nodes.add(node);
			}
			return nodes;
		}
		else
			throw new Exception("No camera defined");
	}
	public Link getSelectedLink() {
		if(selectedLinkId != null)
			return network.getLinks().get(selectedLinkId);
		return null;
	}
	public Link getOppositeToSelectedLink() {
		Id<Link> id = getIdOppositeLink(getSelectedLink());
		if(id != null)
			return network.getLinks().get(id);
		return null;
	}
	public Node getSelectedNode() {
		if(selectedNodeId != null)
			return network.getNodes().get(selectedNodeId);
		return null;
	}
	public Collection<? extends Link> getNetworkLinks() {
		return network.getLinks().values();
	}
	protected Id<Link> getIdNearestLink(double x, double y) {
		Coord coord = new Coord(x, y);
		Link nearest = null;
		double nearestDistance = Double.POSITIVE_INFINITY;
		for(Link link: network.getLinks().values()) {
			double distance = ((LinkImpl) link).calcDistance(coord); 
			if(distance<nearestDistance) {
				nearest = link;
				nearestDistance = distance;
			}
		}
		return nearest.getId();
	}
	public Id<Link> getIdOppositeLink(Link link) {
		for(Link nLink: network.getLinks().values()) {
			if(nLink.getFromNode().equals(link.getToNode()) && nLink.getToNode().equals(link.getFromNode()))
				return nLink.getId();
		}
		return null;
	}
	protected Id<Node> getIdNearestNode(double x, double y) {
		Coord coord = new Coord(x, y);
		Node nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for(Node node:network.getNodes().values()) {
			double distance = CoordUtils.calcEuclideanDistance(coord, node.getCoord());
			if(distance<nearestDistance) {
				nearestDistance = distance;
				nearest = node;
			}
		}
		return nearest.getId();
	}
	public void selectLink(double x, double y) {
		selectedLinkId = getIdNearestLink(x, y);
	}
	public void selectLink(Link link) {
		selectedLinkId = link.getId();
	}
	public void selectOppositeLink() {
		if(selectedLinkId!=null)
			selectedLinkId = getIdOppositeLink(network.getLinks().get(selectedLinkId));
	}
	public void unselectLink() {
		selectedLinkId = null;
	}
	public void selectNode(double x, double y) {
		selectedNodeId = getIdNearestNode(x, y);
	}
	public void unselectNode() {
		selectedNodeId = null;
	}
	public String refreshLink() {
		return selectedLinkId==null?"":selectedLinkId.toString();
	}
	public String refreshNode() {
		return selectedNodeId==null?"":selectedNodeId.toString();
	}
	public Link selectLink(String id) {
		Link link = network.getLinks().get(Id.createLinkId(id));
		if(link!=null)
			selectedLinkId = link.getId();
		else
			selectedLinkId = null;
		return link;
	}
	public Node selectNode(String id) {
		Node node = network.getNodes().get(Id.createNodeId(id));
		if(node!=null)
			selectedNodeId = node.getId();
		else
			selectedNodeId = null;
		return node;
	}
	public void selectLinks(Collection<Link> links, Collection<Double> startTimes, Collection<Double> endTimes) {
		if(links.size()==startTimes.size() && startTimes.size()==endTimes.size()) {
			for(double time = 0; time<totalTime; time+=timeStep) {
				Set<Link> cLinks = selectedLinks.get(time);
				if(cLinks==null) {
					cLinks = new HashSet<Link>();
					selectedLinks.put(time, cLinks);
				}
				Iterator<Link> linksI = links.iterator();
				Iterator<Double> startTimesI = startTimes.iterator();
				Iterator<Double> endTimesI = endTimes.iterator();
				while(linksI.hasNext()) {
					double start = startTimesI.next(), end = endTimesI.next();
					Link link = linksI.next();
					if(start<time && time<end)
						cLinks.add(link);
				}
			}
		}
	}
	public void selectLinkIds(Collection<Id<Link>> linkIds, Collection<Double> startTimes, Collection<Double> endTimes) {
		if(linkIds.size()==startTimes.size() && startTimes.size()==endTimes.size()) {
			for(double time = 0; time<totalTime; time+=timeStep) {
				Set<Link> links = new HashSet<Link>();
				Iterator<Id<Link>> linkIdsI = linkIds.iterator();
				Iterator<Double> startTimesI = startTimes.iterator();
				Iterator<Double> endTimesI = endTimes.iterator();
				while(linkIdsI.hasNext()) {
					double start = startTimesI.next(), end = endTimesI.next();
					Id<Link> linkId = linkIdsI.next();
					if(start<time && time<end)
						links.add(network.getLinks().get(linkId));
				}
				selectedLinks.put(time, links);
			}
		}
	}
	public Collection<? extends Link> getSelectedLinks() {
		Collection<Link> links = selectedLinks.get(time);
		if(links==null)
			return new ArrayList<Link>();
		else
			return links;
	}
	public void selectNodes(Collection<Node> nodes, Collection<Double> startTimes, Collection<Double> endTimes) {
		if(nodes.size()==startTimes.size() && startTimes.size()==endTimes.size()) {
			for(double time = 0; time<totalTime; time+=timeStep) {
				Set<Node> cNodes = new HashSet<Node>();
				Iterator<Node> nodesI = nodes.iterator();
				Iterator<Double> startTimesI = startTimes.iterator();
				Iterator<Double> endTimesI = endTimes.iterator();
				while(nodesI.hasNext()) {
					double start = startTimesI.next(), end = endTimesI.next();
					Node node = nodesI.next();
					if(start<time && time<end)
						cNodes.add(node);
				}
			}
		}
	}
	public  Collection<? extends Node> getSelectedNodes() {
		Collection<Node> nodes = selectedNodes.get(time);
		if(nodes==null)
			return new ArrayList<Node>();
		else
			return nodes;
	}
	public void setTime(double time) {
		this.time = time;
	}
	public double getTime() {
		return time;
	}
	public double getTimeStep() {
		return timeStep;
	}
	public double getTotalTime() {
		return totalTime;
	}

}
