///* *********************************************************************** *
// * project: org.matsim.*
// * ParallelQueueSimEngine2.java
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
//import org.matsim.core.mobsim.queuesim.QueueLink;
//import org.matsim.core.mobsim.queuesim.QueueNetwork;
//import org.matsim.core.mobsim.queuesim.QueueSimEngine;
//
///**
// * An extended version of the QueueSimEngine where
// * the movement of the Links is executed parallel on 
// * multiple Threads. The results of the Simulation stay
// * deterministic but the order of the LinkEvents within
// * a single TimeStep does not.
// */
//public class ParallelQueueSimEngine2 extends QueueSimEngine{
//
//	/** This is the collection of links that have to be moved in the simulation */
//	private List<List<QueueLink>> parallelSimLinksArray;
//		
//	private int numOfThreads = 8;
//	private ParallelMoveLinks3 parallelMoveLinks;
//	
//	private int distributor = 0;
//	
//	public ParallelQueueSimEngine2(final QueueNetwork network, final Random random) 
//	{
//		super(network, random);
//			
//		parallelSimLinksArray = new ArrayList<List<QueueLink>>();
//		
//		createLinkLists();
//		
//		parallelMoveLinks = new ParallelMoveLinks3(simulateAllLinks);
//		parallelMoveLinks.init(parallelSimLinksArray);
//	}
//
//	private void createLinkLists()
//	{
//		for (int i = 0; i < numOfThreads; i++)
//		{
//			parallelSimLinksArray.add(new ArrayList<QueueLink>());
//		}
//		
//		/*
//		 * If we simulate all Links, we have to add them initially to the Lists.
//		 */
//		if (simulateAllLinks) 
//		{
//			int roundRobin = 0;
//			for(QueueLink link : this.allLinks)
//			{
//				parallelSimLinksArray.get(roundRobin % numOfThreads).add(link);
//				roundRobin++;
//			}
//		}
//	}
//	
//	/*
//	 * Parallel movement of the Links. The results of the simulation
//	 * are deterministic but the order of the LinkEvents within a
//	 * single TimeStep are not!
//	 */
//	@Override
//	protected void moveLinks(final double time)
//	{
//		reactivateLinks();
//		
//		/*
//		 * Now split it up to different threads...
//		 */
//		parallelMoveLinks.run(time);
//	}
//	
//	/*
//	 * We do the load balancing between the threads using some kind
//	 * of round robin.
//	 * 
//	 * Additionally we should check from time to time whether the load
//	 * is really still balanced. This is not guaranteed due to the fact
//	 * that some Links get deactivated while other don't. If the number
//	 * of Links is high enough statistically the difference shouldn't
//	 * be to significant.
//	 */
//	@Override
//	protected void reactivateLinks() {
//		if (!simulateAllLinks) {
//			if (!this.simActivateThis.isEmpty()) {
//				for(QueueLink link : this.simActivateThis)
//				{
//					parallelSimLinksArray.get(distributor % numOfThreads).add(link);
//					distributor++;
//				}
//				this.simActivateThis.clear();
//			}
//		}
//	}
//
//	/**
//	 * @return Returns the simLinksArray.
//	 */
//	@Override
//	protected int getNumberOfSimulatedLinks()
//	{
//		int size = 0;
//		for (int i = 0; i < numOfThreads; i++)
//		{
//			size = size + this.parallelSimLinksArray.get(i).size();
//		}
//		
//		return size;
//	}
//}
