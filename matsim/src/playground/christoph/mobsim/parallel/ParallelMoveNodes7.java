///* *********************************************************************** *
// * project: org.matsim.*
// * ParallelMoveNodes7.java
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
//import java.util.List;
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.LinkedBlockingQueue;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import org.apache.log4j.Logger;
//import org.matsim.core.mobsim.queuesim.QueueNode;
//
//public class ParallelMoveNodes7 {
//	
//	private final static Logger log = Logger.getLogger(ParallelMoveNodes7.class);
//	
//	private MoveNodesThread[] moveNodesThreads;
//	private boolean simulateAllNodes = false;
//	private int numOfThreads;
//	
//	private AtomicInteger nodeCounter;
//	private int totalNodes;
//	
//	private BlockingQueue<QueueNodeDependencies3> queue;
//	private List<QueueNodeDependencies3> initialList;
//	
//	public ParallelMoveNodes7(boolean simulateAllNodes)
//	{
//		this.simulateAllNodes = simulateAllNodes;
//	}
//
//	/*
//	 * The Threads are waiting at the TimeStepStartBarrier.
//	 * We trigger them by reaching this Barrier. Now the
//	 * Threads will start moving the Nodes. We wait until
//	 * all of them reach the TimeStepEndBarrier to move on.
//	 * We should not have any Problems with Race Conditions
//	 * because even if the Threads would be faster than this
//	 * Thread, means the reach the TimeStepEndBarrier before
//	 * this Method does, it should work anyway.
//	 */
//	public void run(double time, List<QueueNodeDependencies3> additionalNodes)
//	{	
//		if (queue.size() > 0) log.error("Queue is not empty!");
//		
//		/*
//		 *  When adding the initial Nodes the Threads will automatically
//		 *  start to process them because they are waiting for new Elements
//		 *  in the Queue.
//		 */
//		try
//		{
//			synchronized(this)
//			{				
////				log.info("Starting..." + initialList.size());
////				log.info("Adding additional initial Nodes: " + additionalNodes.size());
//				nodeCounter.set(0);
//				queue.addAll(initialList);
//				queue.addAll(additionalNodes);
//				this.wait();
//			}
//		} 
//		catch (InterruptedException e)
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
//	
//	public void init(List<QueueNodeDependencies3> initialNodesList, int totalNodes, int numOfThreads)
//	{
//		this.initialList = initialNodesList;
//		this.totalNodes = totalNodes;
//		this.numOfThreads = numOfThreads;
//		
//		moveNodesThreads = new MoveNodesThread[numOfThreads];		
//		queue = new LinkedBlockingQueue<QueueNodeDependencies3>();
//		nodeCounter = new AtomicInteger(0);
//		
//		// setup threads
//		for (int i = 0; i < numOfThreads; i++) 
//		{
//			MoveNodesThread moveNodesThread = new MoveNodesThread(simulateAllNodes);
//			moveNodesThread.setName("MoveNodes" + i);
//			moveNodesThread.setBlockingQueue(queue);
//			moveNodesThread.setDaemon(true);	// make the Thread Daemons so they will terminate automatically
//			moveNodesThread.setNodeCounter(nodeCounter);
//			moveNodesThread.setTotalNodes(totalNodes);
//			moveNodesThread.setMainThread(this);
//			moveNodesThreads[i] = moveNodesThread;
//			
//			moveNodesThread.start();
//		}
//	}
//	
//	/**
//	 * The thread class that really handles the links.
//	 */
//	private static class MoveNodesThread extends Thread
//	{
//		private static double time = 0.0;
//		private static boolean simulationRunning = true;
//		private AtomicInteger nodeCounter;
//		private int totalNodes;
//		private boolean simulateAllNodes = false;
//		private Object mainThread;		
//		private BlockingQueue<QueueNodeDependencies3> queue;
//		
//		public MoveNodesThread(boolean simulateAllNodes)
//		{
//			this.simulateAllNodes = simulateAllNodes;
//		}
//		
//		public void setBlockingQueue(BlockingQueue<QueueNodeDependencies3> queue)
//		{
//			this.queue = queue;
//		}
//		
//		public void setNodeCounter(AtomicInteger nodeCounter)
//		{
//			this.nodeCounter = nodeCounter;
//		}
//		
//		public void setTotalNodes(int totalNodes)
//		{
//			this.totalNodes = totalNodes;
//		}
//		
//		public void setMainThread(Object mainThread)
//		{
//			this.mainThread = mainThread;
//		}
//		
//		public static void setTime(final double t)
//		{
//			time = t;
//		}
//				
//		@Override
//		public void run()
//		{
//			int counter = 0;
//			
//			while (simulationRunning)
//			{
//				try
//				{
////					log.info("Moving Node..." + counter++ + " " + nodeCounter.intValue());
//					QueueNodeDependencies3 qnd = queue.take();
//					
//					QueueNode node = qnd.getQueueNode();
//					if (node.isActive() || node.isSignalized() || simulateAllNodes)
//					{
//						node.moveNode(time, qnd.getRandom());
//					}
//					
//					for (QueueNodeDependencies3 child : qnd.getPostProcessedNodes())
//					{
//						child.incProcessedNodes();
//						if (child.isMoveable()) queue.put(child);
//					}
//					
//					/*
//					 *  If we processed the last Node in this Iteration notify the
//					 *  Main Thread.
//					 */
//					if (nodeCounter.incrementAndGet() == totalNodes)
//					{
////						log.info("Done - trying to resume Main Thread " + nodeCounter.intValue());
//						synchronized(mainThread)
//						{
//							mainThread.notify();
//						}
//					}
//				}
//				catch (InterruptedException ie)
//				{
//					log.error("Something is going wrong here...");
//				}
//			}	// while Simulation Running
//			
//		}	// run()
//		
//	}	// ReplannerThread
//	
//}