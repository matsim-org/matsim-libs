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
//import java.util.concurrent.BrokenBarrierException;
//import java.util.concurrent.CyclicBarrier;
//
//import org.apache.log4j.Logger;
//import org.matsim.core.gbl.Gbl;
//import org.matsim.core.mobsim.queuesim.QueueLink;
//
//public class ParallelMoveLinks2 {
//	
//	private final static Logger log = Logger.getLogger(ParallelMoveLinks2.class);
//	
//	private MoveLinkThread[] moveLinksThreads;
//	private ThreadLocker threadLocker;
//	private CyclicBarrier timeStepBarrier;
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
//	public void init(List<List<QueueLink>> linkLists)
//	{
//		this.numOfThreads = linkLists.size();
//		
//		this.threadLocker = new ThreadLocker();
//
//		this.timeStepBarrier = new CyclicBarrier(this.numOfThreads, threadLocker);
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
//			moveLinkThread.setCyclicTimeStepBarrier(this.timeStepBarrier);
//			moveLinksThreads[i] = moveLinkThread;
//			
//			moveLinkThread.start();
//		}
//	}
//
//	/*
//	 * If all MoveNodeThreads arrive the TimeStepBarrier
//	 * this run method will be called and the Main Thread
//	 * that is waiting until its ThreadLocker Object is
//	 * notified wakes up.
//	 */
//	private static class ThreadLocker implements Runnable
//	{		
//		public synchronized void run()
//		{
//			notify();
//		}
//	}
//
//	/**
//	 * The thread class that really handles the links.
//	 */
//	private static class MoveLinkThread extends Thread
//	{
//		private static double time = 0.0;
//		private static boolean simulationRunning = true;
//		private boolean simulateAllLinks = false;
//		
//		private List<QueueLink> links = new ArrayList<QueueLink>();
//				
//		private CyclicBarrier timeStepBarrier;
//
//		public MoveLinkThread(boolean simulateAllLinks)
//		{
//			this.simulateAllLinks = simulateAllLinks;
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
//		public void setCyclicTimeStepBarrier(CyclicBarrier barrier)
//		{
//			this.timeStepBarrier = barrier;
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
//		@Override
//		public void run()
//		{
//			while (simulationRunning)
//			{
//				try
//				{
//					/*
//					 * timeStepBarrier.await() and wait() have to be
//					 * executed in a synchronized block! Otherwise it could
//					 * happen, that the moving ends and the moving of
//					 * the next SimStep executes the notify command before
//					 * wait() is called -> we have a DeadLock!
//					 */
//					synchronized(this)
//					{
//						timeStepBarrier.await();
//						
//						wait();
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
//	            catch (BrokenBarrierException e)
//	            {
//	                e.printStackTrace();
//	            }
//			}	// while Simulation Running
//			
//		}	// run()
//		
//	}	// ReplannerThread
//	
//}