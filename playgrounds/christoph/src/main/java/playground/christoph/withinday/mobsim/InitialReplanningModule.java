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

import java.util.List;

import org.matsim.core.mobsim.queuesim.DriverAgent;

import playground.christoph.withinday.replanning.ReplanningTask;
import playground.christoph.withinday.replanning.WithinDayInitialReplanner;
import playground.christoph.withinday.replanning.WithinDayReplanner;
import playground.christoph.withinday.replanning.identifiers.interfaces.AgentsToReplanIdentifier;
import playground.christoph.withinday.replanning.parallel.ParallelInitialReplanner;

public class InitialReplanningModule extends WithinDayReplanningModule{
	
	public InitialReplanningModule(ParallelInitialReplanner parallelInitialReplanner)
	{
		this.parallelReplanner = parallelInitialReplanner;
	}
	
	public void setRemoveKnowledge(boolean value)
	{
		((ParallelInitialReplanner)this.parallelReplanner).setRemoveKnowledge(value);
	}

	public void doReplanning(double time)
	{
		for (WithinDayReplanner replanner : this.parallelReplanner.getWithinDayReplanners())
		{
			if(replanner instanceof WithinDayInitialReplanner)
			{
				List<AgentsToReplanIdentifier> identifiers = replanner.getAgentsToReplanIdentifers();
				
				for (AgentsToReplanIdentifier identifier : identifiers)
				{
					for (DriverAgent driverAgent : identifier.getAgentsToReplan(time, replanner))
					{
						ReplanningTask replanningTask = new ReplanningTask(driverAgent, replanner.getId());
						this.parallelReplanner.addReplanningTask(replanningTask);
					}
				}
			}
		}
		
		this.parallelReplanner.run(time);
	}
}
