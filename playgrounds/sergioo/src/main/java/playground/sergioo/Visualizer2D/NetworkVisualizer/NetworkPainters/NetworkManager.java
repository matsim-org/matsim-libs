package playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters;

import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;

public class NetworkManager {
	
	//Attributes
	protected final Network network;
	private Id selectedLinkId;
	private Id selectedNodeId;
	
	//Methods
	/**
	 * @param network
	 */
	public NetworkManager(Network network) {
		super();
		this.network = network;
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
