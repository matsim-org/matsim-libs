///* *********************************************************************** *
// * project: org.matsim.*
// * ParallelMoveLinks.java
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
//import java.util.LinkedList;
//import java.util.List;
//import java.util.ListIterator;
//
//import org.apache.log4j.Logger;
//import org.matsim.core.gbl.Gbl;
//import org.matsim.core.mobsim.queuesim.QueueLink;
//
//public class ParallelMoveLinks {
//	
//	private final static Logger log = Logger.getLogger(ParallelMoveLinks.class);
//	
//	private static MoveLinkThread[] moveLinksThreads;
//	private static ThreadLocker threadLocker;
//	private boolean simulateAllLinks = false;
//	private int numOfThreads;
//	
//	public ParallelMoveLinks(boolean simulateAllLinks)
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
//	public void run(List<QueueLink> links, double time)
//	{	
//		List<List<QueueLink>> subLists = new ArrayList<List<QueueLink>>();
//		
//		int subListSize = links.size() /  numOfThreads;
//	
//		for (int i = 0; i < numOfThreads; i++)
//		{
//			if ((i + 1) < numOfThreads)
//			{
//				List<QueueLink> subList = links.subList(i * subListSize, (i + 1) * subListSize);
////				List<QueueLink> subList = new ArrayList<QueueLink>();
////				subList.addAll(links.subList(i * subListSize, (i + 1) * subListSize));
////				subLists.add(subList);
//				moveLinksThreads[i].handleLinks(subList);
//			}
//			// its the last thread, so add all remaining links!
//			else
//			{
//				List<QueueLink> subList = links.subList(i * subListSize, links.size());
////				List<QueueLink> subList = new ArrayList<QueueLink>();
////				subList.addAll(links.subList(i * subListSize, links.size()));
////				subLists.add(subList);
//				moveLinksThreads[i].handleLinks(subList);
//			}
//		}
//
////		for (int i = 0; i < numOfThreads; i++)
////		{
////			subLists.add(new ArrayList<QueueLink>());
////			moveLinksThreads[i].handleLinks(subLists.get(i));
////		}
////		int i = 0;
////		for(QueueLink link : links)
////		{
////			subLists.get(i % numOfThreads).add(link);
////			i++;
////		}
//		
//		// distribute workload between threads
//		// as long as threads are waiting we don't need synchronized data structures
////		for(int i = 0; i < links.size(); i++)
////		{
////			QueueLink link = links.get(i);
////
////			replannerThreads[i % numOfThreads].handleLink(link);
////		}
//
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
//		
//		/*
//		 * Finally remove all inActive Links from the List
//		 * Could probably be solved in a better (parallel!) way...
//		 */
////		for (MoveLinkThread thread : moveLinksThreads)
////		{
////			links.removeAll(thread.getInActiveLinks());
////		}
//		for (MoveLinkThread thread : moveLinksThreads)
//		{
//			LinkedList<QueueLink> inActiveLinks = thread.getInActiveLinks();
//			while(inActiveLinks.peek() != null)
//			{
//				links.remove(inActiveLinks.poll());
//			}
//		}
//
//		/*
//		 *  This seems to be faster than removing the inactive 
//		 *  Links one by one from the List.
//		 *  
//		 *  [TODO] Still quite a big hack. Ideas to improve?
//		 *  Something like a ConcurrentList that can be edited
//		 *  from within the Threads?
//		 */
////		links.clear();
////		for(List<QueueLink> list : subLists)
////		{
////			links.addAll(list);
////		}
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
//	public void init(int numOfThreads)
//	{
//		this.numOfThreads = numOfThreads;
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
//		private final LinkedList<QueueLink> inActiveLinks = new LinkedList<QueueLink>();
////		private LinkedList<QueueLink> links = new LinkedList<QueueLink>();
//		private List<QueueLink> links2 = new ArrayList<QueueLink>();
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
//			this.links2 = links;
//		}
//		
////		public void handleLink(final QueueLink link)
////		{
////			this.links.add(link);
////		}
//
//		public LinkedList<QueueLink> getInActiveLinks()
//		{
//			return this.inActiveLinks;
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
//					ListIterator<QueueLink> simLinks = this.links2.listIterator();
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
//							inActiveLinks.add(link);
////							simLinks.remove();
//						}
//					}
//					
////					while(links.peek() != null)
////					{
////						QueueLink link = links.poll();
////						
////						boolean isActive = link.moveLink(time);
////						if (!isActive && !simulateAllLinks)
////						{
////							inActiveLinks.add(link);
////						}
////					}
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