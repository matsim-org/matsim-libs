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
import org.matsim.freight.carriers.CarrierService;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.carriers.events.CarrierServiceEndEvent;
import org.matsim.freight.carriers.events.CarrierServiceStartEvent;
import org.matsim.freight.carriers.events.eventhandler.CarrierServiceEndEventHandler;
import org.matsim.freight.carriers.events.eventhandler.CarrierServiceStartEventHandler;
import org.matsim.vehicles.Vehicles;

 class MyServiceTrackerEventHandler implements ActivityStartEventHandler, CarrierServiceStartEventHandler, CarrierServiceEndEventHandler {
    private final Vehicles vehicles;
    private final Network network;
    private final Carriers carriers;

    private FreightAnalysisServiceTracking serviceTracking = new FreightAnalysisServiceTracking();

    MyServiceTrackerEventHandler(Vehicles vehicles, Network network, Carriers carriers) {
        this.network = network;
        this.carriers = carriers;
        this.vehicles = vehicles;
        this.init();
    }

    private void init() {
        for (Carrier carrier : carriers.getCarriers().values()) {

            for (CarrierService service : carrier.getServices().values()) {
                serviceTracking.addTracker(service, carrier.getId());
            }
        }
        serviceTracking.estimateArrivalTimes(carriers);
    }

    @Override
    public void handleEvent(CarrierServiceEndEvent event) {
        serviceTracking.handleEndEvent(event);
    }

    @Override
    public void handleEvent(CarrierServiceStartEvent event) {
        serviceTracking.handleStartEvent(event);
    }

    @Override
    public void handleEvent(ActivityStartEvent activityStartEvent) {
        if (activityStartEvent.getActType().equals("service")) {
            serviceTracking.trackServiceActivityStart(activityStartEvent);
        }
    }

    public FreightAnalysisServiceTracking getServiceTracking() {
        return serviceTracking;
    }
}
