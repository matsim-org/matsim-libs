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

package org.matsim.freight.carriers.jsprit;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.vehicles.VehicleType;

/**
 * @author steffenaxer
 */
public class NetworkBasedTransportCostsFactory implements VRPTransportCostsFactory {
    final Scenario scenario;
    final Carriers carriers;
    final Map<String, TravelTime> travelTimes;
    final Config config;

    public NetworkBasedTransportCostsFactory(Scenario scenario, Carriers carriers, Map<String, TravelTime> travelTimes, Config config) {
        this.scenario = scenario;
        this.carriers = carriers;
        this.travelTimes = travelTimes;
        this.config = config;

    }

    @Override
    public VRPTransportCosts createVRPTransportCosts() {
        FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config,
                FreightCarriersConfigGroup.class);

        Set<VehicleType> vehicleTypes = new HashSet<>();
        carriers.getCarriers().values().forEach(
                carrier -> vehicleTypes.addAll(carrier.getCarrierCapabilities().getVehicleTypes()));

        NetworkBasedTransportCosts.Builder netBuilder = NetworkBasedTransportCosts.Builder
                .newInstance(scenario.getNetwork(), vehicleTypes);

        netBuilder.setTimeSliceWidth(freightCarriersConfigGroup.getTravelTimeSliceWidth());
        netBuilder.setTravelTime(travelTimes.get(TransportMode.car));
        return netBuilder.build();
    }
}
