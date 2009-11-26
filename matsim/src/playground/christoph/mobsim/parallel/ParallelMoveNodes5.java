///* *********************************************************************** *
// * project: org.matsim.*
// * ParallelMoveNodes5.java
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
//
//import org.apache.log4j.Logger;
//import org.matsim.core.gbl.Gbl;
//import org.matsim.core.mobsim.queuesim.QueueNode;
//
//public class ParallelMoveNodes5 {
//	
//	private final static Logger log = Logger.getLogger(ParallelMoveNodes2.class);
//	
//	private static MoveNodesThread[] moveNodesThreads;
//	private static ThreadLocker threadLocker;
//	
//	private int numOfThreads;
////	private QueueNodeDependencies[] nodesArray;
//	private List<QueueNodeDependencies[]> parallelSimNodesArray;
//		
//	public ParallelMoveNodes5()
//	{
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
////		for (List<QueueNodeDependencies> list : parallelSimNodesArray)
////		{
////			for (QueueNodeDependencies qnd : nodesArray)
////			{
////				QueueNode node = qnd.getQueueNode();
////				if (node.isActive() || node.isSignalized())
////				{
////					qnd.getQueueNode().moveNode(time, qnd.getRandom());
////				}
////			}
////		}
//		
//		try
//		{
//			// lock threadLocker until wait() statement listens for the notifies
//			synchronized(threadLocker) 
//			{
//				// set current Time
//				MoveNodesThread.setTime(time);
//				threadLocker.time = time;
//					
//				threadLocker.setCounter(moveNodesThreads.length);
//				
//				for (MoveNodesThread moveNodesThread : moveNodesThreads)
//				{
//					moveNodesThread.startMoving();		
//				}
//				
//				threadLocker.wait();
//
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
//		boolean stillMoving = false;
//		// if moving Nodes is finished in all Threads
//		for (MoveNodesThread moveNodesThread : moveNodesThreads)
//		{
//			if (!moveNodesThread.isWaiting())
//			{
//				stillMoving = true;
//				break;
//			}			
//		}
//		return stillMoving;
//	}
//
//	public void init(List<QueueNodeDependencies> simNodesArray, int numOfThreads, boolean simulateAllNodes)
//	{
//		this.numOfThreads = numOfThreads;
//
//		parallelSimNodesArray = new ArrayList<QueueNodeDependencies[]>();
//
////		nodesArray = new QueueNodeDependencies[simNodesArray.size()];
////		simNodesArray.toArray(nodesArray);
//		
//		List<List<QueueNodeDependencies>> lists = new ArrayList<List<QueueNodeDependencies>>();
//		for (int i = 0; i < this.numOfThreads; i++)
//		{
//			lists.add(new ArrayList<QueueNodeDependencies>());
//		}
//		
//		// for each node
//		int distributor = 0;	
//		for (QueueNodeDependencies qnd : simNodesArray)
//		{
//			lists.get(distributor % this.numOfThreads).add(qnd);
//			distributor++;
//		}
//		
//		for (int i = 0; i < this.numOfThreads; i++)
//		{
//			QueueNodeDependencies[] nodesArray = new QueueNodeDependencies[lists.get(i).size()];
//			parallelSimNodesArray.add(lists.get(i).toArray(nodesArray));
//		}
//		
//		threadLocker = new ThreadLocker();
//		
//		MoveNodesThread.setThreadLocker(threadLocker);
//		
//		moveNodesThreads = new MoveNodesThread[this.numOfThreads];
//		
//		// setup threads
//		for (int i = 0; i < this.numOfThreads; i++) 
//		{
//			MoveNodesThread moveNodesThread = new MoveNodesThread(simulateAllNodes);
//			moveNodesThread.setName("MoveNodes" + i);
//			moveNodesThread.setDaemon(true);	// make the Threads Daemons so they will terminate automatically
//			moveNodesThread.setProcessableQueueNodes(parallelSimNodesArray.get(i));
//			
//			moveNodesThreads[i] = moveNodesThread;
//			
//			moveNodesThread.start();
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
//	private static class MoveNodesThread extends Thread
//	{
//		private static ThreadLocker threadLocker;
//		private static double time = 0.0;
//		private static boolean simulationRunning = true;
//
//		private QueueNodeDependencies[] processableQueueNodes;
//		
//		private boolean isWaiting = true;
//		private boolean simulateAllNodes = false;
//		
//		public MoveNodesThread(boolean simulateAllNodes)
//		{
//			this.simulateAllNodes = simulateAllNodes;
//		}
//		
//		public void setProcessableQueueNodes(QueueNodeDependencies[] processableQueueNodes)
//		{
//			this.processableQueueNodes = processableQueueNodes;
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
//		public void startMoving()
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
//					/*
//					 * As long as we get new QueueNodes to move...
//					 */
//					for(QueueNodeDependencies qnd : processableQueueNodes)
//					{			
//						QueueNode node = qnd.getQueueNode();
//						if (node.isActive() || node.isSignalized() || simulateAllNodes)
//						{
//							qnd.getQueueNode().moveNode(time, qnd.getRandom());
//						}
//					}
//					
//				}
//				catch (InterruptedException ie)
//				{
//					log.error("Something is going wrong here...");
//				}		
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