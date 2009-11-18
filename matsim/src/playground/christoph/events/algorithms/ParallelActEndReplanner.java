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
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.queuesim.QueueVehicle;
import org.matsim.core.population.ActivityImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * A class for running {@link ActEndReplanner} in parallel using threads.
 *
 * @author Christoph Dobler
 */
public class ParallelActEndReplanner extends ParallelReplanner {
	
	private final static Logger log = Logger.getLogger(ParallelActEndReplanner.class);
	
	private CyclicBarrier timeStepStartBarrier;
	private CyclicBarrier timeStepEndBarrier;
	
	private static ReplannerThread[] replannerThreads;
	
	public void run(List<ActivityImpl> fromActs, List<QueueVehicle> vehicles, double time)
	{	
		/*
		 *  distribute workload between threads
		 *  as long as threads are waiting we don't need synchronized data structures
		 */ 
		for(int i = 0; i < vehicles.size(); i++)
		{
			QueueVehicle vehicle = vehicles.get(i);
			ActivityImpl fromAct = fromActs.get(i);

			replannerThreads[i % numOfThreads].handleVehicle(vehicle, fromAct);
		}

		try
		{
			// set current Time
			ReplannerThread.setTime(time);
				
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
    
	@Override
	public void init()
	{	
		this.timeStepStartBarrier = new CyclicBarrier(numOfThreads + 1);
		this.timeStepEndBarrier = new CyclicBarrier(numOfThreads + 1);
		
		replannerThreads = new ReplannerThread[numOfThreads];
		
		// setup threads
		for (int i = 0; i < numOfThreads; i++) 
		{
			ReplannerThread replannerThread = new ReplannerThread(i, replannerArray, replanners);
			replannerThread.setName("ParallelActEndReplanner" + i);
			replannerThread.setCyclicTimeStepStartBarrier(this.timeStepStartBarrier);
			replannerThread.setCyclicTimeStepEndBarrier(this.timeStepEndBarrier);
			
			replannerThreads[i] = replannerThread;
			
			replannerThread.start();
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

	/**
	 * The thread class that really handles the persons.
	 */
	private static class ReplannerThread extends Thread
	{
		private static double time = 0.0;
		private static boolean simulationRunning = true;
		
		private final int threadId;
		private final ArrayList<PlanAlgorithm> replanners;
		private final PlanAlgorithm[][] replannerArray;
		private final LinkedList<QueueVehicle> vehicles = new LinkedList<QueueVehicle>();
		private final LinkedList<ActivityImpl> fromActs = new LinkedList<ActivityImpl>();

		private CyclicBarrier timeStepStartBarrier;
		private CyclicBarrier timeStepEndBarrier;
		
		public ReplannerThread(final int i, final PlanAlgorithm replannerArray[][], final ArrayList<PlanAlgorithm> replanners)
		{
			this.threadId = i;
			this.replannerArray = replannerArray;
			this.replanners = replanners;
		}
		
		public static void setTime(final double t)
		{
			time = t;
		}
		
		public void setCyclicTimeStepStartBarrier(CyclicBarrier barrier)
		{
			this.timeStepStartBarrier = barrier;
		}
		
		public void setCyclicTimeStepEndBarrier(CyclicBarrier barrier)
		{
			this.timeStepEndBarrier = barrier;
		}
		
		public void handleVehicle(final QueueVehicle vehicle, final ActivityImpl fromAct)
		{
			this.vehicles.add(vehicle);
			this.fromActs.add(fromAct);
		}

		@Override
		public void run()
		{
			while (simulationRunning)
			{
				try
				{
					/*
					 * The End of the Replanning is synchronized with 
					 * the TimeStepEndBarrier. If all Threads reach this Barrier
					 * the main run() Thread can go on.
					 * 
					 * The Threads wait now at the TimeStepStartBarrier until
					 * they are triggered again in the next TimeStep by the main run()
					 * method.
					 */
					timeStepEndBarrier.await();
						
					timeStepStartBarrier.await();

//					int numRuns = 0;
					
					while(vehicles.peek() != null)
					{
						QueueVehicle vehicle = vehicles.poll();
						ActivityImpl fromAct = fromActs.poll();
						
						// replanner of the person
						PlanAlgorithm replanner = (PlanAlgorithm)vehicle.getDriver().getPerson().getCustomAttributes().get("Replanner");
							
						// get the index of the Replanner in the replanners Array
						int index = replanners.indexOf(replanner);
							
						// get the replanner or a clone if it, if it's not the first running thread
						replanner = this.replannerArray[index][threadId];		
						
						new ActEndReplanner(fromAct, vehicle, time, replanner);
//						log.info("Did Act End Replanning...");
						
//						numRuns++;
//						if (numRuns % 500 == 0) log.info("created new Plan for " + numRuns + " persons in thread " + threadId);
					}
					
//					log.info("Thread " + threadId + " done.");
				}
				catch (InterruptedException e)
				{
					log.error("Something is going wrong here...");
					Gbl.errorMsg(e);
				}
	            catch (BrokenBarrierException e)
	            {
	            	Gbl.errorMsg(e);
	            }

			}	// while Simulation Running
			
		}	// run()
		
	}	// ReplannerThread
	
}