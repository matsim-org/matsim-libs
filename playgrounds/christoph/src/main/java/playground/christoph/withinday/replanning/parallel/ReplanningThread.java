/* *********************************************************************** *
 * project: org.matsim.*
 * ReplanningThread.java
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
import java.util.LinkedList;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.queuesim.DriverAgent;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.christoph.withinday.replanning.WithinDayReplanner;

/*
 * Typical Replanner Implementations should be able to use this 
 * Class method without any Changes.
 * Override the doReplanning() Method to implement the Replanning functionality.
 */
public abstract class ReplanningThread extends Thread{

	private final static Logger log = Logger.getLogger(ReplanningThread.class);
	
	protected double time = 0.0;
	protected boolean simulationRunning = true;
	
	protected int threadId;
	protected ArrayList<PlanAlgorithm> replanners;
	protected PlanAlgorithm[][] replannerArray;
	protected LinkedList<DriverAgent> agentsToReplan = new LinkedList<DriverAgent>();
	protected WithinDayReplanner withinDayReplanner;
	
	protected CyclicBarrier timeStepStartBarrier;
	protected CyclicBarrier timeStepEndBarrier;
	
	public ReplanningThread(int threadId, PlanAlgorithm replannerArray[][], ArrayList<PlanAlgorithm> replanners)
	{
		this.threadId = threadId;
		this.replannerArray = replannerArray;
		this.replanners = replanners;
		this.withinDayReplanner = null;
	}
	
	public void setTime(double time)
	{
		this.time = time;
	}
	
	public void setSimulationRunning(boolean simulationRunning)
	{
		this.simulationRunning = simulationRunning;
	}
	
	public void setCyclicTimeStepStartBarrier(CyclicBarrier barrier)
	{
		this.timeStepStartBarrier = barrier;
	}
	
	public void setCyclicTimeStepEndBarrier(CyclicBarrier barrier)
	{
		this.timeStepEndBarrier = barrier;
	}

	public void addAgentToReplan(DriverAgent driverAgent)
	{			
		agentsToReplan.add(driverAgent);
	}
	
	/*
	 * Typical Replanner Implementations should be able to use 
	 * this method without any Changes.
	 */
	protected void doReplanning()
	{
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

				doReplanning();
			}
			catch (InterruptedException e)
			{
				Gbl.errorMsg(e);
			}
            catch (BrokenBarrierException e)
            {
            	Gbl.errorMsg(e);
            }

		}	// while Simulation Running
		
	}	// run()
	
}
