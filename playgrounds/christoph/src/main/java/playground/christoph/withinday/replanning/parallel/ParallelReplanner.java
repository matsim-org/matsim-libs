/* *********************************************************************** *
 * project: org.matsim.*
 * ParallelReplanner.java
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
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.queuesim.DriverAgent;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.christoph.router.KnowledgePlansCalcRoute;

/*
 * Abstract class that contains the basic elements that are needed 
 * to do parallel replanning within the QueueSimulation.
 * 
 * Features like the creation of parallel running threads and the
 * split up of the replanning actions have to be implemented in 
 * the subclasses.
 */
public abstract class ParallelReplanner {
	
	private final static Logger log = Logger.getLogger(ParallelReplanner.class);
	
	protected int numOfThreads = 1;	// use by default only one thread
	protected ArrayList<PlanAlgorithm> replanners;
	protected PlanAlgorithm[][] replannerArray;
	protected ReplanningThread[] replanningThreads;
	protected int roundRobin = 0;	
	protected CyclicBarrier timeStepStartBarrier;
	protected CyclicBarrier timeStepEndBarrier;
	
	
	public void init()
	{
		
		this.timeStepStartBarrier = new CyclicBarrier(numOfThreads + 1);
		this.timeStepEndBarrier = new CyclicBarrier(numOfThreads + 1);

		// finalize Thread Setup
		for (int i = 0; i < numOfThreads; i++)
		{
			ReplanningThread replanningThread = replanningThreads[i];
			
			replanningThread.setCyclicTimeStepStartBarrier(this.timeStepStartBarrier);
			replanningThread.setCyclicTimeStepEndBarrier(this.timeStepEndBarrier);
			replanningThread.setDaemon(true);
			
			replanningThread.start();
		}

		/*
		 * After initialization the Threads are waiting at the
		 * TimeStepEndBarrier. We trigger this Barrier once so
		 * they wait at the TimeStepStartBarrier what has to be
		 * their state if the run() method is called.
		 */
		try
		{
			this.timeStepEndBarrier.await();
		}
		catch (InterruptedException e)
		{
			Gbl.errorMsg(e);
		}
		catch (BrokenBarrierException e)
		{
			Gbl.errorMsg(e);
		}
	}
	
	/*
	 * Typical Implementations should be able to use this Method
	 * "as it is"...
	 */
	public void run(double time)
	{
		try
		{
			// set current Time
			for (ReplanningThread replannerThread : replanningThreads)
			{
				replannerThread.setTime(time);
			}
				
			this.timeStepStartBarrier.await();
				
			this.timeStepEndBarrier.await();		
		}
		catch (InterruptedException e)
		{
			Gbl.errorMsg(e);
		}
		catch (BrokenBarrierException e)
		{
	      	Gbl.errorMsg(e);
		}
	}
	
	/**
	 * The Number of Threads must have been set before this call!
	 * @param replannerArrayList
	 */
	// Set the Replanners ArrayList here - this can be done once from the Controler
	public void setReplannerArrayList(ArrayList<PlanAlgorithm> replannerArrayList)
	{
		replanners = replannerArrayList;
	}
	
	/*
	 * We also can use the same Array in different Replanners. 
	 */
	public void setReplannerArray(PlanAlgorithm[][] array)
	{
		this.replannerArray = array;
	}
	
	public PlanAlgorithm[][] getReplannerArray()
	{
		return replannerArray;
	}
	
	public void addAgentToReplan(DriverAgent driverAgent)
	{
		this.replanningThreads[this.roundRobin % this.numOfThreads].addAgentToReplan(driverAgent);
		this.roundRobin++;	
	}
	
	/* 
	 * Creates an Array that contains the Replanners that were initialized in the Controler
	 * and clone of them for every additional replanning thread.
	 * 1 replanning thread -> uses existing replanners
	 * 2 replanning thread -> uses existing replanners and one clone of each
	 * 
	 * If the Replanners from the replanners ArrayList have changed, an update of the
	 * ArrayList can be initiated by using this method.
	 */
	public void createReplannerArray()
	{	
		// create and fill Array of PlanAlgorithms used in the threads
		replannerArray = new PlanAlgorithm[replanners.size()][numOfThreads];
		
		for(int i = 0; i < replanners.size(); i++)
		{
			PlanAlgorithm replanner = replanners.get(i);
		
			// fill first row with already defined selectors
			replannerArray[i][0] = replanner;
			
			// fill the other fields in the current row with clones
			for(int j = 1; j < numOfThreads; j++)
			{
				// insert clone
				if (replanner instanceof KnowledgePlansCalcRoute)
				{
					replannerArray[i][j] = ((KnowledgePlansCalcRoute)replanner).clone();
				}
				else
				{
					log.error("replanner class " + replanner.getClass());
					log.error("Could not clone the Replanner - use reference to the existing Replanner and hope the best...");
					replannerArray[i][j] = replanner;
				}	
			}
		}
	}
	
	public void setNumberOfThreads(int numberOfThreads)
	{		
		numOfThreads = Math.max(numberOfThreads, 1); // it should be at least 1 here; we allow 0 in other places for "no threads"
		
		log.info("Using " + numOfThreads + " parallel threads to replan routes.");
		
//		// if the number of used threads has changed -> PlanAlgorithms Array has to be recreated
//		if (numOfThreads != currentNumOfThreads)
//		{
//			createReplannerArray();
//		}
		
		/*
		 *  Throw error message if the number of threads is bigger than the number of available CPUs.
		 *  This should not speed up calculation anymore.
		 */
		if (numOfThreads > Runtime.getRuntime().availableProcessors())
		{
			log.error("The number of parallel running replanning threads is bigger than the number of available CPUs!");
		}	
	}
}
