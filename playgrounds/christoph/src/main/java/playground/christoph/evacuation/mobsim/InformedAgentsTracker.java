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

package playground.christoph.evacuation.mobsim;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.utils.misc.Time;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.events.PersonInformationEvent;
import playground.christoph.evacuation.events.handler.PersonInformationEventHandler;

/**
 * To simplify the decision making process, we assume that all members of a household
 * are informed at the same time, i.e. the first household member that gets informed
 * immediately informs all other members.
 * 
 * @author cdobler
 */
public abstract class InformedAgentsTracker implements PersonInformationEventHandler, MobsimAfterSimStepListener {
	
	static final Logger log = Logger.getLogger(InformedAgentsTracker.class);
	
	/* time since last "info" */
	private int infoTime = 0;
	/*package*/ static final int INFO_PERIOD = 300;
	
	/*package*/ final int totalAgents;
	/*package*/ final Set<Id> informedAgents;
	
	private boolean allAgentsInformed = false;
	
	public InformedAgentsTracker(Population population) {
		this.informedAgents = new HashSet<Id>();
		
		this.totalAgents = population.getPersons().size();
	}

	public boolean allAgentsInformed() {
		return this.allAgentsInformed;
	}
	
	/*package*/ void informAgent(Id id) {
		this.informedAgents.add(id);
	}
	
	public boolean isAgentInformed(Id id) {
		if (allAgentsInformed) return true;
		return this.informedAgents.contains(id);
	}
	
	public Set<Id> getInformedAgents() {
		return this.informedAgents;
	}
		
	@Override
	public void reset(int iteration) {
		this.informedAgents.clear();
	}

	@Override
	public void handleEvent(PersonInformationEvent event) {
		this.informedAgents.add(event.getPersonId());
	}
	
	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {

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
		if (this.informedAgents.size() == totalAgents) {
			log.info("All agents have been informed at " + Time.writeTime(e.getSimulationTime()));
			printStatistics(time);
			this.allAgentsInformed = true;
			return;
		}
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