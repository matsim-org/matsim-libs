///* *********************************************************************** *
// * project: org.matsim.*
// * ParallelQueueSimEngine4.java
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
//public class ParallelQueueSimEngine4 extends QueueSimEngine{
//
//	private static final Logger log = Logger.getLogger(ParallelQueueSimEngine4.class);
//	
//	private ParallelMoveNodes5 parallelMoveNodes5;
//
//	private int numOfThreads = 2;
//	
//	public ParallelQueueSimEngine4(final QueueNetwork network, final Random random) 
//	{
//		super(network, random);
//				
//		parallelMoveNodes5 = new ParallelMoveNodes5();
//		
//		List<QueueNodeDependencies> list = new ArrayList<QueueNodeDependencies>();
//		
//		for (QueueNode queueNode : simNodesArray)
//		{
//			list.add(new QueueNodeDependencies(queueNode, MatsimRandom.getLocalInstance()));
//		}
//		
//		parallelMoveNodes5.init(list, numOfThreads, simulateAllNodes);
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
//		parallelMoveNodes5.run(time);
//	}
//}
