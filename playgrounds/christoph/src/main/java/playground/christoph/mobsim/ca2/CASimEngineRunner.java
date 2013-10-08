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

package playground.christoph.mobsim.ca2;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;

public class CASimEngineRunner extends CASimEngine implements Runnable {

	private double time = 0.0;
	private volatile boolean simulationRunning = true;
	
	private final CyclicBarrier startBarrier;
	private final CyclicBarrier separationBarrier;
	private final CyclicBarrier endBarrier;
	private final CASimEngine multiModalSimEngine;
	
	/*package*/ CASimEngineRunner(CyclicBarrier startBarrier, CyclicBarrier separationBarrier, 
			CyclicBarrier endBarrier, Netsim sim, double spatialResolution, 
			CASimEngine multiModalSimEngine) {
		super(sim, spatialResolution);
		this.startBarrier = startBarrier;
		this.separationBarrier = separationBarrier;
		this.endBarrier = endBarrier;
		this.multiModalSimEngine = multiModalSimEngine;
	}

	/*
	 * Changed behavior:
	 * Nothing to do here - everything is handled by the ParallelMultiModalSimEngine.
	 * The QSim calls this method in the ParallelMultiModalSimEngine which does all
	 * necessary actions.
	 * This method should never be called.
	 */
	@Override
	public void onPrepareSim() {
		throw new RuntimeException("This method should never be called - calls should go to the ParallelMultiModalSimEngine.");
	}
			
	/*
	 * Changed behavior here:
	 * Only the current Time is set. Afterwards the ParallelMultiModalSimEngine
	 * triggers the startBarrier which then will result in calls to moveNodes and moveLinks.
	 */
	@Override
	public void doSimStep(double time) {
		this.time = time;
	}

	@Override
	public void run() {
		/*
		 * The method is ended when the simulationRunning Flag is
		 * set to false.
		 */
		while(true) {
			try {
				/*
				 * The Threads wait at the startBarrier until they are
				 * triggered in the next TimeStep by the run() method in
				 * the ParallelQSimEngine.
				 */
				startBarrier.await();
				
				/*
				 * Check if Simulation is still running.
				 * Otherwise print CPU usage and end Thread.
				 */
				if (!simulationRunning) {
					Gbl.printCurrentThreadCpuTime();
					return;
				}

				/* Move Nodes */
				moveNodes(time);

				/*
				 * After moving the Nodes all we use a CyclicBarrier to synchronize
				 * the Threads.
				 */
				this.separationBarrier.await();
				
				/* Move Links */
				moveLinks(time);

				/*
				 * The End of the Moving is synchronized with
				 * the endBarrier. If all Threads reach this Barrier
				 * the main Thread can go on.
				 */
				endBarrier.await();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} catch (BrokenBarrierException e) {
            	throw new RuntimeException(e);
            }
		}
	}
	
	/*
	 * Is called from ParallelMultiModalSimEngine - therefore no call to
	 * super.afterSim() is necessary.
	 */
	@Override
	public void afterSim() {
		this.simulationRunning = false;
	}
	
	/*
	 * Use the map from the ParallelMultiModalSimEngine. This is a read-only access,
	 * therefore this should be thread-safe.
	 */
	/*package*/ CANode getCANode(Id nodeId) {
		return multiModalSimEngine.getCANode(nodeId);
	}

	/*
	 * Use the map from the ParallelMultiModalSimEngine. This is a read-only access,
	 * therefore this should be thread-safe.
	 */
	/*package*/ CALink getCALink(Id linkId) {
		return multiModalSimEngine.getCALink(linkId);
	}
}
