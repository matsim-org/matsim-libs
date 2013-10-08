/* *********************************************************************** *
 * project: org.matsim.*
 * LinksEventHandler.java
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

/**
 * 
 */
package playground.ikaddoura.parkAndRide.analyze.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Network;

/**
 * @author Ihab
 *
 */
public class PREventHandler implements LinkEnterEventHandler, ActivityEndEventHandler, PersonDepartureEventHandler, PersonStuckEventHandler {
	private Network network;
	private Map<Id, Integer> linkId2cars = new HashMap<Id, Integer>();
	private Map<Id, Integer> linkId2prActs = new HashMap<Id, Integer>();
	private Map<Id, List<Double>> linkId2prEndTimes = new HashMap<Id, List<Double>>();
	
	private int ptLegs;
	private int carLegs;
	private int stuckEvents;
	
	public PREventHandler(Network network) {
		this.network = network;
	}

	@Override
	public void reset(int iteration) {
		this.linkId2cars.clear();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (event.getPersonId().toString().contains("person")){
			Id linkId = event.getLinkId();
			if (this.linkId2cars.get(linkId) == null){
				this.linkId2cars.put(linkId, 1);
			} else {
				int increasedCarNumber = this.linkId2cars.get(linkId) + 1;
				this.linkId2cars.put(linkId, increasedCarNumber);
			}
		} else {
			// no car!
		}
	}
	
	@Override
	public void handleEvent(ActivityEndEvent event) {
		Id linkId = event.getLinkId();
		if (event.getActType().toString().equals("parkAndRide")){
			
			if (this.linkId2prActs.get(linkId) == null){
				this.linkId2prActs.put(linkId, 1);
			} else {
				int increasedPrActs = this.linkId2prActs.get(linkId) + 1;
				this.linkId2prActs.put(linkId, increasedPrActs);
			}
			
			if (this.linkId2prEndTimes.get(linkId) == null){
				List<Double> prEndTimes = new ArrayList<Double>();
				prEndTimes.add(event.getTime());
				this.linkId2prEndTimes.put(linkId, prEndTimes);
			} else {
				List<Double> prEndTimes = this.linkId2prEndTimes.get(linkId);
				prEndTimes.add(event.getTime());
				this.linkId2prEndTimes.put(linkId, prEndTimes);
			}
			
		}
	}
	
	public Map<Id, Integer> getLinkId2cars() {
		return linkId2cars;
	}
	
	public Map<Id, Integer> getLinkId2prActs() {
		return linkId2prActs;
	}
	
	public int getPtLegs() {
		return ptLegs;
	}

	public int getCarLegs() {
		return carLegs;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if(event.getPersonId().toString().contains("person")){
			if (event.getLegMode().toString().equals(TransportMode.pt)){
				this.ptLegs++;
			}
			if (event.getLegMode().toString().equals(TransportMode.car)){
				this.carLegs++;
			}
		}
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		stuckEvents++;
	}
	
	public int getStuckEvents() {
		return stuckEvents;
	}

	public Map<Id, List<Double>> getLinkId2prEndTimes() {
		return linkId2prEndTimes;
	}
	
	
}
