/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.szenarios.munich.testroad;

import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;

/**
 * @author benjamin
 *
 */
public class TravelTimeEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

	private SortedMap<Id, Double> personId2travelTimesPerIteration = new TreeMap<Id, Double>();
	private SortedMap<Id, Double> personId2enterTimesPerIteration = new TreeMap<Id, Double>();

	private Id enterLinkId;
	private Id leaveLinkId;

	public TravelTimeEventHandler(SortedMap<Id, Double> personId2travelTimesPerIteration, SortedMap<Id, Double> personId2enterTimesPerIteration, Id enterLinkId, Id leaveLinkId) {
		this.personId2travelTimesPerIteration = personId2travelTimesPerIteration;
		this.personId2enterTimesPerIteration = personId2enterTimesPerIteration;
		this.enterLinkId = enterLinkId;
		this.leaveLinkId = leaveLinkId;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(event.getLinkId().equals(this.enterLinkId)){
			String id = event.getPersonId().toString();
			if(id.contains("testVehicle")){
				Id personId = event.getPersonId();
				Double enterTime = event.getTime();

				this.personId2enterTimesPerIteration.put(personId, enterTime);
			}
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if(event.getLinkId().equals(this.leaveLinkId)){
			String id = event.getPersonId().toString();
			if(id.contains("testVehicle")){
				Id personId = event.getPersonId();
				Double leaveTime = event.getTime();
				Double enterTime = this.personId2enterTimesPerIteration.get(personId);

				this.personId2travelTimesPerIteration.put(personId, leaveTime - enterTime);
			}
		}
	}
}