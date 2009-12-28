package playground.rost.graph.visnetwork;

import org.matsim.api.core.v01.network.Link;

import playground.rost.eaflow.Intervall.src.Intervalls.AccumalatedFlowOnEdge;
import playground.rost.eaflow.Intervall.src.Intervalls.AccumalatedFlowOnEdgeIntervall;

public class OneWayLink {
	
	protected Link forward;
	protected Link backward;
	protected AccumalatedFlowOnEdge accumalatedFlow;
	protected int maxCapacity = 0;
	
	
	public OneWayLink(Link forward, Link backward)
	{
		this.forward = forward;
		this.backward = backward;
		accumalatedFlow = new AccumalatedFlowOnEdge();
	}
	
	public void setMaxCapacity(int maxCapacity)
	{
		this.maxCapacity = maxCapacity;
	}
	
	public int getMaxCapacity()
	{
		return maxCapacity;
	}
	
	public int getFlowAtTime(int time)
	{
		AccumalatedFlowOnEdgeIntervall accFOEI = accumalatedFlow.getIntervallAt(time);
		if(accFOEI != null)
		{
			return accFOEI.getFlow();
		}
		return 0;
	}
	
	public Link getLink()
	{
		return forward;
	}
	
	public void augmentFlowOnLink(int startTime, int arrivalTime, int flow)
	{
		accumalatedFlow.augmentFlowOverTime(startTime, arrivalTime, flow);
	}
	
}
