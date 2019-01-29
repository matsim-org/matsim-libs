/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalSimEngineRunner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.contrib.multimodal.simengine;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Phaser;

import org.matsim.core.gbl.Gbl;

class MultiModalSimEngineRunner extends NetworkElementActivator implements Runnable {

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
	private final Queue<MultiModalQNodeExtension> nodesQueue = new ConcurrentLinkedQueue<MultiModalQNodeExtension>();

	/*
	 * Needs not to be thread-safe since links are only activated from nodes which
	 * are handled (by design) from links handled by the same thread. Therefore,
	 * no concurrent add operation can occur.
	 * cdobler, sep'14
	 */
	private final List<MultiModalQLinkExtension> linksList = new LinkedList<MultiModalQLinkExtension>();
	
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
	
	/*package*/ MultiModalSimEngineRunner(Phaser startBarrier, Phaser separationBarrier, Phaser endBarrier) {
		this.startBarrier = startBarrier;
		this.separationBarrier = separationBarrier;
		this.endBarrier = endBarrier;
	}
			
	/*package*/ void setTime(final double t) {
		time = t;
	}

	@Override
	public void run() {
		/*
		 * The method is ended when the simulationRunning Flag is
		 * set to false.
		 */
		while(true) {
			/*
			 * The Threads wait at the startBarrier until they are
			 * triggered in the next TimeStep by the run() method in
			 * the ParallelQSimEngine.
			 */
			this.startBarrier.arriveAndAwaitAdvance();
			
			/*
			 * Check if Simulation is still running.
			 * Otherwise print CPU usage and end Thread.
			 */
			if (!simulationRunning) {
				Gbl.printCurrentThreadCpuTime();
				return;
			}
			
			boolean remainsActive;
			
			/* Move Nodes */
			this.lockNodes = true;
			
			MultiModalQNodeExtension node;
			Iterator<MultiModalQNodeExtension> simNodes = this.nodesQueue.iterator();
			while (simNodes.hasNext()) {
				node = simNodes.next();
				remainsActive = node.moveNode(time);
				if (!remainsActive) simNodes.remove();
			}		
			this.lockNodes = false;
			
			/*
			 * After moving the Nodes all we use a CyclicBarrier to synchronize
			 * the Threads.
			 */
			this.separationBarrier.arriveAndAwaitAdvance();
			
			/* Move Links */
			this.lockLinks = true;
			MultiModalQLinkExtension link;
			ListIterator<MultiModalQLinkExtension> simLinks = this.linksList.listIterator();
			while (simLinks.hasNext()) {
				link = simLinks.next();
				remainsActive = link.moveLink(time);
				if (!remainsActive) simLinks.remove();
			}			
			this.lockLinks = false;
			
			/*
			 * The End of the Moving is synchronized with
			 * the endBarrier. If all Threads reach this Barrier
			 * the main Thread can go on.
			 */
			this.endBarrier.arriveAndAwaitAdvance();
        }
	}
	
	/*
	 * Is called from ParallelMultiModalSimEngine - therefore no call to
	 * super.afterSim() is necessary.
	 */
	public void afterSim() {
		this.simulationRunning = false;
	}


	/*
	 * This method is only called while links are NOT "moved", i.e. their
	 * doStimStep(...) methods are called. To ensure that, we  use a boolean lock.
	 * cdobler, sep'14
	 */
	@Override
	protected void activateLink(MultiModalQLinkExtension link) {
		if (!lockLinks) linksList.add(link);
		else throw new RuntimeException("Tried to activate a QLink at a time where this was not allowed. Aborting!");
	}

	@Override
	/*package*/ int getNumberOfSimulatedLinks() {
		return this.linksList.size();
	}

	/*
	 * This method is only called while nodes are NOT "moved", i.e. their
	 * doStimStep(...) methods are called. To ensure that, we  use a boolean lock.
	 * cdobler, sep'14
	 */
	@Override
	protected void activateNode(MultiModalQNodeExtension node) {
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
	/*package*/ int getNumberOfSimulatedNodes() {
		return this.nodesQueue.size();
	}
}