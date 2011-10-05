package playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters;

import java.util.Collection;
import java.util.HashSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.sergioo.Visualizer2D.Camera;

public class NetworkPainterManager {
	
	//Attributes
	protected Network network;
	private Id selectedLinkId;
	private Id selectedNodeId;
	
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
			double xMin = camera.getUpLeftCorner().getX();
			double yMin = camera.getUpLeftCorner().getY()+camera.getSize().getY();
			double xMax = camera.getUpLeftCorner().getX()+camera.getSize().getX();
			double yMax = camera.getUpLeftCorner().getY();
			Collection<Link> links =  new HashSet<Link>();
			for(Link link:network.getLinks().values()) {
				Coord from = link.getFromNode().getCoord();
				Coord to = link.getToNode().getCoord();
				Coord center = link.getCoord();
				if((xMin<from.getX() && yMin<from.getY() && xMax>from.getX() && yMax>from.getY()) || (xMin<to.getX() && yMin<to.getY() && xMax>to.getX() && yMax>to.getY())|| (xMin<center.getX() && yMin<center.getY() && xMax>center.getX() && yMax>center.getY()))
					links.add(link);
			}
			return links;
		}
		else
			throw new Exception("No camera defined");
	}
	public Collection<? extends Node> getNetworkNodes(Camera camera) throws Exception {
		if(camera!=null) {
			double xMin = camera.getUpLeftCorner().getX();
			double yMin = camera.getUpLeftCorner().getY()+camera.getSize().getY();
			double xMax = camera.getUpLeftCorner().getX()+camera.getSize().getX();
			double yMax = camera.getUpLeftCorner().getY();
			Collection<Node> nodes =  new HashSet<Node>();
			for(Node node:network.getNodes().values()) {
				Coord point = node.getCoord();
				if(xMin<point.getX()&&yMin<point.getY()&&xMax>point.getX()&&yMax>point.getY())
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
	public Node getSelectedNode() {
		if(selectedNodeId != null)
			return network.getNodes().get(selectedNodeId);
		return null;
	}
	public Collection<? extends Link> getNetworkLinks() {
		return network.getLinks().values();
	}
	protected Id getIdNearestLink(double x, double y) {
		Coord coord = new CoordImpl(x, y);
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
	public Id getIdOppositeLink(Link link) {
		for(Link nLink: network.getLinks().values()) {
			if(nLink.getFromNode().equals(link.getToNode()) && nLink.getToNode().equals(link.getFromNode()))
				return nLink.getId();
		}
		return null;
	}
	protected Id getIdNearestNode(double x, double y) {
		Coord coord = new CoordImpl(x, y);
		Node nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for(Node node:network.getNodes().values()) {
			double distance = CoordUtils.calcDistance(coord, node.getCoord());
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
	
}
