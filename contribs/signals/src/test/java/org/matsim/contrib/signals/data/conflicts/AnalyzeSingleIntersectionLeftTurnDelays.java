/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package org.matsim.contrib.signals.data.conflicts;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.LaneEnterEvent;
import org.matsim.core.api.experimental.events.LaneLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LaneEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LaneLeaveEventHandler;
import org.matsim.lanes.Lane;
import org.matsim.lanes.Lanes;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

/**
 * @author tthunig
 */
class AnalyzeSingleIntersectionLeftTurnDelays implements LaneEnterEventHandler, LaneLeaveEventHandler, PersonStuckEventHandler {

	@Inject
	private Network network;
	@Inject
	private Lanes lanes;
	
	private Map<Id<Vehicle>, Double> enterTimePerVehicle = new HashMap<>();
	private double leftTurnDelay;
	private int stuckCount;
	
	@Override
	public void reset(int iteration) {
		stuckCount = 0;
		leftTurnDelay = 0.0;
		enterTimePerVehicle.clear();
	}
	
	@Override
	public void handleEvent(LaneLeaveEvent event) {
		if (event.getLaneId().equals(Id.create("2_3.l", Lane.class)) || 
				event.getLaneId().equals(Id.create("4_3.l", Lane.class))) {
			double enterTime = enterTimePerVehicle.remove(event.getVehicleId());
			Lane lane = lanes.getLanesToLinkAssignments().get(event.getLinkId()).getLanes().get(event.getLaneId());
			Link link = network.getLinks().get(event.getLinkId());
			double freespeedTT = lane.getStartsAtMeterFromLinkEnd()/link.getFreespeed();
			// this is the earliest time where matsim sets the agent to the next link
			double matsimFreespeedTT = Math.floor(freespeedTT + 1);
			leftTurnDelay += event.getTime() - enterTime - matsimFreespeedTT;
		}
	}

	/**
	 * This method gives you the total delay of left turning vehicles at the single intersection scenario. 
	 * It includes all delay, also the time that vehicles have to wait in front of a red light.
	 * note: delay of stucked vehicles is not considered. To check for plausibility, you can call getStuckCount(). 
	 */
	public double getLeftTurnDelay() {
		return leftTurnDelay;
	}
	
	public int getStuckCount() {
		return stuckCount;
	}

	@Override
	public void handleEvent(LaneEnterEvent event) {
		if (event.getLaneId().equals(Id.create("2_3.l", Lane.class)) || 
				event.getLaneId().equals(Id.create("4_3.l", Lane.class))) {
			enterTimePerVehicle.put(event.getVehicleId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		stuckCount++;
	}
	

}
