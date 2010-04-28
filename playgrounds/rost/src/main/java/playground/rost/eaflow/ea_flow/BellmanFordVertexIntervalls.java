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
package playground.rost.eaflow.ea_flow;

// java imports
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import playground.rost.eaflow.Intervall.src.Intervalls.EdgeIntervalls;
import playground.rost.eaflow.Intervall.src.Intervalls.Intervall;
import playground.rost.eaflow.Intervall.src.Intervalls.VertexIntervall;
import playground.rost.eaflow.Intervall.src.Intervalls.VertexIntervalls;
import playground.rost.eaflow.ea_flow.GlobalFlowCalculationSettings.EdgeTypeEnum;


/**
 * Implementation of the Moore-Bellman-Ford Algorithm for a static network! i =
 * 1 .. n for all e = (v,w) if l(w) > l(v) + c(e) then l(w) = l(v) + c(e), p(w) =
 * v.
 * @author Manuel Schneider
 */


public class BellmanFordVertexIntervalls {

	public class NodeComparator implements Comparator<Node>
	{

		public int compare(Node first, Node second) {
			if(_labels.containsKey(first) && _labels.containsKey(second))
			{
				//TODO ROST MAKE MORE EFFICIENT! (use a map to retrieve the intervalls..)
				Integer firstPossibleNextRelabel = retrieveFirstNextPossibleTimeToPropagate(_labels.get(first));
				Integer secondPossibleNextRelabel = retrieveFirstNextPossibleTimeToPropagate(_labels.get(second));
				return firstPossibleNextRelabel.compareTo(secondPossibleNextRelabel);
			}
			else
				throw new RuntimeException("nodes cannot be compared!");
		}

		protected Integer retrieveFirstNextPossibleTimeToPropagate(VertexIntervalls vIntervalls)
		{
			VertexIntervall vIntervall = vIntervalls.getFirstUnscannedIntervall();
			if(vIntervall == null)
				return Integer.MAX_VALUE;
			else
				return vIntervall.getLowBound();
		}
	}



	/**
	 * The network on which we find routes. We expect the network not to change
	 * between runs!
	 */
	private final Network network;

	/**
	 * data structure to to represent the flow on a network
	 */
	private HashMap<Link, EdgeIntervalls> _flowlabels;

	/**
	 * data structure to keep distance labels on nodes during and after one Iteration of the shortest TimeExpandedPath Algorithm
	 */
	private HashMap<Node, VertexIntervalls> _labels;

	/**
	 * data structure to hold the present flow
	 */
	private Flow _flow;

	/**
	 * maximal time horizon
	 */
	private final int _timehorizon;

	/**
	 * sink node to which TimeExpandedPaths are searched
	 */
	private final Node _sink;
	/**
	 *
	 */
	private Collection<TimeExpandedPath> _timeexpandedpath;
	 /**
	  *
	  */
	private static int _warmstart;
	/**
	 *
	 */
	private LinkedList<Node> _warmstartlist;

	/**
	 * debug variable, the higher the value the more it tells
	 */
	private static int _debug=0;

	public int gain = 0;

	private long _totalpolls=0L;
	private int _roundpolls=0;

	private long _prepstart=0;
	private long _prepend=0;
	private long _totalpreptime=0;

	private long _calcstart=0;
	private long _calcend=0;
	private long _totalcalctime=0;

	protected PriorityQueue<Node> queue;
	protected int lastArrival = -1;



	//--------------------CONSTRUCTORS-------------------------------------//

	/**
	 * Constructor using all the data initialized in the Flow object use recommended
	 * @param flow
	 */
	public BellmanFordVertexIntervalls(Flow flow) {
		this._flow = flow;
		this.network = flow.getNetwork();
		this._flowlabels = flow.getFlow();
		this._timehorizon = flow.getTimeHorizon();
		this._sink = flow.getSink();
		this._labels = new HashMap<Node, VertexIntervalls>();
	}



	/**
	 * Setter for debug mode the higher the value the more it tells
	 * @param debug > 0 is debug mode is on
	 */
	public static void debug(int debug){
		BellmanFordVertexIntervalls._debug = debug;
	}

	/**
	 * Setter for warmstart mode
	 * @param warmstart > 0 is warmstart mode is on
	public static void warmstart(int warmstart){
		BellmanFordVertexIntervalls._warmstart = warmstart;
	}

	/**
	 * refreshes all dist labels before one run of the algorithm
	 * @return returns all active sources
	 */
	private LinkedList<Node> refreshLabels(){
		LinkedList<Node> nodes = new LinkedList<Node>();
		for(Node node: network.getNodes().values()){
			VertexIntervalls label = new VertexIntervalls();
			_labels.put(node, label);
			nodes.add(node);
			if(isActiveSource(node)){
				_labels.get(node).getIntervallAt(0).setReachable(true);
				_labels.get(node).getIntervallAt(0).setScanned(false);
			}
		}
		return nodes;
	}

	/**
	 * decides whether a node is an active source
	 * @param node to be checked
	 * @return true if there is still demand on the node
	 */
	private boolean isActiveSource(Node node) {
		if(_debug>3){
			System.out.println(node.getId() + " active:" + this._flow.isActiveSource(node));
		}
		return this._flow.isActiveSource(node);
	}

	/**
	 * Constructs  a TimeExpandedPath based on the labels set by the algorithm
	 * @return shortest TimeExpandedPath from one active source to the sink if it exists
	 */
	private Collection<TimeExpandedPath> constructRoutes()throws BFException{
		Set<TimeExpandedPath> result = new HashSet<TimeExpandedPath>();

		Node to = _sink;
		VertexIntervalls tolabels = this._labels.get(to);
		int totime = tolabels.getFirstUnscannedIntervall().getLowBound();
		int arrivalAtSuperSink = totime;
		//check if TimeExpandedPath can be constructed
		if(Integer.MAX_VALUE==totime){
			throw new BFException("sink can not be reached!");
		}
		if(totime>_timehorizon){
			throw new BFException("sink can not be reached within timehorizon!");
		}
		//collect all reachable sinks, that are connected by an zero transit time, infinite capacity
		//arc.
		Set<Node> realSinksToSendTo = new HashSet<Node>();

		for(Link link : _sink.getInLinks().values())
		{
			Node realSink = link.getFromNode();
			VertexIntervall realSinkIntervall = this._labels.get(realSink).getIntervallAt(totime);
			if(realSinkIntervall.getReachable() && realSinkIntervall.getLowBound() == arrivalAtSuperSink)
				realSinksToSendTo.add(realSink);
		}
		for(Node realSink : realSinksToSendTo)
		{
			//start constructing the TimeExpandedPath
			TimeExpandedPath timeExpandedPath = new TimeExpandedPath();
			timeExpandedPath.setArrival(totime);
			for(Link link : realSink.getOutLinks().values())
			{
				if(link.getToNode().equals(_sink))
					timeExpandedPath.push(link, arrivalAtSuperSink, arrivalAtSuperSink, true);
			}
			//now we can start at each real sink
			tolabels = _labels.get(realSink);
			totime = arrivalAtSuperSink;
			to = realSink;
			VertexIntervall tolabel = tolabels.getIntervallAt(totime);
			int fromtime = 0;
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
				if(forward){
					fromtime = totime - tolabel.getTravelTimeToPredecessor();
					if(fromtime > tolabel.getLastDepartureAtFromNode())
					{
						fromtime = tolabel.getLastDepartureAtFromNode();
						totime = fromtime + tolabel.getTravelTimeToPredecessor();
					}
					timeExpandedPath.push(edge, fromtime, totime, forward);
					to= edge.getFromNode();
				}else{
					if(totime > tolabel.getLastDepartureAtFromNode())
					{
						totime = tolabel.getLastDepartureAtFromNode();
					}
					fromtime = totime + tolabel.getTravelTimeToPredecessor();

					timeExpandedPath.push(edge, totime, fromtime, forward);
					to = edge.getToNode();
				}
				tolabels = this._labels.get(to);
				totime= fromtime;
				tolabel = tolabels.getIntervallAt(totime);
			}
			timeExpandedPath.setArrival(arrivalAtSuperSink);
			timeExpandedPath.setStartTime(fromtime);
			result.add(timeExpandedPath);
		}
		return result;
	}


	/**
	 * method for updating the labels of Node to during one iteration of the algorithm
	 * @param from Node from which we start
	 * @param to Node to which we want to go
	 * @param over Link upon which we travel
	 * @param forward indicates, weather we use a forward or backwards edge
	 * @return true if any label of Node to has changed
	 */
	private boolean relabel(VertexIntervall start, Node from, Node to, Link over,boolean forward){
		VertexIntervalls labelto = _labels.get(to);
		EdgeIntervalls	flowover = _flowlabels.get(over);
		boolean changed=false;
		int t=0;
		if(start.getReachable() && !start.isScanned()){
			if(_debug>0){
				System.out.println("wir kommen los:"+ from.getId());
			}	//TODO cast to int capacity handling!!!
			if((int)over.getCapacity()==0){
				return false;
			}
			ArrayList<VertexIntervall> arrive;
			if(_flow.getEdgeType() == EdgeTypeEnum.SIMPLE)
			{
				arrive = flowover.propagate(start, (int)over.getCapacity(),forward);
			}
			else if(_flow.getEdgeType() == EdgeTypeEnum.BOWEDGES_ADD)
			{
				arrive = flowover.propagateBowEdge(start, (int)over.getCapacity(),forward);
			}
			else
			{
				throw new RuntimeException("No Propagate Function for edgeType");
			}
			if(!arrive.isEmpty()){
				if(_debug>0){
					System.out.println("wir kommen weiter: "+ to.getId());
					for(Intervall inter: arrive){
						System.out.println(forward);
						System.out.println(inter);
					}
				}
				//calc latest start time
				boolean temp = labelto.setTrue( arrive , over);
//					System.out.println("label from: ");
//					int tmp = 0;
//					VertexIntervall curr;
//					do
//					{
//						curr  = labelfrom.getIntervallAt(tmp);
//						tmp = curr.getHighBound();
//						System.out.println(curr);
//					}while(!labelfrom.isLast(curr));
//					System.out.println();
//					System.out.println("label to: ");
//					tmp = 0;
//					do
//					{
//						curr  = labelto.getIntervallAt(tmp);
//						tmp = curr.getHighBound();
//						System.out.println(curr);
//					}while(!labelto.isLast(curr));
//					System.out.println();
				if(temp){
					changed = true;
				}
			}else{
				if(_debug>0){
					System.out.println("edge: " + over.getId() +" forward:"+forward+ " blocked " + flowover.toString());
				}
			}

		}

		return changed;
	}

	/**
	 * main bellman ford algorithm calculating a shortest TimeExpandedPath
	 * @return shortest TimeExpandedPath from one active source to the sink if it exists
	 */
	public Collection<TimeExpandedPath> doCalculations() {
		//set the startLabels and add active sources to to the queue
		LinkedList<Node> activesources = this.refreshLabels();
		// queue to save nodes we have to scan
		NodeComparator f = new NodeComparator();
		queue = new PriorityQueue<Node>(activesources.size(), f);

		if(_warmstart>0 && _warmstartlist!=null){
			queue.addAll(_warmstartlist);
			/* for( Node node : activesources){
				if(!queue.contains(node)){
					queue.add(node);
				}

			}*/
			queue.addAll(activesources);
		}else{
			queue.addAll(activesources);
		}


		// v is first vertex in the queue
		// w is the vertex we probably want to insert to the queue and
		// decrease its distance
		Node v, w;
		// dist is the distance from the source to w over v

		// main loop
		//int gain = 0;
		this._roundpolls=0;
		this._calcstart=System.currentTimeMillis();
		while (!queue.isEmpty()) {
			//sort!
			//Collections.sort(queue, new NodeComparator());

			// gets the first vertex in the queue
			v = queue.remove();
			if(v.equals(this._sink))
				break;
			//System.out.println(_labels.get(v).getFirstUnscannedIntervall() + "");
			this._roundpolls++;
			this._totalpolls++;
			// Clean Up before we do anything!
			//System.out.println("cleanupnode:"+v.getId().toString()+"\n old: \n"+_labels.get(v).toString());
			//System.out.println("new: \n"+_labels.get(v).toString());

			// visit neighbors
			gain += _labels.get(v).cleanup();

			//get intervall we have to propagate:
			VertexIntervall start = _labels.get(v).getFirstUnscannedIntervall();
			if(start == null)
			{
				start = _labels.get(v).getFirstUnscannedIntervall();
			}

			// link is outgoing edge of v => forward edge
			for (Link link : v.getOutLinks().values()) {
				w=link.getToNode();
//				if(v.getId().toString().equals("162491757") && w.getId().toString().equals("162491752"))
//				{
//					int foobar = 0;
//				}
//				if(v.getId().toString().equals("162491752") && w.getId().toString().equals("21306260"))
//				{
//					int foobar = 0;
//				}

				boolean changed = relabel(start,v,w,link,true);
				if (changed) {
					queue.remove(w);
					gain += _labels.get(w).cleanup();
					queue.add(w);
				}
			}
			// link is incoming edge of v => backward edge
			for (Link link : v.getInLinks().values()) {
				w=link.getFromNode();
				boolean changed = relabel(start,v,w,link,false);
				if (changed) {
					queue.remove(w);
					gain += _labels.get(w).cleanup();
					queue.add(w);
				}
			}
			start.setScanned(true);
			if(_debug>3){
				printStatus();
			}
			gain += this._labels.get(v).cleanup();

			queue.add(v);
		}
		this._calcend= System.currentTimeMillis();
		this._totalcalctime+=(this._calcend-this._calcstart);
		if (_debug>3) {
		  System.out.println("Removed " + gain + " intervals.");
		}
		//System.out.println("finale labels: \n");
		//printStatus();
		this._timeexpandedpath = new HashSet<TimeExpandedPath>();
		try{
			this._timeexpandedpath = constructRoutes();
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



	/**
	 * creates a new warmstartlist, from the data of one run of the BF algorithm an sets _warmstartlist accordingly
	 */
	private void createwarmstartList() {
		// use cases of _warmstart to decide what to do


	}



	/**
	 * prints the Status on the console
	 *
	 */
	private void printStatus() {
		int i = 3;
		if(i +1 < 6)
			return; //TODO
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


}
