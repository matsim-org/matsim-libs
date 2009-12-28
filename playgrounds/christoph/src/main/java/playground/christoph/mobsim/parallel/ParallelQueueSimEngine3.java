///* *********************************************************************** *
// * project: org.matsim.*
// * ParallelQueueSimEngine3.java
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
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.Random;
//
//import org.apache.log4j.Logger;
//import org.matsim.api.basic.v01.Id;
//import org.matsim.api.core.v01.network.Node;
//import org.matsim.core.gbl.MatsimRandom;
//import org.matsim.core.mobsim.queuesim.QueueNetwork;
//import org.matsim.core.mobsim.queuesim.QueueNode;
//import org.matsim.core.mobsim.queuesim.QueueSimEngine;
//
///**
// * An extended version of the QueueSimEngine where
// * the movement of the Links is executed parallel on 
// * multiple Threads. The results of the Simulation stay
// * deterministic but the order of the LinkEvents within
// * a single TimeStep does not.
// * 
// * A point that has to be checked:
// * What happens if an Agent is teleported? Could this cause
// * Race Conditions is the destination link has / has not 
// * already been processed?
// */
//public class ParallelQueueSimEngine3 extends QueueSimEngine{
//
//	private static final Logger log = Logger.getLogger(ParallelQueueSimEngine3.class);
//	
//	private ParallelMoveNodes parallelMoveNodes;
//	private ParallelMoveNodes2 parallelMoveNodes2;
//	private ParallelMoveNodes3 parallelMoveNodes3;
//	private ParallelMoveNodes4 parallelMoveNodes4;
//	private QueueNodeDependencies[] queueNodeDependencies;
//	private int numOfThreads = 2;
//	
//	private List<List<QueueNodeDependencies>> parallelSimNodesArray;
//	
//	public ParallelQueueSimEngine3(final QueueNetwork network, final Random random) 
//	{
//		super(network, random);
//			
//		log.info("Start QueueNodeDependencies PreProcessing...");
//		createQueueNodeDependencies(super.simNodesArray, network);
//		log.info("done.");
//				
//		log.info("Testing Dependencies...");
//		testDependencies();
//		log.info("done.");
//		
////		parallelMoveNodes = new ParallelMoveNodes();
////		parallelMoveNodes.init(queueNodeDependencies, numOfThreads, simulateAllNodes);
//		
//		// other approach...
//		createParallelSimNodesArrays();
//		
////		log.info("Create Children");
////		createChildren();
//		
////		log.info("PostProcessing SimNodesArray");
////		postProcessParallelSimNodesArrays();
////		
////		log.info("Second run...");
////		postProcessParallelSimNodesArrays();
////
////		log.info("Third run...");
////		postProcessParallelSimNodesArrays();
//		
////		parallelMoveNodes2 = new ParallelMoveNodes2();
////		parallelMoveNodes2.init(parallelSimNodesArray, numOfThreads, simulateAllNodes);
//			
////		parallelMoveNodes3 = new ParallelMoveNodes3();
////		parallelMoveNodes3.init(parallelSimNodesArray.get(0), numOfThreads, simulateAllNodes);
//		
//		parallelMoveNodes4 = new ParallelMoveNodes4(simulateAllNodes);
//		parallelMoveNodes4.init(parallelSimNodesArray, numOfThreads);
//	}
//
//	/*
//	 * Parallel movement of the Nodes. The results of the simulation
//	 * are deterministic but the order of the LinkEvents within a
//	 * single TimeStep are not!
//	 */
//	@Override
//	protected void moveNodes(final double time)
//	{
////		// reset values
////		for (QueueNodeDependencies queueNodeDependencie : queueNodeDependencies)
////		{
////			queueNodeDependencie.setMoved(false);
////			queueNodeDependencie.resetProcessedInNodes();
////		}
//		
//		/*
//		 * Now split it up to different threads...
//		 */
//		if (parallelMoveNodes != null) parallelMoveNodes.run(time);
//		if (parallelMoveNodes2 != null) parallelMoveNodes2.run(time);
//		if (parallelMoveNodes3 != null) parallelMoveNodes3.run(time);
//		if (parallelMoveNodes4 != null) parallelMoveNodes4.run(time);
//	}
//	
//	/*
//	 * This is a quite time consuming PreProcessing Step.
//	 * It should be only necessary to run this once per Simulation.
//	 * 
//	 * Idea: find out which InNodes of a QueueNode have already been
//	 * processed when the simNodesArray is processed. A QueueNode
//	 * can be processed at any time after all its depending InNodes
//	 * have been processed.
//	 */
//	private void createQueueNodeDependencies(QueueNode[] simNodesArray, QueueNetwork queueNetwork)
//	{
//		queueNodeDependencies = new QueueNodeDependencies[simNodesArray.length];
//		
//		Map<Id, QueueNodeDependencies> map = new HashMap<Id, QueueNodeDependencies>();
//		
//		List<QueueNodeDependencies> processedNodes = new ArrayList<QueueNodeDependencies>();
//		
//		/*
//		 * In the first Iteration create all QueueNodeDependencies.
//		 */
//		int i = 0;
//		for (QueueNode queueNode : simNodesArray)
//		{				
//			QueueNodeDependencies queueNodeDependencie = new QueueNodeDependencies(queueNode, MatsimRandom.getLocalInstance());
//			
//			map.put(queueNode.getNode().getId(), queueNodeDependencie);
//			
//			queueNodeDependencies[i] = queueNodeDependencie;
//			i++;
//		}
//		
//		/*
//		 * In the second Iteration add the Pre- & PostProcessing Dependencies
//		 * and the OutNodes.
//		 */
//		for (QueueNodeDependencies queueNodeDependencie : queueNodeDependencies)
//		{	
//			/*
//			 * Pre- and PostProcessing InDependencies
//			 */
//			for (Node inNode : queueNodeDependencie.getQueueNode().getNode().getInNodes().values())
//			{
//				QueueNodeDependencies qnd = map.get(inNode.getId());
//						
//				if (processedNodes.contains(qnd))
//				{
//					queueNodeDependencie.addPreProcessedInNode(qnd);
//				}
//				else
//				{
//					queueNodeDependencie.addPostProcessedInNodes(qnd);
//				}
//			}
//			
//			for (Node outNode : queueNodeDependencie.getQueueNode().getNode().getOutNodes().values())
//			{
//				QueueNodeDependencies qnd = map.get(outNode.getId());
//				queueNodeDependencie.addOutNode(qnd);
//			}
//			
//			processedNodes.add(queueNodeDependencie);
//		}
//		
//		map.clear();
//	}
//	
//	private void createParallelSimNodesArrays()
//	{
//		parallelSimNodesArray = new ArrayList<List<QueueNodeDependencies>>();
//		
//		// reset values
//		for (QueueNodeDependencies queueNodeDependencie : queueNodeDependencies)
//		{
//			queueNodeDependencie.setMoved(false);
//			queueNodeDependencie.resetProcessedInNodes();
//		}
//		
//		int moved = 0;
//		int premoved = 0;
//		int iteration = 0;
//		while(moved != queueNodeDependencies.length)
//		{
//			int preCount = 0;
//			List<QueueNodeDependencies> moveableList = new ArrayList<QueueNodeDependencies>();
//			for (QueueNodeDependencies qnd : queueNodeDependencies)
//			{
//				if(qnd.isMoveable())
//				{				
//					moveableList.add(qnd);		
//
//					preCount++;
//					moved++;
//				}	
//			}
//						
//			for(QueueNodeDependencies qnd : moveableList)
//			{
//				qnd.setMoved(true);
//				
//				for (QueueNodeDependencies qnd2 : qnd.getOutNodes())
//				{
//					qnd2.incNodeProcessed();
//				}
//			}
//			
//			parallelSimNodesArray.add(moveableList);
//			
//			log.info("Iteration " + ++iteration + ", moved " + (moved - premoved) + ", totally moved " + moved);
//			premoved = moved;
//		}
//		
//		// reset values
//		for (QueueNodeDependencies queueNodeDependencie : queueNodeDependencies)
//		{
//			queueNodeDependencie.setMoved(false);
//			queueNodeDependencie.resetProcessedInNodes();
//		}
//		
//		int iter = 0;
//		for (List<QueueNodeDependencies> list : parallelSimNodesArray)
//		{
//			for (QueueNodeDependencies qnd : list)
//			{
//				if (qnd.isMoveable())
//				{
//					qnd.setMoved(true);
//					for (QueueNodeDependencies qnd2 : qnd.getOutNodes())
//					{
//						qnd2.incNodeProcessed();
//					}
//				}
//				else
//				{
//					log.error(iter + " Could not move node!");
//				}
//			}
//			iter++;
//		}
//	}
//	
//	private void postProcessParallelSimNodesArrays()
//	{
//		int iteration = 0;
//		List<QueueNodeDependencies> previousList = parallelSimNodesArray.get(0);
//		for(List<QueueNodeDependencies> list : parallelSimNodesArray)
//		{
//			// skip first List
//			if (iteration == 0)
//			{	
//				iteration++;
//				continue;
//			}
//			
////			log.info("Iteration " + iteration + ", PreListSize: " + list.size());
//			
//			for (Iterator<QueueNodeDependencies> iter = list.iterator(); iter.hasNext();) 
//			{
//				QueueNodeDependencies qnd = iter.next();
//			
//				if(qnd.getPreProcessedInNodes().size() == 1)
//				{
//					iter.remove();
//					previousList.add(qnd);
//				}
//			}
//			
////			log.info("Iteration " + iteration + ", PostListSize: " + list.size());
//			
//			previousList = list;
//			iteration++;
//		}
//		
//		iteration = 0;
//		for (List<QueueNodeDependencies> list : parallelSimNodesArray)
//		{
//			log.info("Iteration " + ++iteration + ", ListSize: " + list.size());
//		}
//	}
//	
//	private void createChildren()
//	{
//		List<Map<QueueNode, QueueNodeDependencies2>> childrenList = new ArrayList<Map<QueueNode, QueueNodeDependencies2>>();
//	
//		/*
//		 * First Iteration:
//		 * Create the QueueNodeDependencies2 Objects
//		 */
//		for(List<QueueNodeDependencies> list : parallelSimNodesArray)
//		{
//			Map<QueueNode, QueueNodeDependencies2> map = new HashMap<QueueNode, QueueNodeDependencies2>();
//			
//			for (QueueNodeDependencies qnd : list)
//			{
//				map.put(qnd.getQueueNode(), new QueueNodeDependencies2(qnd.getQueueNode(), qnd.getRandom()));
//			}
//			
//			log.info("MapSize " + map.size());
//			childrenList.add(map);
//		}
//		
//		/*
//		 * Second Iteration:
//		 * Adding Children.
//		 */
//		for (int i = parallelSimNodesArray.size() - 1; i > 0; i--)
//		{
//			List<QueueNodeDependencies> list = parallelSimNodesArray.get(i);
//			Map<QueueNode, QueueNodeDependencies2> map = childrenList.get(i);
//			Map<QueueNode, QueueNodeDependencies2> parentMap = childrenList.get(i - 1);
//			
//			for (QueueNodeDependencies qnd : list)
//			{
//				if(qnd.getPreProcessedInNodes().size() == 1)
//				{
//					QueueNode parent = qnd.getPreProcessedInNodes().get(0).getQueueNode();
//
//					QueueNodeDependencies2 qnd2 = parentMap.get(parent);
//					
//					if (qnd2 != null)
//					{
//						qnd2.addChild(map.get(qnd.getQueueNode()));
//						map.remove(qnd.getQueueNode());
//					}
//				}
//			}		
//		}
//		for(Map<QueueNode, QueueNodeDependencies2> map : childrenList)
//		{
//			int entries = 0;
//			for (QueueNodeDependencies2 qnd2 : map.values())
//			{
//				entries = entries + qnd2.getNodeCount();
//			}
//			
//			log.info("MapSize " + entries);
//		}
//	}
//	
//	private void testDependencies()
//	{
//		for (QueueNodeDependencies queueNodeDependencie : queueNodeDependencies)
//		{
//			queueNodeDependencie.setMoved(false);
//			queueNodeDependencie.resetProcessedInNodes();
//		}
//		
//		List<QueueNodeDependencies> moveable = new ArrayList<QueueNodeDependencies>();
//		
//		/*
//		 * Find all Nodes that have no Preprocessed Dependencies.
//		 * They can be moved right away from the beginning.
//		 */
//		int preCount = 0;
//		for (QueueNodeDependencies qnd : queueNodeDependencies)
//		{
//			if(qnd.isMoveable())
//			{
//				moveable.add(qnd);
//				preCount++;
//			}
//		}
//		
////		// Add the First Node from the List so the Threads can start.
////		moveable.add(queueNodeDependencies[0]);
//		
//		int errorCount = 0;
//		for (QueueNodeDependencies qnd : queueNodeDependencies)
//		{
//			int in = qnd.getQueueNode().getNode().getInNodes().size();
//			int inPre = qnd.getPreProcessedInNodes().size();
//			int inPost = qnd.getPostProcessedInNodes().size();
//			if (in - inPre - inPost != 0) log.error("Wrong number of InLinks!");
//			
//			int out = qnd.getQueueNode().getNode().getOutNodes().size();
//			int out2 = qnd.getOutNodes().size();
//			if (out - out2 != 0) log.error("Wrong number of OutLinks!");
//			
//			if (!qnd.isMoveable())
//			{
//				log.error("Node should be moveable but is not!");
//				errorCount++;
//				
//				for (QueueNodeDependencies outNode : qnd.getOutNodes())
//				{
//					if (outNode.getPostProcessedInNodes().contains(qnd))
//					{
//						if(!outNode.hasBeenMoved())
//						{
//							log.error("Contained in PostProcessedInNodes");
//						}
//					}
//				}
//				
//				if(qnd.getProcessedInNodes() != qnd.getPreProcessedInNodes().size())
//				{
//					log.error("Wrong number of processed InNodes. " + qnd.getProcessedInNodes() + " vs. " + qnd.getPreProcessedInNodes().size());
//				}
////				return processedInNodes == preProcessedInNodes.size();
//			}	
//			qnd.setMoved(true);
//			
//			/*
//			 * Get all inNodes that were not allowed to be processed 
//			 * before this Node was. Add them to the processable List now.
//			 */
//			for (QueueNodeDependencies qnd2 : qnd.getPostProcessedInNodes())
//			{
//				if (qnd2.isMoveable()) moveable.add(qnd2);
//			}
//			
//			/*
//			 * For all OutNodes:
//			 * Maybe they are now processable so check for that and
//			 * if its true, add them to the list.
//			 */
//			for (QueueNodeDependencies qnd2 : qnd.getOutNodes())
//			{
//				qnd2.incNodeProcessed();
//					
//				if (qnd2.isMoveable())
//				{
//					/*
//					 * Node would be moveable. Now check the criteria
//					 * from the original QueueSimEngine.
//					 */
//					moveable.add(qnd2);
//				}
//			}
//		}
//		log.info("PreCount: " + preCount);
//		log.info("ErrorCount: " + errorCount);
//		log.info("MovableSize: " + moveable.size());
//		log.info("NodeCount: " + queueNodeDependencies.length);
//	}
//}
