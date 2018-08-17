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
import org.matsim.contrib.emissions.types.*;
import org.matsim.contrib.emissions.utils.EmissionSpecificationMarker;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;


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
	private final EmissionsConfigGroup ecg;
	
	private int vehInfoWarnHDVCnt = 0;
	private int vehAttributesNotSpecifiedCnt = 0;
	private static final int maxWarnCnt = 3;
	private int vehInfoWarnMotorCylceCnt = 0;
	
	public static class ColdEmissionAnalysisModuleParameter {
		public final Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable;
		public final Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable;
		private final EmissionsConfigGroup ecg;

		public ColdEmissionAnalysisModuleParameter(
				Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable,
				Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable, EmissionsConfigGroup emissionsConfigGroup) {
			this.avgHbefaColdTable = avgHbefaColdTable;
			this.detailedHbefaColdTable = detailedHbefaColdTable;
			this.ecg = emissionsConfigGroup;
		}
	}

	public ColdEmissionAnalysisModule(
			ColdEmissionAnalysisModuleParameter parameterObject,
			EventsManager emissionEventsManager, Double emissionEfficiencyFactor) {

		this.avgHbefaColdTable = parameterObject.avgHbefaColdTable;
		this.detailedHbefaColdTable = parameterObject.detailedHbefaColdTable;
		this.ecg = parameterObject.ecg;
		this.eventsManager = emissionEventsManager;
		this.emissionEfficiencyFactor = emissionEfficiencyFactor;
	}

	public void reset() {
		logger.info("resetting counters...");
		vehInfoWarnHDVCnt = 0;
		vehAttributesNotSpecifiedCnt = 0;
	}

	void calculateColdEmissionsAndThrowEvent(
			Id<Link> coldEmissionEventLinkId,
			Vehicle vehicle,
			double eventTime,
			double parkingDuration,
			int distance_km ) {

		if(this.ecg.isUsingVehicleTypeIdAsVehicleDescription() ) {
			if(vehicle.getType().getDescription()==null) { // emission specification is in vehicle type id
				vehicle.getType().setDescription(EmissionSpecificationMarker.BEGIN_EMISSIONS
						+vehicle.getType().getId().toString()+ EmissionSpecificationMarker.END_EMISSIONS);
			} else if( vehicle.getType().getDescription().contains(EmissionSpecificationMarker.BEGIN_EMISSIONS.toString()) ) {
				// emission specification is in vehicle type id and in vehicle description too.
			} else {
				String vehicleDescription = vehicle.getType().getDescription() + EmissionSpecificationMarker.BEGIN_EMISSIONS
						+ vehicle.getType().getId().toString()+ EmissionSpecificationMarker.END_EMISSIONS;
				vehicle.getType().setDescription(vehicleDescription);
			}
		}

		String vehicleDescription = vehicle.getType().getDescription();

		Map<ColdPollutant, Double> coldEmissions = new HashMap<>();
		if(vehicle.getType().getDescription() == null){
			throw new RuntimeException("Vehicle type description for vehicle " + vehicle + "is missing. " +
					"Please make sure that requirements for emission vehicles in "
					+ EmissionsConfigGroup.GROUP_NAME + " config group are met. Aborting...");
		}
		Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple = convertVehicleDescription2VehicleInformationTuple(vehicleDescription);
		if (vehicleInformationTuple.getFirst() == null){
			throw new RuntimeException("Vehicle category for vehicle " + vehicle + " is not valid. " +
					"Please make sure that requirements for emission vehicles in " + 
					EmissionsConfigGroup.GROUP_NAME + " config group are met. Aborting...");
		}

		coldEmissions = getColdPollutantDoubleMap(vehicle.getId(), parkingDuration, vehicleInformationTuple, distance_km);

		// a basic apporach to introduce emission reduced cars:
		if(emissionEfficiencyFactor != null){
			coldEmissions = rescaleColdEmissions(coldEmissions);
		}
		Event coldEmissionEvent = new ColdEmissionEvent(eventTime, coldEmissionEventLinkId, vehicle.getId(), coldEmissions);
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
        final Map<ColdPollutant, Double> coldEmissionsOfEvent = new HashMap<>();

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
        } else if(vehicleInformationTuple.getFirst().equals(HbefaVehicleCategory.ZERO_EMISSION_VEHICLE)) {
			for (ColdPollutant cp : ColdPollutant.values()){
				coldEmissionsOfEvent.put( cp, 0.0 );
			}
			return coldEmissionsOfEvent;
		} else if (vehicleInformationTuple.getFirst().equals(HbefaVehicleCategory.MOTORCYCLE)) {
			if(vehInfoWarnMotorCylceCnt == 0) {
				vehInfoWarnMotorCylceCnt++;
				logger.warn("HBEFA 3.1 does not provide cold start emission factors for " +
						HbefaVehicleCategory.MOTORCYCLE +
						". Setting cold emissions to zero.");
				logger.warn(Gbl.ONLYONCE + "\t" + Gbl.FUTURE_SUPPRESSED);
			}
			for (ColdPollutant cp : ColdPollutant.values()){
				coldEmissionsOfEvent.put( cp, 0.0 );
			}
			return coldEmissionsOfEvent;
		} else {
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
//		  HbefaVehicleAttributes hbefaVehicleAttributes = createHbefaVehicleAttributes( vehicleInformationTuple.getSecond() );
		  key.setHbefaVehicleAttributes(vehicleInformationTuple.getSecond());

            if(this.detailedHbefaColdTable.containsKey(key)){
                generatedEmissions = this.detailedHbefaColdTable.get(key).getColdEmissionFactor();
            } else {
			if(vehAttributesNotSpecifiedCnt < maxWarnCnt) {
				vehAttributesNotSpecifiedCnt++;
				logger.warn("No detailed entry (for vehicle `" + vehicleId + "') corresponds to `" + vehicleInformationTuple.getSecond() + "'. Falling back on fleet average values.");
				if(vehAttributesNotSpecifiedCnt == maxWarnCnt) logger.warn(Gbl.FUTURE_SUPPRESSED);
			}

                generatedEmissions = this.avgHbefaColdTable.get(key).getColdEmissionFactor();
            }
        } else {
            generatedEmissions = this.avgHbefaColdTable.get(key).getColdEmissionFactor();
        }
        return generatedEmissions;
	    
	    // yy when thinking about the above, it is actually not so clear what that "fallback" actually means ... since
	    // the exact key now just needs to be in the avg table.  So it is not really a fallback, but rather just
	    // another lookup in another table. ---???  kai, jul'18
	    // (It may implicitly work from convertVehicleDescription2VehicleInformationTuple, which essentially generates an empty vehicle
	    // description if nothing specific is available.  And thus the "average" table should contain "empty" entries, different
	    // from what the tests imply. kai, jul'18)
    }
	
	private Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> convertVehicleDescription2VehicleInformationTuple(String vehicleDescription) {
		Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple;
		HbefaVehicleCategory hbefaVehicleCategory = null;

		int startIndex = vehicleDescription.indexOf(EmissionSpecificationMarker.BEGIN_EMISSIONS.toString()) + EmissionSpecificationMarker.BEGIN_EMISSIONS.toString().length();
		int endIndex = vehicleDescription.lastIndexOf(EmissionSpecificationMarker.END_EMISSIONS.toString());

		String[] vehicleInformationArray = vehicleDescription.substring(startIndex, endIndex).split(";");

		for(HbefaVehicleCategory vehCat : HbefaVehicleCategory.values()){
			if(vehCat.toString().equals(vehicleInformationArray[0])){
				hbefaVehicleCategory = vehCat;
			}
		}
		
		HbefaVehicleAttributes hbefaVehicleAttributes = new HbefaVehicleAttributes();
		if(vehicleInformationArray.length == 4){
			hbefaVehicleAttributes.setHbefaTechnology(vehicleInformationArray[1]);
			hbefaVehicleAttributes.setHbefaSizeClass(vehicleInformationArray[2]);
			hbefaVehicleAttributes.setHbefaEmConcept(vehicleInformationArray[3]);
		} // else interpretation as "average vehicle"

		vehicleInformationTuple = new Tuple<>(hbefaVehicleCategory, hbefaVehicleAttributes);
		return vehicleInformationTuple;
	}
	
	static HbefaVehicleAttributes createHbefaVehicleAttributes( final String hbefaTechnology, final String hbefaSizeClass, final String hbefaEmConcept ) {
		HbefaVehicleAttributes vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology( hbefaTechnology );
		vehAtt.setHbefaSizeClass( hbefaSizeClass );
		vehAtt.setHbefaEmConcept( hbefaEmConcept );
		return vehAtt;
	}
	
//	private static HbefaVehicleAttributes createHbefaVehicleAttributes( final HbefaVehicleAttributes hbefaVehicleAttributes ) {
//		// yyyy is this copy really necessary?  kai, jul'18
//		return createHbefaVehicleAttributes( hbefaVehicleAttributes.getHbefaTechnology(), hbefaVehicleAttributes.getHbefaSizeClass(), hbefaVehicleAttributes.getHbefaEmConcept() ) ;
//	}
	
	
	
}
