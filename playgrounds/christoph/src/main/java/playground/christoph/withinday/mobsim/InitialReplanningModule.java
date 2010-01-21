/* *********************************************************************** *
 * project: org.matsim.*
 * InitialReplanningModule.java
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

package playground.christoph.withinday.mobsim;

import org.apache.log4j.Logger;
import org.matsim.core.mobsim.queuesim.DriverAgent;
import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.core.mobsim.queuesim.QueueVehicle;

import playground.christoph.withinday.replanning.parallel.ParallelInitialReplanner;

public class InitialReplanningModule implements WithinDayReplanningModule{

	private final static Logger log = Logger.getLogger(InitialReplanningModule.class);
	
	protected ReplanningQueueSimulation simulation;
	protected ParallelInitialReplanner parallelInitialReplanner;
	
	public static int replanningCounter = 0;
	
	public InitialReplanningModule(ParallelInitialReplanner parallelInitialReplanner, ReplanningQueueSimulation simulation)
	{
		this.simulation = simulation;
		this.parallelInitialReplanner = parallelInitialReplanner;
	}
	
	public void doReplanning(double time)
	{
		int replanningCounter = 0;
		for (QueueLink queueLink : simulation.getQueueNetwork().getLinks().values())
		{
			for (QueueVehicle vehicle : queueLink.getAllVehicles())
			{
				DriverAgent driverAgent = vehicle.getDriver();
				
				boolean replanning = (Boolean) driverAgent.getPerson().getCustomAttributes().get("initialReplanning");
				
				if (replanning)
				{
					this.parallelInitialReplanner.addAgentToReplan(driverAgent);
					replanningCounter++;
				}
			}
		}
		
		boolean runReplanning = replanningCounter > 0;
		if (runReplanning) this.parallelInitialReplanner.run(time);
	}

	public void setRemoveKnowledge(boolean value)
	{
		this.parallelInitialReplanner.setRemoveKnowledge(value);
	}

}
