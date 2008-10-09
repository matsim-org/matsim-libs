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
 * Class representing a dynamic flow on an network
 * @author Manuel Schneider
 *
 */

public class Flow {
	
	
	/**
	 * The network on which we find routes. We expect the network to change
	 * between runs!
	 */
	private NetworkLayer network;
	
	/**
	 * Edgerepresentation of flow on the network  
	 */
	private HashMap<Link, EdgeIntervalls> _flow;
	
	/**
	 * Pathrepresentation of flow on the network
	 */
	private LinkedList _paths;
	

	/**
	 * list of all sources
	 */
	private LinkedList<Node> _sources;
	
	/**
	 * stores unsatisfied demands for each source
	 */
	private HashMap<Node,Integer> _demands;

	/**
	 * the sink, to which all flow is directed
	 */
	private Node _sink;
	
	/**
	 * maximal time Horizon for the flow
	 */
	private int _timeHorizon;
	
	private static boolean _debug = false;
	
	/**
	 * 
	 * @param network
	 * @param flow
	 * @param sources
	 * @param demands
	 * @param sink
	 * @param horizon
	 */
	public Flow(NetworkLayer network, HashMap<Link, EdgeIntervalls> flow, LinkedList<Node> sources, HashMap<Node, Integer> demands, Node sink, int horizon) {
		this.network = network;
		this._flow = flow;
		this._paths = new LinkedList<Path>();
		this._sources = _sources;
		this._demands = demands;
		this._sink = sink;
		_timeHorizon = horizon;
	}

	/**
	 * 
	 * @param path
	 */
	public void augment(Path path){
		
	}
	
	/**
	 * setter for debug mode
	 * @param debug debug mode true is on
	 */
	public static void debug(boolean debug){
		Flow._debug=debug;
	}
	
}
