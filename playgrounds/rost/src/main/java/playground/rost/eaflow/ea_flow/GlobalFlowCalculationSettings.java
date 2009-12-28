package playground.rost.eaflow.ea_flow;

import org.matsim.api.core.v01.network.Link;

import playground.rost.eaflow.BowGraph.BowTravelTimeCost;
import playground.rost.eaflow.BowGraph.SimpleEdgeTravelTimeCost;
import playground.rost.eaflow.Intervall.src.Intervalls.EdgeIntervalls;
import playground.rost.eaflow.Intervall.src.Intervalls.VertexIntervalls;

public class GlobalFlowCalculationSettings {

	public enum EdgeTypeEnum
	{
		SIMPLE,
		BOWEDGES_ADD,
	}
	
	//TODO DO NOT CHANGE useHoldover TO TRUE!
	//IT WONT WORK, YET!
	public static boolean useHoldover = false;
	
	public static final String superSinkId = "SUPERSINK";
	
	public static void enableDebuggingForAllFlowRelatedClasses()
	{
		MultiSourceEAF.debug(true);
		BellmanFordVertexIntervalls.debug(3);
		VertexIntervalls.debug(true);
		EdgeIntervalls.debug(true);
		Flow.debug(3);
	}
	
	public static void disableDebuggingForAllFlowRelatedClasses()
	{
		MultiSourceEAF.debug(false);
		BellmanFordVertexIntervalls.debug(0);
		VertexIntervalls.debug(false);
		EdgeIntervalls.debug(false);
		Flow.debug(0);
	}
	
	public static FlowEdgeTraversalCalculator getFlowEdgeTraversalCalculator(EdgeTypeEnum edgeTypeToUse, Link link)
	{
		if(edgeTypeToUse == EdgeTypeEnum.SIMPLE)
		{
			return new SimpleEdgeTravelTimeCost(link);
		}
		else if(edgeTypeToUse == EdgeTypeEnum.BOWEDGES_ADD)
		{
			return new BowTravelTimeCost(link);
		}
		throw new RuntimeException("edgeType null or no FlowEdgeTraversalCalculator defined for edgeType!");
	}
	
	public static String edgeTypeToString(EdgeTypeEnum edgeType)
	{
		if(edgeType == EdgeTypeEnum.SIMPLE)
		{
			return "_Simple";
		}
		else if(edgeType == EdgeTypeEnum.BOWEDGES_ADD)
		{
			return "_Bow_Add";
		}
		throw new RuntimeException("edgeType null or no FlowEdgeTraversalCalculator defined for edgeType!");
	}
}
