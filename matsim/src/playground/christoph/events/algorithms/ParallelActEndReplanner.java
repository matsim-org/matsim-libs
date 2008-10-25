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
package playground.christoph.events.algorithms;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.queuesim.Vehicle;
import org.matsim.population.Act;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * A class for running {@link ActEndReplanner} in parallel using threads.
 *
 * @author Christoph Dobler
 */
public class ParallelActEndReplanner extends ParallelReplanner {
	
	private final static Logger log = Logger.getLogger(ParallelActEndReplanner.class);
	
	public static void run(ArrayList<Act> fromActs, ArrayList<Vehicle> vehicles, double time)
	{		
		Thread[] threads = new Thread[numOfThreads];
		ReplannerThread[] replannerThreads = new ReplannerThread[numOfThreads];
		
		// setup threads
		for (int i = 0; i < numOfThreads; i++) 
		{
			ReplannerThread replannerThread = new ReplannerThread(i, replannerArray, replanners, time);
			replannerThreads[i] = replannerThread;
			
			Thread thread = new Thread(replannerThread, "Thread#" + i);
			threads[i] = thread;
		}
		
		// distribute workload between threads, as long as threads are not yet started, so we don't need synchronized data structures
		int i = 0;
		for(int j = 0; j < vehicles.size(); j++)
		{
			Vehicle vehicle = vehicles.get(i);
			Act fromAct = fromActs.get(i);
			
			replannerThreads[i % numOfThreads].handleVehicle(vehicle, fromAct);
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
	
	
	
	/**
	 * The thread class that really handles the persons.
	 */
	private static class ReplannerThread implements Runnable 
	{
		public final int threadId;
		private double time = 0.0;
		private final ArrayList<PlanAlgorithm> replanners;
		private final PlanAlgorithm[][] replannerArray;
		private final List<Vehicle> vehicles = new LinkedList<Vehicle>();
		private final List<Act> fromActs = new LinkedList<Act>();

		public ReplannerThread(final int i, final PlanAlgorithm replannerArray[][], final ArrayList<PlanAlgorithm> replanners, final double time)
		{
			this.threadId = i;
			this.replannerArray = replannerArray;
			this.replanners = replanners;
			this.time = time;
		}

		public void handleVehicle(final Vehicle vehicle, final Act fromAct)
		{
			this.vehicles.add(vehicle);
			this.fromActs.add(fromAct);
		}

		public void run()
		{
			int numRuns = 0;
			
			for(int i = 0; i < vehicles.size(); i++)
			{	
				Vehicle vehicle = vehicles.get(i);
				Act fromAct = fromActs.get(i);
				
				// replanner of the person
				PlanAlgorithm replanner = (PlanAlgorithm)vehicle.getDriver().getPerson().getCustomAttributes().get("Replanner");
					
				// get the index of the Replanner in the replanners Array
				int index = replanners.indexOf(replanner);
					
				// get the replanner or a clone if it, if it's not the first running thread
				replanner = this.replannerArray[index][threadId];
								
				new ActEndReplanner(fromAct, vehicle, time, replanner);
//				log.info("Did Act End Replanning...");
				
				numRuns++;
				if (numRuns % 500 == 0) log.info("created new Plan for " + numRuns + " persons in thread " + threadId);
			
			}
		
//			log.info("Thread " + threadId + " done.");
			
		}	// run
		
	}	// ReplannerThread
	
}