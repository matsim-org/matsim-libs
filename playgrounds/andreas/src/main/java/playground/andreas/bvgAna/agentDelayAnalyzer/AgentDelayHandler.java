/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.andreas.bvgAna.agentDelayAnalyzer;

import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;

/**
 * Collects the <code>AgentDepartureEventHandler</code> and the corresponding <code>PersonEntersVehicleEventHandler</code> for a given set of agent ids.
 * 
 * @author aneumann
 *
 */
public class AgentDelayHandler implements AgentDepartureEventHandler, PersonEntersVehicleEventHandler{
	
	private final Logger log = Logger.getLogger(AgentDelayHandler.class);
	private final Level logLevel = Level.DEBUG;
	
	private final Set<Id> agentIds;
	private TreeMap<Id, AgentDelayAtStopContainer> stopId2DelayAtStopMap = new TreeMap<Id, AgentDelayAtStopContainer>();
	
	public AgentDelayHandler(Set<Id> agentIds){
		this.log.setLevel(this.logLevel);
		this.agentIds = agentIds;
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		
		if(this.agentIds.contains(event.getPersonId())){
			
			if(event.getLegMode() == TransportMode.pt){
		
				if(this.stopId2DelayAtStopMap.get(event.getPersonId()) == null){
					this.log.debug("Adding new AgentDelayAtStopContainer for agent " + event.getPersonId() + " to map.");
					this.stopId2DelayAtStopMap.put(event.getPersonId(), new AgentDelayAtStopContainer(event.getPersonId()));
				}

				this.stopId2DelayAtStopMap.get(event.getPersonId()).addAgentDepartureEvent(event);
			} else {
				this.log.debug("AgentDepartureEvent but no pt");
			}
		}
	}

	@Override
	public void reset(int iteration) {
		this.log.debug("reset method in iteration " + iteration + " not implemented, yet");
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		
		if(this.agentIds.contains(event.getPersonId())){
			this.stopId2DelayAtStopMap.get(event.getPersonId()).addPersonEntersVehicleEvent(event);
		}
		
	}

	/**
	 * Returns departure and enter vehicle time information.
	 * 
	 * @return A map containing a <code>AgentDelayAtStopContainer</code> for each agent id
	 */
	public TreeMap<Id, AgentDelayAtStopContainer> getStopId2DelayAtStopMap() {
		return this.stopId2DelayAtStopMap;
	}
	
	

}
