/* *********************************************************************** *
 /* ********************************************************************** *
 * project: org.matsim.*												   *
 * WarmEmissionAnalysisModule.java									       *
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
 *                                                                         *
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

import playground.benjamin.emissions.events.WarmEmissionEventImpl;
import playground.benjamin.emissions.types.HbefaTrafficSituation;
import playground.benjamin.emissions.types.HbefaVehicleAttributes;
import playground.benjamin.emissions.types.HbefaVehicleCategory;
import playground.benjamin.emissions.types.HbefaWarmEmissionFactor;
import playground.benjamin.emissions.types.HbefaWarmEmissionFactorKey;
import playground.benjamin.emissions.types.WarmPollutant;

/**
 * @author benjamin
 *
 */
public class WarmEmissionAnalysisModule {
	private static final Logger logger = Logger.getLogger(WarmEmissionAnalysisModule.class);

	private final Map<Integer, String> roadTypeMapping;

	private final Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable;
	private final Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable;

	private final EventsManager eventsManager;

	private static int vehAttributesNotSpecifiedCnt = 0;
	private static int maxWarnCnt = 3;
	private static Set<Id> vehAttributesNotSpecified = new HashSet<Id>();
	private static Set<Id> vehicleIdSet = new HashSet<Id>();

	static int eventCounter = 0;
	static int freeFlowCounter = 0;
	static int stopGoCounter = 0;

	public WarmEmissionAnalysisModule(
			Map<Integer, String> roadTypeMapping,
			Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable,
			Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable,
			EventsManager emissionEventsManager) {

		this.roadTypeMapping = roadTypeMapping;
		this.avgHbefaWarmTable = avgHbefaWarmTable;
		this.detailedHbefaWarmTable = detailedHbefaWarmTable;
		this.eventsManager = emissionEventsManager;
	}

	public void calculateWarmEmissionsAndThrowEvent(
			Id linkId,
			Id personId,
			Integer roadType,
			Double freeVelocity,
			Double linkLength,
			Double enterTime,
			Double travelTime,
			String vehicleInformation) {

		Map<WarmPollutant, Double> warmEmissions;

		if(vehicleInformation != null){ // check if vehicle file provides vehicle description
			Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple = convertString2Tuple(vehicleInformation);

			if (vehicleInformationTuple.getFirst() != null){ // check if the required vehicle category could be interpreted
				warmEmissions = calculateWarmEmissions(personId, travelTime, roadType, linkLength, vehicleInformationTuple);
			} else throw new RuntimeException("Vehicle category for person " + personId + " is not valid. " +
					"Please make sure that requirements for emission vehicles in " + 
					VspExperimentalConfigGroup.GROUP_NAME + " config group are met. Aborting...");

		} else throw new RuntimeException("Vehicle type description for person " + personId + "is missing. " +
				"Please make sure that requirements for emission vehicles in "
				+ VspExperimentalConfigGroup.GROUP_NAME + " config group are met. Aborting...");

		Event warmEmissionEvent = new WarmEmissionEventImpl(enterTime, linkId, personId, warmEmissions);
		this.eventsManager.processEvent(warmEmissionEvent);
	}

	private Map<WarmPollutant, Double> calculateWarmEmissions(
			Id personId,
			Double travelTime,
			Integer roadType,
			Double linkLength,
			Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple) {

		Map<WarmPollutant, Double> warmEmissionsOfEvent = new HashMap<WarmPollutant, Double>();

		String hbefaRoadTypeName = this.roadTypeMapping.get(roadType);

		HbefaWarmEmissionFactorKey keyFreeFlow = new HbefaWarmEmissionFactorKey();
		HbefaWarmEmissionFactorKey keyStopAndGo = new HbefaWarmEmissionFactorKey();

		if(vehicleInformationTuple.getFirst().equals(HbefaVehicleCategory.HEAVY_GOODS_VEHICLE)){
			keyFreeFlow.setHbefaVehicleCategory(HbefaVehicleCategory.HEAVY_GOODS_VEHICLE);
			keyStopAndGo.setHbefaVehicleCategory(HbefaVehicleCategory.HEAVY_GOODS_VEHICLE);
		} else{
			keyFreeFlow.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			keyStopAndGo.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
		}
		keyFreeFlow.setHbefaRoadCategory(hbefaRoadTypeName);
		keyStopAndGo.setHbefaRoadCategory(hbefaRoadTypeName);
		keyFreeFlow.setHbefaTrafficSituation(HbefaTrafficSituation.FREEFLOW);
		keyStopAndGo.setHbefaTrafficSituation(HbefaTrafficSituation.STOPANDGO);

		double freeFlowSpeed;
		double stopGoSpeed;
		double efFreeFlow;
		double efStopGo;

		for (WarmPollutant warmPollutant : WarmPollutant.values()) {
			Double generatedEmissions;

			keyFreeFlow.setHbefaComponent(warmPollutant);
			keyStopAndGo.setHbefaComponent(warmPollutant);

			if(this.detailedHbefaWarmTable != null){ // check if detailed emission factors file is set in config
				HbefaVehicleAttributes hbefaVehicleAttributes = new HbefaVehicleAttributes();
				hbefaVehicleAttributes.setHbefaTechnology(vehicleInformationTuple.getSecond().getHbefaTechnology());
				hbefaVehicleAttributes.setHbefaSizeClass(vehicleInformationTuple.getSecond().getHbefaSizeClass());
				hbefaVehicleAttributes.setHbefaEmConcept(vehicleInformationTuple.getSecond().getHbefaEmConcept());
				keyFreeFlow.setHbefaVehicleAttributes(hbefaVehicleAttributes);
				keyStopAndGo.setHbefaVehicleAttributes(hbefaVehicleAttributes);
				
				if(this.detailedHbefaWarmTable.containsKey(keyFreeFlow) && this.detailedHbefaWarmTable.containsKey(keyStopAndGo)){
					freeFlowSpeed = this.detailedHbefaWarmTable.get(keyFreeFlow).getSpeed();
					stopGoSpeed = this.detailedHbefaWarmTable.get(keyStopAndGo).getSpeed();
					efFreeFlow = this.detailedHbefaWarmTable.get(keyFreeFlow).getWarmEmissionFactor();
					efStopGo = this.detailedHbefaWarmTable.get(keyStopAndGo).getWarmEmissionFactor();

				} else {
					freeFlowSpeed = this.avgHbefaWarmTable.get(keyFreeFlow).getSpeed();
					stopGoSpeed = this.avgHbefaWarmTable.get(keyStopAndGo).getSpeed();
					efFreeFlow = this.avgHbefaWarmTable.get(keyFreeFlow).getWarmEmissionFactor();
					efStopGo = this.avgHbefaWarmTable.get(keyStopAndGo).getWarmEmissionFactor();

					if(vehAttributesNotSpecifiedCnt < maxWarnCnt) {
						vehAttributesNotSpecifiedCnt++;
						logger.warn("Detailed vehicle attributes are not specified correctly for person " + personId + ": " + 
								"`" + vehicleInformationTuple.getSecond() + "'. Using fleet average values instead.");
						if(vehAttributesNotSpecifiedCnt == maxWarnCnt) logger.warn(Gbl.FUTURE_SUPPRESSED);
					}
					vehAttributesNotSpecified.add(personId);
				}
			} else {
				freeFlowSpeed = this.avgHbefaWarmTable.get(keyFreeFlow).getSpeed();
				stopGoSpeed = this.avgHbefaWarmTable.get(keyStopAndGo).getSpeed();
				efFreeFlow = this.avgHbefaWarmTable.get(keyFreeFlow).getWarmEmissionFactor();
				efStopGo = this.avgHbefaWarmTable.get(keyStopAndGo).getWarmEmissionFactor();

				vehAttributesNotSpecified.add(personId);
			}
			//TODO: is this really the "right" way of doing this?!?
			double linkLength_m = linkLength / 1000;
			double averageSpeed = linkLength_m / (travelTime / 3600);
			if (averageSpeed < stopGoSpeed) {
				generatedEmissions = linkLength_m * efFreeFlow;
				freeFlowCounter++;
			} else {
				Double stopGoTime = (linkLength_m / averageSpeed) - (linkLength_m / freeFlowSpeed);
				Double stopGoFraction = stopGoSpeed * stopGoTime;
				Double freeFlowFraction = linkLength_m - stopGoFraction;

				generatedEmissions = (freeFlowFraction * efFreeFlow) + (stopGoFraction * efStopGo);
				stopGoCounter++;
			}
			warmEmissionsOfEvent.put(warmPollutant, generatedEmissions);
		}
		eventCounter++;
		vehicleIdSet.add(personId);
		return warmEmissionsOfEvent;
	}

	private Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> convertString2Tuple(String vehicleInformation) {
		Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple;
		HbefaVehicleCategory hbefaVehicleCategory = null;
		HbefaVehicleAttributes hbefaVehicleAttributes = new HbefaVehicleAttributes();

		String[] vehicleInformationArray = vehicleInformation.split(";");

		for(HbefaVehicleCategory vehCat : HbefaVehicleCategory.values()){
			if(vehCat.toString().equals(vehicleInformationArray[0])){
				hbefaVehicleCategory = vehCat;
			} else continue;
		}

		if(vehicleInformationArray.length == 4){
			hbefaVehicleAttributes.setHbefaTechnology(vehicleInformationArray[1]);
			hbefaVehicleAttributes.setHbefaSizeClass(vehicleInformationArray[2]);
			hbefaVehicleAttributes.setHbefaEmConcept(vehicleInformationArray[3]);
		} else{
			// interpretation as "average vehicle"
		}

		vehicleInformationTuple = new Tuple<HbefaVehicleCategory, HbefaVehicleAttributes>(hbefaVehicleCategory, hbefaVehicleAttributes);
		return vehicleInformationTuple;
	}

	public static Set<Id> getVehAttributesNotSpecified() {
		return vehAttributesNotSpecified;
	}

	public static Set<Id> getVehicleIdSet() {
		return vehicleIdSet;
	}

	public static int getFreeFlowOccurences() {
		return freeFlowCounter / WarmPollutant.values().length;
	}
	
	public static int getStopGoOccurences() {
		return stopGoCounter / WarmPollutant.values().length;
	}

	public static int getEventCounter() {
		return eventCounter;
	}
}