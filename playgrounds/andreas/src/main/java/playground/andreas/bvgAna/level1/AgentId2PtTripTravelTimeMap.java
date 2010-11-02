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

package playground.andreas.bvgAna.level1;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.pt.PtConstants;

/**
 * Calculates the trip travel time and the number of transfers needed for each given agent id and pt trip.
 * A pt trip is considered to start at a non pt interaction activity and to end at the following non pt interaction activity.
 *  
 * @author aneumann
 *
 */
public class AgentId2PtTripTravelTimeMap implements ActivityStartEventHandler, ActivityEndEventHandler, AgentDepartureEventHandler, AgentArrivalEventHandler{
	
	private final Logger log = Logger.getLogger(AgentId2PtTripTravelTimeMap.class);
	private final Level logLevel = Level.DEBUG;
	
	private Set<Id> agentIds;
	private TreeMap<Id, ArrayList<AgentId2PtTripTravelTimeMapData>> agentId2PtTripTravelTimeMap = new TreeMap<Id, ArrayList<AgentId2PtTripTravelTimeMapData>>();
	private TreeMap<Id, AgentId2PtTripTravelTimeMapData> tempList = new TreeMap<Id, AgentId2PtTripTravelTimeMapData>();
	
	public AgentId2PtTripTravelTimeMap(Set<Id> agentIds){
		this.log.setLevel(this.logLevel);
		this.agentIds = agentIds;
	}
	
	/**
	 * @return Returns a map containing a <code>AgentId2PtTripTravelTimeMapData</code> for each pt trip for each agent id given
	 */
	public TreeMap<Id, ArrayList<AgentId2PtTripTravelTimeMapData>> getAgentId2PtTripTravelTimeMap(){
		return this.agentId2PtTripTravelTimeMap;
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if(this.agentIds.contains(event.getPersonId()) && !event.getActType().equalsIgnoreCase(PtConstants.TRANSIT_ACTIVITY_TYPE)){
			// check, if there is a valid entry, if so add it
			if(this.tempList.get(event.getPersonId()) != null){
				if(this.agentId2PtTripTravelTimeMap.get(event.getPersonId()) == null){
					this.agentId2PtTripTravelTimeMap.put(event.getPersonId(), new ArrayList<AgentId2PtTripTravelTimeMapData>());
				}
				this.tempList.get(event.getPersonId()).setStartEvent(event);
				this.agentId2PtTripTravelTimeMap.get(event.getPersonId()).add(this.tempList.remove(event.getPersonId()));
			}
		}		
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if(this.agentIds.contains(event.getPersonId()) && !event.getActType().equalsIgnoreCase(PtConstants.TRANSIT_ACTIVITY_TYPE)){
			// Register a new leg
			this.tempList.put(event.getPersonId(), new AgentId2PtTripTravelTimeMapData(event));
		}
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if(this.agentIds.contains(event.getPersonId())){
			if(event.getLegMode().equalsIgnoreCase(TransportMode.transit_walk) || event.getLegMode().equalsIgnoreCase(TransportMode.pt)){
				this.tempList.get(event.getPersonId()).handle(event);
			} else {
				this.log.debug("Leg mode " + event.getLegMode() + " is not of interest");
				this.tempList.remove(event.getPersonId());
			}
		}
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if(this.agentIds.contains(event.getPersonId())){
			if(event.getLegMode().equalsIgnoreCase(TransportMode.transit_walk) || event.getLegMode().equalsIgnoreCase(TransportMode.pt)){
				this.tempList.get(event.getPersonId()).handle(event);
			} else {
				this.log.debug("Leg mode " + event.getLegMode() + " is not of interest");
				this.tempList.remove(event.getPersonId());
			}
		}
	}

	@Override
	public void reset(int iteration) {
		this.log.debug("reset method in iteration " + iteration + " not implemented, yet");
	}	

}
