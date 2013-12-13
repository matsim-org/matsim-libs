/* *********************************************************************** *
 * project: org.matsim.*
 * ParallelMultiModalSimEngine.java
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

import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.router.util.TravelTime;

class ParallelMultiModalSimEngine extends MultiModalSimEngine {
	
	private final static Logger log = Logger.getLogger(ParallelMultiModalSimEngine.class);
	
	private final int numOfThreads;
	
	private Thread[] threads;
	private MultiModalSimEngineRunner[] engines;
	private CyclicBarrier startBarrier;
	private CyclicBarrier separationBarrier;	// separates moveNodes and moveLinks
	private CyclicBarrier endBarrier;
	
	// use the factory
	/*package*/ ParallelMultiModalSimEngine(Netsim sim, Map<String, TravelTime> multiModalTravelTimes) {
		super(sim, multiModalTravelTimes);
		MultiModalConfigGroup multiModalConfigGroup = 
				(MultiModalConfigGroup) sim.getScenario().getConfig().getModule(MultiModalConfigGroup.GROUP_NAME);
		this.numOfThreads = multiModalConfigGroup.getNumberOfThreads();
	}
	
	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		super.setInternalInterface(internalInterface);
		
		/*
		 * If the engines have already been created, hand the internalInterface
		 * over to them.
		 */
		if (this.engines != null) {
			for (MultiModalSimEngineRunner engine : engines) {
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
			for (MultiModalSimEngineRunner engine : this.engines) {
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
	Map<String, TravelTime> getMultiModalTravelTimes() {
		throw new RuntimeException("This method should never be called - calls should go to the MultiModalSimEngineRunner Threads.");
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
		for (MultiModalSimEngineRunner engine : this.engines) {
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
	public void activateLink(MultiModalQLinkExtension link) {
		throw new RuntimeException("This method should never be called - calls should go to the MultiModalSimEngineRunner Threads.");
	}

	@Override
	public void activateNode(MultiModalQNodeExtension node) {
		throw new RuntimeException("This method should never be called - calls should go to the MultiModalSimEngineRunner Threads.");
	}
	
	@Override
	public int getNumberOfSimulatedLinks() {
		int numLinks = 0;
		for (MultiModalSimEngineRunner engine : this.engines) {
			numLinks = numLinks + engine.getNumberOfSimulatedLinks();
		}
		return numLinks;
	}

	@Override
	public int getNumberOfSimulatedNodes() {
		int numNodes = 0;
		for (MultiModalSimEngineRunner engine : this.engines) {
			numNodes = numNodes + engine.getNumberOfSimulatedNodes();
		}
		return numNodes;
	}
	
	private void initMultiModalSimEngineRunners() {

		this.threads = new Thread[numOfThreads];
		this.engines = new MultiModalSimEngineRunner[numOfThreads];

		this.startBarrier = new CyclicBarrier(numOfThreads + 1);
		this.separationBarrier = new CyclicBarrier(numOfThreads);
		this.endBarrier = new CyclicBarrier(numOfThreads + 1);

		// setup runners
		for (int i = 0; i < numOfThreads; i++) {
			MultiModalSimEngineRunner engine = new MultiModalSimEngineRunner(startBarrier, 
					separationBarrier, endBarrier, this.getMobsim(), multiModalTravelTimes, this);

			engine.setInternalInterface(this.internalInterface);
			
			/*
			 * The idea that each thread could have its own instance of a EventsManager.
			 * If this is realized, here something like eventsManager.getInstance() could be added.
			 */
//			engine.setEventsManager(eventsManager);
			
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

		// only for statistics
		int nodes[] = new int[this.engines.length];
		int links[] = new int[this.engines.length];
		
		int roundRobin = 0;
		Scenario scenario = this.qSim.getScenario();
		
		for (Node node : scenario.getNetwork().getNodes().values()) {
			MultiModalQNodeExtension multiModalQNodeExtension = this.getMultiModalQNodeExtension(node.getId());
			
			// if the node is simulated by the MultiModalSimulation
			if (multiModalQNodeExtension != null) {
				int i = roundRobin % this.numOfThreads;
				MultiModalSimEngine simEngine = this.engines[i];
				multiModalQNodeExtension.setMultiModalSimEngine(simEngine);
				nodes[i]++;
				
				/*
				 * Assign each link to its in-node to ensure that they are processed by the same
				 * thread which should avoid running into some race conditions.
				 */
				for (Link link : node.getOutLinks().values()) {
					MultiModalQLinkExtension multiModalQLinkExtension = this.getMultiModalQLinkExtension(link.getId());
					if (multiModalQLinkExtension != null) {
						multiModalQLinkExtension.setMultiModalSimEngine(simEngine);
						links[i]++;
					}
				}
				
				roundRobin++;
			}
		}
		
		// print some statistics
		for (int i = 0; i < this.engines.length; i++) {
			log.info("Assigned " + nodes[i] + " nodes and " + links[i] + " links to MultiModalSimEngineRunner #" + i);
		}
	}
	
}
