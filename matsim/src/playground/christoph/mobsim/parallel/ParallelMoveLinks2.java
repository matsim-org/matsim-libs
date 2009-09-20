///* *********************************************************************** *
// * project: org.matsim.*
// * ParallelMoveLinks2.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2008 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//package playground.christoph.mobsim.parallel;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.ListIterator;
//
//import org.apache.log4j.Logger;
//import org.matsim.core.gbl.Gbl;
//import org.matsim.core.mobsim.queuesim.QueueLink;
//
//public class ParallelMoveLinks2 {
//	
//	private final static Logger log = Logger.getLogger(ParallelMoveLinks2.class);
//	
//	private static MoveLinkThread[] moveLinksThreads;
//	private static ThreadLocker threadLocker;
//	private boolean simulateAllLinks = false;
//	private int numOfThreads;
//	
//	public List<List<QueueLink>> parallelSimLinksArray;
//	
//	public ParallelMoveLinks2(boolean simulateAllLinks)
//	{
//		this.simulateAllLinks = simulateAllLinks;
//	}
//
//	/*
//	 * Hand over SubLists to the Threads. This allows
//	 * them to remove inactive Links from the list directly!
//	 * 
//	 * Warning: the SubLists are still connected to the
//	 * underlying list. We can iterate over them in the
//	 * Threads but we are not allowed to modify them!
//	 * 
//	 * Edit: Now we copy the SubLists. This allows us to
//	 * remove inactive Links directly from those Lists.
//	 */
//	public void run(double time)
//	{		
//		try
//		{
//			// lock threadLocker until wait() statement listens for the notifies
//			synchronized(threadLocker) 
//			{
//				// set current Time
//				MoveLinkThread.setTime(time);
//							
//				threadLocker.setCounter(moveLinksThreads.length);
//				threadLocker.time = time;
//				
//				for (MoveLinkThread moveLinksThread : moveLinksThreads)
//				{
//					moveLinksThread.startReplanning();		
//				}
//	
//				threadLocker.wait();
//			}		
//		} 
//		catch (InterruptedException e)
//		{
//			Gbl.errorMsg(e);
//		}
//	}
//	
//	public static synchronized boolean isRunning()
//	{
//		boolean stillReplanning = false;
//		// if moving Links is finished in all Threads
//		for (MoveLinkThread moveLinkThread : moveLinksThreads)
//		{
//			if (!moveLinkThread.isWaiting())
//			{
//				stillReplanning = true;
//				break;
//			}			
//		}
//		return stillReplanning;
//	}
//	
//	public void init(List<List<QueueLink>> linkLists)
//	{
//		this.numOfThreads = linkLists.size();
//		
//		threadLocker = new ThreadLocker();
//		
//		MoveLinkThread.setThreadLocker(threadLocker);
//		
//		moveLinksThreads = new MoveLinkThread[numOfThreads];
//		
//		// setup threads
//		for (int i = 0; i < numOfThreads; i++) 
//		{
//			MoveLinkThread moveLinkThread = new MoveLinkThread(simulateAllLinks);
//			moveLinkThread.setName("MoveLinks" + i);
//			moveLinkThread.handleLinks(linkLists.get(i));
//			moveLinkThread.setDaemon(true);	// make the Thread Daemons so they will terminate automatically
//			moveLinksThreads[i] = moveLinkThread;
//			
//			moveLinkThread.start();
//		}
//	}
//
//	private static class ThreadLocker
//	{
//		private int count = 0;
//		public double time;
//		
//		public synchronized void setCounter(int i)
//		{
//			this.count = i;
//		}
//				
//		public synchronized void incCounter()
//		{
//			count++;
//		}
//		
//		public synchronized void decCounter()
//		{
//			count--;
////			log.info("Count: " + count + ", Time: " + time);
//			if(count == 0)
//			{
////				log.info("Notify " + time);
//				notify();
//			}
//		}
//	}
//
//	/**
//	 * The thread class that really handles the links.
//	 */
//	private static class MoveLinkThread extends Thread
//	{
//		private static ThreadLocker threadLocker;		
//		private static double time = 0.0;
//		private static boolean simulationRunning = true;
//		private boolean simulateAllLinks = false;
//		
//		private List<QueueLink> links = new ArrayList<QueueLink>();
//				
//		private boolean isWaiting = true;
//
//		public MoveLinkThread(boolean simulateAllLinks)
//		{
//			this.simulateAllLinks = simulateAllLinks;
//		}
//
//		public static void setThreadLocker (ThreadLocker tl)
//		{
//			threadLocker = tl;
//		}
//		
//		public static void setTime(final double t)
//		{
//			time = t;
//		}
//		
//		public void handleLinks(List<QueueLink> links)
//		{
//			this.links = links;
//		}
//				
//		public void startReplanning()
//		{
//			synchronized(this)
//			{
//				notify();
//			}
//		}
//		
//		public boolean isWaiting()
//		{
//			return isWaiting;
//		}
//		
//		@Override
//		public void run()
//		{
//			while (simulationRunning)
//			{
//				try
//				{
//					/*
//					 * threadLocker.decCounter() and wait() have to be
//					 * executed in a synchronized block! Otherwise it could
//					 * happen, that the replanning ends and the replanning of
//					 * the next SimStep executes the notify command before
//					 * wait() is called -> we have a DeadLock!
//					 */
//					synchronized(this)
//					{
//						threadLocker.decCounter();
//						wait();
//						isWaiting = false;
//					}
//					
//					ListIterator<QueueLink> simLinks = this.links.listIterator();
//					QueueLink link;
//					boolean isActive;
//
//					while (simLinks.hasNext()) 
//					{
//						link = simLinks.next();
//						
//						isActive = link.moveLink(time);
//						
//						if (!isActive && !simulateAllLinks)
//						{
//							simLinks.remove();
//						}
//					}
//				}
//				catch (InterruptedException ie)
//				{
//					log.error("Something is going wrong here...");
//				}
//							
//				finally
//				{
//					isWaiting = true;
//				}
//			}	// while Simulation Running
//			
//		}	// run()
//		
//	}	// ReplannerThread
//	
//}