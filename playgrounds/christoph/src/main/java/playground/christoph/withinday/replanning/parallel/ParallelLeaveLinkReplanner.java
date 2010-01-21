/* *********************************************************************** *
 * project: org.matsim.*
 * ParallelLeaveLinkReplanner.java
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
import org.matsim.api.core.v01.network.Network;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.christoph.withinday.replanning.LeaveLinkReplanner;

/**
 * A class for running {@link LeaveLinkReplanner} in parallel using threads.
 *
 * @author Christoph Dobler
 */
public class ParallelLeaveLinkReplanner extends ParallelReplanner {

	private final static Logger log = Logger.getLogger(ParallelLeaveLinkReplanner.class);

	private Network network;

	public ParallelLeaveLinkReplanner(Network network)
	{
		this.network = network;
	}

	@Override
	public void init()
	{
		replanningThreads = new InternalReplanningThread[numOfThreads];

		// Do initial Setup of the Threads
		for (int i = 0; i < numOfThreads; i++)
		{
			ReplanningThread replanningThread = new InternalReplanningThread(i, replannerArray, replanners, this.network);
			replanningThread.setName("ParallelLeaveLinkReplanner" + i);
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
		public InternalReplanningThread(int i, PlanAlgorithm replannerArray[][], ArrayList<PlanAlgorithm> replanners, Network network)
		{
			super(i, replannerArray, replanners);
			this.withinDayReplanner = new LeaveLinkReplanner(network);
		}
	}
	
}