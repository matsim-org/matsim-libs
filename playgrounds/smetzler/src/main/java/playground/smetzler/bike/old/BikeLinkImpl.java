package playground.smetzler.bike.old;

import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
//import playground.smetzler.bike.BikeLink;
import org.matsim.core.network.NetworkUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;

public class BikeLinkImpl 
implements BikeLink 
{
	private String cycleway;
	private String cyclewaySurface;
	
	Link delegate ;

	protected BikeLinkImpl(Id<Link> id, Node from, Node to, Network network, double length, double freespeed,
			double capacity, double lanes, String cycleway, String cyclewaySurface) {
		delegate = NetworkUtils.createLink(id, from, to, network, length, freespeed, capacity, lanes);
		
		this.cycleway = cycleway;
		this.cyclewaySurface = cyclewaySurface;
		// TODO Auto-generated constructor stub
	}
	
	
	@Override
	public String toString() {
		return super.toString() +
		"[cycleway=" + this.cycleway + "]" +
		"[cyclewaySurface=" + this.cyclewaySurface + "]";
		
	}


	@Override
	public String getcycleway() {
		return this.cycleway;
	}


	@Override
	public void setcycleway(String cycleway) {
		this.cycleway = cycleway;
	}


	@Override
	public String getcyclewaySurface() {
		return this.cyclewaySurface;
	}


	@Override
	public void getcyclewaySurface(String cyclewaySurface) {
		this.cyclewaySurface = cyclewaySurface;
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
		return this.delegate.getAttributes();
	}
}