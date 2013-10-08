/* *********************************************************************** *
 * project: org.matsim.*
 * ParallelCASimEngine.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimNode;

class ParallelCASimEngine extends CASimEngine {
	
	private final int numOfThreads;
	
	private Thread[] threads;
	private CASimEngineRunner[] engines;
	private CyclicBarrier startBarrier;
	private CyclicBarrier separationBarrier;	// separates moveNodes and moveLinks
	private CyclicBarrier endBarrier;
	
	// use the factory
	/*package*/ ParallelCASimEngine(Netsim sim, double spatialResolution) {
		super(sim, spatialResolution);
		this.numOfThreads = this.getMobsim().getScenario().getConfig().qsim().getNumberOfThreads();
	}
	
	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		super.setInternalInterface(internalInterface);
		
		/*
		 * If the engines have already been created, hand the internalinterface
		 * over to them.
		 */
		if (this.engines != null) {
			for (CASimEngineRunner engine : engines) {
				engine.setInternalInterface(internalInterface);
			}
		}
	}
	
	@Override
	public void onPrepareSim() {
		super.onPrepareSim();
		initMultiModalSimEngineRunners();
	}
	
	/*
	 * The Threads are waiting at the startBarrier. We trigger them by reaching this Barrier.
	 * Now the Threads will start moving the Nodes and Links. We wait until all of them reach 
	 * the endBarrier to move on. We should not have any Problems with Race Conditions because 
	 * even if the Threads would be faster than this Thread, means they reach the endBarrier 
	 * before this Method does, it should work anyway.
	 */
	@Override
	public void doSimStep(final double time) {
		try {
			// set current Time
			for (CASimEngineRunner engine : this.engines) {
				engine.doSimStep(time);
			}

			/*
			 * Triggering the barrier will cause calls to moveLinks and moveNodes
			 * in the threads.
			 */
			this.startBarrier.await();

			this.endBarrier.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (BrokenBarrierException e) {
	      	throw new RuntimeException(e);
		}
		
		this.printSimLog(time);
	}
	
	@Override
	/*package*/ void moveNodes(final double time) {
		throw new RuntimeException("This method should never be called - calls should go to the MultiModalSimEngineRunner Threads.");
	}
	
	@Override
	/*package*/ void moveLinks(final double time) {
		throw new RuntimeException("This method should never be called - calls should go to the MultiModalSimEngineRunner Threads.");
	}
	
	@Override
	public void afterSim() {

		/*
		 * Calling the afterSim Method of the QSimEngineThreads
		 * will set their simulationRunning flag to false.
		 */
		for (CASimEngineRunner engine : this.engines) {
			engine.afterSim();
		}

		/*
		 * Triggering the startBarrier of the MultiModalSimEngineRunners.
		 * They will check whether the Simulation is still running.
		 * It is not, so the Threads will stop running.
		 */
		try {
			this.startBarrier.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (BrokenBarrierException e) {
			throw new RuntimeException(e);
		}

		// wait until each thread is finished
		try {
			for (Thread thread : this.threads) {
				thread.join();
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		super.afterSim();
	}

	@Override
	public void activateLink(CALink link) {
		throw new RuntimeException("This method should never be called - calls should go to the MultiModalSimEngineRunner Threads.");
	}

	@Override
	public void activateNode(CANode node) {
		throw new RuntimeException("This method should never be called - calls should go to the MultiModalSimEngineRunner Threads.");
	}
	
	@Override
	public int getNumberOfSimulatedLinks() {
		int numLinks = 0;
		for (CASimEngineRunner engine : this.engines) {
			numLinks = numLinks + engine.getNumberOfSimulatedLinks();
		}
		return numLinks;
	}

	@Override
	public int getNumberOfSimulatedNodes() {
		int numNodes = 0;
		for (CASimEngineRunner engine : this.engines) {
			numNodes = numNodes + engine.getNumberOfSimulatedNodes();
		}
		return numNodes;
	}
	
	private void initMultiModalSimEngineRunners() {

		this.threads = new Thread[numOfThreads];
		this.engines = new CASimEngineRunner[numOfThreads];

		this.startBarrier = new CyclicBarrier(numOfThreads + 1);
		this.separationBarrier = new CyclicBarrier(numOfThreads);
		this.endBarrier = new CyclicBarrier(numOfThreads + 1);

		// setup runners
		for (int i = 0; i < numOfThreads; i++) {
			CASimEngineRunner engine = new CASimEngineRunner(startBarrier, 
					separationBarrier, endBarrier, this.getMobsim(), this.getSpatialResoluation(), this);

			engine.setInternalInterface(this.internalInterface);
			
			Thread thread = new Thread(engine);
			thread.setName("MultiModalSimEngineRunner" + i);

			thread.setDaemon(true);	// make the Thread Daemons so they will terminate automatically
			this.threads[i] = thread;
			this.engines[i] = engine;

			thread.start();
		}
		
		// assign the Links and Nodes to the SimEngines
		assignSimEngines();
	}

	private void assignSimEngines() {
		int roundRobin = 0;

		for (NetsimNode node : this.getMobsim().getNetsimNetwork().getNetsimNodes().values()) {
			CASimEngine simEngine = engines[roundRobin % this.numOfThreads];
			super.getCANode(node.getNode().getId()).setMultiModalSimEngine(simEngine);
		
			/*
			 * Assign each link to its in-node to ensure that they are processed by the same
			 * thread which should avoid running into some race conditions.
			 */
			for (Link l : node.getNode().getOutLinks().values()) {
				super.getCALink(l.getId()).setCASimEngine(simEngine);
			}
			
			roundRobin++;
		}
	}
	
}
