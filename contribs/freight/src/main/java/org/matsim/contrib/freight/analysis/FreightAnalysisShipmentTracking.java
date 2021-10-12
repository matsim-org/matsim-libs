/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C) 2021 by the members listed in the COPYING,        *
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

package org.matsim.contrib.freight.analysis;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.events.ShipmentDeliveredEvent;
import org.matsim.contrib.freight.events.ShipmentPickedUpEvent;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

/**
 * @author Jakob Harnisch (MATSim advanced class 2020/21)
 */

class FreightAnalysisShipmentTracking {

	private final LinkedHashMap<Id<CarrierShipment>, ShipmentTracker> shipments = new LinkedHashMap<>();

    public void addTracker(CarrierShipment shipment){
        shipments.put(shipment.getId(),new ShipmentTracker(shipment) );
    }
	public LinkedHashMap<Id<CarrierShipment>, ShipmentTracker> getShipments() {
		return shipments;
	}

	// tracking Shipments based on Guesses the same way as Services are tracked, and also with the same issues.
	public void trackDeliveryActivity(ActivityStartEvent activityStartEvent) {
    	for (ShipmentTracker shipment: shipments.values()){
    		if (shipment.to==activityStartEvent.getLinkId() ){
				if(shipment.driverId == null){
					if(shipment.shipment.getDeliveryTimeWindow().getStart()<=activityStartEvent.getTime() && activityStartEvent.getTime()<=shipment.shipment.getDeliveryTimeWindow().getEnd()){
						if (shipment.possibleDrivers.contains(activityStartEvent.getPersonId().toString())) {
							shipment.driverIdGuess = activityStartEvent.getPersonId();
							shipment.deliveryTimeGuess=activityStartEvent.getTime();
						}
					}
				} else if (shipment.driverId.toString().equals(activityStartEvent.getPersonId().toString())){
					shipment.deliveryTime=activityStartEvent.getTime();
				}
			}
		}
	}

	// for improving the guess, we track the pickup activities aswell to narrow down the selection of drivers on those that could have picked up the shipment when we later on try to match the delivery activity.
	public void trackPickupActivity(ActivityStartEvent activityStartEvent) {
    	for (ShipmentTracker shipmentTracker: shipments.values()){
    		if (shipmentTracker.from==activityStartEvent.getLinkId()){
    			if (shipmentTracker.driverId==null){
    				if(shipmentTracker.shipment.getPickupTimeWindow().getStart()<=activityStartEvent.getTime() && activityStartEvent.getTime()<=shipmentTracker.shipment.getPickupTimeWindow().getEnd()){
    					shipmentTracker.possibleDrivers.add(activityStartEvent.getPersonId().toString());
					}
				}
			}
		}
	}
// untested LSP Event handling for precise Shipment Tracking
	public void trackPickedUpEvent(ShipmentPickedUpEvent event) {
		if (shipments.containsKey(event.getShipment().getId())) {
			CarrierShipment shipment = event.getShipment();
			shipments.get(shipment.getId()).pickUpTime = event.getTime();
			shipments.get(shipment.getId()).driverId = event.getDriverId();
		}
	}


	public void trackDeliveryEvent(ShipmentDeliveredEvent event) {
		if (shipments.containsKey(event.getShipment().getId())){
			ShipmentTracker shipmentTracker = shipments.get(event.getShipment().getId());
			shipmentTracker.deliveryTime=event.getTime();
			shipmentTracker.deliveryDuration +=  (event.getTime() - shipmentTracker.pickUpTime);
		}
	}
}

class ShipmentTracker {
	public Id<Person> driverIdGuess;
	public double deliveryTimeGuess;
	public LinkedHashSet<String> possibleDrivers = new LinkedHashSet<>();
	Id<Link> from;
	Id<Link> to;
	public Double pickUpTime = 0.;
	public Double deliveryDuration = 0.;
	public Double deliveryTime = 0.;
	public Id<Person> driverId;
	public CarrierShipment shipment;
	public Id<CarrierShipment> id;
	public Id<Carrier> carrierId;

	public ShipmentTracker(CarrierShipment shipment) {
		this.id = shipment.getId();
		this.from = shipment.getFrom();
		this.to=shipment.getTo();
		this.shipment=shipment;
	}

//	public ShipmentTracker(Id<Carrier> carrierId, CarrierShipment shipment) {
//		this(shipment);
//		this.carrierId = carrierId;
//	}
}
