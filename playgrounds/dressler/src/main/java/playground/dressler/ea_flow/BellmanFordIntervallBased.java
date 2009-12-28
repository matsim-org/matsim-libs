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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkLayer;

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


public class BellmanFordIntervallBased {
	/*
	

	*//**
	 * The network on which we find routes. We expect the network not to change
	 * between runs!
	 *//*
	private final NetworkLayer network;

	*//**
	 * data structure to to represent the flow on a network  
	 *//*
	private HashMap<Link, EdgeIntervalls> _flowlabels;
	
	*//**
	 * data structure to keep distance labels on nodes during and after one Iteration of the shortest TimeExpandedPath Algorithm
	 *//*
	private HashMap<Node, VertexIntervalls> _labels;
	
	*//**
	 * data structure to hold the present flow
	 *//*
	private Flow _flow;
 
	*//**
	 * maximal time horizon
	 *//*
	private final int _timehorizon;
	
	*//**
	 * sink node to which TimeExpandedPaths are searched
	 *//*
	private final Node _sink;
	*//**
	 * 
	 *//*
	private TimeExpandedPath _timeexpandedpath;
	 *//**
	  * 
	  *//*
	private static int _warmstart;
	*//**
	 * 
	 *//*
	private LinkedList<Node> _warmstartlist;
	
	*//**
	 * debug variable, the higher the value the more it tells
	 *//*
	private static int _debug=0;

	int gain = 0;
	
	private long _totalpolls=0L;
	
	private int _roundpolls=0;
	
	private long _prepstart=0;
	private long _prepend=0;
	private long _totalpreptime=0;
	
	private long _calcstart=0;
	private long _calcend=0;
	private long _totalcalctime=0;
	
	
	
	//--------------------CONSTRUCTORS-------------------------------------//
	
	*//**
	 * Constructor using all the data initialized in the Flow object use recommended
	 * @param flow 
	 *//*
	public BellmanFordIntervallBased( Flow flow) {
		this._flow = flow;
		this.network = flow.getNetwork();
		this._flowlabels = flow.getFlow();
		this._timehorizon = flow.getTimeHorizon(); 
		this._sink = flow.getSink();
		this._labels = new HashMap<Node, VertexIntervalls>();
	}
	
	
	
	*//**
	 * Setter for debug mode the higher the value the more it tells
	 * @param debug > 0 is debug mode is on
	 *//*
	public static void debug(int debug){
		BellmanFordIntervallBased._debug = debug;
	}
	
	*//**
	 * Setter for warmstart mode 
	 * @param warmstart > 0 is warmstart mode is on
	 *//*
	public static void warmstart(int warmstart){
		BellmanFordIntervallBased._warmstart = warmstart;
	}
	
	private class IntervallNode{
		VertexIntervall _ival;
		Node _node;
		
		IntervallNode(VertexIntervall ival, Node node){
			this._ival = ival;
			this._node = node; 
			
		}
		Boolean equals(IntervallNode node){
			return(node._node.equals(this._node) && node._ival.equals(this._ival));
		}
		
	}
	
	*//**
	 * refreshes all dist labels before one run of the algorithm
	 * @return returns all active sources
	 *//*
	private LinkedList<IntervallNode> refreshLabels(){
		LinkedList<IntervallNode> nodes = new LinkedList<IntervallNode>();
		for(Node node: network.getNodes().values()){
			VertexIntervalls label = new VertexIntervalls();
			_labels.put(node, label);
			if(isActiveSource(node)){
				VertexIntervall temp = _labels.get(node).getIntervallAt(0);
				temp.setReachable(true);
				nodes.add(new IntervallNode(temp,node));
			}
		}
		return nodes;
	}
	
	*//**
	 * decides whether a node is an active source
	 * @param node to be checked
	 * @return true if there is still demand on the node
	 *//*
	private boolean isActiveSource(Node node) {
		if(_debug>3){
			System.out.println(node.getId() + " active:" + this._flow.isActiveSource(node));
		}
		return this._flow.isActiveSource(node);
	}
	
	*//**
	 * Constructs  a TimeExpandedPath based on the labels set by the algorithm 
	 * @return shortest TimeExpandedPath from one active source to the sink if it exists
	 *//*
	private TimeExpandedPath constructRoute()throws BFException{
		Node to = _sink;
		VertexIntervalls tolabels = this._labels.get(to);
		int totime = tolabels.firstPossibleTime();
		//check if TimeExpandedPath can be constructed
		if(Integer.MAX_VALUE==totime){
			throw new BFException("sink can not be reached!");
		}
		if(totime>_timehorizon){
			throw new BFException("sink can not be reached within timehorizon!");
		}
			
		//start constructing the TimeExpandedPath
		TimeExpandedPath TimeExpandedPath = new TimeExpandedPath();
		TimeExpandedPath.setArrival(totime);
		VertexIntervall tolabel = tolabels.getIntervallAt(totime);
		while(tolabel.getPredecessor()!=null){
			Link edge = tolabel.getPredecessor();
			//find out weather forward or backwards edge is used
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
				TimeExpandedPath.push(edge, fromtime, forward);
				to= edge.getFromNode();
			}else{
				fromtime = (totime+_flowlabels.get(edge).getTravelTime());
				TimeExpandedPath.push(edge, fromtime, forward);
				to =edge.getToNode();
			}
			tolabels = this._labels.get(to);
			totime= fromtime;
			tolabel = tolabels.getIntervallAt(totime);
		}
		
		
		return TimeExpandedPath;
	}
	
	
	*//**
	 * method for updating the labels of Node to during one iteration of the algorithm
	 * @param from Node from which we start
	 * @param to Node to which we want to go 
	 * @param over Link upon which we travel
	 * @param forward indicates, weather we use a forward or backwards edge
	 * @return true if any label of Node to has changed
	 *//*
	private LinkedList<IntervallNode> relabel(IntervallNode from, Node to, Link over,boolean forward){
		VertexIntervalls labelfrom = _labels.get(from._node);
		VertexIntervalls labelto = _labels.get(to);
		EdgeIntervalls	flowover = _flowlabels.get(over);
		LinkedList<IntervallNode> changed = new LinkedList<IntervallNode>();
		//int t=0;
		VertexIntervall i = from._ival;
		
		
		
		
		
		
		//do{
			//i = labelfrom.getIntervallAt(t);
			//t=i.getHighBound();
			if(i.getDist()){
				if(_debug>0){
					System.out.println("wir kommen los:"+ from._node.getId());
				}	//TODO cast to int capacity handling!!!
				//if((int)over.getCapacity(1.)==0){
			//		continue;
				//}
				ArrayList<Intervall> arrive = flowover.propagate(i, (int)over.getCapacity(1.),forward);
				if(!arrive.isEmpty()){
					if(_debug>0){
						System.out.println("wir kommen weiter: "+ to.getId());
						for(Intervall inter: arrive){
							System.out.println(forward);
							System.out.println(inter);
						}
					}
					
					for(Intervall possible : arrive){
						LinkedList<VertexIntervall> temp = labelto.setTrue( possible , over );
						for(VertexIntervall tempint : temp ){
							changed.add(new IntervallNode(tempint,to));
						}
					}
					//boolean temp = labelto.setTrue( arrive , over );
					//if(temp){
					//	changed = true;
					//}
				}else{
					if(_debug>0){
						System.out.println("edge: " + over.getId() +" forward:"+forward+ " blocked " + flowover.toString());
					}	
				}
					
			}
		//}while(!labelfrom.isLast(i));
		return changed;
	}
	
	*//**
	 * main bellman ford algorithm calculating a shortest TimeExpandedPath
	 * @return shortest TimeExpandedPath from one active source to the sink if it exists
	 *//*
	public TimeExpandedPath doCalculations() {
		// queue to save nodes we have to scan
		Queue<IntervallNode> queue = new LinkedList<IntervallNode>();
		//set the startLabels and add active sources to to the queue
		LinkedList<IntervallNode> activesources = this.refreshLabels();
		
		
		
		if(_warmstart>0 && _warmstartlist!=null){
			queue.addAll(_warmstartlist);					
			 for( Node node : activesources){
				if(!queue.contains(node)){
					queue.add(node);
				}
				
			}
			queue.addAll(activesources);
		}else{
			queue.addAll(activesources);
		}
		queue.addAll(activesources);

		// v is first vertex in the queue
		// w is the vertex we probably want to insert to the queue and 
		// decrease its distance
		Node w;
		IntervallNode v;
		// dist is the distance from the source to w over v

		// main loop
		//int gain = 0;
		this._roundpolls=0;
		this._calcstart=System.currentTimeMillis();
		while (!queue.isEmpty()) {
			// gets the first vertex in the queue
			v = queue.poll();
			this._roundpolls++;
			this._totalpolls++;
			// Clean Up before we do anything!
			//System.out.println("cleanupnode:"+v.getId().toString()+"\n old: \n"+_labels.get(v).toString());
			gain += _labels.get(v._node).cleanup();
			//System.out.println("new: \n"+_labels.get(v).toString());

			// visit neighbors
			//TODO
			// link is outgoing edge of v => forward edge
			for (Link link : v._node.getOutLinks().values()) {
				w=link.getToNode();
				LinkedList<IntervallNode> changed = relabel(v,w,link,true);
			//	if(!changed.isEmpty()){
					for(IntervallNode changednode : changed){
						//if(!queue.contains(changednode)){
							queue.add(changednode);
						//}
					}
			//	}
				
			}
			// link is incoming edge of v => backward edge
			for (Link link : v._node.getInLinks().values()) {
				w=link.getFromNode();
				LinkedList<IntervallNode> changed = relabel(v,w,link,false);
		//		if(!changed.isEmpty()){
					for(IntervallNode changednode : changed){
			//			if(!queue.contains(changednode)){
							queue.add(changednode);
			//			}
					}
			//	}
			}
			if(_debug>3){
				printStatus();
			}
		}
		this._calcend= System.currentTimeMillis();
		this._totalcalctime+=(this._calcend-this._calcstart);
		if (_debug>3) {
		  System.out.println("Removed " + gain + " intervals.");
		}
		//System.out.println("finale labels: \n");
		//printStatus();
		this._timeexpandedpath = null;
		try{ 
			this._timeexpandedpath = constructRoute();
		}catch (BFException e){
			System.out.println("stop reason: " + e.getMessage());
		}
		if(_warmstart>0){
			this._prepstart= System.currentTimeMillis();
			createwarmstartList();
			this._prepend= System.currentTimeMillis();
			this._totalpreptime+=(this._prepend-this._prepstart);
		}
		return this._timeexpandedpath;
		
	}
	
	
	
	*//**
	 * creates a new warmstartlist, from the data of one run of the BF algorithm an sets _warmstartlist accordingly
	 *//*
	private void createwarmstartList() {
		// use cases of _warmstart to decide what to do
		if (_warmstart == 1) { // add the found path
		  _warmstartlist = new LinkedList<Node>();
		  if (_timeexpandedpath != null)
		  for (TimeExpandedPath.PathEdge edge : _timeexpandedpath.getPathEdges()) {
			  _warmstartlist.add(edge.getEdge().getFromNode());
			  //System.out.println(edge.getEdge().getFromNode().getId());
		  }
		} else if (_warmstart == 2) { // rebuild shortest path tree from last interval
		  _warmstartlist = new LinkedList<Node>();
		 
		  _warmstartlist.addAll(_labels.keySet());
		  
		  Collections.sort(_warmstartlist, new Comparator<Node>() {
		          public int compare(Node n1, Node n2) {
		        	   int v1 = _labels.get(n1).getLast().getLowBound();		        	   
		        	   int v2 = _labels.get(n2).getLast().getLowBound();
		        	   if (v1 > v2) {
		        		  return 1;
		        	   } else if (v1 == v2) {
		        		   return 0;
		        	   } else {
		        		   return -1;
		        	   }
		        	   		               
		          }
		     });
		  
		  for (Node node : _warmstartlist) {
			  System.out.println(node.getId().toString() + " " + _labels.get(node).getLast().getLowBound());
		  }
		  
		} else if (_warmstart == 3) { // rebuild shortest path tree from firstPossibleTime
			  _warmstartlist = new LinkedList<Node>();
				 
			  _warmstartlist.addAll(_labels.keySet());
			  
			  Collections.sort(_warmstartlist, new Comparator<Node>() {
			          public int compare(Node n1, Node n2) {
			        	   int v1 = _labels.get(n1).firstPossibleTime();		        	   
			        	   int v2 = _labels.get(n2).firstPossibleTime();
			        	   if (v1 > v2) {
			        		  return 1;
			        	   } else if (v1 == v2) {
			        		   return 0;
			        	   } else {
			        		   return -1;
			        	   }
			        	   		               
			          }
			     });
			  
			  for (Node node : _warmstartlist) {
				  System.out.println(node.getId().toString() + " " + _labels.get(node).getLast().getLowBound());
			  }
			  
			}
		
	}



	*//**
	 * prints the Status on the console
	 *
	 *//*
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



	public String measure() {
		String result=
		"              Polls: "+this._roundpolls+
		"\n      Preptime (ms): "+(this._prepend-this._prepstart)+
		"\n      Calctime (ms): "+(this._calcend-this._calcstart)+
		"\n         Totalpolls: "+(this._totalpolls)+
		"\n  Totalpreptime (s): "+(this._totalpreptime/1000)+
		"\n  Totalcalctime (s): "+(this._totalcalctime/1000);
		return result;
	}
	*/

}