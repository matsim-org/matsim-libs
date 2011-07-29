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

import java.util.ArrayList;
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
import playground.fhuelsmann.emission.objects.HbefaObject;
import playground.fhuelsmann.emission.objects.HotValue;
import playground.fhuelsmann.emission.objects.VisumObject;

public class WarmEmissionAnalysisModule{
	private static final Logger logger = Logger.getLogger(WarmEmissionAnalysisModule.class);

	private final VisumObject[] roadTypes;
	private final String[][] roadTypesTrafficSituations;

	private final HbefaHot hbefaHot;
	private HbefaTable hbefaTable;

	private HbefaTable hbefaHdvTable;

	private EventsManager eventsManager;
	private static int vehInfoWarnCnt = 0;
	private static int maxVehInfoWarnCnt = 10;

	public WarmEmissionAnalysisModule(VisumObject[] roadTypes, String[][] roadTypesTrafficSituations, HbefaHot hbefahot, HbefaTable hbefaTable, HbefaTable hbefaHdvTable, EventsManager eventsManager){
		this.roadTypes = roadTypes;
		this.roadTypesTrafficSituations = roadTypesTrafficSituations;
		this.hbefaHot = hbefahot;
		this.hbefaTable = hbefaTable;
		this.hbefaHdvTable = hbefaHdvTable;
		this.eventsManager = eventsManager;
	}

	public void calculateWarmEmissionsAndThrowEvent(Id linkId, Id personId,
			Integer roadType, Double freeVelocity, Double linkLength,
			Double enterTime, Double travelTime, String ageFuelCcm) {

		Map<Pollutant, Double> warmEmissions = calculateWarmEmissions(personId, roadType, linkLength, travelTime, ageFuelCcm);
		Map<String, Double> warmEmissionStrings = new HashMap<String, Double>();
		for (Entry<Pollutant, Double> entry : warmEmissions.entrySet()) {
			warmEmissionStrings.put(entry.getKey().getText(), entry.getValue());
		}
		Event warmEmissionEvent = new WarmEmissionEventImpl(enterTime, linkId, personId, warmEmissionStrings);
		this.eventsManager.processEvent(warmEmissionEvent);
	}

	private Map<Pollutant, Double> calculateWarmEmissions(Id personId,
			Integer roadType, Double linkLength, Double travelTime,
			String ageFuelCcm) {
		// TODO: use freeVelocity, not hbefa value!

		Map<Pollutant, double[][]> hashOfPollutant = findEmissions(roadType, ageFuelCcm);

		Map<Pollutant, Double> warmEmissions;
		if(hashOfPollutant != null){
			warmEmissions = calculateDetailedEmissions(hashOfPollutant, travelTime, linkLength);
		} else {
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
				warmEmissions = calculateAverageEmissions(hbefaRoadType, travelTime, linkLength, hbefaTable.getHbefaTableWithSpeedAndEmissionFactor());
			}
			else{// HDV emissions; TODO: "only for CO2 and FC are values available, otherwise 0.0", "so far only fc and co2 emissionFactors are listed in the hbefaHdvTable" --- WHAT?!?
				warmEmissions = calculateAverageEmissions(hbefaRoadType, travelTime, linkLength, hbefaHdvTable.getHbefaTableWithSpeedAndEmissionFactor());
			}
		}
		return warmEmissions;
	}

	private String findHbefaFromVisumRoadType(int roadType){
		return this.roadTypes[roadType].getHBEFA_RT_NR();
	}

	private Map<Pollutant, double[][]> findEmissions(Integer roadType, String ageFuelCcm) {
		Map<Pollutant, double[][]> hashOfPollutant = new TreeMap<Pollutant, double[][]>();

		String[] ageFuelCcmArray = ageFuelCcm.split(";");

		String[] fuelCcmEuro = mapVehicleAttributesFromMiD2Hbefa(ageFuelCcmArray);
		if (fuelCcmEuro == null) {
			return null;
		}
		
		for(Pollutant pollutant : Pollutant.values()) {
			double[][] emissionsInFourSituations = new double[4][2];
			for(int i = 0; i < 4; i++){// [0] for freeFlow, [1] for heavy, [2] for saturated, [3] for Stop&Go
				String key = makeKey(pollutant, roadType, fuelCcmEuro[0], fuelCcmEuro[1], fuelCcmEuro[2], i);
				HotValue hotValue = this.hbefaHot.getHbefaHot().get(key);
				if (hotValue != null) {
					emissionsInFourSituations[i][0] = hotValue.getV();
					emissionsInFourSituations[i][1] = hotValue.getEFA();
				} else {
					return null;
				}
			}
			// in hashOfPollutant we save V and EFA for 4 traffic situations
			hashOfPollutant.put(pollutant, emissionsInFourSituations);
		}
		return hashOfPollutant;
	}

	private Map<Pollutant, Double> calculateDetailedEmissions(Map<Pollutant, double[][]> hashOfPollutant, double travelTime, double linkLength){
		Map<Pollutant, Double> emissionsOfEvent = new HashMap<Pollutant, Double>();

		for( Entry<Pollutant, double[][]> entry : hashOfPollutant.entrySet() ){

			Pollutant pollutant = entry.getKey();
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
				emissionsOfEvent.put(pollutant, generatedEmissions);
			}
			else {
				stopGoTime= (linkLength / 1000) / averageSpeed - (linkLength / 1000) / freeFlowSpeed;

				stopGoFraction = stopGoSpeed * stopGoTime;
				freeFlowFraction = (linkLength / 1000) - stopGoFraction;
				double generatedEmissions = (freeFlowFraction * efFreeFlow) + (stopGoFraction * efStopGo);
				emissionsOfEvent.put(pollutant, generatedEmissions);
			}
		}
		return emissionsOfEvent;
	}

	private Map<Pollutant, Double> calculateAverageEmissions(int hbefa_road_type, double travelTime, double linkLength, HbefaObject[][] HbefaTable) {
		Map<Pollutant, Double> avgEmissionsOfEvent = new HashMap<Pollutant, Double>();

		// TODO: Why can road type be 0 here?
		if (hbefa_road_type == 0) {
			for(Pollutant pollutant : Pollutant.values())	{
				avgEmissionsOfEvent.put(pollutant, 0.0);
			}
		}
		else {	
			double freeFlowSpeed = HbefaTable[hbefa_road_type][0].getVelocity();
			double stopGoSpeed = HbefaTable[hbefa_road_type][3].getVelocity();
			double averageSpeed = (linkLength / 1000) / (travelTime / 3600);

			for(Pollutant pollutant : Pollutant.values()) {
				Double generatedEmissions;
				Double efFreeFlow = HbefaTable[hbefa_road_type][0].getEf(pollutant);
				if(averageSpeed < stopGoSpeed){
					generatedEmissions = linkLength / 1000 * efFreeFlow;
					avgEmissionsOfEvent.put(pollutant, generatedEmissions);
				}
				else{
					Double stopGoTime = ((linkLength / 1000) / averageSpeed) - ((linkLength / 1000) / freeFlowSpeed);
					Double stopGoFraction = stopGoSpeed * stopGoTime;
					Double freeFlowFraction = (linkLength / 1000) - stopGoFraction;
					Double efStopGo = HbefaTable[hbefa_road_type][3].getEf(pollutant);

					generatedEmissions = (freeFlowFraction * efFreeFlow) + (stopGoFraction * efStopGo);
					avgEmissionsOfEvent.put(pollutant, generatedEmissions);
				}
			}
		}
		return avgEmissionsOfEvent;
	}

	private String makeKey(Pollutant pollutant, int roadType, String technology, String Sizeclass, String EmConcept, int traficSitNumber){
		return "PC[3.1]" + ";" + "pass. car" + ";" + "2010" + ";" + ";" + pollutant + ";" + ";" + this.roadTypesTrafficSituations[roadType][traficSitNumber]
		                                                                                                                                    + ";" + "0%" + ";" + technology + ";" + Sizeclass + ";" + EmConcept + ";";
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