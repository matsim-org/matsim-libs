///* *********************************************************************** *
// * project: org.matsim.*
// * ParallelMoveNodes.java
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
//
//import org.apache.log4j.Logger;
//import org.matsim.core.gbl.Gbl;
//import org.matsim.core.mobsim.queuesim.QueueNode;
//
//public class ParallelMoveNodes {
//	
//	private final static Logger log = Logger.getLogger(ParallelMoveNodes.class);
//	
//	private static MoveNodesThread[] moveNodesThreads;
//	private static ThreadLocker threadLocker;
//	private QueueNodeDependencies[] queueNodeDependencies;
//	
//	int counter;
//	
//	private LinkedList<QueueNodeDependencies> moveableNodes;
//		
//	public ParallelMoveNodes()
//	{
//		moveableNodes = new LinkedList<QueueNodeDependencies>();
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
//		counter = 0;
//		
//		try
//		{
//			// lock threadLocker until wait() statement listens for the notifies
//			synchronized(threadLocker) 
//			{
//				/*
//				 * Find all Nodes that are already movable.
//				 * They can be moved right away from the beginning.
//				 */
//				for (QueueNodeDependencies qnd : queueNodeDependencies)
//				{
//					if(qnd.isMoveable())
//					{
//						moveableNodes.add(qnd);
//					}
//				}
//				
//				// set current Time
//				MoveNodesThread.setTime(time);
//							
//				threadLocker.setCounter(moveNodesThreads.length);
//				threadLocker.time = time;
//								
//				for (MoveNodesThread moveNodesThread : moveNodesThreads)
//				{
//					moveNodesThread.startMoving();		
//				}
//				
//				threadLocker.wait();
//			}		
//		} 
//		catch (InterruptedException e)
//		{
//			Gbl.errorMsg(e);
//		}
////		log.info("Counter: " + counter);
//	}
//	
//	
//	/*
//	 * We handle the moveableNodes only in this Method, so we don't
//	 * have to care about Race Conditions!
//	 */
//	protected synchronized QueueNodeDependencies getNextQueueNodeDependencies(MoveNodesThread moveNodesThread)
//	{
//		/*
//		 * We now the given moveNodesThread is currently waiting
//		 * for a new QueueNode so we can retrieve the processable
//		 * QueueNodes from its list without concerning about
//		 * Race Conditions.
//		 */
//		moveableNodes.addAll(moveNodesThread.getProcessableQueueNodes());
//		
////		log.info(moveNodesThread.getName() + ": Added " + moveNodesThread.getProcessableQueueNodes().size() + " new processable Nodes.");
//		counter = counter + moveNodesThread.getProcessableQueueNodes().size();
//		moveNodesThread.getProcessableQueueNodes().clear();
//		
////		log.info("moveable nodes: " + moveableNodes.size());
//		
//		/*
//		 * We probably got new Nodes to process so we notify
//		 * the Threads in case they are waiting for new Nodes. 
//		 */
//		// [TODO] Not sure if this call could cause Race Conditions... Check it!
////		for (MoveNodesThread thread : moveNodesThreads)
////		{
////			if (thread != moveNodesThread && thread.isWaiting)
////			{
////				threadLocker.incCounter();
////				thread.startMoving();
////			}
////		}
//		
//		return moveableNodes.poll();
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
//	public void init(QueueNodeDependencies[] queueNodeDependencies, int numOfThreads, boolean simulateAllNodes)
//	{
//		this.queueNodeDependencies = queueNodeDependencies;
//		
//		threadLocker = new ThreadLocker();
//		
//		MoveNodesThread.setThreadLocker(threadLocker);
//		
//		moveNodesThreads = new MoveNodesThread[numOfThreads];
//		
//		// setup threads
//		for (int i = 0; i < numOfThreads; i++) 
//		{
//			MoveNodesThread moveNodeThread = new MoveNodesThread(simulateAllNodes, this);
//			moveNodeThread.setName("MoveNodes" + i);
//			moveNodeThread.setDaemon(true);	// make the Threads Daemons so they will terminate automatically
//			moveNodesThreads[i] = moveNodeThread;
//			
//			moveNodeThread.start();
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
//		private ParallelMoveNodes parallelMoveNodes;
//		private List<QueueNodeDependencies> processableQueueNodes = new ArrayList<QueueNodeDependencies>();
//
//		private QueueNodeDependencies queueNodeDependencies;
//		private boolean isWaiting = true;
//		private boolean simulateAllNodes = false;
//		
//		public MoveNodesThread(boolean simulateAllNodes, ParallelMoveNodes parallelMoveNodes)
//		{
//			this.simulateAllNodes = simulateAllNodes;
//			this.parallelMoveNodes = parallelMoveNodes;
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
//		public List<QueueNodeDependencies> getProcessableQueueNodes()
//		{
//			return this.processableQueueNodes;
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
////						log.info("waiting...");
//						wait();
//						isWaiting = false;
//					}
////					log.info("running...");
//					
//					/*
//					 * As long as we get new QueueNodes to move...
//					 */
//					while ((queueNodeDependencies = parallelMoveNodes.getNextQueueNodeDependencies(this)) != null)
//					{
////						log.info("Moving Node...");
////						log.info(queueNodeDependencies.getQueueNode().getNode().getId());
//						
//						/*
//						 * Node is moveable. Now check the criteria from 
//						 * the original QueueSimEngine.
//						 */
//						QueueNode node = queueNodeDependencies.getQueueNode();
//						if (node.isActive() || node.isSignalized() || simulateAllNodes || !queueNodeDependencies.hasBeenMoved())
//						{
//							queueNodeDependencies.getQueueNode().moveNode(time, queueNodeDependencies.getRandom());
//						}
//						
//						// Set the moved Flag anyway!
//						queueNodeDependencies.setMoved(true);
//						
//						/*
//						 * Get all moveable inNodes that were not allowed to be processed 
//						 * before this Node was. Add them to the processable List now.
//						 */
//						for (QueueNodeDependencies qnd : queueNodeDependencies.getPostProcessedInNodes())
//						{
//							synchronized(qnd)
//							{
//								if (qnd.isMoveable())
//								{
//									processableQueueNodes.add(qnd);
//									
//									// hack: no other Thread will think the Node is processable
//									qnd.incNodeProcessed();
//								}
//							}
//						}
//						
//						/*
//						 * For all OutNodes:
//						 * Maybe they are now processable so check for that and
//						 * if its true, add them to the list.
//						 */
//						for (QueueNodeDependencies qnd : queueNodeDependencies.getOutNodes())
//						{
//							synchronized(qnd)
//							{
//								qnd.incNodeProcessed();
//								
//								if (qnd.isMoveable())
//								{
//									processableQueueNodes.add(qnd);
//									
//									// hack: no other Thread will think the Node is processable
//									qnd.incNodeProcessed();
//								}	
//							}
//						}
//					}
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