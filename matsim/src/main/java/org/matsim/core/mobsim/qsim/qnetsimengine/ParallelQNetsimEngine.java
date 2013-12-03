/* *********************************************************************** *
 * project: org.matsim.*
 * ParallelQSimEngine.java
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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.QSim;

/**
 * An extended version of the QSimEngine that uses an array of
 * QSimEngineThreads to handle the movement of the Links and Nodes.
 * The results of the Simulation stays deterministic but the order
 * of the LinkEvents within a single TimeStep does not. Ordering
 * the Events by Time AND AgentId should produce deterministic results.
 * <br/><br/>
 * Due to the fact that a Random Number generator is used for each
 * simulated Node instead of one in total as the Single CPU
 * QSim does, the Results will slightly vary between the parallel and the sequential version.
 * <br/><br/>
 * The ParallelQNetsimEngine (this class) will fulfill the QSimEngine interface upwards (i.e. against the QSim).
 * The QSimEngineThreads will fulfill the QSimEngine interface downwards (i.e. against nodes and links).
 */
class ParallelQNetsimEngine extends QNetsimEngine {

	final private static Logger log = Logger.getLogger(ParallelQNetsimEngine.class);

	private final int numOfThreads;
	private Thread[] threads;
	private QSimEngineRunner[] engines;

	private CyclicBarrier separationBarrier;	// separates moveNodes and moveLinks
	private CyclicBarrier startBarrier;
	private CyclicBarrier endBarrier;
	
	private final Set<QLinkInternalI> linksToActivateInitially = new HashSet<QLinkInternalI>();
	private boolean isPrepared = false;

	ParallelQNetsimEngine(final QSim sim) {
		super(sim);
		// (DepartureHandler does not need to be added here since it is added in the "super" c'tor)

		this.numOfThreads = this.getMobsim().getScenario().getConfig().qsim().getNumberOfThreads();
	}

	@Override
	public void onPrepareSim() {
		super.onPrepareSim();
		initQSimEngineThreads();
		this.isPrepared = true;
	}

	/**
	 * Implements one simulation step, called from simulation framework
	 * @param time The current time in the simulation.
	 */
	@Override
	public void doSimStep(final double time) {
		run(time);

		this.printSimLog(time);
	}

	@Override
	public void afterSim() {

		/*
		 * Calling the afterSim Method of the QSimEngineThreads
		 * will set their simulationRunning flag to false.
		 */
		for (QSimEngineRunner engine : this.engines) {
			engine.afterSim();
		}

		/*
		 * Triggering the startBarrier of the QSimEngineThreads.
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

		super.afterSim();
	}

	/*
	 * The Threads are waiting at the startBarrier.
	 * We trigger them by reaching this Barrier. Now the
	 * Threads will start moving the Nodes and Links. We wait
	 * until all of them reach the endBarrier to move
	 * on. We should not have any Problems with Race Conditions
	 * because even if the Threads would be faster than this
	 * Thread, means the reach the endBarrier before
	 * this Method does, it should work anyway.
	 */
	private void run(double time) {

		try {
			// set current Time
			for (QSimEngineRunner engine : this.engines) {
				engine.setTime(time);
			}

			this.startBarrier.await();

			this.endBarrier.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (BrokenBarrierException e) {
			throw new RuntimeException(e);
		}
	}

	/*
	 * We have to call this method synchronized because it is used by
	 * parallel Threads to move the Nodes.
	 * Maybe each Thread could have its own list and the Links can be collected
	 * from the main Thread to avoid this?
	 *
	 * Should not be called anymore from the parallel Threads because we use
	 * them as LinkActivators.
	 */
	@Override
	protected synchronized void activateLink(final QLinkInternalI link) {
		/*
		 * The ActivityEngine might activate links when inserting Agents into the mobsim.
		 * This only occurs before the onPrepareSim method of this class is called. Links
		 * containing such agents are activated when assigned to a QSimEngineRunner.
		 */
		if (isPrepared) {
			throw new RuntimeException("Links should be activated by a QSimEngineRunner and not by the ParallelQNetsimEngine!");
		} else { 
			linksToActivateInitially.add(link);
		}
	}

	@Override
	protected synchronized void activateNode(final QNode node) {
		throw new RuntimeException("Nodes should be activated by a QSimEngineRunner and not by the ParallelQNetsimEngine!");
	}

	@Override
	public int getNumberOfSimulatedLinks() {

		int numLinks = 0;

		for (QSimEngineRunner engine : this.engines) {
			numLinks = numLinks + engine.getNumberOfSimulatedLinks();
		}

		return numLinks;
	}

	@Override
	public int getNumberOfSimulatedNodes() {

		int numNodes = 0;

		for (QSimEngineRunner engine : this.engines) {
			numNodes = numNodes + engine.getNumberOfSimulatedNodes();
		}

		return numNodes;
	}

	private void initQSimEngineThreads() {

		this.threads = new Thread[this.numOfThreads];
		this.engines = new QSimEngineRunner[this.numOfThreads] ;
		LinkReActivator linkReActivator = new LinkReActivator(this.engines);
		NodeReActivator nodeReActivator = new NodeReActivator(this.engines);

		this.startBarrier = new CyclicBarrier(this.numOfThreads + 1);
		this.separationBarrier = new CyclicBarrier(this.numOfThreads, linkReActivator);
		//		this.endBarrier = new CyclicBarrier(numOfThreads + 1);
		this.endBarrier = new CyclicBarrier(this.numOfThreads + 1, nodeReActivator);

		// setup threads
		for (int i = 0; i < this.numOfThreads; i++) {
			QSimEngineRunner engine = new QSimEngineRunner(this.startBarrier, this.separationBarrier,
					this.endBarrier);
			Thread thread = new Thread(engine);
			thread.setName("QSimEngineThread" + i);

			thread.setDaemon(true);	// make the Thread Daemons so they will terminate automatically
			this.threads[i] = thread;
			this.engines[i] = engine;

			thread.start();
		}

		/*
		 *  Assign every Link and Node to an Activator. By doing so, the
		 *  activateNode(...) and activateLink(...) methods in this class
		 *  should become obsolete.
		 */
		assignNetElementActivators();
	}

	/*
	 * Within the MoveThreads Links are only activated when a Vehicle is moved
	 * over a Node which is processed by that Thread. So we can assign each QLink
	 * to the Thread that handles its InNode.
	 */
	private void assignNetElementActivators() {

		// only for statistics
		int nodes[] = new int[this.engines.length];
		int links[] = new int[this.engines.length];

		int roundRobin = 0;
		for (QNode node : network.getNetsimNodes().values()) {
			int i = roundRobin % this.numOfThreads;
			node.setNetElementActivator(this.engines[i]);
			nodes[i]++;

			// set activator for out links
			for (Link outLink : node.getNode().getOutLinks().values()) {
				AbstractQLink qLink = (AbstractQLink) network.getNetsimLink(outLink.getId());
				// (must be of this type to work.  kai, feb'12)

				// removing qsim as "person in the middle".  not fully sure if this is the same in the parallel impl.  kai, oct'10
				qLink.setNetElementActivator(this.engines[i]);
				
				/*
				 * If the QLink contains agents that end their activity in the first time
				 * step, the link should be activated.
				 */
				if (linksToActivateInitially.remove(qLink)) {
					this.engines[i].activateLink(qLink);					
				}
				
				links[i]++;
				
			}

			roundRobin++;
		}

		// print some statistics
		for (int i = 0; i < this.engines.length; i++) {
			log.info("Assigned " + nodes[i] + " nodes and " + links[i] + " links to QSimEngineRunner #" + i);
		}
		
		this.linksToActivateInitially.clear();
	}

	/*
	 * We do the load balancing between the Threads using some kind
	 * of round robin.
	 *
	 * Additionally we should check from time to time whether the load
	 * is really still balanced. This is not guaranteed due to the fact
	 * that some Links get deactivated while others don't. If the number
	 * of Links is high enough statistically the difference should not
	 * be to significant.
	 */
	/*package*/ static class LinkReActivator implements Runnable {
		private final QSimEngineRunner[] runners;

		public LinkReActivator(QSimEngineRunner[] threads) {
			this.runners = threads;
		}

		@Override
		public void run() {
			/*
			 * Each Thread contains a List of Links to activate.
			 */
			for (QSimEngineRunner runner : this.runners) {
				/*
				 * We do not redistribute the Links - they will be processed
				 * by the same thread during the whole simulation.
				 */
				runner.activateLinks();
			}
		}
	}

	/*package*/ static class NodeReActivator implements Runnable {
		private final QSimEngineRunner[] runners;

		public NodeReActivator(QSimEngineRunner[] runners) {
			this.runners = runners;
		}

		@Override
		public void run() {
			/*
			 * Each Thread contains a List of Links to activate.
			 */
			for (QSimEngineRunner runner : this.runners) {
				/*
				 * We do not redistribute the Nodes - they will be processed
				 * by the same thread during the whole simulation.
				 */
				runner.activateNodes();
			}
		}
	}
}