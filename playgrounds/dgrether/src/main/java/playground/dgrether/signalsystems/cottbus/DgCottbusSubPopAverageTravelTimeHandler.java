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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;

/**
 * @author dgrether
 * 
 */
public class DgCottbusSubPopAverageTravelTimeHandler implements PersonArrivalEventHandler, PersonDepartureEventHandler {

	private static final Logger log = Logger.getLogger(DgCottbusSubPopAverageTravelTimeHandler.class);
	
	private Double travelTimeFootball = 0.0;
	private Double travelTimeCommuter = 0.0;
	
	private Map<Id<Person>, Double> departureTimePerPerson = new HashMap<>();
	
	private Set<Id<Person>> footballPersonIds = new HashSet<>();
	private Set<Id<Person>> commuterPersonIds = new HashSet<>();
	
	
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
		this.departureTimePerPerson.clear();
		this.footballPersonIds.clear();
		this.commuterPersonIds.clear();
	}

	private boolean isFootballId(Id<Person> id){
		if (id.toString().contains("FB")){
			return true;
		}
		return false;
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		double personTT = event.getTime() - this.departureTimePerPerson.get(event.getPersonId());
		if (isFootballId(event.getPersonId())){
			this.travelTimeFootball += personTT;
		}
		else {
			this.travelTimeCommuter += personTT;
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.departureTimePerPerson.put(event.getPersonId(), event.getTime());
		
		if (isFootballId(event.getPersonId())){
			if (! footballPersonIds.contains(event.getPersonId())){
				footballPersonIds.add(event.getPersonId());
			}
		}
		else {
			if (! commuterPersonIds.contains(event.getPersonId())){
				this.commuterPersonIds.add(event.getPersonId());
			}
		}
	}




}
