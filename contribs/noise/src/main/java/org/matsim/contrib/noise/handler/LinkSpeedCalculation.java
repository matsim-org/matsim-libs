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

/**
 * 
 */
package org.matsim.contrib.noise.handler;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.contrib.noise.data.NoiseLink;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

/**
 * @author ikaddoura
 *
 */
public class LinkSpeedCalculation implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonArrivalEventHandler{

	@Inject
	private NoiseContext noiseContext;
	
	private Map<Id<Vehicle>, Double> vehicleId2enterTime = new HashMap<>();

	@Override
	public void reset(int iteration) {
		this.vehicleId2enterTime.clear();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		if (this.vehicleId2enterTime.get(event.getVehicleId()) != null) {
			double traveltime = event.getTime() - this.vehicleId2enterTime.get(event.getVehicleId());
			
			boolean isHGV = false;
			for (String hgvPrefix : this.noiseContext.getNoiseParams().getHgvIdPrefixesArray()) {
				if (event.getVehicleId().toString().startsWith(hgvPrefix)) {
					isHGV = true;
					break;
				}
			}
			
			NoiseLink noiseLink = this.noiseContext.getNoiseLinks().get(event.getLinkId());
			if (isHGV || this.noiseContext.getBusVehicleIDs().contains(event.getVehicleId())) {
				// HGV or Bus
				if (noiseLink != null) {
					double travelTimeSum = noiseLink.getTravelTimeHGV_sec() + traveltime;
					noiseLink.setTravelTimeHGV_Sec(travelTimeSum);
					int hgvAgents = noiseLink.getHgvAgentsLeaving() + 1;
					noiseLink.setHgvAgentsLeaving(hgvAgents);
					
				} else {
					noiseLink = new NoiseLink(event.getLinkId());
					noiseLink.setTravelTimeHGV_Sec(traveltime);
					noiseLink.setHgvAgentsLeaving(1);
					this.noiseContext.getNoiseLinks().put(event.getLinkId(), noiseLink);
				}
							
			} else {
				// Car
				if (noiseLink != null) {
					double travelTimeSum = noiseLink.getTravelTimeCar_sec() + traveltime;
					noiseLink.setTravelTimeCar_Sec(travelTimeSum);
					int carAgents = noiseLink.getCarAgentsLeaving() + 1;
					noiseLink.setCarAgentsLeaving(carAgents);
					
				} else {
					noiseLink = new NoiseLink(event.getLinkId());
					noiseLink.setTravelTimeCar_Sec(traveltime);
					noiseLink.setCarAgentsLeaving(1);
					this.noiseContext.getNoiseLinks().put(event.getLinkId(), noiseLink);
				}
					
			}
			
		} else {
			// the person has just departed, don't count this vehicle
		}
	}
	
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		// the person has arrived and is no longer traveling
		this.vehicleId2enterTime.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		// the person has entered the link, store the time
		this.vehicleId2enterTime.put(event.getVehicleId(), event.getTime());
	}

	public void setNoiseContext(NoiseContext noiseContext) {
		this.noiseContext = noiseContext;
	}

}
