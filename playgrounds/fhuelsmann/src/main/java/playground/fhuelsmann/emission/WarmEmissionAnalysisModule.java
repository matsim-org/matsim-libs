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

package playground.fhuelsmann.emission;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;

import playground.benjamin.events.WarmEmissionEventImpl;
import playground.fhuelsmann.emission.objects.HbefaWarmEmissionFactors;
import playground.fhuelsmann.emission.objects.HbefaWarmEmissionFactorsDetailed;
import playground.fhuelsmann.emission.objects.HbefaWarmEmissionTableCreator;
import playground.fhuelsmann.emission.objects.HbefaWarmEmissionTableCreatorDetailed;
import playground.fhuelsmann.emission.objects.VisumRoadTypes;
import playground.fhuelsmann.emission.objects.WarmPollutant;

public class WarmEmissionAnalysisModule{
	private static final Logger logger = Logger.getLogger(WarmEmissionAnalysisModule.class);

	private final VisumRoadTypes[] roadTypes;
	private final String[][] roadTypesTrafficSituations;

	private final HbefaWarmEmissionTableCreatorDetailed hbefaWarmEmissionTableCreatorDetailed;
	private final HbefaWarmEmissionTableCreator hbefaAvgWarmEmissionTableCreator;
	private final HbefaWarmEmissionTableCreator hbefaAvgWarmEmissionTableCreatorHDV;

	private final EventsManager eventsManager;
	private static int vehInfoWarnCnt = 0;
	private static int maxVehInfoWarnCnt = 10;

	public WarmEmissionAnalysisModule(VisumRoadTypes[] roadTypes,
			String[][] roadTypesTrafficSituations,
			HbefaWarmEmissionTableCreatorDetailed hbefaWarmEmissionTableCreatorDetailed,
			HbefaWarmEmissionTableCreator hbefaAvgWarmEmissionTableCreator,
			HbefaWarmEmissionTableCreator hbefaAvgWarmEmissionTableCreatorHDV,
			EventsManager eventsManager){
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
		Map<String, Double> warmEmissionStrings = new HashMap<String, Double>();
		for (Entry<WarmPollutant, Double> entry : warmEmissions.entrySet()) {
			warmEmissionStrings.put(entry.getKey().getText(), entry.getValue());
		}
		Event warmEmissionEvent = new WarmEmissionEventImpl(enterTime, linkId, personId, warmEmissionStrings);
		this.eventsManager.processEvent(warmEmissionEvent);
	}

	private Map<WarmPollutant, Double> calculateWarmEmissions(Id personId,
			Integer roadType, Double linkLength, Double travelTime,
			String ageFuelCcm) {
		// TODO: use freeVelocity, not hbefa value!
		Map<WarmPollutant, double[][]> hashOfPollutant;
		if(ageFuelCcm != null) {
			hashOfPollutant = findEmissions(roadType, ageFuelCcm);
		} else {
			hashOfPollutant = null;
			// We don't know anything about the vehicle this person is driving, so we don't know how polluting it is.
		}

		Map<WarmPollutant, Double> warmEmissions;
		if(hashOfPollutant != null){
			warmEmissions = calculateDetailedEmissions(hashOfPollutant, travelTime, linkLength);
			logger.info(warmEmissions);
		} else {
			// We don't know how polluting this person's vehicle is, so we calculate its emissions based on averages.
			if(vehInfoWarnCnt < maxVehInfoWarnCnt){
				vehInfoWarnCnt++;
				logger.warn("Vehicle information for person " + personId + " is either non-existing or not valid. Using fleet average values instead.");
				if (vehInfoWarnCnt == maxVehInfoWarnCnt)
					logger.warn(Gbl.FUTURE_SUPPRESSED);
			}
			// "linkage between Hbefa road types and Visum road types" -- WHY another mapping here?!?
			int hbefaRoadType = Integer.valueOf(findHbefaFromVisumRoadType(roadType));

			// TODO: use freeVelocity, not hbefa value!
			if(!personId.toString().contains("gv_")){// Non-HDV emissions; TODO: better filter?!?
				warmEmissions = calculateAverageEmissions(hbefaRoadType, travelTime, linkLength, this.hbefaAvgWarmEmissionTableCreator.getHbefaWarmEmissionTable());
			}
			else{
				warmEmissions = calculateAverageEmissions(hbefaRoadType, travelTime, linkLength, this.hbefaAvgWarmEmissionTableCreatorHDV.getHbefaWarmEmissionTable());
			}
		}
		return warmEmissions;
	}

	private String findHbefaFromVisumRoadType(int roadType){
		return this.roadTypes[roadType].getHBEFA_RT_NR();
	}

	private Map<WarmPollutant, double[][]> findEmissions(Integer roadType, String ageFuelCcm) {
		Map<WarmPollutant, double[][]> hashOfPollutant = new TreeMap<WarmPollutant, double[][]>();

		String[] ageFuelCcmArray = ageFuelCcm.split(";");

		String[] fuelCcmEuro = mapVehicleAttributesFromMiD2Hbefa(ageFuelCcmArray);
		if (fuelCcmEuro == null) {
			return null;
		}
		
		for(WarmPollutant warmPollutant : WarmPollutant.values()) {
			double[][] emissionsInFourSituations = new double[4][2];
			for(int i = 0; i < 4; i++){// [0] for freeFlow, [1] for heavy, [2] for saturated, [3] for Stop&Go
				String key = makeKey(warmPollutant, roadType, fuelCcmEuro[0], fuelCcmEuro[1], fuelCcmEuro[2], i);
				HbefaWarmEmissionFactorsDetailed hbefaWarmEmissionFactorsDetailed = this.hbefaWarmEmissionTableCreatorDetailed.getHbefaHot().get(key);
				if (hbefaWarmEmissionFactorsDetailed != null) {
					emissionsInFourSituations[i][0] = hbefaWarmEmissionFactorsDetailed.getV();
					emissionsInFourSituations[i][1] = hbefaWarmEmissionFactorsDetailed.getEFA();
				} else {
					return null;
				}
			}
			// in hashOfPollutant we save V and EFA for 4 traffic situations
			hashOfPollutant.put(warmPollutant, emissionsInFourSituations);
		}
		return hashOfPollutant;
	}

	private Map<WarmPollutant, Double> calculateDetailedEmissions(Map<WarmPollutant, double[][]> hashOfPollutant, double travelTime, double linkLength){
		Map<WarmPollutant, Double> emissionsOfEvent = new HashMap<WarmPollutant, Double>();

		for( Entry<WarmPollutant, double[][]> entry : hashOfPollutant.entrySet() ){

			WarmPollutant warmPollutant = entry.getKey();
			double averageSpeed = (linkLength / 1000) / (travelTime / 3600);

			double freeFlowSpeed = entry.getValue()[0][0]; // freeFlow
			double stopGoSpeed = entry.getValue()[3][0]; // Stop&Go

			double efFreeFlow = entry.getValue()[0][1];
			double efStopGo = entry.getValue()[3][1];

			double freeFlowFraction = 0.0;
			double stopGoFraction = 0.0;
			double stopGoTime = 0.0;

			if (averageSpeed < stopGoSpeed){
				double generatedEmissions = linkLength / 1000 * efStopGo;
				emissionsOfEvent.put(warmPollutant, generatedEmissions);
			}
			else {
				stopGoTime= (linkLength / 1000) / averageSpeed - (linkLength / 1000) / freeFlowSpeed;

				stopGoFraction = stopGoSpeed * stopGoTime;
				freeFlowFraction = (linkLength / 1000) - stopGoFraction;
				double generatedEmissions = (freeFlowFraction * efFreeFlow) + (stopGoFraction * efStopGo);
				emissionsOfEvent.put(warmPollutant, generatedEmissions);
			}
		}
		return emissionsOfEvent;
	}

	private Map<WarmPollutant, Double> calculateAverageEmissions(int hbefa_road_type, double travelTime, double linkLength, HbefaWarmEmissionFactors[][] HbefaTable) {
		Map<WarmPollutant, Double> avgEmissionsOfEvent = new HashMap<WarmPollutant, Double>();

		// TODO: Why can road type be 0 here?
		if (hbefa_road_type == 0) {
			for(WarmPollutant warmPollutant : WarmPollutant.values())	{
				avgEmissionsOfEvent.put(warmPollutant, 0.0);
			}
		}
		else {	
			double freeFlowSpeed = HbefaTable[hbefa_road_type][0].getVelocity();
			double stopGoSpeed = HbefaTable[hbefa_road_type][3].getVelocity();
			double averageSpeed = (linkLength / 1000) / (travelTime / 3600);

			for(WarmPollutant warmPollutant : WarmPollutant.values()) {
				Double generatedEmissions;
				Double efFreeFlow = HbefaTable[hbefa_road_type][0].getEf(warmPollutant);
				if(averageSpeed < stopGoSpeed){
					generatedEmissions = linkLength / 1000 * efFreeFlow;
					avgEmissionsOfEvent.put(warmPollutant, generatedEmissions);
				}
				else{
					Double stopGoTime = ((linkLength / 1000) / averageSpeed) - ((linkLength / 1000) / freeFlowSpeed);
					Double stopGoFraction = stopGoSpeed * stopGoTime;
					Double freeFlowFraction = (linkLength / 1000) - stopGoFraction;
					Double efStopGo = HbefaTable[hbefa_road_type][3].getEf(warmPollutant);

					generatedEmissions = (freeFlowFraction * efFreeFlow) + (stopGoFraction * efStopGo);
					avgEmissionsOfEvent.put(warmPollutant, generatedEmissions);
				}
			}
		}
		return avgEmissionsOfEvent;
	}

	private String makeKey(WarmPollutant warmPollutant, int roadType, String technology, String sizeClass, String emConcept, int traficSitNumber){
		return "PC[3.1]" + ";"
		+ "pass. car" + ";"
		+ "2010" + ";"
		+ ";"
		+ warmPollutant + ";"
		+ ";"
		+ this.roadTypesTrafficSituations[roadType][traficSitNumber] + ";"
		+ "0%" + ";"
		+ technology + ";"
		+ sizeClass + ";"
		+ emConcept + ";";
	}

	private String[] mapVehicleAttributesFromMiD2Hbefa(String[] ageFuelCcmArray){
		String[] fuelCcmEuro = new String[3];

		int fuelType = splitAndConvert(ageFuelCcmArray[1], ":");
		if(fuelType == 1) fuelCcmEuro[0] = "petrol (4S)";
		else if(fuelType == 2) fuelCcmEuro[0] = "diesel";
		else return null;

		int cubicCap = splitAndConvert(ageFuelCcmArray[2], ":");
		if (cubicCap <= 1400) fuelCcmEuro[1] = "<1,4L";	
		else if(cubicCap <= 2000 && cubicCap > 1400) fuelCcmEuro[1] = "1,4-<2L";
		else if(cubicCap > 2000 && cubicCap < 90000) fuelCcmEuro[1] = ">=2L";
		else return null;

		int year = splitAndConvert(ageFuelCcmArray[0], ":");
		if (year < 1993 && fuelCcmEuro[0].equals("petrol (4S)")) fuelCcmEuro[2] = "PC-P-Euro-0";
		else if (year < 1993 && fuelCcmEuro[0].equals("diesel")) fuelCcmEuro[2] = "PC-D-Euro-0";
		else if(year < 1997 && fuelCcmEuro[0].equals("petrol (4S)")) fuelCcmEuro[2] = "PC-P-Euro-1";
		else if(year < 1997 && fuelCcmEuro[0].equals("diesel")) fuelCcmEuro[2] = "PC-D-Euro-1";
		else if(year < 2001 && fuelCcmEuro[0].equals("petrol (4S)") ) fuelCcmEuro[2] = "PC-P-Euro-2";
		else if(year < 2001 && fuelCcmEuro[0].equals("diesel") ) fuelCcmEuro[2] = "PC-D-Euro-2";
		else if(year < 2006 && fuelCcmEuro[0].equals("petrol (4S)")) fuelCcmEuro[2] = "PC-P-Euro-3";
		else if(year < 2006 && fuelCcmEuro[0].equals("diesel")) fuelCcmEuro[2] = "PC-D-Euro-3";
		else if(year < 2011 && fuelCcmEuro[0].equals("petrol (4S)") ) fuelCcmEuro[2] = "PC-P-Euro-4";
		else if(year < 2011 && fuelCcmEuro[0].equals("diesel") ) fuelCcmEuro[2] = "PC-D-Euro-4";
		else if(year < 2015 && fuelCcmEuro[0].equals("petrol (4S)") ) fuelCcmEuro[2] = "PC-P-Euro-5";
		else if(year < 2015 && fuelCcmEuro[0].equals("diesel") ) fuelCcmEuro[2] = "PC-D-Euro-5";
		else return null;

		return fuelCcmEuro;
	}

	// is used in order to split a phrase like baujahr:1900 , we are only interested in 1900 as Integer
	private int splitAndConvert(String string, String splittZeichen){
	
		String[] array = string.split(splittZeichen);
		return Integer.valueOf(array[1]);
	}
}