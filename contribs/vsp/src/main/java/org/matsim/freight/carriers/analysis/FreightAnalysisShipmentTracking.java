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

package org.matsim.freight.carriers.analysis;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierShipment;
import org.matsim.freight.carriers.events.CarrierShipmentDeliveryEndEvent;
import org.matsim.freight.carriers.events.CarrierShipmentPickupEndEvent;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

/**
 *  @deprecated We have new event types now, allowing us to use a more straight forward analysis without guessing.
 *  I will let this here for some time so we can have a look, what else should be moved over, but in the end, We will remove this here.
 *  (kmt apr'23)
 *
 * @author Jakob Harnisch (MATSim advanced class 2020/21)
 */

@Deprecated(since = "apr23", forRemoval = true)
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
					if(shipment.shipment.getDeliveryStartingTimeWindow().getStart() <= activityStartEvent.getTime()) {
                        if (activityStartEvent.getTime()<= shipment.shipment.getDeliveryStartingTimeWindow().getEnd()) {
                            if (shipment.possibleDrivers.contains(activityStartEvent.getPersonId().toString())) {
                                shipment.driverIdGuess = activityStartEvent.getPersonId();
                                shipment.deliveryTimeGuess=activityStartEvent.getTime();
                            }
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
					if(shipmentTracker.shipment.getPickupStartingTimeWindow().getStart() <= activityStartEvent.getTime()) {
						if (activityStartEvent.getTime()<= shipmentTracker.shipment.getPickupStartingTimeWindow().getEnd()) {
							shipmentTracker.possibleDrivers.add(activityStartEvent.getPersonId().toString());
						}
					}
				}
			}
		}
	}
// untested LSP Event handling for precise Shipment Tracking
	public void trackPickedUpEvent(CarrierShipmentPickupEndEvent event) {
		if (shipments.containsKey(event.getShipmentId())) {
			shipments.get(event.getShipmentId()).pickUpTime = event.getTime();
			//FixMe: Driver is no longer part of the events... kmt jul22
//			shipments.get(shipment.getId()).driverId = event.getDriverId();
		}
	}


	public void trackDeliveryEvent(CarrierShipmentDeliveryEndEvent event) {
		if (shipments.containsKey(event.getShipmentId())){
			ShipmentTracker shipmentTracker = shipments.get(event.getShipmentId());
			shipmentTracker.deliveryTime=event.getTime();
			shipmentTracker.deliveryDuration +=  (event.getTime() - shipmentTracker.pickUpTime);
		}
	}
}

/**
 *  @deprecated We have new event types now, allowing us to use a more straight forward analysis without guessing.
 *  I will let this here for some time so we can have a look, what else should be moved over, but in the end, We will remove this here.
 *  (kmt apr'23)
 */

@Deprecated(since = "apr23", forRemoval = true)
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
		this.from = shipment.getPickupLinkId();
		this.to=shipment.getDeliveryLinkId();
		this.shipment=shipment;
	}

//	public ShipmentTracker(Id<Carrier> carrierId, CarrierShipment shipment) {
//		this(shipment);
//		this.carrierId = carrierId;
//	}
}
