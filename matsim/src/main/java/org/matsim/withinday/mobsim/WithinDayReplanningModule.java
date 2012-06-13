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

import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.withinday.replanning.identifiers.interfaces.Identifier;
import org.matsim.withinday.replanning.parallel.ParallelReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayReplannerFactory;
import org.matsim.withinday.replanning.replanners.tools.ReplanningTask;

public abstract class WithinDayReplanningModule<T extends WithinDayReplannerFactory<? extends Identifier>> {

	protected ParallelReplanner<T> parallelReplanner;
	protected final Random random = MatsimRandom.getLocalInstance();
	
	public void doReplanning(double time) {
		for (T factory : this.parallelReplanner.getWithinDayReplannerFactories()) {
			Set<? extends Identifier> identifiers = factory.getIdentifers(); 
			Id id = factory.getId();
			double replanningProbability = factory.getReplanningProbability();
			
			for (Identifier identifier : identifiers) {
				for (PlanBasedWithinDayAgent withinDayAgent : identifier.getAgentsToReplan(time)) {
					
					/*
					 * Check whether the current Agent should be replanned based on the 
					 * replanning probability. If not, continue with the next one.
					 */
					if (!replanAgent(replanningProbability)) continue;
					
					ReplanningTask replanningTask = new ReplanningTask(withinDayAgent, id);
					this.parallelReplanner.addReplanningTask(replanningTask);
				}
			}
		}
		
		this.parallelReplanner.run(time);
	}
	
	/*
	 * Based on a random number it is decided whether an agent should do a replanning or not.
	 * number <= replanningProbability: do replanning 
	 * else: no replanning
	 * 
	 * TODO: 
	 * Replace the random object by another random number generator that uses a combination 
	 * of the agent's id and the time as seed. When doing so, this could be shifted to the 
	 * parallel running threads without becoming non-deterministic.
	 */
	private final boolean replanAgent(double replanningProbability) {
		double rand = random.nextDouble();
		if (rand <= replanningProbability) return true;
		else return false;
	}
}
