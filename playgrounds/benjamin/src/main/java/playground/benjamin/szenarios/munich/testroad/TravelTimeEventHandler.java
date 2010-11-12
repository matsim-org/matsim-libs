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
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;

/**
 * @author benjamin
 *
 */
public class TravelTimeEventHandler implements ActivityEndEventHandler, LinkLeaveEventHandler {
	
	private SortedMap<Double, Double> activityEndTimes2travelTimesPerIteration = new TreeMap<Double, Double>();
	
	private double activityEndTime;
	private double leaveTime;
	private Id linkId;
	private Id testVehicleActivityLinkId;

	public TravelTimeEventHandler(SortedMap<Double, Double> activityEndTimes2travelTimesPerIteration, Id linkId, Id testVehicleActivityLinkId) {
		this.activityEndTimes2travelTimesPerIteration = activityEndTimes2travelTimesPerIteration;
		this.linkId = linkId;
		this.testVehicleActivityLinkId = testVehicleActivityLinkId;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		String id = event.getPersonId().toString();
		if(id.contains("testVehicle")){
			if(event.getLinkId().equals(this.testVehicleActivityLinkId)){
				activityEndTime = event.getTime();
			}
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		String id = event.getPersonId().toString();
		if(id.contains("testVehicle")){
			if(event.getLinkId().equals(this.linkId)){
				leaveTime = event.getTime();
				
				this.activityEndTimes2travelTimesPerIteration.put(activityEndTime, leaveTime - activityEndTime);
			}
		}
		
	}

}
