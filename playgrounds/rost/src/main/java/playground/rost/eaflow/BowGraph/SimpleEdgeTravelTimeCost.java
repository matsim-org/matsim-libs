package playground.rost.eaflow.BowGraph;

import org.matsim.api.core.v01.network.Link;

import playground.rost.eaflow.ea_flow.FlowEdgeTraversalCalculator;

public class SimpleEdgeTravelTimeCost implements FlowEdgeTraversalCalculator {

	protected Link link;
	protected final int capacity;
	protected final int traveltime;

	public SimpleEdgeTravelTimeCost(Link link)
	{
		this.link = link;
		capacity = (int)link.getCapacity();
		traveltime = (int)(link.getLength() / link.getFreespeed());
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
