package playground.sergioo.passivePlanning2012.core.router.util;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

public class PathV2 extends Path {

	//Constructors
	public PathV2(Path path) {
		super(path.nodes, path.links, path.travelTime, path.travelCost);
	}
	
	//Methods
	public double getLength() {
		double length=0;
		for(Link link:links)
			length += link.getLength();
		return length;
	}
	public double getAverageCapacity() {
		double capacity=0;
		double length=0;
		for(Link link:links) {
			capacity += link.getLength()*link.getCapacity();
			length += link.getLength();
		}
		return capacity/length;
	}
	public double getAverageNumLanes() {
		double numLanes=0;
		double length=0;
		for(Link link:links) {
			numLanes += link.getLength()*link.getNumberOfLanes();
			length += link.getLength();
		}
		return numLanes/length;
	}
	public double getAverageFreeSpeed() {
		double freeSpeed=0;
		double length=0;
		for(Link link:links) {
			freeSpeed += link.getLength()*link.getFreespeed();
			length += link.getLength();
		}
		return freeSpeed/length;
	}
	
}
