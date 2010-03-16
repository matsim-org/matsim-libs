/* *********************************************************************** *
 * project: org.matsim.*
 * BellmanFordIntervalBasedWithCost.java
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
import java.util.List;
import java.util.Queue;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import playground.dressler.Interval.EdgeIntervals;
import playground.dressler.Interval.Interval;
import playground.dressler.Interval.Pair;
import playground.dressler.Interval.SourceIntervals;
import playground.dressler.Interval.VertexInterval;
import playground.dressler.Interval.VertexIntervalWithCost;
import playground.dressler.Interval.VertexIntervals;
import playground.dressler.Interval.VertexIntervalsWithCost;
import playground.dressler.control.FlowCalculationSettings;
import playground.dressler.ea_flow.BellmanFordIntervalBased.BFTask;

/**
 * Implementation of the Moore-Bellman-Ford Algorithm for a static network! i =
 * 1 .. n for all e = (v,w) if l(w) > l(v) + c(e) then l(w) = l(v) + c(e), p(w)
 * = v.
 * 
 * @author Manuel Schneider
 */

public class BellmanFordIntervalBasedWithCost extends BellmanFordIntervalBased {

	/**
	 * data structure to keep one label on each sink
	 */

	HashMap<Node, VertexInterval> _sinklabels;
	
	
	// --------------------CONSTRUCTORS-------------------------------------//

	/**
	 * Constructor using all the data initialized in the Flow object use
	 * recommended
	 * 
	 * @param flow
	 */
	public BellmanFordIntervalBasedWithCost(FlowCalculationSettings settings, Flow flow) {
		super(settings, flow);
	}

	/**
	 * refreshes all _labels and _sourcelabels before one run of the algorithm
	 * and fill the queue
	 */
	@Override
	void refreshLabelsForward(Queue<BFTask> queue) {
		this._labels = new HashMap<Node, VertexIntervals>(3 * this._network.getNodes().size() / 2);
		this._sourcelabels = new HashMap<Node, VertexInterval>(3 * this._network.getNodes().size() / 2);

		for (Node node : this._network.getNodes().values()) {
			VertexIntervalWithCost temp1 = new VertexIntervalWithCost(0, this._settings.TimeHorizon);
			VertexIntervalsWithCost label = new VertexIntervalsWithCost(temp1);
			_labels.put(node, (VertexIntervals) label);
			if (this._settings.isSource(node)) {
				VertexIntervalWithCost temp2 = new VertexIntervalWithCost(0, this._settings.TimeHorizon);
				if (this._flow.isActiveSource(node)) {
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
	@Override
	void refreshLabelsReverse(Queue<BFTask> queue) {
		this._labels = new HashMap<Node, VertexIntervals>(3 * this._network.getNodes().size() / 2);
		this._sourcelabels = new HashMap<Node, VertexInterval>(3 * this._network.getNodes().size() / 2);

		for (Node node : this._network.getNodes().values()) {
			VertexIntervalWithCost temp1 = new VertexIntervalWithCost(0, this._settings.TimeHorizon);
			VertexIntervalsWithCost label = new VertexIntervalsWithCost(temp1);
			_labels.put(node, label);
			// sources are not really special in reverse search, except that
			// they have additional labels
			if (this._settings.isSource(node)) {
				VertexIntervalWithCost temp2 = new VertexIntervalWithCost(0, this._settings.TimeHorizon);
				this._sourcelabels.put(node, temp2);
			}

			if (this._settings.isSink(node)) {
				queue.add(new BFTask(new VirtualSink(node), 0, true));
			}
		}
	}

	@Override
	protected List<BFTask> processSourceForward(Node v) {
		// send out of source v
		// just set the regular label on v

		VertexIntervalWithCost inter = (VertexIntervalWithCost) this._sourcelabels.get(v);

		// already scanned or not reachable (neither should occur ...)
		if (!inter.getReachable()) {
			System.out.println("Source " + v.getId() + " was not reachable!");
			return null;
		}

		if (inter.isScanned()) {
			// don't scan again ... but for a source, this should not happen
			// anyway
			return null;
		}
		inter.setScanned(true);

		List<BFTask> queue = new ArrayList<BFTask>();

		PathStep pred = new StepSourceFlow(v, 0, true);

		// Interval i = new Interval(0, this._settings.TimeHorizon);
		// ArrayList<VertexIntervalWithCost> changed =
		// this._labels.get(v).setTrueList(i , pred, false);
		VertexIntervalWithCost arrive = new VertexIntervalWithCost(0,
				this._settings.TimeHorizon);
		arrive.setPredecessor(pred);
		arrive.setReachable(true);
		ArrayList<VertexInterval> changed = this._labels.get(v).setTrueList(arrive);
		for (VertexInterval changedintervall : changed) {
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
	Pair<List<BFTask>, Interval> processNormalNodeForward(Node v, int t) {

		Interval inter;

		if (this._settings.useImplicitVertexCleanup) {
			Pair<Boolean, Interval> todo = getUnscannedInterSetScanned(v, t, false);
			inter = todo.second;
			if (!todo.first) { // we don't have anything todo
				this._totalnonpolls++;
				this._roundnonpolls++;
				return new Pair<List<BFTask>, Interval>(null, inter);
			}
		} else {
			VertexIntervalWithCost temp = (VertexIntervalWithCost) this._labels	.get(v).getIntervalAt(t);

			if (!temp.getReachable() || temp.getPredecessor() == null) {
				System.out.println("Node " + v.getId() + " was not reachable or had no predecessor!");
				return new Pair<List<BFTask>, Interval>(null, temp);
			}
			
			if (temp.isScanned()) {
				// don't scan again ... can happen with vertex cleanup
				this._totalnonpolls++;
				this._roundnonpolls++;
				return new Pair<List<BFTask>, Interval>(null, temp);
			}
			temp.setScanned(true);
			inter = temp;
		}

		List<BFTask> queue = new ArrayList<BFTask>();

		// visit neighbors
		// link is outgoing edge of v => forward edge
		for (Link link : v.getOutLinks().values()) {
			Node w = link.getToNode();
			ArrayList<VertexInterval> changed = relabel(v, inter, w, link, true, false, this._settings.TimeHorizon);
			if (changed == null)
				continue;

			for (VertexInterval changedinterval : changed) {
				queue.add(new BFTask(new VirtualNormalNode(w, 0), changedinterval, false));
			}

		}
		// link is incoming edge of v => backward edge
		for (Link link : v.getInLinks().values()) {
			Node w = link.getFromNode();
			ArrayList<VertexInterval> changed = relabel(v, inter, w, link, false, false, this._settings.TimeHorizon);
			if (changed == null)
				continue;

			for (VertexInterval changedinterval : changed) {
				queue.add(new BFTask(new VirtualNormalNode(w, 0), changedinterval, false));
			}
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
					VertexIntervalWithCost temp = new VertexIntervalWithCost(0, this._settings.TimeHorizon);
					temp.setScanned(false);
					temp.setReachable(true);

					queue.add(new BFTask(new VirtualSource(v), temp, false));

					StepSourceFlow pred = new StepSourceFlow(v, arrive.getLowBound(), false);
					VertexIntervalWithCost sourcelabel = (VertexIntervalWithCost) this._sourcelabels.get(v);
					sourcelabel.setArrivalAttributesForward(pred);
				}
			}
		}

		return new Pair<List<BFTask>, Interval>(queue, inter);
	}

	/** Return the (usually) largest interval around t that is unscanned but reachable
	 * This also sets those intervals to scanned!
	 * @param v The node where we are looking.
	 * @param t Time that the interval should contain
	 * @param reverse Is this for the reverse search?
	 * @return a Boolean whether scanning is needed and the interval (containing t) this concerns
	 */
	@Override
	Pair<Boolean, Interval> getUnscannedInterSetScanned(Node v, int t, boolean reverse) {

		// TODO
		// For length 1 intervals, cost is not treated perfectly.
		// A better implementation would try both settings and pick the better output. 

		VertexIntervalsWithCost label = (VertexIntervalsWithCost) this._labels.get(v);
		VertexIntervalWithCost inter = (VertexIntervalWithCost) label.getIntervalAt(t);

		// safety check, should not happen
		if (!reverse) {
			if (!inter.getReachable() || inter.getPredecessor() == null) {
				System.out.println("Node " + v.getId()
						+ " was not reachable or had no predecessor!");
				return new Pair<Boolean, Interval>(false, inter);
			}
		} else {
			if (!inter.getReachable() || inter.getSuccessor() == null) {
				System.out.println("Node " + v.getId()
						+ " was not reachable or had no successor!");
				return new Pair<Boolean, Interval>(false, inter);

			}
		}

		if (inter.isScanned()) {
			// don't scan again ... can happen with vertex cleanup or this
			// method
			return new Pair<Boolean, Interval>(false, inter);
		}
		inter.setScanned(true);

		int low = inter.getLowBound();
		int high = inter.getHighBound();

		VertexIntervalWithCost tempi;
		while (low > 0) {
			tempi = (VertexIntervalWithCost) label.getIntervalAt(low - 1);
			if (tempi.getReachable()
					&& !tempi.isScanned()
					&& ((!reverse && tempi.getPredecessor() != null) 
						|| (reverse && tempi.getSuccessor() != null))) {
				if (tempi.isSameCost(inter.cost, inter.costIsRelative)) {					
					tempi.setScanned(true);
					low = tempi.getLowBound();
				} else {
					break;
				}
			} else {
				break;
			}
		}

		tempi = inter;

		while (!label.isLast(tempi)) {
			tempi = (VertexIntervalWithCost) label.getIntervalAt(high);
			if (tempi.getReachable()
					&& !tempi.isScanned()
					&& ((!reverse && tempi.getPredecessor() != null) 
						|| (reverse && tempi.getSuccessor() != null))) {
				if (tempi.isSameCost(inter.cost, inter.costIsRelative)) {
					tempi.setScanned(true);
					high = tempi.getHighBound();
				} else {
					break;
				}				
			} else {
				break;
			}
		}

		return new Pair<Boolean, Interval>(true, new Interval(low, high));
	}

	@Override
	Pair<List<BFTask>, Interval>  processNormalNodeReverse(Node v, int t) {

		Interval inter;

		if (this._settings.useImplicitVertexCleanup) {
			Pair<Boolean, Interval> todo = getUnscannedInterSetScanned(v, t, true);
			inter = todo.second;
			if (!todo.first) {
				this._totalnonpolls++;
				this._roundnonpolls++;
				return new Pair<List<BFTask>, Interval>(null, inter);
			}
		} else {
			VertexIntervalWithCost temp = (VertexIntervalWithCost) this._labels.get(v).getIntervalAt(t);

			if (!temp.getReachable() || temp.getSuccessor() == null) {
				System.out.println("Node " + v.getId() + " was not reachable or had no successor!");
				return new Pair<List<BFTask>, Interval>(null, temp);
			}

			if (temp.isScanned()) {
				// don't scan again ... can happen with vertex cleanup
				this._totalnonpolls++;
				this._roundnonpolls++;
				return new Pair<List<BFTask>, Interval>(null, temp);
			}
			temp.setScanned(true);
			inter = temp;
		}

		List<BFTask> queue = new ArrayList<BFTask>();

		// visit neighbors

		// link is incoming edge of v => forward edges have v as successor
		for (Link link : v.getInLinks().values()) {
			Node w = link.getFromNode();

			ArrayList<VertexInterval> changed = relabel(v, inter, w, link,
					true, true, this._settings.TimeHorizon);

			if (changed == null)
				continue;

			for (VertexInterval changedinterval : changed) {
				queue.add(new BFTask(new VirtualNormalNode(w, 0), changedinterval, true));
			}

		}

		// link is outgoing edge of v => backward edges have v as successor
		for (Link link : v.getOutLinks().values()) {
			Node w = link.getToNode();

			ArrayList<VertexInterval> changed = relabel(v, inter, w, link,
					false, true, this._settings.TimeHorizon);

			if (changed == null)
				continue;

			for (VertexInterval changedinterval : changed) {
				queue.add(new BFTask(new VirtualNormalNode(w, 0), changedinterval, true));
			}

		}

		// treat sources.
		// here it does not matter if they are active or not
		// we can always be reached from them because the links have infinite
		// capacity
		if (this._settings.isSource(v)) {
			// we've found a source, mark it
			VertexIntervalWithCost vi = (VertexIntervalWithCost) this._sourcelabels.get(v);

			// maybe we should "leave" the source as late as possible?
			// no, that does seem to make it a lot worse!
			PathStep succ = new StepSourceFlow(v, inter.getLowBound(), true);
			// PathStep succ = new StepSourceFlow(v, inter.getHighBound() - 1,
			// true);

			if (this._flow.isActiveSource(v)) {
				// mark it as reachable if it was unreachable

				// note: trying to arrive as late as possible hurts the
				// performance!
				/*
				 * if (!vi.getReachable() || vi.getSuccessor() == null ||
				 * vi.getSuccessor().getArrivalTime() < succ.getArrivalTime()) {
				 */

				if (!vi.getReachable()) {
					vi.setArrivalAttributesReverse(succ);

				}
			} else { // isNonActiveSource(v) == true
				if (!vi.getReachable()) {

					vi.setArrivalAttributesReverse(succ);

					// only non active sources need to propagate
					// (active sources would just get ignored anyway)
					if (this._flow.isNonActiveSource(v)) {
						queue.add(new BFTask(new VirtualSource(v), 0, true));
					}
				}
			}
		}

		return new Pair<List<BFTask>, Interval>(queue, inter);	
	}

	@Override
	protected List<BFTask> processSinkReverse(Node v, int lastArrival) {
		// we want to arrive at lastArrival
		// propagate that to the associated real node

		// TODO sinklabels do not exist yet, but should be used here

		// the lower part of the interval does not really matter, but it avoids
		// a lot of polls
		// and speed things up by a factor of 10.
		// (0, lastArrival + 1) == good
		// (lastArrival, lastArrival + 1) == bad, do not use

		VertexIntervalWithCost arrive = new VertexIntervalWithCost(0, lastArrival + 1);
		// VertexIntervalWithCost arrive = new
		// VertexIntervalWithCost(lastArrival, lastArrival + 1);

		PathStep succ = new StepSinkFlow(v, lastArrival, true);
		arrive.setSuccessor(succ);
		arrive.setReachable(true);

		ArrayList<VertexInterval> changed = this._labels.get(v).setTrueList(arrive);

		List<BFTask> queue = new ArrayList<BFTask>();

		for (VertexInterval changedintervall : changed) {
			queue.add(new BFTask(new VirtualNormalNode(v, 0), changedintervall, true));
		}

		return queue;
	}

	@Override
	protected List<BFTask> processSourceReverse(Node v) {
		// active sources are the end of the search
		// nonactive sources are just transit nodes, and need to scan residual
		// edges.
		if (!this._flow.isNonActiveSource(v)) {
			return null;
		}

		VertexIntervalWithCost inter = (VertexIntervalWithCost) this._sourcelabels.get(v);

		// already scanned or not reachable (neither should occur ...)
		if (inter.isScanned() || !inter.getReachable()) {
			System.out.println("Source " + v.getId()
					+ " was already scanned or not reachable ...");
			return null;
		}
		inter.setScanned(true);

		ArrayList<Interval> sendBackWhen = this._flow.getSourceOutflow(v)
				.canSendFlowBackAll(this._settings.TimeHorizon);

		// successor depends on the time, but should get adjusted

		PathStep succ = new StepSourceFlow(v, 0, false);
		VertexIntervalWithCost arriveProperties = new VertexIntervalWithCost();
		arriveProperties.setArrivalAttributesReverse(succ);
		ArrayList<VertexInterval> changed = this._labels.get(v).setTrueList(sendBackWhen, arriveProperties);

		if (changed == null)
			return null;

		List<BFTask> queue = new ArrayList<BFTask>();

		for (VertexInterval changedintervall : changed) {
			queue.add(new BFTask(new VirtualNormalNode(v, 0), changedintervall, true));
		}
		return queue;
	}

	/**
	 * main bellman ford algorithm calculating a shortest TimeExpandedPath
	 * 
	 * @return maybe multiple TimeExpandedPath from active source(s) to the sink
	 *         if it exists
	 */
	public List<TimeExpandedPath> doCalculationsForward() {
		if (_debug > 0) {
			System.out.println("Running BellmanFord in Forward mode with Costs.");
		} else {
			System.out.print(">");
		}

		// queue to save nodes we have to scan
		// TaskComparator taskcomp = new TaskComparator();
		// Queue<BFTask> queue = new PriorityQueue<BFTask>((1), taskcomp);
		// DEBUG! BFS instead of Priority Queue
		Queue<BFTask> queue = new LinkedList<BFTask>();

		// set fresh labels, initialize queue
		refreshLabelsForward(queue);

		// System.out.println(this._labels);
		// System.out.println(this._sourcelabels);

		// where the network should be empty
		// this is decreased whenever a sink is found
		int cutofftime = this._settings.TimeHorizon;

		BFTask task;

		// main loop
		int gain = 0;
		this._calcstart = System.currentTimeMillis();

		while (true) {
			// System.out.println("The queue is: ");
			// System.out.println(queue);

			this._roundpolls++;
			this._totalpolls++;

			// gets the first task in the queue
			task = queue.poll();
			if (task == null) {
				break;
			}

			if (task.time > cutofftime) {
				// System.out.println("Ignoring too late task in BFS!");
				continue;

			}

			Node v = task.node.getRealNode();

			if (this._settings.isSink(v)) {
				/* keep scanning until strictly later to give more sinks a
				 chance to be found!
				 despite the priority queue, this could be called multiple times:
				 all current intervalls could be at cutofftime, but still have a
				 residual edge to scan, which will lead to an earlier
				 discovery of the sink */
				if (task.time < cutofftime) {
					cutofftime = task.time;
					if (_debug > 0) {
						System.out.println("Setting new cutoff time: "
								+ cutofftime);
					}
				}
			} else if (task.node instanceof VirtualSource) {
				// send out of source v

				List<BFTask> tempqueue = processSourceForward(v);

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
					Pair<List<BFTask>, Interval> ret = processNormalNodeForward(v, low); 
					List<BFTask> tempqueue = ret.first;

					if (tempqueue != null) {
						queue.addAll(tempqueue);
					}
					low = ret.second.getHighBound() + 1;
				}

			} else {
				throw new RuntimeException(
						"Unsupported instance of VirtualNode in BellmanFordIntervalBased");
			}

			if (_debug > 3) {
				printStatus();
			}
		}

		// update the unreachable marks if this is not a pure forward search;

		if (this._settings.trackUnreachableVertices) {
			for (Node node : this._network.getNodes().values()) {
				VertexIntervalWithCost iv = (VertexIntervalWithCost) this._labels.get(node).getFirstPossibleForward();
				int t;
				if (iv != null) {
					t = iv.getLowBound() - 1; // lowbound is just reachable, -1 is not

					// the following can happen, and that part of the network
					// was not scanned fully!
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
					System.out
							.println("Huh, new unreachable < old unreachable "
									+ node.getId() + " time t " + t
									+ " old unreachable "
									+ this._unreachable.get(node));
				}

				this._unreachable.put(node, t);
			}
		}

		this._calcend = System.currentTimeMillis();
		this._totalcalctime += (this._calcend - this._calcstart);
		if (_debug > 3) {
			System.out.println("Removed " + gain + " intervals.");
		}

		// System.out.println("final labels: \n");
		// printStatus();

		// BIG DEBUG FIXME remove again
		// check if all labels have the correct type
		for (Node node : this._labels.keySet()) {
			VertexIntervalsWithCost label = (VertexIntervalsWithCost) this._labels.get(node);
			VertexIntervalWithCost i = (VertexIntervalWithCost) label.getIntervalAt(0);
			while (!label.isLast(i)) {
				i = (VertexIntervalWithCost) label.getNext(i);
			}
		}
		// END OF BIG DEBUG

		List<TimeExpandedPath> TEPs = null;
		try {
			TEPs = constructRoutesForward();
		} catch (BFException e) {
			System.out.println("stop reason: " + e.getMessage());
		}
		// System.out.println(TEPs);

		return TEPs;

	}

	/**
	 * Main algorithm calculating a shortest TimeExpandedPath this version
	 * constructs the shortest path tree starting at the sink
	 * 
	 * @param lastArrival
	 *            The time the flow reached the sink in the last iteration.
	 *            Without this, this is all pretty pointless. Also, it only
	 *            helps if the sink is still reachable at that time. Otherwise,
	 *            nothing will be found.
	 * @return maybe multiple TimeExpandedPath from active source(s) to the sink
	 *         if it exists
	 */
	public List<TimeExpandedPath> doCalculationsReverse(int lastArrival) {
		if (_debug > 0) {
			System.out
					.println("Running BellmanFord in Reverse mode with Costs.");
		} else {
			System.out.print("<");
		}

		// queue to save nodes we have to scan

		// PriorityQueue seems to be much slower than a regular Breadth First
		// Search
		// at least the finding of multiple paths seems much more effective with
		// BFS!

		// TaskComparator taskcomp = new TaskComparator();
		// Queue<BFTask> queue = new PriorityQueue<BFTask>((1), taskcomp);
		Queue<BFTask> queue = new LinkedList<BFTask>();

		// set fresh labels, initialize queue
		refreshLabelsReverse(queue);

		// System.out.println(this._labels);
		// System.out.println(this._sourcelabels);

		int cutofftime = lastArrival;

		BFTask task;

		// main loop
		// int gain = 0;

		this._calcstart = System.currentTimeMillis();
		while (true) {
			// System.out.println("The queue is: ");
			// System.out.println(queue);

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

				List<BFTask> tempqueue = processSinkReverse(v, lastArrival);

				if (tempqueue != null) {
					queue.addAll(tempqueue);
				}

			} else if (task.node instanceof VirtualSource) {

				List<BFTask> tempqueue = processSourceReverse(v);

				if (tempqueue != null) {
					queue.addAll(tempqueue);
				}

			} else if (task.node instanceof VirtualNormalNode) {

				if (this._settings.useVertexCleanup) {
					// Effectiveness with this implementation is questionable
					this._vertexGain += _labels.get(v).cleanup();
				}

				if (this._settings.trackUnreachableVertices) {
					if (task.ival.getHighBound() <= this._unreachable.get(v)) {
						// System.out.println("Skipping a task!");
						continue;
					}
				}

				int low = task.ival.getLowBound();
				while (low < task.ival.getHighBound()) {
					Pair<List<BFTask>, Interval> ret = processNormalNodeReverse(v, low); 
					List<BFTask> tempqueue = ret.first;

					if (tempqueue != null) {
						queue.addAll(tempqueue);
					}
					low = ret.second.getHighBound() + 1;
				}

			} else {
				throw new RuntimeException(
						"Unsupported instance of VirtualNode in BellmanFordIntervalBased");
			}

			if (_debug > 3) {
				printStatus();
			}
		}
		this._calcend = System.currentTimeMillis();
		this._totalcalctime += (this._calcend - this._calcstart);
		if (_debug > 3) {
			System.out.println("Removed " + this._vertexGain + " intervals.");
		}

		// System.out.println("final labels: \n");
		// printStatus();

		// BIG DEBUG FIXME remove again
		// check if all labels have the correct type
		for (Node node : this._labels.keySet()) {
			VertexIntervalsWithCost label = (VertexIntervalsWithCost) this._labels.get(node);
			VertexIntervalWithCost i = (VertexIntervalWithCost) label.getIntervalAt(0);
			while (!label.isLast(i)) {
				i = (VertexIntervalWithCost) label.getNext(i);
			}
		}
		// END OF BIG DEBUG

		List<TimeExpandedPath> TEPs = null;
		try {
			TEPs = constructRoutesReverse();
		} catch (BFException e) {
			System.out.println("stop reason: " + e.getMessage());
		}

		return TEPs;

	}

	/**
	 * Main algorithm calculating a shortest TimeExpandedPath this version
	 * constructs the shortest path tree starting at the sources and sinks
	 * simultaneously
	 * 
	 * @param lastArrival
	 *            The time the flow reached the sink in the last iteration.
	 * @return maybe multiple TimeExpandedPath from active source(s) to the sink
	 *         if it exists
	 */
	/*
	 * public List<TimeExpandedPath> doCalculationsMixed(int lastArrival) { if
	 * (_debug > 0) { System.out.println("Running BellmanFord in Mixed mode.");
	 * } else { System.out.print("*"); }
	 * 
	 * // queue to save nodes we have to scan
	 * 
	 * // PriorityQueue seems to be much slower than a regular Breadth First
	 * Search // at least the finding of multiple paths seems much more
	 * effective with BFS!
	 * 
	 * //TaskComparator taskcomp = new TaskComparator(); //Queue<BFTask> queue =
	 * new PriorityQueue<BFTask>((1), taskcomp); Queue<BFTask> queue = new
	 * LinkedList<BFTask>();
	 * 
	 * //set fresh labels, initialize queue
	 * 
	 * refreshLabelsMixed(queue);
	 * 
	 * //System.out.println(this._labels);
	 * //System.out.println(this._sourcelabels);
	 * 
	 * int cutofftimeReverse = lastArrival; int cutofftimeForward =
	 * this._settings.TimeHorizon;
	 * 
	 * BFTask task;
	 * 
	 * // main loop //int gain = 0; this._calcstart=System.currentTimeMillis();
	 * 
	 * while (true) { //System.out.println("The queue is: ");
	 * //System.out.println(queue);
	 * 
	 * this._roundpolls++; this._totalpolls++;
	 * 
	 * // gets the first task in the queue task = queue.poll(); if (task ==
	 * null) { break; }
	 * 
	 * Node v = task.node.getRealNode();
	 * 
	 * if (task.reverse) { // do a reverse step if (task.time >
	 * cutofftimeReverse) { // the algorithm should never get here?
	 * System.out.println("Beyond cut-off time, ignoring."); continue; }
	 * 
	 * 
	 * if (task.node instanceof VirtualSink) {
	 * 
	 * List<BFTask> tempqueue = processSinkReverse(v, lastArrival);
	 * 
	 * if (tempqueue != null) { queue.addAll(tempqueue); }
	 * 
	 * } else if (task.node instanceof VirtualSource) {
	 * 
	 * List<BFTask> tempqueue = processSourceReverse(v);
	 * 
	 * if (tempqueue != null) { queue.addAll(tempqueue); }
	 * 
	 * } else if (task.node instanceof VirtualNormalNode) {
	 * 
	 * if (this._settings.useVertexCleanup) { // Effectiveness with this
	 * implementation is questionable this._vertexGain +=
	 * _labels.get(v).cleanup(); }
	 * 
	 * List<BFTask> tempqueue = processNormalNodeReverse(v,
	 * task.ival.getLowBound());
	 * 
	 * if (tempqueue != null) { queue.addAll(tempqueue); }
	 * 
	 * } else { throw newRuntimeException(
	 * "Unsupported instance of VirtualNode in BellmanFordIntervalBased"); }
	 * 
	 * } else { // do a forward step if (task.time > cutofftimeForward) {
	 * //System.out.println("Ignoring too late task in BFS!"); continue;
	 * 
	 * }
	 * 
	 * if (this._settings.isSink(v)) { // keep scanning until strictly later to
	 * give more sinks a chance to be found! // despite the priority queue, this
	 * could be called multiple times: // all current intervalls could be at
	 * cutofftime, but still have a // residual edge to scan, which will lead to
	 * an earlier discovery of the sink if (task.time < cutofftimeForward) {
	 * cutofftimeForward = task.time; if (_debug > 0) {
	 * System.out.println("Setting new cutoff time: " + cutofftimeForward); } }
	 * } else if (task.node instanceof VirtualSource) { // send out of source v
	 * List<BFTask> tempqueue = processSourceForward(v);
	 * 
	 * if (tempqueue != null) { queue.addAll(tempqueue); }
	 * 
	 * } else if (task.node instanceof VirtualNormalNode) {
	 * 
	 * if (this._settings.useVertexCleanup) { // Effectiveness with this
	 * implementation is questionable this._vertexGain +=
	 * _labels.get(v).cleanup(); }
	 * 
	 * List<BFTask> tempqueue = processNormalNodeForward(v,
	 * task.ival.getLowBound());
	 * 
	 * if (tempqueue != null) { queue.addAll(tempqueue); }
	 * 
	 * } else { throw newRuntimeException(
	 * "Unsupported instance of VirtualNode in BellmanFordIntervalBased"); }
	 * 
	 * }
	 * 
	 * 
	 * if(_debug>3){ printStatus(); } } this._calcend=
	 * System.currentTimeMillis();
	 * this._totalcalctime+=(this._calcend-this._calcstart); if (_debug>3) {
	 * System.out.println("Removed " + this._vertexGain + " intervals."); }
	 * 
	 * //System.out.println("final labels: \n"); //printStatus();
	 * 
	 * 
	 * boolean foundsome = false; List<TimeExpandedPath> TEPs = null; try{ TEPs
	 * = constructRoutesMixed(); foundsome = true; }catch (BFException e){
	 * System.out.println("stop reason: " + e.getMessage()); }
	 * 
	 * //System.out.println("Gain from Cleanup: " + gain);
	 * 
	 * return TEPs; }
	 */

	/**
	 * creates a new warmstartlist, from the data of one run of the BF algorithm
	 * an sets _warmstartlist accordingly
	 */
	/*
	 * private void createwarmstartList() { // use cases of _warmstart to decide
	 * what to do if (_warmstart == 1) { // add the found path _warmstartlist =
	 * new LinkedList<Node>(); if (_timeexpandedpath != null) for
	 * (TimeExpandedPath.PathEdge edge : _timeexpandedpath.getPathEdges()) {
	 * _warmstartlist.add(edge.getEdge().getFromNode());
	 * //System.out.println(edge.getEdge().getFromNode().getId()); } } else if
	 * (_warmstart == 2) { // rebuild shortest path tree from last interval
	 * _warmstartlist = new LinkedList<Node>();
	 * 
	 * _warmstartlist.addAll(_labels.keySet());
	 * 
	 * Collections.sort(_warmstartlist, new Comparator<Node>() { public int
	 * compare(Node n1, Node n2) { int v1 =
	 * _labels.get(n1).getLast().getLowBound(); int v2 =
	 * _labels.get(n2).getLast().getLowBound(); if (v1 > v2) { return 1; } else
	 * if (v1 == v2) { return 0; } else { return -1; }
	 * 
	 * } });
	 * 
	 * for (Node node : _warmstartlist) {
	 * System.out.println(node.getId().toString() + " " +
	 * _labels.get(node).getLast().getLowBound()); }
	 * 
	 * } else if (_warmstart == 3) { // rebuild shortest path tree from
	 * firstPossibleTime _warmstartlist = new LinkedList<Node>();
	 * 
	 * _warmstartlist.addAll(_labels.keySet());
	 * 
	 * Collections.sort(_warmstartlist, new Comparator<Node>() { public int
	 * compare(Node n1, Node n2) { int v1 = _labels.get(n1).firstPossibleTime();
	 * int v2 = _labels.get(n2).firstPossibleTime(); if (v1 > v2) { return 1; }
	 * else if (v1 == v2) { return 0; } else { return -1; }
	 * 
	 * } });
	 * 
	 * for (Node node : _warmstartlist) {
	 * System.out.println(node.getId().toString() + " " +
	 * _labels.get(node).getLast().getLowBound()); }
	 * 
	 * }
	 * 
	 * }
	 */

	/**
	 * prints the Status on the console
	 * 
	 */
	void printStatus() {
		StringBuilder print = new StringBuilder();
		print.append("Regular lables");
		for (Node node : this._network.getNodes().values()) {
			VertexIntervalsWithCost inter = (VertexIntervalsWithCost) this._labels
					.get(node);
			print.append(node.getId() + ":");
			print.append(inter.toString());
			/*
			 * int t = inter.firstPossibleTime(); if (t == Integer.MAX_VALUE) {
			 * print.append(node.getId().toString() + " t: "+ "inf." +"\n"); }
			 * else { print.append(node.getId().toString() + " t: "+ t
			 * +" pred: "+ inter.getIntervalAt(t).getPredecessor() + " succ: " +
			 * inter.getIntervalAt(t).getSuccessor() + "\n"); }
			 */
		}

		print.append("Source labels");
		for (Node node : this._flow.getSources()) {
			VertexIntervalWithCost inter = (VertexIntervalWithCost) this._sourcelabels
					.get(node);
			print.append(node.getId().toString() + " " + inter + "\n");
		}
		print.append("\n");
		System.out.println(print.toString());
	}

	public String measure() {
		String result =
		// "  Calctime (ms): "+(this._calcend-this._calcstart)+
		"  Polls: " + this._roundpolls + "\n  Nonpolls: "
				+ this._roundnonpolls
				+
				// "\n      Preptime (ms): "+(this._prepend-this._prepstart)+
				"\n  Totalpolls: " + (this._totalpolls) + "\n  Totalnonpolls: "
				+ (this._totalnonpolls);
		// "\n  Totalpreptime (s): "+(this._totalpreptime/1000)+
		// "\n  Totalcalctime (s): "+(this._totalcalctime/1000);

		// statistics for VertexIntervalls
		// min, max, avg size
		int min = Integer.MAX_VALUE;
		int max = 0;
		long sum = 0;
		for (Node node : this._network.getNodes().values()) {
			int size = this._labels.get(node).getSize();
			// // DEBUG
			// if (size > 100) {
			// System.out.println("Large node label in node: " + node);
			// System.out.println(this._labels.get(node));
			// }
			sum += size;
			min = Math.min(min, size);
			max = Math.max(max, size);
		}
		result += "\n  Size of VertexIntervalls (min/avg/max): " + min + " / "
				+ sum / (double) this._flow.getSources().size() + " / " + max
				+ "\n";
		return result;
	}

	/* reset status information of the algo for the next iter */
	public void startNewIter(int lastArrival) {
		this._vertexGain = 0;
		this._roundpolls = 0;
		this._roundnonpolls = 0;
		if (lastArrival > this._oldLastArrival) {
			// reset some status information
			// nothing needed currently
		}

	}

	/**
	 * method for updating the labels of Node to during one iteration of the
	 * algorithm
	 * 
	 * @param from
	 *            a Task with a VertexInterval
	 * @param ival
	 *            an Interval during which the from vertex is reachable, no
	 *            checks performed
	 * @param to
	 *            Node to which we want to go
	 * @param over
	 *            Link upon which we travel
	 * @param original
	 *            indicates, weather we use an original or residual edge
	 * @param reverse
	 *            Is this for the reverse search?
	 * @return null or the list of labels that have changed
	 */
	private ArrayList<VertexInterval> relabel(Node from, Interval ival,
			Node to, Link over, boolean original, boolean reverse,
			int timehorizon) {
		VertexIntervals labelto = (VertexIntervalsWithCost) _labels.get(to);
		EdgeIntervals flowover = this._flow.getFlow(over);
		ArrayList<VertexInterval> changed;

		ArrayList<Interval> arrive;

		VertexIntervalWithCost arriveProperties = new VertexIntervalWithCost();
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
		} else {
			return null;
		}

	}

}