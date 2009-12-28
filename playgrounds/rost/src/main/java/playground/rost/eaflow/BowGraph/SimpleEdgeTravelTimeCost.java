package playground.rost.eaflow.BowGraph;

import java.util.ArrayList;

import org.matsim.api.core.v01.network.Link;

import playground.rost.eaflow.BowGraph.BowTravelTimeCost.BowEdge;
import playground.rost.eaflow.ea_flow.FlowEdgeTraversalCalculator;

public class SimpleEdgeTravelTimeCost implements FlowEdgeTraversalCalculator {

	protected Link link;
	protected final int capacity;	
	protected final int traveltime;
	
	public SimpleEdgeTravelTimeCost(Link link)
	{
		this.link = link;
		capacity = (int)link.getCapacity(1.);
		traveltime = (int)((double)link.getLength() / (double)link.getFreespeed(1.));
	}
	
	
	public int getMaximalTravelTime() {
		return traveltime;
	}

	public int getMinimalTravelTime() {
		return traveltime;
	}

	public int getRemainingBackwardCapacityWithThisTravelTime(int currentFlow) {
		return currentFlow;
	}

	public int getRemainingForwardCapacityWithThisTravelTime(int currentFlow) {
		return capacity - currentFlow;
	}

	public Integer getTravelTimeForAdditionalFlow(int currentFlow) {
		if(currentFlow == capacity)
			return null;
		else return traveltime;
	}

	public Integer getTravelTimeForFlow(int currentFlow) {
		return traveltime;
	}

}
