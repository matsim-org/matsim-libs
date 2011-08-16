package playground.sergioo.NetworkVisualizer.gui;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;

public class NetworkManager {
	
	//Attributes
	private final Network network;
	private final Collection<Tuple<Coord,Coord>> lines;
	private final Collection<Coord> points;
	private Id selectedLinkId;
	private Id selectedNodeId;
	private Tuple<Coord,Coord> selectedLine;
	private Coord selectedPoint;
	
	//Methods
	/**
	 * @param network
	 */
	public NetworkManager(Network network) {
		super();
		this.network = network;
		lines = new ArrayList<Tuple<Coord,Coord>>();
		points = new ArrayList<Coord>();
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
	public Tuple<Coord,Coord> getSelectedLine() {
		return selectedLine;
	}
	public Coord getSelectedPoint() {
		return selectedPoint;
	}
	public Network getNetwork() {
		return network;
	}
	public Collection<? extends Link> getNetworkLinks() {
		return network.getLinks().values();
	}
	public Collection<Tuple<Coord, Coord>> getLines() {
		return lines;
	}
	public Collection<Coord> getPoints() {
		return points;
	}
	private Id getIdNearestLink(double x, double y) {
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
	private Id getIdNearestNode(double x, double y) {
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
	private Tuple<Coord,Coord> getCoordsNearestLine(double x, double y) {
		Coord coord = new CoordImpl(x, y);
		Tuple<Coord,Coord> nearest = null;
		double nearestDistance = Double.POSITIVE_INFINITY;
		for(Tuple<Coord,Coord> line:lines) {
			double distance = CoordUtils.distancePointLinesegment(line.getFirst(), line.getSecond(), coord); 
			if(distance<nearestDistance) {
				nearest = line;
				nearestDistance = distance;
			}
		}
		return nearest;
	}
	private Coord getCoordNearestPoint(double x, double y) {
		Coord coord = new CoordImpl(x, y);
		Coord nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for(Coord point:points) {
			double distance = CoordUtils.calcDistance(coord, point);
			if(distance<nearestDistance) {
				nearestDistance = distance;
				nearest = point;
			}
		}
		return nearest;
	}
	public void selectLink(double x, double y) {
		selectedLinkId = getIdNearestLink(x, y);
	}
	public void selectOppositeLink() {
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
	public void selectLine(double x, double y) {
		selectedLine = getCoordsNearestLine(x, y);
	}
	public void unselectLine() {
		selectedPoint = null;
	}
	public void selectPoint(double x, double y) {
		selectedPoint = getCoordNearestPoint(x, y);
	}
	public void unselectPoint() {
		selectedPoint = null;
	}
	public String refreshLink() {
		return selectedLinkId==null?"":selectedLinkId.toString();
	}
	public String refreshNode() {
		return selectedNodeId==null?"":selectedNodeId.toString();
	}
	public String refreshLine() {
		return selectedLine==null?"":selectedLine.getFirst().getX()+","+selectedLine.getFirst().getY()+" "+selectedLine.getSecond().getX()+","+selectedLine.getSecond().getY();
	}
	public String refreshPoint() {
		return selectedPoint==null?"":selectedPoint.getX()+","+selectedPoint.getY();
	}
	public void addPoint(Coord point) {
		points.add(point);
	}
	public void addLine(Tuple<Coord,Coord> line) {
		lines.add(line);
	}
}
