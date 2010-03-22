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

package org.matsim.ptproject.qsim;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;

/**
 * An extended version of the QSimEngine that uses an array of
 * QSimEngineThreads to handle the movement of the Links and Nodes.
 * The results of the Simulation stays deterministic but the order 
 * of the LinkEvents within a single TimeStep does not. Ordering 
 * the Events by Time AND AgentId should produce deterministic results.
 * 
 * Due to the fact that a Random Number generator is used for each
 * simulated Node instead of one in total as the Single CPU
 * QSim does, the Results will slightly vary between
 * the Simulations!
 */
public class ParallelQSimEngine extends QSimEngineImpl{

	private QSimEngineThread[] threads;
	private int numOfThreads;
	
	private ExtendedQueueNode[][] parallelNodesArrays;
	private List<List<QLink>> parallelSimLinksLists;

	private CyclicBarrier separationBarrier;	// separates moveNodes and moveLinks
	private CyclicBarrier startBarrier;
	private CyclicBarrier endBarrier;
	

	public ParallelQSimEngine(final QSim sim, final Random random, int numOfThreads)
	{
	  super(sim, random);
		initQSimEngineThreads(numOfThreads);
	}
	
	/**
	 * Implements one simulation step, called from simulation framework
	 * @param time The current time in the simulation.
	 */
	@Override
	public void simStep(final double time)
	{
		run(time);
	}

	@Override
	public void afterSim() {

		/*
		 * Calling the afterSim Method of the QSimEngineThreads
		 * will set their simulationRunning flag to false.
		 */
		for (QSimEngineThread thread : threads)
		{
			thread.afterSim();
		}
		
		/*
		 * Triggering the startBarrier of the QSimEngineThreads.
		 * They will check whether the Simulation is still running.
		 * It is not, so the Threads will stop running.
		 */
		try
		{
			this.startBarrier.await();
		} 
		catch (InterruptedException e)
		{
			Gbl.errorMsg(e);
		} catch (BrokenBarrierException e)
		{
			Gbl.errorMsg(e);
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
	private void run(double time)
	{
		try
		{
			// set current Time
			for (QSimEngineThread thread : threads)
			{
				thread.setTime(time);
			}

			this.startBarrier.await();

			this.endBarrier.await();
		}
		catch (InterruptedException e)
		{
			Gbl.errorMsg(e);
		}
		catch (BrokenBarrierException e)
		{
	      	Gbl.errorMsg(e);
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
	public synchronized void activateLink(final QLink link)
	{
		super.activateLink(link);
	}

	@Override
	public int getNumberOfSimulatedLinks() {
		
		int numLinks = 0;
		
		for (QSimEngineThread thread : threads)
		{
			numLinks = numLinks + thread.getNumberOfSimulatedLinks();
		}
		
		return numLinks;
	}
	
	private void initQSimEngineThreads(int numOfThreads)
	{ 
		this.numOfThreads = numOfThreads;

		createNodesArrays(simNodesArray);
		createLinkLists(allLinks);

		threads = new QSimEngineThread[numOfThreads];
		LinkReActivator linkReActivator = new LinkReActivator(threads);

		this.startBarrier = new CyclicBarrier(numOfThreads + 1);
		this.separationBarrier = new CyclicBarrier(numOfThreads, linkReActivator);
		this.endBarrier = new CyclicBarrier(numOfThreads + 1);

		// setup threads
		for (int i = 0; i < numOfThreads; i++)
		{
			QSimEngineThread thread = new QSimEngineThread(simulateAllNodes, simulateAllLinks, this.startBarrier, this.separationBarrier, this.endBarrier, this.getQSim());
			thread.setName("QSimEngineThread" + i);

			thread.setExtendedQueueNodeArray(this.parallelNodesArrays[i]);
			thread.setLinks(parallelSimLinksLists.get(i));
			thread.setDaemon(true);	// make the Thread Daemons so they will terminate automatically
			threads[i] = thread;

			thread.start();
		}

		// Assign every Link to a LinkActivator, depending on its InNode
		assignLinkActivators();
	}

	
	/*
	 * Create equal sized Nodes Arrays.
	 */
	private void createNodesArrays(QNode[] simNodesArray)
	{
		List<List<ExtendedQueueNode>> nodes = new ArrayList<List<ExtendedQueueNode>>();
		for (int i = 0; i < numOfThreads; i++)
		{
			nodes.add(new ArrayList<ExtendedQueueNode>());
		}

		int roundRobin = 0;
		for (QNode queueNode : simNodesArray)
		{
			ExtendedQueueNode extendedQueueNode = new ExtendedQueueNode(queueNode, MatsimRandom.getLocalInstance());
			nodes.get(roundRobin % numOfThreads).add(extendedQueueNode);
			roundRobin++;
		}

		/*
		 * Now we create Arrays out of our Lists because iterating over them
		 * is much faster.
		 */
		parallelNodesArrays = new ExtendedQueueNode[this.numOfThreads][];
		for (int i = 0; i < nodes.size(); i++)
		{
			List<ExtendedQueueNode> list = nodes.get(i);

			ExtendedQueueNode[] array = new ExtendedQueueNode[list.size()];
			list.toArray(array);
			parallelNodesArrays[i] = array;
		}
	}

	/*
	 * Create the Lists of QueueLinks that are handled on parallel Threads.
	 */
	private void createLinkLists(List<QLink> allLinks)
	{
		parallelSimLinksLists = new ArrayList<List<QLink>>();

		for (int i = 0; i < numOfThreads; i++)
		{
			parallelSimLinksLists.add(new ArrayList<QLink>());
		}

		/*
		 * If we simulate all Links, we have to add them initially to the Lists.
		 */
		if (simulateAllLinks)
		{
			int roundRobin = 0;
			for(QLink link : allLinks)
			{
				parallelSimLinksLists.get(roundRobin % numOfThreads).add(link);
				roundRobin++;
			}
		}
	}

	/*
	 * Within the MoveThreads Links are only activated when
	 * a Vehicle is moved over a Node what is processed by
	 * that Node. So we can assign each QLink to the
	 * Thread that handles its InNode.
	 */
	private void assignLinkActivators()
	{
		int thread = 0;
		for (ExtendedQueueNode[] array : parallelNodesArrays)
		{
//			for (ExtendedQueueNode node : array)
//			{
//				Node n = node.getQueueNode().getNode();
//
//				for (Link outLink : n.getOutLinks().values())
//				{
//					QLink qLink = node.getQueueNode().queueNetwork.getQueueLink(outLink.getId());
//				}
//			}
			thread++;
		}
	}
	
	/*
	 * Contains a QueueNode and MATSimRandom Object that is used when
	 * moving Vehicles over the Node.
	 * This is needed to ensure that the Simulation produces deterministic
	 * results that do not depend on the number of parallel Threads.
	 */
	/*package*/ static class ExtendedQueueNode
	{
		private QNode queueNode;
		private Random random;

		public ExtendedQueueNode(QNode queueNode, Random random)
		{
			this.queueNode = queueNode;
			this.random = random;
		}

		public QNode getQueueNode()
		{
			return this.queueNode;
		}

		public Random getRandom()
		{
			return this.random;
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
	/*package*/ static class LinkReActivator implements Runnable
	{
		private final QSimEngineThread[] threads;
//		private final int numOfThreads;
//		private int distributor = 0;

		public LinkReActivator(QSimEngineThread[] threads)
		{
			this.threads = threads;
//			this.numOfThreads = threads.length;
		}

		public void run()
		{
			/*
			 * Each Thread contains a List of Links to activate.
			 */
			for (QSimEngineThread thread : threads)
			{
				/*
				 * We do not redistribute the Links - they will be processed
				 * by the same thread during the whole simulation.
				 */
				thread.activateLinks();
				
//				/*
//				 * We redistribute the deactivated Links among all Threads.
//				 */
//				for(QLink link : thread.getLinksToActivate())
//				{
//					threads[distributor % numOfThreads].addLink(link);
//					distributor++;
//				}
//				thread.getLinksToActivate().clear();
			}
		}

	}
}
