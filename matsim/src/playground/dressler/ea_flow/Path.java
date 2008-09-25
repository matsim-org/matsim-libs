/* *********************************************************************** *
 * project: org.matsim.*
 * FakeTravelTimeCost.java
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

//java imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

// other imports
import playground.dressler.Intervall.src.Intervalls.*;

// matsim imports
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Route;
import org.matsim.router.util.LeastCostPathCalculator;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;
/**
 * Class representing a path with flow over time on an network
 * @author Manuel Schneider
 *
 */
public class Path {
	
	/**
	 * amount of flow on the path
	 */
	private int _flow;
	
	/**
	 * the actual path in order from the sink
	 */
	private ArrayList<PathEdge> _edges;
	
	/**
	 * Class representing a path with flow over time on an network
	 * @author Manuel Schneider
	 *
	 */
	private class PathEdge {
		
		/**
		 * Edge in a path
		 */
		Link edge;
		
		/**
		 * time upon wich the flow enters the dge
		 */
		int time;
		
		/**
		 * reminder if this is a forward edge or not
		 */
		boolean forward;
		
		PathEdge(Link edge,int time, boolean forward){
			this.time = time;
			this.edge = edge;
			this.forward = forward;
		}
	}

}
