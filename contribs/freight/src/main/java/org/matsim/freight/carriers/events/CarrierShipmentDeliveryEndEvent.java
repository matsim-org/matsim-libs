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

package org.matsim.freight.carriers.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierShipment;
import org.matsim.vehicles.Vehicle;

import java.util.Map;

/**
 * An event, that informs that a Freight {@link CarrierShipment} delivery-activity has ended.
 *
 * @author sschroeder
 * @author kturner
 *
 */
public class CarrierShipmentDeliveryEndEvent extends AbstractCarrierEvent {

	public static final String EVENT_TYPE = "Freight shipment delivered ends";

	private final Id<CarrierShipment> shipmentId;
	private final double deliveryDuration;
	private final int capacityDemand;
	public CarrierShipmentDeliveryEndEvent(double time, Id<Carrier> carrierId, CarrierShipment shipment, Id<Vehicle> vehicleId) {
		super(time, carrierId, shipment.getTo(), vehicleId);
		this.shipmentId = shipment.getId();
		this.deliveryDuration = shipment.getDeliveryServiceTime();
		this.capacityDemand = shipment.getSize();
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public Id<CarrierShipment> getShipmentId() {
		return shipmentId;
	}

	public double getDeliveryDuration() {
		return deliveryDuration;
	}

	public int getCapacityDemand() {
		return capacityDemand;
	}

	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(CarrierEventAttributes.ATTRIBUTE_SHIPMENT_ID, this.shipmentId.toString());
		attr.put(CarrierEventAttributes.ATTRIBUTE_DROPOFF_DURATION, String.valueOf(this.deliveryDuration));
		attr.put(CarrierEventAttributes.ATTRIBUTE_CAPACITYDEMAND, String.valueOf(capacityDemand));
		return attr;
	}

	public static CarrierShipmentDeliveryEndEvent convert(GenericEvent event) {
		var attributes = event.getAttributes();
		double time = Double.parseDouble(attributes.get(ATTRIBUTE_TIME));
		Id<Carrier> carrierId = Id.create(attributes.get(ATTRIBUTE_CARRIER_ID), Carrier.class);
		Id<CarrierShipment> shipmentId = Id.create(attributes.get(CarrierEventAttributes.ATTRIBUTE_SHIPMENT_ID), CarrierShipment.class);
		Id<Link> shipmentTo = Id.createLinkId(attributes.get(ATTRIBUTE_LINK));
		int size = Integer.parseInt(attributes.get(CarrierEventAttributes.ATTRIBUTE_CAPACITYDEMAND));
		CarrierShipment shipment = CarrierShipment.Builder.newInstance(shipmentId, null, shipmentTo, size)
				.setDeliveryServiceTime(Double.parseDouble(attributes.get(CarrierEventAttributes.ATTRIBUTE_DROPOFF_DURATION)))
				.build();
		Id<Vehicle> vehicleId = Id.createVehicleId(attributes.get(ATTRIBUTE_VEHICLE));
		return new CarrierShipmentDeliveryEndEvent(time, carrierId, shipment, vehicleId);
	}

}
