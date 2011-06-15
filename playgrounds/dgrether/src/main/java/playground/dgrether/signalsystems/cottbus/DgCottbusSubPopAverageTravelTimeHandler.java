/* *********************************************************************** *
 * project: org.matsim.*
 * SubPopAverageTravelTimeHandler
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
package playground.dgrether.signalsystems.cottbus;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;

/**
 * @author dgrether
 * 
 */
public class DgCottbusSubPopAverageTravelTimeHandler implements LinkEnterEventHandler,
		LinkLeaveEventHandler, AgentArrivalEventHandler, AgentDepartureEventHandler {

	private static final Logger log = Logger.getLogger(DgCottbusSubPopAverageTravelTimeHandler.class);
	
	private double travelTimeCommuter = 0.0;
	private double travelTimeFootball = 0.0;
	
	private Set<Id> footballPersonIds = new HashSet<Id>();
	private Set<Id> commuterPersonIds = new HashSet<Id>();
	
	
	public DgCottbusSubPopAverageTravelTimeHandler() {}
	
	public double getFootballAvgTT() {
		log.info("found " + footballPersonIds.size() + " football travellers with total travel time: " + this.travelTimeFootball);
		return this.travelTimeFootball / this.footballPersonIds.size();
	}
	
	public double getCommuterAvgTT() {
		log.info("found " + commuterPersonIds.size() + " commuter travellers with total travel time: " + this.travelTimeCommuter);
		return this.travelTimeCommuter / this.commuterPersonIds.size();
	}

	@Override
	public void reset(int iteration) {
		this.travelTimeCommuter = 0.0;
		this.travelTimeFootball = 0.0;
		this.footballPersonIds.clear();
		this.commuterPersonIds.clear();
	}

	private boolean isFootballId(Id id){
		if (id.toString().contains("FB")){
			return true;
		}
		return false;
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (isFootballId(event.getPersonId())){
			this.travelTimeFootball -= event.getTime();
		}
		else {
			this.travelTimeCommuter -= event.getTime();
			
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (isFootballId(event.getPersonId())){
			this.travelTimeFootball += event.getTime();
		}
		else {
			this.travelTimeCommuter += event.getTime();
		}
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (isFootballId(event.getPersonId())){
			this.travelTimeFootball += event.getTime();
		}
		else {
			this.travelTimeCommuter += event.getTime();
		}
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (isFootballId(event.getPersonId())){
			if (! footballPersonIds.contains(event.getPersonId())){
				footballPersonIds.add(event.getPersonId());
			}
			this.travelTimeFootball -= event.getTime();
		}
		else {
			if (! commuterPersonIds.contains(event.getPersonId())){
				this.commuterPersonIds.add(event.getPersonId());
			}
			this.travelTimeCommuter -= event.getTime();
		}
	}




}
