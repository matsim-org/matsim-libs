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
package org.matsim.contrib.noise;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.contrib.noise.NoiseContext;
import org.matsim.contrib.noise.NoiseLink;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;
import org.matsim.vehicles.VehicleUtils;

/**
 * @author ikaddoura
 *
 */
final class LinkSpeedCalculation implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonArrivalEventHandler {

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
            NoiseLink noiseLink = this.noiseContext.getNoiseLinks().get(event.getLinkId());

            double travelTime = event.getTime() - this.vehicleId2enterTime.get(event.getVehicleId());

            final NoiseConfigGroup noiseParams = this.noiseContext.getNoiseParams();

			final Id<NoiseVehicleType> id;
			switch (noiseParams.getNoiseComputationMethod()) {
				case RLS90:
					boolean isHGV = false;
					for (String hgvPrefix : noiseParams.getHgvIdPrefixesArray()) {
						if (event.getVehicleId().toString().startsWith(hgvPrefix)) {
							isHGV = true;
							break;
						}
					}

                    if (isHGV || this.noiseContext.getBusVehicleIDs().contains(event.getVehicleId())) {
                        // HGV or Bus
                        id = RLS90VehicleType.hgv.getId();
                    } else {
                        id = RLS90VehicleType.car.getId();
                    }
                    break;
                case RLS19:
					Vehicle vehicle = VehicleUtils.findVehicle(event.getVehicleId(), noiseContext.getScenario());
					String typeString = (String) vehicle.getType().getAttributes().getAttribute("RLS19Type");
					id = RLS19VehicleType.valueOf(typeString).getId();
					break;
                default:
                    throw new IllegalStateException("Unexpected value: " + noiseParams.getNoiseComputationMethod());
            }

			if (noiseLink != null) {
				double travelTimeSum = noiseLink.getTravelTime_sec(id) + travelTime;
				noiseLink.setTravelTime(id, travelTimeSum);
				int agents = noiseLink.getAgentsLeaving(id) + 1;
				noiseLink.setAgentsLeaving(id, agents);

			} else {
				noiseLink = new NoiseLink(event.getLinkId());
				noiseLink.setTravelTime(id, travelTime);
				noiseLink.setAgentsLeaving(id, 1);
				this.noiseContext.getNoiseLinks().put(event.getLinkId(), noiseLink);
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
