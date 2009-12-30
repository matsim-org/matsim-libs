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
import java.util.ListIterator;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;

public class ParallelMoveNodesAndLinks {
	
	private final static Logger log = Logger.getLogger(ParallelMoveNodesAndLinks.class);
	
	private MoveThread[] moveThreads;
	private boolean simulateAllNodes = false;
	private boolean simulateAllLinks = false;
	private int numOfThreads;
	
	private ExtendedQueueNode[][] parallelNodesArrays;
	private List<List<QueueLink>> parallelSimLinksLists;
	
	private CyclicBarrier cyclicBarrier;
	
	private static AtomicInteger threadLock;
	private static AtomicInteger barrier;
//	private static Lock globalLock;
//	private static Condition runCondition;
//	private static Condition waitCondition;
	
	public ParallelMoveNodesAndLinks(boolean simulateAllNodes, boolean simulateAllLinks)
	{
		this.simulateAllNodes = simulateAllNodes;
		this.simulateAllLinks = simulateAllLinks;
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
				barrier.set(this.numOfThreads);
				
				for (MoveThread moveNodesThread : moveThreads) 
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
/*		

			for (MoveThread moveNodesThread : moveThreads) 
			{
				moveNodesThread.setTime(time);
			}			
			globalLock.lock();
			threadLock.set(this.numOfThreads);
			barrier.set(this.numOfThreads);
//			log.info("signalizing...");
			waitCondition.signalAll();
//			log.info("awaiting...");
			runCondition.await();
			globalLock.unlock();
//			log.info("unlocked...");
*/
		} 
		catch (InterruptedException e)
		{
			Gbl.errorMsg(e);
		}
	}
		
	public void initNodesAndLinks(QueueNode[] simNodesArray, List<QueueLink> allLinks, List<QueueLink> simActivateThis, int numOfThreads)
	{	
		this.numOfThreads = numOfThreads;
		
		threadLock = new AtomicInteger(0);
		barrier = new AtomicInteger(0);
//		globalLock = new ReentrantLock();
//		runCondition = globalLock.newCondition();
//		waitCondition = globalLock.newCondition();
				
		createNodesArrays(simNodesArray);
		createLinkLists(allLinks);
		
		LinkReActivator linkReActivator = new LinkReActivator(simActivateThis, parallelSimLinksLists, numOfThreads);
		
		this.cyclicBarrier = new CyclicBarrier(numOfThreads, linkReActivator);
				
		moveThreads = new MoveThread[numOfThreads];
						
		// setup threads
		for (int i = 0; i < numOfThreads; i++) 
		{
			MoveThread moveThread = new MoveThread(simulateAllNodes);
			moveThread.setName("MoveNodesAndLinks" + i);
			moveThread.setCyclicBarrier(this.cyclicBarrier);
//			moveThread.setRunnable(linkReActivator);
			moveThread.setExtendedQueueNodeArray(this.parallelNodesArrays[i]);
			moveThread.handleLinks(parallelSimLinksLists.get(i));
			moveThread.setDaemon(true);	// make the Thread Daemons so they will terminate automatically
			moveThreads[i] = moveThread;
			
			moveThread.start();
		}
	}
		
	/*
	 * Create equal sized Nodes Arrays.
	 */
	private void createNodesArrays(QueueNode[] simNodesArray)
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
	private void createLinkLists(List<QueueLink> allLinks)
	{
		parallelSimLinksLists = new ArrayList<List<QueueLink>>();
		
		for (int i = 0; i < numOfThreads; i++)
		{
			parallelSimLinksLists.add(new ArrayList<QueueLink>());
		}
		
		/*
		 * If we simulate all Links, we have to add them initially to the Lists.
		 */
		if (simulateAllLinks) 
		{
			int roundRobin = 0;
			for(QueueLink link : allLinks)
			{
				parallelSimLinksLists.get(roundRobin % numOfThreads).add(link);
				roundRobin++;
			}
		}
	}

	
	/*
	 * We have a List for each Thread so we have to add up their size
	 * to get the Number of simulated Links.
	 */
	/*package*/ int getNumberOfSimulatedLinks()
	{
		int size = 0;
		for (List<?> list : this.parallelSimLinksLists)
		{
			size = size + list.size();
		}
		
		return size;
	}
	
	/*
	 * We do the load balancing between the Threads using some kind
	 * of round robin.
	 * 
	 * Additionally we should check from time to time whether the load
	 * is really still balanced. This is not guaranteed due to the fact
	 * that some Links get deactivated while others don't. If the number
	 * of Links is high enough statistically the difference shouldn't
	 * be to significant.
	 */	
	private static class LinkReActivator implements Runnable
	{
		private List<QueueLink> simActivateThis;
		private List<List<QueueLink>> parallelSimLinksLists;
		private int numOfThreads;
		private int distributor = 0;
		
		
		public LinkReActivator(List<QueueLink> simActivateThis, List<List<QueueLink>> parallelSimLinksLists, int numOfThreads)
		{
			this.simActivateThis = simActivateThis;
			this.parallelSimLinksLists = parallelSimLinksLists;
			this.numOfThreads = numOfThreads;
		}
		
		public void run()
		{
			if (!simActivateThis.isEmpty())
			{
				for(QueueLink link : simActivateThis)
				{
					parallelSimLinksLists.get(distributor % numOfThreads).add(link);
					distributor++;
				}
				simActivateThis.clear();
			}	
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
	private static class MoveThread extends Thread
	{
		private double time = 0.0;
		private boolean simulateAllNodes = false;
		private boolean simulateAllLinks = false;
		private CyclicBarrier cyclicBarrier;
//		private Runnable runnable;
//		private static Lock lock = new ReentrantLock();
//		private static Condition condition = lock.newCondition();
		
		private ExtendedQueueNode[] queueNodes;
		private List<QueueLink> links = new ArrayList<QueueLink>();
		
//		private static Object waiter = new Object();
				
		public MoveThread(boolean simulateAllNodes)
		{
			this.simulateAllNodes = simulateAllNodes;
		}

		public void setCyclicBarrier(CyclicBarrier cyclicBarrier)
		{
			this.cyclicBarrier = cyclicBarrier;
		}
		
//		public void setRunnable(Runnable runnable)
//		{
//			this.runnable = runnable;
//		}
		
		public void setExtendedQueueNodeArray(ExtendedQueueNode[] queueNodes)
		{
			this.queueNodes = queueNodes;
		}
		
		public void handleLinks(List<QueueLink> links)
		{
			this.links = links;
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
					
/*
//					log.info("trying to get Lock..." + Thread.currentThread().getName());
					globalLock.lock();
					int activeThreads2 = threadLock.decrementAndGet();
					
					if (activeThreads2 == 0)
					{
//						log.info("signalizing..." + Thread.currentThread().getName());
						runCondition.signal();
					}
//					log.info("awaiting..." + Thread.currentThread().getName() + " " + activeThreads2);
					waitCondition.await();
					globalLock.unlock();
//					log.info("unlocked..." + Thread.currentThread().getName());
*/
					
					/*
					 * Move Nodes
					 */
					for (ExtendedQueueNode extendedQueueNode : queueNodes)
					{							
						QueueNode node = extendedQueueNode.getQueueNode();
//						synchronized(node)
//						{
							if (node.isActive() || node.isSignalized() || simulateAllNodes)
							{
								node.moveNode(time, extendedQueueNode.getRandom());
							}		
//						}
					}
					
					/*
					 * After moving the Nodes all we use a CyclicBarrier to synchronize
					 * the Threads. By using a Runnable within the Barrier we activate
					 * some Links. 
					 */
					this.cyclicBarrier.await();
					
//					lock.lock();
//					int activeThreads = barrier.decrementAndGet();
//					
//					if (activeThreads > 0)
//					{
//						synchronized(barrier)
//						{
//							lock.unlock();
//							barrier.wait();
//						}
//					}
//					else
//					{
//						runnable.run();
//						synchronized(barrier)
//						{
//							barrier.notifyAll();
//						}
//						lock.unlock();
//					}

//					lock.lock();
//					int activeThreads = barrier.decrementAndGet();
//					
//					if (activeThreads > 0)
//					{
//						condition.await();
//						lock.unlock();
//					}
//					else
//					{
//						runnable.run();
//						condition.signal();
//						lock.unlock();
//					}
					
					/*
					 * Move Links
					 */
					ListIterator<QueueLink> simLinks = this.links.listIterator();
					QueueLink link;
					boolean isActive;
					
					while (simLinks.hasNext()) 
					{
						link = simLinks.next();
						
						/*
						 * Synchronize on the QueueLink is only some kind of Workaround.
						 * It is only needed, if the QueueSimulation teleports Vehicles
						 * between different Threads. It would be probably faster, if the
						 * QueueSimulation would contain a synchronized method to do the
						 * teleportation instead of synchronize on EVERY QueueLink.
						 */
						synchronized(link)
						{
							isActive = link.moveLink(time);
							
							if (!isActive && !simulateAllLinks)
							{
								simLinks.remove();
							}
						}
					}
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
		}	// run()
		
	}	// ReplannerThread
	
}