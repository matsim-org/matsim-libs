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
	
	private static ReplannerThread[] replannerThreads;
	private static ThreadLocker threadLocker;
	
	public void run(ArrayList<ActivityImpl> fromActs, ArrayList<QueueVehicle> vehicles, double time)
	{		
		// distribute workload between threads
		// as long as threads are waiting we don't need synchronized data structures
		for(int i = 0; i < vehicles.size(); i++)
		{
			QueueVehicle vehicle = vehicles.get(i);
			ActivityImpl fromAct = fromActs.get(i);

			replannerThreads[i % numOfThreads].handleVehicle(vehicle, fromAct);
		}

		try
		{
			// lock threadLocker until wait() statement listens for the notifies
			synchronized(threadLocker) 
			{
				// set current Time
				ReplannerThread.setTime(time);
							
				threadLocker.setCounter(replannerThreads.length);
				threadLocker.time = time;
				
				for (ReplannerThread replannerThread : replannerThreads)
				{
					replannerThread.startReplanning();		
				}
	
				threadLocker.wait();
			}		
		} 
		catch (InterruptedException e)
		{
			Gbl.errorMsg(e);
		}
	}
	
	public static synchronized boolean isRunning()
	{
		boolean stillReplanning = false;
		// if replanning is finished in all Threads
		for (ReplannerThread replannerThread : replannerThreads)
		{
			if (!replannerThread.isWaiting())
			{
				stillReplanning = true;
				break;
			}			
		}
		return stillReplanning;
	}
    
	public static void init()
	{
		threadLocker = new ThreadLocker();
		
		ReplannerThread.setThreadLocker(threadLocker);
		
		replannerThreads = new ReplannerThread[numOfThreads];
		
		// setup threads
		for (int i = 0; i < numOfThreads; i++) 
		{
			ReplannerThread replannerThread = new ReplannerThread(i, replannerArray, replanners);
			replannerThreads[i] = replannerThread;
			
			replannerThread.start();
		}
	}

	private static class ThreadLocker
	{
		private int count = 0;
		public double time;
		
		public synchronized void setCounter(int i)
		{
			this.count = i;
		}
				
		public synchronized void incCounter()
		{
			count++;
		}
		
		public synchronized void decCounter()
		{
			count--;
//			log.info("Count: " + count + ", Time: " + time);
			if(count == 0)
			{
//				log.info("Notify " + time);
//				alreadyNotified = true;
				notify();
			}
		}
	}

	/**
	 * The thread class that really handles the persons.
	 */
	//private static class ReplannerThread implements Runnable
	private static class ReplannerThread extends Thread
	{
		private static ThreadLocker threadLocker;		
		private static double time = 0.0;
		private static boolean simulationRunning = true;
		
		private final int threadId;
		private final ArrayList<PlanAlgorithm> replanners;
		private final PlanAlgorithm[][] replannerArray;
		private final LinkedList<QueueVehicle> vehicles = new LinkedList<QueueVehicle>();
		private final LinkedList<ActivityImpl> fromActs = new LinkedList<ActivityImpl>();

		private boolean isWaiting = true;

		public ReplannerThread(final int i, final PlanAlgorithm replannerArray[][], final ArrayList<PlanAlgorithm> replanners)
		{
			this.threadId = i;
			this.replannerArray = replannerArray;
			this.replanners = replanners;
		}

		public static void setThreadLocker (ThreadLocker tl)
		{
			threadLocker = tl;
		}
		
		public static void setTime(final double t)
		{
			time = t;
		}
		
		public void handleVehicle(final QueueVehicle vehicle, final ActivityImpl fromAct)
		{
			this.vehicles.add(vehicle);
			this.fromActs.add(fromAct);
		}

		public void startReplanning()
		{
			synchronized(this)
			{
				notify();
			}
		}
		
		public boolean isWaiting()
		{
			return isWaiting;
		}
		
		@Override
		public void run()
		{
			while (simulationRunning)
			{
				try
				{
					/*
					 * thradLocker.decCounter() and wait() have to be
					 * executed in a synchronized block! Otherwise it could
					 * happen, that the replanning ends and the replanning of
					 * the next SimStep executes the notify command before
					 * wait() is called -> we have a DeadLock!
					 */
					synchronized(this)
					{
						threadLocker.decCounter();
						wait();
						isWaiting = false;
					}

					//log.info("Runnning: " + time);

					int numRuns = 0;
					
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
						
						numRuns++;
						if (numRuns % 500 == 0) log.info("created new Plan for " + numRuns + " persons in thread " + threadId);
					}
					
//					log.info("Thread " + threadId + " done.");
				}
				catch (InterruptedException ie)
				{
					log.error("Something is going wrong here...");
				}
							
				finally
				{
					isWaiting = true;
				}
			}	// while Simulation Running
			
		}	// run()
		
	}	// ReplannerThread
	
}