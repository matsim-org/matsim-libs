///* *********************************************************************** *
// * project: org.matsim.*
// * ParallelQueueSimEngine9.java
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
//import java.util.Random;
//
//import org.apache.log4j.Logger;
//import org.matsim.core.mobsim.queuesim.QueueLink;
//import org.matsim.core.mobsim.queuesim.QueueNetwork;
//import org.matsim.core.mobsim.queuesim.QueueSimEngine;
//
///**
// * An extended version of the QueueSimEngine where
// * the movement of the Nodes is executed parallel on 
// * multiple Threads. The results of the Simulation stay
// * deterministic but the order of the NodeEvents within
// * a single TimeStep does not.
// */
//public class ParallelQueueSimEngine9 extends QueueSimEngine{
//
//	private static final Logger log = Logger.getLogger(ParallelQueueSimEngine9.class);
//	
//	private ParallelMoveNodes9 parallelMoveNodes9;
//	private int numOfThreads = 8;
//
//	public ParallelQueueSimEngine9(final QueueNetwork network, final Random random) 
//	{
//		super(network, random);
//		
//		parallelMoveNodes9 = new ParallelMoveNodes9(simulateAllNodes);
//		parallelMoveNodes9.init(super.simNodesArray, numOfThreads);	
//	}
//
//	/*
//	 * We have to call this method synchronized because is used by
//	 * parallel Threads.
//	 * Maybe each Thread could have its own list and the Links can be collected
//	 * from the main Thread to avoid this?
//	 */
//	@Override
//	protected synchronized void activateLink(final QueueLink link)
//	{
//		super.activateLink(link);
//	}
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
//		parallelMoveNodes9.run(time);
//	}
//	
//
//}
