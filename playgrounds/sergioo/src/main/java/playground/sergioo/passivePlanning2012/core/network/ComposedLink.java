package playground.sergioo.passivePlanning2012.core.network;

import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;

public class ComposedLink implements Link {
	private Link delegate ;

	//Attributes
	private final Node startNode;
	private final Node endNode;

	//Methods
	protected ComposedLink(Id<Link> id, Node from, Node to, Network network, double length, double freespeed, double capacity, double lanes, Node startNode, Node endNode) {
		delegate = NetworkUtils.createLink(id, from, to, network, length, freespeed, capacity, lanes);
		this.startNode = startNode;
		this.endNode = endNode;
	}
	public Node getStartNode() {
		return startNode;
	}
	public Node getEndNode() {
		return endNode;
	}
	public Id<Link> getId() {
		return this.delegate.getId();
	}
	public Coord getCoord() {
		return this.delegate.getCoord();
	}
	public boolean setFromNode(Node node) {
		return this.delegate.setFromNode(node);
	}
	public boolean setToNode(Node node) {
		return this.delegate.setToNode(node);
	}
	public Node getToNode() {
		return this.delegate.getToNode();
	}
	public Node getFromNode() {
		return this.delegate.getFromNode();
	}
	public double getLength() {
		return this.delegate.getLength();
	}
	public double getNumberOfLanes() {
		return this.delegate.getNumberOfLanes();
	}
	public double getNumberOfLanes(double time) {
		return this.delegate.getNumberOfLanes(time);
	}
	public double getFreespeed() {
		return this.delegate.getFreespeed();
	}
	public double getFreespeed(double time) {
		return this.delegate.getFreespeed(time);
	}
	public double getCapacity() {
		return this.delegate.getCapacity();
	}
	public double getCapacity(double time) {
		return this.delegate.getCapacity(time);
	}
	public void setFreespeed(double freespeed) {
		this.delegate.setFreespeed(freespeed);
	}
	public void setLength(double length) {
		this.delegate.setLength(length);
	}
	public void setNumberOfLanes(double lanes) {
		this.delegate.setNumberOfLanes(lanes);
	}
	public void setCapacity(double capacity) {
		this.delegate.setCapacity(capacity);
	}
	public void setAllowedModes(Set<String> modes) {
		this.delegate.setAllowedModes(modes);
	}
	public Set<String> getAllowedModes() {
		return this.delegate.getAllowedModes();
	}
	public double getFlowCapacityPerSec() {
		return this.delegate.getFlowCapacityPerSec();
	}
	public double getFlowCapacityPerSec(double time) {
		return this.delegate.getFlowCapacityPerSec(time);
	}

	@Override
	public Attributes getAttributes() {
		return delegate.getAttributes();
	}
}

	
