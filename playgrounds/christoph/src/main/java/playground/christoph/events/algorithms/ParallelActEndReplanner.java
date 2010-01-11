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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.queuesim.DriverAgent;
import org.matsim.core.mobsim.queuesim.PersonAgent;
import org.matsim.core.mobsim.queuesim.QueueVehicle;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
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
	
	private int roundRobin = 0;
	
	private ReplannerThread[] replannerThreads;
	
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
    
	public void run(double time)
	{	
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

	public void addScheduledActivityEnd(DriverAgent driverAgent)
	{
		this.replannerThreads[this.roundRobin % this.numOfThreads].addScheduledActivityEnd(driverAgent);
		this.roundRobin++;
	}
	
	/*
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
		private final LinkedList<DriverAgent> endingActivities = new LinkedList<DriverAgent>();
		
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

		public void addScheduledActivityEnd(DriverAgent driverAgent)
		{			
			endingActivities.add(driverAgent);
		}
		
		/*
		 * Collects the Vehicles and the current Activities of
		 * the Agents in the EndingActivities List.
		 */
		private void getReplanningData()
		{
			DriverAgent driverAgent;
			while ((driverAgent = endingActivities.poll()) != null)
			{
				// Skip Agent if Replanning Flag is not set
				boolean replanning = (Boolean)driverAgent.getPerson().getCustomAttributes().get("endActivityReplanning");
				if (!replanning) continue;
					
				PersonImpl person = (PersonImpl) driverAgent.getPerson();				
				PersonAgent pa = (PersonAgent) driverAgent;					
						
				// New approach using non deprecated Methods
				// The Person is currently at an Activity and is going to leave it.
				// The Person's CurrentLeg should point to the leg that leads to that Activity...
				List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
				
				Leg leg = pa.getCurrentLeg();
				
				ActivityImpl fromAct = null;
				
				// first Activity is running - there is no previous Leg
				if (leg == null)
				{
					fromAct = (ActivityImpl)planElements.get(0);
				}
				else
				{
					int index = planElements.indexOf(leg);
					// If the leg is part of the Person's plan
					if (index >= 0)
					{
						fromAct = (ActivityImpl)planElements.get(index + 1);
					}
				}
				
				if (fromAct == null)
				{
					log.error("Found fromAct that is null!");
				}
				
				this.handleVehicle(pa.getVehicle(), fromAct);
			}
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
					
					getReplanningData();
					
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