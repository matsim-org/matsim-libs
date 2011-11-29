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
package playground.benjamin.emissions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;

import playground.benjamin.emissions.events.ColdEmissionEventImpl;
import playground.benjamin.emissions.types.ColdPollutant;
import playground.benjamin.emissions.types.HbefaColdEmissionFactor;
import playground.benjamin.emissions.types.HbefaColdEmissionFactorKey;
import playground.benjamin.emissions.types.HbefaVehicleAttributes;
import playground.benjamin.emissions.types.HbefaVehicleCategory;

/**
 * Two categories for distance driven AFTER coldstart:
 * <ul>
 * <li> 0-1km </li>
 * <li> 1-2km </li>
 * </ul>
 * HBEFA 3.1 does not provide further distance categories for cold start emission factors; <br>
 * HBEFA 3.1 does not provide cold start emission factors for Heavy Goods Vehicles; <br>
 * <br>
 * The biggest part of cold start emissions is known to be emitted during the first few kilometers;
 * here it is assumed to be emitted at the first link of the leg.
 * 
 * 
 * @author benjamin
 */
public class ColdEmissionAnalysisModule {
	private static final Logger logger = Logger.getLogger(ColdEmissionAnalysisModule.class);
	
	private final Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable;
	private final Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable;
	
	private final EventsManager eventsManager;
	
	private static int vehInfoWarnValidCnt = 0;
	private static int maxVehInfoWarnCnt = 3;
	private static Set<Id> vehInfoNotValid = new HashSet<Id>();

	public ColdEmissionAnalysisModule(
			Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable,
			Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable,
			EventsManager emissionEventsManager) {

		this.avgHbefaColdTable = avgHbefaColdTable;
		this.detailedHbefaColdTable = detailedHbefaColdTable;
		this.eventsManager = emissionEventsManager;
	}

	public void calculateColdEmissionsAndThrowEvent(
			Id coldEmissionEventLinkId,
			Id personId,
			Double startEngineTime,
			Double parkingDuration,
			Double accumulatedDistance,
			String vehicleInformation) {

		Map<ColdPollutant, Double> coldEmissions;

		if(vehicleInformation != null){ // check if vehicle file provides vehicle description
			Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple = convertString2Tuple(vehicleInformation);

			if (vehicleInformationTuple.getFirst() != null){ // check if the required vehicle category could be interpreted
				coldEmissions = calculateColdEmissions(personId, parkingDuration, accumulatedDistance, vehicleInformationTuple);
			} else throw new RuntimeException("Vehicle category for person " + personId + " is not valid. " +
					"Please make sure that requirements for emission vehicles in " + 
					VspExperimentalConfigGroup.GROUP_NAME + " config group are met. Aborting...");

		} else throw new RuntimeException("Vehicle type description for person " + personId + "is missing. " +
				"Please make sure that requirements for emission vehicles in "
				+ VspExperimentalConfigGroup.GROUP_NAME + " config group are met. Aborting...");

		Event coldEmissionEvent = new ColdEmissionEventImpl(startEngineTime, coldEmissionEventLinkId, personId, coldEmissions);
		this.eventsManager.processEvent(coldEmissionEvent);
	}

	private Map<ColdPollutant, Double> calculateColdEmissions(
			Id personId,
			Double parkingDuration,
			Double accumulatedDistance,
			Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple) {

		Map<ColdPollutant, Double> coldEmissionsOfEvent = new HashMap<ColdPollutant, Double>();
		
		HbefaVehicleAttributes hbefaVehicleAttributes = new HbefaVehicleAttributes();
		hbefaVehicleAttributes.setHbefaTechnology(vehicleInformationTuple.getSecond().getHbefaTechnology());
		hbefaVehicleAttributes.setHbefaSizeClass(vehicleInformationTuple.getSecond().getHbefaSizeClass());
		hbefaVehicleAttributes.setHbefaEmConcept(vehicleInformationTuple.getSecond().getHbefaEmConcept());
		
		HbefaColdEmissionFactorKey key = new HbefaColdEmissionFactorKey();
		
		if(vehicleInformationTuple.getFirst().equals(HbefaVehicleCategory.HEAVY_GOODS_VEHICLE)){
			key.setHbefaVehicleCategory(HbefaVehicleCategory.HEAVY_GOODS_VEHICLE);
		} else{
			key.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
		}
		
		int distance_km;
		if ((accumulatedDistance / 1000) < 1.0) distance_km = 1;
		else distance_km = 2;

		int parkingDuration_h = (int) (parkingDuration / 3600);
		if (parkingDuration_h >= 12) parkingDuration_h = 13;
		
		key.setHbefaDistance(distance_km);
		key.setHbefaParkingTime(parkingDuration_h);
		key.setHbefaVehicleAttributes(hbefaVehicleAttributes);
		
		for (ColdPollutant coldPollutant : ColdPollutant.values()) {
			Double generatedEmissions;
			
			key.setHbefaComponent(coldPollutant);
			
			if(this.detailedHbefaColdTable != null){ // check if detailed emission factors file is set in config
				if(this.detailedHbefaColdTable.containsKey(key)){
					generatedEmissions = this.detailedHbefaColdTable.get(key).getColdEmissionFactor();
				} else {
					generatedEmissions = this.avgHbefaColdTable.get(key).getColdEmissionFactor();
					
					if(vehInfoWarnValidCnt < maxVehInfoWarnCnt) {
						vehInfoWarnValidCnt++;
						logger.warn("Detailed vehicle information for person " + personId + " is not valid. Using fleet average values instead.");
						if(vehInfoWarnValidCnt == maxVehInfoWarnCnt){
							logger.warn(Gbl.FUTURE_SUPPRESSED);
						}
					}
					vehInfoNotValid.add(personId);
				}
			} else {
				generatedEmissions = this.avgHbefaColdTable.get(key).getColdEmissionFactor();
			}
			coldEmissionsOfEvent.put(coldPollutant, generatedEmissions);
		}
		return coldEmissionsOfEvent;
	}

	private Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> convertString2Tuple(String vehicleInformation) {
		Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple;
		HbefaVehicleCategory hbefaVehicleCategory = null;
		HbefaVehicleAttributes hbefaVehicleAttributes = new HbefaVehicleAttributes();

		String[] vehicleInformationArray = vehicleInformation.split(";");

		for(HbefaVehicleCategory vehCat : HbefaVehicleCategory.values()){
			if(vehCat.equals(vehicleInformationArray[0])){
				hbefaVehicleCategory = vehCat;
			} else continue;
		}

		if(vehicleInformationArray.length == 4){
			hbefaVehicleAttributes.setHbefaTechnology(vehicleInformationArray[1]);
			hbefaVehicleAttributes.setHbefaSizeClass(vehicleInformationArray[2]);
			hbefaVehicleAttributes.setHbefaEmConcept(vehicleInformationArray[3]);
		} else{
			//do nothing
		}

		vehicleInformationTuple = new Tuple<HbefaVehicleCategory, HbefaVehicleAttributes>(hbefaVehicleCategory, hbefaVehicleAttributes);
		return vehicleInformationTuple;
	}

	public static int getVehInfoWarnValidCnt() {
		return vehInfoWarnValidCnt;
	}

	public static Set<Id> getVehInfoNotValid() {
		return vehInfoNotValid;
	}
}