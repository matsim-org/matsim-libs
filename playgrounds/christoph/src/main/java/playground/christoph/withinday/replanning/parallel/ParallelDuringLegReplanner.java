/* *********************************************************************** *
 * project: org.matsim.*
 * ParallelDuringLegReplanner.java
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

import org.apache.log4j.Logger;

import playground.christoph.withinday.replanning.WithinDayDuringLegReplanner;

/**
 * A class for running {@link WithinDayDuringLegReplanner} in parallel using threads.
 *
 * @author Christoph Dobler
 */
public class ParallelDuringLegReplanner extends ParallelReplanner {

	private final static Logger log = Logger.getLogger(ParallelDuringLegReplanner.class);

	public ParallelDuringLegReplanner(int numOfThreads)
	{
		super(numOfThreads);
		this.init();
	}
	
	@Override
	protected void init()
	{
		replanningThreads = new InternalReplanningThread[numOfThreads];

		// Do initial Setup of the Threads
		for (int i = 0; i < numOfThreads; i++)
		{
			ReplanningThread replanningThread = new InternalReplanningThread();
			replanningThread.setName("ParallelDuringLegReplanner" + i);
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
		public InternalReplanningThread()
		{
			super();
		}
	}
	
}