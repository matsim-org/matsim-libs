/* *********************************************************************** *
 /* *********************************************************************** *
 * project: org.matsim.*
 * ColdEmissionHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
 *                                                                         
 * *********************************************************************** */
package org.matsim.contrib.emissions;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.ColdEmissionAnalysisModule.ColdEmissionAnalysisModuleParameter;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;


/**
 * @author benjamin
 */
final class ColdEmissionHandler implements LinkLeaveEventHandler, VehicleLeavesTrafficEventHandler, VehicleEntersTrafficEventHandler {

    private final Logger logger = Logger.getLogger(ColdEmissionHandler.class);

    private final Vehicles vehicles;
    private final Network network;
    private final ColdEmissionAnalysisModule coldEmissionAnalysisModule;

    private int zeroLinkLengthWarnCnt = 0;
    private int nonCarWarn = 0;

    private final Map<Id<Vehicle>, Double> vehicleId2stopEngineTime = new HashMap<>();
    private final Map<Id<Vehicle>, Double> vehicleId2accumulatedDistance = new HashMap<>();
    private final Map<Id<Vehicle>, Double> vehicleId2parkingDuration = new HashMap<>();
    private final Map<Id<Vehicle>, Id<Link>> vehicleId2coldEmissionEventLinkId = new HashMap<>();
    
    public ColdEmissionHandler(
            Vehicles vehicles,
            Network network,
            ColdEmissionAnalysisModuleParameter parameterObject2,
            EventsManager emissionEventsManager, Double emissionEfficiencyFactor) {

        this.vehicles = vehicles;
        this.network = network;
        this.coldEmissionAnalysisModule = new ColdEmissionAnalysisModule(parameterObject2, emissionEventsManager, emissionEfficiencyFactor);
        emissionEventsManager.addHandler(this);

    }

    @Override
    public void reset(int iteration) {
        vehicleId2stopEngineTime.clear();
        vehicleId2accumulatedDistance.clear();
        vehicleId2parkingDuration.clear();
        vehicleId2coldEmissionEventLinkId.clear();
        coldEmissionAnalysisModule.reset();
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
		Id<Vehicle> vehicleId = event.getVehicleId();
        Id<Link> linkId = event.getLinkId();
        Link link = this.network.getLinks().get(linkId);
        double linkLength = link.getLength();

        if (linkLength == 0.) {
            if (zeroLinkLengthWarnCnt == 0 ){
                logger.warn("Length of the link "+ linkId + " is zero. No emissions will be estimated for this link. Make sure, this is intentional.");
                logger.warn(Gbl.ONLYONCE);
                zeroLinkLengthWarnCnt++;
            }
            return;
        }

        Double previousDistance = this.vehicleId2accumulatedDistance.get(vehicleId);
        if (previousDistance != null) {
            double distance = previousDistance + linkLength;
            double parkingDuration = this.vehicleId2parkingDuration.get(vehicleId);
            Id<Link> coldEmissionEventLinkId = this.vehicleId2coldEmissionEventLinkId.get(vehicleId);

            Vehicle vehicle = vehicles.getVehicles().get(vehicleId);

            if ((distance / 1000) > 1.0) {
                this.coldEmissionAnalysisModule.calculateColdEmissionsAndThrowEvent(
                        coldEmissionEventLinkId,
                        vehicle,
                        event.getTime(),
                        parkingDuration,
                        2
                );
                this.vehicleId2accumulatedDistance.remove(vehicleId);
            } else {
                this.vehicleId2accumulatedDistance.put(vehicleId, distance);
            }
        }
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        if (!event.getNetworkMode().equals("car")) {
            if( nonCarWarn <=1) {
                logger.warn("non-car modes are supported, however, not properly tested yet.");
                logger.warn(Gbl.ONLYONCE);
                nonCarWarn++;
            }
        }
        Id<Vehicle> vehicleId = event.getVehicleId();
        Double stopEngineTime = event.getTime();
        this.vehicleId2stopEngineTime.put(vehicleId, stopEngineTime);
    }

    // TODO actually, the engine starts before with the PersonEntersVehicleEvent
    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        if (!event.getNetworkMode().equals("car")) {
            if( nonCarWarn <=1) {
                logger.warn("non-car modes are supported, however, not properly tested yet.");
                logger.warn(Gbl.ONLYONCE);
                nonCarWarn++;
            }
        }
        Id<Link> linkId = event.getLinkId();
        Id<Vehicle> vehicleId = event.getVehicleId();
        double startEngineTime = event.getTime();
        this.vehicleId2coldEmissionEventLinkId.put(vehicleId, linkId);

        double parkingDuration;
        if (this.vehicleId2stopEngineTime.containsKey(vehicleId)) {
            double stopEngineTime = this.vehicleId2stopEngineTime.get(vehicleId);
            parkingDuration = startEngineTime - stopEngineTime;

        } else { //parking duration is assumed to be at least 12 hours when parking overnight
            parkingDuration = 43200.0;
        }
        this.vehicleId2parkingDuration.put(vehicleId, parkingDuration);
        this.vehicleId2accumulatedDistance.put(vehicleId, 0.0);

        Vehicle vehicle = vehicles.getVehicles().get(vehicleId);

        this.coldEmissionAnalysisModule.calculateColdEmissionsAndThrowEvent(
                linkId,
                vehicle,
                startEngineTime,
                parkingDuration,
                1
        );
    }
}
