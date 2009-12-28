///* *********************************************************************** *
// * project: org.matsim.*
// * ParallelMoveNodes8.java
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
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.TreeMap;
//import java.util.concurrent.BrokenBarrierException;
//import java.util.concurrent.CyclicBarrier;
//
//import org.apache.log4j.Logger;
//import org.matsim.api.basic.v01.Id;
//import org.matsim.api.core.v01.network.Link;
//import org.matsim.api.core.v01.network.Node;
//import org.matsim.core.gbl.Gbl;
//import org.matsim.core.gbl.MatsimRandom;
//import org.matsim.core.mobsim.queuesim.QueueNetwork;
//import org.matsim.core.mobsim.queuesim.QueueNode;
//
//public class ParallelMoveNodes8 {
//	
//	private final static Logger log = Logger.getLogger(ParallelMoveNodes8.class);
//	
//	private MoveNodesThread[] moveNodesThreads;
//	private boolean simulateAllNodes = false;
//	private CyclicBarrier timeStepStartBarrier;
//	private CyclicBarrier timeStepEndBarrier;
//	private int numOfThreads;
//		
//	private List<QueueNodeDependencies3> initialDependencies;
//	private List<QueueNodeDependencies3> allDependencies;
//	
//	private QueueNodeDependencies3[][] parallelArrays;
//	private QueueNodeDependencies3[] remainingArray;
//	
////	private RemainingNodesRunnable remainingNodesRunnable;
//	
//	public ParallelMoveNodes8(boolean simulateAllNodes)
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
////		for (QueueNodeDependencies3[] array : parallelArrays)
////		{
////			for (QueueNodeDependencies3 qnd : array)
////			{												
////				QueueNode node = qnd.getQueueNode();
////				if (node.isActive() || node.isSignalized() || simulateAllNodes)
////				{
////					node.moveNode(time, qnd.getRandom());
////				}
////			}
////		}
////		
////		moveRemainingNodes(time);
//		
//		
//		/*
//		 *  When adding the initial Nodes the Threads will automatically
//		 *  start to process them because they are waiting for new Elements
//		 *  in the Queue.
//		 */
//		try
//		{
//			// set current Time
//			for (MoveNodesThread moveNodesThread : moveNodesThreads) moveNodesThread.setTime(time);
//			
//			this.timeStepStartBarrier.await();
//			
//			this.timeStepEndBarrier.await();
//				
//			moveRemainingNodes(time);
//		
//		} 
//		catch (InterruptedException e)
//		{
//			Gbl.errorMsg(e);
//		}
//	    catch (BrokenBarrierException e)
//	    {
//	    	Gbl.errorMsg(e);
//	    }
//	}
//	
//	private void createArrays()
//	{			
//		for (QueueNodeDependencies3 qnd : allDependencies)
//		{
//			qnd.identifyPostProcessableChildren();
//		}
//		
//		List<QueueNodeDependencies3> availableNodes = new ArrayList<QueueNodeDependencies3>();
//		availableNodes.addAll(this.allDependencies);
//		
//		int counter = 0;
//		List<List<QueueNodeDependencies3>> initialNodes = new ArrayList<List<QueueNodeDependencies3>>();
//
//		for (int i = 0; i < numOfThreads; i++)
//		{
//			initialNodes.add(new ArrayList<QueueNodeDependencies3>());
//		}
//		
//		int roundRobin = 0;
//		for (QueueNodeDependencies3 qnd : this.initialDependencies)
//		{
//			initialNodes.get(roundRobin % numOfThreads).add(qnd);
//			availableNodes.remove(qnd);
//			counter++;
//			
//			initialNodes.get(roundRobin % numOfThreads).addAll(qnd.getPostProcessableChildren());
//			availableNodes.removeAll(qnd.getPostProcessableChildren());
//			counter = counter + qnd.getPostProcessableChildren().size();
//			roundRobin++;
//		}
//		
//		/*
//		 * Iterate over all Lists (one List per Thread)
//		 */
//		for (List<QueueNodeDependencies3> list : initialNodes)
//		{
//			/*
//			 * Iterate over the Nodes in the List as long as new Nodes are found.
//			 */
//			/*
//			 * Now we identify Children that depend on more than one parent but
//			 * all of them are contained in this initial Nodes' List.
//			 * We have to do this in a loop because maybe some of the identified
//			 * Nodes depend on each other.
//			 */
//			boolean foundNewNodes;
//			do
//			{
//				foundNewNodes = false;
//				List<QueueNodeDependencies3> newList = new ArrayList<QueueNodeDependencies3>();
//				for (QueueNodeDependencies3 qnd : list)
//				{						
//						List<QueueNodeDependencies3> multiChildren = qnd.getPostProcessedNodes();
//						for (QueueNodeDependencies3 qnd2 : multiChildren)
//						{
//							// If the Node is already contained in the list skip it
//							if (list.contains(qnd2) || newList.contains(qnd2)) continue;
//							
//							boolean addableNode = true;
//							for (QueueNodeDependencies3 qnd3 : qnd2.getPreProcessedNodes())
//							{
//								if (!list.contains(qnd3) && !newList.contains(qnd3))
//								{
//									addableNode = false;
//									break;
//								}
//							}
//							if (addableNode)
//							{
//								newList.add(qnd2);
//								availableNodes.remove(qnd2);
//								foundNewNodes = true;
//								counter++;
//							}
//							
//						}	// for all Children
//						
//					}	// for all Nodes in the List
//			
//	//			log.info("Loop..." + availableNodes.size());
//		
////				availableNodes.removeAll(newList);
//				list.addAll(newList);
//				
//			}
//			while (foundNewNodes);
//
//		}
//		log.info("Total processed Nodes " + counter);
//		log.info("Remaining Nodes " + availableNodes.size());
//		
//		/*
//		 * Now we create Arrays out of our Lists because iterating over them
//		 * is much faster.
//		 * Additionally we correct the Order of the Nodes to respect their Dependencies.
//		 * We do this by creating a copy of the original Nodes List (which is ordered)
//		 * and remove all Elements that are not in our List.
//		 */
//		parallelArrays = new QueueNodeDependencies3[this.numOfThreads][];
//		for (int i = 0; i < initialNodes.size(); i++)
//		{
//			List<QueueNodeDependencies3> list = initialNodes.get(i);
//			
//			List<QueueNodeDependencies3> clone = new ArrayList<QueueNodeDependencies3>();
//			clone.addAll(allDependencies);
//			
//			Iterator<QueueNodeDependencies3> iter = clone.iterator();
//			while (iter.hasNext())
//			{
//				QueueNodeDependencies3 qnd = iter.next();
//				if (!list.contains(qnd)) iter.remove();
//			}
//			
//			QueueNodeDependencies3[] array = new QueueNodeDependencies3[clone.size()];
//			clone.toArray(array);
//			parallelArrays[i] = array;
//		}
//		
//		List<QueueNodeDependencies3> clone = new ArrayList<QueueNodeDependencies3>();
//		clone.addAll(allDependencies);
//		
//		Iterator<QueueNodeDependencies3> iter = clone.iterator();
//		while (iter.hasNext())
//		{
//			QueueNodeDependencies3 qnd = iter.next();
//			if (!availableNodes.contains(qnd)) iter.remove();
//		}
//		remainingArray = new QueueNodeDependencies3[clone.size()];
//		clone.toArray(remainingArray);
//	}
//	
//	/*
//	 * Create equal sized Arrays and ignore the Dependencies.
//	 */
//	private void createEqualArrays()
//	{	
//		List<List<QueueNodeDependencies3>> initialNodes = new ArrayList<List<QueueNodeDependencies3>>();
//		for (int i = 0; i < numOfThreads; i++)
//		{
//			initialNodes.add(new ArrayList<QueueNodeDependencies3>());
//		}
//		
//		int roundRobin = 0;
//		for (QueueNodeDependencies3 qnd : this.allDependencies)
//		{
//			initialNodes.get(roundRobin % numOfThreads).add(qnd);
//			roundRobin++;
//		}
//		
//		/*
//		 * Now we create Arrays out of our Lists because iterating over them
//		 * is much faster.
//		 * Additionally we correct the Order of the Nodes to respect their Dependencies.
//		 * We do this by creating a copy of the original Nodes List (which is ordered)
//		 * and remove all Elements that are not in our List.
//		 */
//		parallelArrays = new QueueNodeDependencies3[this.numOfThreads][];
//		for (int i = 0; i < initialNodes.size(); i++)
//		{
//			List<QueueNodeDependencies3> list = initialNodes.get(i);
//			
//			QueueNodeDependencies3[] array = new QueueNodeDependencies3[list.size()];
//			list.toArray(array);
//			parallelArrays[i] = array;
//		}
//
//		// no remaining Nodes...
//		remainingArray = new QueueNodeDependencies3[0];
//	}
//	
//	/*
//	 * Test whether the created Arrays respect the order of the QueueNodes they contain.
//	 */
//	private void testDependencies()
//	{
//		List<QueueNodeDependencies3> remainingList = new ArrayList<QueueNodeDependencies3>(); 
//		for (QueueNodeDependencies3 qnd : remainingArray)
//		{
//			remainingList.add(qnd);
//		}
//		
//		for (QueueNodeDependencies3[] array : parallelArrays)
//		{
//			List<QueueNodeDependencies3> list = new ArrayList<QueueNodeDependencies3>();
//			for (QueueNodeDependencies3 qnd : array)
//			{
//				list.add(qnd);
//			}
//			
//			int position = 0;
//			for (QueueNodeDependencies3 qnd : list)
//			{
//				for (QueueNodeDependencies3 qnd2 : qnd.getPreProcessedNodes())
//				{
//					if (!list.contains(qnd2)) log.error("Dependent Node not processed by the same Thread!");
//					else
//					{
//						int pos = list.indexOf(qnd2);
//						if (!(pos < position)) log.error("Parent Node is not processed before its Child!");
//					}
//				}
//				for (QueueNodeDependencies3 qnd2 : qnd.getPostProcessedNodes())
//				{
//					if (list.contains(qnd2))
//					{
//						int pos = list.indexOf(qnd2);
//						if (!(pos > position)) log.error("Child Node is not processed before its Parent!");
//					}
//					else
//					{
//						if (!remainingList.contains(qnd2)) log.error("Dependent Node not processed by the same Thread!");
//					}
//				}
//				
//				position++;
//			}
//		}
//		
//		// Testing the Remaining Array
//		int position = 0;
//		for (QueueNodeDependencies3 qnd : remainingList)
//		{
//			for (QueueNodeDependencies3 qnd2 : qnd.getPreProcessedNodes())
//			{
//				if (remainingList.contains(qnd2))
//				{
//					int pos = remainingList.indexOf(qnd2);
//					if (!(pos < position)) log.error("Parent Node is not processed before its Child!");
//				}
//			}
//			for (QueueNodeDependencies3 qnd2 : qnd.getPostProcessedNodes())
//			{
//				if (remainingList.contains(qnd2))
//				{
//					int pos = remainingList.indexOf(qnd2);
//					if (!(pos > position)) log.error("Child Node is not processed before its Parent!");
//				}
//				else log.error("Child Node is not processed before its Parent!");
//			}
//			
//			position++;
//		}
//	}
//	
//	public void init(QueueNode[] simNodesArray, QueueNetwork queueNetwork, int numOfThreads)
//	{
//		this.numOfThreads = numOfThreads;
//		
//		moveNodesThreads = new MoveNodesThread[numOfThreads];
//		
//		this.timeStepStartBarrier = new CyclicBarrier(this.numOfThreads + 1);
//		this.timeStepEndBarrier = new CyclicBarrier(this.numOfThreads + 1);
//
//		log.info("Start QueueNodeDependencies PreProcessing...");
//		createQueueNodeDependencies(simNodesArray, queueNetwork);
//		log.info("done.");
//		
////		createArrays();
//		createEqualArrays();
//		
////		log.info("Testing Dependencies");
////		testDependencies();
////		log.info("done");
//		
////		remainingNodesRunnable = new RemainingNodesRunnable(this.remainingArray);
//		
//		// setup threads
//		for (int i = 0; i < numOfThreads; i++) 
//		{
//			MoveNodesThread moveNodesThread = new MoveNodesThread(simulateAllNodes);
//			moveNodesThread.setName("MoveNodes" + i);
//			moveNodesThread.setQueueNodeArray(this.parallelArrays[i]);			
////			moveNodesThread.setDaemon(true);	// make the Thread Daemons so they will terminate automatically
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
//	private void  moveRemainingNodes(double time)
//	{
//		for (QueueNodeDependencies3 qnd : remainingArray)
//		{												
//			QueueNode node = qnd.getQueueNode();
//			if (node.isActive() || node.isSignalized() || simulateAllNodes)
//			{
//				node.moveNode(time, qnd.getRandom());
//			}
//		}
//	}
//	
////	private class RemainingNodesRunnable implements Runnable
////	{
////		private double time;
////		private QueueNodeDependencies3[] remainingArrray;
////		
////	    RemainingNodesRunnable(QueueNodeDependencies3[] remainingArrray)
////	    {
////	    	this.remainingArrray = remainingArray;
////	    }
////	    
////	    public void setTime(double time)
////	    {
////	    	this.time = time;
////	    }
////	    
////	    public void run()
////	    {
////	    	for (QueueNodeDependencies3 qnd : remainingArray)
////			{												
////				QueueNode node = qnd.getQueueNode();
////				if (node.isActive() || node.isSignalized() || simulateAllNodes)
////				{
////					node.moveNode(time, qnd.getRandom());
////				}
////			}
////	    }
////	}
//	
//	/*
//	 * This is (depending on the size of the used Network) a quite 
//	 * time consuming PreProcessing Step. It should be only necessary 
//	 * to run this once per Simulation.
//	 * 
//	 * Idea: find out which InNodes of a QueueNode have already been
//	 * processed when the simNodesArray is processed. A QueueNode
//	 * can be processed at any time after all its depending InNodes
//	 * have been processed.
//	 */
//	private void createQueueNodeDependencies(QueueNode[] simNodesArray, QueueNetwork queueNetwork)
//	{
//		Map<Id, QueueNodeDependencies3> allQNDs = new TreeMap<Id, QueueNodeDependencies3>(); 
//		initialDependencies = new ArrayList<QueueNodeDependencies3>();
//		allDependencies = new ArrayList<QueueNodeDependencies3>();
//		
//		for (QueueNode queueNode : simNodesArray)
//		{
//			QueueNodeDependencies3 qnd = new QueueNodeDependencies3(queueNode, MatsimRandom.getLocalInstance());
//			allQNDs.put(queueNode.getNode().getId(), qnd);
//			initialDependencies.add(qnd);
//			allDependencies.add(qnd);
//		}
//		
//		// First add all In- and OutNodes to the PostProcessed Nodes List...
//		for (QueueNodeDependencies3 qnd : allQNDs.values())
//		{
//			for (Link link : qnd.getQueueNode().getNode().getInLinks().values())
//			{
//				Node node = link.getFromNode();
//				qnd.addPostProcessedNode(allQNDs.get(node.getId()));
//			}
//			
//			for (Link link : qnd.getQueueNode().getNode().getOutLinks().values())
//			{
//				Node node = link.getToNode();
//				qnd.addPostProcessedNode(allQNDs.get(node.getId()));
//			}
//		}
//		
//		/*
//		 * Iterate over the Array in the same order as the Simulation does.
//		 */
//		for (QueueNode queueNode : simNodesArray)
//		{
//			QueueNodeDependencies3 qnd = allQNDs.get(queueNode.getNode().getId());
//			
//			/*
//			 * Iterate over all remaining Entries in the Map and add
//			 * the current qnd as PreProcessed Node if they are direct
//			 * children.
//			 */
//			for (QueueNodeDependencies3 possibleChild : allQNDs.values())
//			{
//				if (possibleChild.getPostProcessedNodes().contains(qnd))
//				{
//					possibleChild.removePostProcessedNode(qnd);
//					possibleChild.addPreProcessedNode(qnd);
//				}
//			}
//			
//			/*
//			 *  Finally remove the processed QueueNode from the Map so we
//			 *  don't change its Pre- and PostProcessed Lists anymore.
//			 */
//			allQNDs.remove(queueNode.getNode().getId());
//		}
//		
//		allQNDs = null;
//		
//		/*
//		 * In a last Step we delete all QNDs from the initial List that can't
//		 * be processed initially (means there are Nodes that have to be 
//		 * processed before). 
//		 */
//		Iterator<QueueNodeDependencies3> iter = initialDependencies.iterator();
//		while (iter.hasNext())
//		{
//			QueueNodeDependencies3 qnd = iter.next();
//			
//			// If the Node is not movable we remove it from the List.
//			if (!qnd.isMoveable()) iter.remove();
//		}
//	}
//	
//	private void findAllDependentNodes(QueueNodeDependencies3 qnd)
//	{	
//		List<QueueNodeDependencies3> list = new ArrayList<QueueNodeDependencies3>();
//		list.add(qnd);
//		list.addAll(qnd.getPreProcessedParents());
//		list.addAll(qnd.getPostProcessedNodes());
//		
//		int iteration = 0;
//		boolean foundNewElement = false;
//		do
//		{
//			foundNewElement = false;
//			List<QueueNodeDependencies3> newElements = new ArrayList<QueueNodeDependencies3>();
//			for (QueueNodeDependencies3 qnd2 : list)
//			{
//				newElements.addAll(qnd2.getPreProcessedParents());
//				newElements.addAll(qnd2.getPostProcessedNodes());
//			}
//			
//			int newElement = 0;
//			for (QueueNodeDependencies3 qnd2 : newElements)
//			{
//				if (!list.contains(qnd2))
//				{
//					foundNewElement = true;
//					newElement++;
//					list.add(qnd2);
//				}
//			}
//			iteration++;
//			log.info("Iteration: " + iteration + ", found new Elements: " + newElement + ", current Size: " + list.size());
//		}
//		while (foundNewElement && iteration < 10);
//		
//		log.info("Size: " + list.size());
//	}
//	
//	private void doAnalysis()
//	{		
//		int counter;
//		
//		counter = 0;
//		for (QueueNodeDependencies3 qnd : allDependencies)
//		{
//			counter = counter + qnd.removeImplicitDependencies();
//		}
//		log.info("Total removable Dependencies " + counter);
//		
//		
//		for (int n = 0; n < 1; n++)
//		{
//			int count = 0;
//			for (QueueNodeDependencies3 qnd : allDependencies)
//			{
//				int size = qnd.getPreProcessedNodes().size();
//				if (size == n) findAllDependentNodes(qnd);
//			}
//		}
//
//		
//		for (int n = 0; n < 10; n++)
//		{
//			int count = 0;
//			for (QueueNodeDependencies3 qnd : allDependencies)
//			{
//				int size = qnd.getPreProcessedNodes().size();
//				if (size == n) count++;
//			}
//			log.info("Analysis: n = " + n + ", count = " + count);
//		}
//				
//		for (QueueNodeDependencies3 qnd : allDependencies)
//		{
//			qnd.identifyPostProcessableChildren();
//		}
//		
//		counter = 0;
//		for (QueueNodeDependencies3 qnd : initialDependencies)
//		{
//			List<QueueNodeDependencies3> list =  qnd.getPostProcessableChildren();
////			log.info("PostProcessableChildren " + list.size());
//			counter = counter + list.size();
//		}
//		log.info("Total PostProcessableChildren " + counter);
//	}
//	
//	/**
//	 * The thread class that really handles the links.
//	 */
//	private static class MoveNodesThread extends Thread
//	{
//		private double time = 0.0;
//		private boolean simulateAllNodes = false;
//		private QueueNodeDependencies3[] queueNodes;
//		private CyclicBarrier timeStepStartBarrier;
//		private CyclicBarrier timeStepEndBarrier;
//		
//		public MoveNodesThread(boolean simulateAllNodes)
//		{
//			this.simulateAllNodes = simulateAllNodes;
//		}
//
//		public void setQueueNodeArray(QueueNodeDependencies3[] queueNodes)
//		{
//			this.queueNodes = queueNodes;
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
//		public void setTime(final double t)
//		{
//			time = t;
//		}
//				
//		@Override
//		public void run()
//		{	
//			while(true)
//			{
//				try
//				{
//					/*
//					 * The End of the Link Moving is synchronized with 
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
//					for (QueueNodeDependencies3 qnd : queueNodes)
//					{							
//						QueueNode node = qnd.getQueueNode();
//						if (node.isActive() || node.isSignalized() || simulateAllNodes)
//						{
//							node.moveNode(time, qnd.getRandom());
//						}
//					}
//				}
//				catch (InterruptedException e)
//				{
//					Gbl.errorMsg(e);
//				}
//	            catch (BrokenBarrierException e)
//	            {
//	            	Gbl.errorMsg(e);
//	            }
//			}
//		}	// run()
//		
//	}	// ReplannerThread
//	
//}