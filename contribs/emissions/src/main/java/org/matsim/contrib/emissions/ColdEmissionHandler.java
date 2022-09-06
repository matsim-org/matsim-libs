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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * @author benjamin
 */
final class ColdEmissionHandler implements LinkLeaveEventHandler, VehicleLeavesTrafficEventHandler, VehicleEntersTrafficEventHandler {
    private static final Logger logger = LogManager.getLogger(ColdEmissionHandler.class);

    private final ColdEmissionAnalysisModule coldEmissionAnalysisModule;
    private final Scenario scenario;
    private final EmissionsConfigGroup emissionsConfigGroup;

    private int zeroLinkLengthWarnCnt = 0;
    private int nonCarWarn = 0;

    private final Map<Id<Vehicle>, Double> vehicleId2stopEngineTime = new HashMap<>();
    private final Map<Id<Vehicle>, Double> vehicleId2accumulatedDistance = new HashMap<>();
    private final Map<Id<Vehicle>, Double> vehicleId2parkingDuration = new HashMap<>();
    private final Map<Id<Vehicle>, Id<Link>> vehicleId2coldEmissionEventLinkId = new HashMap<>();

    /*package-private*/ ColdEmissionHandler( Scenario scenario, Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable,
                                Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable, Set<Pollutant> coldPollutants, EventsManager eventsManager ){

        this.coldEmissionAnalysisModule = new ColdEmissionAnalysisModule( avgHbefaColdTable, detailedHbefaColdTable,
                        ConfigUtils.addOrGetModule( scenario.getConfig(), EmissionsConfigGroup.class ), coldPollutants, eventsManager );
        this.scenario = scenario;
        this.emissionsConfigGroup = ConfigUtils.addOrGetModule( scenario.getConfig(), EmissionsConfigGroup.class );
        eventsManager.addHandler( this );
    }

        @Override
    public void reset(int iteration) {
        logger.info("resetting counters...");
        vehicleId2stopEngineTime.clear();
        vehicleId2accumulatedDistance.clear();
        vehicleId2parkingDuration.clear();
        vehicleId2coldEmissionEventLinkId.clear();

        }

    private static int noVehWarnCnt=0;

    @Override
    public void handleEvent(LinkLeaveEvent event) {
		Id<Vehicle> vehicleId = event.getVehicleId();
        Id<Link> linkId = event.getLinkId();
        Link link = this.scenario.getNetwork().getLinks().get(linkId);
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

            Vehicle vehicle = VehicleUtils.findVehicle( event.getVehicleId(), scenario );

            if ( vehicle==null ){
                handleNullVehicle( vehicleId, emissionsConfigGroup );
            } else{

                if( (distance / 1000) > 1.0 ){
                    Map<Pollutant, Double> coldEmissions = coldEmissionAnalysisModule.checkVehicleInfoAndCalculateWColdEmissions(vehicle.getType(),
                            vehicleId,
                            coldEmissionEventLinkId,
                            event.getTime(),
                            parkingDuration, 2);

                    coldEmissionAnalysisModule.throwColdEmissionEvent(vehicle.getId(), linkId, event.getTime(), coldEmissions);

                    this.vehicleId2accumulatedDistance.remove( vehicleId );
                } else{
                    this.vehicleId2accumulatedDistance.put( vehicleId, distance );
                }
                // yyyy I have absolutely no clue what the distance stuff is doing here.  kai, jan'20
            }
        }
    }
    static void handleNullVehicle( Id<Vehicle> vehicleId, EmissionsConfigGroup emissionsConfigGroup ){
        if( emissionsConfigGroup.getNonScenarioVehicles().equals( EmissionsConfigGroup.NonScenarioVehicles.abort ) ){
            throw new RuntimeException(
                            "No vehicle defined for id " + vehicleId + ". " +
                                            "Please make sure that requirements for emission vehicles in " + EmissionsConfigGroup.GROUP_NAME + " config group are met."
                                            + " Or set the parameter + 'nonScenarioVehicles' to 'ignore' in order to skip such vehicles."
                                            + " Aborting..." );
        } else if( emissionsConfigGroup.getNonScenarioVehicles().equals(
                        EmissionsConfigGroup.NonScenarioVehicles.ignore ) ){
            if( noVehWarnCnt < 10 ){
                logger.warn(
                                "No vehicle defined for id " + vehicleId + ". The vehicle will be ignored." );
                noVehWarnCnt++;
                if( noVehWarnCnt == 10 ) logger.warn( Gbl.FUTURE_SUPPRESSED );
            }
        } else{
            throw new RuntimeException( "Not yet implemented. Aborting..." );
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

        Vehicle vehicle = VehicleUtils.findVehicle( vehicleId, scenario ) ;
        if ( vehicle==null ) {
            handleNullVehicle( vehicleId, emissionsConfigGroup );
        } else{
            Map<Pollutant, Double> coldEmissions = coldEmissionAnalysisModule.checkVehicleInfoAndCalculateWColdEmissions(
                    vehicle.getType(), vehicleId, linkId, startEngineTime, parkingDuration, 1);

            coldEmissionAnalysisModule.throwColdEmissionEvent(vehicleId, linkId, startEngineTime, coldEmissions);
            // yyyy again, I do not know what the "distance" does.  kai, jan'20
        }
    }

    ColdEmissionAnalysisModule getColdEmissionAnalysisModule(){
        return coldEmissionAnalysisModule;
    }
}
