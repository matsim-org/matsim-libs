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
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;

//playground imports
import playground.dressler.Interval.EdgeIntervals;
import playground.dressler.Interval.Interval;
import playground.dressler.Interval.SourceIntervals;
import playground.dressler.Interval.VertexIntervals;

public class FlowCalculationSettings {

	/* some constants */
	public static final int SEARCHALGO_FORWARD = 1;
	public static final int SEARCHALGO_REVERSE = 2;
	public static final int SEARCHALGO_MIXED = 3;
	
	/* default settings */
	public int searchAlgo = SEARCHALGO_REVERSE;
	public boolean useVertexCleanup = false;
	public boolean useImplicitVertexCleanup = true; // unite vertex intervals before propagating?
	public int TimeHorizon = 654321; // should be safe
	public int MaxRounds = 654321;	// should be safe
	public int checkConsistency = 0; // after how many iterations should consistency be checked? 0 = off
	public boolean checkTouchedNodes = true; // should Flow.UnfoldAndAugment() try to shortcut the search for suitable forward steps?
	
	public boolean sortPathsBeforeAugmenting = true; // try to augment shorter (#steps) first? 
	public boolean keepPaths = true; // should TEPs be stored at all?
	public boolean unfoldPaths = true; // if they are stored, should they be unfolded to contain only forward edges?
	public boolean useRepeatedPaths = true; // try to repeat paths
	
	public boolean trackUnreachableVertices = true && (searchAlgo == FlowCalculationSettings.SEARCHALGO_REVERSE);; // only works in REVERSE, wastes time otherwise
	
	
	/* when are links available? not included means "always" */
	public HashMap<Link, Interval> whenAvailable = null;
	
	
	/* interal storage for the network parameters */ 
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
		
		//printStatus();
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
		
		//printStatus();
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
		System.out.println("==== Flow Calculation Settings ====");		
		System.out.println("Network has " + this._network.getNodes().size() + " nodes and " + this._network.getLinks().size() + " edges.");
		System.out.println("Total demand sources: " + this._totaldemandsources + " | sinks: " + this._totaldemandsinks);
		System.out.println("Time Horizon: " + this.TimeHorizon);
		System.out.println("Timestep: " + this._timeStep);		
		System.out.println("FlowFactor: " + this._flowFactor);
		System.out.println("Edges rounded to zero length: " + this._roundedtozerolength);
		System.out.println("Edges rounded to zero capacity: " + this._roundedtozerocapacity);
		
		switch (this.searchAlgo) {
		  case FlowCalculationSettings.SEARCHALGO_FORWARD: 
			  System.out.println("Algorithm to use: Forward Search"); 
			  break;
		  case FlowCalculationSettings.SEARCHALGO_REVERSE:
			  System.out.println("Algorithm to use: Reverse Search");
			  break;
		  case FlowCalculationSettings.SEARCHALGO_MIXED:
			  System.out.println("Algorithm to use: Mixed Search");
			  break;
		  default:
			  System.out.println("Algorithm to use: Unkown (" + this.searchAlgo +")");
		}
		  
		System.out.println("Track unreachable vertices: " + this.trackUnreachableVertices);
		System.out.println("Use vertex cleanup: " + this.useVertexCleanup);
		System.out.println("Use implicit vertex cleanup: " + this.useImplicitVertexCleanup);
		System.out.println("Use repeated paths: " + this.useRepeatedPaths);
		System.out.println("Sort paths before augmenting: " + this.sortPathsBeforeAugmenting);
		System.out.println("Check consistency every: " + this.checkConsistency + " rounds (0 = off)");
		System.out.println("Use touched nodes hashmaps: " + this.checkTouchedNodes);
		System.out.println("Keep paths at all: " + this.keepPaths);
		System.out.println("Unfold stored paths: " + this.unfoldPaths);
		
		System.out.println("===================================");
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
	
	public void writeSimpleNetwork() {
		// write simple data format to sysout
		// representing the dynamic graph, not the time-expanded graph
		System.out.println("% generated from matsim data");            
        System.out.println("N " + this._network.getNodes().size());
        System.out.println("TIME " + this.TimeHorizon);
        HashMap<Node,Integer> newNodeNames = new HashMap<Node,Integer>();
        int max = 0;
        for (NodeImpl node : this._network.getNodes().values()) {
        	try {
        		int i = Integer.parseInt(node.getId().toString());
        		if (i > 0) 	newNodeNames.put(node,i);
        		if (i > max) max = i;
        	} catch (Exception except) {

        	}            	
        }
            
        for (NodeImpl node : this._network.getNodes().values()) {            	
        	try {
        		int i = Integer.parseInt(node.getId().toString());
        		if (i <= 0) {
        		  max += 1;
            	  newNodeNames.put(node, max);
            	  System.out.println("% node " + max + " was '" + node.getId()+ "'");
        		}
        	} catch (Exception except) {
        		max += 1;
        		newNodeNames.put(node, max);
        		System.out.println("% node " + max + " was '" + node.getId()+ "'");

        	}            	
        }
            
        for (NodeImpl node : this._network.getNodes().values()) {
        	if (this._demands.containsKey(node)) {
        		int d = this._demands.get(node);
        		if (d > 0) {
        			System.out.println("S " + newNodeNames.get(node) + " " + d);            			
        		}
        		if (d < 0) {
        			System.out.println("T " + newNodeNames.get(node) + " " + (-d));            			
        		}
        	}
        }

        System.out.println("% E from to capacity length");
        for (LinkImpl link : this._network.getLinks().values()) {                
        	System.out.println("E " + (newNodeNames.get(link.getFromNode())) + " " + (newNodeNames.get(link.getToNode())) + " " + (int) getCapacity(link) + " " + getLength(link));

        }            
	}
	
	private String NameEdge(Link link, int t, HashMap<Node,Integer> nodeNames) {
		if (t < 0) return null;
		if (t + getLength(link) >= this.TimeHorizon) return null;
		return "a#" + nodeNames.get(link.getFromNode()) + "#" +nodeNames.get(link.getToNode()) + "#t" + t; 
	}
	
	private String NameNode(Node node, int t, HashMap<Node,Integer> nodeNames) {
		if (t < 0) return null;
		if (t >= this.TimeHorizon) return null;
		return "n#" + nodeNames.get(node) + "#t" + t; 
	}
	
	private String NameSource(Node node, HashMap<Node,Integer> nodeNames) {
		return "source#" + nodeNames.get(node); 
	}
	
	private String NameSink(Node node, HashMap<Node,Integer> nodeNames) {
		return "sink#" + nodeNames.get(node); 
	}
	
	private String NameSourceLink(Node node, int t, HashMap<Node,Integer> nodeNames) {
		if (t < 0) return null;
		if (t >= this.TimeHorizon) return null;
		return "sourcelink#" + nodeNames.get(node) + "#t" + t; 
	}
	
	private String NameSinklink(Node node, int t, HashMap<Node,Integer> nodeNames) {
		if (t < 0) return null;
		if (t >= this.TimeHorizon) return null;
		return "sinklink#" + nodeNames.get(node) + "#t" + t;
	}
	
	private String NameSupersink() {
		return "supersink";
	}
	
	private String NameSupersource() {
		return "supersource";
	}
	
	private String NameSuperSinkLink(Node node, HashMap<Node,Integer> nodeNames) {
		return "supersinklink#" + nodeNames.get(node);
	}
	
	private String NameSuperSourceLink(Node node, HashMap<Node,Integer> nodeNames) {
		return "supersourcelink#" + nodeNames.get(node);
	}
	
	/**
	 * Writes the EAT problem as .lp file for CPLEX etc to standard out
	 * This might be a big file and it includes some useless comments at the top ...
	 */
	public void writeLP() {
		System.out.println("\\ generated from matsim data");            
        System.out.println("\\ N " + this._network.getNodes().size());
        System.out.println("\\ TIME " + this.TimeHorizon);
        HashMap<Node,Integer> newNodeNames = new HashMap<Node,Integer>();
        int max = 0;
        for (NodeImpl node : this._network.getNodes().values()) {
        	try {
        		int i = Integer.parseInt(node.getId().toString());
        		if (i > 0) 	newNodeNames.put(node,i);
        		if (i > max) max = i;
        	} catch (Exception except) {

        	}            	
        }
        
        for (NodeImpl node : this._network.getNodes().values()) {            	
        	try {
        		int i = Integer.parseInt(node.getId().toString());
        		if (i <= 0) {
        		  max += 1;
            	  newNodeNames.put(node, max);
            	  System.out.println("\\ node " + max + " was '" + node.getId()+ "'");
        		}
        	} catch (Exception except) {
        		max += 1;
        		newNodeNames.put(node, max);
        		System.out.println("\\ node " + max + " was '" + node.getId()+ "'");

        	}            	
        }

        System.out.println("Minimize");
        System.out.println("totaltraveltime: ");

        StringBuilder Sobj = new StringBuilder(" ");        	
        for (NodeImpl node : this._network.getNodes().values()) {
        	int d = 0;
        	if (this._demands.containsKey(node)) {
        		d = this._demands.get(node);
        	} 

        	if (d < 0) {
        		for (Link link : node.getOutLinks().values()) {
        			for (int t = 0; t < this.TimeHorizon - this.getLength(link); t++) {
        				String tmp = NameEdge(link, t, newNodeNames);
        				if (tmp != null) {
        					// append " -" + t + " " + tmp
        					Sobj.append(" -");
        				    Sobj.append(t);
        				    Sobj.append(" ");
        				    Sobj.append(tmp);
        				}
        			}
        		}

        		for (Link link : node.getInLinks().values()) {
        			for (int t = 0; t < this.TimeHorizon - this.getLength(link); t++) {
        				String tmp = NameEdge(link, t, newNodeNames);
        				if (tmp != null) {
        					//Sobj += " +" + (t + this.getLength(link)) + " " + tmp;
        					Sobj.append(" + ");
        					Sobj.append((t + this.getLength(link)));
        					Sobj.append(" ");
        					Sobj.append(tmp);
        				}
        				
        			}
        		}


        	}
        }
        System.out.println(Sobj);

        
        
        System.out.println("Subject to");
        
        
        for (NodeImpl node : this._network.getNodes().values()) {
        	int d = 0;
        	if (this._demands.containsKey(node)) {
        		d = this._demands.get(node);
        	} 
        	// write flow conservation for each time step        	
        	for (int t = 0; t < this.TimeHorizon; t++) {        		
        		System.out.println("flow_conservation#" + newNodeNames.get(node) + "@t" + t + ":");
        		StringBuilder S = new StringBuilder(" ");
        		for (Link link : node.getOutLinks().values()) {
        			String tmp = NameEdge(link, t, newNodeNames);
        			if (tmp != null) { 
        				//S += " + " + tmp;
        				S.append(" + ");
        				S.append(tmp);
        			}
        		}
        		for (Link link : node.getInLinks().values()) {
        			String tmp = NameEdge(link, t - this.getLength(link), newNodeNames);
        			if (tmp != null) { 
        				// S += " - " + tmp;
        				S.append(" - ");
        				S.append(tmp);
        			}
        		}


        		if (d > 0) {
        			//S += " >= 0";
        			S.append(" >= 0");
        		} else if (d < 0) {
        			//S += " <= 0";
        			S.append(" <= 0");
        		} else {
        			//S += " = 0";
        			S.append(" = 0");
        		}
        		System.out.println(S);
        	}
        	
        	
        	// TODO this could be weaved into the step above ... for a little speedup (<= factor 2)
        	if (d != 0) {
        		if (d > 0) {
        		  System.out.println("supply@" + newNodeNames.get(node) + ":");
        		}  else {
        		  System.out.println("demand@" + newNodeNames.get(node) + ":");
        		}
        		StringBuilder S = new StringBuilder(" ");        	
        		// write supply / demand constraint    
        		
        		for (Link link : node.getOutLinks().values()) {
        			for (int t = 0; t < this.TimeHorizon - this.getLength(link); t++) {
        				String tmp = NameEdge(link, t, newNodeNames);
        				if (tmp != null) {
        					//S += " + " + tmp;
        					S.append(" + ");
            				S.append(tmp);
        				}
        			}
        		}

        		for (Link link : node.getInLinks().values()) {
        			for (int t = 0; t < this.TimeHorizon - this.getLength(link); t++) {
        				String tmp = NameEdge(link, t, newNodeNames);
        				if (tmp != null) {
        					//S += " - " + tmp;
        					S.append(" - ");
            				S.append(tmp);
        				}
        			}
        		}


        		if (d > 0) {
        			//S += " = " + d;;
        			S.append(" = ");
        			S.append(d);
        		} else  { // note: d < 0
        			//S += " >= " + d;
        			S.append(" >= ");
        			S.append(d);
        		}			
        		System.out.println(S);
        	}
        }

        //System.out.println("% E from to capacity length");
        System.out.println("Bounds");
        for (LinkImpl link : this._network.getLinks().values()) {
        	for (int t = 0; t < this.TimeHorizon; t++) {
        		String tmp = NameEdge(link, t, newNodeNames);
        		if (tmp != null)
        		  System.out.println(" 0 <= " + tmp + " <= " + this.getCapacity(link));
        	}        	
        }            
        System.out.println("End");
	}
	
	
	/**
	 * Writes the EAT problem as .net Network file for CPLEX to standard out
	 * This might be a big file and it includes some useless comments at the top ...
	 * @param costOnSinks if true, only the sink links will have non-zero costs, which is an equivalent formulation
	 */
	public void writeNET(boolean costOnSinks) {
		System.out.println("\\ generated from matsim data");            
        System.out.println("\\ N " + this._network.getNodes().size());
        System.out.println("\\ TIME " + this.TimeHorizon);
        HashMap<Node,Integer> newNodeNames = new HashMap<Node,Integer>();
        int max = 0;
        for (NodeImpl node : this._network.getNodes().values()) {
        	try {
        		int i = Integer.parseInt(node.getId().toString());
        		if (i > 0) 	newNodeNames.put(node,i);
        		if (i > max) max = i;
        	} catch (Exception except) {

        	}            	
        }
        
        for (NodeImpl node : this._network.getNodes().values()) {            	
        	try {
        		int i = Integer.parseInt(node.getId().toString());
        		if (i <= 0) {
        		  max += 1;
            	  newNodeNames.put(node, max);
            	  System.out.println("\\ node " + max + " was '" + node.getId()+ "'");
        		}
        	} catch (Exception except) {
        		max += 1;
        		newNodeNames.put(node, max);
        		System.out.println("\\ node " + max + " was '" + node.getId()+ "'");

        	}            	
        }

        System.out.println("MINIMIZE NETWORK frommatsim");
        
        System.out.println("SUPPLY");          
        System.out.println(NameSupersource() + " : " + this._totaldemandsources);
        // we need an transshipment! and sink demands are just upper bounds anyway
        System.out.println(NameSupersink() + " : " + (- this._totaldemandsources));
  
        System.out.println("ARCS");
        
        // the time-expanded arcs
        for (Link link : this._network.getLinks().values()) {        	
        	for (int t = 0; t < this.TimeHorizon; t++) {        		        		
        		StringBuilder S = new StringBuilder();
        		String tmp = NameEdge(link, t, newNodeNames);
        		if (tmp != null) {
        			S.append(tmp + " : " + NameNode(link.getFromNode(), t, newNodeNames));
        			S.append(" -> " + NameNode(link.getToNode(), t + this.getLength(link), newNodeNames));
        		}
        		System.out.println(S);
        	}
        }
         
        // the arcs from the virtual sources to the source node
        // and from the sink nodes to the virtual sinks
        for (NodeImpl node : this._network.getNodes().values()) {
        	int d = 0;
        	if (this._demands.containsKey(node)) {
        		d = this._demands.get(node);
        	} 
        	if (d == 0)
        		continue;

        	if (d < 0) {
        		for (int t = 0; t < this.TimeHorizon; t++) {   
        			StringBuilder sb = new StringBuilder();        		
        		    sb.append(NameSinklink(node, t, newNodeNames));
        		    sb.append(" : " + NameNode(node, t, newNodeNames));
        		    sb.append(" -> " + NameSink(node, newNodeNames));
        		    System.out.println(sb);
        		}        		
        	} else {
        		for (int t = 0; t < this.TimeHorizon; t++) {   
        			StringBuilder sb = new StringBuilder();        		
        		    sb.append(NameSourceLink(node, t, newNodeNames));
        		    sb.append(" : " + NameSource(node, newNodeNames));
        		    sb.append(" -> " + NameNode(node, t, newNodeNames));
        		    System.out.println(sb);
        		}
        	}
        }

        
        // the arcs from the supersource/supersink to the virtual sources/sinks
        for (NodeImpl node : this._network.getNodes().values()) {
        	int d = 0;
        	if (this._demands.containsKey(node)) {
        		d = this._demands.get(node);
        	} 
        	if (d == 0)
        		continue;

        	StringBuilder sb = new StringBuilder();
        	if (d < 0) {
        		sb.append(NameSuperSinkLink(node, newNodeNames));        		
        		sb.append(" : " + NameSink(node, newNodeNames));
        		sb.append(" -> " + NameSupersink());
        	} else {
        		sb.append(NameSuperSourceLink(node, newNodeNames));        		
        		sb.append(" : " + NameSupersource());
        		sb.append(" -> " + NameSource(node, newNodeNames));        		
        	}
            System.out.println(sb);
        }
        
        
        System.out.println("OBJECTIVE");
        
        // there are two options here        
        // put all weights on the sinklinks
        // or give every link a weight equal to its length (except sinklinks)
        
        if (costOnSinks) {
        	// the sinklinks have their departure time as cost         
        	for (NodeImpl node : this._network.getNodes().values()) {
        		int d = 0;
        		if (this._demands.containsKey(node)) {
        			d = this._demands.get(node);
        		}         	
        		if (d < 0) {
        			for (int t = 0; t < this.TimeHorizon; t++) {   
        				StringBuilder sb = new StringBuilder();        		
        				sb.append(NameSinklink(node, t, newNodeNames));
        				sb.append(" : " + t);
        				System.out.println(sb);
        			}        		
        		} 
        	}
        } else {
        	 // the time-expanded arcs have their length as cost
            for (Link link : this._network.getLinks().values()) {        	
            	for (int t = 0; t < this.TimeHorizon; t++) {        		        		
            		StringBuilder S = new StringBuilder();
            		String tmp = NameEdge(link, t, newNodeNames);
            		if (tmp != null) {
            			S.append(tmp + " : " + this.getLength(link));            			
            		}
            		System.out.println(S);
            	}
            }
             
            // the arcs from the virtual sources to the source node have their arrival time as cost
            for (NodeImpl node : this._network.getNodes().values()) {
            	int d = 0;
            	if (this._demands.containsKey(node)) {
            		d = this._demands.get(node);
            	} 
            	if (d > 0) {
            		for (int t = 0; t < this.TimeHorizon; t++) {   
            			StringBuilder sb = new StringBuilder();        		
            		    sb.append(NameSourceLink(node, t, newNodeNames));
            		    sb.append(" : " + t);
            		    System.out.println(sb);
            		}
            	}
            }
        }

        System.out.println("BOUNDS");

        for (LinkImpl link : this._network.getLinks().values()) {
        	for (int t = 0; t < this.TimeHorizon; t++) {
        		String tmp = NameEdge(link, t, newNodeNames);
        		if (tmp != null)
        		  System.out.println(" 0 <= " + tmp + " <= " + this.getCapacity(link));
        	}        	
        }
        
        // source/sink links are left free ... dunno if this matters
        
        // the arcs from the supersource/supersink to the virtual sources/sinks have 
        // capacity equal to the demands there
        for (NodeImpl node : this._network.getNodes().values()) {
        	int d = 0;
        	if (this._demands.containsKey(node)) {
        		d = this._demands.get(node);
        	} 
        	if (d == 0)
        		continue;

        	StringBuilder sb = new StringBuilder();
        	sb.append("0 <= ");
        	if (d < 0) {
        		sb.append(NameSuperSinkLink(node, newNodeNames));        		
        		sb.append(" <= " + (-d));        		
        	} else {
        		sb.append(NameSuperSourceLink(node, newNodeNames));        		
        		sb.append(" <= " + d);        		
        	}
            System.out.println(sb);
        }
        
        
        System.out.println("ENDNETWORK");
	}
}
