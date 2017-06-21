/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

package playground.agarwalamit.berlin.berlinBVG09;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.types.HbefaVehicleCategory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

/**
 * Created by amit on 11.06.17.
 */

public class BerlinTransitVehicleTypeIdentifier {

    public enum BerlinTransitEmissionVehicleType {
        BUS_AS_HGV, TRAINS_AS_ZERO_EMISSIONS
    }

    private static final Logger LOGGER = Logger.getLogger(BerlinTransitVehicleTypeIdentifier.class);
    private final Map<HbefaVehicleCategory,List<Id<VehicleType>>> vehicleCategoryToVehicleTypeList = new HashMap<>();
    private Vehicles transitVehicles;
    private final Map<Id<Vehicle>, BerlinTransitEmissionVehicleType> transitVehicleType2BerlinVehicleType = new HashMap<>();

    public BerlinTransitVehicleTypeIdentifier (final String vehiclesFile) {
        run(vehiclesFile);
    }

    private HbefaVehicleCategory getHBEFAVehicleCategoryFromVehicleType(final Id<VehicleType> vehicleTypeId) {
        for (Map.Entry<HbefaVehicleCategory,List<Id<VehicleType>>> e : vehicleCategoryToVehicleTypeList.entrySet()) {
            if (e.getValue().contains(vehicleTypeId)) {
                return e.getKey();
            }
        }

        LOGGER.warn("no HBEFA vehicle category is identified, using "+ HbefaVehicleCategory.PASSENGER_CAR);
        return HbefaVehicleCategory.PASSENGER_CAR;
    }

    public BerlinTransitEmissionVehicleType getBerlinTransitEmissionVehicleType (final Id<Vehicle> vehicleId) {
        HbefaVehicleCategory hbefaVehicleCategory = getHBEFAVehicleCategory(vehicleId);
        if ( hbefaVehicleCategory.equals(HbefaVehicleCategory.HEAVY_GOODS_VEHICLE) ) return BerlinTransitEmissionVehicleType.BUS_AS_HGV;
        else return BerlinTransitEmissionVehicleType.TRAINS_AS_ZERO_EMISSIONS;
    }

    public HbefaVehicleCategory getHBEFAVehicleCategory(final Id<Vehicle> vehicleId) {
        if (! transitVehicles.getVehicles().containsKey(vehicleId)) {
            throw new RuntimeException("Vehicle id"+ vehicleId+ " is not a transit vehicle id.");
        }
        Id<VehicleType> vehicleTypeId = transitVehicles.getVehicles().get(vehicleId).getType().getId();
        return getHBEFAVehicleCategoryFromVehicleType(vehicleTypeId);
    }

    private void run (final String vehiclesFile) {
        Config config = ConfigUtils.createConfig();
        config.transit().setUseTransit(true);
        config.transit().setVehiclesFile(vehiclesFile);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        this.transitVehicles = scenario.getTransitVehicles();

        // get info from transit vehicles
        // trains/trams will be emission free, rest will be HGV

        vehicleCategoryToVehicleTypeList.put(HbefaVehicleCategory.ZERO_EMISSION_VEHICLE, new ArrayList<>());
        vehicleCategoryToVehicleTypeList.put(HbefaVehicleCategory.HEAVY_GOODS_VEHICLE, new ArrayList<>());

        for(VehicleType vehicleType : this.transitVehicles.getVehicleTypes().values()) {
            String description = vehicleType.getDescription();
            if (description.equals("S Default") || description.equals("Regio Default") ||
                    description.equals("Bahn Default") || description.equals("Fern Default") || description.equals("Default Tram") ||
                    description.equals("default U-Bahn") || description.startsWith("Kleinprofil A3") || description.contains("zug") ||
                    description.contains("Straßenbahnen") || description.contains("tram") || description.startsWith("GT6N-Doppeltraktion") ||
                    description.startsWith("Flexity Zweirichter") || description.startsWith("Klp") || description.startsWith("Grp") ||
                    description.startsWith("KT4D-Doppeltraktion")|| description.startsWith("Flexity Einrichter") || description.contains("GT6") ||
                    description.contains("KT4D")) {

                vehicleCategoryToVehicleTypeList.get(HbefaVehicleCategory.ZERO_EMISSION_VEHICLE).add(vehicleType.getId());

            } else if (description.startsWith("Volvo") || description.contains("bus") || description.contains("Bus") ||
                    description.equals("Solaris Urbino") || description.startsWith("MAN") || description.equals("Historisches Fahrzeug") ||
                    description.equals("betriebsärtzlicher Dienst") || description.startsWith("Doppeldeck") || description.startsWith("Solaris Urbino") ||
                    description.startsWith("Top-Tour") || description.contains("Nostalgie") || description.startsWith("Eindecker barrierefrei")) {

                List<Id<VehicleType>> vehicleTypes = vehicleCategoryToVehicleTypeList.get(HbefaVehicleCategory.HEAVY_GOODS_VEHICLE);
                vehicleCategoryToVehicleTypeList.get(HbefaVehicleCategory.HEAVY_GOODS_VEHICLE).add(vehicleType.getId() );

            } else if (description.contains("Fähre") || description.contains("Faehre")) {
                LOGGER.warn("Not sure, how to estimate emissions for ferries. Currently adding them to "+ HbefaVehicleCategory.HEAVY_GOODS_VEHICLE);
                vehicleCategoryToVehicleTypeList.get(HbefaVehicleCategory.HEAVY_GOODS_VEHICLE).add(vehicleType.getId());
            } else {
                LOGGER.warn("Not sure about the vehicle type "+description+". Using seating and standing capacity to identify the HBEFA vehicle category.");
                if (vehicleType.getCapacity().getSeats() + vehicleType.getCapacity().getStandingRoom() <= 10 ) {
                    // it looks like that total capacity of the bus are somewhat in the range of 4-8 or so (probably for 10% sample)
                    vehicleCategoryToVehicleTypeList.get(HbefaVehicleCategory.HEAVY_GOODS_VEHICLE).add(vehicleType.getId());
                } else {
                    vehicleCategoryToVehicleTypeList.get(HbefaVehicleCategory.ZERO_EMISSION_VEHICLE).add(vehicleType.getId());
                }
            }
        }
    }
}
