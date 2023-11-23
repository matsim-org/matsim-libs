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

package playground.vsp.andreas.bvgAna.level1;

import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;

/**
 * Collects the <code>AgentDepartureEventHandler</code> and the corresponding <code>PersonEntersVehicleEventHandler</code> for a given set of agent ids.
 *
 * @author aneumann
 *
 */
public class AgentId2DepartureDelayAtStopMap implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler{

	private final Logger log = LogManager.getLogger(AgentId2DepartureDelayAtStopMap.class);
//	private final Level logLevel = Level.OFF;

	private final Set<Id<Person>> agentIds;
	private TreeMap<Id<Person>, AgentId2DepartureDelayAtStopMapData> stopId2DelayAtStopMap = new TreeMap<>();

	public AgentId2DepartureDelayAtStopMap(Set<Id<Person>> agentIds){
//		this.log.setLevel(this.logLevel);
		this.agentIds = agentIds;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {

		if(this.agentIds.contains(event.getPersonId())){

			if(event.getLegMode() == TransportMode.pt){

				if(this.stopId2DelayAtStopMap.get(event.getPersonId()) == null){
//					this.log.debug("Adding new AgentDelayAtStopContainer for agent " + event.getDriverId() + " to map.");
					this.stopId2DelayAtStopMap.put(event.getPersonId(), new AgentId2DepartureDelayAtStopMapData(event.getPersonId()));
				}

				this.stopId2DelayAtStopMap.get(event.getPersonId()).addAgentDepartureEvent(event);
//			} else {
//				this.log.debug("AgentDepartureEvent but no pt");
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
	public TreeMap<Id<Person>, AgentId2DepartureDelayAtStopMapData> getStopId2DelayAtStopMap() {
		return this.stopId2DelayAtStopMap;
	}



}
