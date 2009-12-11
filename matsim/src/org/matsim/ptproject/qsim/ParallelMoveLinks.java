/* *********************************************************************** *
 * project: org.matsim.*
 * ParallelMoveLinks.java
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
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;

public class ParallelMoveLinks {
	
	private final static Logger log = Logger.getLogger(ParallelMoveLinks.class);
	
	private MoveLinksThread[] moveLinksThreads;
	private boolean simulateAllLinks = false;
	private int numOfThreads;
	private int distributor = 0;
		
	static AtomicInteger threadLock;
		
	/* 
	 * This is the collection of links that have to be moved in the simulation
	 */
	private List<List<QueueLink>> parallelSimLinksLists;
		
	public ParallelMoveLinks(boolean simulateAllLinks)
	{
		this.simulateAllLinks = simulateAllLinks;
	}

	/*
	 * The Threads are waiting at the TimeStepStartBarrier.
	 * We trigger them by reaching this Barrier. Now the
	 * Threads will start moving the Links. We wait until
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
							
				for (MoveLinksThread moveLinksThread : moveLinksThreads)
				{
					moveLinksThread.setTime(time);
					synchronized(moveLinksThread)
					{
						moveLinksThread.notify();
					}
				}
//				synchronized(MoveLinksThread.waiter)
//				{
//					MoveLinksThread.waiter.notifyAll();
//				}
				threadLock.wait();
			}		
		} 
		catch (InterruptedException e)
		{
			Gbl.errorMsg(e);
		}
	}
		
	public void init(List<QueueLink> allLinks, int numOfThreads)
	{		
		this.numOfThreads = numOfThreads;

		threadLock = new AtomicInteger(0);
		
		createLinkLists(allLinks);
		
		moveLinksThreads = new MoveLinksThread[numOfThreads];
		
		// setup threads
		for (int i = 0; i < numOfThreads; i++) 
		{
			MoveLinksThread moveLinkThread = new MoveLinksThread(simulateAllLinks);
			moveLinkThread.setName("MoveLinks" + i);
			moveLinkThread.handleLinks(parallelSimLinksLists.get(i));
			moveLinkThread.setDaemon(true);	// make the Thread Daemons so they will terminate automatically
			moveLinksThreads[i] = moveLinkThread;
			
			moveLinkThread.start();
		}
			
		/*
		 * After initialization the Threads are waiting at the
		 * TimeStepEndBarrier. We trigger this Barrier once so 
		 * they wait at the TimeStepStartBarrier what has to be
		 * their state if the run() method is called.
		 */
//		try
//		{
//			this.timeStepEndBarrier.await();
//		} 
//		catch (InterruptedException e) 
//		{
//			Gbl.errorMsg(e);
//		} 
//		catch (BrokenBarrierException e)
//		{
//			Gbl.errorMsg(e);
//		}
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
	 * We do the load balancing between the Threads using some kind
	 * of round robin.
	 * 
	 * Additionally we should check from time to time whether the load
	 * is really still balanced. This is not guaranteed due to the fact
	 * that some Links get deactivated while others don't. If the number
	 * of Links is high enough statistically the difference shouldn't
	 * be to significant.
	 */	
	/*package*/ void reactivateLinks(List<QueueLink> simActivateThis) 
	{
		if (!simulateAllLinks)
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
	
	/**
	 * The thread class that really handles the links.
	 */
	private static class MoveLinksThread extends Thread
	{
		private double time = 0.0;
		private boolean simulationRunning = true;
		private boolean simulateAllLinks = false;
		
		private List<QueueLink> links = new ArrayList<QueueLink>();
		
		protected static Object waiter = new Object();
		
		public MoveLinksThread(boolean simulateAllLinks)
		{
			this.simulateAllLinks = simulateAllLinks;
		}
				
		public void setTime(final double t)
		{
			time = t;
		}
		
		public void handleLinks(List<QueueLink> links)
		{
			this.links = links;
		}

		@Override
		public void run()
		{
			while (simulationRunning)
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
					log.error("Something is going wrong here...");
					Gbl.errorMsg(e);
				}
//	            catch (BrokenBarrierException e)
//	            {
//	            	Gbl.errorMsg(e);
//	            }
			}	// while Simulation Running
			
		}	// run()
		
	}	// ReplannerThread
	
}