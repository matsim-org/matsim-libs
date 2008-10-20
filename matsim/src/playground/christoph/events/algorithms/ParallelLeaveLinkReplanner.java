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
package playground.christoph.events.algorithms;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.queuesim.QueueNode;
import org.matsim.mobsim.queuesim.Vehicle;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * A class for running {@link LeaveLinkReplanner} in parallel using threads.
 *
 * @author Christoph Dobler
 */
public class ParallelLeaveLinkReplanner extends ParallelReplanner {
	
	private final static Logger log = Logger.getLogger(ParallelLeaveLinkReplanner.class);
	
	/** 
	 * The Method uses the same structure as the LeaveLinkReplanner but instead of single node and vehicles
	 * Objects now ArrayLists are handed over.
	 * 
	 * @param currentNodes
	 * @param vehicles
	 * @param time
	 */
	public static void run(ArrayList<QueueNode> currentNodes, ArrayList<Vehicle> vehicles, double time)
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
		//for (Vehicle vehicle : vehicles)
		for(int j = 0; j < vehicles.size(); j++)
		{
			Vehicle vehicle = vehicles.get(i);
			QueueNode queueNode = currentNodes.get(i);
			
			replannerThreads[i % numOfThreads].handleVehicle(vehicle, queueNode);
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
		private final List<QueueNode> currentNodes = new LinkedList<QueueNode>();

		public ReplannerThread(final int i, final PlanAlgorithm replannerArray[][], final ArrayList<PlanAlgorithm> replanners, final double time)
		{
			this.threadId = i;
			this.replannerArray = replannerArray;
			this.replanners = replanners;
			this.time = time;
		}

		public void handleVehicle(final Vehicle vehicle, final QueueNode currentNode)
		{
			this.vehicles.add(vehicle);
			this.currentNodes.add(currentNode);
		}

		public void run()
		{
			int numRuns = 0;
			
			//for (Vehicle vehicle : this.vehicles)
			for(int i = 0; i < vehicles.size(); i++)
			{	
				Vehicle vehicle = vehicles.get(i);
				QueueNode queueNode = currentNodes.get(i);
				
				// for LeaveLink Replanning... other to be implemented
				// If replanning flag is set in the Person
//				boolean replanning = (Boolean)vehicle.getDriver().getPerson().getCustomAttributes().get("leaveLinkReplanning");
//				if(replanning)
				/*
				 *  Replanning Flag is already checked in the MyQueueNetwork class.
				 *  If among the link leaving Agents no one has set the replanning flag,
				 *  there is no need to start the Replanner...
				 */			
//				{
					// replanner of the person
					PlanAlgorithm replanner = (PlanAlgorithm)vehicle.getDriver().getPerson().getCustomAttributes().get("Replanner");
					
					// get the index of the Replanner in the replanners Array
					int index = replanners.indexOf(replanner);
					
					// get the replanner or a clone if it, if it's not the first running thread
					replanner = this.replannerArray[index][threadId];
					
					new LeaveLinkReplanner(queueNode, vehicle, time, replanner);
//					log.info("Did Leave Link Replanning...");
//				}
				
				// TODO EndEventReplanner implementation
				
				
				// TODO InitialReplanner implementation
	
				numRuns++;
				//if (numRuns % 500 == 0) log.info("created new Plan for " + numRuns + " persons in thread " + threadId);
			
			}
		
//			log.info("Thread " + threadId + " done.");
			
		}	// run
		
	}	// ReplannerThread
	
}