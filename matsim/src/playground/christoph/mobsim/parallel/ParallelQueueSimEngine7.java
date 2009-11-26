///* *********************************************************************** *
// * project: org.matsim.*
// * ParallelQueueSimEngine7.java
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
//import java.util.Random;
//import java.util.TreeMap;
//
//import org.apache.log4j.Logger;
//import org.matsim.api.basic.v01.Id;
//import org.matsim.api.core.v01.network.Link;
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
//public class ParallelQueueSimEngine7 extends QueueSimEngine{
//
//	private static final Logger log = Logger.getLogger(ParallelQueueSimEngine7.class);
//	
//	private ParallelMoveNodes7 parallelMoveNodes7;
//	private List<QueueNodeDependencies3> initialDependencies;
//	private List<QueueNodeDependencies3> allDependencies;
//	private int numOfThreads = 1;
//
//	public ParallelQueueSimEngine7(final QueueNetwork network, final Random random) 
//	{
//		super(network, random);
//			
//		log.info("Start QueueNodeDependencies PreProcessing...");
//		createQueueNodeDependencies(super.simNodesArray, network);
//		log.info("done.");
//				
//		parallelMoveNodes7 = new ParallelMoveNodes7(simulateAllNodes);
//		parallelMoveNodes7.init(initialDependencies, network.getNodes().size(), numOfThreads);	
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
//		// reset values
//		for (QueueNodeDependencies3 qnd : allDependencies)
//		{
////			qnd.setMoved(false);
//			qnd.resetProcessedNodes();
//		}
//		
//		List<QueueNodeDependencies3> additionalNodes = new ArrayList<QueueNodeDependencies3>();
//		// set inactive Nodes to moved
//		for (QueueNodeDependencies3 qnd : allDependencies)
//		{
//			if (!qnd.getQueueNode().isActive())
//			{
//				for (QueueNodeDependencies3 child : qnd.getPostProcessedNodes())
//				{
//					child.incProcessedNodes();
//					if (child.isMoveable()) additionalNodes.add(child);
//				}
//			}
//		}
//		
//		/*
//		 * Now split it up to different threads...
//		 */
//		if (parallelMoveNodes7 != null) parallelMoveNodes7.run(time, additionalNodes);
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
//}
