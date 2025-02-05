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

import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierService;
import org.matsim.vehicles.Vehicle;

/**
 * An event, that informs that a Freight {@link CarrierService} activity has started.
 *
 * @author Tilman Matteis  - creating it for the use in Logistics / LogisticServiceProviders (LSP)s
 * @author Kai Martins-Turner (kturner) - integrating and adapting it into/for the MATSim freight contrib
 */
public final class CarrierServiceStartEvent extends AbstractCarrierEvent {

	public static final String EVENT_TYPE = "Freight service starts";

	private final Id<CarrierService> serviceId;
	private final double serviceDuration;
	private final int capacityDemand;

	public CarrierServiceStartEvent(double time, Id<Carrier> carrierId, CarrierService service, Id<Vehicle> vehicleId) {
		super(time, carrierId, service.getServiceLinkId(), vehicleId);
		this.serviceId = service.getId();
		this.serviceDuration = service.getServiceDuration();
        this.capacityDemand = service.getCapacityDemand();
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public Id<CarrierService> getServiceId() {
		return serviceId;
	}


	public double getServiceDuration() {
		return serviceDuration;
	}

	public int getCapacityDemand() {
		return this.capacityDemand;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(CarrierEventAttributes.ATTRIBUTE_SERVICE_ID, serviceId.toString());
		attr.put(CarrierEventAttributes.ATTRIBUTE_SERVICE_DURATION, String.valueOf(serviceDuration));
		attr.put(CarrierEventAttributes.ATTRIBUTE_CAPACITYDEMAND, String.valueOf(capacityDemand));
		return attr;
	}

	public static CarrierServiceStartEvent convert(GenericEvent event) {
		Map<String, String> attributes = event.getAttributes();
		double time = Double.parseDouble(attributes.get(ATTRIBUTE_TIME));
		Id<Carrier> carrierId = Id.create(attributes.get(ATTRIBUTE_CARRIER_ID), Carrier.class);
		Id<CarrierService> carrierServiceId = Id.create(attributes.get(CarrierEventAttributes.ATTRIBUTE_SERVICE_ID), CarrierService.class);
		Id<Link> locationLinkId = Id.createLinkId(attributes.get(ATTRIBUTE_LINK));
		CarrierService service = CarrierService.Builder.newInstance(carrierServiceId, locationLinkId)
				.setServiceDuration(Double.parseDouble(attributes.get(CarrierEventAttributes.ATTRIBUTE_SERVICE_DURATION)))
				.setCapacityDemand(Integer.parseInt(attributes.get(CarrierEventAttributes.ATTRIBUTE_CAPACITYDEMAND)))
				.build();
		Id<Vehicle> vehicleId = Id.create(attributes.get(ATTRIBUTE_VEHICLE), Vehicle.class);
		return new CarrierServiceStartEvent(time, carrierId, service, vehicleId);
	}
}
