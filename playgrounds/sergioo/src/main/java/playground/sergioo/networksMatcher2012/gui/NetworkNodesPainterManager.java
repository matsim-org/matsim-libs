package playground.sergioo.networksMatcher2012.gui;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.sergioo.visualizer2D2012.networkVisualizer.networkPainters.NetworkPainterManager;

public class NetworkNodesPainterManager extends NetworkPainterManager {

	private static final String SEPARATOR = " & ";
	//Attributes
	private Set<Id<Node>> selectedNodesId;
	
	//Methods
	public NetworkNodesPainterManager(Network network) {
		super(network);
		selectedNodesId = new HashSet<Id<Node>>();
	}
	public Set<Node> getSelectedNodes() {
		Set<Node> selectedNodes = new HashSet<Node>();
		for(Id<Node> selectedNodeId:selectedNodesId)
			selectedNodes.add(network.getNodes().get(selectedNodeId));
		return selectedNodes;
	}
	public Set<Node> getSelectedNodesAndClear() {
		Set<Node> selectedNodes = getSelectedNodes();
		selectedNodesId.clear();
		return selectedNodes;
	}
	public void selectNodes(Set<Node> nodes) {
		selectedNodesId = new HashSet<Id<Node>>();
		for(Node node:nodes)
			selectedNodesId.add(node.getId());
	}
	public Collection<? extends Link> getLinks(){
		return network.getLinks().values();
	}
	public void selectNearestNode(double x, double y) {
		selectedNodesId.add(getIdNearestNode(x, y));
	}
	public void unselectNearestNode(double x, double y) {
		if(selectedNodesId.size()>0)
			selectedNodesId.remove(getIdNearestSelectedNode(x, y));
	}
	private Id<Node> getIdNearestSelectedNode(double x, double y) {
		Coord coord = new Coord(x, y);
		Node nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for(Id<Node> nodeId:selectedNodesId) {
			Node node = network.getNodes().get(nodeId);
			double distance = CoordUtils.calcEuclideanDistance(coord, node.getCoord());
			if(distance<nearestDistance) {
				nearestDistance = distance;
				nearest = node;
			}
		}
		return nearest.getId();
	}
	public String refreshNodes() {
		String text = "";
		for(Id<Node> selectedNodeId:selectedNodesId)
			text += selectedNodeId.toString()+SEPARATOR;
		return text;
	}
	public void clearNodesSelection() {
		selectedNodesId.clear();
	}

}
