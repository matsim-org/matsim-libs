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
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.vehicles.Vehicle;

import java.util.Map;

import static org.matsim.contrib.freight.events.FreightEventAttributes.*;

/**
 * This informs the world that a shipment has been picked up.
 * 
 * @author sschroeder, kturner
 */
public class ShipmentPickedUpEvent extends AbstractFreightEvent {

	public static final String EVENT_TYPE = "Freight shipment picked up";

	private final Id<CarrierShipment> shipmentId;
	private final double pickupDuration;
	private final int capacityDemand;

	
	public ShipmentPickedUpEvent(double time, Id<Carrier> carrierId, CarrierShipment shipment, Id<Vehicle> vehicleId) {
		super(time, carrierId, shipment.getFrom(), vehicleId);
		this.shipmentId = shipment.getId();
		this.pickupDuration = shipment.getPickupServiceTime();
		this.capacityDemand = shipment.getSize();
	}


	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public Id<CarrierShipment> getShipmentId() {
		return shipmentId;
	}


	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_SHIPMENT_ID, this.shipmentId.toString());
		attr.put(ATTRIBUTE_PICKUP_DURATION, String.valueOf(this.pickupDuration));
		attr.put(ATTRIBUTE_CAPACITYDEMAND, String.valueOf(capacityDemand));
		return attr;
	}
}
