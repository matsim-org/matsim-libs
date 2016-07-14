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

package playground.sergioo.ptsim2013.qnetsimengine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.interfaces.NetsimLink;

import playground.sergioo.ptsim2013.QSim;


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
class PTParallelQNetsimEngine extends PTQNetsimEngine {

	final private static Logger log = Logger.getLogger(PTParallelQNetsimEngine.class);

	private int numOfThreads;
	private Thread[] threads;
	private QSimEngineRunner[] engines;

	private QNode[][] parallelNodesArrays;
	private List<List<QNode>> parallelNodesLists;
	private List<List<PTQLink>> parallelSimLinksLists;

	private CyclicBarrier separationBarrier;	// separates moveNodes and moveLinks
	private CyclicBarrier startBarrier;
	private CyclicBarrier endBarrier;


	PTParallelQNetsimEngine(final QSim sim, NetsimNetworkFactory<QNode, PTQLink> netsimNetworkFactory) {
		super(sim, netsimNetworkFactory);
		// (DepartureHander does not need to be added here since it is added in the "super" c'tor)

		this.numOfThreads = this.getMobsim().getScenario().getConfig().qsim().getNumberOfThreads();
	}

	@Override
	public void onPrepareSim() {
		super.onPrepareSim();
		initQSimEngineThreads(this.numOfThreads);
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
	protected synchronized void activateLink(final NetsimLink link) {
		log.warn("Links should be activated by a QSimEngineRunner and not by the ParallelQNetsimEngine!");
		super.activateLink(link);
	}

	@Override
	protected synchronized void activateNode(final QNode node) {
		log.warn("Nodes should be activated by a QSimEngineRunner and not by the ParallelQNetsimEngine!");
		super.activateNode(node);
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

	private void initQSimEngineThreads(int numOfThreads) {
		this.numOfThreads = numOfThreads;

		createNodesLists();
		createLinkLists();

		if (useNodeArray) {
			createNodesArray();
			// if we use arrays, we don't need the lists anymore
			this.parallelNodesLists = null;
		}

		this.threads = new Thread[numOfThreads];
		this.engines = new QSimEngineRunner[numOfThreads] ;
		LinkReActivator linkReActivator = new LinkReActivator(this.engines);
		NodeReActivator nodeReActivator = new NodeReActivator(this.engines);

		this.startBarrier = new CyclicBarrier(numOfThreads + 1);
		this.separationBarrier = new CyclicBarrier(numOfThreads, linkReActivator);
		//		this.endBarrier = new CyclicBarrier(numOfThreads + 1);
		this.endBarrier = new CyclicBarrier(numOfThreads + 1, nodeReActivator);

		// setup threads
		for (int i = 0; i < numOfThreads; i++) {
			QSimEngineRunner engine = new QSimEngineRunner(simulateAllNodes, simulateAllLinks, this.startBarrier, this.separationBarrier, 
					this.endBarrier);

			if (useNodeArray) {
				engine.setQNodeArray(this.parallelNodesArrays[i]);
			} else {
				engine.setQNodeList(this.parallelNodesLists.get(i));
			}

			engine.setLinks(this.parallelSimLinksLists.get(i));
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
	 * Create equal sized Nodes Lists.
	 */
	private void createNodesLists() {
		parallelNodesLists = new ArrayList<List<QNode>>();
		for (int i = 0; i < this.numOfThreads; i++) {
			parallelNodesLists.add(new ArrayList<QNode>());
		}

		int roundRobin = 0;
		for (QNode node : allNodes) {
			parallelNodesLists.get(roundRobin % this.numOfThreads).add(node);
			roundRobin++;
		}
	}

	/*
	 * Create Nodes Array
	 */
	private void createNodesArray() {
		/*
		 * Now we create Arrays out of our Lists because iterating over them
		 * is much faster.
		 */
		this.parallelNodesArrays = new QNode[this.numOfThreads][];
		for (int i = 0; i < parallelNodesLists.size(); i++) {
			List<QNode> list = parallelNodesLists.get(i);

			QNode[] array = new QNode[list.size()];
			list.toArray(array);
			this.parallelNodesArrays[i] = array;
		}
	}

	/*
	 * Create the Lists of QueueLinks that are handled on parallel Threads.
	 */
	private void createLinkLists() {
		this.parallelSimLinksLists = new ArrayList<List<PTQLink>>();

		for (int i = 0; i < this.numOfThreads; i++) {
			this.parallelSimLinksLists.add(new ArrayList<PTQLink>());
		}

		/*
		 * If we simulate all Links, we have to add them initially to the Lists.
		 */
		if (simulateAllLinks) {
			int roundRobin = 0;
			for(PTQLink link : allLinks) {
				this.parallelSimLinksLists.get(roundRobin % this.numOfThreads).add(link);
				roundRobin++;
			}
		}
	}

	/*
	 * Within the MoveThreads Links are only activated when a Vehicle is moved 
	 * over a Node which is processed by that Thread. So we can assign each QLink 
	 * to the Thread that handles its InNode.
	 */
	private void assignNetElementActivators() {
		int thread = 0;
		if (useNodeArray) {
			for (QNode[] array : parallelNodesArrays) {
				for (QNode node : array) {
					node.setNetElementActivator(this.engines[thread]);
					
					// set activator for links
					for (Link outLink : node.getNode().getOutLinks().values()) {
						PTQLink qLink = (PTQLink) network.getNetsimLink(outLink.getId());
						// (must be of this type to work.  kai, feb'12)
						
						// removing qsim as "person in the middle".  not fully sure if this is the same in the parallel impl.  kai, oct'10
						qLink.setNetElementActivator(this.engines[thread]);
					}
				}
				thread++;
			}
		} else {
			for (List<QNode> list : parallelNodesLists) {
				for (QNode node : list) {
					// set activator for nodes
					node.setNetElementActivator(this.engines[thread]);
					// If we simulate all Nodes, we have to add them initially to the Lists.
					if (simulateAllNodes) this.engines[thread].activateNode(node);
					
					// set activator for links
					for (Link outLink : node.getNode().getOutLinks().values()) {
						PTQLink qLink = (PTQLink) network.getNetsimLink(outLink.getId());
						// (must be of this type to work.  kai, feb'12)
						
						// removing qsim as "person in the middle".  not fully sure if this is the same in the parallel impl.  kai, oct'10
						qLink.setNetElementActivator(this.engines[thread]);
					}
				}
				if (simulateAllNodes) this.engines[thread].activateNodes();
				thread++;
			}
		}
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
