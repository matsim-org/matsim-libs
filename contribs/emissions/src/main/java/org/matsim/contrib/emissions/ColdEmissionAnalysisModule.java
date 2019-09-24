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
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.NonScenarioVehicles;
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
final class ColdEmissionAnalysisModule {
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
	private final Set<String> coldPollutants;
  private int noVehWarnCnt = 0;

	public static class ColdEmissionAnalysisModuleParameter {
		public final Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable;
		public final Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable;
		private final Set<String> coldPollutants;
		private final EmissionsConfigGroup ecg;

		public ColdEmissionAnalysisModuleParameter(
				Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable,
				Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable, Set<String> coldPollutants, EmissionsConfigGroup emissionsConfigGroup) {
			this.avgHbefaColdTable = avgHbefaColdTable;
			this.detailedHbefaColdTable = detailedHbefaColdTable;
			this.coldPollutants = coldPollutants;
			this.ecg = emissionsConfigGroup;
		}
	}

	public ColdEmissionAnalysisModule(
			ColdEmissionAnalysisModuleParameter parameterObject,
			EventsManager emissionEventsManager, Double emissionEfficiencyFactor) {

		this.avgHbefaColdTable = parameterObject.avgHbefaColdTable;
		this.detailedHbefaColdTable = parameterObject.detailedHbefaColdTable;
		this.coldPollutants = parameterObject.coldPollutants;
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
		
		if (vehicle == null) {
			if (ecg.getNonScenarioVehicles().equals(NonScenarioVehicles.abort)) {
				throw new RuntimeException(
						"Vehicle is null. " +
						"Please make sure that requirements for emission vehicles in " + EmissionsConfigGroup.GROUP_NAME + " config group are met."
								+ " Or set the parameter + 'nonScenarioVehicles' to 'ignore' in order to skip such vehicles."
								+ " Aborting...");
			} else if (ecg.getNonScenarioVehicles().equals(NonScenarioVehicles.ignore)) {
				if (noVehWarnCnt < 10) {
					logger.warn(
							"Vehicle will be ignored.");
					noVehWarnCnt++;
					if (noVehWarnCnt == 10) logger.warn(Gbl.FUTURE_SUPPRESSED);
				}
			} else {
				throw new RuntimeException("Not yet implemented. Aborting...");
			}
							
		} else {
			
//			if(this.ecg.isUsingVehicleTypeIdAsVehicleDescription() ) {
//				if(vehicle.getType().getDescription()==null) { // emission specification is in vehicle type id
//					vehicle.getType().setDescription( EmissionUtils.EmissionSpecificationMarker.BEGIN_EMISSIONS
//							+vehicle.getType().getId().toString()+ EmissionUtils.EmissionSpecificationMarker.END_EMISSIONS );
//				} else if( vehicle.getType().getDescription().contains( EmissionUtils.EmissionSpecificationMarker.BEGIN_EMISSIONS.toString() ) ) {
//					// emission specification is in vehicle type id and in vehicle description too.
//				} else {
//					String vehicleDescription = vehicle.getType().getDescription() + EmissionUtils.EmissionSpecificationMarker.BEGIN_EMISSIONS
//							+ vehicle.getType().getId().toString()+ EmissionUtils.EmissionSpecificationMarker.END_EMISSIONS;
//					vehicle.getType().setDescription(vehicleDescription);
//				}
//			}
//
//			String vehicleDescription = vehicle.getType().getDescription();
//
//		if(vehicle.getType().getDescription() == null){
//			throw new RuntimeException("Vehicle type description for vehicle " + vehicle + "is missing. " +
//					"Please make sure that requirements for emission vehicles in "
//					+ EmissionsConfigGroup.GROUP_NAME + " config group are met. Aborting...");
//		}

			String hbefaVehicleTypeDescription = EmissionUtils.getHbefaVehicleDescription( vehicle.getType(), this.ecg );

			Gbl.assertNotNull( hbefaVehicleTypeDescription );

			Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple = EmissionUtils.convertVehicleDescription2VehicleInformationTuple(hbefaVehicleTypeDescription);

			Gbl.assertNotNull( vehicleInformationTuple );

		if (vehicleInformationTuple.getFirst() == null){
			throw new RuntimeException("Vehicle category for vehicle " + vehicle + " is not valid. " +
					"Please make sure that requirements for emission vehicles in " + 
					EmissionsConfigGroup.GROUP_NAME + " config group are met. Aborting...");
		}
		
		Map<String, Double> coldEmissions = getColdPollutantDoubleMap( vehicle.getId(), parkingDuration, vehicleInformationTuple, distance_km );

			// a basic apporach to introduce emission reduced cars:
			if(emissionEfficiencyFactor != null){
				coldEmissions = rescaleColdEmissions(coldEmissions);
			}
			Event coldEmissionEvent = new ColdEmissionEvent(eventTime, coldEmissionEventLinkId, vehicle.getId(), coldEmissions);
			this.eventsManager.processEvent(coldEmissionEvent);
		}
	}

	private Map<String, Double> rescaleColdEmissions(Map<String, Double> coldEmissions) {
		Map<String, Double> rescaledColdEmissions = new HashMap<>();
		
		for(String wp : coldEmissions.keySet()){
			Double orgValue = coldEmissions.get(wp);
			Double rescaledValue = emissionEfficiencyFactor * orgValue;
			rescaledColdEmissions.put(wp, rescaledValue);
		}
		return rescaledColdEmissions;
	}

    private Map<String, Double> getColdPollutantDoubleMap(Id<Vehicle> vehicleId, double parkingDuration, Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple, int distance_km) {
        final Map<String, Double> coldEmissionsOfEvent = new HashMap<>();

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
			for (String cp : coldPollutants){
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
			for (String cp : coldPollutants){
				coldEmissionsOfEvent.put( cp, 0.0 );
			}
			return coldEmissionsOfEvent;
		} else {
            key.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
        }

        int parkingDuration_h = Math.max(1, (int) (parkingDuration / 3600));
        if (parkingDuration_h >= 12) parkingDuration_h = 13;

        key.setHbefaParkingTime(parkingDuration_h);

        for (String coldPollutant : coldPollutants) {
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

    private double getTableEmissions(Id<Vehicle> vehicleId, Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple, int distance_km, HbefaColdEmissionFactorKey key, String coldPollutant) {
        key.setHbefaDistance(distance_km);
        HbefaColdEmissionFactor generatedEmissions = null;

        key.setHbefaComponent(coldPollutant);
		key.setHbefaVehicleAttributes(vehicleInformationTuple.getSecond());

        if(this.detailedHbefaColdTable != null && key.getHbefaVehicleAttributes().isDetailed()) { // check if detailed emission factors file is set in config
//		  HbefaVehicleAttributes hbefaVehicleAttributes = createHbefaVehicleAttributes( vehicleInformationTuple.getSecond() );
			generatedEmissions = this.detailedHbefaColdTable.get(key);
		}
		if(generatedEmissions == null){
			if(vehAttributesNotSpecifiedCnt < maxWarnCnt) {
				vehAttributesNotSpecifiedCnt++;
				logger.warn("No detailed entry (for vehicle `" + vehicleId + "') corresponds to `" + vehicleInformationTuple.getSecond() + "'. Falling back on fleet average values.");
				if(vehAttributesNotSpecifiedCnt == maxWarnCnt) logger.warn(Gbl.FUTURE_SUPPRESSED);
			}

			//try just with engine technoogy
			HbefaVehicleAttributes hbva = new HbefaVehicleAttributes();
			hbva.setHbefaTechnology(key.getHbefaVehicleAttributes().getHbefaTechnology());
			key.setHbefaVehicleAttributes(hbva);
			generatedEmissions = this.avgHbefaColdTable.get(key);

        }
		if (generatedEmissions == null) {
			//revert way back to fleet averages, not just fuel type
			key.setHbefaVehicleAttributes(new HbefaVehicleAttributes());
			generatedEmissions = this.avgHbefaColdTable.get(key);
		}
        return generatedEmissions.getColdEmissionFactor();
	    
	    // yy when thinking about the above, it is actually not so clear what that "fallback" actually means ... since
	    // the exact key now just needs to be in the avg table.  So it is not really a fallback, but rather just
	    // another lookup in another table. ---???  kai, jul'18
	    // (It may implicitly work from convertVehicleDescription2VehicleInformationTuple, which essentially generates an empty vehicle
	    // description if nothing specific is available.  And thus the "average" table should contain "empty" entries, different
	    // from what the tests imply. kai, jul'18)
    }
	
	static HbefaVehicleAttributes createHbefaVehicleAttributes( final String hbefaTechnology, final String hbefaSizeClass, final String hbefaEmConcept ) {
		HbefaVehicleAttributes vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology( hbefaTechnology );
		vehAtt.setHbefaSizeClass( hbefaSizeClass );
		vehAtt.setHbefaEmConcept( hbefaEmConcept );
		return vehAtt;
	}
	
}
