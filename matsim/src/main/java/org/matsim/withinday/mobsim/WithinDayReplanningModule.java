/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayReplanningModule.java
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

package org.matsim.withinday.mobsim;

import java.util.Set;

import org.matsim.ptproject.qsim.agents.WithinDayAgent;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentsToReplanIdentifier;
import org.matsim.withinday.replanning.parallel.ParallelReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayReplanner;
import org.matsim.withinday.replanning.replanners.tools.ReplanningTask;


public abstract class WithinDayReplanningModule<T extends WithinDayReplanner<? extends AgentsToReplanIdentifier>> {

	protected ParallelReplanner<T> parallelReplanner;

	// yyyy how to make this final? Add an additional hook to disable replanning? cdobler, Oct'10
	public void doReplanning(double time) {
		for (T replanner : this.parallelReplanner.getWithinDayReplanners()) {
			Set<? extends AgentsToReplanIdentifier> identifiers = replanner.getAgentsToReplanIdentifers(); 
				
			for (AgentsToReplanIdentifier identifier : identifiers) {
				for (WithinDayAgent withinDayAgent : identifier.getAgentsToReplan(time, replanner.getId())) {
					ReplanningTask replanningTask = new ReplanningTask(withinDayAgent, replanner.getId());
					this.parallelReplanner.addReplanningTask(replanningTask);
				}
			}
		}
		
		this.parallelReplanner.run(time);
	}
}
