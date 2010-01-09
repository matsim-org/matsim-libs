///* *********************************************************************** *
// * project: org.matsim.*
// * ParallelQueueSimEngine.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.ptproject.qsim;

import java.util.Collection;
import java.util.Random;

import org.apache.log4j.Logger;

/**
 * An extended version of the QueueSimEngine where
 * the movement of the Links and Nodes is executed parallel on 
 * multiple Threads. The results of the Simulation stays
 * deterministic but the order of the LinkEvents within
 * a single TimeStep does not. Ordering the Events by Time AND
 * AgentId should produce deterministic results.
 * 
 * Due to the fact that a Random Number generator is used for each
 * simulated Node instead of one in total as the Single CPU
 * QueueSimulation does, the Results will slightly vary between
 * the Simulations!
 */
public class ParallelQueueSimEngine extends QueueSimEngine{

	private static final Logger log = Logger.getLogger(ParallelQueueSimEngine.class);

	private ParallelMoveLinks parallelMoveLinks;
	private ParallelMoveNodes parallelMoveNodes;
	private ParallelMoveNodesAndLinks parallelMoveNodesAndLinks;
	private int numOfThreads = 1;
	
	/*
	 * @cdobler
	 * ParallelMoveNodes and ParallelMoveLinks could be used instead of
	 * ParallelMoveNodesAndLinks - the do exactly the same.
	 * 
	 * TODO discuss which implementation should be preferred
	 */
	ParallelQueueSimEngine(Collection<QueueLink> links, Collection<QueueNode> nodes, Random random, int numOfThreads)
	{
		super(links, nodes, random);
				
//		/*
//		 * parallel moveNodes
//		 */
//		parallelMoveNodes = new ParallelMoveNodes(simulateAllNodes);
//		parallelMoveNodes.init(super.simNodesArray, numOfThreads);
				
//		/*
//		 * parallel moveLinks
//		 */	
//		parallelMoveLinks = new ParallelMoveLinks(simulateAllLinks);
//		parallelMoveLinks.init(super.allLinks, numOfThreads);
		
		/*
		 * parallel moveNodesAndLinks
		 */
		parallelMoveNodesAndLinks = new ParallelMoveNodesAndLinks(simulateAllNodes, simulateAllLinks);
		parallelMoveNodesAndLinks.initNodesAndLinks(simNodesArray, allLinks, simActivateThis, numOfThreads);
	}

	public ParallelQueueSimEngine(final QueueNetwork network, final Random random, int numOfThreads)
	{
		this(network.getLinks().values(), network.getNodes().values(), random, numOfThreads);
	}

	@Override
	protected void simStep(final double time)
	{
		parallelMoveNodesAndLinks.run(time);
	}
	
	/*
	 * Methods for parallel moveNodes
	 */
	
	/*
	 * We have to call this method synchronized because it is used by
	 * parallel Threads to move the Nodes.
	 * Maybe each Thread could have its own list and the Links can be collected
	 * from the main Thread to avoid this?
	 */
	@Override
	protected synchronized void activateLink(final QueueLink link)
	{
		super.activateLink(link);
	}
	
	/*
	 * Parallel movement of the Nodes. The results of the simulation
	 * are deterministic but the order of the LinkEvents within a
	 * single TimeStep are not!
	 */
	@Override
	protected void moveNodes(final double time)
	{	
//		super.moveNodes(time);
		
		/*
		 * Now split it up to different threads.
		 */
		parallelMoveNodes.run(time);
	}
	
		
	/*
	 * Methods for parallel moveLinks
	 */
		
	/*
	 * Parallel movement of the Links. The results of the simulation
	 * are deterministic but the order of the LinkEvents within a
	 * single TimeStep are not!
	 */
	@Override
	protected void moveLinks(final double time)
	{
//		super.moveLinks(time);
		
		/*
		 * Reactivating the Links is handled by the ParallelMoveLinks Module.
		 */
		parallelMoveLinks.reactivateLinks(this.simActivateThis);
		
		/*
		 * Now split it up to different Threads.
		 */
		parallelMoveLinks.run(time);
	}
	
	/*
	 * We get the Number of simulated Links from the ParallelMoveLinks Module.
	 */
	@Override
	protected int getNumberOfSimulatedLinks()
	{
		return this.parallelMoveNodesAndLinks.getNumberOfSimulatedLinks();
//		return this.parallelMoveLinks.getNumberOfSimulatedLinks();
	}
	
}
