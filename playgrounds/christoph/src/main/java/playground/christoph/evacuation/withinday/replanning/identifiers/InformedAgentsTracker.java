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
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.utils.misc.Time;

import playground.christoph.evacuation.config.EvacuationConfig;

/*
 * To simplify the decision making process, we assume that all members of a household
 * are informed at the same time.
 */
public abstract class InformedAgentsTracker implements MobsimBeforeSimStepListener, MobsimAfterSimStepListener {
	
	static final Logger log = Logger.getLogger(InformedAgentsTracker.class);
	
	/* time since last "info" */
	private int infoTime = 0;
	private static final int INFO_PERIOD = 600;
	
	/*package*/ final int totalAgents;
	/*package*/ final Set<Id> informedAgents;
	/*package*/ final Set<Id> notInformedAgents;
	/*package*/ final Set<Id> toBeInitiallyReplannedAgents;
	/*package*/ final Queue<Id> replannedAgentsInCurrentTimeStep;
	
	/*
	 * This is true if all agents have been informed and have
	 * performed an initial replanning. 
	 */
	private boolean allAgentsInformed = false;
	
	public InformedAgentsTracker(Set<Id> agentIds) {
		this.informedAgents = new HashSet<Id>();
		this.notInformedAgents = new HashSet<Id>(agentIds);
		this.replannedAgentsInCurrentTimeStep = new ConcurrentLinkedQueue<Id>();
		this.toBeInitiallyReplannedAgents = new HashSet<Id>();
		
		this.totalAgents = agentIds.size();
	}

	public boolean allAgentsInformed() {
		return this.allAgentsInformed;
	}
	
	public void informAgent(Id id) {
		this.informedAgents.add(id);
		this.notInformedAgents.remove(id);
	}
	
	public boolean isAgentInformed(Id id) {
		return this.informedAgents.contains(id);
	}
	
	public boolean isAgentNotInformed(Id id) {
		return this.notInformedAgents.contains(id);
	}
	
	public int getInformedAgentsCount() {
		return this.informedAgents.size();
	}
	
	public int getNotInformedAgentsCount() {
		return this.notInformedAgents.size();
	}
	
	public Set<Id> getInformedAgents() {
		return this.informedAgents;
	}
	
	public Set<Id> getNotInformedAgents() {
		return this.notInformedAgents;
	}
	
	public boolean addToBeInitiallyReplannedAgent(Id id) {
		return this.toBeInitiallyReplannedAgents.add(id);
	}
	
	public boolean agentRequiresInitialReplanning(Id id) {
		return this.toBeInitiallyReplannedAgents.contains(id);
	}
	
	public Set<Id> getAgentsRequiringInitialReplanning() {
		return this.toBeInitiallyReplannedAgents;
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
		if (this.toBeInitiallyReplannedAgents.size() == 0 
				&& this.informedAgents.size() == totalAgents 
				&& this.notInformedAgents.size() == 0) {
			log.info("All agents have been informed at " + Time.writeTime(e.getSimulationTime()));
			printStatistics(time);
			this.allAgentsInformed = true;
			return;
		}
	}
	
	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		/*
		 * Clear the list of agents who have been replanned in the time step.
		 */
		for (Id id : this.replannedAgentsInCurrentTimeStep) {
			boolean removed = this.toBeInitiallyReplannedAgents.remove(id);
			
			// debug
			if (!removed) {
				log.warn("Initially replanned an agent which was not marked as to be! " + id.toString());
			}
		}
		this.replannedAgentsInCurrentTimeStep.clear();
		
		// debug
		if (this.toBeInitiallyReplannedAgents.size() > 0) {
			log.warn("Found " + this.toBeInitiallyReplannedAgents.size() + " agents that should " +
					"have been replanned initally but they were not!");
		}
	}
	
	private void printStatistics(double time) {
		
		int informed = this.informedAgents.size();
		int notInformed = this.notInformedAgents.size();
		
		DecimalFormat df = new DecimalFormat("#.##");
		
		log.info("Simulation at " + Time.writeTime(time) + ", Informed Agents Statistics: # total Agents=" + totalAgents
			+ ", # informed Agents=" + informed + "(" + df.format((100.0*informed)/totalAgents) + "%)"
			+ ", # not informed Agents=" + notInformed + "(" + df.format((100.0*notInformed)/totalAgents) + "%)");
	}

}