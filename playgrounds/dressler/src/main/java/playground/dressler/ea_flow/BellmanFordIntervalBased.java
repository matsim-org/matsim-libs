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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkLayer;

import playground.dressler.Interval.EdgeIntervals;
import playground.dressler.Interval.Interval;
import playground.dressler.Interval.SourceIntervals;
import playground.dressler.Interval.VertexInterval;
import playground.dressler.Interval.VertexIntervals;
import playground.dressler.ea_flow.FlowCalculationSettings;


/**
 * Implementation of the Moore-Bellman-Ford Algorithm for a static network! i =
 * 1 .. n for all e = (v,w) if l(w) > l(v) + c(e) then l(w) = l(v) + c(e), p(w) =
 * v.
 * @author Manuel Schneider
 */


public class BellmanFordIntervalBased {
		
	/**
	 * data structure to hold the present flow
	 */
	private Flow _flow;
 
	/**
	 * the calculation settings, providing most of the information
	 */
	private FlowCalculationSettings _settings;
	
	/**
	 * The network on which we find routes. We expect the network not to change
	 * between runs!
	 * This is simply for quick access.
	 */
	private final NetworkLayer _network;

	/**
	 * data structure to keep distance labels on nodes during and after one iteration of the shortest TimeExpandedPath Algorithm
	 */
	HashMap<Node, VertexIntervals> _labels;
	
	/**
	 * data structure to keep one label on each source
	 */

	HashMap<Node, VertexInterval> _sourcelabels;
	
	//private static int _warmstart;
	//private LinkedList<Node> _warmstartlist;
	
	/**
	 * debug variable, the higher the value the more it tells
	 */
	private static int _debug = 0;

	private long _totalpolls = 0L;
	
	private int _roundpolls = 0;
	
	public long vertexGain = 0L; 
	
	/*private long _prepstart=0;
	private long _prepend=0;
	private long _totalpreptime=0;*/
	
	private long _calcstart=0;
	private long _calcend=0;
	private long _totalcalctime=0;
	
	//--------------------CONSTRUCTORS-------------------------------------//
	
	/**
	 * Constructor using all the data initialized in the Flow object use recommended
	 * @param flow 
	 */
	public BellmanFordIntervalBased(FlowCalculationSettings settings, Flow flow) {
		this._settings = settings;
		this._flow = flow;
		this._network = settings.getNetwork();
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
	
	private class BFTask  {		
		public int time;
		public Interval ival;
		public VirtualNode node;
		public boolean reverse; 
		
		BFTask(VirtualNode node, Interval ival, boolean rev){
			this.time = ival.getLowBound();
			this.ival = ival;
			this.node = node;
			this.reverse = rev;
		}
		
		BFTask(VirtualNode node, int time, boolean rev){
			this.time = time;
			this.node = node; 			
			this.ival = null;
			this.reverse = rev;
		}
		
		Boolean equals(BFTask other){
			// this ignores ival!
			return(this.time == other.time 
					&& this.reverse == other.reverse
					&& this.ival.equals(other.ival)
					&& other.node.equals(this.node));
		}
		
		@Override
		public String toString(){
			return node.toString() + " @ " + time;
		}
	}
	
	// Comparator needs total order!!!
	private class TaskComparator implements Comparator<BFTask> {
		public int compare(BFTask first, BFTask second) {
			if (first.time < second.time) return -1; 
			if (first.time > second.time) return 1;

			// Important! PriorityQueue assumes that compare = 0 implies the same object ...
			// The following is just to make that work ...
			if (first.ival == null && second.ival != null) return -1;
			if (first.ival != null && second.ival == null) return 1;

			if (first.ival.getLowBound() < second.ival.getLowBound()) return -1;
			if (first.ival.getLowBound() > second.ival.getLowBound()) return 1;
			
			if (first.ival.getHighBound() < second.ival.getHighBound()) return -1;
			if (first.ival.getHighBound() > second.ival.getHighBound()) return 1;
			
			if (first.node.priority() < second.node.priority()) return -1;
			if (first.node.priority() > second.node.priority()) return 1;
			
			// false < true
			if (!first.reverse && second.reverse) return -1;
			if (first.reverse && !second.reverse) return 1;

			return first.node.getRealNode().getId().compareTo(second.node.getRealNode().getId());

		}
	}
		
	
	/**
	 * refreshes all _labels and _sourcelabels before one run of the algorithm
	 */
	private void refreshLabels(Queue<BFTask> queue){		
		this._labels = new HashMap<Node, VertexIntervals>();
		this._sourcelabels = new HashMap<Node, VertexInterval>();
		
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
	 */
	private void refreshLabelsReverse(Queue<BFTask> queue){		
		this._labels = new HashMap<Node, VertexIntervals>();
		this._sourcelabels = new HashMap<Node, VertexInterval>();
		
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
	
	private void refreshLabelsMixed(Queue<BFTask> queue){
	  // TODO stub!	  
	}
		
	/**
	 * Constructs  a TimeExpandedPath based on the labels set by the algorithm 
	 * @return shortest TimeExpandedPath from one active source to the sink if it exists
	 */
	private Collection<TimeExpandedPath> constructRoutes() throws BFException {
		
		if (_debug > 0) {
		  System.out.println("Constructing routes ...");
		}

		// HashSet does not seem to be deterministic! Don't use it. 
		//Set<TimeExpandedPath> result = new HashSet<TimeExpandedPath>();
		LinkedList<TimeExpandedPath> result = new LinkedList<TimeExpandedPath>();
		
		int earliestArrivalTime = Integer.MAX_VALUE;
		
				
		for (Node superSink : this._flow.getSinks()) {
			VertexInterval superSinkLabel = this._labels.get(superSink).getFirstPossible();
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
			VertexInterval superSinkLabel = this._labels.get(superSink).getFirstPossible();
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
				//System.out.println("sinkNode: " + sinkNode.getId());
				
				Node toNode = sinkNode;
				VertexInterval toLabel = this._labels.get(toNode).getFirstPossible();
				

				// should not happen
				/*if (toLabel == null) {
					throw new BFException("Sink cannot be reached at all!");
				}*/
				
				// exactly when we want to arrives
				int toTime = earliestArrivalTime;		


				//start constructing the TimeExpandedPath
				TimeExpandedPath TEP = new TimeExpandedPath();		

				PathStep pred;

				// include the superSink, whenever we start one step too early
				if (!toNode.equals(superSink)) {				
					pred = new StepEdge(edgesToSuperSink.get(toNode), toTime, toTime, true);
					TEP.append(pred);
				}
				
				// step into the sink
				pred = new StepSinkFlow(superSink, toTime, true);
				TEP.append(pred);


				pred = toLabel.getPredecessor();

				while (pred != null) {
					pred = pred.copyShiftedToArrival(toTime);

					TEP.prepend(pred);			

					toNode = pred.getStartNode().getRealNode();
					toTime = pred.getStartTime();
					//TEP.setStartTime(toTime); // really startTime

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

				//System.out.println("Adding " + TEP);
				result.add(TEP);
			}
		}
		//System.out.println(result);
		return result;
	}
	
	
	/**
	 * Constructs  a TimeExpandedPath based on the labels set by the algorithm 
	 * @return shortest TimeExpandedPath from one active source to the sink if it exists
	 */
	private Collection<TimeExpandedPath> constructRoutesReverse() throws BFException {
		
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
			
			int fromTime = 0;

			Node fromNode = source;
			VertexInterval fromLabel = sourceLabel;

			PathStep succ;

			succ = fromLabel.getSuccessor();

			boolean reachedsink = false;
			while (succ != null) {				
				succ = succ.copyShiftedToStart(fromTime);
				//System.out.println("succ: " + succ);
				TEP.append(succ);

				fromNode = succ.getArrivalNode().getRealNode();
				fromTime = succ.getArrivalTime();

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
	 * method for updating the labels of Node to during one iteration of the algorithm
	 * @param from a Task with a VertexInterval
	 * @param to Node to which we want to go 
	 * @param over Link upon which we travel
	 * @param original indicates, weather we use an original or residual edge
	 * @param reverse Is this for the reverse search?
	 * @return null or the list of labels that have changed
	 */
	private ArrayList<VertexInterval> relabel(Node from, Interval ival, Node to, Link over, boolean original, boolean reverse, int timehorizon) {		
			VertexIntervals labelto = _labels.get(to);
			EdgeIntervals flowover = this._flow.getFlow(over);
			ArrayList<VertexInterval> changed;
						
			// just to make sure that the VertexInterval is still good
			// it might even pick up a larger interval (or smaller if something weird happened)
			VertexInterval start = _labels.get(from).getIntervalAt(ival.getLowBound());
			
			// we don't check if this is scanned because scanned gets set before relabel is called!
			// but checking reachable won't hurt
			if(start.getReachable()){
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
				

				
				arrive = flowover.propagate(start, this._settings.getCapacity(over),original, reverse, timehorizon);
				

				if(arrive != null && !arrive.isEmpty()){
					changed = labelto.setTrueList(arrive, arriveProperties);
					return changed;
				}else{					
					return null;
				}					
			} else {
				System.out.println("Weird. Relabel called for unreachable interval!");
				return null;
			}
			

	}

	
	protected List<BFTask> processSourceForward(Node v) {
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
		
		List<BFTask> queue = new ArrayList<BFTask>();
		
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
	
	protected List<BFTask> processNormalNodeForward(Node v, int t) {
		
		VertexInterval inter = this._labels.get(v).getIntervalAt(t);
		
		if (!inter.getReachable()) {
			System.out.println("Node " + v.getId() + " was not reachable!");
			return null;
		}
		
		if (inter.isScanned()) {
			// don't scan again ... can happen with vertex cleanup
			return null;
		}
		inter.setScanned(true);
		
		List<BFTask> queue = new ArrayList<BFTask>();
		
		// visit neighbors
		// link is outgoing edge of v => forward edge
		for (Link link : v.getOutLinks().values()) {				
			Node w = link.getToNode();
			ArrayList<VertexInterval> changed = relabel(v, inter, w, link, true, false, this._settings.TimeHorizon);
			if (changed == null) continue;
			//if (!this._settings.isSink(w)) {
				for(VertexInterval changedinterval : changed){
					queue.add(new BFTask(new VirtualNormalNode(w, 0), changedinterval, false));
				}					
			//}
		}
		// link is incoming edge of v => backward edge
		for (Link link : v.getInLinks().values()) {
			Node w = link.getFromNode();
			ArrayList<VertexInterval> changed = relabel(v, inter, w, link, false, false, this._settings.TimeHorizon);
			if (changed == null) continue;

			//if (!this._settings.isSink(w)) {
				for(VertexInterval changedinterval : changed){
					queue.add(new BFTask(new VirtualNormalNode(w, 0), changedinterval, false));
				}
			//}
		}
		
		// treat empty sources! 
		if (this._flow.isNonActiveSource(v)) {
			if (!this._sourcelabels.get(v).getReachable()) {
				// we might have to do something ...
				// check if we can reverse flow
				SourceIntervals si = this._flow.getSourceOutflow(v);
				Interval arrive = si.canSendFlowBack(inter);
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
		
		return queue;
	}
	
	protected List<BFTask> processNormalNodeReverse(Node v, int t) {
		// set scanned
		// As long as no cleanup happens in between, this should be safe.
		
		VertexInterval inter = this._labels.get(v).getIntervalAt(t);
		
		if (!inter.getReachable()) {
			System.out.println("Node " + v.getId() + " was not reachable!");
			return null;
		}
		
		if (inter.isScanned()) {
			// don't scan again ... can happen with vertex cleanup
			return null;
		}
						
		inter.setScanned(true);
		
		List<BFTask> queue = new ArrayList<BFTask>();
		
		// visit neighbors
		// link is outgoing edge of v => backward edges have v as successor
		for (Link link : v.getOutLinks().values()) {					
			Node w = link.getToNode();
			
			ArrayList<VertexInterval> changed = relabel(v, inter, w, link, false, true, this._settings.TimeHorizon);
			
			if (changed == null) continue;
			
			for(VertexInterval changedinterval : changed){
				queue.add(new BFTask(new VirtualNormalNode(w, 0), changedinterval, true));
			}					
			
		}
		
		// link is incoming edge of v => forward edges have v as successor
		for (Link link : v.getInLinks().values()) {
			Node w = link.getFromNode();

			ArrayList<VertexInterval> changed = relabel(v, inter, w, link, true, true, this._settings.TimeHorizon);
			
			if (changed == null) continue;

			for(VertexInterval changedinterval : changed){
				queue.add(new BFTask(new VirtualNormalNode(w, 0), changedinterval, true));
			}

		}

		// treat sources.
		// here it does not matter if they are active or not
		// we can always be reached from them because the links have infinite capacity
		if (this._settings.isSource(v)) {
			// we've found a source, mark it
			VertexInterval vi = this._sourcelabels.get(v);
			if (!vi.getReachable()) {
				PathStep succ = new StepSourceFlow(v, inter.getLowBound(), true);
				vi.setArrivalAttributesReverse(succ);
				// well, if it's an active source, this will not do anything
				// we do it anyway, it doesn't really hurt.
				queue.add(new BFTask(new VirtualSource(v), 0, true));
			}
		}			
		
       return queue;	
	}
	
	protected List<BFTask> processSinkReverse(Node v, int lastArrival) {
		// we want to arrive at lastArrival
		// propagate that to the associated real node
		
		// TODO sinklabels do not exist yet, but should be used here

		// the lower part of the interval does not really matter, but it avoids a lot of polls
		// and speed things up by a factor of 10.				
		// (0, lastArrival + 1)  == good
		// (lastArrival, lastArrival + 1) == bad, do not use
		
		VertexInterval arrive = new VertexInterval(0, lastArrival + 1);
		PathStep succ = new StepSinkFlow(v, lastArrival, true);
		arrive.setSuccessor(succ);
		arrive.setReachable(true);
		
		ArrayList<VertexInterval> changed = this._labels.get(v).setTrueList(arrive);
		
		List<BFTask> queue = new ArrayList<BFTask>();
		
		for(VertexInterval changedintervall : changed){
			queue.add(new BFTask(new VirtualNormalNode(v, 0), changedintervall, true));
		}
		
		return queue;
	}
	
	protected List<BFTask> processSourceReverse(Node v, int lastArrival) {
		// active sources are the end of the search
		// nonactive sources are just transit nodes, and need to scan residual edges.
		if (!this._flow.isNonActiveSource(v)) {
			return null;
		}

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
		
		List<BFTask> queue = new ArrayList<BFTask>();

		for(VertexInterval changedintervall : changed){
			queue.add(new BFTask(new VirtualNormalNode(v, 0), changedintervall, true));
		}
		return queue;
	}
	
	
	
	/**
	 * main bellman ford algorithm calculating a shortest TimeExpandedPath
	 * @return maybe multiple TimeExpandedPath from active source(s) to the sink if it exists
	 */
	public Collection<TimeExpandedPath> doCalculationsForward() {
		if (_debug > 0) {
		  System.out.println("Running BellmanFord in Forward mode.");
		} else {
			System.out.print(">");
		}
		
		// queue to save nodes we have to scan
		//TaskComparator taskcomp = new TaskComparator();
		//Queue<BFTask> queue = new PriorityQueue<BFTask>((1), taskcomp);
		// DEBUG! BFS instead of Priority Queue
		Queue<BFTask> queue = new LinkedList<BFTask>();

		//set fresh labels, initialize queue
		refreshLabels(queue);
		
		//System.out.println(this._labels);
		//System.out.println(this._sourcelabels);
		
		// where the network should be empty
		// this is decreased whenever a sink is found
		int cutofftime = this._settings.TimeHorizon;

		// TODO warmstart
		
		BFTask task;

		// main loop
		int gain = 0;
		this._roundpolls=0;
		this._calcstart=System.currentTimeMillis();
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
				
			if (task.time > cutofftime) {
				//System.out.println("Beyond cut-off time, stopping.");
				//break;
				
				// TODO CHANGE ME BACK TO PRIORITY QUEUE WHEN NEEDED!
				
				//System.out.println("Ignoring too late task in BFS!");
				continue;

			}
			
			Node v = task.node.getRealNode();
			
			if (this._settings.isSink(v)) {
				// keep scanning until strictly later to give more sinks a chance to be found!
				// despite the priority queue, this could be called multiple times:
				// all current intervalls could be at cutofftime, but still have a 
				// residual edge to scan, which will lead to an earlier discovery of the sink
				if (task.time < cutofftime) {				  
				  cutofftime = task.time;	
				  if (_debug > 0) {
				    System.out.println("Setting new cutoff time: " + cutofftime);
				  }
				}				
			} else if (task.node instanceof VirtualSource) {
				// send out of source v
				queue.addAll(processSourceForward(v));
				
			} else if (task.node instanceof VirtualNormalNode) {
				
				if (this._settings.useVertexCleanup) { 			
					// Effectiveness with this implementation is questionable
					this.vertexGain += _labels.get(v).cleanup();
				}

				queue.addAll(processNormalNodeForward(v, task.ival.getLowBound()));
				
			} else {
				throw new RuntimeException("Unsupported instance of VirtualNode in BellmanFordIntervalBased");
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
		
		//System.out.println("final labels: \n");
		//printStatus();
		
		Collection<TimeExpandedPath> TEPs = null; 
		try{ 
			TEPs  = constructRoutes();		
		}catch (BFException e){
			System.out.println("stop reason: " + e.getMessage());
		}
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
	public Collection<TimeExpandedPath> doCalculationsReverse(int lastArrival) {
		if (_debug > 0) {
			System.out.println("Running BellmanFord in Reverse mode.");
		} else {
			System.out.print("<");
		}
		
		// queue to save nodes we have to scan
		
		// PriorityQueue seems to be much slower than a regular Breadth First Search
		// at least the finding of multiple paths seems much more effective with BFS!
		
		//TaskComparator taskcomp = new TaskComparator();
		//Queue<BFTask> queue = new PriorityQueue<BFTask>((1), taskcomp);
		Queue<BFTask> queue = new LinkedList<BFTask>();

		//set fresh labels, initialize queue
		refreshLabelsReverse(queue);
		
		//System.out.println(this._labels);
		//System.out.println(this._sourcelabels);
		
		int cutofftime = lastArrival;

		// TODO warmstart
		
		BFTask task;

		// main loop
		//int gain = 0;
		this._roundpolls=0;
		this._calcstart=System.currentTimeMillis();
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
				
			if (task.time > cutofftime) {
				// the algorithm should never get here?
				System.out.println("Beyond cut-off time, ignoring.");
				continue;
			}
			
			Node v = task.node.getRealNode();
			
			if (task.node instanceof VirtualSink) {
				
				queue.addAll(processSinkReverse(v, lastArrival));
				
			} else 	if (task.node instanceof VirtualSource) {
				
				queue.addAll(processSourceForward(v));
				
			} else if (task.node instanceof VirtualNormalNode) {
				
				if (this._settings.useVertexCleanup) { 			
					// Effectiveness with this implementation is questionable
					this.vertexGain += _labels.get(v).cleanup();
				}

				processNormalNodeReverse(v, task.ival.getLowBound());

			} else {
				throw new RuntimeException("Unsupported instance of VirtualNode in BellmanFordIntervalBased");
			}
						
			
			if(_debug>3){
				printStatus();
			}
		}
		this._calcend= System.currentTimeMillis();
		this._totalcalctime+=(this._calcend-this._calcstart);
		if (_debug>3) {
		  System.out.println("Removed " + this.vertexGain + " intervals.");
		}
		
		//System.out.println("final labels: \n");
		//printStatus();
		
		boolean foundsome = false; 
		Collection<TimeExpandedPath> TEPs = null; 
		try{ 
			TEPs  = constructRoutesReverse();
			foundsome = true;
		}catch (BFException e){
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
	public Collection<TimeExpandedPath> doCalculationsMixed(int lastArrival) {
		if (_debug > 0) {
			System.out.println("Running BellmanFord in Mixed mode.");
		} else {
			System.out.print("<");
		}
		
		// queue to save nodes we have to scan
		
		// PriorityQueue seems to be much slower than a regular Breadth First Search
		// at least the finding of multiple paths seems much more effective with BFS!
		
		//TaskComparator taskcomp = new TaskComparator();
		//Queue<BFTask> queue = new PriorityQueue<BFTask>((1), taskcomp);
		Queue<BFTask> queue = new LinkedList<BFTask>();

		//set fresh labels, initialize queue
		
		// TODO
		refreshLabelsMixed(queue);
		
		//System.out.println(this._labels);
		//System.out.println(this._sourcelabels);
		
		int cutofftimeReverse = lastArrival;
		int cutofftimeForward = this._settings.TimeHorizon;

		// TODO warmstart
		
		BFTask task;

		// main loop
		//int gain = 0;
		this._roundpolls=0;
		this._calcstart=System.currentTimeMillis();
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
				
			if (!task.reverse && task.time > cutofftimeForward) {
				System.out.println("Beyond cut-off time, stopping.");
				break;
			}
			
			Node v = task.node.getRealNode();
			
			if (task.reverse) {
				// TODO!
				// do a reverse search step
				
			} else {
				// TODO!
			    // do a forward step
			}
			
			
			if(_debug>3){
				printStatus();
			}
		}
		this._calcend= System.currentTimeMillis();
		this._totalcalctime+=(this._calcend-this._calcstart);
		if (_debug>3) {
		  System.out.println("Removed " + this.vertexGain + " intervals.");
		}
		
		//System.out.println("final labels: \n");
		//printStatus();
		
		boolean foundsome = false; 
		Collection<TimeExpandedPath> TEPs = null; 
		try{ 
			TEPs  = constructRoutesReverse();
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
	private void createwarmstartList() {/*
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



	/**
	 * prints the Status on the console
	 *
	 */
	private void printStatus() {
		StringBuilder print = new StringBuilder();
		print.append("Regular lables");
		for (Node node : this._network.getNodes().values()){
			VertexIntervals inter = this._labels.get(node);
			int t = inter.firstPossibleTime();
			if (t == Integer.MAX_VALUE) {
				print.append(node.getId().toString() + " t: "+ "inf." +"\n");
			} else {
				print.append(node.getId().toString() + " t: "+ t +" pred: "+ inter.getIntervalAt(t).getPredecessor() + " succ: " + inter.getIntervalAt(t).getSuccessor() + "\n");				
			}
		}
		
		print.append("Source labels");
		for (Node node : this._flow.getSources()) {
			VertexInterval inter = this._sourcelabels.get(node);
			print.append(node.getId().toString() + " " + inter  +"\n");			
		}
		print.append("\n");
		System.out.println(print.toString());	
	}



	public String measure() {
		String result=
		"  Polls: "+this._roundpolls+
		//"\n      Preptime (ms): "+(this._prepend-this._prepstart)+
		"\n  Calctime (ms): "+(this._calcend-this._calcstart)+
		"\n  Totalpolls: "+(this._totalpolls)+
		//"\n  Totalpreptime (s): "+(this._totalpreptime/1000)+
		"\n  Totalcalctime (s): "+(this._totalcalctime/1000);
		
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
		result += "\n  Size of VertexIntervalls (min/avg/max): " + min + " / " + sum / (double) this._network.getNodes().size() + " / " + max + "\n"; 
		return result;
	}



	/* reset status information of the algo for the next iter */
	public void startNewIter() {
		this.vertexGain = 0;
	}
	

}