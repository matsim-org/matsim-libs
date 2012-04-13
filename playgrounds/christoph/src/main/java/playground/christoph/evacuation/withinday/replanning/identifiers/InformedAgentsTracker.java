/* *********************************************************************** *
 * project: org.matsim.*
 * InformedAgentsTracker.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.withinday.replanning.identifiers;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.utils.misc.Time;

import playground.christoph.evacuation.config.EvacuationConfig;

/*
 * To simplify the decision making process, we assume that all members of a household
 * are informed at the same time.
 */
public abstract class InformedAgentsTracker implements MobsimBeforeSimStepListener {
	
	static final Logger log = Logger.getLogger(InformedAgentsTracker.class);
	
	/* time since last "info" */
	private int infoTime = 0;
	private static final int INFO_PERIOD = 600;
	
	/*package*/ int totalAgents = 0;
	/*package*/ final Set<Id> informedAgents;
	/*package*/ final Set<Id> toBeInitiallyReplannedAgents;
	/*package*/ final Queue<Id> replannedAgentsInCurrentTimeStep;
	
	/*
	 * This is true if all agents have been informed and have
	 * performed an initial replanning. 
	 */
	private boolean allAgentsInformed = false;
	
	public InformedAgentsTracker() {
		this.informedAgents = new HashSet<Id>();
		this.replannedAgentsInCurrentTimeStep = new ConcurrentLinkedQueue<Id>();
		this.toBeInitiallyReplannedAgents = new HashSet<Id>();
	}

	public boolean allAgentsInformed() {
		return this.allAgentsInformed;
	}
	
	public boolean isAgentInformed(Id id) {
		return this.informedAgents.contains(id);
	}
	
	public boolean agentRequiresInitialReplanning(Id id) {
		return this.toBeInitiallyReplannedAgents.contains(id);
	}
	
	/*
	 * Collect all Agents that have been initially replanned in the
	 * current time step. At the end of the time step, they are marked
	 * as initially replanned. This avoids multiple replanners to
	 * handle them in the same time step.
	 */
	public void setAgentInitiallyReplannedInCurrentTimeStep(Id id) {
		this.replannedAgentsInCurrentTimeStep.add(id);
	}

	/*package*/ void setTotalAgentCount(int count) {
		this.totalAgents = count;
	}
	
	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {

		// If all agents have already been informed, there is nothing left to do.
		if (allAgentsInformed) return;

		double time = e.getSimulationTime();
		if (time >= this.infoTime) {
			this.infoTime += INFO_PERIOD;
			
			if (time >= EvacuationConfig.evacuationTime) this.printStatistics(time);
		}
		
		/*
		 * If all agents have been informed and have performed their initial
		 * replanning, we set the marker variable to true.
		 */
		if (this.toBeInitiallyReplannedAgents.size() == 0 && this.informedAgents.size() == totalAgents) {
			log.info("All agents have been informed at " + Time.writeTime(e.getSimulationTime()));
			printStatistics(time);
			this.allAgentsInformed = true;
			return;
		}
		
		/*
		 * Clear the list of agents who have been replanned in the last time step.
		 */
		for (Id id : this.replannedAgentsInCurrentTimeStep) {
			this.toBeInitiallyReplannedAgents.remove(id);
		}
		this.replannedAgentsInCurrentTimeStep.clear();
	}
	
	private void printStatistics(double time) {
		
		int informed = this.informedAgents.size();
		int notInformed = this.totalAgents - informed;
		
		DecimalFormat df = new DecimalFormat("#.##");
		
		log.info("Simulation at " + Time.writeTime(time) + ", Informed Agents Statistics: # total Agents=" + totalAgents
			+ ", # informed Agents=" + informed + "(" + df.format((100.0*informed)/totalAgents) + "%)"
			+ ", # not informed Agents=" + notInformed + "(" + df.format((100.0*notInformed)/totalAgents) + "%)");
	}

}
