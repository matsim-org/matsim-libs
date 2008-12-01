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
import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;

import playground.dressler.Intervall.src.Intervalls.EdgeIntervalls;
import playground.dressler.ea_flow.TimeExpandedPath.PathEdge;
/**
 * Class representing a dynamic flow on an network with multiple sources and a single sink 
 * @author Manuel Schneider
 *
 */

public class Flow {
////////////////////////////////////////////////////////////////////////////////////////	
//--------------------------FIELDS----------------------------------------------------//
////////////////////////////////////////////////////////////////////////////////////////	
	/**
	 * The network on which we find routes. We expect the network to change
	 * between runs!
	 */
	private final NetworkLayer _network;
	
	/**
	 * used to calculate the length of every edge in the network
	 */
	private final FakeTravelTimeCost _lengths; 
	
	/**
	 * Edge representation of flow on the network  
	 */
	private HashMap<Link, EdgeIntervalls> _flow;
	
	/**
	 * TimeExpandedTimeExpandedPath representation of flow on the network
	 */
	private LinkedList<TimeExpandedPath> _TimeExpandedPaths;
	
	/**
	 * list of all sources
	 */
	private final LinkedList<Node> _sources;
	
	/**
	 * stores unsatisfied demands for each source
	 */
	private HashMap<Node,Integer> _demands;
	
	/**
	 *stores for all nodes whether they are an non active source 
	 */
	private HashMap<Node,Boolean> _nonactives;

	/**
	 * the sink, to which all flow is directed
	 */
	private final  Node _sink;
	
	/**
	 * maximal time Horizon for the flow
	 */
	private final  int _timeHorizon;
	
	/**
	 * TODO use debug mode
	 * flag for debug mode
	 */
	@SuppressWarnings("unused")
	private static int _debug = 0;
	
///////////////////////////////////////////////////////////////////////////////////	
//-----------------------------Constructors--------------------------------------//	
///////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Constructor that initializes a zero flow over time on the specified network
	 * the length of the edges will be as specified by FakeTravelTimeCost 
	 * @param network network on which the flow will "live"
	 * @param sources the potential sources of the flow		
	 * @param demands the demands in the sources as nonnegative integers
	 * @param sink the sink for all the flow
	 * @param horizon the time horizon in which flow is allowed
	 */
	public Flow(final NetworkLayer network,final FakeTravelTimeCost lengths, HashMap<Node, Integer> demands, final Node sink, final int horizon) {
		this._network = network;
		this._lengths = lengths;
		this._flow = new HashMap<Link,EdgeIntervalls>();
		// initialize distances
		for(Link link : network.getLinks().values()){
			int l = (int) _lengths.getLinkTravelCost(link, 1.);
			this._flow.put(link, new EdgeIntervalls(l));
		}
		this._TimeExpandedPaths = new LinkedList<TimeExpandedPath>();
		this._demands = demands;
		this._sources = new LinkedList<Node>();
		this._sources.addAll(demands.keySet());
		this._sink = sink;
		_timeHorizon = horizon;
		this._nonactives = this.nonActives();
		
	}
	
	/**
	 * Constructor for flow which uses an already defined flow 
	 * @param network network on which the flow will "live"
	 * @param flow the preset flow on the network
	 * @param sources the potential sources of the flow
	 * @param demands demands the demands in the sources as nonnegative integers
	 * @param sink the sink for all the flow
	 * @param horizon the time horizon in which flow is allowed
	 */
	public Flow(final NetworkLayer network,final FakeTravelTimeCost lengths, HashMap<Link, EdgeIntervalls> flow,
			HashMap<Node, Integer> demands,final Node sink,final int horizon) {
		this._network = network;
		this._lengths = lengths;
		this._flow = flow;
		this._TimeExpandedPaths = new LinkedList<TimeExpandedPath>();
		this._demands = demands;
		this._sources = new LinkedList<Node>();
		this._sources.addAll(demands.keySet());
		this._sink = sink;
		_timeHorizon = horizon;
		this._nonactives = this.nonActives();
	}

	/**
	 * for all Nodes it is specified if the node is an non active source
	 */
	private HashMap<Node,Boolean> nonActives(){
		HashMap<Node,Boolean> nonactives = new HashMap<Node,Boolean>();
		for(Node node : this._network.getNodes().values()){
			if(!this._sources.contains(node)){
				nonactives.put(node, false);
			}else{
				if(this._demands.get(node)!=0){
					nonactives.put(node, false);
				}else{
					nonactives.put(node, true);
				}
			}
		}
		return nonactives;
	}
	
//////////////////////////////////////////////////////////////////////////////////
//--------------------Flow handling Methods-------------------------------------//	
//////////////////////////////////////////////////////////////////////////////////	
	/**
	 * Method to determine whether a Node is a Source with positive demand
	 * @param node Node that is checked
	 * @return true iff Node is a Source and has positive demand
	 */
	public boolean isActiveSource(final Node node) {
		Integer i = _demands.get(node);
		if (i== null){
			return false;
		}else{
			if (i>0){
				return true;
			}else{
				return false;
			}
		}
	}
	
	/**
	 * Method for finding the minimum of the demand at the start node
	 * and the minimal capacity along the TimeExpandedPath
	 * @param TimeExpandedPath
	 * @return minimum over all unused capacities and the demand in the first node
	 */
	private int bottleNeckCapacity(final TimeExpandedPath TimeExpandedPath){
		//check if first node is a source
		Node source = TimeExpandedPath.getSource();
		if(!this._demands.containsKey(source)){
			throw new IllegalArgumentException("Startnode is no source " + TimeExpandedPath);
		}
		int result = this._demands.get(source);
		//go through the pat edges
		for(PathEdge edge : TimeExpandedPath.getPathEdges()){
			Link link = edge.getEdge();
			int cap =(int) link.getCapacity(1.);
			int time = edge.getTime();
			//check forward capacity
			if(edge.isForward()){
				int flow = this._flow.get(link).getFlowAt(time);
				int i = cap-flow;
				if (i<0){
					throw new IllegalArgumentException("too much flow on " + edge);
				}
				if(i<result ){
					result= i;
				}
			}
			// backwards capacity
			else{
				int flow = this._flow.get(link).getFlowAt(time-(this._flow.get(link).getTravelTime()));
				if(flow<result){
					result= flow;
				}
			}
		}
		return result;
	}
	
	/**
	 * Method to add another TimeExpandedPath to the flow. The TimeExpandedPath will be added with flow equal to its bottleneck capacity
	 * @param TimeExpandedPath the TimeExpandedPath on which the maximal flow possible is augmented 
	 */
	public void augment(TimeExpandedPath TimeExpandedPath){
		int gamma = bottleNeckCapacity(TimeExpandedPath);
		for(PathEdge edge : TimeExpandedPath.getPathEdges()){
			Link link = edge.getEdge();
			int time = edge.getTime();
			EdgeIntervalls flow = _flow.get(link);
			if(edge.isForward()){
				flow.augment(time, gamma, (int)link.getCapacity(1.));
			}else{
				flow.augmentreverse((time-this._flow.get(link).getTravelTime()), gamma);
			}
		}
		TimeExpandedPath.setFlow(gamma);
		reduceDemand(TimeExpandedPath);
		this._TimeExpandedPaths.add(TimeExpandedPath);
	}
	
	/**
	 * Reduces the demand of the first node in the TimeExpandedPath by the flow value of the TimeExpandedPath
	 * @param TimeExpandedPath TimeExpandedPath used to determine flow and Source Node
	 */
	private void reduceDemand(final TimeExpandedPath TimeExpandedPath) {
		Node source = TimeExpandedPath.getSource();
		if(!this._demands.containsKey(source)){
			throw new IllegalArgumentException("Startnode is no source" + TimeExpandedPath);
		}
		int flow = TimeExpandedPath.getFlow();
		int demand = this._demands.get(source)-flow;
		if(demand<0){
			throw new IllegalArgumentException("too much flow on TimeExpandedPath" + TimeExpandedPath);
		}
		this._demands.put(source, demand);
		if (demand==0){
			this._nonactives.put(source, true);
		}
	}
	
	/**
	 * decides whether a Node is an non active Source
	 * @param node Node to check for	
	 * @return true iff node is a Source with demand 0
	 */
	public boolean isNonActiveSource(final Node node){
		return this._nonactives.get(node);
	}
	
////////////////////////////////////////////////////////////////////////////////////
//-----------evaluation methods---------------------------------------------------//
////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * gives back an array containing the amount of flow into the sink for all time steps from 0 to time horizon
	 */
	public int[] arrivals(){
		int maxtime = 0;
		int[] temp = new int[this._timeHorizon+1];
		for (TimeExpandedPath TimeExpandedPath : _TimeExpandedPaths){
			int flow = TimeExpandedPath.getFlow();
			int time = TimeExpandedPath.getArrival();
			if (maxtime < time){
				maxtime = time; 
			}
			temp[time]+=flow;
		}
		
		int[] result = new int[maxtime+1];
		for(int i=0; i<=maxtime;i++){
			result[i]=temp[i];
		}
		return result;
		
	}
	
	/**
	 * gives back an array containing the total amount of flow into the sink by a given time 
	 * for all time steps from 0 to time horizon
	 */
	public int[] arrivalPattern(){
		int[] result = this.arrivals();
		int sum = 0;
		for (int i=0;i<result.length; i++){
			sum+=result[i];
			result[i]=sum;
		}
		return result;
	}
	/**
	 * String representation of the arrivals specifying the amount of flow into the sink
	 * for all time steps from 0 to time horizon
	 * @return String representation of the arrivals
	 */
	public String arrivalsToString(){
		//StringBuilder strb1 = new StringBuilder();
		StringBuilder strb2 = new StringBuilder("  arrivals:");
		int[] a =this.arrivals();
		for (int i=1; i<a.length;i++){
			String temp = String.valueOf(a[i]);
			strb2.append(" "+i+":"+temp);
		}
		return strb2.toString();
	}
	
	/**
	 * a STring specifying the total amount of flow into the sink by a given time 
	 * for all time steps from 0 to time horizon
	 * @return String representation of the arrival pattern
	 */
	public String arrivalPatternToString(){
		//StringBuilder strb1 = new StringBuilder();
		StringBuilder strb2 = new StringBuilder("arrival pattern:");
		int[] a =this.arrivalPattern();
		for (int i=1; i<a.length;i++){
			String temp = String.valueOf(a[i]);
			strb2.append(" "+i+":"+temp);
		}
		return strb2.toString();
	}

	
//////////////////////////////////////////////////////////////////////////////////////
//-------------------Getters Setters toString---------------------------------------//	
//////////////////////////////////////////////////////////////////////////////////////
	
	
	/**
	 * returns a String representation of a TimeExpandedPath
	 */
	public String toString(){
		StringBuilder strb = new StringBuilder();
		for(Link link : _flow.keySet()){
			EdgeIntervalls edge =_flow.get(link);
			strb.append(link.getId().toString()+ ": " + edge.toString()+ "\n");
		}
		return strb.toString();
	}

	/**
	 * @return the _demands
	 */
	public HashMap<Node, Integer> getDemands() {
		return this._demands;
	}

	/**
	 * @param demands the _demands to set
	 */
	public void setDemands(HashMap<Node, Integer> demands) {
		this._demands = demands;
	}

	/**
	 * @return the _flow
	 */
	public HashMap<Link, EdgeIntervalls> getFlow() {
		return this._flow;
	}

	/**
	 * @param flow the _flow to set
	 */
	public void setFlow(HashMap<Link, EdgeIntervalls> flow) {
		this._flow = flow;
	}

	/**
	 * @return the _sink
	 */
	public Node getSink() {
		return _sink;
	}

	/**
	 * @return the _sources
	 */
	public LinkedList<Node> getSources() {
		return this._sources;
	}

	/**
	 * @return the _timeHorizon
	 */
	public int getTimeHorizon() {
		return this._timeHorizon;
	}

	/**
	 * @return the network
	 */
	public NetworkLayer getNetwork() {
		return this._network;
	}
	
	/**
	 * setter for debug mode
	 * @param debug debug mode true is on
	 */
	public static void debug(int debug){
		Flow._debug=debug;
	}

}
