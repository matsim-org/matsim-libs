/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.freight.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierShipment;

/**
 * This informs the world that a shipment has been picked up.
 * 
 * @author sschroeder
 *
 */
public class ShipmentPickedUpEvent extends Event {

	private final CarrierShipment shipment;

	
	public ShipmentPickedUpEvent(Id<Carrier> carrierId, CarrierShipment shipment, double time) {
		super(time);
		this.shipment = shipment;
	}


	public CarrierShipment getShipment() {
		return shipment;
	}

	@Override
	public String getEventType() {
		// TODO Auto-generated method stub
		return null;
	}

}
