///* *********************************************************************** *
// * project: org.matsim.*
// * ParallelQueueSimEngine.java
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
//import org.matsim.core.mobsim.queuesim.QueueNetwork;
//import org.matsim.core.mobsim.queuesim.QueueSimEngine;
//
//public class ParallelQueueSimEngine extends QueueSimEngine{
//
//	protected int numOfThreads = 2;
//	protected ParallelMoveLinks parallelMoveLinks;
//	
//	public ParallelQueueSimEngine(final QueueNetwork network, final Random random) 
//	{
//		super(network, random);
//		
//		parallelMoveLinks = new ParallelMoveLinks(simulateAllLinks);
//		parallelMoveLinks.init(4);
//	}
//
//	/*
//	 * This piece of code should be parallelizable quite easy... lets see...
//	 */
//	@Override
//	protected void moveLinks(final double time) {
//		reactivateLinks();
//		
//		/*
//		 * Now split it up to different threads...
//		 */
//		parallelMoveLinks.run(simLinksArray, time);
//	}
//	
//}
