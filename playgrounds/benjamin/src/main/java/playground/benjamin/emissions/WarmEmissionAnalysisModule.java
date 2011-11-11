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
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;

import playground.benjamin.emissions.dataTypes.HbefaTrafficSituation;
import playground.benjamin.emissions.dataTypes.HbefaWarmEmissionFactors;
import playground.benjamin.emissions.dataTypes.HbefaWarmEmissionFactorsDetailed;
import playground.benjamin.emissions.dataTypes.HbefaWarmEmissionTableCreator;
import playground.benjamin.emissions.dataTypes.HbefaWarmEmissionTableCreatorDetailed;
import playground.benjamin.emissions.dataTypes.RoadTypeTrafficSit;
import playground.benjamin.emissions.events.WarmEmissionEventImpl;
import playground.benjamin.emissions.events.WarmPollutant;

public class WarmEmissionAnalysisModule {
	private static final Logger logger = Logger.getLogger(WarmEmissionAnalysisModule.class);

	private final Map<Integer, RoadTypeTrafficSit> roadTypeMapping;

	private final HbefaWarmEmissionTableCreatorDetailed hbefaWarmEmissionTableCreatorDetailed;
	private final HbefaWarmEmissionTableCreator hbefaAvgWarmEmissionTableCreator;
	private final HbefaWarmEmissionTableCreator hbefaAvgWarmEmissionTableCreatorHDV;

	private final EventsManager eventsManager;
	private static int vehInfoWarnCnt = 0;
	private static int maxVehInfoWarnCnt = 3;
	private static Set<Id> personIdSet = new HashSet<Id>();

	public WarmEmissionAnalysisModule(
			Map<Integer, RoadTypeTrafficSit> roadTypeMapping,
			HbefaWarmEmissionTableCreatorDetailed hbefaWarmEmissionTableCreatorDetailed,
			HbefaWarmEmissionTableCreator hbefaAvgWarmEmissionTableCreator,
			HbefaWarmEmissionTableCreator hbefaAvgWarmEmissionTableCreatorHDV,
			EventsManager eventsManager) {
		this.roadTypeMapping = roadTypeMapping;
		this.hbefaWarmEmissionTableCreatorDetailed = hbefaWarmEmissionTableCreatorDetailed;
		this.hbefaAvgWarmEmissionTableCreator = hbefaAvgWarmEmissionTableCreator;
		this.hbefaAvgWarmEmissionTableCreatorHDV = hbefaAvgWarmEmissionTableCreatorHDV;
		this.eventsManager = eventsManager;
	}

	public void calculateWarmEmissionsAndThrowEvent(Id linkId, Id personId,
			Integer roadType, Double freeVelocity, Double linkLength,
			Double enterTime, Double travelTime, String ageFuelCcm) {

		Map<WarmPollutant, Double> warmEmissions = calculateWarmEmissions(personId, roadType, linkLength, travelTime, ageFuelCcm);
		Event warmEmissionEvent = new WarmEmissionEventImpl(enterTime, linkId, personId, warmEmissions);
		this.eventsManager.processEvent(warmEmissionEvent);
	}

	private Map<WarmPollutant, Double> calculateWarmEmissions(Id personId,
			Integer roadType, Double linkLength, Double travelTime,
			String ageFuelCcm) {

		Map<WarmPollutant, Map<HbefaTrafficSituation, double[]>> relevantInformation;
		
		if (ageFuelCcm != null) {
			relevantInformation = gatherRelevantInformation(roadType, ageFuelCcm);
		} else {// We don't know anything about the vehicle this person is driving, so we don't know how polluting it is.
			relevantInformation = null;
		}
		
		Map<WarmPollutant, Double> warmEmissions;
		if (relevantInformation != null) {
			warmEmissions = calculateDetailedEmissions(relevantInformation,	travelTime, linkLength);
		} else {// We don't know anything about the vehicle this person is driving, so we don't know how polluting it is.
			int hbefaRoadType = this.roadTypeMapping.get(roadType).getHBEFA_RT_NR();
			
			// Non-HDV emissions; TODO: better filter?!?
			if (!personId.toString().contains("gv_")) {
				warmEmissions = calculateAverageEmissions(hbefaRoadType, travelTime, linkLength, this.hbefaAvgWarmEmissionTableCreator.getHbefaWarmTable());
			} else {
				warmEmissions = calculateAverageEmissions(hbefaRoadType, travelTime, linkLength, this.hbefaAvgWarmEmissionTableCreatorHDV.getHbefaWarmTable());
			}
			
			if (vehInfoWarnCnt < maxVehInfoWarnCnt) {
				logger.warn("Vehicle information for person " + personId + " is either non-existing or not valid. Using fleet average values instead.");
				if (vehInfoWarnCnt == maxVehInfoWarnCnt)
					logger.warn(Gbl.FUTURE_SUPPRESSED);
			}
			vehInfoWarnCnt++;
			personIdSet.add(personId);
		}
		return warmEmissions;
	}

	private Map<WarmPollutant, Double> calculateDetailedEmissions(Map<WarmPollutant, Map<HbefaTrafficSituation, double[]>> relevantInformation,	double travelTime, double linkLength) {
			Map<WarmPollutant, Double> emissionsOfEvent = new HashMap<WarmPollutant, Double>();
	
			for (WarmPollutant warmPollutant : relevantInformation.keySet()){
				
				double generatedEmissions;
				double averageSpeed = (linkLength / 1000) / (travelTime / 3600);
	
				Map<HbefaTrafficSituation, double[]> trafficSit2VandEF = relevantInformation.get(warmPollutant);
				double freeFlowSpeed = trafficSit2VandEF.get(HbefaTrafficSituation.FREEFLOW)[0];
				double stopGoSpeed = trafficSit2VandEF.get(HbefaTrafficSituation.STOPANDGO)[0];
				double efFreeFlow = trafficSit2VandEF.get(HbefaTrafficSituation.FREEFLOW)[1];
				double efStopGo = trafficSit2VandEF.get(HbefaTrafficSituation.STOPANDGO)[1];
	
				double freeFlowFraction;
				double stopGoFraction;
				double stopGoTime;
	
				if (averageSpeed < stopGoSpeed) {
					generatedEmissions = linkLength / 1000 * efStopGo;
				} else {
					stopGoTime = (linkLength / 1000) / averageSpeed	- (linkLength / 1000) / freeFlowSpeed;
	
					stopGoFraction = stopGoSpeed * stopGoTime;
					
					freeFlowFraction = (linkLength / 1000) - stopGoFraction;
					
					generatedEmissions = (freeFlowFraction * efFreeFlow) + (stopGoFraction * efStopGo);
				}
				emissionsOfEvent.put(warmPollutant, generatedEmissions);
			}
			return emissionsOfEvent;
		}

	private Map<WarmPollutant, Map<HbefaTrafficSituation, double[]>> gatherRelevantInformation(Integer roadType, String ageFuelCcm) {
		Map<WarmPollutant, Map<HbefaTrafficSituation, double[]>> relevantInformation = new TreeMap<WarmPollutant, Map<HbefaTrafficSituation, double[]>>();

		String[] ageFuelCcmArray = mapVehicleAttributesFromMiD2Hbefa(ageFuelCcm);
		if (ageFuelCcmArray == null) {
			return null;
		}

		for (WarmPollutant warmPollutant : WarmPollutant.values()) {
			Map<HbefaTrafficSituation, double[]> trafficSit2VandEf = new HashMap<HbefaTrafficSituation, double[]>();

			for (HbefaTrafficSituation trafficSit : HbefaTrafficSituation.values()) {
				String key = makeKey(warmPollutant, roadType, ageFuelCcmArray[0], ageFuelCcmArray[1], ageFuelCcmArray[2], trafficSit);
				HbefaWarmEmissionFactorsDetailed hbefaWarmEmissionFactorsDetailed = this.hbefaWarmEmissionTableCreatorDetailed.getHbefaWarmTableDetailed().get(key);
				if (hbefaWarmEmissionFactorsDetailed != null) {
					double[] vAndEf = new double[2];
					vAndEf[0] = hbefaWarmEmissionFactorsDetailed.getV();
					vAndEf[1] = hbefaWarmEmissionFactorsDetailed.getEFA();

					trafficSit2VandEf.put(trafficSit, vAndEf);
				} else {
					logger.warn("For traffic situation " + trafficSit	+ " and pollutant " + warmPollutant + " no vehicle specific emission factor is found."
								+ "\n"
								+ " Continuing calculation with fleet average values instead.");
					return null;
				}
			}
			relevantInformation.put(warmPollutant, trafficSit2VandEf);
		}
		return relevantInformation;
	}

	private String[] mapVehicleAttributesFromMiD2Hbefa(String ageFuelCcm) {
		String[] ageFuelCcmArray = ageFuelCcm.split(";");
		String[] result = new String[3];
	
		int year = splitAndConvert(ageFuelCcmArray[0], ":");
		int fuelType = splitAndConvert(ageFuelCcmArray[1], ":");
		int cubicCap = splitAndConvert(ageFuelCcmArray[2], ":");

		if (year < 1993 && fuelType == 1)
			result[0] = "PC-P-Euro-0";
		else if (year < 1993 && fuelType == 2)
			result[0] = "PC-D-Euro-0";
		else if (year < 1997 && fuelType == 1)
			result[0] = "PC-P-Euro-1";
		else if (year < 1997 && fuelType == 2)
			result[0] = "PC-D-Euro-1";
		else if (year < 2001 && fuelType == 1)
			result[0] = "PC-P-Euro-2";
		else if (year < 2001 && fuelType == 2)
			result[0] = "PC-D-Euro-2";
		else if (year < 2006 && fuelType == 1)
			result[0] = "PC-P-Euro-3";
		else if (year < 2006 && fuelType == 2)
			result[0] = "PC-D-Euro-3";
		else if (year < 2011 && fuelType == 1)
			result[0] = "PC-P-Euro-4";
		else if (year < 2011 && fuelType == 2)
			result[0] = "PC-D-Euro-4";
		else if (year < 2015 && fuelType == 1)
			result[0] = "PC-P-Euro-5";
		else if (year < 2015 && fuelType == 2)
			result[0] = "PC-D-Euro-5";
		else
			return null;

		if (fuelType == 1)
			result[1] = "petrol (4S)";
		else if (fuelType == 2)
			result[1] = "diesel";
		else
			return null;
	
		if (cubicCap <= 1400)
			result[2] = "<1,4L";
		else if (cubicCap <= 2000 && cubicCap > 1400)
			result[2] = "1,4-<2L";
		else if (cubicCap > 2000 && cubicCap < 90000)
			result[2] = ">=2L";
		else
			return null;
	
		return result;
	}

	private String makeKey(WarmPollutant warmPollutant, int roadType, String emConcept, String technology, String sizeClass, HbefaTrafficSituation trafficSit){
		String trafficSituation = this.roadTypeMapping.get(roadType).getTRAFFIC_SITUATION();
		
		return "PC[3.1]"
				+ ";"
				+ "pass. car"
				+ ";"
				+ "2010"
				+ ";"
				+ ";"
				+ warmPollutant.getText()
				+ ";"
				+ ";"
				+ trafficSituation
				+ ";"
				+ "0%"
				+ ";"
				+ technology
				+ ";"
				+ sizeClass
				+ ";"
				+ emConcept
				+ ";"
				;
	}

	// is used in order to split a phrase like baujahr:1900 , we are only interested in 1900 as Integer
	private int splitAndConvert(String string, String splitSign) {
		String[] array = string.split(splitSign);
		return Integer.valueOf(array[1]);
	}

	private Map<WarmPollutant, Double> calculateAverageEmissions(int hbefaRoadType, double travelTime, double linkLength, HbefaWarmEmissionFactors[][] HbefaTable) {
		Map<WarmPollutant, Double> avgEmissionsOfEvent = new HashMap<WarmPollutant, Double>();

		if (hbefaRoadType == 0) {
			for (WarmPollutant warmPollutant : WarmPollutant.values()) {
				avgEmissionsOfEvent.put(warmPollutant, null);
			}
		} else {
			double freeFlowSpeed = HbefaTable[hbefaRoadType][0].getVelocity();
			double stopGoSpeed = HbefaTable[hbefaRoadType][3].getVelocity();
			double averageSpeed = (linkLength / 1000) / (travelTime / 3600);

			for (WarmPollutant warmPollutant : WarmPollutant.values()) {
				Double generatedEmissions;
				Double efFreeFlow = HbefaTable[hbefaRoadType][0].getEf(warmPollutant);
				
				if (averageSpeed < stopGoSpeed) {
					generatedEmissions = linkLength / 1000 * efFreeFlow;
				} else {
					Double stopGoTime = ((linkLength / 1000) / averageSpeed) - ((linkLength / 1000) / freeFlowSpeed);
					Double stopGoFraction = stopGoSpeed * stopGoTime;
					Double freeFlowFraction = (linkLength / 1000) - stopGoFraction;
					Double efStopGo = HbefaTable[hbefaRoadType][3].getEf(warmPollutant);

					generatedEmissions = (freeFlowFraction * efFreeFlow) + (stopGoFraction * efStopGo);
				}
				avgEmissionsOfEvent.put(warmPollutant, generatedEmissions);
			}
		}
		return avgEmissionsOfEvent;
	}

	public static int getVehInfoWarnCnt() {
		return vehInfoWarnCnt;
	}

	public static Set<Id> getPersonIdSet() {
		return personIdSet;
	}
}