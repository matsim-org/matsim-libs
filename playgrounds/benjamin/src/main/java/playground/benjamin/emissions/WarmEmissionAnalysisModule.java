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
import playground.benjamin.emissions.dataTypes.VisumRoadTypes;
import playground.benjamin.emissions.events.WarmEmissionEventImpl;
import playground.benjamin.emissions.events.WarmPollutant;

public class WarmEmissionAnalysisModule {
	private static final Logger logger = Logger.getLogger(WarmEmissionAnalysisModule.class);

	private final VisumRoadTypes[] roadTypes;
	private final String[][] roadTypesTrafficSituations;

	private final HbefaWarmEmissionTableCreatorDetailed hbefaWarmEmissionTableCreatorDetailed;
	private final HbefaWarmEmissionTableCreator hbefaAvgWarmEmissionTableCreator;
	private final HbefaWarmEmissionTableCreator hbefaAvgWarmEmissionTableCreatorHDV;

	private final EventsManager eventsManager;
	private static int vehInfoWarnCnt = 0;
	private static int maxVehInfoWarnCnt = 3;
	private static Set<Id> personIdSet = new HashSet<Id>();

	public WarmEmissionAnalysisModule(
			VisumRoadTypes[] roadTypes,
			String[][] roadTypesTrafficSituations,
			HbefaWarmEmissionTableCreatorDetailed hbefaWarmEmissionTableCreatorDetailed,
			HbefaWarmEmissionTableCreator hbefaAvgWarmEmissionTableCreator,
			HbefaWarmEmissionTableCreator hbefaAvgWarmEmissionTableCreatorHDV,
			EventsManager eventsManager) {
		this.roadTypes = roadTypes;
		this.roadTypesTrafficSituations = roadTypesTrafficSituations;
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

		// TODO: use freeVelocity, not hbefa value!

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
			
			if (vehInfoWarnCnt < maxVehInfoWarnCnt) {
				logger.warn("Vehicle information for person " + personId + " is either non-existing or not valid. Using fleet average values instead.");
				if (vehInfoWarnCnt == maxVehInfoWarnCnt)
					logger.warn(Gbl.FUTURE_SUPPRESSED);
			}

			// "linkage between Hbefa road types and Visum road types" -- WHY another mapping here?!?
			int hbefaRoadType = Integer.valueOf(this.roadTypes[roadType].getHBEFA_RT_NR());

			if (!personId.toString().contains("gv_")) {// Non-HDV emissions; TODO: better filter?!?
				warmEmissions = calculateAverageEmissions(hbefaRoadType, travelTime, linkLength, this.hbefaAvgWarmEmissionTableCreator.getHbefaWarmTable());
			} else {
				warmEmissions = calculateAverageEmissions(hbefaRoadType, travelTime, linkLength, this.hbefaAvgWarmEmissionTableCreatorHDV.getHbefaWarmTable());
			}
			vehInfoWarnCnt++;
			personIdSet.add(personId);
		}
		return warmEmissions;
	}

	private Map<WarmPollutant, Double> calculateDetailedEmissions(Map<WarmPollutant, Map<HbefaTrafficSituation, double[]>> relevantInformation,	double travelTime, double linkLength) {
			Map<WarmPollutant, Double> emissionsOfEvent = new HashMap<WarmPollutant, Double>();
	
			for (WarmPollutant warmPollutant : relevantInformation.keySet()){
				
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
					double generatedEmissions = linkLength / 1000 * efStopGo;
					emissionsOfEvent.put(warmPollutant, generatedEmissions);
				} else {
					stopGoTime = (linkLength / 1000) / averageSpeed
							- (linkLength / 1000) / freeFlowSpeed;
	
					stopGoFraction = stopGoSpeed * stopGoTime;
					freeFlowFraction = (linkLength / 1000) - stopGoFraction;
					double generatedEmissions = (freeFlowFraction * efFreeFlow)
							+ (stopGoFraction * efStopGo);
					emissionsOfEvent.put(warmPollutant, generatedEmissions);
				}
			}
			return emissionsOfEvent;
		}

	private Map<WarmPollutant, Map<HbefaTrafficSituation, double[]>> gatherRelevantInformation(Integer roadType, String ageFuelCcm) {
		Map<WarmPollutant, Map<HbefaTrafficSituation, double[]>> relevantInformation = new TreeMap<WarmPollutant, Map<HbefaTrafficSituation, double[]>>();

		String[] ageFuelCcmArray = ageFuelCcm.split(";");

		String[] fuelCcmEuro = mapVehicleAttributesFromMiD2Hbefa(ageFuelCcmArray);
		if (fuelCcmEuro == null) {
			return null;
		}

		for (WarmPollutant warmPollutant : WarmPollutant.values()) {
			Map<HbefaTrafficSituation, double[]> trafficSit2VandEf = new HashMap<HbefaTrafficSituation, double[]>();

			for (HbefaTrafficSituation trafficSit : HbefaTrafficSituation.values()) {
				String key = makeKey(warmPollutant, roadType, fuelCcmEuro[0], fuelCcmEuro[1], fuelCcmEuro[2], trafficSit);
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

	private String[] mapVehicleAttributesFromMiD2Hbefa(String[] ageFuelCcmArray) {
		String[] fuelCcmEuro = new String[3];
	
		int fuelType = splitAndConvert(ageFuelCcmArray[1], ":");
		if (fuelType == 1)
			fuelCcmEuro[0] = "petrol (4S)";
		else if (fuelType == 2)
			fuelCcmEuro[0] = "diesel";
		else
			return null;
	
		int cubicCap = splitAndConvert(ageFuelCcmArray[2], ":");
		if (cubicCap <= 1400)
			fuelCcmEuro[1] = "<1,4L";
		else if (cubicCap <= 2000 && cubicCap > 1400)
			fuelCcmEuro[1] = "1,4-<2L";
		else if (cubicCap > 2000 && cubicCap < 90000)
			fuelCcmEuro[1] = ">=2L";
		else
			return null;
	
		int year = splitAndConvert(ageFuelCcmArray[0], ":");
		if (year < 1993 && fuelCcmEuro[0].equals("petrol (4S)"))
			fuelCcmEuro[2] = "PC-P-Euro-0";
		else if (year < 1993 && fuelCcmEuro[0].equals("diesel"))
			fuelCcmEuro[2] = "PC-D-Euro-0";
		else if (year < 1997 && fuelCcmEuro[0].equals("petrol (4S)"))
			fuelCcmEuro[2] = "PC-P-Euro-1";
		else if (year < 1997 && fuelCcmEuro[0].equals("diesel"))
			fuelCcmEuro[2] = "PC-D-Euro-1";
		else if (year < 2001 && fuelCcmEuro[0].equals("petrol (4S)"))
			fuelCcmEuro[2] = "PC-P-Euro-2";
		else if (year < 2001 && fuelCcmEuro[0].equals("diesel"))
			fuelCcmEuro[2] = "PC-D-Euro-2";
		else if (year < 2006 && fuelCcmEuro[0].equals("petrol (4S)"))
			fuelCcmEuro[2] = "PC-P-Euro-3";
		else if (year < 2006 && fuelCcmEuro[0].equals("diesel"))
			fuelCcmEuro[2] = "PC-D-Euro-3";
		else if (year < 2011 && fuelCcmEuro[0].equals("petrol (4S)"))
			fuelCcmEuro[2] = "PC-P-Euro-4";
		else if (year < 2011 && fuelCcmEuro[0].equals("diesel"))
			fuelCcmEuro[2] = "PC-D-Euro-4";
		else if (year < 2015 && fuelCcmEuro[0].equals("petrol (4S)"))
			fuelCcmEuro[2] = "PC-P-Euro-5";
		else if (year < 2015 && fuelCcmEuro[0].equals("diesel"))
			fuelCcmEuro[2] = "PC-D-Euro-5";
		else
			return null;
	
		return fuelCcmEuro;
	}

	private String makeKey(WarmPollutant warmPollutant, int roadType, String technology, String sizeClass, String emConcept, HbefaTrafficSituation trafficSit){
		int trafficSitNumber = trafficSit.getNumber();
		
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
				+ this.roadTypesTrafficSituations[roadType][trafficSitNumber]
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

	// is used in order to split a phrase like baujahr:1900 , we are only
	// interested in 1900 as Integer
	private int splitAndConvert(String string, String splittZeichen) {
		String[] array = string.split(splittZeichen);
		return Integer.valueOf(array[1]);
	}

	private Map<WarmPollutant, Double> calculateAverageEmissions(int hbefa_road_type, double travelTime, double linkLength, HbefaWarmEmissionFactors[][] HbefaTable) {
		Map<WarmPollutant, Double> avgEmissionsOfEvent = new HashMap<WarmPollutant, Double>();

		// TODO: Why can road type be 0 here?
		if (hbefa_road_type == 0) {
			for (WarmPollutant warmPollutant : WarmPollutant.values()) {
				avgEmissionsOfEvent.put(warmPollutant, 0.0);
			}
		} else {
			double freeFlowSpeed = HbefaTable[hbefa_road_type][0].getVelocity();
			double stopGoSpeed = HbefaTable[hbefa_road_type][3].getVelocity();
			double averageSpeed = (linkLength / 1000) / (travelTime / 3600);

			for (WarmPollutant warmPollutant : WarmPollutant.values()) {
				Double generatedEmissions;
				Double efFreeFlow = HbefaTable[hbefa_road_type][0]
						.getEf(warmPollutant);
				if (averageSpeed < stopGoSpeed) {
					generatedEmissions = linkLength / 1000 * efFreeFlow;
					avgEmissionsOfEvent.put(warmPollutant, generatedEmissions);
				} else {
					Double stopGoTime = ((linkLength / 1000) / averageSpeed)
							- ((linkLength / 1000) / freeFlowSpeed);
					Double stopGoFraction = stopGoSpeed * stopGoTime;
					Double freeFlowFraction = (linkLength / 1000)
							- stopGoFraction;
					Double efStopGo = HbefaTable[hbefa_road_type][3]
							.getEf(warmPollutant);

					generatedEmissions = (freeFlowFraction * efFreeFlow)
							+ (stopGoFraction * efStopGo);
					avgEmissionsOfEvent.put(warmPollutant, generatedEmissions);
				}
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