/* *********************************************************************** *
 * project: org.matsim.*
 * ActEndReplanningModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.christoph.mobsim;

import java.util.concurrent.PriorityBlockingQueue;

import org.apache.log4j.Logger;
import org.matsim.core.mobsim.queuesim.DriverAgent;

import playground.christoph.events.algorithms.ParallelActEndReplanner;

public class ActEndReplanningModule {

	private final static Logger log = Logger.getLogger(ActEndReplanningModule.class);
	
	protected ReplanningQueueSimulation simulation;
	protected ParallelActEndReplanner parallelActEndReplanner;
	
	public static int replanningCounter = 0;
	
	public ActEndReplanningModule(ParallelActEndReplanner parallelActEndReplanner, ReplanningQueueSimulation simulation)
	{
		this.simulation = simulation;
		this.parallelActEndReplanner = parallelActEndReplanner;
	}
		
	public void doActEndReplanning(double time)
	{	
		PriorityBlockingQueue<DriverAgent> queue = simulation.getActivityEndsList();
		
		for (DriverAgent driverAgent : queue)
		{			
			// If the Agent will depart
			if (driverAgent.getDepartureTime() <= time)
			{	
				this.parallelActEndReplanner.addScheduledActivityEnd(driverAgent);
				replanningCounter++;
			}
			
			// It's a priority Queue -> no further Agents will be found
			else break;
		}		
		
		DriverAgent driverAgent = queue.peek();
		boolean runReplanning = (driverAgent != null && driverAgent.getDepartureTime() <= time);
		if (runReplanning) this.parallelActEndReplanner.run(time);
	}
}
