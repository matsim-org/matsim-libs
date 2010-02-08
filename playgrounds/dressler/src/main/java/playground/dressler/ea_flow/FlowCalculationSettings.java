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
import java.util.HashMap;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;

//playground imports
import playground.dressler.Interval.EdgeIntervals;
import playground.dressler.Interval.SourceIntervals;
import playground.dressler.Interval.VertexIntervals;

public class FlowCalculationSettings {

	private Node _superSink;
	
	private HashMap<Link, Integer> _capacities;
	private HashMap<Link, Integer> _lengths;
	
	private int _timeStep;		
	
	private NetworkLayer _network;
	
	// leave these untouched!
	private HashMap<Node, Integer> _demands;
	private int _totaldemand;
	
	public int TimeHorizon = 200000; // should be safe
	public int MaxRounds = 100000;	// should be safe
	
	FlowCalculationSettings (NetworkLayer network, String sinkId, HashMap<Node, Integer> demands, int timeStep) {
		this._network = network;
		this._demands = demands;
		this._superSink = this._network.getNodes().get(new IdImpl(sinkId));
		
		if (this._superSink == null) {		  
			throw new RuntimeException("Sink " + sinkId + " not found in network!");
		}
		
		this._timeStep = timeStep;		
		
		this._capacities = new HashMap<Link, Integer>();
		this._lengths = new HashMap<Link, Integer>();
		
		
		double capperiod = network.getCapacityPeriod();
		
		int roundedtozerocap = 0;
		int roundedtozerotime = 0;

		for (Link link : network.getLinks().values()){
			
			// from long to int ...
			int newTravelTime = (int) Math.round(link.getLength() / (link.getFreespeed(0.) * timeStep));
			if (newTravelTime == 0 && link.getLength() != 0d) {				
				roundedtozerotime++; 
			}
			
			this._lengths.put(link, newTravelTime);
			
			
			long newcapacity = Math.round(link.getCapacity(1.) * timeStep / capperiod);
			
			// no one uses that much capacity for real ...
			if (newcapacity > Integer.MAX_VALUE) {
				newcapacity = Integer.MAX_VALUE;
			}
			
			if (newcapacity == 0d && link.getCapacity(1.) != 0d) {
				roundedtozerocap++;
			}
			this._capacities.put(link, (int) newcapacity);
		}
		System.out.println("Rounded to zero length: " + roundedtozerotime);
		System.out.println("Rounded to zero capacity: " + roundedtozerocap);
		
		this._totaldemand = 0;
		for (Node node : this._network.getNodes().values()) {
			Integer i = this._demands.get(node);
			if (i != null)
			  this._totaldemand += i; 
		}
		System.out.println("Total demand: " + this._totaldemand);
	}
	
	/**
	 * Returns the capacity of the edge as intended for the calculation! 
	 * @param edge The edge to check.
	 * @return The flow capacity per tiemstep for the calculation. 
	 */
	public int getCapacity(Link edge) {
	  return this._capacities.get(edge);
	}
	
	/**
	 * Returns the length of the edge as intended for the calculation! 
	 * @param edge The edge to check.
	 * @return The length (in timesteps) for the calculation. 
	 */
	public int getLength(Link edge) {
	  return this._lengths.get(edge);
	}
	
	/**
	 * Returns the set timestep.
	 * @return the set timestep. 
	 */
	public int getTimeStep() {
		return this._timeStep;
	}
	
	public NetworkLayer getNetwork() {
		return this._network;
	}   
	
	public Node getSink() {
		return this._superSink;
	}
	
	public boolean isSink(Node node) {
		return (node.getId().equals(this._superSink.getId()));
	}	 
	
	/**
	 * decides whether a node is or was a source
	 * @param node to be checked
	 * @return true if there was originally demand on the node
	 */
	public boolean isSource(Node node) {
		Integer demand = this._demands.get(node);
		return (demand != null && demand > 0);
	}
	
	public  int getDemand(Node node) {
		Integer i = this._demands.get(node);
		if (i != null) return i;
		return 0;
	}
	
	public int getTotalDemand() {
		return this._totaldemand;
	}
	
	public static void enableDebuggingForAllFlowRelatedClasses()
	{
		MultiSourceEAF.debug(true);
		//BellmanFordVertexIntervalls.debug(3);
		BellmanFordIntervalBased.debug(3);
		VertexIntervals.debug(true);
		EdgeIntervals.debug(3);
		SourceIntervals.debug(3);
		Flow.debug(3);
	}
	
	public static void disableDebuggingForAllFlowRelatedClasses()
	{
		MultiSourceEAF.debug(false);
		//BellmanFordVertexIntervalls.debug(0);
		BellmanFordIntervalBased.debug(0);
		VertexIntervals.debug(false);
		EdgeIntervals.debug(0);
		SourceIntervals.debug(0);
		Flow.debug(0);
	}	
}
