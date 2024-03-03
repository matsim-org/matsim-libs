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

import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierShipment;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.carriers.events.CarrierShipmentDeliveryEndEvent;
import org.matsim.freight.carriers.events.CarrierShipmentPickupEndEvent;
import org.matsim.freight.carriers.events.eventhandler.CarrierShipmentDeliveryEndEventHandler;
import org.matsim.freight.carriers.events.eventhandler.CarrierShipmentPickupEndEventHandler;
import org.matsim.vehicles.Vehicles;

public class MyShipmentTrackerEventHandler implements ActivityStartEventHandler, CarrierShipmentPickupEndEventHandler, CarrierShipmentDeliveryEndEventHandler {

    private final Vehicles vehicles;
    private final Network network;
    private final Carriers carriers;

    private FreightAnalysisShipmentTracking shipmentTracking = new FreightAnalysisShipmentTracking();

    MyShipmentTrackerEventHandler(Vehicles vehicles, Network network, Carriers carriers) {
        this.network = network;
        this.carriers = carriers;
        this.vehicles = vehicles;
        this.init();
    }

    private void init(){
        for (Carrier carrier : carriers.getCarriers().values()) {
            // for all shipments and services of the carriers, tracking is started here.
            for (CarrierShipment shipment : carrier.getShipments().values()) {
                shipmentTracking.addTracker(shipment);
            }
        }
    }

    @Override
    public void handleEvent(CarrierShipmentDeliveryEndEvent event) {
        shipmentTracking.trackDeliveryEvent(event);
    }

    @Override
    public void handleEvent(CarrierShipmentPickupEndEvent event) {
        shipmentTracking.trackPickedUpEvent(event);
    }

    @Override
    public void reset(int iteration) {
        CarrierShipmentPickupEndEventHandler.super.reset(iteration);
    }

    @Override
    public void handleEvent(ActivityStartEvent activityStartEvent) {

        if (activityStartEvent.getActType().equals("delivery")) {
            shipmentTracking.trackDeliveryActivity(activityStartEvent);
        }

        if (activityStartEvent.getActType().equals("pickup")){
            shipmentTracking.trackPickupActivity(activityStartEvent);
        }

    }

    public FreightAnalysisShipmentTracking getShipmentTracker(){
        return shipmentTracking;
    }
}
