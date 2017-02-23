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

package playground.ikaddoura.decongestion.handler;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

/**
 * 
 * Computes the total delay and travel time.
 * 
 * WARNING: Link-based analysis. Ignores the travel time and delay on the first link of each trip, i.e. the link on which the trip has started.
 * 
 * @author ikaddoura
 */

public class DelayAnalysis implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonArrivalEventHandler {
	private static final Logger log = Logger.getLogger(DelayAnalysis.class);

	private Map<Id<Vehicle>, Double> vehicleId2enterTime = new HashMap<>();
	
	@Inject
	private Scenario scenario;
	
	// some aggregated numbers for analysis purposes
	private double totalDelayPerDay_sec = 0.;
	private double totalTravelTimePerDay_sec = 0.;
	private int warnCnt = 0;

	public void setScenario(Scenario scenario) {
		this.scenario = scenario;
	}

	@Override
	public void reset(int iteration) {
		this.totalDelayPerDay_sec = 0.;
		this.totalTravelTimePerDay_sec = 0.;
		this.vehicleId2enterTime.clear();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		if (this.vehicleId2enterTime.get(event.getVehicleId()) != null) {
			
			// compute the travel time
			double traveltimeThisAgent = event.getTime() - this.vehicleId2enterTime.get(event.getVehicleId());			
			double freespeedTravelTime = 1 + Math.ceil(this.scenario.getNetwork().getLinks().get(event.getLinkId()).getLength() / this.scenario.getNetwork().getLinks().get(event.getLinkId()).getFreespeed());
			double delayThisAgent = traveltimeThisAgent - freespeedTravelTime;		
			
			if (delayThisAgent < -1.)  { 
				throw new RuntimeException("The delay is negative. Aborting..." + delayThisAgent);
			
			} else if (delayThisAgent < 0.) {
				if (warnCnt  == 0) {
					log.warn("Delay is " + delayThisAgent + ". A delay of 1 sec may result from rounding errors. Therefore a delay of 1 sec is ignored and set to zero.");
					warnCnt ++;
				}
				delayThisAgent = 0.;
				
			} else if (delayThisAgent > 0.) {			
				this.totalDelayPerDay_sec = this.totalDelayPerDay_sec + delayThisAgent;
			}
						
			this.totalTravelTimePerDay_sec = this.totalTravelTimePerDay_sec + traveltimeThisAgent;
		}
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		this.vehicleId2enterTime.put(event.getVehicleId(), event.getTime());
	}
	
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		this.vehicleId2enterTime.remove(event.getPersonId());
	}

	public double getTotalDelay() {
		return totalDelayPerDay_sec;
	}

	public double getTotalTravelTime() {
		return totalTravelTimePerDay_sec;
	}

}

