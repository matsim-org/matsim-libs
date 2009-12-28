///* *********************************************************************** *
// * project: org.matsim.*
// * ParallelMoveNodes4.java
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
//import java.util.concurrent.BrokenBarrierException;
//import java.util.concurrent.CyclicBarrier;
//
//import org.apache.log4j.Logger;
//import org.matsim.core.gbl.Gbl;
//import org.matsim.core.mobsim.queuesim.QueueNode;
//
//public class ParallelMoveNodes4 {
//	
//	private final static Logger log = Logger.getLogger(ParallelMoveNodes4.class);
//	
//	private MoveNodesThread[] moveNodesThreads;
//
//	private CyclicBarrier iterationBarrier;
//	private CyclicBarrier timeStepStartBarrier;
//	private CyclicBarrier timeStepEndBarrier;
//	
//	private int numOfThreads;
//	private int iterations;
//	private boolean simulateAllNodes;
//	private List<QueueNodeDependencies[][]> parallelSimNodesArray;
//		
//	public ParallelMoveNodes4(boolean simulateAllNodes)
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
//	public void init(List<List<QueueNodeDependencies>> simNodesArray, int numOfThreads)
//	{
//		this.numOfThreads = numOfThreads;
//		this.iterations = simNodesArray.size();
//		
//		this.timeStepStartBarrier = new CyclicBarrier(this.numOfThreads + 1);
//		this.timeStepEndBarrier = new CyclicBarrier(this.numOfThreads + 1);
//		this.iterationBarrier = new CyclicBarrier(this.numOfThreads);
//		
//		
//		/*
//		 * Structure of the parallelSimNodesArray
//		 * Outer List: one for each Thread
//		 * Middle Array: one for each Iteration
//		 * Inner Array: one for each QueueNodeDependencies
//		 */
//		parallelSimNodesArray = new ArrayList<QueueNodeDependencies[][]>();
//		
//		for (int thread = 0;  thread < this.numOfThreads; thread++)
//		{
//			QueueNodeDependencies[][] newList = new QueueNodeDependencies[iterations][];
//			parallelSimNodesArray.add(newList);
//		}
//
//		/*
//		 * For each Iteration of independent moveable Nodes.
//		 */
//		int iteration = 0;
//		int distributor = 0;
//		
//		// for each iteration
//		for (List<QueueNodeDependencies> list : simNodesArray)
//		{
//			List<List<QueueNodeDependencies>> tempList = new ArrayList<List<QueueNodeDependencies>>();
//			
//			for (int i = 0; i < this.numOfThreads; i++)
//			{
//				tempList.add(new ArrayList<QueueNodeDependencies>());
//			}
//			
//			// for each node	
//			for (QueueNodeDependencies qnd : list)
//			{
//				tempList.get(distributor % this.numOfThreads).add(qnd);
//				distributor++;
//			}
//			
//			// Copy the Lists to Arrays
//			for (int i = 0; i < this.numOfThreads; i++)
//			{
//				parallelSimNodesArray.get(i)[iteration] = new QueueNodeDependencies[tempList.get(i).size()];;
//				tempList.get(i).toArray(parallelSimNodesArray.get(i)[iteration]);
//			}
//			
//			iteration++;
//		}
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
//			moveNodesThread.setCyclicIterationBarrier(iterationBarrier);
//			moveNodesThread.setCyclicTimeStepStartBarrier(timeStepStartBarrier);
//			moveNodesThread.setCyclicTimeStepEndBarrier(timeStepEndBarrier);
//			
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
//		private QueueNodeDependencies[][] processableQueueNodes;	// [Iteration][Nodes]
//		private int iteration;
//		private int iterationCount;
//		
//		private CyclicBarrier iterationBarrier;
//		private CyclicBarrier timeStepStartBarrier;
//		private CyclicBarrier timeStepEndBarrier;
//		private boolean simulateAllNodes = false;
//		
//		public MoveNodesThread(boolean simulateAllNodes)
//		{
//			this.simulateAllNodes = simulateAllNodes;
//		}
//		
//		public void setProcessableQueueNodes(QueueNodeDependencies[][] processableQueueNodes)
//		{
//			this.processableQueueNodes = processableQueueNodes;
//			this.iterationCount = processableQueueNodes.length;
//		}
//		
//		public void setCyclicIterationBarrier(CyclicBarrier barrier)
//		{
//			this.iterationBarrier = barrier;
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
////					log.info("Starting..." + time + " " + this.getName());
//					
//					for (QueueNodeDependencies[] qnds : processableQueueNodes)
//					{
//						/*
//						 * As long as we get new QueueNodes to move...
//						 */
//						for(QueueNodeDependencies qnd : qnds)
//						{				
//							QueueNode node = qnd.getQueueNode();
//							if (node.isActive() || node.isSignalized() || simulateAllNodes)
//							{
//								qnd.getQueueNode().moveNode(time, qnd.getRandom());
//							}
//						}
//						
////						for (QueueNodeDependencies qnd : processableQueueNodes.get(iteration))
////						{
////							qnd.setMoved(true);
////							for (QueueNodeDependencies qnd2 : qnd.getOutNodes())
////							{
////								qnd2.incNodeProcessed();
////							}
////						}
//						
////						for (QueueNodeDependencies qnd : processableQueueNodes.get(iteration))
////						{
////							if (qnd.isMoveable())
////							{
////								qnd.setMoved(true);
////								for (QueueNodeDependencies qnd2 : qnd.getOutNodes())
////								{
////									qnd2.incNodeProcessed();
////								}
////							}
////							else
////							{
////								log.info(processableQueueNodes.get(iteration - 1).get(0).hasBeenMoved());
////								log.error("Node is not moveable?!" + iteration);
////							}						
////						}
//						
////						// in the next loop the next iteration will be processed
////						iteration++;
////						
////						if (iteration != iterationCount)
////						{
////							iterationBarrier.await();		
////						}
////						// It is the last Iteration - reset Counter 
////						else iteration = 0;
//
//					}	// for all Iterations
////					log.info("... done" + time + " " + this.getName());
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