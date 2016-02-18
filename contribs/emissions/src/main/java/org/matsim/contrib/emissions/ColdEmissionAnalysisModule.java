/* *********************************************************************** *
 /* *********************************************************************** *
 * project: org.matsim.*
 * ColdEmissionAnalysisModule.java
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
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.HbefaColdEmissionFactor;
import org.matsim.contrib.emissions.types.HbefaColdEmissionFactorKey;
import org.matsim.contrib.emissions.types.HbefaVehicleAttributes;
import org.matsim.contrib.emissions.types.HbefaVehicleCategory;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;


/**
 * 2 categories for distance driven AFTER coldstart:
 * <ul>
 * <li> 0 - 1 km </li>
 * <li> 1 - 2 km </li>
 * </ul>
 * 
 * 13 categories for parking time BEFORE coldstart:
 * <ul>
 * <li> 0 - 1 h [1]</li>
 * <li> 1 - 2 h [2]</li>
 * <li> ... </li>
 * <li> 11 - 12 h [12]</li>
 * <li> > 12 h [13]</li>
 * </ul>
 * 
 * Remarks:
 * <ul>
 * <li>HBEFA 3.1 does not provide further distance categories for cold start emission factors when average amient temperature is assumed <br>
 * <li>HBEFA 3.1 does not provide cold start emission factors for Heavy Goods Vehicles; thus, HGV are assumed to produce the same cold start emission factors as passenger cars <br>
 * <li>In the current implementation, vehicles emit one part of their cold start emissions when the engine is started (distance class 0 - 1 km);
 * after reaching 1 km, the rest of their cold start emissions is emitted (difference between distance class 1 - 2 km and distance class 0 - 1 km)
 * </ul>
 * 
 * 
 * @author benjamin
 */
public class ColdEmissionAnalysisModule {
	private static final Logger logger = Logger.getLogger(ColdEmissionAnalysisModule.class);
	
	private final Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable;
	private final Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable;
	
	private final EventsManager eventsManager;
	private final Double emissionEfficiencyFactor;
	
	private int vehInfoWarnHDVCnt = 0;
	private int vehAttributesNotSpecifiedCnt = 0;
	private static final int maxWarnCnt = 3;

	public static class ColdEmissionAnalysisModuleParameter {
		public final Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable;
		public final Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable;

		public ColdEmissionAnalysisModuleParameter(
				Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable,
				Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable) {
			this.avgHbefaColdTable = avgHbefaColdTable;
			this.detailedHbefaColdTable = detailedHbefaColdTable;
		}
	}

	public ColdEmissionAnalysisModule(
			ColdEmissionAnalysisModuleParameter parameterObject,
			EventsManager emissionEventsManager, Double emissionEfficiencyFactor) {

		this.avgHbefaColdTable = parameterObject.avgHbefaColdTable;
		this.detailedHbefaColdTable = parameterObject.detailedHbefaColdTable;
		this.eventsManager = emissionEventsManager;
		this.emissionEfficiencyFactor = emissionEfficiencyFactor;
	}

	public void reset() {
		logger.info("resetting counters...");
		vehInfoWarnHDVCnt = 0;
		vehAttributesNotSpecifiedCnt = 0;
	}

	public void calculateColdEmissionsAndThrowEvent(
			Id<Link> coldEmissionEventLinkId,
			Id<Vehicle> vehicleId,
			double eventTime,
			double parkingDuration,
            int distance_km,
			Id<VehicleType> vehicleTypeId) {

		Map<ColdPollutant, Double> coldEmissions;
		if(vehicleTypeId == null){
			throw new RuntimeException("Vehicle type description for vehicle " + vehicleId + "is missing. " +
					"Please make sure that requirements for emission vehicles in "
					+ EmissionsConfigGroup.GROUP_NAME + " config group are met. Aborting...");
		}
		Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple = convertVehicleTypeId2VehicleInformationTuple(vehicleTypeId);
		if (vehicleInformationTuple.getFirst() == null){
			throw new RuntimeException("Vehicle category for vehicle " + vehicleId + " is not valid. " +
					"Please make sure that requirements for emission vehicles in " + 
					EmissionsConfigGroup.GROUP_NAME + " config group are met. Aborting...");
		}
        coldEmissions = getColdPollutantDoubleMap(vehicleId, parkingDuration, vehicleInformationTuple, distance_km);

		// a basic apporach to introduce emission reduced cars:
		if(emissionEfficiencyFactor != null){
			coldEmissions = rescaleColdEmissions(coldEmissions);
		}
		Event coldEmissionEvent = new ColdEmissionEvent(eventTime, coldEmissionEventLinkId, vehicleId, coldEmissions);
		this.eventsManager.processEvent(coldEmissionEvent);
	}

	private Map<ColdPollutant, Double> rescaleColdEmissions(Map<ColdPollutant, Double> coldEmissions) {
		Map<ColdPollutant, Double> rescaledColdEmissions = new HashMap<>();
		
		for(ColdPollutant wp : coldEmissions.keySet()){
			Double orgValue = coldEmissions.get(wp);
			Double rescaledValue = emissionEfficiencyFactor * orgValue;
			rescaledColdEmissions.put(wp, rescaledValue);
		}
		return rescaledColdEmissions;
	}

    private Map<ColdPollutant, Double> getColdPollutantDoubleMap(Id<Vehicle> vehicleId, double parkingDuration, Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple, int distance_km) {
        Map<ColdPollutant, Double> coldEmissionsOfEvent = new HashMap<>();

        HbefaColdEmissionFactorKey key = new HbefaColdEmissionFactorKey();

        if(vehicleInformationTuple.getFirst().equals(HbefaVehicleCategory.HEAVY_GOODS_VEHICLE)){
            key.setHbefaVehicleCategory(HbefaVehicleCategory.HEAVY_GOODS_VEHICLE);
            key.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
            if(vehInfoWarnHDVCnt < maxWarnCnt) {
                vehInfoWarnHDVCnt++;
                logger.warn("HBEFA 3.1 does not provide cold start emission factors for " +
                        HbefaVehicleCategory.HEAVY_GOODS_VEHICLE +
                        ". Setting vehicle category to " + HbefaVehicleCategory.PASSENGER_CAR + "...");
                if(vehInfoWarnHDVCnt == maxWarnCnt) logger.warn(Gbl.FUTURE_SUPPRESSED);
            }
        } else{
            key.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
        }


        int parkingDuration_h = Math.max(1, (int) (parkingDuration / 3600));
        if (parkingDuration_h >= 12) parkingDuration_h = 13;

        key.setHbefaParkingTime(parkingDuration_h);

        for (ColdPollutant coldPollutant : ColdPollutant.values()) {
            double generatedEmissions;
            if (distance_km == 1) {
               generatedEmissions = getTableEmissions(vehicleId, vehicleInformationTuple, 1, key, coldPollutant);
            } else {
               generatedEmissions = getTableEmissions(vehicleId, vehicleInformationTuple, 2, key, coldPollutant) - getTableEmissions(vehicleId, vehicleInformationTuple, 1, key, coldPollutant);
            }
            coldEmissionsOfEvent.put(coldPollutant, generatedEmissions);
        }
        return coldEmissionsOfEvent;
    }

    private double getTableEmissions(Id<Vehicle> vehicleId, Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple, int distance_km, HbefaColdEmissionFactorKey key, ColdPollutant coldPollutant) {
        key.setHbefaDistance(distance_km);
        double generatedEmissions;

        key.setHbefaComponent(coldPollutant);

        if(this.detailedHbefaColdTable != null){ // check if detailed emission factors file is set in config
            HbefaVehicleAttributes hbefaVehicleAttributes = new HbefaVehicleAttributes();
            hbefaVehicleAttributes.setHbefaTechnology(vehicleInformationTuple.getSecond().getHbefaTechnology());
            hbefaVehicleAttributes.setHbefaSizeClass(vehicleInformationTuple.getSecond().getHbefaSizeClass());
            hbefaVehicleAttributes.setHbefaEmConcept(vehicleInformationTuple.getSecond().getHbefaEmConcept());
            key.setHbefaVehicleAttributes(hbefaVehicleAttributes);

            if(this.detailedHbefaColdTable.containsKey(key)){
                generatedEmissions = this.detailedHbefaColdTable.get(key).getColdEmissionFactor();
            } else {
                generatedEmissions = this.avgHbefaColdTable.get(key).getColdEmissionFactor();

                if(vehAttributesNotSpecifiedCnt < maxWarnCnt) {
                    vehAttributesNotSpecifiedCnt++;
                    logger.warn("Detailed vehicle attributes are not specified correctly for vehicle " + vehicleId + ": " +
                            "`" + vehicleInformationTuple.getSecond() + "'. Using fleet average values instead.");
                    if(vehAttributesNotSpecifiedCnt == maxWarnCnt) logger.warn(Gbl.FUTURE_SUPPRESSED);
                }
            }
        } else {
            generatedEmissions = this.avgHbefaColdTable.get(key).getColdEmissionFactor();
        }
        return generatedEmissions;
    }

    private Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> convertVehicleTypeId2VehicleInformationTuple(Id<VehicleType> vehicleTypeId) {
		Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple;
		HbefaVehicleCategory hbefaVehicleCategory = null;
		HbefaVehicleAttributes hbefaVehicleAttributes = new HbefaVehicleAttributes();

		String[] vehicleInformationArray = vehicleTypeId.toString().split(";");

		for(HbefaVehicleCategory vehCat : HbefaVehicleCategory.values()){
			if(vehCat.toString().equals(vehicleInformationArray[0])){
				hbefaVehicleCategory = vehCat;
			}
		}

		if(vehicleInformationArray.length == 4){
			hbefaVehicleAttributes.setHbefaTechnology(vehicleInformationArray[1]);
			hbefaVehicleAttributes.setHbefaSizeClass(vehicleInformationArray[2]);
			hbefaVehicleAttributes.setHbefaEmConcept(vehicleInformationArray[3]);
		} // else interpretation as "average vehicle"

		vehicleInformationTuple = new Tuple<>(hbefaVehicleCategory, hbefaVehicleAttributes);
		return vehicleInformationTuple;
	}

}