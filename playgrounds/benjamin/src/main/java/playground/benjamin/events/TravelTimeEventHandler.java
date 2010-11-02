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
package playground.benjamin.events;

import java.util.SortedMap;
import java.util.TreeMap;

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
	
	private SortedMap<Double, Double> departureTimes2travelTimes = new TreeMap<Double, Double>();
	
	private double enterTime;
	private double leaveTime;

	public TravelTimeEventHandler(SortedMap<Double, Double> departureTimes2travelTimes) {
		this.departureTimes2travelTimes = departureTimes2travelTimes;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		String id = event.getPersonId().toString();
		if(id.contains("testVehicle")){
			if(event.getLinkId().equals(new IdImpl("592536888"))){
				enterTime = event.getTime();
			}
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		String id = event.getPersonId().toString();
		if(id.contains("testVehicle")){
			if(event.getLinkId().equals(new IdImpl("590000822"))){
				leaveTime = event.getTime();
				
				this.departureTimes2travelTimes.put(enterTime, leaveTime - enterTime);
			}
		}
		
	}

}
