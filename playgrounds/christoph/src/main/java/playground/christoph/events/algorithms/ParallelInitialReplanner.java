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

package playground.christoph.events.algorithms;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.christoph.router.util.KnowledgeTools;

public class ParallelInitialReplanner extends ParallelReplanner {
	
	private final static Logger log = Logger.getLogger(ParallelInitialReplanner.class);
	
	protected boolean removeKnowledge = false;
	
	@Override
	public void init()
	{
	}
	
	/** 
	 * The Method uses the same structure as the LeaveLinkReplanner but instead of single node and vehicles
	 * Objects now ArrayLists are handed over.
	 * 
	 * @param population
	 * @param time
	 */	
	public void run(List<? extends Person> persons, double time)
	{		
		Thread[] threads = new Thread[this.numOfThreads];
		ReplannerThread[] replannerThreads = new ReplannerThread[numOfThreads];
		
		// setup threads
		for (int i = 0; i < numOfThreads; i++) 
		{
			ReplannerThread replannerThread = new ReplannerThread(i, replannerArray, replanners, time);
			replannerThread.setRemoveKnowledge(removeKnowledge);
			replannerThreads[i] = replannerThread;
			
			Thread thread = new Thread(replannerThread, "Thread#" + i);
			threads[i] = thread;
		}
		
		// distribute workload between threads, as long as threads are not yet started, so we don't need synchronized data structures
		int i = 0;
		for (Person person : persons)
		{
			replannerThreads[i % numOfThreads].handlePerson(person);
			i++;
		}
		
		// start the threads
		for (Thread thread : threads) 
		{
			thread.start();
		}
		
		// wait for the threads to finish
		try {
			for (Thread thread : threads) 
			{
				thread.join();
			}
		} 
		catch (InterruptedException e)
		{
			Gbl.errorMsg(e);
		}
	}
	
	
	public void run(Population population, double time)
	{
		ArrayList<Person> persons = new ArrayList<Person>();
		
		for(Person person : population.getPersons().values())
		{
			persons.add(person);
		}
		
		run(persons, time);
	}
	
	public void setRemoveKnowledge(boolean value)
	{
		removeKnowledge = value;
	}
	
	
	/**
	 * The thread class that really handles the persons.
	 */
	private static class ReplannerThread implements Runnable 
	{
		public final int threadId;
		private boolean removeKnowledge = false;
		private double time = 0.0;
		private final ArrayList<PlanAlgorithm> replanners;
		private final PlanAlgorithm[][] replannerArray;
		private final List<Person> persons = new LinkedList<Person>();
		private KnowledgeTools knowledgeTools;
		
		public ReplannerThread(final int i, final PlanAlgorithm replannerArray[][], final ArrayList<PlanAlgorithm> replanners, final double time)
		{
			this.threadId = i;
			this.replannerArray = replannerArray;
			this.replanners = replanners;
			this.time = time;
			this.knowledgeTools = new KnowledgeTools();
		}

		public void setRemoveKnowledge(boolean value)
		{
			removeKnowledge = value;
		}
		
		public void handlePerson(final Person person)
		{
			this.persons.add(person);
		}

		public void run()
		{
			int numRuns = 0;
			
			for (Person person : this.persons)
			{	
				// replanner of the person
				PlanAlgorithm replanner = (PlanAlgorithm)person.getCustomAttributes().get("Replanner");
					
				// get the index of the Replanner in the replanners Array
				int index = replanners.indexOf(replanner);
					
				// get the replanner or a clone if it, if it's not the first running thread
				replanner = this.replannerArray[index][threadId];
	
				replanner.run(person.getSelectedPlan());

				// If flag is set, remove Knowledge after doing the replanning.
				if (removeKnowledge)
				{
					knowledgeTools.removeKnowledge(person);
					knowledgeTools.removeSubNetwork(person);
				}
				
				numRuns++;
				if (numRuns % 500 == 0) log.info("created new Plan for " + numRuns + " persons in thread " + threadId);
			
			}
		
//			log.info("Thread " + threadId + " done.");
			
		}	// run
		
	}	// ReplannerThread
	
}
