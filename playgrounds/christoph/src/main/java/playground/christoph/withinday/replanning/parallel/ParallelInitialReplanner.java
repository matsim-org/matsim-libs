/* *********************************************************************** *
 * project: org.matsim.*
 * ParallelInitialReplanner.java
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
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.DriverAgent;

import playground.christoph.withinday.replanning.InitialReplanner;
import playground.christoph.withinday.replanning.ReplanningTask;
import playground.christoph.withinday.replanning.WithinDayReplanner;

public class ParallelInitialReplanner extends ParallelReplanner {
	
	private final static Logger log = Logger.getLogger(ParallelInitialReplanner.class);
	
	protected boolean removeKnowledge = false;
	
	public ParallelInitialReplanner(int numOfThreads)
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
			replanningThread.setName("ParallelInitialReplanner" + i);
			replanningThreads[i] = replanningThread;
		}

		// Do all other Initialization Operations in the super Class.
		super.init();
	}
	
	public void setRemoveKnowledge(boolean value)
	{
		removeKnowledge = value;
	}
		
	/*
	 * The thread class that really handles the persons.
	 */
	private static class InternalReplanningThread extends ReplanningThread 
	{			
		public void setRemoveKnowledge(boolean value)
		{
			((InitialReplanner)this.withinDayReplanner).setRemoveKnowledge(value);
		}
		
		/*
		 * We only override the method because we want to show log messages!
		 */
		@Override
		protected void doReplanning()
		{
			int numRuns = 0;
			
			ReplanningTask replanningTask;
			while((replanningTask = replanningTasks.poll()) != null)
			{
				Id id = replanningTask.getWithinDayReplannerId();
				DriverAgent driverAgent = replanningTask.getAgentToReplan();
				
				if (id == null)
				{
					log.error("WithinDayReplanner Id is null!");
					return;
				}
				
				if (driverAgent == null)
				{
					log.error("DriverAgent is null!");
					return;
				}
				
				WithinDayReplanner withinDayReplanner = this.withinDayReplanners.get(id);
				
				if (withinDayReplanner != null)
				{
					withinDayReplanner.setTime(time);
					boolean replanningSuccessful = withinDayReplanner.doReplanning(driverAgent);
					
					if (!replanningSuccessful) log.error("Replanning was not successful!");
					else numRuns++;
				}
				else
				{
					log.error("WithinDayReplanner is null!");
				}
				
				if (numRuns % 500 == 0) log.info("created new Plan for " + numRuns + " persons in thread " + Thread.currentThread().getName());
			}
		}
		
	}	// InternalReplanningThread
	
}
