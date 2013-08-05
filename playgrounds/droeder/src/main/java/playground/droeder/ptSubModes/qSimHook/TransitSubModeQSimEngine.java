/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.droeder.ptSubModes.qSimHook;

import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.pt.PTPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author droeder based on TransitQSimEngine (mrieser)
 * 
 * just changed handleDeparture-Method, but need to copy-paste handleAgentPTDeparture because it's private
 *
 */
class TransitSubModeQSimEngine extends TransitQSimEngine {
	private static final Logger log = Logger
			.getLogger(TransitSubModeQSimEngine.class);
	private QSim qSim;
	private TransitSchedule schedule;
	private Set<String> modes;

	/**
	 * @param queueSimulation
	 */
	protected TransitSubModeQSimEngine(QSim queueSimulation) {
		super(queueSimulation);
		this.qSim = queueSimulation;
		this.schedule = queueSimulation.getScenario().getTransitSchedule();
		this.modes = qSim.getScenario().getConfig().transit().getTransitModes();
	}
	
	// just copy and paste, because method is private in TransitQSimEngine
	private void handleAgentPTDeparture(final MobsimAgent planAgent, Id linkId) {
		// this puts the agent into the transit stop.
		Id accessStopId = ((PTPassengerAgent) planAgent).getDesiredAccessStopId();
		if (accessStopId == null) {
			// looks like this agent has a bad transit route, likely no
			// route could be calculated for it
			log.error("pt-agent doesn't know to what transit stop to go. Removing agent from simulation. Agent " + planAgent.getId().toString());
			this.qSim.getAgentCounter().decLiving();
			this.qSim.getAgentCounter().incLost();
			return;
		}
		TransitStopFacility stop = this.schedule.getFacilities().get(accessStopId);
		if (stop.getLinkId() == null || stop.getLinkId().equals(linkId)) {
			this.agentTracker.addAgentToStop(this.qSim.getSimTimer().getTimeOfDay(), (PTPassengerAgent) planAgent, stop.getId());
			super.getInternalInterface().registerAdditionalAgentOnLink(planAgent) ;
		} else {
			throw new TransitAgentTriesToTeleportException("Agent "+planAgent.getId() + " tries to enter a transit stop at link "+stop.getLinkId()+" but really is at "+linkId+"!");
		}
	}


	/**
	 * check not only TransportMode.pt
	 */
	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id linkId) {
		if (this.modes.contains(agent.getMode())) {
			handleAgentPTDeparture(agent, linkId);
			return true ;
		}
		return false ;
	}


}
