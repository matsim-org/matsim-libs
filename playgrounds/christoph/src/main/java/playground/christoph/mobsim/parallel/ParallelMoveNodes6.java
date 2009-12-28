///* *********************************************************************** *
// * project: org.matsim.*
// * ParallelMoveNodes6.java
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
//import java.util.Random;
//import java.util.concurrent.BrokenBarrierException;
//import java.util.concurrent.CyclicBarrier;
//
//import org.apache.log4j.Logger;
//import org.matsim.core.gbl.Gbl;
//import org.matsim.core.gbl.MatsimRandom;
//import org.matsim.core.mobsim.queuesim.QueueNode;
//
//public class ParallelMoveNodes6 {
//	
//	private final static Logger log = Logger.getLogger(ParallelMoveNodes6.class);
//	
//	private MoveNodesThread[] moveNodesThreads;
//	private CyclicBarrier timeStepStartBarrier;
//	private CyclicBarrier timeStepEndBarrier;
//	private boolean simulateAllNodes = false;
//	private int numOfThreads;
//		
//	public ParallelMoveNodes6(boolean simulateAllNodes)
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
//	public void run(double time)
//	{			
//		try
//		{
//			// set current Time
//			MoveNodesThread.setTime(time);
//			
//			this.timeStepStartBarrier.await();
//			
//			this.timeStepEndBarrier.await();				
//		} 
//		catch (InterruptedException e)
//		{
//			Gbl.errorMsg(e);
//		}
//        catch (BrokenBarrierException e)
//        {
//        	Gbl.errorMsg(e);
//        }
//	}
//	
//	public void init(List<List<QueueNode>> nodeLists)
//	{
//		this.numOfThreads = nodeLists.size();
//		
//		this.timeStepStartBarrier = new CyclicBarrier(this.numOfThreads + 1);
//		this.timeStepEndBarrier = new CyclicBarrier(this.numOfThreads + 1);
//				
//		moveNodesThreads = new MoveNodesThread[numOfThreads];
//		
//		// setup threads
//		for (int i = 0; i < numOfThreads; i++) 
//		{
//			MoveNodesThread moveNodesThread = new MoveNodesThread(simulateAllNodes);
//			moveNodesThread.setName("MoveNodes" + i);
//			moveNodesThread.handleNodes(nodeLists.get(i));
//			moveNodesThread.setDaemon(true);	// make the Thread Daemons so they will terminate automatically
//			moveNodesThread.setCyclicTimeStepStartBarrier(this.timeStepStartBarrier);
//			moveNodesThread.setCyclicTimeStepEndBarrier(this.timeStepEndBarrier);
//			moveNodesThreads[i] = moveNodesThread;
//			
//			moveNodesThread.start();
//		}
//		
//		/*
//		 * After initialization the Threads are waiting at the
//		 * TimeStepEndBarrier. We trigger this Barrier once so 
//		 * they wait at the TimeStepStartBarrier what has to be
//		 * their state if the run() method is called.
//		 */
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
//	}
//	
//	/**
//	 * The thread class that really handles the links.
//	 */
//	private static class MoveNodesThread extends Thread
//	{
//		private static double time = 0.0;
//		private static boolean simulationRunning = true;
//
//		private List<QueueNode> nodes = new ArrayList<QueueNode>();
//		private QueueNode[] nodesArray;
//		
//		private CyclicBarrier timeStepStartBarrier;
//		private CyclicBarrier timeStepEndBarrier;
//		private boolean simulateAllNodes = false;
//		private Random random = MatsimRandom.getRandom();
//		
//		public MoveNodesThread(boolean simulateAllNodes)
//		{
//			this.simulateAllNodes = simulateAllNodes;
//		}
//		
//		public void handleNodes(List<QueueNode> nodes)
//		{
//			this.nodes = nodes;
//			nodesArray = new QueueNode[nodes.size()];
//			nodes.toArray(nodesArray);
//		}
//
//		public void setCyclicTimeStepStartBarrier(CyclicBarrier barrier)
//		{
//			this.timeStepStartBarrier = barrier;
//		}
//		
//		public void setCyclicTimeStepEndBarrier(CyclicBarrier barrier)
//		{
//			this.timeStepEndBarrier = barrier;
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
//			while (simulationRunning)
//			{
//				try
//				{
//					/*
//					 * The End of the Node Moving is synchronized with 
//					 * the TimeStepEndBarrier. If all Threads reach this Barrier
//					 * the main run() Thread can go on.
//					 * 
//					 * The Threads wait now at the TimeStepStartBarrier until
//					 * they are triggered again in the next TimeStep by the main run()
//					 * method.
//					 */
//					timeStepEndBarrier.await();
//
//					timeStepStartBarrier.await();
//					
//					for (QueueNode node : nodesArray)
//					{
//						if (node.isActive() || node.isSignalized() || simulateAllNodes)
//						{
//							node.moveNode(time, random);
//						}
//					}
//					
////					ListIterator<QueueNode> simNodes = this.nodes.listIterator();
////					QueueNode node;
////
////					while (simNodes.hasNext()) 
////					{
////						node = simNodes.next();
////						
////						if (node.isActive() || node.isSignalized() || simulateAllNodes)
////						{
////							node.moveNode(time, random);
////						}
////					}
//
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