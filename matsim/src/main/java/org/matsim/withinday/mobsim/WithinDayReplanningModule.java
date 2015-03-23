/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayReplanningModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentSelector;
import org.matsim.withinday.replanning.parallel.ParallelReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayReplannerFactory;
import org.matsim.withinday.replanning.replanners.tools.ReplanningTask;

public abstract class WithinDayReplanningModule<T extends WithinDayReplannerFactory<? extends AgentSelector>> {

	protected ParallelReplanner<T> parallelReplanner;
	
	public void doReplanning(double time) {
		for (T factory : this.parallelReplanner.getWithinDayReplannerFactories()) {
			Set<? extends AgentSelector> identifiers = factory.getIdentifers(); 
			Id<WithinDayReplanner> id = factory.getId();
			
			for (AgentSelector identifier : identifiers) {
				for (MobsimAgent withinDayAgent : identifier.getAgentsToReplan(time)) {					
					ReplanningTask replanningTask = new ReplanningTask(withinDayAgent, id);
					this.parallelReplanner.addReplanningTask(replanningTask);
				}
			}
		}
		
		this.parallelReplanner.run(time);
	}
	

}
