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
package playground.julia.emissions;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;

import com.mysql.jdbc.StringUtils;



import playground.vsp.emissions.events.ColdEmissionEventImpl;
import playground.vsp.emissions.types.ColdPollutant;
import playground.vsp.emissions.types.HbefaColdEmissionFactor;
import playground.vsp.emissions.types.HbefaColdEmissionFactorKey;
import playground.vsp.emissions.types.HbefaVehicleAttributes;
import playground.vsp.emissions.types.HbefaVehicleCategory;

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
 * <li>HBEFA 3.1 does not provide further distance categories for cold start emission factors; <br>
 * <li>HBEFA 3.1 does not provide cold start emission factors for Heavy Goods Vehicles; <br>
 * <li>The major part of cold start emissions is known to be emitted during the first few kilometers;
 * here it is assumed to be emitted on the first link of the leg.
 * </ul>
 * 
 * 
 * @author benjamin
 */
public class ColdEmissionAnalysisModule {
	private static final Logger logger = Logger.getLogger(ColdEmissionAnalysisModule.class);
	
	final Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable;
	final Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable;
	
	final EventsManager eventsManager;
	final Double emissionEfficiencyFactor;
	
	int vehInfoWarnHDVCnt = 0;
	int vehAttributesNotSpecifiedCnt = 0;
	final int maxWarnCnt = 3;
//	Set<Id> vehAttributesNotSpecified = new HashSet<Id>();
//	Set<Id> vehicleIdSet = new HashSet<Id>();
	//TODO anderes format?
	Map <String, HbefaVehicleCategory> vehcatMap;

	public static class ColdEmissionAnalysisModuleParameter {
		public Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable;
		public Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable;


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
		this.vehcatMap = new HashMap <String, HbefaVehicleCategory>();
		for(HbefaVehicleCategory vehcat: HbefaVehicleCategory.values()){
				vehcatMap.put(vehcat.toString(), vehcat);
			}
	}

	public void reset() {
		logger.info("resetting counters...");
		vehInfoWarnHDVCnt = 0;
		vehAttributesNotSpecifiedCnt = 0;
//		vehAttributesNotSpecified.clear();
//		vehicleIdSet.clear();
	}

	public void calculateColdEmissionsAndThrowEvent(
			Id coldEmissionEventLinkId,
			Id personId,
			Double startEngineTime,
			Double parkingDuration,
			Double accumulatedDistance,
			String vehicleInformation) {

		Map<ColdPollutant, Double> coldEmissions;
		if(vehicleInformation == null){
			throw new RuntimeException("Vehicle type description for person " + personId + "is missing. " +
					"Please make sure that requirements for emission vehicles in "
					+ VspExperimentalConfigGroup.GROUP_NAME + " config group are met. Aborting...");
		}
		Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple = convertString2Tuple(vehicleInformation);
		if (vehicleInformationTuple.getFirst() == null){
			throw new RuntimeException("Vehicle category for person " + personId + " is not valid. " +
					"Please make sure that requirements for emission vehicles in " + 
					VspExperimentalConfigGroup.GROUP_NAME + " config group are met. Aborting...");
		}
		coldEmissions = calculateColdEmissions(personId, parkingDuration, accumulatedDistance, vehicleInformationTuple);

//		logger.debug("Original cold emissions: " + coldEmissions);
		// a basic apporach to introduce emission reduced cars:
		if(emissionEfficiencyFactor != null){
			coldEmissions = rescaleColdEmissions(coldEmissions);
//			logger.debug("Original cold emissions have been rescaled to: " + coldEmissions);
		}
		Event coldEmissionEvent = new ColdEmissionEventImpl(startEngineTime, coldEmissionEventLinkId, personId, coldEmissions);
		this.eventsManager.processEvent(coldEmissionEvent);
	}

	private Map<ColdPollutant, Double> rescaleColdEmissions(Map<ColdPollutant, Double> coldEmissions) {
		Map<ColdPollutant, Double> rescaledColdEmissions = new HashMap<ColdPollutant, Double>();
		
		for(ColdPollutant wp : coldEmissions.keySet()){
			Double orgValue = coldEmissions.get(wp);
			Double rescaledValue = emissionEfficiencyFactor * orgValue;
			rescaledColdEmissions.put(wp, rescaledValue);
		}
		return rescaledColdEmissions;
	}

	private Map<ColdPollutant, Double> calculateColdEmissions(
			Id personId,
			double parkingDuration,
			double accumulatedDistance,
			Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple) {

		Map<ColdPollutant, Double> coldEmissionsOfEvent = new HashMap<ColdPollutant, Double>();
		
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
		
		int distance_km;
		if ((accumulatedDistance / 1000) < 1.0) distance_km = 1;
		else distance_km = 2;

		int parkingDuration_h = Math.max(1, (int) (parkingDuration / 3600));
		if (parkingDuration_h >= 12) parkingDuration_h = 13;
		
		key.setHbefaDistance(distance_km);
		key.setHbefaParkingTime(parkingDuration_h);
		key.setHbefaVehicleAttributes(vehicleInformationTuple.getSecond());
		
		double generatedEmissions;
		
		if(this.detailedHbefaColdTable != null){ // check if detailed emission factors file is set in config
			
		for (ColdPollutant coldPollutant : ColdPollutant.values()) {
			generatedEmissions=.0;
			key.setHbefaComponent(coldPollutant);
			
			if(this.detailedHbefaColdTable.containsKey(key)){
				generatedEmissions = this.detailedHbefaColdTable.get(key).getColdEmissionFactor();
			} else {
				generatedEmissions = this.avgHbefaColdTable.get(key).getColdEmissionFactor();
			
				if(vehAttributesNotSpecifiedCnt < maxWarnCnt) {
					vehAttributesNotSpecifiedCnt++;
					logger.warn("Detailed vehicle attributes are not specified correctly for person " + personId + ": " + 
						    "`" + vehicleInformationTuple.getSecond() + "'. Using fleet average values instead.");
					if(vehAttributesNotSpecifiedCnt == maxWarnCnt) logger.warn(Gbl.FUTURE_SUPPRESSED);
				}
//					vehAttributesNotSpecified.add(personId);
			}
			coldEmissionsOfEvent.put(coldPollutant, generatedEmissions);
			} 
		
		}else { // detailed emission factors file not set
			for (ColdPollutant coldPollutant : ColdPollutant.values()) {
				generatedEmissions = this.avgHbefaColdTable.get(key).getColdEmissionFactor();
				
				if(vehAttributesNotSpecifiedCnt < maxWarnCnt) {
					vehAttributesNotSpecifiedCnt++;
					logger.warn("Detailed vehicle attributes are not specified correctly for person " + personId + ": " + 
						    "`" + vehicleInformationTuple.getSecond() + "'. Using fleet average values instead.");
					if(vehAttributesNotSpecifiedCnt == maxWarnCnt) logger.warn(Gbl.FUTURE_SUPPRESSED);
			}
			//				vehAttributesNotSpecified.add(personId);
			coldEmissionsOfEvent.put(coldPollutant, generatedEmissions);	
			}
			
		}
//		vehicleIdSet.add(personId);
		return coldEmissionsOfEvent;
	}

	private Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> convertString2Tuple(String vehicleInformation) {
		Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple;
		HbefaVehicleCategory hbefaVehicleCategory = null;
		HbefaVehicleAttributes hbefaVehicleAttributes = new HbefaVehicleAttributes();
	
		//aus dem String zuerst die vehcat auslesen
		int techStart= vehicleInformation.indexOf(";")+1;
		if (techStart==0) techStart = vehicleInformation.length()+1;
		String potVehcat = vehicleInformation.substring(0, techStart-1);
		
		if(vehcatMap.containsKey(potVehcat)){
			hbefaVehicleCategory = vehcatMap.get(potVehcat);			
			int sizeStart= vehicleInformation.indexOf(";", techStart)+1;
			int conceptStart= vehicleInformation.indexOf(";", sizeStart)+1;	

			// if one of the indices is -1 => not enough matches of ";"
			// => interpretation as "average vehicle"
			if (!(sizeStart==-1||conceptStart==-1)) {
				int end= vehicleInformation.indexOf(";", conceptStart);
				if (end==-1)end=vehicleInformation.length();

				// all substrings with content
				// otherwise => interpretation as "average vehicle"
				if (sizeStart - techStart > 1 && conceptStart - sizeStart > 1 && end - conceptStart > 1) {
					hbefaVehicleAttributes.setHbefaTechnology(vehicleInformation.substring(techStart, sizeStart-1));
					hbefaVehicleAttributes.setHbefaSizeClass(vehicleInformation.substring(sizeStart, conceptStart-1));
					hbefaVehicleAttributes.setHbefaEmConcept(vehicleInformation.substring(conceptStart, end));
				} 		
			}
		}else{//veh category not set
			return new Tuple<HbefaVehicleCategory, HbefaVehicleAttributes>(null, null);
		}
		
		vehicleInformationTuple = new Tuple<HbefaVehicleCategory, HbefaVehicleAttributes>(hbefaVehicleCategory, hbefaVehicleAttributes);
		return vehicleInformationTuple;
		
	}

//	public Set<Id> getVehAttributesNotSpecified() {
//		return vehAttributesNotSpecified;
//	}
//
//	public Set<Id> getVehicleIdSet() {
//		return vehicleIdSet;
//	}
}