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
import playground.dressler.ea_flow.Path.PathEdge;
/**
 * Class representing a dynamic flow on an network with multible sources and a single sink 
 * @author Manuel Schneider
 *
 */

public class Flow {
	
//--------------------------FIELDS----------------------------------------------------//
	
	/**
	 * The network on which we find routes. We expect the network to change
	 * between runs!
	 */
	private final NetworkLayer _network;
	
	/**
	 * Edgerepresentation of flow on the network  
	 */
	private HashMap<Link, EdgeIntervalls> _flow;
	
	/**
	 * Pathrepresentation of flow on the network
	 */
	private LinkedList<Path> _paths;
	

	/**
	 * list of all sources
	 */
	private final LinkedList<Node> _sources;
	
	/**
	 * stores unsatisfied demands for each source
	 */
	private HashMap<Node,Integer> _demands;
	
	/**
	 *stores for all nodes wheather they are an nonactive source 
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
	 * flag for debug mode
	 */
	@SuppressWarnings("unused")
	private static boolean _debug = false;
	
	
//-------------------------------Methods-----------------------------------------//
	
//-----------------------------Constructors--------------------------------------//	
	
	/**
	 * Constructor that initializes a zero flow over time on the specified network
	 * the length of the edges will be length/speed
	 * @param network network on which the flow will "live"
	 * @param sources the potential sources of the flow		
	 * @param demands the demands in the sources as nonnegative integers
	 * @param sink the sink for al the flow
	 * @param horizon the timehorizon in which flow is admittable
	 */
	public Flow(final NetworkLayer network,final LinkedList<Node> sources, HashMap<Node, Integer> demands,final Node sink,final int horizon) {
		this._network = network;
		this._flow = new HashMap<Link,EdgeIntervalls>();
		// initialize distances
		for(Link link : network.getLinks().values()){
			int l = (int)link.getLength()/(int)link.getFreespeed(1.); // TODO
			this._flow.put(link, new EdgeIntervalls(l));
			//TODO achtung cast von double auf int
		}
		this._paths = new LinkedList<Path>();
		this._sources = sources;
		this._demands = demands;
		this._sink = sink;
		_timeHorizon = horizon;
		this._nonactives = this.nonActives();
		
	}
	
	/**
	 * Constructor for flow wich uses an already defined flow 
	 * @param network network on which the flow will "live"
	 * @param flow the preset flow on the network
	 * @param sources the potential sources of the flow
	 * @param demands demands the demands in the sources as nonnegative integers
	 * @param sink the sink for al the flow
	 * @param horizon the timehorizon in which flow is admittable
	 */
	public Flow(final NetworkLayer network, HashMap<Link, EdgeIntervalls> flow,final LinkedList<Node> sources, HashMap<Node, Integer> demands,final Node sink,final int horizon) {
		this._network = network;
		this._flow = flow;
		this._paths = new LinkedList<Path>();
		this._sources = sources;
		this._demands = demands;
		this._sink = sink;
		_timeHorizon = horizon;
		this._nonactives = this.nonActives();
	}

	/**
	 * for all Nodes it is specified if the node is an unactive source
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
//--------------------Flow handeling Methods-------------------------------------//	
	
	/**
	 * Method to determen wheather a Node is a Source with positive demand
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
	 * Mtehod for finding the minimum of the demand at the start node
	 * and the minimal capacity along the Path
	 * @param path
	 * @return
	 */
	private int bottleNeckCapacity(final Path path){
		Node source = path.getSource();
		if(!this._demands.containsKey(source)){
			throw new IllegalArgumentException("Startnode is no source " + path);
		}
		int result = this._demands.get(source);
		for(PathEdge edge : path.getPathEdges()){
			Link link = edge.getEdge();
			int cap =(int) link.getCapacity(1.);
			int time = edge.getTime();
			int flow = this._flow.get(link).getFlowAt(time);
			int i = cap-flow;
			if (i<0){
				throw new IllegalArgumentException("too much flow on " + edge);
			}
			if(i<result ){
				result= i;
			}
		}
		return result;
	}
	
	/**
	 * Method to add another path to the flow. The Path will be added with flow equal to its bottlenec capacity
	 * @param path the path on wich the maximal flow possible is augmented 
	 */
	public void augment(Path path){
		int gamma = bottleNeckCapacity(path);
		for(PathEdge edge : path.getPathEdges()){
			Link link = edge.getEdge();
			int time = edge.getTime();
			EdgeIntervalls flow = _flow.get(link);
			if(edge.isForward()){
				flow.augment(time, gamma, (int)link.getCapacity(1.));
			}else{
				//TODO look at what time to raise!!!!
				flow.augmentreverse(time, gamma);
			}
		}
		path.setFlow(gamma);
		reduceDemand(path);
		this._paths.add(path);
	}
	
	/**
	 * Reduces the demand of the first node in the path by the flow value of the Path
	 * @param path path used to determine flow and Source Node
	 */
	private void reduceDemand(final Path path) {
		Node source = path.getSource();
		if(!this._demands.containsKey(source)){
			throw new IllegalArgumentException("Startnode is no source" + path);
		}
		int flow = path.getFlow();
		int demand = this._demands.get(source)-flow;
		if(demand<0){
			throw new IllegalArgumentException("too much flow on path" + path);
		}
		this._demands.put(source, demand);
		if (demand==0){
			this._nonactives.put(source, true);
		}
	}
	
	/**
	 * decides wheather a Node is an nonactive Source
	 * @param node Node to check for	
	 * @return true iff node is a Source with demand 0
	 */
	public boolean isNonActiveSource(final Node node){
		return this._nonactives.get(node);
	}
	
	
//-----------evaluation methods---------------------------------------------------//
	
	public int[] arrivals(){
		int maxtime = 0;
		int[] temp = new int[this._timeHorizon+1];
		for (Path path : _paths){
			int flow = path.getFlow();
			int time = path.getArrival();
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
	
	public int[] arrivalPattern(){
		int[] result = this.arrivals();
		int sum = 0;
		for (int i=0;i<result.length; i++){
			sum+=result[i];
			result[i]=sum;
		}
		return result;
	}
	
	public String arrivalsToString(){
		//StringBuilder strb1 = new StringBuilder();
		StringBuilder strb2 = new StringBuilder("       arrivals:");
		int[] a =this.arrivals();
		for (int i=1; i<a.length;i++){
			String temp = String.valueOf(a[i]);
			strb2.append(" "+i+":"+temp);
		}
		return strb2.toString();
	}
	
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
	
//-------------------Getters Setters toString---------------------------------------//	

	/**
	 * returns a String representation of a Path
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
	public static void debug(boolean debug){
		Flow._debug=debug;
	}
	
//---------------Commented out stuff---------------------------------------------------//	
	
	
	
}
