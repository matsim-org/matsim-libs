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

	private static int vehInfoWarnValidCnt = 0;
	private static int maxVehInfoWarnCnt = 3;
	private static Set<Id> vehInfoNotValid = new HashSet<Id>();

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

		HbefaVehicleAttributes hbefaVehicleAttributes = new HbefaVehicleAttributes();
		hbefaVehicleAttributes.setHbefaTechnology(vehicleInformationTuple.getSecond().getHbefaTechnology());
		hbefaVehicleAttributes.setHbefaSizeClass(vehicleInformationTuple.getSecond().getHbefaSizeClass());
		hbefaVehicleAttributes.setHbefaEmConcept(vehicleInformationTuple.getSecond().getHbefaEmConcept());

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
		keyFreeFlow.setHbefaVehicleAttributes(hbefaVehicleAttributes);
		keyStopAndGo.setHbefaVehicleAttributes(hbefaVehicleAttributes);

		double averageSpeed = (linkLength / 1000) / (travelTime / 3600);

		for (WarmPollutant warmPollutant : WarmPollutant.values()) {
			Double generatedEmissions;

			keyFreeFlow.setHbefaComponent(warmPollutant);
			keyStopAndGo.setHbefaComponent(warmPollutant);

			double freeFlowSpeed;
			double stopGoSpeed;
			double efFreeFlow;
			double efStopGo;

			if(this.detailedHbefaWarmTable != null){ // check if detailed emission factors file is set in config
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
				freeFlowSpeed = this.avgHbefaWarmTable.get(keyFreeFlow).getSpeed();
				stopGoSpeed = this.avgHbefaWarmTable.get(keyStopAndGo).getSpeed();
				efFreeFlow = this.avgHbefaWarmTable.get(keyFreeFlow).getWarmEmissionFactor();
				efStopGo = this.avgHbefaWarmTable.get(keyStopAndGo).getWarmEmissionFactor();
			}

			//TODO: is this really the "right" way of doing this?!?
			if (averageSpeed < stopGoSpeed) {
				generatedEmissions = linkLength / 1000 * efFreeFlow;
			} else {
				Double stopGoTime = ((linkLength / 1000) / averageSpeed) - ((linkLength / 1000) / freeFlowSpeed);
				Double stopGoFraction = stopGoSpeed * stopGoTime;
				Double freeFlowFraction = (linkLength / 1000) - stopGoFraction;

				generatedEmissions = (freeFlowFraction * efFreeFlow) + (stopGoFraction * efStopGo);
			}
			warmEmissionsOfEvent.put(warmPollutant, generatedEmissions);
		}
		return warmEmissionsOfEvent;
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