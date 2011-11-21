/* *********************************************************************** *
 /* *********************************************************************** *
 * project: org.matsim.*
 * FhEmissions.java
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
/**  @author friederike**/

package playground.benjamin.emissions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;

import playground.benjamin.emissions.events.WarmEmissionEventImpl;
import playground.benjamin.emissions.types.HbefaAvgWarmEmissionFactorKey;
import playground.benjamin.emissions.types.HbefaDetailedWarmEmissionFactorKey;
import playground.benjamin.emissions.types.HbefaTrafficSituation;
import playground.benjamin.emissions.types.HbefaVehicleCategory;
import playground.benjamin.emissions.types.HbefaWarmEmissionFactor;
import playground.benjamin.emissions.types.WarmPollutant;

/**
 * @author benjamin
 *
 */
public class WarmEmissionAnalysisModule {
	private static final Logger logger = Logger.getLogger(WarmEmissionAnalysisModule.class);

	private final Map<Integer, String> roadTypeMapping;

	private final Map<HbefaAvgWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable;
	private final Map<HbefaDetailedWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable;

	private final EventsManager eventsManager;
	
	private static int vehInfoWarnAvailCnt = 0;
	private static int vehInfoWarnValidCnt = 0;
	private static int maxVehInfoWarnCnt = 2;
	private static Set<Id> vehInfoNotAvail = new HashSet<Id>();
	private static Set<Id> vehInfoNotValid = new HashSet<Id>();

	public WarmEmissionAnalysisModule(
			Map<Integer, String> roadTypeMapping,
			Map<HbefaAvgWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable,
			Map<HbefaDetailedWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable,
			EventsManager emissionEventsManager) {
		this.roadTypeMapping = roadTypeMapping;
		this.avgHbefaWarmTable = avgHbefaWarmTable;
		this.detailedHbefaWarmTable = detailedHbefaWarmTable;
		this.eventsManager = emissionEventsManager;
	}

	public void calculateWarmEmissionsAndThrowEvent(Id linkId, Id personId,
			Integer roadType, Double freeVelocity, Double linkLength,
			Double enterTime, Double travelTime, String ageFuelCcm) {

		Map<WarmPollutant, Double> warmEmissions = calculateWarmEmissions(personId, roadType, linkLength, travelTime, ageFuelCcm);
		Event warmEmissionEvent = new WarmEmissionEventImpl(enterTime, linkId, personId, warmEmissions);
		this.eventsManager.processEvent(warmEmissionEvent);
	}

	// TODO: merge detailed and average emission calculation
	private Map<WarmPollutant, Double> calculateWarmEmissions(Id personId,
			Integer roadType, Double linkLength, Double travelTime,
			String ageFuelCcm) {

		Map<WarmPollutant, Double> warmEmissions;

		if(this.detailedHbefaWarmTable != null){ // check if detailed emission factors file is set in config

			if (ageFuelCcm != null){ // check if vehicle file provides information

				String [] hbefaVehicleAttributes = mapVehicleAttributesFromMiD2Hbefa(ageFuelCcm);

				if(hbefaVehicleAttributes != null){ // check if vehicle information is valid
					warmEmissions = calculateDetailedEmissions(personId, travelTime, roadType, linkLength, hbefaVehicleAttributes);
				} else {
					warmEmissions = calculateAverageEmissions(personId, travelTime, roadType, linkLength);
					if(vehInfoWarnValidCnt <= maxVehInfoWarnCnt) {
						logger.warn("Vehicle information for person " + personId + " is not valid. Using fleet average values instead.");
						if(vehInfoWarnValidCnt == maxVehInfoWarnCnt){
							logger.warn(Gbl.FUTURE_SUPPRESSED);
						}
					}
					vehInfoWarnValidCnt++;
					vehInfoNotValid.add(personId);
				}
			} else {
				warmEmissions = calculateAverageEmissions(personId, travelTime, roadType, linkLength);

				if (vehInfoWarnAvailCnt <= maxVehInfoWarnCnt) {
					logger.warn("Vehicle information for person " + personId + " is non-existing. Using fleet average values instead.");
					if (vehInfoWarnAvailCnt == maxVehInfoWarnCnt)
						logger.warn(Gbl.FUTURE_SUPPRESSED);
				}
				vehInfoWarnAvailCnt++;
				vehInfoNotAvail.add(personId);
			}
		} else {
			warmEmissions = calculateAverageEmissions(personId, travelTime, roadType, linkLength);
			vehInfoWarnAvailCnt++;
			vehInfoNotAvail.add(personId);
		}
		return warmEmissions;
	}

	
	private Map<WarmPollutant, Double> calculateDetailedEmissions(Id personId, double travelTime, int roadType, double linkLength, String[] hbefaVehicleAttributes) {
		Map<WarmPollutant, Double> emissionsOfEvent = new HashMap<WarmPollutant, Double>();

		String hbefaRoadTypeName = this.roadTypeMapping.get(roadType);

		String hbefaTechnology = hbefaVehicleAttributes[0];
		String hbefaSizeClass = hbefaVehicleAttributes[1];
		String hbefaEmConcept = hbefaVehicleAttributes[2];

		HbefaDetailedWarmEmissionFactorKey keyFreeFlow = new HbefaDetailedWarmEmissionFactorKey();
		HbefaDetailedWarmEmissionFactorKey keyStopAndGo = new HbefaDetailedWarmEmissionFactorKey();
		//TODO: better filter for passenger cars vs. HDVs; maybe through vehicle file?
		if(personId.toString().contains("gv_")){
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
		keyFreeFlow.setHbefaTechnology(hbefaTechnology);
		keyStopAndGo.setHbefaTechnology(hbefaTechnology);
		keyFreeFlow.setHbefaSizeClass(hbefaSizeClass);
		keyStopAndGo.setHbefaSizeClass(hbefaSizeClass);
		keyFreeFlow.setHbefaEmConcept(hbefaEmConcept);
		keyStopAndGo.setHbefaEmConcept(hbefaEmConcept);

		double averageSpeed = (linkLength / 1000) / (travelTime / 3600);

		for (WarmPollutant warmPollutant : WarmPollutant.values()) {
			keyFreeFlow.setHbefaComponent(warmPollutant);
			keyStopAndGo.setHbefaComponent(warmPollutant);

			double freeFlowSpeed = this.detailedHbefaWarmTable.get(keyFreeFlow).getSpeed();
			double stopGoSpeed = this.detailedHbefaWarmTable.get(keyStopAndGo).getSpeed();
			Double efFreeFlow = this.detailedHbefaWarmTable.get(keyFreeFlow).getEmissionFactor();				

			//TODO: is this really the "right" way of doing this?!?
			Double generatedEmissions;
			if (averageSpeed < stopGoSpeed) {
				generatedEmissions = linkLength / 1000 * efFreeFlow;
			} else {
				Double stopGoTime = ((linkLength / 1000) / averageSpeed) - ((linkLength / 1000) / freeFlowSpeed);
				Double stopGoFraction = stopGoSpeed * stopGoTime;
				Double freeFlowFraction = (linkLength / 1000) - stopGoFraction;
				Double efStopGo = this.detailedHbefaWarmTable.get(keyStopAndGo).getEmissionFactor();

				generatedEmissions = (freeFlowFraction * efFreeFlow) + (stopGoFraction * efStopGo);
			}
			emissionsOfEvent.put(warmPollutant, generatedEmissions);
		}
		return emissionsOfEvent;
	}

	@Deprecated
	private String[] mapVehicleAttributesFromMiD2Hbefa(String ageFuelCcm) {
		String[] ageFuelCcmArray = ageFuelCcm.split(";");
		String[] technologySizeConcept = new String[3];
	
		int year = splitAndReduce(ageFuelCcmArray[0], ":");
		int fuelType = splitAndReduce(ageFuelCcmArray[1], ":");
		int cubicCap = splitAndReduce(ageFuelCcmArray[2], ":");

		if (fuelType == 1)
			technologySizeConcept[0] = "petrol (4S)";
		else if (fuelType == 2)
			technologySizeConcept[0] = "diesel";
		else
			return null;

		if (cubicCap <= 1400)
			technologySizeConcept[1] = "<1,4L";
		else if (cubicCap <= 2000 && cubicCap > 1400)
			technologySizeConcept[1] = "1,4-<2L";
		else if (cubicCap > 2000 && cubicCap < 90000)
			technologySizeConcept[1] = ">=2L";
		else
			return null;

		if (year < 1993 && fuelType == 1)
			technologySizeConcept[2] = "PC-P-Euro-0";
		else if (year < 1993 && fuelType == 2)
			technologySizeConcept[2] = "PC-D-Euro-0";
		else if (year < 1997 && fuelType == 1)
			technologySizeConcept[2] = "PC-P-Euro-1";
		else if (year < 1997 && fuelType == 2)
			technologySizeConcept[2] = "PC-D-Euro-1";
		else if (year < 2001 && fuelType == 1)
			technologySizeConcept[2] = "PC-P-Euro-2";
		else if (year < 2001 && fuelType == 2)
			technologySizeConcept[2] = "PC-D-Euro-2";
		else if (year < 2006 && fuelType == 1)
			technologySizeConcept[2] = "PC-P-Euro-3";
		else if (year < 2006 && fuelType == 2)
			technologySizeConcept[2] = "PC-D-Euro-3";
		else if (year < 2011 && fuelType == 1)
			technologySizeConcept[2] = "PC-P-Euro-4";
		else if (year < 2011 && fuelType == 2)
			technologySizeConcept[2] = "PC-D-Euro-4";
		else if (year < 2015 && fuelType == 1)
			technologySizeConcept[2] = "PC-P-Euro-5";
		else if (year < 2015 && fuelType == 2)
			technologySizeConcept[2] = "PC-D-Euro-5";
		else
			return null;
	
		return technologySizeConcept;
	}

	@Deprecated
	private int splitAndReduce(String string, String splitSign) {
		String[] array = string.split(splitSign);
		return Integer.valueOf(array[1]);
	}

	private Map<WarmPollutant, Double> calculateAverageEmissions(Id personId, double travelTime, int roadType, double linkLength) {
		Map<WarmPollutant, Double> avgEmissionsOfEvent = new HashMap<WarmPollutant, Double>();

		String hbefaRoadTypeName = this.roadTypeMapping.get(roadType);

		HbefaAvgWarmEmissionFactorKey keyFreeFlow = new HbefaAvgWarmEmissionFactorKey();
		HbefaAvgWarmEmissionFactorKey keyStopAndGo = new HbefaAvgWarmEmissionFactorKey();
		//TODO: better filter for passenger cars vs. HDVs; maybe through vehicle file?
		if(personId.toString().contains("gv_")){
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

		double averageSpeed = (linkLength / 1000) / (travelTime / 3600);

		for (WarmPollutant warmPollutant : WarmPollutant.values()) {
			keyFreeFlow.setHbefaComponent(warmPollutant);
			keyStopAndGo.setHbefaComponent(warmPollutant);

			double freeFlowSpeed = this.avgHbefaWarmTable.get(keyFreeFlow).getSpeed();
			double stopGoSpeed = this.avgHbefaWarmTable.get(keyStopAndGo).getSpeed();
			Double efFreeFlow = this.avgHbefaWarmTable.get(keyFreeFlow).getEmissionFactor();				

			//TODO: is this really the "right" way of doing this?!?
			Double generatedEmissions;
			if (averageSpeed < stopGoSpeed) {
				generatedEmissions = linkLength / 1000 * efFreeFlow;
			} else {
				Double stopGoTime = ((linkLength / 1000) / averageSpeed) - ((linkLength / 1000) / freeFlowSpeed);
				Double stopGoFraction = stopGoSpeed * stopGoTime;
				Double freeFlowFraction = (linkLength / 1000) - stopGoFraction;
				Double efStopGo = this.avgHbefaWarmTable.get(keyStopAndGo).getEmissionFactor();

				generatedEmissions = (freeFlowFraction * efFreeFlow) + (stopGoFraction * efStopGo);
			}
			avgEmissionsOfEvent.put(warmPollutant, generatedEmissions);
		}
		return avgEmissionsOfEvent;
	}

	public static int getVehInfoWarnAvailCnt() {
		return vehInfoWarnAvailCnt;
	}
	
	public static int getVehInfoWarnValidCnt() {
		return vehInfoWarnValidCnt;
	}

	public static Set<Id> getVehInfoNotAvail() {
		return vehInfoNotAvail;
	}

	public static Set<Id> getVehInfoNotValid() {
		return vehInfoNotValid;
	}
}