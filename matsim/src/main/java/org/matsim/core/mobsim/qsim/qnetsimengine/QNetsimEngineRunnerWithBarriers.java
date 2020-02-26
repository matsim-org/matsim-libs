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

import java.util.concurrent.Phaser;

import org.matsim.core.gbl.Gbl;

/**
 * Split up the old {@code QNetsimEngineRunner} which was implementing
 * 2 different approaches.
 * 
 * @author droeder @ Senozon Deutschland GmbH
 */
class QNetsimEngineRunnerWithBarriers extends AbstractQNetsimEngineRunner implements Runnable {

	private volatile boolean simulationRunning = true;

	private final Phaser startBarrier;
	private final Phaser separationBarrier;
	private final Phaser endBarrier;

	/*package*/ QNetsimEngineRunnerWithBarriers(Phaser startBarrier, Phaser separationBarrier, Phaser endBarrier) {
		this.startBarrier = startBarrier;
		this.separationBarrier = separationBarrier;
		this.endBarrier = endBarrier;
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

			super.startMeasure();
			
			// Check if Simulation is still running. Otherwise print CPU usage and end thread.
			if (!this.simulationRunning) {
				Gbl.printCurrentThreadCpuTime();
				return;
			}

			moveNodes();

			// After moving the QNodes all we use a Phaser to synchronize the threads.
			this.separationBarrier.arriveAndAwaitAdvance();

			moveLinks();

			super.endMeasure();
			
			/*
			 * The end of moving is synchronized with the endBarrier. If all threads 
			 * reach this barrier the main thread can go on.
			 */
			this.endBarrier.arriveAndAwaitAdvance();
		}
	}

	@Override
	public void afterSim() {
	}
}