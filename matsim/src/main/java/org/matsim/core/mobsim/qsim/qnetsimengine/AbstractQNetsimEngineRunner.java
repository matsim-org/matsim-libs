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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.matsim.core.mobsim.qsim.QSim;

/**
 * These are the "threads" of the {@link QNetsimEngineWithThreadpool}. The "run()" method is implicitly called by starting the thread.
 * 
 * @author droeder after
 * @author (of this documentation) nagel
 *
 */
abstract class AbstractQNetsimEngineRunner extends NetElementActivationRegistry {

	private double time = 0.0;

	/*
	 * This needs to be thread-safe since QNodes could be activated concurrently
	 * from multiple threads. In previous implementations, this data structure was
	 * a Map since it was possible that the same node was activated concurrently.
	 * Now, the implementation of the QNode was adapted in a way that this is not
	 * possible anymore.
	 * cdobler, sep'14
	 */
	private final Queue<QNodeI> nodesQueue = new ConcurrentLinkedQueue<>();

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

	/*package*/ long[] runTimes;
	private long startTime = 0;
	{	
		if (QSim.analyzeRunTimes) runTimes = new long[QNetsimEngineWithThreadpool.numObservedTimeSteps];
		else runTimes = null;
	}

	/*package*/ final void setTime(final double t) {
		time = t;
	}

	public abstract void afterSim() ;

	protected void moveNodes() {
		boolean remainsActive;
		this.lockNodes = true;
		QNodeI node;
		Iterator<QNodeI> simNodes = this.nodesQueue.iterator();
		while (simNodes.hasNext()) {
			node = simNodes.next();
			remainsActive = node.doSimStep(time);
			if (!remainsActive) simNodes.remove();
		}
		this.lockNodes = false;
	}
	
	protected final void moveLinks() {
		boolean remainsActive;
		lockLinks = true;
		QLinkI link;
		ListIterator<QLinkI> simLinks = this.linksList.listIterator();
		while (simLinks.hasNext()) {
			link = simLinks.next();

			remainsActive = link.doSimStep();

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
	protected final void registerLinkAsActive(QLinkI link) {
		if (!lockLinks) linksList.add(link);
		else throw new RuntimeException("Tried to activate a QLink at a time where this was not allowed. Aborting!");
	}

	@Override
	public final int getNumberOfSimulatedLinks() {
		return this.linksList.size();
	}

	/*
	 * This method is only called while nodes are NOT "moved", i.e. their
	 * doStimStep(...) methods are called. To ensure that, we  use a boolean lock.
	 * cdobler, sep'14
	 */
	@Override
	protected final void registerNodeAsActive(QNodeI node) {
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
	public final int getNumberOfSimulatedNodes() {
		return this.nodesQueue.size();
	}

	protected final void startMeasure() {
		if (QSim.analyzeRunTimes) this.startTime = System.nanoTime();		
	}

	protected final void endMeasure() {
		if (QSim.analyzeRunTimes) {
			long end = System.nanoTime();
			int bin = (int) this.time;
			if (bin < this.runTimes.length) this.runTimes[bin] = end - this.startTime;
		}
	}
}