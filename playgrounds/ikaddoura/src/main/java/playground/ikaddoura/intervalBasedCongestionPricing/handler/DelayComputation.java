/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.intervalBasedCongestionPricing.handler;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.vehicles.Vehicle;

import playground.ikaddoura.intervalBasedCongestionPricing.data.CongestionInfo;
import playground.ikaddoura.intervalBasedCongestionPricing.data.CongestionLinkInfo;

/**
 * 
 * Provides the number of agents per link, the agents' total travel time and the final agent's travel time.
 * 
 * @author ikaddoura
 */

public class DelayComputation implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonArrivalEventHandler {

	private double totalDelayPerDay_sec = 0.;
	private double totalTravelTimePerDay_sec = 0.;

	private final CongestionInfo congestionInfo;
	private Map<Id<Vehicle>, Double> vehicleId2enterTime = new HashMap<>();

	public DelayComputation(CongestionInfo congestionInfo) {
		this.congestionInfo = congestionInfo;
	}

	@Override
	public void reset(int iteration) {
		this.totalDelayPerDay_sec = 0.;
		this.totalTravelTimePerDay_sec = 0.;
		
		this.vehicleId2enterTime.clear();
		this.congestionInfo.getCongestionLinkInfos().clear();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		this.vehicleId2enterTime.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		if (event.getTime() > this.congestionInfo.getCurrentTimeBinEndTime() || event.getTime() < this.congestionInfo.getCurrentTimeBinEndTime() - this.congestionInfo.getTIME_BIN_SIZE()) throw new RuntimeException("The event time ist not within the current time interval. Aborting...");
			
		// create congestion link info
		if (!this.congestionInfo.getCongestionLinkInfos().containsKey(event.getLinkId())) {
			congestionInfo.getCongestionLinkInfos().put(event.getLinkId(), new CongestionLinkInfo(event.getLinkId()));
		}
		
		if (this.vehicleId2enterTime.containsKey(event.getVehicleId())) {
			
			// remember the agents leaving the link
			this.congestionInfo.getCongestionLinkInfos().get(event.getLinkId()).getLeavingVehicles().add(event.getVehicleId());
			
			// compute the travel time
			double traveltimeThisAgent = event.getTime() - this.vehicleId2enterTime.get(event.getVehicleId());
			double travelTimeSum = this.congestionInfo.getCongestionLinkInfos().get(event.getLinkId()).getTravelTimeSum_sec() + traveltimeThisAgent;
			this.congestionInfo.getCongestionLinkInfos().get(event.getLinkId()).setTravelTimeSum(travelTimeSum);
			this.congestionInfo.getCongestionLinkInfos().get(event.getLinkId()).setTravelTimeLastLeavingAgent(traveltimeThisAgent);
			if (traveltimeThisAgent > this.congestionInfo.getCongestionLinkInfos().get(event.getLinkId()).getTravelTimeMaximum()) {
				this.congestionInfo.getCongestionLinkInfos().get(event.getLinkId()).setTravelTimeMaximum(traveltimeThisAgent);
			}
			
			double freespeedTravelTime = Math.round(congestionInfo.getScenario().getNetwork().getLinks().get(event.getLinkId()).getLength() / congestionInfo.getScenario().getNetwork().getLinks().get(event.getLinkId()).getFreespeed());
			this.totalDelayPerDay_sec = this.totalDelayPerDay_sec + (traveltimeThisAgent - freespeedTravelTime);
			this.totalTravelTimePerDay_sec = this.totalTravelTimePerDay_sec + traveltimeThisAgent;
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		// remember the enter time
		this.vehicleId2enterTime.put(event.getVehicleId(), event.getTime());
	}

	public double getTotalDelay() {
		return totalDelayPerDay_sec;
	}

	public double getTotalTravelTime() {
		return totalTravelTimePerDay_sec;
	}

}

