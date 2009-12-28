/* *********************************************************************** *
 * project: org.matsim.*												   *	
 * GlobalFlowCalculationSettings.java									   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */


package playground.dressler.ea_flow;

//matsim imports
import org.matsim.api.core.v01.network.Link;

//playground imports
import playground.dressler.ea_flow.BowTravelTimeCost;
import playground.dressler.ea_flow.SimpleEdgeTravelTimeCost;
import playground.dressler.Intervall.src.Intervalls.EdgeIntervalls;
import playground.dressler.Intervall.src.Intervalls.VertexIntervalls;

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
		EdgeIntervalls.debug(3);
		Flow.debug(3);
	}
	
	public static void disableDebuggingForAllFlowRelatedClasses()
	{
		MultiSourceEAF.debug(false);
		BellmanFordVertexIntervalls.debug(0);
		VertexIntervalls.debug(false);
		EdgeIntervalls.debug(0);
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
