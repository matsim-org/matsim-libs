/* *********************************************************************** *
 * project: org.matsim.*
 * ParallelMoveNodes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;

public class ParallelMoveNodes {
	
	private final static Logger log = Logger.getLogger(ParallelMoveNodes.class);
	
	private MoveNodesThread[] moveNodesThreads;
	private boolean simulateAllNodes = false;
	private int numOfThreads;
	
	private ExtendedQueueNode[][] parallelArrays;
	
	private static AtomicInteger threadLock;
	
	public ParallelMoveNodes(boolean simulateAllNodes)
	{
		this.simulateAllNodes = simulateAllNodes;
	}

	/*
	 * The Threads are waiting at the TimeStepStartBarrier.
	 * We trigger them by reaching this Barrier. Now the
	 * Threads will start moving the Nodes. We wait until
	 * all of them reach the TimeStepEndBarrier to move on.
	 * We should not have any Problems with Race Conditions
	 * because even if the Threads would be faster than this
	 * Thread, means the reach the TimeStepEndBarrier before
	 * this Method does, it should work anyway.
	 */
	public void run(double time)
	{	
		try
		{
			// lock threadLocker until wait() statement listens for the notifies
			synchronized(threadLock) 
			{
				threadLock.set(this.numOfThreads);
							
				for (MoveNodesThread moveNodesThread : moveNodesThreads) 
				{
					moveNodesThread.setTime(time);
					synchronized(moveNodesThread)
					{
						moveNodesThread.notify();
					}
				}
//				synchronized(MoveNodesThread.waiter)
//				{
//					MoveNodesThread.waiter.notifyAll();
//				}
				threadLock.wait();
			}		
		} 
		catch (InterruptedException e)
		{
			Gbl.errorMsg(e);
		}
	}
	
	/*
	 * Create equal sized Arrays.
	 */
	private void createArrays(QueueNode[] simNodesArray)
	{	
		List<List<ExtendedQueueNode>> nodes = new ArrayList<List<ExtendedQueueNode>>();
		for (int i = 0; i < numOfThreads; i++)
		{
			nodes.add(new ArrayList<ExtendedQueueNode>());
		}
		
		int roundRobin = 0;
		for (QueueNode queueNode : simNodesArray)
		{
			ExtendedQueueNode extendedQueueNode = new ExtendedQueueNode(queueNode, MatsimRandom.getLocalInstance());
			nodes.get(roundRobin % numOfThreads).add(extendedQueueNode);
			roundRobin++;
		}
		
		/*
		 * Now we create Arrays out of our Lists because iterating over them
		 * is much faster.
		 */
		parallelArrays = new ExtendedQueueNode[this.numOfThreads][];
		for (int i = 0; i < nodes.size(); i++)
		{
			List<ExtendedQueueNode> list = nodes.get(i);
			
			ExtendedQueueNode[] array = new ExtendedQueueNode[list.size()];
			list.toArray(array);
			parallelArrays[i] = array;
		}
	}
		
	public void init(QueueNode[] simNodesArray, int numOfThreads)
	{
		this.numOfThreads = numOfThreads;
		
		threadLock = new AtomicInteger(0);

		createArrays(simNodesArray);
		
		moveNodesThreads = new MoveNodesThread[numOfThreads];
						
		// setup threads
		for (int i = 0; i < numOfThreads; i++) 
		{
			MoveNodesThread moveNodesThread = new MoveNodesThread(simulateAllNodes);
			moveNodesThread.setName("MoveNodes" + i);
			moveNodesThread.setExtendedQueueNodeArray(this.parallelArrays[i]);			
			moveNodesThread.setDaemon(true);	// make the Thread Daemons so they will terminate automatically
			moveNodesThreads[i] = moveNodesThread;
			
			moveNodesThread.start();
		}
	}
	
	/*
	 * Contains a QueueNode and MATSimRandom Object that is used when
	 * moving Vehicles over the Node.
	 * This is needed to ensure that the Simulation produces deterministic
	 * results that do not depend on the number of parallel Threads.
	 */
	private static class ExtendedQueueNode
	{
		private QueueNode queueNode;
		private Random random;
		
		public ExtendedQueueNode(QueueNode queueNode, Random random)
		{
			this.queueNode = queueNode;
			this.random = random;
		}
		
		public QueueNode getQueueNode()
		{
			return this.queueNode;
		}
		
		public Random getRandom()
		{
			return this.random;
		}
	}
	
	/*
	 * The thread class that really handles the Nodes.
	 */
	private static class MoveNodesThread extends Thread
	{
		private double time = 0.0;
		private boolean simulateAllNodes = false;
		private ExtendedQueueNode[] queueNodes;
		
		private static Object waiter = new Object();
		
		public MoveNodesThread(boolean simulateAllNodes)
		{
			this.simulateAllNodes = simulateAllNodes;
		}

		public void setExtendedQueueNodeArray(ExtendedQueueNode[] queueNodes)
		{
			this.queueNodes = queueNodes;
		}
		
		public void setTime(final double t)
		{
			time = t;
		}
				
		@Override
		public void run()
		{	
			while(true)
			{
				try
				{
					/*
					 * threadLocker.decCounter() and wait() have to be
					 * executed in a synchronized block! Otherwise it could
					 * happen, that the replanning ends and the replanning of
					 * the next SimStep executes the notify command before
					 * wait() is called -> we have a DeadLock!
					 */
					synchronized(this)
//					synchronized(waiter)
					{
						int activeThreads = threadLock.decrementAndGet();
						if (activeThreads == 0)
						{
							synchronized(threadLock)
							{
								threadLock.notify();
							}
						}
						wait();
//						waiter.wait();
					}
					
					for (ExtendedQueueNode extendedQueueNode : queueNodes)
					{							
						QueueNode node = extendedQueueNode.getQueueNode();
						if (node.isActive() || node.isSignalized() || simulateAllNodes)
						{
							node.moveNode(time, extendedQueueNode.getRandom());
						}
					}
				}
				catch (InterruptedException e)
				{
					Gbl.errorMsg(e);
				}
			}
		}	// run()
		
	}	// ReplannerThread
	
}