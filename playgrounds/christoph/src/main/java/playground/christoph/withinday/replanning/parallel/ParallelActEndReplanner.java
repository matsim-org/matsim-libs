/* *********************************************************************** *
 * project: org.matsim.*
 * ParallelActEndReplanner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.christoph.withinday.replanning.parallel;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.christoph.withinday.replanning.ActEndReplanner;

/**
 * A class for running {@link ActEndReplanner} in parallel using threads.
 *
 * @author Christoph Dobler
 */
public class ParallelActEndReplanner extends ParallelReplanner {
	
	private final static Logger log = Logger.getLogger(ParallelActEndReplanner.class);
		
	@Override
	public void init()
	{	
		replanningThreads = new InternalReplanningThread[numOfThreads];

		// Do initial Setup of the Threads
		for (int i = 0; i < numOfThreads; i++)
		{
			ReplanningThread replanningThread = new InternalReplanningThread(i, replannerArray, replanners);
			replanningThread.setName("ParallelActEndReplanner" + i);
			replanningThreads[i] = replanningThread;
		}

		// Do all other Initialization Operations in the super Class.
		super.init();
	}
	
	/*
	 * The thread class that really handles the replanning.
	 */
	private static class InternalReplanningThread extends ReplanningThread
	{		
		public InternalReplanningThread(int i, PlanAlgorithm replannerArray[][], ArrayList<PlanAlgorithm> replanners)
		{
			super(i, replannerArray, replanners);
			this.withinDayReplanner = new ActEndReplanner();
		}
				
	}	// ReplannerThread
	
}