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

package playground.christoph.events.algorithms;

import java.util.ArrayList;

import org.apache.log4j.Logger;
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
	
	protected ArrayList<PlanAlgorithm> replanners;
	protected int numOfThreads = 1;	// use by default only one thread
	protected PlanAlgorithm[][] replannerArray;
	
	public abstract void init();
	
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
//		int currentNumOfThreads = numOfThreads;
		
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
