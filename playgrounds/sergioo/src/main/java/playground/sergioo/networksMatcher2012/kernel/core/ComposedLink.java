package playground.sergioo.networksMatcher2012.kernel.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

public abstract class ComposedLink implements Link {
	private Link delegate ;
	
	//Attributes
	
	private final List<Link> links;
	
	
	//Static methods
	
	public static double getAngle(Link link) {
		return Math.atan2(link.getToNode().getCoord().getY()-link.getFromNode().getCoord().getY(), link.getToNode().getCoord().getX()-link.getFromNode().getCoord().getX());
	}
	
	public static double getLength(Link link) {
		return Math.hypot(link.getToNode().getCoord().getY()-link.getFromNode().getCoord().getY(), link.getToNode().getCoord().getX()-link.getFromNode().getCoord().getX());
	}
	
	//Methods
	
	public ComposedLink(Link link, Node from, Node to, Network network) {
		delegate = NetworkUtils.createLink(link.getId(), from, to, network, 0, 0, 0, 0);
		links = new ArrayList<>();
		links.add(link);
	}
	
	public ComposedLink(Id<Link> id, Node from, Node to, Network network) {
		delegate = NetworkUtils.createLink(id, from, to, network, 0, 0, 0, 0);
		links = new ArrayList<>();
	}

	public List<Link> getLinks() {
		return links;
	}
	
	public double getAngle() {
		Node to = this.getToNode() ;
		Node from = this.getFromNode() ;

		return Math.atan2(to.getCoord().getY()-from.getCoord().getY(), to.getCoord().getX()-from.getCoord().getX());
	}

	public double getAngle(boolean in) {
		return getAngle(this, in);
	}
	
	public double getAngle(ComposedLink link, boolean in) {
		if(link.getLinks().size()==1 && link.getLinks().get(0).getClass().equals(Link.class))
			return Math.atan2(link.getLinks().get(0).getToNode().getCoord().getY()-link.getLinks().get(0).getFromNode().getCoord().getY(), link.getLinks().get(0).getToNode().getCoord().getX()-link.getLinks().get(0).getFromNode().getCoord().getX());
		else if(in) {
			for(Link subLink:link.getLinks())
				if(subLink.getToNode().getId().equals(link.getToNode().getId()))
					if(subLink.getClass().equals(Link.class))
						return Math.atan2(link.getLinks().get(0).getToNode().getCoord().getY()-link.getLinks().get(0).getFromNode().getCoord().getY(), link.getLinks().get(0).getToNode().getCoord().getX()-link.getLinks().get(0).getFromNode().getCoord().getX());
					else		
						return getAngle((ComposedLink) subLink, in);
		}
		else
			for(Link subLink:link.getLinks())
				if(subLink.getFromNode().getId().equals(link.getFromNode().getId()))
					if(subLink.getClass().equals(Link.class))
						return Math.atan2(link.getLinks().get(0).getToNode().getCoord().getY()-link.getLinks().get(0).getFromNode().getCoord().getY(), link.getLinks().get(0).getToNode().getCoord().getX()-link.getLinks().get(0).getFromNode().getCoord().getX());
					else		
						return getAngle((ComposedLink) subLink, in);
		return Double.NaN;
	}

	@Override
	public Id<Link> getId() {
		return this.delegate.getId();
	}

	@Override
	public Coord getCoord() {
		return this.delegate.getCoord();
	}

	@Override
	public boolean setFromNode(Node node) {
		return this.delegate.setFromNode(node);
	}

	@Override
	public boolean setToNode(Node node) {
		return this.delegate.setToNode(node);
	}

	@Override
	public Node getToNode() {
		return this.delegate.getToNode();
	}

	@Override
	public Node getFromNode() {
		return this.delegate.getFromNode();
	}

	@Override
	public double getLength() {
		return this.delegate.getLength();
	}

	@Override
	public double getNumberOfLanes() {
		return this.delegate.getNumberOfLanes();
	}

	@Override
	public double getNumberOfLanes(double time) {
		return this.delegate.getNumberOfLanes(time);
	}

	@Override
	public double getFreespeed() {
		return this.delegate.getFreespeed();
	}

	@Override
	public double getFreespeed(double time) {
		return this.delegate.getFreespeed(time);
	}

	@Override
	public double getCapacity() {
		return this.delegate.getCapacity();
	}

	@Override
	public double getCapacity(double time) {
		return this.delegate.getCapacity(time);
	}

	@Override
	public void setFreespeed(double freespeed) {
		this.delegate.setFreespeed(freespeed);
	}

	@Override
	public void setLength(double length) {
		this.delegate.setLength(length);
	}

	@Override
	public void setNumberOfLanes(double lanes) {
		this.delegate.setNumberOfLanes(lanes);
	}

	@Override
	public void setCapacity(double capacity) {
		this.delegate.setCapacity(capacity);
	}

	@Override
	public void setAllowedModes(Set<String> modes) {
		this.delegate.setAllowedModes(modes);
	}

	@Override
	public Set<String> getAllowedModes() {
		return this.delegate.getAllowedModes();
	}

	@Override
	public double getFlowCapacityPerSec() {
		return this.delegate.getFlowCapacityPerSec();
	}

	@Override
	public double getFlowCapacityPerSec(double time) {
		return this.delegate.getFlowCapacityPerSec(time);
	}
	
}
