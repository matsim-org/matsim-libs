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
 * Class representing a dynamic flow on an network
 * @author Manuel Schneider
 *
 */

public class Flow {
	
	
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
		this._network = network;
		this._flow = flow;
		this._paths = new LinkedList<Path>();
		this._sources = sources;
		this._demands = demands;
		this._sink = sink;
		_timeHorizon = horizon;
	}
	
	/**
	 * 
	 * @param network
	 * @param sources
	 * @param demands
	 * @param sink
	 * @param horizon
	 */
	public Flow(NetworkLayer network, LinkedList<Node> sources, HashMap<Node, Integer> demands, Node sink, int horizon) {
		this._network = network;
		this._flow = new HashMap<Link,EdgeIntervalls>();
		this._paths = new LinkedList<Path>();
		this._sources = sources;
		this._demands = demands;
		this._sink = sink;
		_timeHorizon = horizon;
	}

	/**
	 * 
	 * @param node
	 * @return
	 */
	public boolean isActiveSource(Node node) {
		if(this._sources.contains(node)){
			return true;
		}
		return false;
		
		//TODO nonactive sources or move to flow
	}
	
	private int bottleNeckCapacity(Path path){
		return 1;
		//TODO implement
	}
	
	/**
	 * TODO
	 * @param path
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
				//TODO look at what tim to raise!!!!
				flow.augmentreverse(time, gamma);
			}
		}
		Node source = path.getSource();
		reduceDemand(source,gamma);
	}
	
	/**
	 * 
	 * @param source
	 * @param gamma
	 */
	private void reduceDemand(Node source, int gamma) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 
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
	 * @param _sink the _sink to set
	 */
	public void setSink(Node sink) {
		this._sink = sink;
	}

	/**
	 * @return the _sources
	 */
	public LinkedList<Node> getSources() {
		return this._sources;
	}

	/**
	 * @param _sources the _sources to set
	 */
	public void setSources(LinkedList<Node> sources) {
		this._sources = sources;
	}

	/**
	 * @return the _timeHorizon
	 */
	public int getTimeHorizon() {
		return this._timeHorizon;
	}

	/**
	 * @param horizon the _timeHorizon to set
	 */
	public void setTimeHorizon(int horizon) {
		_timeHorizon = horizon;
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
	
	
}
