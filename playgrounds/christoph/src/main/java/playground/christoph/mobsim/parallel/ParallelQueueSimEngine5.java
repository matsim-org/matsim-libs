///* *********************************************************************** *
// * project: org.matsim.*
// * ParallelQueueSimEngine5.java
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
//
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
//public class ParallelQueueSimEngine5 extends QueueSimEngine{
//
//	/** This is the collection of links that have to be moved in the simulation */
//	private List<List<QueueNode>> parallelSimNodesArray;
//		
//	private int numOfThreads = 4;
//	private ParallelMoveNodes6 parallelMoveNodes;
//		
//	public ParallelQueueSimEngine5(final QueueNetwork network, final Random random) 
//	{
//		super(network, random);
//		
//		parallelSimNodesArray = new ArrayList<List<QueueNode>>();
//		
//		createNodeLists();
//		
//		parallelMoveNodes = new ParallelMoveNodes6(simulateAllNodes);
//		parallelMoveNodes.init(parallelSimNodesArray);
//	}
//
//	private void createNodeLists()
//	{
//		for (int i = 0; i < numOfThreads; i++)
//		{
//			parallelSimNodesArray.add(new ArrayList<QueueNode>());
//		}
//
//		int i = 0;
//		for(QueueNode node : this.simNodesArray)
//		{
//			parallelSimNodesArray.get(i % numOfThreads).add(node);
//			i++;
//		}
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
//		/*
//		 * Now split it up to different threads...
//		 */
//		parallelMoveNodes.run(time);
//	}
//	
//}
