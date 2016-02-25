/* *********************************************************************** *
 * project: org.matsim.*
 * QSimEngineRunner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.qsim.QSim;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Phaser;

/**
 * These are the "threads" of the {@link QNetsimEngine}. The "run()" method is implicitly called by starting the thread.
 * 
 * @author (of this documentation) nagel
 *
 */
class QNetsimEngineRunner extends NetElementActivator implements Runnable, Callable<Boolean> {

	private double time = 0.0;

	private volatile boolean simulationRunning = true;

	private final Phaser startBarrier;
	private final Phaser separationBarrier;
	private final Phaser endBarrier;

	/*
	 * This needs to be thread-safe since QNodes could be activated concurrently
	 * from multiple threads. In previous implementations, this data structure was
	 * a Map since it was possible that the same node was activated concurrently.
	 * Now, the implementation of the QNode was adapted in a way that this is not
	 * possible anymore.
	 * cdobler, sep'14
	 */
	private final Queue<QNode> nodesQueue = new ConcurrentLinkedQueue<>();

	/*
	 * Needs not to be thread-safe since links are only activated from nodes which
	 * are handled (by design) from links handled by the same thread. Therefore,
	 * no concurrent add operation can occur.
	 * cdobler, sep'14
	 */
	private final List<QLinkI> linksList = new LinkedList<>();

	/*
	 * Ensure that nodes and links are only activate during times where we expect it.
	 * Otherwise this could result in unpredictable behavior. Therefore we throw
	 * an exception then.
	 * Doing so allows us adding nodes and links directly to the nodesQueue respectively
	 * the linksList. Previously, we had to cache them in other data structures and copy
	 * them at a later point in time.
	 * cdobler, sep'14
	 */
	private boolean lockNodes = false;
	private boolean lockLinks = false;

	private boolean movingNodes;

	/*package*/ long[] runTimes;
	private long startTime = 0;
	{	
		if (QSim.analyzeRunTimes) runTimes = new long[QNetsimEngine.numObservedTimeSteps];
		else runTimes = null;
	}
	
	/*package*/ QNetsimEngineRunner(Phaser startBarrier, Phaser separationBarrier, Phaser endBarrier) {
		this.startBarrier = startBarrier;
		this.separationBarrier = separationBarrier;
		this.endBarrier = endBarrier;
	}
	QNetsimEngineRunner() {
		// this is the execution path with invokeAll and the threadpool; it does not need (and should not use) the barriers.
		// kai, jan'14
		this.startBarrier = null;
		this.separationBarrier = null;
		this.endBarrier = null;
	}

	/*package*/ void setTime(final double t) {
		time = t;
	}

	public void afterSim() {
		this.simulationRunning = false;
	}

	@Override
	public Boolean call() {
		// implementing "call" and "run" side by side because it seems the easier way to 
		// experimentally switch between the two types of threading.
		// kai, jan'14

		// Check if Simulation is still running. Otherwise print CPU usage and end thread.
		if (!this.simulationRunning) {
			Gbl.printCurrentThreadCpuTime();
			return false;
		}

		if (this.movingNodes) {
			moveNodes();
		} else {
			moveLinks();
		}
		return true ;
	}

	@Override
	public void run() {

		// The method is ended when the simulationRunning flag is set to false.
		while(true) {

			/*
			 * The threads wait at the startBarrier until they are triggered in the next 
			 * time step by the run() method in the QNetsimEngine.
			 */
			startBarrier.arriveAndAwaitAdvance();

			if (QSim.analyzeRunTimes) this.startTime = System.nanoTime();
			
			// Check if Simulation is still running. Otherwise print CPU usage and end thread.
			if (!this.simulationRunning) {
				Gbl.printCurrentThreadCpuTime();
				return;
			}

			moveNodes();

			// After moving the QNodes all we use a Phaser to synchronize the threads.
			this.separationBarrier.arriveAndAwaitAdvance();

			moveLinks();

			if (QSim.analyzeRunTimes) {
				long end = System.nanoTime();
				int bin = (int) this.time;
				if (bin < this.runTimes.length) this.runTimes[bin] = end - this.startTime;
			}
			
			/*
			 * The end of moving is synchronized with the endBarrier. If all threads 
			 * reach this barrier the main thread can go on.
			 */
			this.endBarrier.arriveAndAwaitAdvance();
		}
	}
	private void moveNodes() {
		boolean remainsActive;
		this.lockNodes = true;
		QNode node;
		Iterator<QNode> simNodes = this.nodesQueue.iterator();
		while (simNodes.hasNext()) {
			node = simNodes.next();
			remainsActive = node.doSimStep(time);
			if (!remainsActive) simNodes.remove();
		}
		this.lockNodes = false;
	}
	private void moveLinks() {
		boolean remainsActive;
		lockLinks = true;
		QLinkI link;
		ListIterator<QLinkI> simLinks = this.linksList.listIterator();
		while (simLinks.hasNext()) {
			link = simLinks.next();

			remainsActive = link.doSimStep(time);

			if (!remainsActive) simLinks.remove();
		}
		lockLinks = false;
	}

	/*
	 * This method is only called while links are NOT "moved", i.e. their
	 * doStimStep(...) methods are called. To ensure that, we  use a boolean lock.
	 * cdobler, sep'14
	 */
	@Override
	protected void activateLink(QLinkI link) {
		if (!lockLinks) linksList.add(link);
		else throw new RuntimeException("Tried to activate a QLink at a time where this was not allowed. Aborting!");
	}

	@Override
	public int getNumberOfSimulatedLinks() {
		return this.linksList.size();
	}

	/*
	 * This method is only called while nodes are NOT "moved", i.e. their
	 * doStimStep(...) methods are called. To ensure that, we  use a boolean lock.
	 * cdobler, sep'14
	 */
	@Override
	protected void activateNode(QNode node) {
		if (!this.lockNodes) this.nodesQueue.add(node);
		else throw new RuntimeException("Tried to activate a QNode at a time where this was not allowed. Aborting!");
	}

	/*
	 * Note that the size() method is O(n) for a ConcurrentLinkedQueue as used
	 * for the nodesQueue. However, this method is only called once every simulated
	 * hour for the log message. Therefore, it should be okay.
	 * cdobler, sep'14
	 */
	@Override
	public int getNumberOfSimulatedNodes() {
		return this.nodesQueue.size();
	}

	public void setMovingNodes(boolean movingNodes) {
		this.movingNodes = movingNodes;
	}
}