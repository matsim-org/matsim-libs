/* *********************************************************************** *
 * project: org.matsim.*
 * CottbusFootballTraveltimeHandler
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

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

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
public class CottbusFootballTraveltimeHandler implements LinkEnterEventHandler,
		LinkLeaveEventHandler, AgentArrivalEventHandler, AgentDepartureEventHandler {

	private Map<Id, Double> arrivaltimesSPN2FB;
	private Map<Id, Double> arrivaltimesCB2FB;

	private Map<Id, Double> arrivaltimesFB2SPN;
	private Map<Id, Double> arrivaltimesFB2CB;
	private Map<Id, Double> personId2TravelTimeMap;

	public CottbusFootballTraveltimeHandler(){
		this.reset(0);
	}
	
	@Override
	public void reset(int iteration) {
		this.personId2TravelTimeMap = new TreeMap<Id, Double>();
		this.arrivaltimesFB2CB = new TreeMap<Id, Double>();
		this.arrivaltimesFB2SPN = new TreeMap<Id, Double>();
		this.arrivaltimesCB2FB = new TreeMap<Id, Double>();
		this.arrivaltimesSPN2FB = new TreeMap<Id, Double>();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Double agentTt = this.personId2TravelTimeMap.get(event.getPersonId());
		this.personId2TravelTimeMap.put(event.getPersonId(), agentTt - event.getTime());
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Double agentTt = this.personId2TravelTimeMap.get(event.getPersonId());
		this.personId2TravelTimeMap.put(event.getPersonId(), agentTt + event.getTime());
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		Double agentTt = this.personId2TravelTimeMap.get(event.getPersonId());
		this.personId2TravelTimeMap.put(event.getPersonId(), agentTt + event.getTime());

		if (event.getPersonId().toString().endsWith(CottbusFootballStrings.SPN2FB)) {
			Double tr = this.arrivaltimesSPN2FB.get(event.getPersonId());
			if (tr == null) {
				this.arrivaltimesSPN2FB.put(event.getPersonId(), event.getTime());
			}
			else {
				this.arrivaltimesFB2SPN.put(event.getPersonId(), event.getTime());
			}
		}
		if (event.getPersonId().toString().endsWith(CottbusFootballStrings.CB2FB)) {
			Double tr = this.arrivaltimesCB2FB.get(event.getPersonId());
			if (tr == null) {
				this.arrivaltimesCB2FB.put(event.getPersonId(), event.getTime());
			}
			else {
				this.arrivaltimesFB2CB.put(event.getPersonId(), event.getTime());
			}
		}
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		Double agentTt = this.personId2TravelTimeMap.get(event.getPersonId());
		if (agentTt == null) {
			this.personId2TravelTimeMap.put(event.getPersonId(), 0 - event.getTime());
		}
		else {
			this.personId2TravelTimeMap.put(event.getPersonId(), agentTt - event.getTime());
		}
	}

	public double getAverageTravelTime() {
		Double att = 0.0;
		for (Entry<Id, Double> entry : personId2TravelTimeMap.entrySet()) {
			att += entry.getValue();
		}
		att = att / personId2TravelTimeMap.size();
		return att;
	}

	public Map<Id, Double> getArrivalTimesCB2FB() {
		return this.arrivaltimesCB2FB;
	}

	public Map<Id, Double> getArrivalTimesFB2CB() {
		return this.arrivaltimesFB2CB;
	}

	public Map<Id, Double> getArrivalTimesSPN2FB() {
		return this.arrivaltimesSPN2FB;
	}

	public Map<Id, Double> getArrivalTimesFB2SPN() {
		return this.arrivaltimesFB2SPN;
	}

}
