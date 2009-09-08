package playground.rost.eaflow.BowGraph;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelMinCost;
import org.matsim.core.router.util.TravelTime;

import playground.rost.eaflow.BowGraph.BowTravelTimeCost.BowEdges.BowEdge;

public class BowTravelTimeCost implements TravelMinCost, TravelTime {
	
	public class BowEdges
	{
		protected int maxCapacity;
		protected ArrayList<BowEdge> bowEdges = new ArrayList<BowEdge>();
		
		public class BowEdge
		{
			public int capacity;
			public int traveltime;
			public int minFlowToUseThisEdge;
			public int maxFlowToUseThisEdge;
			public BowEdge(int capacity, int traveltime, int minFlowToUseThisEdge, int maxFlowToUseThisEdge)
			{
				this.capacity = capacity;
				this.traveltime = traveltime;
				this.minFlowToUseThisEdge = minFlowToUseThisEdge;
				this.maxFlowToUseThisEdge = maxFlowToUseThisEdge;
				
			}
		}
		
		public BowEdges(Link link)
		{
			maxCapacity = (int)link.getCapacity(1.);
			bowEdges.add(new BowEdge(maxCapacity / 2, (int)link.getLength() / (int)link.getFreespeed(1.), 0,maxCapacity/2));
			bowEdges.add(new BowEdge(maxCapacity / 2, 2*(int)link.getLength() / (int)link.getFreespeed(1.),maxCapacity/2+1,maxCapacity));
		}
		
		public Integer getTravelTimeForFlow(int flow)
		{
			if(flow > this.maxCapacity)
				return null;
			int l = 0; 
			int r = bowEdges.size()-1;
			int m = (l+r)/2;
			BowEdge bEdge;
			do
			{		
				bEdge = bowEdges.get(m);
				if(flow >= bEdge.minFlowToUseThisEdge && flow <= bEdge.maxFlowToUseThisEdge)
					return bEdge.traveltime;
				else if(flow < bEdge.minFlowToUseThisEdge)
					r = m-1;
				else if(flow > bEdge.maxFlowToUseThisEdge)
					l = m+1;
				m = (l+r)/2;
			}while(l-r >= 0);
			return null;
		}
		
		public int getMinimalTravelTime()
		{
			return bowEdges.get(0).traveltime;
		}
		
		public int getMaximalTravelTime()
		{
			return bowEdges.get(bowEdges.size()-1).traveltime;
		}
		
	}
	
	

	protected Link link;
	protected BowEdges bowEdges;
	
	
	
	public BowTravelTimeCost(Link link)
	{
		this.link = link;
		bowEdges = new BowEdges(link);
		
	}
	
	
	public double getLinkTravelCost(Link link, double time) {
		return Math.round((link.getLength() / link.getFreespeed(0)));
	}

	public double getLinkTravelTime(Link link, double time) {
		return 0; 
	}

	public double getLinkMinimumTravelCost(Link link) {
		return Math.round((link.getLength() / link.getFreespeed(0)));
	}
	
	public double getLinkTravelCost(int currentFlow)
	{
		int capacity = (int)link.getCapacity(1.);
		if(currentFlow < capacity / 2)
		{
			return Math.round((link.getLength() / link.getFreespeed(0)));
		}
		else
		{
			return 2*Math.round((link.getLength() / link.getFreespeed(0)));
		}
	}
	
	public static BowTravelTimeCost get(TravelMinCost tmCost)
	{
		if(tmCost instanceof BowTravelTimeCost)
		{
			return (BowTravelTimeCost)tmCost;
		}
		return null;
	}
	
	public int getRemainingCapacityWithThisTravelTime(int currentFlow)
	{
		int capacity = (int)link.getCapacity(1.);
		int breakpoint = capacity / 2;
		int result = 0;
		if(currentFlow < breakpoint)
			result = breakpoint - currentFlow;
		else
			result = capacity - currentFlow;
		if(result == 0)
			System.out.println("PROBLEM!");
		return result;
	}
	
	public int getRemainingBackwardCapacityWithThisTravelTime(int currentFlow)
	{
		int capacity = (int)link.getCapacity(1.);
		int breakpoint = capacity / 2;
		if(currentFlow <= breakpoint)
			return currentFlow;
		else 
			return currentFlow-breakpoint;
	}

	public final BowEdges getBowEdges()
	{
		return bowEdges;
	}
	
	public Integer getCurrentTravelTimeForFlow(int flow)
	{
		return this.bowEdges.getTravelTimeForFlow(flow);
	}
	
	
	public int getMinimalTravelTime()
	{
		return this.bowEdges.getMinimalTravelTime();
	}
	
	public int getMaximalTravelTime()
	{
		return this.bowEdges.getMaximalTravelTime();
	}
	
	public Integer getTravelTimeForAdditionalFlow(int flow)
	{
		return this.bowEdges.getTravelTimeForFlow(flow+1);
	}
}
