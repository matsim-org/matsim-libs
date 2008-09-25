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
	 * The cost calculator. Provides the cost for each link and time step.
	 */
	private TravelCost costFunction;

	/**
	 * The travel time calculator. Provides the travel time for each link and
	 * time step. This is ignored.
	 */
	 private TravelTime timeFunction;
	
	/**
	 * Datastructure to to represent the flow on a network  
	 */
	private HashMap<Link, EdgeIntervalls> _flow;
	
	/**
	 * Datastructure to keep distance labels on nodes during and after one Iteration of the shortest Path Algorithm
	 */
	private HashMap<Node, VertexIntervalls> _labels;

	/**
	 * 
	 */
	private LinkedList<Node> _sources;
	
	/**
	 * 
	 */
	private HashMap<Node,Integer> _demands;
	
	/**
	 * 
	 */
	private ArrayList _paths;
	
	/**
	 * 
	 */
	private int _timeHorizon;
	
	/**
	 * 
	 */
	private int _gamma;
	
	/**
	 * 
	 */
	private Node _sink;

	
}
