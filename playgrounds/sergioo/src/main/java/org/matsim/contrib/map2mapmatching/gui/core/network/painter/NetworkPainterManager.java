package org.matsim.contrib.map2mapmatching.gui.core.network.painter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.map2mapmatching.gui.core.Camera;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.geometry.CoordUtils;

public class NetworkPainterManager {
	
	//Attributes
	protected Network network;
	protected Id<Link> selectedLinkId;
	protected Id<Node> selectedNodeId;
	protected Collection<Link> selectedLinks = new ArrayList<Link>();
	protected Collection<Node> selectedNodes = new ArrayList<Node>();
	
	//Methods
	/**
	 * @param network
	 */
	public NetworkPainterManager(Network network) {
		super();
		this.network = network;
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
	public void selectLinks(Collection<Link> links) {
		selectedLinks.addAll(links);
	}
	public void selectLinkIds(Collection<Id<Link>> linkIds) {
		for(Id<Link> linkId:linkIds)
			selectedLinks.add(network.getLinks().get(linkId));
	}
	public void addLink(double x, double y) {
		selectedLinks.add(network.getLinks().get(getIdNearestLink(x, y)));
	}
	public void removeLink(double x, double y) {
		selectedLinks.remove(network.getLinks().get(getIdNearestLink(x, y)));
	}
	public Collection<? extends Link> getSelectedLinks() {
		return selectedLinks;
	}
	public void selectNodes(Collection<Node> nodes) {
		selectedNodes.addAll(nodes);
	}
	public void addNode(double x, double y) {
		selectedNodes.add(network.getNodes().get(getIdNearestNode(x, y)));
	}
	public void removeNode(double x, double y) {
		selectedNodes.remove(network.getNodes().get(getIdNearestNode(x, y)));
	}
	public Collection<? extends Node> getSelectedNodes() {
		return selectedNodes;
	}
	
	
	
}
