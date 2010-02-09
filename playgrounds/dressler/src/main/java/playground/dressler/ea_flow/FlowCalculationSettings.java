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
import java.util.ArrayList;
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

	private HashMap<Link, Integer> _capacities;
	private HashMap<Link, Integer> _lengths;
	
	private int _timeStep;		
	private double _flowFactor;
	
	private NetworkLayer _network;
	
	// leave these untouched!
	private HashMap<Node, Integer> _demands;
	private int _totaldemandsources;
	private int _totaldemandsinks;
	
	private int _roundedtozerocapacity;
	private int _roundedtozerolength;
	
	public int TimeHorizon = 200000; // should be safe
	public int MaxRounds = 100000;	// should be safe
	
    /* Constructor to set the parameters for the EAF calculation 
     * @param network The MATSim network
     * @param demands The demands. Sources are > 0, Sinks are < 0
     * @param timeStep The granularity of the time-expanded network 
     * @param flowFactor Link Capacities will be scaled by this (in addition to accounting for timeStep)
     */
	FlowCalculationSettings (NetworkLayer network, HashMap<Node, Integer> demands, int timeStep, double flowFactor) {
		this._network = network;
		this._demands = demands;
		this._timeStep = timeStep;		
		this._flowFactor = flowFactor;
		
		scaleParameters();
		
		setDemands();
		
		printStatus();
	}
	

    /* Constructor to set the parameters for the EAF calculation 
     * @param network The MATSim network
     * @param sinkId  The ID of the supersink. It will get demand equal to minus the total demand of the sources. 
     * @param demands The demands. Should be >= 0 ! Note that the demand of supersink will be overwritten. 
     * @param timeStep The granularity of the time-expanded network 
     * @param flowFactor Link Capacities will be scaled by this (in addition to accounting for timeStep) 
     */
	FlowCalculationSettings (NetworkLayer network, String sinkId, HashMap<Node, Integer> demands, int timeStep, double flowFactor) {
		this._network = network;
		this._demands = demands;
		this._timeStep = timeStep;
		this._flowFactor = flowFactor;

		Node superSink = network.getNodes().get(new IdImpl(sinkId));  
		if (superSink == null) {		  
			throw new RuntimeException("Sink " + sinkId + " not found in network!");
		}
		
		scaleParameters();
		
		int totaldemand = 0;
		for (Node node : this._network.getNodes().values()) {
			Integer i = this._demands.get(node);
			if (i != null && i > 0)
			  totaldemand += i; 
		}
		demands.put(superSink, -totaldemand);
		
		setDemands();
		
		printStatus();
	}
	
	
	private void scaleParameters() {
		this._capacities = new HashMap<Link, Integer>();
		this._lengths = new HashMap<Link, Integer>();
		
        double capperiod = this._network.getCapacityPeriod();
		
		this._roundedtozerocapacity = 0;
		this._roundedtozerolength = 0;

		for (Link link : this._network.getLinks().values()){
			
			// from long to int ...
			int newTravelTime = (int) Math.round(link.getLength() / (link.getFreespeed(0.) * this._timeStep));
			if (newTravelTime == 0 && link.getLength() != 0d) {				
				this._roundedtozerolength++; 
			}
			
			this._lengths.put(link, newTravelTime);
			
			
			long newcapacity = Math.round(link.getCapacity(1.) * this._timeStep * this._flowFactor / capperiod);
			
			// no one uses that much capacity for real ...
			if (newcapacity > Integer.MAX_VALUE) {
				newcapacity = Integer.MAX_VALUE;
			}
			
			if (newcapacity == 0d && link.getCapacity(1.) != 0d) {
				this._roundedtozerocapacity++;
			}
			this._capacities.put(link, (int) newcapacity);
		}		
	}
	
	private void setDemands() {
		this._totaldemandsources = 0;
		this._totaldemandsinks = 0;
		for (Node node : this._network.getNodes().values()) {
			Integer i = this._demands.get(node);
			if (i != null) {
				if (i > 0) {
					this._totaldemandsources += i;
				} else {
					this._totaldemandsinks += -i;
				}
				
			}
		}
	
	}
	
	public void printStatus() {
		System.out.println("Network has " + this._network.getNodes().size() + " nodes and " + this._network.getLinks().size() + " edges.");
		System.out.println("Total demand sources: " + this._totaldemandsources + " | sinks: " + this._totaldemandsinks);
		System.out.println("Timestep: " + this._timeStep);
		System.out.println("FlowFactor: " + this._flowFactor);
		System.out.println("Edges rounded to zero length: " + this._roundedtozerolength);
		System.out.println("Edges rounded to zero capacity: " + this._roundedtozerocapacity);
		
		
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
	
	/*public Node getSink() {
		return this._superSink;
	}*/
	
	/**
	 * decides whether a node is or was a sink
	 * @param node to be checked
	 * @return true if there was originally negative demand on the node
	 */
	public boolean isSink(Node node) {
		Integer demand = this._demands.get(node);
		return (demand != null && demand < 0);		
	}	 
	
	/**
	 * decides whether a node is or was a source
	 * @param node to be checked
	 * @return true if there was originally positive demand on the node
	 */
	public boolean isSource(Node node) {
		Integer demand = this._demands.get(node);
		return (demand != null && demand > 0);
	}
	
	public int getDemand(Node node) {
		Integer i = this._demands.get(node);
		if (i != null) return i;
		return 0;
	}
	
	public int getTotalDemand() {
		return this._totaldemandsources;
	}
	
	public static void enableDebuggingForAllFlowRelatedClasses()
	{
		MultiSourceEAF.debug(true);
		//BellmanFordVertexIntervalls.debug(3);
		BellmanFordIntervalBased.debug(3);
		VertexIntervals.debug(3);
		EdgeIntervals.debug(3);
		SourceIntervals.debug(3);
		Flow.debug(3);
	}
	
	public static void disableDebuggingForAllFlowRelatedClasses()
	{
		MultiSourceEAF.debug(false);
		//BellmanFordVertexIntervalls.debug(0);
		BellmanFordIntervalBased.debug(0);
		VertexIntervals.debug(0);
		EdgeIntervals.debug(0);
		SourceIntervals.debug(0);
		Flow.debug(0);
	}	
}
