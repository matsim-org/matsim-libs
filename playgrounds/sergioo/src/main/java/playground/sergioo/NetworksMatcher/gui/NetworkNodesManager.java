package playground.sergioo.NetworksMatcher.gui;

import java.util.Collection;
import java.util.HashSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters.NetworkManager;

public class NetworkNodesManager extends NetworkManager {

	//Attributes
	private Collection<Id> selectedNodesId;
	
	//Methods
	public NetworkNodesManager(Network network) {
		super(network);
		selectedNodesId = new HashSet<Id>();
	}
	public Collection<Node> getSelectedNodes() {
		Collection<Node> selectedNodes = new HashSet<Node>();
		for(Id selectedNodeId:selectedNodesId)
			selectedNodes.add(network.getNodes().get(selectedNodeId));
		return selectedNodes;
	}
	public void selectNodeFromCollection(double x, double y) {
		selectedNodesId.add(getIdNearestNode(x, y));
	}
	public void unselectNodeFromCollection(double x, double y) {
		selectedNodesId.remove(getIdNearestSelectedNode(x, y));
	}
	private Id getIdNearestSelectedNode(double x, double y) {
		Coord coord = new CoordImpl(x, y);
		Node nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for(Id nodeId:selectedNodesId) {
			Node node = network.getNodes().get(nodeId);
			double distance = CoordUtils.calcDistance(coord, node.getCoord());
			if(distance<nearestDistance) {
				nearestDistance = distance;
				nearest = node;
			}
		}
		return nearest.getId();
	}

}
