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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import javax.management.RuntimeErrorException;

import playground.dressler.Interval.EdgeFlowI;
import playground.dressler.Interval.Interval;
import playground.dressler.Interval.Pair;
import playground.dressler.Interval.SourceIntervals;
import playground.dressler.Interval.VertexInterval;
import playground.dressler.Interval.VertexIntervals;
import playground.dressler.control.FlowCalculationSettings;
import playground.dressler.network.IndexedLinkI;
import playground.dressler.network.IndexedNetworkI;
import playground.dressler.network.IndexedNodeI;
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
	final IndexedNetworkI _network;
	final private int nnodes;
	final private int nlinks;

	/**
	 * data structure to keep distance labels on nodes during and after one iteration of the shortest TimeExpandedPath Algorithm
	 */
	VertexIntervals[] _labels;
	
	/*
	 * data structure to remember when a vertex cannot be reached at all
	 * (either from the source or the sink, though only from the source is used right now)
	 * more precisely: <= unreachable(node) is unreachable.
	 * so initialize with -1 
	 */
	
	int[] _unreachable;
	
	
	/**
	 * data structure to keep one label on each source
	 */

	VertexInterval[] _sourcelabels;
	
	int[] _distforward = null;
	int[] _distreverse = null;	
	
	boolean[] _originsToIgnore = null;
	
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
	CPUTimer Tholdovertime = new CPUTimer(" +-holdover");
	CPUTimer Tforwardtime = new CPUTimer(" +-forward");
	CPUTimer Tbackwardtime = new CPUTimer(" +-backward");
	CPUTimer Tpropagate = new CPUTimer("  *-propagate");	
	CPUTimer Tsettrue = new CPUTimer("  *-settrue");
	CPUTimer TinnerQueue = new CPUTimer("  *-innerqueue");
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
		nnodes = this._network.getLargestIndexNodes() + 1;
		nlinks = this._network.getLargestIndexLinks() + 1;
		
		if (this._settings.trackUnreachableVertices) {
			if (this._unreachable == null) {
			  this._unreachable = new int[nnodes];
			}
			
			for (IndexedNodeI node : this._network.getNodes()) {
				this._unreachable[node.getIndex()] = -1;
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
		this._labels = new VertexIntervals[nnodes];
		this._sourcelabels = new VertexInterval[nnodes];
		
		for(IndexedNodeI node: this._network.getNodes()){
			VertexInterval temp1 = new VertexInterval(0,this._settings.TimeHorizon);
			VertexIntervals label = new VertexIntervals(temp1);
			_labels[node.getIndex()] = label;
			if (this._settings.isSource(node)) {
				VertexInterval temp2 = new VertexInterval(0, this._settings.TimeHorizon);
				if (this._flow.isActiveSource(node)){
					BFTask task =  new BFTask(new VirtualSource(node), temp2, false);
					task.origin = node;					
					queue.add(task);					
					temp2.setScanned(false);
					temp2.setReachable(true);				
				} else {
					temp2.setScanned(false);
					temp2.setReachable(false);
				}
				this._sourcelabels[node.getIndex()] = temp2;
			}
		}		
	}		
	
	/**
	 * refreshes all _labels and _sourcelabels, before one run of the algorithm
	 * and fill the queue
	 */
	void refreshLabelsReverse(TaskQueue queue){		
		this._labels = new VertexIntervals[nnodes];
		this._sourcelabels = new VertexInterval[nnodes];
		
		for(IndexedNodeI node: this._network.getNodes()){
			VertexInterval temp1 = new VertexInterval(0,this._settings.TimeHorizon);
			VertexIntervals label = new VertexIntervals(temp1);
			_labels[node.getIndex()] = label;
			// sources are not really special in reverse search, except that they have additional labels
			if (this._settings.isSource(node)) {
				VertexInterval temp2 = new VertexInterval(0, this._settings.TimeHorizon);
				this._sourcelabels[node.getIndex()] = temp2;
			}
			
			if (this._settings.isSink(node)) {
				BFTask task = new BFTask(new VirtualSink(node), 0, true);
				task.origin = node;
			    queue.add(task);
			}
		}		
	}		
	
	
	/**
	 * refreshes all _labels and _sourcelabels, before one run of the algorithm
	 * and fill the queue
	 */
	void refreshLabelsMixed(TaskQueue queue){		
		this._labels = new VertexIntervals[nnodes];
		this._sourcelabels = new VertexInterval[nnodes];
		
		for(IndexedNodeI node: this._network.getNodes()){
			VertexInterval temp1 = new VertexInterval(0,this._settings.TimeHorizon);
			VertexIntervals label = new VertexIntervals(temp1);
			_labels[node.getIndex()] = label;
			
			if (this._settings.isSource(node)) {
				// mark sources reachable and add them to the queue
				VertexInterval temp2 = new VertexInterval(0, this._settings.TimeHorizon);
				if (this._flow.isActiveSource(node)){
					BFTask task = new BFTask(new VirtualSource(node), temp2, false);
					task.origin = node;
					queue.add(task);
					temp2.setScanned(false);
					temp2.setReachable(true);
				} else {
					temp2.setScanned(false);
					temp2.setReachable(false);
				}
				this._sourcelabels[node.getIndex()] = temp2;
			}

			// also add sinks to the queue
			if (this._settings.isSink(node)) {
				BFTask task = new BFTask(new VirtualSink(node), 0, true);
				task.origin = node;				
			    queue.add(task);
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
		
				
		for (IndexedNodeI superSink : this._flow.getSinks()) {			
			VertexInterval superSinkLabel = this._labels[superSink.getIndex()].getFirstPossibleForward();
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


		for (IndexedNodeI superSink : this._flow.getSinks()) {			

			VertexInterval superSinkLabel = this._labels[superSink.getIndex()].getFirstPossibleForward();
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
			LinkedList<IndexedNodeI> realSinksToSendTo = new LinkedList<IndexedNodeI>();
			LinkedHashMap<IndexedNodeI, IndexedLinkI> edgesToSuperSink = new LinkedHashMap<IndexedNodeI, IndexedLinkI>();


			boolean notasupersink = false;
			for(IndexedLinkI link : superSink.getInLinks()) {
				IndexedNodeI realSink = link.getFromNode();
				// while not strictly necessary, we only want sinks and not just generic predecessors 
				if (this._settings.getLength(link) == 0) {
					VertexInterval realSinkInterval = this._labels[realSink.getIndex()].getIntervalAt(superSinkTime);
					// are we reachable and is there capacity left?
					// (the capacity does not need to be "infinite" because it will be accounted for in the bottleneck					
					if(realSinkInterval.getReachable()) {
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
				// well, we tried 
				realSinksToSendTo = new LinkedList<IndexedNodeI>();
				// Note: We used to add the supersink, instead of replacing the list with the supersink.
				realSinksToSendTo.add(superSink);
			}

			
			
			for (IndexedNodeI sinkNode : realSinksToSendTo) {
								
				//start constructing the TimeExpandedPath
				TimeExpandedPath path = new TimeExpandedPath();		

				// include the superSink, whenever we start one step too early
				if (!sinkNode.equals(superSink)) {					
					// step into the sink
					PathStep step = new StepSinkFlow(superSink, earliestArrivalTime, true);
					path.prepend(step);
					
					step = new StepEdge(edgesToSuperSink.get(sinkNode), earliestArrivalTime, earliestArrivalTime, true);
					path.prepend(step);
				} else {
					// step into the sink
					PathStep step = new StepSinkFlow(sinkNode, earliestArrivalTime, true);
					path.prepend(step);								
				}
				
				VirtualNode arrival = new VirtualNormalNode(sinkNode, earliestArrivalTime);
				LinkedList<PathStep> list = backTrackPredecessor(arrival);
				
				path.prepend(list);
				
				result.add(path);
		
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
		
		// HashSet does not seem to be deterministic! Don't use it.
		//Set<TimeExpandedPath> result = new HashSet<TimeExpandedPath>();
		LinkedList<TimeExpandedPath> result = new LinkedList<TimeExpandedPath>();

		for (IndexedNodeI source: this._flow.getSources()) {
			if (!this._flow.isActiveSource(source)) {
				// inactive source, cannot start a path here
				continue;
			}

			//VertexIntervalls tolabels = this._labels.get(to);
			VertexInterval sourceLabel = this._sourcelabels[source.getIndex()];
			if (sourceLabel == null || !sourceLabel.getReachable()) {
				// unreachable source
				continue;
			}			

			
			VirtualNode vNode = new VirtualSource(source);
			List<PathStep> list;
			list = backTrackSuccessor(vNode);
			TimeExpandedPath path = new TimeExpandedPath();
			path.append(list);
			result.add(path);
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
		
		/* We need to do this for all kinds of labels: Normal vertices and sources
		 * (which must empty sources then). 
		 */
					
		//List<VertexInterval> intersection = new ArrayList<VertexInterval>();
		 
		
		int countnormal = 0;
		// for normal nodes
		for (IndexedNodeI node : this._network.getNodes()) {
			VertexIntervals VIs = this._labels[node.getIndex()];
			if (VIs == null) {
				continue;
			}
			
			VertexInterval current = VIs.getIntervalAt(0);
			// TODO switch to iterator ...
			while(!VIs.isLast(current)){
				if (current.getPredecessor() != null && current.getSuccessor() != null) {  					 
					// TODO maybe choose the latest one for less bad interactions?					

					VirtualNode vNode = new VirtualNormalNode(node, current.getLowBound());
					//VirtualNode vNode = new VirtualNormalNode(node, current.getHighBound() - 1);
					
					TimeExpandedPath path = new TimeExpandedPath();
					path.prepend(backTrackPredecessor(vNode));
					path.append(backTrackSuccessor(vNode));
					result.add(path);
					
					countnormal++;
					
					// let's keep on scanning, doesn't matter much. 
					// break;
				} 
				current = VIs.getNext(current);					
			}
		}
		
		// for the sources
		int countsources = 0;
		for (IndexedNodeI node : this._network.getNodes()) {
			if (_settings.isSource(node)) {
				VertexInterval VI = this._sourcelabels[node.getIndex()];
							
				if (VI.getPredecessor() != null && VI.getSuccessor() != null) {  					 
					VirtualNode vNode = new VirtualSource(node);
					TimeExpandedPath path = new TimeExpandedPath();
					path.prepend(backTrackPredecessor(vNode));
					path.append(backTrackSuccessor(vNode));
					result.add(path);
					countsources++;
				} 							
			}
		}
		//System.out.println("Intersecting intervals: normal nodes " + countnormal + " sources: " + countsources);
		

		if (result.isEmpty()) {
			// We didn't find anything. Maybe the reverse search started to early?
			// Well, let's just do the route construction from forward search
			System.out.println("defaulting to forward paths");
			return constructRoutesForward();
		} else {
			return result;
		}
	}
	
	
	protected LinkedList<PathStep> backTrackSuccessor(VirtualNode start) {
				
		BreadCrumb bc;
		PathStep succ;
		VirtualNode vNode;
		IndexedNodeI fromNode;
		int fromTime;		
		VertexInterval fromLabel;
		
		LinkedList<PathStep> list = new LinkedList<PathStep>();
		
		vNode = start;

		while (true) { // exists on bc == null or if a sink is found			
			
			fromNode = vNode.getRealNode();
			fromTime = vNode.getRealTime();
			
			if (vNode instanceof VirtualNormalNode) {
				fromLabel = this._labels[fromNode.getIndex()].getIntervalAt(fromTime);
			} else if (vNode instanceof VirtualSource) {
				fromLabel = this._sourcelabels[fromNode.getIndex()];			
			} else if (vNode instanceof VirtualSink) {
				// If we had sink labels (for costs), we maybe could continue.
				// But here sinks are the end of the search.
				return list;
			} else {
				throw new IllegalArgumentException("Unknown kind of virtual node!");
			}
			
			bc = fromLabel.getSuccessor();
			
			if (bc == null) {
				System.out.println("Backtracking successors did not end in sink. Weird.");
				return list;
			}
			
			succ = bc.createPathStepReverse(vNode, _settings);
			
			// treat Holdover differently to avoid stepping just by +- 1 time layer
			if (succ instanceof StepHold) {
				if (true) throw new UnsupportedOperationException("Holdover not supported in reverse search right now");
				// FIXME
				VertexInterval tempi = this._labels[fromNode.getIndex()].getIntervalAt(fromTime);
				
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
			}
			
			list.addLast(succ);

			vNode = succ.getArrivalNode();
		}
	
	}
	
	protected LinkedList<PathStep> backTrackPredecessor(VirtualNode arrival) {
		//start constructing the TimeExpandedPath
		LinkedList<PathStep> list = new LinkedList<PathStep>();		

		BreadCrumb bc;
		PathStep pred;
		VirtualNode vNode;
		
		IndexedNodeI toNode; 
		int toTime;		
		VertexInterval toLabel;

		vNode = arrival;
		
		
		while (true) { // exists if a source is found (or if bc == null for other reasons)
			
			toNode =  vNode.getRealNode();
			toTime = vNode.getRealTime();
			
			if (vNode instanceof VirtualNormalNode) {
				toLabel = this._labels[toNode.getIndex()].getIntervalAt(toTime);
			} else if (vNode instanceof VirtualSource) {
				toLabel = this._sourcelabels[toNode.getIndex()];			
			} else if (vNode instanceof VirtualSink) {
				// if we had sink labels (for costs), we maybe could continue.
				// But here, sinks should never occur.
				throw new RuntimeException("Predecessors ended in Sinks! This should not happen!");
			} else {
				throw new IllegalArgumentException("Unknown kind of virtual node!");
			}
			
			bc = toLabel.getPredecessor();
			
			if (bc == null) {
				if (!(vNode instanceof VirtualSource)) {
				  System.out.println("Backtracking predecessors did not end in source. Weird.");
				}
				return list;
			}			
			
			pred = bc.createPathStepForward(vNode, _settings);
			
			// sadly, we treat Holdover differently, because it cannot be shifted properly
			if (pred instanceof StepHold) {
				// FIXME, no Holdover with Breadcrumbs yet! 
				
				// could probably recycle toLabel from last iteration ...
				VertexInterval tempi = this._labels[toNode.getIndex()].getIntervalAt(toTime);
				
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
			}
			
			list.addFirst(pred);
			
			vNode = pred.getStartNode();
		}  
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
	private ArrayList<VertexInterval> relabel(IndexedNodeI from, Interval ival, IndexedNodeI to, IndexedLinkI over, boolean original, boolean reverse, int timehorizon) {
		    this.Tpropagate.onoff();
		    
			VertexIntervals labelto = _labels[to.getIndex()];
			EdgeFlowI flowover = this._flow.getFlow(over);
			ArrayList<VertexInterval> changed;

			ArrayList<Interval> arrive;

			VertexInterval arriveProperties = new VertexInterval();
			arriveProperties.setReachable(true);				

			if (!reverse) {
				// Create predecessor. It is not shifted correctly.
				/*PathStep pred;
				if (original) {
					pred = new StepEdge(over, 0, this._settings.getLength(over), original);
				} else {
					pred = new StepEdge(over, this._settings.getLength(over), 0, original);				
				}
				arriveProperties.setPredecessor(pred);*/
				BreadCrumb pred;
				if (original) {
					pred = new BreadCrumbEdgeBackwards(over);
				} else {
					pred = new BreadCrumbEdge(over);				
				}
				arriveProperties.setPredecessor(pred);
			} else {
				BreadCrumb succ;
				if (original) {
					succ = new BreadCrumbEdge(over);
				} else {
					succ = new BreadCrumbEdgeBackwards(over);				
				}
				arriveProperties.setSuccessor(succ);
				
				
				// Create successor. It is not shifted correctly.
				/*PathStep succ;
				if (original) {
					succ = new StepEdge(over, 0, this._settings.getLength(over), original);
				} else {
					succ = new StepEdge(over, this._settings.getLength(over), 0, original);				
				}
				arriveProperties.setSuccessor(succ);*/
			}

			arrive = flowover.propagate(ival, original, reverse, timehorizon);
			this.Tpropagate.onoff();
						

			if (arrive != null && !arrive.isEmpty()) {
				this.Tsettrue.onoff();
				changed = labelto.setTrueList(arrive, arriveProperties);
				this.Tsettrue.onoff();
				
				return changed;
			}else{					
				return null;
			}					


	}

	
	
	
	TaskQueue processSourceForward(IndexedNodeI v) {
		TaskQueue queue = new SimpleTaskQueue();
		processSourceForward(v, queue);
		return queue;
	}
		
	void processSourceForward(IndexedNodeI v, TaskQueue queue) {
		// send out of source v
		// just set the regular label on v
				
		VertexInterval inter = this._sourcelabels[v.getIndex()];
		
		// already scanned or not reachable (neither should occur ...)				
		if (!inter.getReachable()) {
			System.out.println("Source " + v.getId() + " was not reachable!");
			//return null;
		}
		
		if (inter.isScanned()) {
			// don't scan again ... but for a source, this should not happen anyway
			//return null;
		}
		inter.setScanned(true);
		
		//List<BFTask> queue = new ArrayList<BFTask>();
		
		//PathStep pred = new StepSourceFlow(v, 0, true);
		BreadCrumb pred = new BreadCrumbIntoSource();
		
		//Interval i = new Interval(0, this._settings.TimeHorizon);
		//ArrayList<VertexInterval> changed = this._labels.get(v).setTrueList(i , pred, false);
		VertexInterval arrive = new VertexInterval(0, this._settings.TimeHorizon);
		arrive.setPredecessor(pred);		
		arrive.setReachable(true);
		ArrayList<VertexInterval> changed = this._labels[v.getIndex()].setTrueList(arrive);
		
		this.TinnerQueue.onoff();
		VirtualNormalNode VN = new VirtualNormalNode(v, 0);
		for(VertexInterval changedintervall : changed){
			queue.add(new BFTask(VN, changedintervall, false));
		}	
		this.TinnerQueue.onoff();
		
		//return queue;
	}
	
	
	/**
	 * Does the forward search for a normal node, picking up intervals at time t.
	 * @param v the node to process
	 * @param t the time at which oen should propagate 
	 * @return The resulting tasks and the processed interval (containing t)
	 */
	Pair<TaskQueue, Interval> processNormalNodeForward(IndexedNodeI v, int t) {
		TaskQueue queue = new SimpleTaskQueue();
		Interval i = processNormalNodeForward(v, t, queue);
		return new Pair<TaskQueue, Interval>(queue, i);
	}	
	
	Interval processNormalNodeForward(IndexedNodeI v, int t, TaskQueue queue) {
				
		Interval inter;
		
		this.Tpickintervaltime.onoff();
		if (this._settings.useImplicitVertexCleanup) {
			Pair<Boolean, Interval> todo = getUnscannedInterSetScanned(v, t, false);
			inter = todo.second;
			if (!todo.first) { // we don't have anything todo
				this._totalnonpolls++;
				this._roundnonpolls++;
				
				this.Tpickintervaltime.onoff();
				return inter;
			}
		    
		} else {
			VertexInterval temp = this._labels[v.getIndex()].getIntervalAt(t);
			
			if (!temp.getReachable() || temp.getPredecessor() == null) {
				throw new RuntimeException("Node " + v.getId() + " was not reachable or had no predecessor!");
			}
			
			if (temp.isScanned()) {
				// don't scan again ... can happen with vertex cleanup
				this._totalnonpolls++;
				this._roundnonpolls++;
				
				this.Tpickintervaltime.onoff();
				return temp;
			}
			temp.setScanned(true);
			inter = temp;
		}
		this.Tpickintervaltime.onoff();
		
		
			
		//List<BFTask> queue = new ArrayList<BFTask>();
		
		this.Tholdovertime.onoff();
		
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
		this.Tholdovertime.onoff();
		this.Tforwardtime.onoff();
		
		// visit neighbors
		// link is outgoing edge of v => forward edge
		for (IndexedLinkI link : v.getOutLinks()) {				
			IndexedNodeI w = link.getToNode();
			//this.Tpropagate.onoff(); // stopped in relabel
			ArrayList<VertexInterval> changed = relabel(v, inter, w, link, true, false, this._settings.TimeHorizon);
			if (changed == null) continue;

			this.TinnerQueue.onoff();
			VirtualNormalNode VN = new VirtualNormalNode(w, 0);
			for(VertexInterval changedinterval : changed){				
				queue.add(new BFTask(VN, changedinterval, false));				
			}
			this.TinnerQueue.onoff();			

		}
		
		this.Tforwardtime.onoff();
		this.Tbackwardtime.onoff();
		
		// link is incoming edge of v => backward edge
		for (IndexedLinkI link : v.getInLinks()) {
			IndexedNodeI w = link.getFromNode();
			
			//this.Tpropagate.onoff(); // stopped in relabel
			ArrayList<VertexInterval> changed = relabel(v, inter, w, link, false, false, this._settings.TimeHorizon);
			if (changed == null) continue;

			this.TinnerQueue.onoff();
			VirtualNormalNode VN = new VirtualNormalNode(w, 0);
			for(VertexInterval changedinterval : changed){				
				queue.add(new BFTask(VN, changedinterval, false));		
			}
			this.TinnerQueue.onoff();
		}
		
		this.Tbackwardtime.onoff();
		this.Temptysourcestime.onoff();
		
		// treat empty sources! 
		if (this._flow.isNonActiveSource(v)) {
			if (!this._sourcelabels[v.getIndex()].getReachable()) {
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
					
					VertexInterval sourcelabel = this._sourcelabels[v.getIndex()];
					
					//StepSourceFlow pred = new StepSourceFlow(v, arrive.getLowBound(), false);					
					//sourcelabel.setArrivalAttributesForward(pred);
					BreadCrumb bc = new BreadCrumbOutOfSource(arrive.getLowBound());
					sourcelabel.setArrivalAttributesForward(bc);
					
				}
			}
		}
		
		this.Temptysourcestime.onoff();
		
		return inter;
	}
	
	private ArrayList<VertexInterval> relabelHoldover(IndexedNodeI v, Interval inter,boolean original, boolean reverse,
			int timeHorizon) {
		if (true) throw new UnsupportedOperationException("Holdover is currently out of service.");
		
		VertexIntervals labelto = _labels[v.getIndex()];
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

			//arriveProperties.setPredecessor(pred);
		} else {
			// Create successor. It is not shifted correctly. Just the information holdover (and original) should be enough, though.
			
			// TODO FIXME test 
			PathStep succ;
			if (original) {
				succ = new StepHold(v, inter.getHighBound()-1, inter.getHighBound(), original);
			} else {
				succ = new StepHold(v, inter.getLowBound(), inter.getLowBound()-1, original);
			}

			//arriveProperties.setSuccessor(succ);					
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
	Pair<Boolean, Interval> getUnscannedInterSetScanned(IndexedNodeI v, int t, boolean reverse) {
		
		VertexIntervals label = this._labels[v.getIndex()];
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


	Pair<TaskQueue, Interval> processNormalNodeReverse(IndexedNodeI v, int t) {
		TaskQueue queue = new SimpleTaskQueue();
		Interval i = processNormalNodeReverse(v, t, queue);
		return new Pair<TaskQueue, Interval>(queue, i);
	}
	
	Interval processNormalNodeReverse(IndexedNodeI v, int t, TaskQueue queue) {
		
		Interval inter;

		this.Tpickintervaltime.onoff();
		if (this._settings.useImplicitVertexCleanup) {
			Pair<Boolean, Interval> todo = getUnscannedInterSetScanned(v, t, true);
			inter = todo.second;
			if (!todo.first) {
				this._totalnonpolls++;
				this._roundnonpolls++;
				//return new Pair<TaskQueue, Interval>(null, inter);
				return inter;
			}
		} else {
			VertexInterval temp = this._labels[v.getIndex()].getIntervalAt(t);

			if (!temp.getReachable() || temp.getSuccessor() == null) {
				System.out.println("Node " + v.getId() + " was not reachable or had no successor!");				
				//return new Pair<TaskQueue, Interval>(null, temp);
				return temp;
			}

			if (temp.isScanned()) {
				// don't scan again ... can happen with vertex cleanup
				this._totalnonpolls++;
				this._roundnonpolls++;
				return temp;
				//return new Pair<TaskQueue, Interval>(null, temp);
			}
			temp.setScanned(true);
			inter = temp;
		}
		this.Tpickintervaltime.onoff();
		
		
		this.Tholdovertime.onoff();
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
		this.Tholdovertime.onoff();
		
		// visit neighbors
		
		this.Tforwardtime.onoff();
		
		// link is incoming edge of v => forward edges have v as successor
		for (IndexedLinkI link : v.getInLinks()) {
			IndexedNodeI w = link.getFromNode();

			ArrayList<VertexInterval> changed = relabel(v, inter, w, link, true, true, this._settings.TimeHorizon);
			
			if (changed == null) continue;

			TinnerQueue.onoff();
			VirtualNormalNode VN = new VirtualNormalNode(w, 0);
			for(VertexInterval changedinterval : changed){
				queue.add(new BFTask(VN, changedinterval, true));
			}
			TinnerQueue.onoff();

		}
		
		this.Tforwardtime.onoff();
		this.Tbackwardtime.onoff();
		
		// link is outgoing edge of v => backward edges have v as successor
		for (IndexedLinkI link : v.getOutLinks()) {					
			IndexedNodeI w = link.getToNode();
			
			ArrayList<VertexInterval> changed = relabel(v, inter, w, link, false, true, this._settings.TimeHorizon);
			
			if (changed == null) continue;

			TinnerQueue.onoff();
			VirtualNormalNode VN = new VirtualNormalNode(w, 0);
			for(VertexInterval changedinterval : changed) {
				queue.add(new BFTask(VN, changedinterval, true));
			}					
			TinnerQueue.onoff();
			
		}
		
		this.Tbackwardtime.onoff();
		this.Temptysourcestime.onoff();
		// treat sources.
		// here it does not matter if they are active or not
		// we can always be reached from them because the links have infinite capacity
		if (this._settings.isSource(v)) {
			// we've found a source, mark it
			VertexInterval vi = this._sourcelabels[v.getIndex()];
			
			/*
			// maybe we should "leave" the source as late as possible?
			// no, that does seem to make it a lot worse!
			PathStep succ = new StepSourceFlow(v, inter.getLowBound(), true);
			//PathStep succ = new StepSourceFlow(v, inter.getHighBound() - 1, true);
			 */
			
			BreadCrumb succ = new BreadCrumbOutOfSource(inter.getLowBound());
			
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
		this.Temptysourcestime.onoff();
		
       //return new Pair<TaskQueue, Interval>(queue, inter);
		return inter;
	}
	
	TaskQueue processSinkReverse(IndexedNodeI v, int lastArrival) {
		TaskQueue queue = new SimpleTaskQueue();
		processSinkReverse(v, lastArrival, queue);
		return queue;
	}
	
	void processSinkReverse(IndexedNodeI v, int lastArrival, TaskQueue queue) {
		// we want to arrive at lastArrival
		// propagate that to the associated real node
		
		// TODO sinklabels do not exist yet, but should be used here

		// the lower part of the interval does not really matter, but it avoids a lot of polls
		// and speed things up by a factor of 10.				
		// (0, lastArrival + 1)  == good
		// (lastArrival, lastArrival + 1) == bad, do not use
		
		VertexInterval arrive = new VertexInterval(0, lastArrival + 1);
		//VertexInterval arrive = new VertexInterval(lastArrival, lastArrival + 1);
		
		//PathStep succ = new StepSinkFlow(v, lastArrival, true);
		
		BreadCrumb succ = new BreadCrumbIntoSink();
		arrive.setSuccessor(succ);
		arrive.setReachable(true);
		
		ArrayList<VertexInterval> changed = this._labels[v.getIndex()].setTrueList(arrive);
		
		//TaskQueue queue = new SimpleTaskQueue();
		
		TinnerQueue.onoff();
		VirtualNormalNode VN = new VirtualNormalNode(v, 0);
		for(VertexInterval changedintervall : changed){
			queue.add(new BFTask(VN, changedintervall, true));
		}
		TinnerQueue.onoff();
		
		//return queue;
		return;
	}
	
	TaskQueue processSourceReverse(IndexedNodeI v) {
		TaskQueue queue = new SimpleTaskQueue();
		processSourceReverse(v, queue);
		return queue;
	}
	
	void processSourceReverse(IndexedNodeI v, TaskQueue queue) {
		
		// active sources are the end of the search				
		if (this._flow.isActiveSource(v)) {
			// we have a shortest path
			_hasPath = true;			
			//return null;
			return;
		}
		
		// nonactive sources are just transit nodes, and need to scan residual edges.		
		
		VertexInterval inter = this._sourcelabels[v.getIndex()];
		
		// already scanned or not reachable (neither should occur ...)
		if (inter.isScanned() || !inter.getReachable()) {
			System.out.println("Source " + v.getId() + " was already scanned or not reachable ...");
			//return null;
			return;
		}
		inter.setScanned(true);

		ArrayList<Interval> sendBackWhen = this._flow.getSourceOutflow(v).canSendFlowBackAll(this._settings.TimeHorizon);

		// successor depends on the time, but should get adjusted

		//PathStep succ = new StepSourceFlow(v, 0, false);
		BreadCrumb succ = new BreadCrumbIntoSource();
		
		VertexInterval arriveProperties = new VertexInterval();				
		arriveProperties.setArrivalAttributesReverse(succ);
		ArrayList<VertexInterval> changed = this._labels[v.getIndex()].setTrueList(sendBackWhen , arriveProperties);

		if (changed == null) return;// null;
		
		//TaskQueue queue = new SimpleTaskQueue();

		TinnerQueue.onoff();
		VirtualNormalNode VN = new VirtualNormalNode(v, 0); // the time does not matter ...
		for(VertexInterval changedInterval : changed){
			queue.add(new BFTask(VN, changedInterval, true));			
		}
		TinnerQueue.onoff();
		return;// queue;
	}
	
	
	
	/**
	 * main bellman ford algorithm calculating a shortest TimeExpandedPath
	 * @return maybe multiple TimeExpandedPath from active source(s) to the sink if it exists
	 */
	public List<TimeExpandedPath> doCalculationsForward(int guessedArrival) {
	
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
			  if (this._settings.useBucketQueue) {
				  queue = new BucketTaskQueue(new TaskComparator());
			  } else {
				  queue = new PriorityTaskQueue(new TaskComparator());
			  }
			  break;
		  case FlowCalculationSettings.QUEUE_GUIDED:
		  case FlowCalculationSettings.QUEUE_STATIC:
		  case FlowCalculationSettings.QUEUE_SEEKER:
			  
			  // computing the distances is only needed once, because sinks always remain active
			  if (this._distforward == null) {				  
				  Dijkstra dijkstra = new Dijkstra(this._settings);
				  //compute distances to sinks
				  dijkstra.setStart(this._flow.getSinks()); 
				  this._distforward = dijkstra.calcDistances(false, true);
				  //System.out.println(this._distforward);
			  }			  
			  TaskComparatorI taskcomp;
			  if (this._settings.queueAlgo == FlowCalculationSettings.QUEUE_GUIDED) {
				 taskcomp = new TaskComparatorGuided(this._distforward);  
			  } else if (this._settings.queueAlgo == FlowCalculationSettings.QUEUE_STATIC) {
				 taskcomp = new TaskComparatorStaticGuide(this._distforward);
			  } else if (this._settings.queueAlgo == FlowCalculationSettings.QUEUE_SEEKER) {
				  taskcomp = new TaskComparatorDistanceSeeking(this._distforward, guessedArrival);
			  } else {
				  throw new RuntimeException("Unsupported Queue Algo!");
			  }
			  
			  if (this._settings.useBucketQueue) {
				  queue = new BucketTaskQueue(taskcomp);
			  } else {
				  queue = new PriorityTaskQueue(taskcomp);
			  } 
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

		this._originsToIgnore = new boolean[nnodes];		
		
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
				//System.out.println("queue empty");
				break;
			}
			
			// FIXME experimental ... ignore tasks from sources which already have a path.
			if (_settings.filterOrigins && this._originsToIgnore[task.origin.getIndex()]) {
				//System.out.println("Ignoring a task due to origin!");
				this._roundnonpolls++;
				this._totalnonpolls++;
				continue;
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
			
			IndexedNodeI v = task.node.getRealNode();
			
			if (this._settings.isSink(v)) {		
				Tsinktime.onoff();
				if (this._flow.isActiveSink(v)) {
									
					if (task.time < cutofftime) {				  
						cutofftime = task.time;	
						if (_debug > 0) {
							System.out.println("Setting new cutoff time: " + cutofftime);
						}
												
					}
					
					// do we have a shortest path?
					if (_haslastcost && task.time == _lastcost) {
						_hasPath = true;
						
						//System.out.println("Have shortest path to " + v.getId());
						//System.out.println("Have shortest path from " + task.origin);
						
						// FIXME experimental
						//System.out.println("Ignoring source " + task.origin);
						if (this._settings.filterOrigins) {
							this._originsToIgnore[task.origin.getIndex()] = true;
						}
					}					
					
				}
				Tsinktime.onoff();
			} else if (task.node instanceof VirtualSource) {
				Tsourcetime.onoff();
				// send out of source v
				
				processSourceForward(v, queue);
				Tsourcetime.onoff();
			} else if (task.node instanceof VirtualNormalNode) {
				Tnormaltime.onoff();
				if (this._settings.useVertexCleanup) { 			
					// Effectiveness with this implementation is questionable
					this._vertexGain += _labels[v.getIndex()].cleanup();
				}

				// We want to ensure that we really scan all of task.ival .
				// This does not really matter here, but in the search with cost.
				int low = task.ival.getLowBound();
				while (low < task.ival.getHighBound()) {
					Interval ret = processNormalNodeForward(v, low, queue);
					
					low = ret.getHighBound() + 1;
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
				for (IndexedNodeI node : this._network.getNodes()) {
					VertexInterval iv = this._labels[node.getIndex()].getFirstPossibleForward();
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
					if (t < this._unreachable[node.getIndex()]) {
						System.out.println("Huh, new unreachable < old unreachable " + node.getId() + " time t " + t + " old unreachable " + this._unreachable[node.getIndex()]);
					}

					this._unreachable[node.getIndex()] = t;
				}
			}
		}
		
		
		
		
		this.Tcalc.onoff();
		
		//checkAllLabels();
		
		//System.out.println("final labels: \n");
		//printStatus();
				
		Tconstructroutetime.onoff();
		List<TimeExpandedPath> TEPs = null; 
		try { 
			TEPs  = constructRoutesForward();		
		} catch (BFException e) {
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
			  if (this._settings.useBucketQueue) {
				  queue = new BucketTaskQueue(new TaskComparatorReverse(lastArrival));
			  } else {
				  queue = new PriorityTaskQueue(new TaskComparatorReverse(lastArrival));
			  }			  
			  break;
		  case FlowCalculationSettings.QUEUE_GUIDED:
		  case FlowCalculationSettings.QUEUE_STATIC:
			
			  // FIXME needs option to recalc these distances less often!
			  
			  Dijkstra dijkstra = new Dijkstra(this._settings);
			  //compute distance to sources 
							  
			  for (IndexedNodeI node : this._flow.getSources()) {
			    if (this._flow.isActiveSource(node)) {
			    	dijkstra.setStart(node);			   
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
			
			IndexedNodeI v = task.node.getRealNode();
			
			if (task.node instanceof VirtualSink) {
				Tsinktime.onoff();
				processSinkReverse(v, lastArrival, queue);
				Tsinktime.onoff();
			} else 	if (task.node instanceof VirtualSource) {
				Tsourcetime.onoff();
				processSourceReverse(v, queue); 
				Tsourcetime.onoff();
			} else if (task.node instanceof VirtualNormalNode) {
				Tnormaltime.onoff();
				if (this._settings.useVertexCleanup) { 			
					// Effectiveness with this implementation is questionable
					this._vertexGain += _labels[v.getIndex()].cleanup();
				}
				
				if (this._settings.trackUnreachableVertices) {
					if (task.ival.getHighBound() <= this._unreachable[v.getIndex()]) {
						//System.out.println("Skipping a task!");
						continue;
					}
				}
				
				int low = task.ival.getLowBound();
				while (low < task.ival.getHighBound()) {
					Interval ret = processNormalNodeReverse(v, low, queue); 
					low = ret.getHighBound() + 1;
				}
				Tnormaltime.onoff();

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
		
		Tconstructroutetime.onoff(); 
		List<TimeExpandedPath> TEPs = null; 
		try{ 
			TEPs  = constructRoutesReverse();			
		} catch (BFException e) {
			Tconstructroutetime.onoff();
			System.out.println("stop reason: " + e.getMessage());
		}
		Tconstructroutetime.onoff();
		
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
		
		
		Tqueuetime.onoff();
		//TaskComparator taskcomp = new TaskComparator();
		//Queue<BFTask> queue = new PriorityQueue<BFTask>((1), taskcomp);
		TaskQueue queue = new SimpleTaskQueue();

		//set fresh labels, initialize queue
				
		refreshLabelsMixed(queue);
		Tqueuetime.onoff();
		
		//System.out.println(this._labels);
		//System.out.println(this._sourcelabels);
		
		int cutofftimeReverse = lastArrival;
		int cutofftimeForward = this._settings.TimeHorizon;

		// TODO warmstart
		
		BFTask task;
		
		int finalPoll = Integer.MAX_VALUE / 2;
		boolean quickCutOffArmed = false;
		_hasPath = false;

		// main loop
		//int gain = 0;
		this.Tcalc.onoff();
		
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
			
			if (!quickCutOffArmed &&  _hasPath) {								
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
			
			IndexedNodeI v = task.node.getRealNode();
			
			if (task.reverse) { // do a reverse step
				if (task.node instanceof VirtualSink) {
					Tsinktime.onoff();
					processSinkReverse(v, cutofftimeReverse, queue);
					Tsinktime.onoff();
				} else 	if (task.node instanceof VirtualSource) {
					Tsourcetime.onoff();
					processSourceReverse(v, queue); 
					Tsourcetime.onoff();
				} else if (task.node instanceof VirtualNormalNode) {
					Tnormaltime.onoff();
					if (this._settings.useVertexCleanup) { 			
						// Effectiveness with this implementation is questionable
						this._vertexGain += _labels[v.getIndex()].cleanup();
					}
					
					if (this._settings.trackUnreachableVertices) {
						if (task.ival.getHighBound() <= this._unreachable[v.getIndex()]) {
							//System.out.println("Skipping a task!");
							continue;
						}
					}
					
					int low = task.ival.getLowBound();
					while (low < task.ival.getHighBound()) {
						Interval ret = processNormalNodeReverse(v, low, queue); 
						low = ret.getHighBound() + 1;
					}
					Tnormaltime.onoff();

				} else {
					throw new RuntimeException("Unsupported instance of VirtualNode in BellmanFordIntervalBased");
				}
				
			} else { // do a forward step

				if (this._settings.isSink(v)) {		
					Tsinktime.onoff();
					if (this._flow.isActiveSink(v)) {

						if (task.time < cutofftimeForward) {				  
							cutofftimeForward = task.time;	
							if (_debug > 0) {
								System.out.println("Setting new cutoff time: " + cutofftimeForward);
							}

						}

						// do we have a shortest path?
						if (_haslastcost && task.time == _lastcost) {
							_hasPath = true;

							//System.out.println("Have shortest path to " + v.getId());
							//System.out.println("Have shortest path from " + task.origin);

							// FIXME experimental
							//System.out.println("Ignoring source " + task.origin);
							if (this._settings.filterOrigins) {
								this._originsToIgnore[task.origin.getIndex()] = true;
							}
						}					

					}
					Tsinktime.onoff();
				} else if (task.node instanceof VirtualSource) {
					Tsourcetime.onoff();
					// send out of source v

					processSourceForward(v, queue);
					Tsourcetime.onoff();
				} else if (task.node instanceof VirtualNormalNode) {
					Tnormaltime.onoff();
					if (this._settings.useVertexCleanup) { 			
						// Effectiveness with this implementation is questionable
						this._vertexGain += _labels[v.getIndex()].cleanup();
					}

					// We want to ensure that we really scan all of task.ival .
					// This does not really matter here, but in the search with cost.
					int low = task.ival.getLowBound();
					while (low < task.ival.getHighBound()) {
						Interval ret = processNormalNodeForward(v, low, queue);

						low = ret.getHighBound() + 1;
					}
					Tnormaltime.onoff();
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
		
		
		Tconstructroutetime.onoff();
		List<TimeExpandedPath> TEPs = null; 
		try{ 
			TEPs  = constructRoutesMixed();
		}catch (BFException e){
			System.out.println("stop reason: " + e.getMessage());
		}
		Tconstructroutetime.onoff();
	
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
		  
		} else if (_warmstart == 2) { // rebuild shortest path tree from last interval
		  		  
		} else if (_warmstart == 3) { // rebuild shortest path tree from firstPossibleTime
						  
		}*/
		
	}

	
	@SuppressWarnings("unused")
	private boolean checkAllLabels() {
		System.out.println("Checking all labels ... ");
		// check normal links
		
		boolean allokay = true;

		for (IndexedNodeI from : this._network.getNodes()) {
			
			// don't check sinks ... they are not processed
			if (this._settings.isSink(from)) {
				continue;
			}
			
			VertexIntervals VIfrom = (VertexIntervals) this._labels[from.getIndex()];
			
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
			for (IndexedLinkI edge : from.getOutLinks()) {
				IndexedNodeI to = edge.getToNode();
				VertexIntervals VIto = this._labels[to.getIndex()];
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
			for (IndexedLinkI edge : from.getInLinks()) {
				IndexedNodeI to = edge.getFromNode();
				VertexIntervals VIto = this._labels[to.getIndex()];
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
		for (IndexedNodeI v : this._flow.getSources()) {
			VertexInterval ifrom = this._sourcelabels[v.getIndex()];

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

			VertexIntervals VIto = this._labels[v.getIndex()];
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
	
	@SuppressWarnings("unused")
	private boolean checkAllLabelsReverse() {
		System.out.println("Checking all labels for reverse search ... ");
		// check normal links
		
		boolean allokay = true;

		for (IndexedNodeI from : this._network.getNodes()) {
			
			// don't check sinks ... they do not have labels
			// FIXME we should still check if they start the search
			if (this._settings.isSink(from)) {
				continue;
			}
			
			// active sources end the search, so might have unscanned labels
			if (this._flow.isActiveSource(from)) {
				continue;
			}
			
			VertexIntervals VIfrom = (VertexIntervals) this._labels[from.getIndex()];
			
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
			for (IndexedLinkI edge : from.getInLinks()) {
				IndexedNodeI to = edge.getFromNode();
				VertexIntervals VIto = this._labels[to.getIndex()];
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
			for (IndexedLinkI edge : from.getOutLinks()) {
				IndexedNodeI to = edge.getToNode();
				VertexIntervals VIto = this._labels[to.getIndex()];
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
		for (IndexedNodeI node : this._network.getNodes()){
			VertexIntervals inter = this._labels[node.getIndex()];
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
		for (IndexedNodeI node : this._flow.getSources()) {
			VertexInterval inter = this._sourcelabels[node.getIndex()];
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
			"\n  " + this.Tholdovertime +
			"\n  " + this.Tforwardtime +
			"\n  " + this.Tbackwardtime +
			"\n  " + this.Tpropagate +
			"\n  " + this.Tsettrue +
			"\n  " + this.TinnerQueue +
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
		for (IndexedNodeI node : this._network.getNodes()) {
			int size = this._labels[node.getIndex()].getSize();
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
		
		this._originsToIgnore = null;
		
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
		this.TinnerQueue.newiter();
		this.Temptysourcestime.newiter();
		this.Tupdatesinkstime.newiter();
		this.Tholdovertime.newiter();
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