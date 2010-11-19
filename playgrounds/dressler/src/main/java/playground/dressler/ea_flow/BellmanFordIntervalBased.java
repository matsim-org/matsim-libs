/* *********************************************************************** *
 * project: org.matsim.*
 * BellmanFordIntervalBased.java
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.management.RuntimeErrorException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;

import playground.dressler.Interval.EdgeFlowI;
import playground.dressler.Interval.Interval;
import playground.dressler.Interval.Pair;
import playground.dressler.Interval.SourceIntervals;
import playground.dressler.Interval.VertexInterval;
import playground.dressler.Interval.VertexIntervals;
import playground.dressler.control.FlowCalculationSettings;
import playground.dressler.util.CPUTimer;
import playground.dressler.util.Dijkstra;


/**
 * @author Manuel Schneider
 */


public class BellmanFordIntervalBased {
		
	/**
	 * data structure to hold the present flow
	 */
	Flow _flow;
 
	/**
	 * the calculation settings, providing most of the information
	 */
	FlowCalculationSettings _settings;
	
	/**
	 * The network on which we find routes. We expect the network not to change
	 * between runs!
	 * This is simply for quick access.
	 */
	final NetworkImpl _network;

	/**
	 * data structure to keep distance labels on nodes during and after one iteration of the shortest TimeExpandedPath Algorithm
	 */
	HashMap<Node, VertexIntervals> _labels;
	
	/*
	 * data structure to remember when a vertex cannot be reached at all
	 * (either from the source or the sink, though only from the source is used right now)
	 * more precisely: <= unreachable(node) is unreachable.
	 * so initialize with -1 
	 */
	
	HashMap<Node, Integer> _unreachable;
	
	
	/**
	 * data structure to keep one label on each source
	 */

	HashMap<Node, VertexInterval> _sourcelabels;
	
	HashMap<Id, Integer> _distforward = null;
	HashMap<Id, Integer> _distreverse = null;	
	
	//private static int _warmstart;
	//private LinkedList<Node> _warmstartlist;
	
	//int _oldLastArrival = -1; // will get changed on the first iteration
	
	// information from the last iteration
	boolean _haslastcost = false;
	int _lastcost = 0;	
	
	// within an interation, can we finish?
	boolean _hasPath;
	
	/**
	 * debug variable, the higher the value the more it tells
	 */
	static int _debug = 0;

	/*
	 * lots of "profiling" data ...
	 */
	long _totalpolls = 0L;
	
	int _roundpolls = 0;
	
	long _totalnonpolls = 0L;
	long _roundnonpolls = 0;
	
	long _vertexGain = 0L;
	long _totalVertexGain = 0L;
	
	/*private long _prepstart=0;
	private long _prepend=0;
	private long _totalpreptime=0;*/
	
	CPUTimer Tcalc = new CPUTimer("Calc");

	CPUTimer Tqueuetime = new CPUTimer("+Queuetime");
	
	CPUTimer Tnormaltime = new CPUTimer("+-normal nodes");
	CPUTimer Tsourcetime = new CPUTimer("+-source nodes");
	CPUTimer Tsinktime = new CPUTimer("+-sink nodes");
		
	// details for the processnormalnode
	CPUTimer Tpickintervaltime = new CPUTimer(" +-Pickintervall");
	CPUTimer Tforwardtime = new CPUTimer(" +-forward");
	CPUTimer Tbackwardtime = new CPUTimer(" +-backward");
	CPUTimer Tpropagate = new CPUTimer("  *-propagate");
	CPUTimer Tsettrue = new CPUTimer("  *-settrue");
	CPUTimer Temptysourcestime = new CPUTimer(" +-emptysources");
	CPUTimer Tupdatesinkstime = new CPUTimer(" +-updatesinks");
	
	CPUTimer Tconstructroutetime = new CPUTimer("+construct routes");
	
	
	//--------------------CONSTRUCTORS-------------------------------------//
	
	/**
	 * Constructor using all the data initialized in the Flow object use recommended
	 * @param flow 
	 */
	public BellmanFordIntervalBased(FlowCalculationSettings settings, Flow flow) {
		this._settings = settings;
		this._flow = flow;
		this._network = settings.getNetwork();
		
		if (this._settings.trackUnreachableVertices) {
			if (this._unreachable == null) {
			  this._unreachable = new HashMap<Node, Integer>(3 * this._network.getNodes().size() / 2);
			}
			
			for (Node node : this._network.getNodes().values()) {
				this._unreachable.put(node, -1);
			}
		}		
	}
	
	
	
	/**
	 * Setter for debug mode the higher the value the more it tells
	 * @param debug > 0 is debug mode is on
	 */
	public static void debug(int debug){
		BellmanFordIntervalBased._debug = debug;
	}
	
	/**
	 * Setter for warmstart mode 
	 * @param warmstart > 0 is warmstart mode is on
	 */
	public static void warmstart(int warmstart){
		System.out.println("Warmstart currently not supported.");
		//BellmanFordIntervalBased._warmstart = warmstart;
	}
	
	
	
	/**
	 * refreshes all _labels and _sourcelabels before one run of the algorithm
	 * and fill the queue
	 */	
	void refreshLabelsForward(TaskQueue queue){		
		this._labels = new HashMap<Node, VertexIntervals>(3 * this._network.getNodes().size() / 2);
		this._sourcelabels = new HashMap<Node, VertexInterval>(3 * this._network.getNodes().size() / 2);
		
		for(Node node: this._network.getNodes().values()){
			VertexInterval temp1 = new VertexInterval(0,this._settings.TimeHorizon);
			VertexIntervals label = new VertexIntervals(temp1);
			_labels.put(node, label);
			if (this._settings.isSource(node)) {
				VertexInterval temp2 = new VertexInterval(0, this._settings.TimeHorizon);
				if (this._flow.isActiveSource(node)){				
					queue.add(new BFTask(new VirtualSource(node), temp2, false));
					temp2.setScanned(false);
					temp2.setReachable(true);				
				} else {
					temp2.setScanned(false);
					temp2.setReachable(false);
				}
				this._sourcelabels.put(node, temp2);
			}
		}		
	}		
	
	/**
	 * refreshes all _labels and _sourcelabels, before one run of the algorithm
	 * and fill the queue
	 */
	void refreshLabelsReverse(TaskQueue queue){		
		this._labels = new HashMap<Node, VertexIntervals>(3 * this._network.getNodes().size() / 2);
		this._sourcelabels = new HashMap<Node, VertexInterval>(3 * this._network.getNodes().size() / 2);
		
		for(Node node: this._network.getNodes().values()){
			VertexInterval temp1 = new VertexInterval(0,this._settings.TimeHorizon);
			VertexIntervals label = new VertexIntervals(temp1);
			_labels.put(node, label);
			// sources are not really special in reverse search, except that they have additional labels
			if (this._settings.isSource(node)) {
				VertexInterval temp2 = new VertexInterval(0, this._settings.TimeHorizon);
				this._sourcelabels.put(node, temp2);
			}
			
			if (this._settings.isSink(node)) {
			    queue.add(new BFTask(new VirtualSink(node), 0, true));
			}
		}		
	}		
	
	
	/**
	 * refreshes all _labels and _sourcelabels, before one run of the algorithm
	 * and fill the queue
	 */
	void refreshLabelsMixed(TaskQueue queue){		
		this._labels = new HashMap<Node, VertexIntervals>(3 * this._network.getNodes().size() / 2);
		this._sourcelabels = new HashMap<Node, VertexInterval>(3 * this._network.getNodes().size() / 2);
		
		for(Node node: this._network.getNodes().values()){
			VertexInterval temp1 = new VertexInterval(0,this._settings.TimeHorizon);
			VertexIntervals label = new VertexIntervals(temp1);
			_labels.put(node, label);
			
			if (this._settings.isSource(node)) {
				// mark sources reachable and add them to the queue
				VertexInterval temp2 = new VertexInterval(0, this._settings.TimeHorizon);
				if (this._flow.isActiveSource(node)){				
					queue.add(new BFTask(new VirtualSource(node), temp2, false));
					temp2.setScanned(false);
					temp2.setReachable(true);
				} else {
					temp2.setScanned(false);
					temp2.setReachable(false);
				}
				this._sourcelabels.put(node, temp2);
			}

			// also add sinks to the queue
			if (this._settings.isSink(node)) {
			    queue.add(new BFTask(new VirtualSink(node), 0, true));
			}
		}		
	}	
	                                   
	
	/**
	 * Constructs  a TimeExpandedPath based on the labels set by the algorithm 
	 * @return shortest TimeExpandedPath from one active source to the sink if it exists
	 */
	List<TimeExpandedPath> constructRoutesForward() throws BFException {
				
		if (_debug > 0) {
		  System.out.println("Constructing routes ...");
		}

		// HashSet does not seem to be deterministic! Don't use it. 
		//Set<TimeExpandedPath> result = new HashSet<TimeExpandedPath>();
		LinkedList<TimeExpandedPath> result = new LinkedList<TimeExpandedPath>();
		
		int earliestArrivalTime = Integer.MAX_VALUE;
		
				
		for (Node superSink : this._flow.getSinks()) {			
			VertexInterval superSinkLabel = this._labels.get(superSink).getFirstPossibleForward();
			if (superSinkLabel != null) {				
				int superSinkTime = superSinkLabel.getLowBound();
				earliestArrivalTime = Math.min(earliestArrivalTime, superSinkTime);
			}			
		}
		
		if (earliestArrivalTime == Integer.MAX_VALUE) {
		  throw new BFException("Sink cannot be reached at all!");
		}
		
		if(earliestArrivalTime >= this._settings.TimeHorizon){
			throw new BFException("Sink cannot be reached within TimeHorizon.");
		}


		for (Node superSink : this._flow.getSinks()) {

			//VertexIntervalls tolabels = this._labels.get(to);			
			VertexInterval superSinkLabel = this._labels.get(superSink).getFirstPossibleForward();
			if (superSinkLabel == null) {
				// unreachable sink
				continue;
			}			

			int superSinkTime = superSinkLabel.getLowBound();
			
			if (superSinkTime > earliestArrivalTime) {
				// still not the best sink
				continue;
			}

			// collect all reachable sinks, that are connected by a zero transit time
			LinkedList<Node> realSinksToSendTo = new LinkedList<Node>();
			HashMap<Node, Link> edgesToSuperSink = new HashMap<Node, Link>();


			boolean notasupersink = false;
			for(Link link : superSink.getInLinks().values()) {
				Node realSink = link.getFromNode();
				// while not strictly necessary, we only want sinks and not just generic predecessors 
				if (this._settings.getLength(link) == 0) {
					VertexInterval realSinkIntervall = this._labels.get(realSink).getIntervalAt(superSinkTime);
					// are we reachable and is there capacity left?
					// (the capacity does not need to be "infinite" because it will be accounted for in the bottleneck					
					if(realSinkIntervall.getReachable()) {
						if (this._flow.getFlow(link).getFlowAt(superSinkTime) < this._settings.getCapacity(link)) {
							realSinksToSendTo.add(realSink);
							edgesToSuperSink.put(realSink, link);
						} else {
							notasupersink = true;
						}
					} // no else, because an unreachable sink does not matter
				} else {
					notasupersink = true;
				}
			}

			if (notasupersink) {
				// put the supersink in there, because it really has interesting edges coming in
				// that is, edges that have non-zero length or finite capacity 
				realSinksToSendTo.add(superSink);
			}

			
			
			for (Node sinkNode : realSinksToSendTo) {
								
				Node toNode = sinkNode;
				int toTime = earliestArrivalTime;		
				
				VertexInterval toLabel = this._labels.get(toNode).getIntervalAt(toTime);
				
				// should not happen
				/*if (toLabel == null) {
					throw new BFException("Sink cannot be reached at all!");
				}*/
				
				// exactly when we want to arrives
				

				//start constructing the TimeExpandedPath
				TimeExpandedPath TEP = new TimeExpandedPath();		

				PathStep pred;

				// include the superSink, whenever we start one step too early
				if (!toNode.equals(superSink)) {				
					pred = new StepEdge(edgesToSuperSink.get(toNode), toTime, toTime, true);
					TEP.append(pred);
					// step into the sink
					pred = new StepSinkFlow(superSink, toTime, true);
					TEP.append(pred);
				} else {
					// step into the sink
					pred = new StepSinkFlow(sinkNode, toTime, true);
					TEP.append(pred);
				}
				
				


				pred = toLabel.getPredecessor();

				while (pred != null) {					
					
					// sadly, we treat Holdover differently, because it cannot be shifted properly
					if (pred instanceof StepHold) {
						
						// could probably recycle toLabel from last iteration ...
						VertexInterval tempi = this._labels.get(toNode).getIntervalAt(toTime);
						
						if (pred.getForward()) {							
							// set the right arrival time, does not change start time for holdover
							pred = pred.copyShiftedToArrival(toTime);
					
							// go back to just before the label that was reached by holdover														
							toTime = tempi.getLowBound() - 1;
							
							// set the right start time, does not change arrival time for holdover
							pred = pred.copyShiftedToStart(toTime);					
						} else {
							pred = pred.copyShiftedToArrival(toTime);
							
							// go forward to the next label							
							toTime = tempi.getHighBound();
							
							// set the right start time, does not change arrival time for holdover
							pred = pred.copyShiftedToStart(toTime);
						}
					} else {
						pred = pred.copyShiftedToArrival(toTime);
					}
					
					TEP.prepend(pred);			

					toNode = pred.getStartNode().getRealNode();
					toTime = pred.getStartTime();
					//TEP.setStartTime(toTime); // really startTime

					if (pred instanceof StepEdge || pred instanceof StepHold) {			  		
						toLabel = this._labels.get(toNode).getIntervalAt(toTime);			 
					} else if (pred instanceof StepSourceFlow) {
						if (pred.getForward()) {
							toLabel = this._sourcelabels.get(toNode);				  
						} else {
							toLabel = this._labels.get(toNode).getIntervalAt(toTime);
						}					
					} else {
						throw new RuntimeException("Unknown instance of PathStep in ConstructRoutes()");
					}
					
					pred = toLabel.getPredecessor();

				}

				//System.out.println("Adding " + TEP);
				result.add(TEP);
			}
		}
		
		return result;
	}
	
	
	/**
	 * Constructs  a TimeExpandedPath based on the labels set by the algorithm 
	 * @return shortest TimeExpandedPath from one active source to the sink if it exists
	 */
	List<TimeExpandedPath> constructRoutesReverse() throws BFException {
		
		if (_debug > 0) {
			System.out.println("Constructing routes ...");
		}
		
		// DEBUG
		/*System.out.println("Normal labels:");
		for (Node node : this._network.getNodes().values()) {
			System.out.println("Node: " + node.getId());
			System.out.println(this._labels.get(node));
		}
		
		System.out.println("Source labels:");
		for (Node node : this._sourcelabels.keySet()) {
			System.out.println("Source: " + node.getId());
			System.out.println(this._sourcelabels.get(node));
		}*/
		
		// HashSet does not seem to be deterministic! Don't use it.
		//Set<TimeExpandedPath> result = new HashSet<TimeExpandedPath>();
		LinkedList<TimeExpandedPath> result = new LinkedList<TimeExpandedPath>();

		for (Node source: this._flow.getSources()) {
			if (!this._flow.isActiveSource(source)) {
				// inactive source, cannot start a path here
				continue;
			}

			//VertexIntervalls tolabels = this._labels.get(to);
			VertexInterval sourceLabel = this._sourcelabels.get(source);
			if (sourceLabel == null || !sourceLabel.getReachable()) {
				// unreachable source
				continue;
			}			

			//start constructing the TimeExpandedPath
			TimeExpandedPath TEP = new TimeExpandedPath();		
			
			Node fromNode = source;
			VertexInterval fromLabel = sourceLabel;

			PathStep succ;

			succ = fromLabel.getSuccessor();
			
			int fromTime = 0; // TODO maybe it's better to leave the sink as late as possible?
			// if it is an active source, we can leave at any time!
			// does this help?

			boolean reachedsink = false;
			while (succ != null) {
				
				// treat Holdover differently to avoid stepping just by +- 1 time layer
				if (succ instanceof StepHold) {
					VertexInterval tempi = this._labels.get(fromNode).getIntervalAt(fromTime);
					
					if (succ.getForward()) {
						// set the right start time, does not change arrival time for holdover
						succ = succ.copyShiftedToStart(fromTime);

						// go to just beyond the label that was reached by holdover														
						fromTime = tempi.getHighBound();
						
						// set the right arrival time, does not change start time for holdover
						succ = succ.copyShiftedToArrival(fromTime);					
					} else {
						succ = succ.copyShiftedToStart(fromTime);
						
						// go back to just before this label
						fromTime = tempi.getLowBound() - 1;
						
						succ = succ.copyShiftedToArrival(fromTime);						
					}								   	
				} else {
				  succ = succ.copyShiftedToStart(fromTime);
				}
				//System.out.println("succ: " + succ);
				TEP.append(succ);

				fromNode = succ.getArrivalNode().getRealNode();
				fromTime = succ.getArrivalTime();

				if (succ instanceof StepEdge || succ instanceof StepHold) {			  		
					fromLabel = this._labels.get(fromNode).getIntervalAt(fromTime);			 
				} else if (succ instanceof StepSourceFlow) {
					if (succ.getForward()) {
						fromLabel = this._labels.get(fromNode).getIntervalAt(fromTime);						
					} else {
						fromLabel = this._sourcelabels.get(fromNode);				  
					}
				} else if (succ instanceof StepSinkFlow) {
					// TODO sinklabels do not exist yet ... but would be a null successor
					reachedsink = true;
					break; 					
				} else {
					throw new RuntimeException("Unknown instance of PathStep in ConstructRoutes()");
				}
				succ = fromLabel.getSuccessor();
			}
			
			// This should always happen if a source is marked reachable at all
			if (reachedsink) {
				//System.out.println(TEP);
				result.add(TEP);
			} else {
				System.out.println("Weird. Did not find the sink!!!");
			}
		}

		return result;
	}
	
	/**
	 * Constructs  a TimeExpandedPath based on the labels set by the algorithm 
	 * @return shortest TimeExpandedPath from one active source to the sink if it exists
	 */
	List<TimeExpandedPath> constructRoutesMixed() throws BFException {
		
		if (_debug > 0) {
			System.out.println("Constructing routes from mixed labels ...");
		}
		
		LinkedList<TimeExpandedPath> result = new LinkedList<TimeExpandedPath>();
		
		/* First we need to determine vertices (more specific: VertexIntervals)
		   that are labelled both forward and backwards.
		   That might take some time ... */
		
		List<VertexInterval> intersection = new ArrayList<VertexInterval>(); 
		
		for (Node node : this._network.getNodes().values()) {
			VertexIntervals VIs = this._labels.get(node);
			if (VIs == null) {
				continue;
			}
			
			VertexInterval current = VIs.getIntervalAt(0);
			while(!VIs.isLast(current)){
				if (current.getPredecessor() != null) {
					// it should be in particular reachable in this case									
					if (current.getSuccessor() != null) {
					  intersection.add(current);
					  break; // we want only the earliest one
					}											
				} 
				current = VIs.getNext(current);							
			}
		}

		
		//System.out.println("size of intersection set: " + intersection.size());
		
		if (intersection.isEmpty()) {
			// We didn't find anything. Maybe the reverse search started to early?
			// Well, let's just do the route construction from forward search
			
			return constructRoutesForward();
		}
		
		
		
		for (VertexInterval reachabletwice: intersection) {
			// the following can only work if the PathSteps stored are shifted correctly!
			
			//start constructing the TimeExpandedPath
			TimeExpandedPath TEP = new TimeExpandedPath();		
			
			
			// first go from the start interval back to the source  
			PathStep pred = reachabletwice.getPredecessor();
			
			// TODO ... if reachabletwice is larger than one unit, one could try to derive
			// multiple paths through it.
			int toTime = reachabletwice.getLowBound();

			// include the superSink, whenever we start one step too early

			while (pred != null) {
				pred = pred.copyShiftedToArrival(toTime);
				toTime = pred.getStartTime();
				
				TEP.prepend(pred);

				Node toNode = pred.getStartNode().getRealNode();
				VertexInterval toLabel;

				if (pred instanceof StepEdge) {			  		
					toLabel = this._labels.get(toNode).getIntervalAt(toTime);			 
				} else if (pred instanceof StepSourceFlow) {
					if (pred.getForward()) {
						toLabel = this._sourcelabels.get(toNode);				  
					} else {
						toLabel = this._labels.get(toNode).getIntervalAt(toTime);
					}
				} else {
					throw new RuntimeException("Unknown instance of PathStep in ConstructRoutes()");
				}
				pred = toLabel.getPredecessor();
			}
			
			// then go from the start interval to the sink
			
			PathStep succ = reachabletwice.getSuccessor();
			int fromTime = reachabletwice.getLowBound();

			while (succ != null) {				
				succ = succ.copyShiftedToStart(fromTime);
				//System.out.println("succ: " + succ);
				TEP.append(succ);

				Node fromNode = succ.getArrivalNode().getRealNode();
				fromTime = succ.getArrivalTime();
				
				VertexInterval fromLabel;

				if (succ instanceof StepEdge) {			  		
					fromLabel = this._labels.get(fromNode).getIntervalAt(fromTime);			 
				} else if (succ instanceof StepSourceFlow) {
					if (succ.getForward()) {
						fromLabel = this._labels.get(fromNode).getIntervalAt(fromTime);						
					} else {
						fromLabel = this._sourcelabels.get(fromNode);				  
					}
				} else if (succ instanceof StepSinkFlow) {
					// TODO sinklabels do not exist yet ... but would be a null successor
					break; 					
				} else {
					throw new RuntimeException("Unknown instance of PathStep in ConstructRoutes()");
				}
				succ = fromLabel.getSuccessor();
			}
			
			result.add(TEP);
		}

		return result;
	}
	
	/**
	 * method for updating the labels of Node to during one iteration of the algorithm
	 * @param from a Task with a VertexInterval
	 * @param ival an Interval during which the from vertex is reachable, no checks performed
	 * @param to Node to which we want to go 
	 * @param over Link upon which we travel
	 * @param original indicates, weather we use an original or residual edge
	 * @param reverse Is this for the reverse search?
	 * @return null or the list of labels that have changed
	 */
	private ArrayList<VertexInterval> relabel(Node from, Interval ival, Node to, Link over, boolean original, boolean reverse, int timehorizon) {		
			VertexIntervals labelto = _labels.get(to);
			EdgeFlowI flowover = this._flow.getFlow(over);
			ArrayList<VertexInterval> changed;


			ArrayList<Interval> arrive;

			VertexInterval arriveProperties = new VertexInterval();
			arriveProperties.setReachable(true);				

			if (!reverse) {
				// Create predecessor. It is not shifted correctly.
				PathStep pred;
				if (original) {
					pred = new StepEdge(over, 0, this._settings.getLength(over), original);
				} else {
					pred = new StepEdge(over, this._settings.getLength(over), 0, original);				
				}
				arriveProperties.setPredecessor(pred);
			} else {
				// Create successor. It is not shifted correctly.			
				PathStep succ;
				if (original) {
					succ = new StepEdge(over, 0, this._settings.getLength(over), original);
				} else {
					succ = new StepEdge(over, this._settings.getLength(over), 0, original);				
				}
				arriveProperties.setSuccessor(succ);
			}



			arrive = flowover.propagate(ival, original, reverse, timehorizon);
						

			if (arrive != null && !arrive.isEmpty()) {				
				changed = labelto.setTrueList(arrive, arriveProperties);
				
				return changed;
			}else{					
				return null;
			}					


	}

	
	TaskQueue processSourceForward(Node v) {
		// send out of source v
		// just set the regular label on v
				
		VertexInterval inter = this._sourcelabels.get(v);
		
		// already scanned or not reachable (neither should occur ...)				
		if (!inter.getReachable()) {
			System.out.println("Source " + v.getId() + " was not reachable!");
			return null;
		}
		
		if (inter.isScanned()) {
			// don't scan again ... but for a source, this should not happen anyway
			return null;
		}
		inter.setScanned(true);
		
		TaskQueue queue = new SimpleTaskQueue();
		
		PathStep pred = new StepSourceFlow(v, 0, true);
		
		//Interval i = new Interval(0, this._settings.TimeHorizon);
		//ArrayList<VertexInterval> changed = this._labels.get(v).setTrueList(i , pred, false);
		VertexInterval arrive = new VertexInterval(0, this._settings.TimeHorizon);
		arrive.setPredecessor(pred);		
		arrive.setReachable(true);
		ArrayList<VertexInterval> changed = this._labels.get(v).setTrueList(arrive);
		for(VertexInterval changedintervall : changed){
			queue.add(new BFTask(new VirtualNormalNode(v, 0), changedintervall, false));
		}
		
		return queue;
	}
	
	
	/**
	 * Does the forward search for a normal node, picking up intervals at time t.
	 * @param v the node to process
	 * @param t the time at which oen should propagate 
	 * @return The resulting tasks and the processed interval (containing t)
	 */
	Pair<TaskQueue, Interval> processNormalNodeForward(Node v, int t) {
				
		Interval inter;
		
		if (this._settings.useImplicitVertexCleanup) {
			Pair<Boolean, Interval> todo = getUnscannedInterSetScanned(v, t, false);
			inter = todo.second;
			if (!todo.first) { // we don't have anything todo
				this._totalnonpolls++;
				this._roundnonpolls++;
				return new Pair<TaskQueue, Interval>(null, inter);
			}
		    
		} else {
			VertexInterval temp = this._labels.get(v).getIntervalAt(t);
			
			if (!temp.getReachable() || temp.getPredecessor() == null) {
				System.out.println("Node " + v.getId() + " was not reachable or had no predecessor!");
				return new Pair<TaskQueue, Interval>(null, temp);
			}
			
			if (temp.isScanned()) {
				// don't scan again ... can happen with vertex cleanup
				this._totalnonpolls++;
				this._roundnonpolls++;
				return new Pair<TaskQueue, Interval>(null, temp);
			}
			temp.setScanned(true);
			inter = temp;
		}
			
		TaskQueue queue = new SimpleTaskQueue();
		
		if (this._settings.useHoldover) {
			int max = inter.getHighBound();
			int min = inter.getLowBound();
			
			ArrayList<VertexInterval> changed; 
			
			// scan holdover forward
			
			// FIXME! This is overkill and the data structure cannot really handle jumping
			// over existing labels in holdover. So only scan forward until the next label!
			// also, the remainder of this function assumes that the interval is connected.
			// (on the other hand, without costs, most of this is irrelevant and bad things
			// cannot happen anyway)
			changed = relabelHoldover(v, inter, true, false, this._settings.TimeHorizon);
			
			if (changed != null) {
				for (VertexInterval changedinterval : changed) {
					
					changedinterval.setScanned(true); // we will do this now
					
					if (changedinterval.getHighBound() > max) {
						max = changedinterval.getHighBound();
					}
					//queue.add(new BFTask(new VirtualNormalNode(v, 0), changedinterval, false));
				}
			}
			
			// scan holdover backward
			changed = relabelHoldover(v, inter, false, false, this._settings.TimeHorizon);
			
			if (changed != null) {
				for (VertexInterval changedinterval : changed) {
					
					changedinterval.setScanned(true); // we will do this now
					
					if (changedinterval.getLowBound() < min) {
						min = changedinterval.getLowBound();
					}
					//queue.add(new BFTask(new VirtualNormalNode(v, 0), changedinterval, false));
				}
			}
			
			if (inter.getLowBound() != min || inter.getHighBound() != max) {
				Interval tempinter = new Interval(min,max);
				//System.out.println(inter + " replaced by " + tempinter);
				//System.out.println(this._labels.get(v));
				inter = tempinter;
			}
		}
		
		// visit neighbors
		// link is outgoing edge of v => forward edge
		for (Link link : v.getOutLinks().values()) {				
			Node w = link.getToNode();
			ArrayList<VertexInterval> changed = relabel(v, inter, w, link, true, false, this._settings.TimeHorizon);
			if (changed == null) continue;

			for(VertexInterval changedinterval : changed){
				queue.add(new BFTask(new VirtualNormalNode(w, 0), changedinterval, false));
			}					

		}
		// link is incoming edge of v => backward edge
		for (Link link : v.getInLinks().values()) {
			Node w = link.getFromNode();
			ArrayList<VertexInterval> changed = relabel(v, inter, w, link, false, false, this._settings.TimeHorizon);
			if (changed == null) continue;

			for(VertexInterval changedinterval : changed){
				queue.add(new BFTask(new VirtualNormalNode(w, 0), changedinterval, false));
			}
		}
		
		// treat empty sources! 
		if (this._flow.isNonActiveSource(v)) {
			if (!this._sourcelabels.get(v).getReachable()) {
				// we might have to do something ...
				// check if we can reverse flow
				SourceIntervals si = this._flow.getSourceOutflow(v);
				Interval arrive = si.canSendFlowBackFirst(inter);
				if (arrive != null) {
					
					// indeed, we need to process this source
					VertexInterval temp = new VertexInterval(0, this._settings.TimeHorizon);
					temp.setScanned(false);
					temp.setReachable(true);	
					
					
					queue.add(new BFTask(new VirtualSource(v), temp, false));
																  
					StepSourceFlow pred = new StepSourceFlow(v, arrive.getLowBound(), false);
					VertexInterval sourcelabel = this._sourcelabels.get(v); 
					sourcelabel.setArrivalAttributesForward(pred);
				}
			}
		}
		
		
		return new Pair<TaskQueue, Interval>(queue, inter);
	}
	
	private ArrayList<VertexInterval> relabelHoldover(Node v, Interval inter,boolean original, boolean reverse,
			int timeHorizon) {
		
		VertexIntervals labelto = _labels.get(v);
		EdgeFlowI flowover = this._flow.getHoldover(v);
		ArrayList<VertexInterval> changed;

		ArrayList<Interval> arrive;

		VertexInterval arriveProperties = new VertexInterval();
		arriveProperties.setReachable(true);				

		if (!reverse) {

			// Create predecessor. It is not shifted correctly. Just the information holdover (and original) should be enough, though.
			PathStep pred;
			if (original) {
				pred = new StepHold(v, inter.getHighBound()-1, inter.getHighBound(), original);
			} else {
				pred = new StepHold(v, inter.getLowBound(), inter.getLowBound()-1, original);
			}

			arriveProperties.setPredecessor(pred);
		} else {
			// Create successor. It is not shifted correctly. Just the information holdover (and original) should be enough, though.
			
			// TODO FIXME test 
			PathStep succ;
			if (original) {
				succ = new StepHold(v, inter.getHighBound()-1, inter.getHighBound(), original);
			} else {
				succ = new StepHold(v, inter.getLowBound(), inter.getLowBound()-1, original);
			}

			arriveProperties.setSuccessor(succ);					
		}



		arrive = flowover.propagate(inter, original, reverse, timeHorizon);


		if (arrive != null && !arrive.isEmpty()) {
			changed = labelto.setTrueList(arrive, arriveProperties);
			return changed;			
		} else {					
			return null;
		}					
	}



	/** Return the (usually) largest interval around t that is unscanned but reachable
	 * This also sets those intervals to scanned!
	 * @param v The node where we are looking.
	 * @param t Time that the interval should contain
	 * @param reverse Is this for the reverse search?
	 * @return a Boolean whether scanning is needed and the interval (containing t) this concerns
	 */
	Pair<Boolean, Interval> getUnscannedInterSetScanned(Node v, int t, boolean reverse) {
		
		VertexIntervals label = this._labels.get(v);
		VertexInterval inter = label.getIntervalAt(t);

		// safety check, should not happen
		if (!reverse) {
			if (!inter.getReachable() || inter.getPredecessor() == null) {

				System.out.println("Node " + v.getId() + " was not reachable "+inter.getReachable()+" or had no predecessor!");
				return new Pair<Boolean, Interval>(false, inter);
			} 
		} else {
			if (!inter.getReachable() || inter.getSuccessor() == null) {
				System.out.println("Node " + v.getId() + " was not reachable "+inter.getReachable()+" or had no successor!");
				return new Pair<Boolean, Interval>(false, inter);

			}
		}

		if (inter.isScanned()) {
			// don't scan again ... can happen with vertex cleanup or this method
			return new Pair<Boolean, Interval>(false, inter);
		}
		inter.setScanned(true);
		
		int low = inter.getLowBound();
		int high = inter.getHighBound();
		
		VertexInterval tempi;
		while (low > 0) {
			tempi = label.getIntervalAt(low - 1);
			if (tempi.getReachable() && !tempi.isScanned()
				&& (     (!reverse && tempi.getPredecessor() != null) 
					  || (reverse && tempi.getSuccessor() != null) ) ) {					 
				tempi.setScanned(true);
				low = tempi.getLowBound();
			} else {
				break;
			}
		}
				
		tempi = inter;
		
		while (!label.isLast(tempi)) {
			tempi = label.getIntervalAt(high);
			if (tempi.getReachable() && !tempi.isScanned()
				&& (     (!reverse && tempi.getPredecessor() != null) 
					  || (reverse && tempi.getSuccessor() != null) ) ) {					 
				
				tempi.setScanned(true);
				high = tempi.getHighBound();
			} else {
				break;
			}
		}
		
		return new Pair<Boolean, Interval>(true, new Interval(low, high)); 
	}


	Pair<TaskQueue, Interval> processNormalNodeReverse(Node v, int t) {
		
		Interval inter;

		if (this._settings.useImplicitVertexCleanup) {
			Pair<Boolean, Interval> todo = getUnscannedInterSetScanned(v, t, true);
			inter = todo.second;
			if (!todo.first) {
				this._totalnonpolls++;
				this._roundnonpolls++;
				return new Pair<TaskQueue, Interval>(null, inter);
			}
		} else {
			VertexInterval temp = this._labels.get(v).getIntervalAt(t);

			if (!temp.getReachable() || temp.getSuccessor() == null) {
				System.out.println("Node " + v.getId() + " was not reachable or had no successor!");
				return new Pair<TaskQueue, Interval>(null, temp);
			}

			if (temp.isScanned()) {
				// don't scan again ... can happen with vertex cleanup
				this._totalnonpolls++;
				this._roundnonpolls++;
				return new Pair<TaskQueue, Interval>(null, temp);
			}
			temp.setScanned(true);
			inter = temp;
		}
		
		
		// TODO hier weiter!
		if (this._settings.useHoldover) {
			//System.out.println("No holdover yet in processNormalNodeReverse()!");
			int max = inter.getHighBound();
			int min = inter.getLowBound();
			
			ArrayList<VertexInterval> changed; 
			
			// scan holdover reverse
			
			// FIXME! This is overkill and the data structure cannot really handle jumping
			// over existing labels in holdover. So only scan forward until the next label!
			// also, the remainder of this function assumes that the interval is connected.
			// (on the other hand, without costs, most of this is irrelevant and bad things
			// cannot happen anyway)
			changed = relabelHoldover(v, inter, true, true, this._settings.TimeHorizon);			
			
			if (changed != null) {
				for (VertexInterval changedinterval : changed) {
					
					changedinterval.setScanned(true); // we will do this now
					
					if (changedinterval.getLowBound() < min) {
						min = changedinterval.getLowBound();
					}
					//queue.add(new BFTask(new VirtualNormalNode(v, 0), changedinterval, false));
				}
			}
			
			// scan holdover residual
			changed = relabelHoldover(v, inter, false, true, this._settings.TimeHorizon);
			
			if (changed != null) {
				for (VertexInterval changedinterval : changed) {
					
					changedinterval.setScanned(true); // we will do this now
					
					if (changedinterval.getHighBound() > max) {
						max = changedinterval.getHighBound();
					}
					//queue.add(new BFTask(new VirtualNormalNode(v, 0), changedinterval, false));
				}
			}
			
			if (inter.getLowBound() != min || inter.getHighBound() != max) {
				Interval tempinter = new Interval(min,max);
				//System.out.println(inter + " replaced by " + tempinter);
				//System.out.println(this._labels.get(v));
				inter = tempinter;
			}
		}
		
		TaskQueue queue = new SimpleTaskQueue();
		
		// visit neighbors
		
		// link is incoming edge of v => forward edges have v as successor
		for (Link link : v.getInLinks().values()) {
			Node w = link.getFromNode();

			ArrayList<VertexInterval> changed = relabel(v, inter, w, link, true, true, this._settings.TimeHorizon);
			
			if (changed == null) continue;

			for(VertexInterval changedinterval : changed){
				queue.add(new BFTask(new VirtualNormalNode(w, 0), changedinterval, true));
			}

		}
		
		// link is outgoing edge of v => backward edges have v as successor
		for (Link link : v.getOutLinks().values()) {					
			Node w = link.getToNode();
			
			ArrayList<VertexInterval> changed = relabel(v, inter, w, link, false, true, this._settings.TimeHorizon);
			
			if (changed == null) continue;
			
			for(VertexInterval changedinterval : changed) {
				queue.add(new BFTask(new VirtualNormalNode(w, 0), changedinterval, true));
			}					
			
		}
		
		

		// treat sources.
		// here it does not matter if they are active or not
		// we can always be reached from them because the links have infinite capacity
		if (this._settings.isSource(v)) {
			// we've found a source, mark it
			VertexInterval vi = this._sourcelabels.get(v);
			
			// maybe we should "leave" the source as late as possible?
			// no, that does seem to make it a lot worse!
			PathStep succ = new StepSourceFlow(v, inter.getLowBound(), true);
			//PathStep succ = new StepSourceFlow(v, inter.getHighBound() - 1, true);
			
			if (this._flow.isActiveSource(v)) {
				// mark it as reachable if it was unreachable 

				// note: trying to arrive as late as possible hurts the performance!
				/* if (!vi.getReachable() 
						|| vi.getSuccessor() == null
						|| vi.getSuccessor().getArrivalTime() < succ.getArrivalTime()) {
				 */

				if (!vi.getReachable()) {
					vi.setArrivalAttributesReverse(succ);
					// An active source doesn't really need processing,
					// but we do it anyway to handle this case centrally.
					queue.add(new BFTask(new VirtualSource(v), 0, true));
				}	
			} else { // isNonActiveSource(v) == true
				if (!vi.getReachable()) {

					vi.setArrivalAttributesReverse(succ);

					// non active sources really need to propagate
					if (this._flow.isNonActiveSource(v)) {
						queue.add(new BFTask(new VirtualSource(v), 0, true));
					}
				}
			}
		}			
		
       return new Pair<TaskQueue, Interval>(queue, inter);	
	}
	
	TaskQueue processSinkReverse(Node v, int lastArrival) {
		// we want to arrive at lastArrival
		// propagate that to the associated real node
		
		// TODO sinklabels do not exist yet, but should be used here

		// the lower part of the interval does not really matter, but it avoids a lot of polls
		// and speed things up by a factor of 10.				
		// (0, lastArrival + 1)  == good
		// (lastArrival, lastArrival + 1) == bad, do not use
		
		VertexInterval arrive = new VertexInterval(0, lastArrival + 1);
		//VertexInterval arrive = new VertexInterval(lastArrival, lastArrival + 1);
		
		PathStep succ = new StepSinkFlow(v, lastArrival, true);
		arrive.setSuccessor(succ);
		arrive.setReachable(true);
		
		ArrayList<VertexInterval> changed = this._labels.get(v).setTrueList(arrive);
		
		TaskQueue queue = new SimpleTaskQueue();
		
		for(VertexInterval changedintervall : changed){
			queue.add(new BFTask(new VirtualNormalNode(v, 0), changedintervall, true));
		}
		
		return queue;
	}
	
	TaskQueue processSourceReverse(Node v) {
		
		// active sources are the end of the search				
		if (this._flow.isActiveSource(v)) {
			// we have a shortest path
			_hasPath = true;			
			return null;
		}
		
		// nonactive sources are just transit nodes, and need to scan residual edges.		
		
		VertexInterval inter = this._sourcelabels.get(v);
		
		// already scanned or not reachable (neither should occur ...)
		if (inter.isScanned() || !inter.getReachable()) {
			System.out.println("Source " + v.getId() + " was already scanned or not reachable ...");
			return null;
		}
		inter.setScanned(true);

		ArrayList<Interval> sendBackWhen = this._flow.getSourceOutflow(v).canSendFlowBackAll(this._settings.TimeHorizon);

		// successor depends on the time, but should get adjusted

		PathStep succ = new StepSourceFlow(v, 0, false);
		VertexInterval arriveProperties = new VertexInterval();				
		arriveProperties.setArrivalAttributesReverse(succ);
		ArrayList<VertexInterval> changed = this._labels.get(v).setTrueList(sendBackWhen , arriveProperties);

		if (changed == null) return null;
		
		TaskQueue queue = new SimpleTaskQueue();

		for(VertexInterval changedintervall : changed){
			queue.add(new BFTask(new VirtualNormalNode(v, 0), changedintervall, true));			
		}
		return queue;
	}
	
	
	
	/**
	 * main bellman ford algorithm calculating a shortest TimeExpandedPath
	 * @return maybe multiple TimeExpandedPath from active source(s) to the sink if it exists
	 */
	public List<TimeExpandedPath> doCalculationsForward() {
	
		if (_debug > 0) {
		  System.out.println("Running BellmanFord in Forward mode.");
		} else {
			System.out.print(">");
		}
		
		Tcalc.onoff();		
		Tqueuetime.onoff();
		// queue to save nodes we have to scan		
		TaskQueue queue;
		
		switch (this._settings.queueAlgo) {
		  case FlowCalculationSettings.QUEUE_BFS:
			  queue = new SimpleTaskQueue();
			  break;
		  case FlowCalculationSettings.QUEUE_DFS:
			  queue = new PriorityTaskQueue(new TaskComparator());
			  break;
		  case FlowCalculationSettings.QUEUE_GUIDED:
		  case FlowCalculationSettings.QUEUE_STATIC:
			  
			  // computing the distances is only needed once, because sinks always remain active
			  if (this._distforward == null) {				  
				  Dijkstra dijkstra = new Dijkstra(this._settings);
				  //compute distances to sinks
				  dijkstra.setStart(this._flow.getSinks()); 
				  this._distforward = dijkstra.calcDistances(false, true);
				  //System.out.println(this._distforward);
			  }			  
			  Comparator<BFTask> taskcomp;
			  if (this._settings.queueAlgo == FlowCalculationSettings.QUEUE_GUIDED) {
				 taskcomp = new TaskComparatorGuided(this._distforward);  
			  } else {
				 taskcomp = new TaskComparatorStaticGuide(this._distforward);
			  }
			  
			  queue = new PriorityTaskQueue(taskcomp); 
			  break;
		  default:
			  throw new RuntimeException("Unsupported Queue Algo!");
		}		

		//set fresh labels, initialize queue
		refreshLabelsForward(queue);
		Tqueuetime.onoff();
		
		//System.out.println(this._labels);
		//System.out.println(this._sourcelabels);

		// TODO warmstart
		
		// where the network should be empty
		// this is decreased whenever a sink is found
		int cutofftime = this._settings.TimeHorizon;

		int finalPoll = Integer.MAX_VALUE / 2;
		boolean quickCutOffArmed = false;
		_hasPath = false;
		
		BFTask task;

		// main loop
			
		
		while (true) {
			//System.out.println("The queue is: ");
			//System.out.println(queue);
		
			this._roundpolls++;
			this._totalpolls++;			
			
			Tqueuetime.onoff(); // rather meaningless
			// gets the first task in the queue		
			task = queue.poll();
			Tqueuetime.onoff();
			
			if (task == null) {
				break;
			}
											
			
			if (!quickCutOffArmed &&  _hasPath && !this._settings.trackUnreachableVertices) {								
				// now, we could stop ...
				
				// do we want to stop after the first path is found?
				if (this._settings.quickCutOff >= 0.0) {			
					finalPoll = (int) ((Integer) this._roundpolls * (1.0 + _settings.quickCutOff));
					quickCutOffArmed = true;
					if (_debug > 0) {
						System.out.println("Quickcutoff activated. currentPoll: " + this._roundpolls + " finalPoll: " + finalPoll);
					}
					//break;
				}
			}
			
			if (quickCutOffArmed && this._roundpolls > finalPoll) {
				if (_debug > 0) {
					System.out.println("Reached final poll, cutoff!");
				}
				break;
			}
			
			if (task.time > cutofftime) {
				//System.out.println("Ignoring too late task in BFS!");				
				continue;

			}
			
			Node v = task.node.getRealNode();
			
			if (this._settings.isSink(v)) {		
				Tsinktime.onoff();
				if (this._flow.isActiveSink(v)) {
					if (task.time < cutofftime) {				  
						cutofftime = task.time;	
						if (_debug > 0) {
							System.out.println("Setting new cutoff time: " + cutofftime);
						}
						
						// do we have a shortest path?
						if (_haslastcost && cutofftime == _lastcost) {
							_hasPath = true;
						}
					}
					
				}
				Tsinktime.onoff();
			} else if (task.node instanceof VirtualSource) {
				Tsourcetime.onoff();
				// send out of source v
				
				TaskQueue tempqueue = processSourceForward(v); 
				Tsourcetime.onoff();
				
				Tqueuetime.onoff();
				if (tempqueue != null) {					
				  queue.addAll(tempqueue);				  
				}
				Tqueuetime.onoff();
				
			} else if (task.node instanceof VirtualNormalNode) {
				Tnormaltime.onoff();
				if (this._settings.useVertexCleanup) { 			
					// Effectiveness with this implementation is questionable
					this._vertexGain += _labels.get(v).cleanup();
				}

				// We want to ensure that we really scan all of task.ival .
				// This does not really matter here, but in the search with cost.
				int low = task.ival.getLowBound();
				while (low < task.ival.getHighBound()) {
					Pair<TaskQueue, Interval> ret = processNormalNodeForward(v, low); 
					TaskQueue tempqueue = ret.first;

					Tnormaltime.onoff();
					Tqueuetime.onoff();
					if (tempqueue != null) {
						queue.addAll(tempqueue);
					}
					Tqueuetime.onoff();
					Tnormaltime.onoff();
					
					low = ret.second.getHighBound() + 1;
				}
				Tnormaltime.onoff();
			} else {
				throw new RuntimeException("Unsupported instance of VirtualNode in BellmanFordIntervalBased");
			}
						
			
			if(_debug>3){
				printStatus();
			}
		}
		
		
		// update the unreachable marks if this is not a pure forward search;
		
		if (this._settings.trackUnreachableVertices) {
			if (quickCutOffArmed && this._roundpolls >= finalPoll) {
				// DO NOTHING!			
				// with quickcutoff, we cannot determine unreachable vertices!
			} else {
				for (Node node : this._network.getNodes().values()) {
					VertexInterval iv = this._labels.get(node).getFirstPossibleForward();
					int t;
					if (iv != null) {
						t = iv.getLowBound() - 1; // lowbound is just reachable, -1 is not

						// the following can happen, and that part of the network was not scanned fully!
						// so the labels are unreliable!
						if (t > cutofftime) {
							t = cutofftime; 
						}				  
					} else {
						// not reachable at all so far
						t = cutofftime;					
					}

					// DEBUG
					if (t < this._unreachable.get(node)) {
						System.out.println("Huh, new unreachable < old unreachable " + node.getId() + " time t " + t + " old unreachable " + this._unreachable.get(node));
					}

					this._unreachable.put(node, t);
				}
			}
		}
		
		
		
		
		this.Tcalc.onoff();
		
		//checkAllLabels();
		
		//System.out.println("final labels: \n");
		//printStatus();
				
		Tconstructroutetime.onoff();
		List<TimeExpandedPath> TEPs = null; 
		try{ 
			TEPs  = constructRoutesForward();		
		}catch (BFException e){
			System.out.println("stop reason: " + e.getMessage());
		}
		Tconstructroutetime.onoff();
		//System.out.println(TEPs);
				
		return TEPs;
		
	}
	
	
	/**
	 * Main algorithm calculating a shortest TimeExpandedPath
	 * this version constructs the shortest path tree starting at the sink
	 * @param lastArrival The time the flow reached the sink in the last iteration. Without this, this is all pretty pointless.
	 *         Also, it only helps if the sink is still reachable at that time. Otherwise, nothing will be found.  
	 * @return maybe multiple TimeExpandedPath from active source(s) to the sink if it exists
	 */
	public List<TimeExpandedPath> doCalculationsReverse(int lastArrival) {
		if (_debug > 0) {
			System.out.println("Running BellmanFord in Reverse mode.");
		} else {
			System.out.print("<");
		}
		
		Tcalc.onoff();
		Tqueuetime.onoff();
		// queue to save nodes we have to scan
		TaskQueue queue;
		
		switch (this._settings.queueAlgo) {
		  case FlowCalculationSettings.QUEUE_BFS:
			  queue = new SimpleTaskQueue();
			  break;
		  case FlowCalculationSettings.QUEUE_DFS:
			  queue = new PriorityTaskQueue(new TaskComparatorReverse());
			  break;
		  case FlowCalculationSettings.QUEUE_GUIDED:
		  case FlowCalculationSettings.QUEUE_STATIC:
			
			  // FIXME needs option to recalc these distances less often!
			  
			  Dijkstra dijkstra = new Dijkstra(this._settings);
			  //compute distance to sources 
							  
			  for (Node node : this._flow.getSources()) {
			    if (this._flow.isActiveSource(node)) {
			    	dijkstra.setStart(node.getId());			   
			    }
			  }
			  this._distreverse = dijkstra.calcDistances(true, false);
			  //System.out.println(this._distreverse);
				
			  Comparator<BFTask> taskcomp;
			  if (this._settings.queueAlgo == FlowCalculationSettings.QUEUE_GUIDED) {
				 taskcomp = new TaskComparatorGuided(this._distreverse);  
			  } else {
				 taskcomp = new TaskComparatorStaticGuide(this._distreverse);
			  }
			  
			  queue = new PriorityTaskQueue(taskcomp); 
			  break;
		  default:
			  throw new RuntimeException("Unsupported Queue Algo!");
		}		
			
		//set fresh labels, initialize queue
		refreshLabelsReverse(queue);
		Tqueuetime.onoff();
		
		//System.out.println(this._labels);
		//System.out.println(this._sourcelabels);
		
		int cutofftime = lastArrival;
		int finalPoll = Integer.MAX_VALUE / 2;
		boolean quickCutOffArmed = false;
		_hasPath = false;
		
		// TODO warmstart
		
		BFTask task;

		// main loop
		//int gain = 0;

		while (true) {
			//System.out.println("The queue is: ");
			//System.out.println(queue);
			
			this._roundpolls++;
			this._totalpolls++;			
									
			// gets the first task in the queue
			Tqueuetime.onoff();
			task = queue.poll();
			Tqueuetime.onoff();
			if (task == null) {
				break;
			}
			
			if (!quickCutOffArmed && _hasPath) {								
				// now, we could stop ...
				
				if (this._settings.quickCutOff >= 0.0) {			
					finalPoll = (int) ((Integer) this._roundpolls * (1.0 + _settings.quickCutOff));
					quickCutOffArmed = true;
					if (_debug > 0) {
						System.out.println("Quickcutoff activated. currentPoll: " + this._roundpolls + " finalPoll: " + finalPoll);
					}
					//break;
				}
			}
			
			if (quickCutOffArmed && this._roundpolls > finalPoll) {
				if (_debug > 0) {
					System.out.println("Reached final poll, cutoff!");
				}
				break;
			}
			
				
			if (task.time > cutofftime) {
				// the algorithm should never get here?
				System.out.println("Beyond cut-off time, ignoring.");
				continue;
			}		
			
			Node v = task.node.getRealNode();
			
			if (task.node instanceof VirtualSink) {
				
				TaskQueue tempqueue = processSinkReverse(v, lastArrival); 

				Tqueuetime.onoff();
				if (tempqueue != null) {
					queue.addAll(tempqueue);
				}
				Tqueuetime.onoff();
				
			} else 	if (task.node instanceof VirtualSource) {

				TaskQueue tempqueue = processSourceReverse(v); 

				Tqueuetime.onoff();
				if (tempqueue != null) {
					queue.addAll(tempqueue);
				}
				Tqueuetime.onoff();
				
			} else if (task.node instanceof VirtualNormalNode) {
				
				if (this._settings.useVertexCleanup) { 			
					// Effectiveness with this implementation is questionable
					this._vertexGain += _labels.get(v).cleanup();
				}
				
				if (this._settings.trackUnreachableVertices) {
					if (task.ival.getHighBound() <= this._unreachable.get(v)) {
						//System.out.println("Skipping a task!");
						continue;
					}
				}
				
				int low = task.ival.getLowBound();
				while (low < task.ival.getHighBound()) {
					Pair<TaskQueue, Interval> ret = processNormalNodeReverse(v, low); 
					TaskQueue tempqueue = ret.first;

					Tqueuetime.onoff();
					if (tempqueue != null) {
						queue.addAll(tempqueue);
					}
					Tqueuetime.onoff();
					low = ret.second.getHighBound() + 1;
				}

			} else {
				throw new RuntimeException("Unsupported instance of VirtualNode in BellmanFordIntervalBased");
			}
						
			
			if(_debug>3){
				printStatus();
			}
		}
		
		this.Tcalc.onoff();
		
		//checkAllLabelsReverse();
		
		//System.out.println("final labels: \n");
		//printStatus();
		
		boolean foundsome = false; 
		List<TimeExpandedPath> TEPs = null; 
		try{ 
			TEPs  = constructRoutesReverse();			
			foundsome = true;
		} catch (BFException e) {
			System.out.println("stop reason: " + e.getMessage());
		}
		
	    return TEPs;
	
	}
	
	/**
	 * Main algorithm calculating a shortest TimeExpandedPath
	 * this version constructs the shortest path tree starting at the sources and sinks simultaneously
	 * @param lastArrival The time the flow reached the sink in the last iteration.
	 * @return maybe multiple TimeExpandedPath from active source(s) to the sink if it exists
	 */
	public List<TimeExpandedPath> doCalculationsMixed(int lastArrival) {
		if (_debug > 0) {
			System.out.println("Running BellmanFord in Mixed mode.");
		} else {
			System.out.print("*");
		}
		
		// queue to save nodes we have to scan
		
		// PriorityQueue seems to be much slower than a regular Breadth First Search
		// at least the finding of multiple paths seems much more effective with BFS!
		
		//TaskComparator taskcomp = new TaskComparator();
		//Queue<BFTask> queue = new PriorityQueue<BFTask>((1), taskcomp);
		TaskQueue queue = new SimpleTaskQueue();

		//set fresh labels, initialize queue
		
		refreshLabelsMixed(queue);		
		
		//System.out.println(this._labels);
		//System.out.println(this._sourcelabels);
		
		int cutofftimeReverse = lastArrival;
		int cutofftimeForward = this._settings.TimeHorizon;

		// TODO warmstart
		
		BFTask task;

		// main loop
		//int gain = 0;
		this.Tcalc.onoff();
		
		while (true) {
			//System.out.println("The queue is: ");
			//System.out.println(queue);
			
			this._roundpolls++;
			this._totalpolls++;			
			
			// gets the first task in the queue			
			task = queue.poll();
			if (task == null) {
				break;
			}
			
			Node v = task.node.getRealNode();
			
			if (task.reverse) { // do a reverse step
				if (task.time > cutofftimeReverse) {
					// the algorithm should never get here?
					System.out.println("Beyond cut-off time, ignoring.");
					continue;
				}
				
				
				if (task.node instanceof VirtualSink) {
					
					TaskQueue tempqueue = processSinkReverse(v, lastArrival); 

					if (tempqueue != null) {
						queue.addAll(tempqueue);
					}
					
				} else 	if (task.node instanceof VirtualSource) {
					
					TaskQueue tempqueue = processSourceReverse(v); 

					if (tempqueue != null) {
						queue.addAll(tempqueue);
					}
					
				} else if (task.node instanceof VirtualNormalNode) {
					
					if (this._settings.useVertexCleanup) { 			
						// Effectiveness with this implementation is questionable
						this._vertexGain += _labels.get(v).cleanup();
					}									
					
					int low = task.ival.getLowBound();
					while (low < task.ival.getHighBound()) {
						Pair<TaskQueue, Interval> ret = processNormalNodeReverse(v, low); 
						TaskQueue tempqueue = ret.first;

						if (tempqueue != null) {
							queue.addAll(tempqueue);
						}
						low = ret.second.getHighBound() + 1;
					}
				} else {
					throw new RuntimeException("Unsupported instance of VirtualNode in BellmanFordIntervalBased");
				}
				
			} else { // do a forward step
				if (task.time > cutofftimeForward) {
					//System.out.println("Ignoring too late task in BFS!");
					continue;

				}
				
				if (this._settings.isSink(v)) {
					// keep scanning until strictly later to give more sinks a chance to be found!
					if (this._flow.isActiveSink(v)) {
						if (task.time < cutofftimeForward) {				  
							cutofftimeForward = task.time;	
							if (_debug > 0) {
								System.out.println("Setting new cutoff time: " + cutofftimeForward);
							}
						}				
					}
				} else if (task.node instanceof VirtualSource) {
					// send out of source v
					TaskQueue tempqueue = processSourceForward(v); 
					
					if (tempqueue != null) {
					  queue.addAll(tempqueue);
					}
					
				} else if (task.node instanceof VirtualNormalNode) {
					
					if (this._settings.useVertexCleanup) { 			
						// Effectiveness with this implementation is questionable
						this._vertexGain += _labels.get(v).cleanup();
					}

					int low = task.ival.getLowBound();
					while (low < task.ival.getHighBound()) {
						Pair<TaskQueue, Interval> ret = processNormalNodeForward(v, low); 
						TaskQueue tempqueue = ret.first;

						if (tempqueue != null) {
							queue.addAll(tempqueue);
						}
						low = ret.second.getHighBound() + 1;
					}

				} else {
					throw new RuntimeException("Unsupported instance of VirtualNode in BellmanFordIntervalBased");
				}
							
			}
			
			
			if(_debug>3){
				printStatus();
			}
		}
		
		this.Tcalc.onoff();
		if (_debug>3) {
		  System.out.println("Removed " + this._vertexGain + " intervals.");
		}
		
		//System.out.println("final labels: \n");
		//printStatus();
		
		
		boolean foundsome = false; 
		List<TimeExpandedPath> TEPs = null; 
		try{ 
			TEPs  = constructRoutesMixed();
			foundsome = true;
		}catch (BFException e){
			System.out.println("stop reason: " + e.getMessage());
		}
		
		//System.out.println("Gain from Cleanup: " + gain);
			
		  return TEPs;		
	}
	
	
	/**
	 * creates a new warmstartlist, from the data of one run of the BF algorithm an sets _warmstartlist accordingly
	 */
	private void createwarmstartList() {
		System.out.println("Warmstart is not implemented!");
		/*
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
			  
			}*/
		
	}

	
	private boolean checkAllLabels() {
		System.out.println("Checking all labels ... ");
		// check normal links
		
		boolean allokay = true;

		for (Node from : this._network.getNodes().values()) {
			
			// don't check sinks ... they are not processed
			if (this._settings.isSink(from)) {
				continue;
			}
			
			VertexIntervals VIfrom = (VertexIntervals) this._labels.get(from);
			
			// check label itself
			boolean thislabelokay = true;
			for (int t = 0; t < this._settings.TimeHorizon; t++) {
				VertexInterval ifrom = VIfrom.getIntervalAt(t);
				if (ifrom.getReachable() && !ifrom.isScanned()) {
					thislabelokay = false;
				}
			}
			
			if (!thislabelokay) {
				allokay = false;
				System.out.println("Reachable but not scanned interval!");
				System.out.println("Node v " + from.getId());
				System.out.println(VIfrom);
			}
			
			// check holdover
			if (this._settings.useHoldover) {
				boolean holdoverokay = true;
				
				// forward holdover
				for (int t = 0; t < this._settings.TimeHorizon - 1; t++) {
					VertexInterval ifrom = VIfrom.getIntervalAt(t);
					// FIXME ignores holdover capacities
					if (ifrom.getReachable() && !(VIfrom.getIntervalAt(t+1).getReachable())) {
						holdoverokay = false;
						System.out.println("bad t = " + t);
					}
				}	
				if (!holdoverokay) {
					allokay = false;
					System.out.println("Holdover forward not correct!");
					System.out.println("Node v " + from.getId());
					System.out.println(VIfrom);
				}
				
				
				// backward holdover
				holdoverokay = true;				
				for (int t = 1; t < this._settings.TimeHorizon; t++) {
					VertexInterval ifrom = VIfrom.getIntervalAt(t);
					if (ifrom.getReachable() && !(VIfrom.getIntervalAt(t-1).getReachable())) {
						if (this._flow.getHoldover(from).getFlowAt(t-1) > 0) {
						  holdoverokay = false;
						  System.out.println("bad t = " + t);
						}
					}
				}	
				if (!holdoverokay) {
					allokay = false;
					System.out.println("Holdover backward not correct!");
					System.out.println("Node v " + from.getId());
					System.out.println(VIfrom);
					System.out.println("Holdover flow:\n");
					System.out.println(this._flow.getHoldover(from));
				}
			}
			
			// forward links
			for (Link edge : from.getOutLinks().values()) {
				Node to = edge.getToNode();
				VertexIntervals VIto = this._labels.get(to);
				EdgeFlowI EF = this._flow.getFlow(edge);
				int length = this._settings.getLength(edge);
				boolean thisedgeokay = true;
				for (int t = 0; t < this._settings.TimeHorizon - length; t++) {
					VertexInterval ifrom = VIfrom.getIntervalAt(t);
					if (ifrom.getReachable()) {
						if (EF.getFlowAt(t) < this._settings.getCapacity(edge)) {
							VertexInterval ito = VIto.getIntervalAt(t + length);

							boolean okay = true;

							if (!ito.getReachable()) { 
								okay = false;
							}

							if (!okay) {		
								System.out.println("bad t = " + t);
								thisedgeokay = false;
							}
						}
					}
				}
				if (!thisedgeokay) {		
					allokay = false;
					System.out.println("Label not okay!");
					System.out.println("From node " + from.getId());
					System.out.println("To node " + to.getId());
					System.out.println("Edge " + edge.getId() + " forward");
					System.out.println("Edge from " + edge.getFromNode().getId() + " to " + edge.getToNode().getId());
					System.out.println("Edge length " + length + " edge cap " + this._settings.getCapacity(edge));					
					System.out.println("From label:\n");
					System.out.println(VIfrom);
					System.out.println("To label:\n");
					System.out.println(VIto);
					System.out.println("Edge flow:\n");
					System.out.println(EF);
				}
			}

			// backward links
			for (Link edge : from.getInLinks().values()) {
				Node to = edge.getFromNode();
				VertexIntervals VIto = this._labels.get(to);
				EdgeFlowI EF = this._flow.getFlow(edge);
				int length = this._settings.getLength(edge);
				boolean thisedgeokay = true;				
				for (int t = length; t < this._settings.TimeHorizon; t++) {
					VertexInterval ifrom = VIfrom.getIntervalAt(t);
					if (ifrom.getReachable()) {
						if (EF.getFlowAt(t - length) > 0) {
							VertexInterval ito = VIto.getIntervalAt(t - length);

							boolean okay = true;

							if (!ito.getReachable()) { 
								okay = false;
							}

							if (!okay) {		
								System.out.println("bad t = " + t);
								thisedgeokay = false;
							}
						}
					}
				}

				if (!thisedgeokay) {		
					allokay = false;
					System.out.println("Label not okay!");
					System.out.println("From node " + from.getId());
					System.out.println("To node " + to.getId());
					System.out.println("Edge " + edge.getId() + " backward");
					System.out.println("Edge from " + edge.getFromNode().getId() + " to " + edge.getToNode().getId());
					System.out.println("Edge length " + length + " edge cap " + this._settings.getCapacity(edge));					
					System.out.println("From label:\n");
					System.out.println(VIfrom);
					System.out.println("To label:\n");
					System.out.println(VIto);
					System.out.println("Edge flow:\n");
					System.out.println(EF);
				}
			}
			
		}
		
		// check sources
		for (Node v : this._flow.getSources()) {
			VertexInterval ifrom = this._sourcelabels.get(v);

			// check basic attributes
			if (this._flow.isActiveSource(v) && !ifrom.getReachable()) {
				allokay = false;
				System.out.println("Active source not updated properly!");
				System.out.println("Source v " + v);
				System.out.println(ifrom);
			}

			if (ifrom.getReachable() && !ifrom.isScanned()) {
				allokay = false;
				System.out.println("Reachable but not scanned interval!");
				System.out.println("Source v " + v);
				System.out.println(ifrom);
			}

			VertexIntervals VIto = this._labels.get(v);
			SourceIntervals SI = this._flow.getSourceOutflow(v);
			
			// check forward flow out of source
			if (ifrom.getReachable()) {
				boolean thisokay = true;

				for (int t = 0; t < this._settings.TimeHorizon; t++) {
					VertexInterval ito = VIto.getIntervalAt(t);

					boolean okay = true;

					if (!ito.getReachable()) { 
						okay = false;
					}

					if (!okay) {		
						System.out.println("bad t = " + t);
						thisokay = false;
					}

				}
				if (!thisokay) {		
					allokay = false;
					System.out.println("Label not okay!");
					System.out.println("From source " + v.getId());
					System.out.println("To real node " + v.getId());
					System.out.println("From label:\n");
					System.out.println(ifrom);
					System.out.println("To label:\n");
					System.out.println(VIto);
					System.out.println("Source out flow:\n");
					System.out.println(SI);
				}
			}

			// check sourceoutflow backwards
			// but only for nonactive sources
			if (this._flow.isNonActiveSource(v)) {

				boolean thisokay = true;

				for (int t = 0; t < this._settings.TimeHorizon; t++) {
					VertexInterval ito = VIto.getIntervalAt(t);

					boolean okay = true;

					if (ito.getReachable() && SI.getFlowAt(t) > 0) {
						if (!ifrom.getReachable()) {
							okay = false;
						}
					}

					if (!okay) {		
						System.out.println("bad t = " + t);
						thisokay = false;
					}

				}
				if (!thisokay) {		
					allokay = false;
					System.out.println("Label not okay!");
					System.out.println("From real node  " + v.getId());
					System.out.println("back to source node " + v.getId());
					System.out.println("From label:\n");
					System.out.println(ifrom);
					System.out.println("To label:\n");
					System.out.println(VIto);
					System.out.println("Source out flow:\n");
					System.out.println(SI);
				}

			}
		}
			
		// no need to check sinks
		// System.out.println("Not checking sinklabels ... (they don't exist without costs)");
		
		System.out.println("Done checking all labels.");
		return allokay;
	}
	
	private boolean checkAllLabelsReverse() {
		System.out.println("Checking all labels for reverse search ... ");
		// check normal links
		
		boolean allokay = true;

		for (Node from : this._network.getNodes().values()) {
			
			// don't check sinks ... they do not have labels
			// FIXME we should still check if they start the search
			if (this._settings.isSink(from)) {
				continue;
			}
			
			// active sources end the search, so might have unscanned labels
			if (this._flow.isActiveSource(from)) {
				continue;
			}
			
			VertexIntervals VIfrom = (VertexIntervals) this._labels.get(from);
			
			// check label itself
			boolean thislabelokay = true;
			for (int t = 0; t < this._lastcost; t++) {
				VertexInterval ifrom = VIfrom.getIntervalAt(t);
				if (ifrom.getReachable() && !ifrom.isScanned()) {
					thislabelokay = false;
				}
			}
			
			if (!thislabelokay) {
				allokay = false;
				System.out.println("Reachable but not scanned interval!");
				System.out.println("Node v " + from.getId());
				System.out.println(VIfrom);
			}
			
			// check holdover
			if (this._settings.useHoldover) {
				boolean holdoverokay = true;
				
				// forward holdover
				for (int t = 1; t < this._lastcost; t++) {
					VertexInterval ifrom = VIfrom.getIntervalAt(t);
					// FIXME ignores holdover capacities
					if (ifrom.getReachable() && !(VIfrom.getIntervalAt(t -1).getReachable())) {
						holdoverokay = false;
						System.out.println("bad t = " + t);
					}
				}	
				if (!holdoverokay) {
					allokay = false;
					System.out.println("Holdover forward not correct!");
					System.out.println("Node v " + from.getId());
					System.out.println(VIfrom);
				}
				
				
				// backward holdover
				holdoverokay = true;				
				for (int t = 0; t < this._lastcost - 1; t++) {
					VertexInterval ifrom = VIfrom.getIntervalAt(t);
					if (ifrom.getReachable() && !(VIfrom.getIntervalAt(t+1).getReachable())) {
						if (this._flow.getHoldover(from).getFlowAt(t) > 0) {
						  holdoverokay = false;
						  System.out.println("bad t = " + t);
						}
					}
				}	
				if (!holdoverokay) {
					allokay = false;
					System.out.println("Holdover backward not correct!");
					System.out.println("Node v " + from.getId());
					System.out.println(VIfrom);
					System.out.println("Holdover flow:\n");
					System.out.println(this._flow.getHoldover(from));
				}
			}
			
			// forward links
			for (Link edge : from.getInLinks().values()) {
				Node to = edge.getFromNode();
				VertexIntervals VIto = this._labels.get(to);
				EdgeFlowI EF = this._flow.getFlow(edge);
				int length = this._settings.getLength(edge);
				boolean thisedgeokay = true;
				for (int t = length; t < this._lastcost; t++) {
					VertexInterval ifrom = VIfrom.getIntervalAt(t);
					if (ifrom.getReachable()) {
						if (EF.getFlowAt(t - length) < this._settings.getCapacity(edge)) {
							VertexInterval ito = VIto.getIntervalAt(t - length);

							boolean okay = true;

							if (!ito.getReachable()) { 
								okay = false;
							}

							if (!okay) {		
								System.out.println("bad t = " + t);
								thisedgeokay = false;
							}
						}
					}
				}
				if (!thisedgeokay) {		
					allokay = false;
					System.out.println("Label not okay!");
					System.out.println("From node " + from.getId());
					System.out.println("To node " + to.getId());
					System.out.println("Edge " + edge.getId() + " forward");
					System.out.println("Edge from " + edge.getFromNode().getId() + " to " + edge.getToNode().getId());
					System.out.println("Edge length " + length + " edge cap " + this._settings.getCapacity(edge));					
					System.out.println("From label:\n");
					System.out.println(VIfrom);
					System.out.println("To label:\n");
					System.out.println(VIto);
					System.out.println("Edge flow:\n");
					System.out.println(EF);
				}
			}

			// backward links
			for (Link edge : from.getOutLinks().values()) {
				Node to = edge.getToNode();
				VertexIntervals VIto = this._labels.get(to);
				EdgeFlowI EF = this._flow.getFlow(edge);
				int length = this._settings.getLength(edge);
				boolean thisedgeokay = true;				
				for (int t = 0; t < this._lastcost - length; t++) {
					VertexInterval ifrom = VIfrom.getIntervalAt(t);
					if (ifrom.getReachable()) {
						if (EF.getFlowAt(t) > 0) {
							VertexInterval ito = VIto.getIntervalAt(t + length);

							boolean okay = true;

							if (!ito.getReachable()) { 
								okay = false;
							}

							if (!okay) {		
								System.out.println("bad t = " + t);
								thisedgeokay = false;
							}
						}
					}
				}

				if (!thisedgeokay) {		
					allokay = false;
					System.out.println("Label not okay!");
					System.out.println("From node " + from.getId());
					System.out.println("To node " + to.getId());
					System.out.println("Edge " + edge.getId() + " backward");
					System.out.println("Edge from " + edge.getFromNode().getId() + " to " + edge.getToNode().getId());
					System.out.println("Edge length " + length + " edge cap " + this._settings.getCapacity(edge));					
					System.out.println("From label:\n");
					System.out.println(VIfrom);
					System.out.println("To label:\n");
					System.out.println(VIto);
					System.out.println("Edge flow:\n");
					System.out.println(EF);
				}
			}
			
		}
		
		// check sources
		// TODO
		System.out.println("Not checking sources in checkAllLabelsReverse() ... not implemented yet");
/*		for (Node v : this._flow.getSources()) {
			VertexInterval ifrom = this._sourcelabels.get(v);

			// check basic attributes
			if (this._flow.isActiveSource(v) && !ifrom.getReachable()) {
				allokay = false;
				System.out.println("Active source not updated properly!");
				System.out.println("Source v " + v);
				System.out.println(ifrom);
			}

			if (ifrom.getReachable() && !ifrom.isScanned()) {
				allokay = false;
				System.out.println("Reachable but not scanned interval!");
				System.out.println("Source v " + v);
				System.out.println(ifrom);
			}

			VertexIntervals VIto = this._labels.get(v);
			SourceIntervals SI = this._flow.getSourceOutflow(v);
			
			// check forward flow out of source
			if (ifrom.getReachable()) {
				boolean thisokay = true;

				for (int t = 0; t < this._settings.TimeHorizon; t++) {
					VertexInterval ito = VIto.getIntervalAt(t);

					boolean okay = true;

					if (!ito.getReachable()) { 
						okay = false;
					}

					if (!okay) {		
						System.out.println("bad t = " + t);
						thisokay = false;
					}

				}
				if (!thisokay) {		
					allokay = false;
					System.out.println("Label not okay!");
					System.out.println("From source " + v.getId());
					System.out.println("To real node " + v.getId());
					System.out.println("From label:\n");
					System.out.println(ifrom);
					System.out.println("To label:\n");
					System.out.println(VIto);
					System.out.println("Source out flow:\n");
					System.out.println(SI);
				}
			}

			// check sourceoutflow backwards
			// but only for nonactive sources
			if (this._flow.isNonActiveSource(v)) {

				boolean thisokay = true;

				for (int t = 0; t < this._settings.TimeHorizon; t++) {
					VertexInterval ito = VIto.getIntervalAt(t);

					boolean okay = true;

					if (ito.getReachable() && SI.getFlowAt(t) > 0) {
						if (!ifrom.getReachable()) {
							okay = false;
						}
					}

					if (!okay) {		
						System.out.println("bad t = " + t);
						thisokay = false;
					}

				}
				if (!thisokay) {		
					allokay = false;
					System.out.println("Label not okay!");
					System.out.println("From real node  " + v.getId());
					System.out.println("back to source node " + v.getId());
					System.out.println("From label:\n");
					System.out.println(ifrom);
					System.out.println("To label:\n");
					System.out.println(VIto);
					System.out.println("Source out flow:\n");
					System.out.println(SI);
				}

			}
		}*/
			
		// no need to check sinks
		// System.out.println("Not checking sinklabels ... (they don't exist without costs)");
		
		System.out.println("Done checking all labels.");
		return allokay;
	}


	/**
	 * prints the Status on the console
	 *
	 */
	void printStatus() {
		StringBuilder print = new StringBuilder();
		print.append("Regular lables");
		for (Node node : this._network.getNodes().values()){
			VertexIntervals inter = this._labels.get(node);
			print.append(node.getId() + ":");
			print.append(inter.toString());
			/*int t = inter.firstPossibleTime();
			if (t == Integer.MAX_VALUE) {
				print.append(node.getId().toString() + " t: "+ "inf." +"\n");
			} else {
				print.append(node.getId().toString() + " t: "+ t +" pred: "+ inter.getIntervalAt(t).getPredecessor() + " succ: " + inter.getIntervalAt(t).getSuccessor() + "\n");				
			}*/
		}
		
		print.append("\n Source labels");
		for (Node node : this._flow.getSources()) {
			VertexInterval inter = this._sourcelabels.get(node);
			print.append(node.getId().toString() + " " + inter  +"\n");			
		}
		print.append("\n");
		
		System.out.println(print.toString());	
	}



	public String measure() {
		String result=
			"  Polls: " + this._roundpolls +
			"\n  Nonpolls: " + this._roundnonpolls +
			//"\n      Preptime (ms): "+(this._prepend-this._prepstart)+
			"\n  VertexCleanUp: " + this._vertexGain +
			"\n  " + this.Tcalc +
			"\n  " + this.Tqueuetime +
			"\n  " + this.Tnormaltime +
			"\n  " + this.Tpickintervaltime +
			"\n  " + this.Tforwardtime +
			"\n  " + this.Tbackwardtime +
			"\n  " + this.Tpropagate +
			"\n  " + this.Tsettrue +
			"\n  " + this.Temptysourcestime +
			"\n  " + this.Tupdatesinkstime + 
			"\n  " + this.Tsourcetime +
			"\n  " + this.Tsinktime +
			"\n  " + this.Tconstructroutetime + 
			"\n\n  Totalpolls: " + (this._totalpolls) +
			"\n  Totalnonpolls: " + (this._totalnonpolls) +
			"\n  Total VertexCleanUp: " + this._totalVertexGain+
		 "\n \n actual augmentation time: "+this._flow.Taug+
		 "\n Unfolding time: "+ this._flow.Tunfold+
		 "\n unlooping time: " +this._flow.Tunloop+
		 "\n removing time: " + this._flow.Topposing+ "\n \n";
		//"\n  Totalpreptime (s): "+(this._totalpreptime/1000)+
		//"\n  Totalcalctime (s): "+(this._totalcalctime/1000);
		
		// statistics for VertexIntervalls
		// min, max, avg size
		int min = Integer.MAX_VALUE;
		int max = 0;
		long sum = 0;
		for (Node node : this._network.getNodes().values()) {
			int size = this._labels.get(node).getSize();
//			// DEBUG
//			if (size > 100) {
//				System.out.println("Large node label in node: " + node);
//				System.out.println(this._labels.get(node));
//			}
			sum += size;
			min = Math.min(min, size);
			max = Math.max(max, size);
		}
		result += "\n  Size of VertexIntervalls (min/avg/max): " + min + " / " + sum / (double) this._network.getNodes().size()+ " / " + max + "\n"; 
		return result;
	}



	/**
	 *  reset status information of the algo for the next iter 
	 */
	public void startNewIter(int lastArrival) {
		// "free" some data structures
		this._labels = null;
		this._sourcelabels = null;
		
		// reset statistics
		this._vertexGain = 0;
		this._roundpolls=0;
		this._roundnonpolls = 0;
		
		this.Tcalc.newiter();
		this.Tqueuetime.newiter();
		this.Tnormaltime.newiter();
		this.Tpickintervaltime.newiter();
		this.Tforwardtime.newiter();
		this.Tbackwardtime.newiter();
		this.Temptysourcestime.newiter();
		this.Tupdatesinkstime.newiter(); 
		this.Tsourcetime.newiter();
		this.Tsinktime.newiter();
		this.Tconstructroutetime.newiter(); 
		this.Tpropagate.newiter();
		this.Tsettrue.newiter();
		this._flow.Tunfold.newiter();
		this._flow.Tunloop.newiter();
		this._flow.Topposing.newiter();
		this._flow.Taug.newiter();
		
		if (lastArrival > this._lastcost) {
			// nothing needed currently
			this._haslastcost = true;
			this._lastcost = lastArrival;
		}
		
	}
	

}