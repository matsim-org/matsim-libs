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

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.queuesim.DriverAgent;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.christoph.router.util.KnowledgeTools;
import playground.christoph.withinday.replanning.ActEndReplanner;
import playground.christoph.withinday.replanning.InitialReplanner;

public class ParallelInitialReplanner extends ParallelReplanner {
	
	private final static Logger log = Logger.getLogger(ParallelInitialReplanner.class);
	
	protected boolean removeKnowledge = false;
		
	@Override
	public void init()
	{
		replanningThreads = new InternalReplanningThread[numOfThreads];

		// Do initial Setup of the Threads
		for (int i = 0; i < numOfThreads; i++)
		{
			ReplanningThread replanningThread = new InternalReplanningThread(i, replannerArray, replanners);
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
		private KnowledgeTools knowledgeTools;
		
		public InternalReplanningThread(int threadId, PlanAlgorithm replannerArray[][], ArrayList<PlanAlgorithm> replanners)
		{
			super(threadId, replannerArray, replanners);
			this.withinDayReplanner = new InitialReplanner();
			this.knowledgeTools = new KnowledgeTools();
		}
	
		public void setRemoveKnowledge(boolean value)
		{
			((InitialReplanner)this.withinDayReplanner).setRemoveKnowledge(value);
		}
		
		@Override
		protected void doReplanning()
		{
			int numRuns = 0;
			
			DriverAgent driverAgent;
			while((driverAgent = agentsToReplan.poll()) != null)
			{
				// replanner of the person
				PlanAlgorithm replanner = (PlanAlgorithm)driverAgent.getPerson().getCustomAttributes().get("Replanner");
				
				// get the index of the Replanner in the replanners Array
				int index = replanners.indexOf(replanner);
					
				// get the replanner or a clone if it, if it's not the first running thread
				replanner = this.replannerArray[index][threadId];		
				
				withinDayReplanner.setTime(time);
				withinDayReplanner.setDriverAgent(driverAgent);
				withinDayReplanner.setReplanner(replanner);
				boolean replanningSuccessful = withinDayReplanner.doReplanning();
				
				if (!replanningSuccessful) log.error("Replanning was not successful!");
				else numRuns++;
				
				if (numRuns % 500 == 0) log.info("created new Plan for " + numRuns + " persons in thread " + threadId);
			}
		}
		
	}	// InternalReplanningThread
	
}
