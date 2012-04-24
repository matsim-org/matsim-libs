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
package playground.ikaddoura.parkAndRide.analyze;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;

/**
 * @author Ihab
 *
 */
public class PREventHandler implements LinkEnterEventHandler, ActivityEndEventHandler  {
	private Network network;
	private Map<Id, Integer> linkId2cars = new HashMap<Id, Integer>();
	private Map<Id, Integer> linkId2prActs = new HashMap<Id, Integer>();

	
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
			System.out.println(event.toString());
			if (this.linkId2prActs.get(linkId) == null){
				this.linkId2prActs.put(linkId, 1);
			} else {
				int increasedPrActs = this.linkId2prActs.get(linkId) + 1;
				this.linkId2prActs.put(linkId, increasedPrActs);
			}
		}
	}
	
	public Map<Id, Integer> getLinkId2cars() {
		return linkId2cars;
	}
	
	public Map<Id, Integer> getLinkId2prActs() {
		return linkId2prActs;
	}
	
}
