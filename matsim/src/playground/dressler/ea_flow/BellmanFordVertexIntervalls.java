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

// java imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;

import playground.dressler.Intervall.src.Intervalls.EdgeIntervalls;
import playground.dressler.Intervall.src.Intervalls.Intervall;
import playground.dressler.Intervall.src.Intervalls.VertexIntervall;
import playground.dressler.Intervall.src.Intervalls.VertexIntervalls;


/**
 * Implementation of the Moore-Bellman-Ford Algorithm for a static network! i =
 * 1 .. n for all e = (v,w) if l(w) > l(v) + c(e) then l(w) = l(v) + c(e), p(w) =
 * v.
 * @author Manuel Schneider
 */


public class BellmanFordVertexIntervalls {
	
	/**
	 * The network on which we find routes. We expect the network not to change
	 * between runs!
	 */
	private final NetworkLayer network;

	/**
	 * The cost calculator. Provides the cost for each link and time step.
	 */
	private final TravelCost costFunction;

	/**
	 * The travel time calculator. Provides the travel time for each link and
	 * time step. This is ignored.
	 */
	private final TravelTime timeFunction;
	
	/**
	 * Datastructure to to represent the flow on a network  
	 */
	private HashMap<Link, EdgeIntervalls> _flowlabels;
	
	/**
	 * Datastructure to keep distance labels on nodes during and after one Iteration of the shortest Path Algorithm
	 */
	private HashMap<Node, VertexIntervalls> _labels;
	
	/**
	 * 
	 */
	private Flow _flow;

	/**
	 * 
	 */
	private final int _timehorizon;
	
	/**
	 * 
	 */
	private final Node _sink;

	/**
	 * 
	 */
	final FakeTravelTimeCost length = new FakeTravelTimeCost();
	
	/**
	 * 
	 */
	private static boolean _debug=false;

	/**
	 * Default constructor.
	 * 
	 * @param network
	 *            The network on which to route.
	 * @param costFunction
	 *            Determines the link cost defining the cheapest route. Note,
	 *            comparisons are only made with accuraracy 0.001 due to
	 *            numerical problems otherwise.
	 * @param timeFunction
	 *            Determines the travel time on links. This is ignored!
	 */
	public BellmanFordVertexIntervalls(final NetworkLayer network,
			final TravelCost costFunction, final TravelTime timeFunction,
			HashMap<Link, EdgeIntervalls> flow, int timeHorizon,Node sink) {

		this.network = network;
		this.costFunction = costFunction;
		this.timeFunction = timeFunction;

		this._flowlabels = flow;
		this._timehorizon = timeHorizon;
		this._sink = sink;
		this._labels = new HashMap<Node, VertexIntervalls>();
	}
	
	/**
	 * 
	 * @param costFunction
	 * @param timeFunction
	 * @param flow
	 */
	public BellmanFordVertexIntervalls(final TravelCost costFunction, final TravelTime timeFunction, Flow flow) {
		this._flow = flow;
		this.network = flow.getNetwork();
		this.costFunction = costFunction;
		this.timeFunction = timeFunction;
		this._flowlabels = flow.getFlow();
		this._timehorizon = flow.getTimeHorizon(); 
		this._sink = flow.getSink();
		this._labels = new HashMap<Node, VertexIntervalls>();
	}
	
	
	
	/**
	 * Setter for debug mode
	 * @param debug true is debug mode is on
	 */
	public static void debug(boolean debug){
		BellmanFordVertexIntervalls._debug = debug;
	}
	
	/**
	 * 
	 * @return
	 */
	private LinkedList<Node> refreshLabels(){
		LinkedList<Node> nodes = new LinkedList<Node>();
		for(Node node: network.getNodes().values()){
			VertexIntervalls label = new VertexIntervalls();
			_labels.put(node, label);
			if(isActiveSource(node)){
				nodes.add(node);
				_labels.get(node).getIntervallAt(0).setDist(true);
			}
		}
		return nodes;
	}
	
	/**
	 * 
	 * @param node
	 * @return
	 */
	private boolean isActiveSource(Node node) {
		return this._flow.isActiveSource(node);
	}
	
	/**
	 * 
	 * @return
	 */
	private Path constructRoute()throws BFException{
		Node to = _sink;
		VertexIntervalls tolabels = this._labels.get(to);
		int totime = tolabels.firstPossibleTime();
		//check if path can be constructed
		if(Integer.MAX_VALUE==totime){
			throw new BFException("sink can not be reached!");
		}
		if(totime>_timehorizon){
			throw new BFException("sink can not be reached within timehorizon!");
		}
			
		//start constructing the path
		Path path = new Path();
		path.setArrival(totime);
		VertexIntervall tolabel = tolabels.getIntervallAt(totime);
		while(tolabel.getPredecessor()!=null){
			Link edge = tolabel.getPredecessor();
			//findout weather forward or backwards edge is used
			boolean forward;
			if(edge.getFromNode().equals(to)){
				forward = false;
			}else{
				if(edge.getToNode().equals(to)){
					forward = true;
				}else{
					throw new IllegalArgumentException("edge: " + edge.getId().toString()+ " is not incident to node: "+ to.getId().toString());
				}
			}
			//find next node and edge
			int fromtime;
			if(forward){
				fromtime = (totime-_flowlabels.get(edge).getTravelTime());
				path.push(edge, fromtime, forward);
				to= edge.getFromNode();
			}else{
				fromtime = (totime+_flowlabels.get(edge).getTravelTime());
				path.push(edge, fromtime, forward);
				to =edge.getToNode();
			}
			tolabels = this._labels.get(to);
			totime= fromtime;
			tolabel = tolabels.getIntervallAt(totime);
		}
		
		
		return path;
	}
	
	
	/**
	 * method for updating the labels of Node to
	 * @param from Node from wich we start
	 * @param to Node to which we want to go 
	 * @param over Link upon which we travel
	 * @param forward indicates, weather we use a forwar or backwards edge
	 * @return true if any label of Node to has changed
	 */
	private boolean relabel(Node from, Node to, Link over,boolean forward){
		VertexIntervalls labelfrom = _labels.get(from);
		VertexIntervalls labelto = _labels.get(to);
		EdgeIntervalls	flowover = _flowlabels.get(over);
		boolean changed=false;
		int t=0;
		VertexIntervall i;
		do{
			i = labelfrom.getIntervallAt(t);
			t=i.getHighBound();
			if(i.getDist()){
				if(_debug){
					System.out.println("wir kommen los");
				}	//TODO cas auf int!!!
				ArrayList<Intervall> arrive = flowover.propagate(i, (int)over.getCapacity(1.),forward);
				if(!arrive.isEmpty()){
					if(_debug){
						System.out.println("wir kommen weiter");
						for(Intervall inter: arrive){
							System.out.println(forward);
							System.out.println(inter);
						}
					}
					boolean temp = labelto.setTrue( arrive , over );
					if(temp){
						changed = true;
					}
				}
			}
		}while(!labelfrom.isLast(i));
		return changed;
	}
	
	/**
	 * 
	 * @return
	 */
	public Path doCalculations() {
		// queue to save nodes we have to scan
		Queue<Node> queue = new LinkedList<Node>();
		//set the startLabels and add active sources to to the queue
		queue.addAll(refreshLabels());

		// v is first vertex in the queue
		// w is the vertex we probably want to insert to the queue and 
		// decrease its distance
		Node v, w;
		// dist is the distance from the source to w over v

		// mainloop
		while (!queue.isEmpty()) {
			// gets the first vertex in the queue
			v = queue.poll();

			// visit neighbors
			
			// link is outgoing edge of v => forward edge
			for (Link link : v.getOutLinks().values()) {
				w=link.getToNode();
				boolean changed = relabel(v,w,link,true);
				if (changed && !queue.contains(w)) {
					queue.add(w);
				}
			}
			// link is incomming edge of v => backward edge
			for (Link link : v.getInLinks().values()) {
				w=link.getFromNode();
				boolean changed = relabel(v,w,link,false);
				if (changed && !queue.contains(w)) {
					queue.add(w);
				}
			}
			if(_debug){
				printStatus();
			}
		}
		System.out.println("finale labels: \n");
		printStatus();
		Path path = null;
		try{ 
			path = constructRoute();
		}catch (BFException e){
			System.out.println(e.getMessage());
			//TODO better handling
		}
		return path;
		
	}

	/**
	 * 
	 *
	 */
	private void printStatus() {
		StringBuilder print = new StringBuilder();
		for(Node node : network.getNodes().values()){
			VertexIntervalls inter =_labels.get(node);
			int t =  inter.firstPossibleTime();
			if(t==Integer.MAX_VALUE){
				print.append(node.getId().toString() + " t: "+ "inf." +"\n");
			}else{
				VertexIntervall test =inter.getIntervallAt(t);
				
				if(test.getPredecessor()==null){
					print.append(node.getId().toString() + " t: "+ t +"\n");
				}else{
					print.append(node.getId().toString() + " t: "+ t +" over: "+ inter.getIntervallAt(t).getPredecessor().getId()+ "\n");
				}
			}
		}
		print.append("\n");
		System.out.println(print.toString());	
	}
	

}