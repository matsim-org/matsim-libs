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

import java.math.BigDecimal;
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

	final Map<Integer, String> roadTypeMapping;

	final Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable;
	final Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable;

	final EventsManager eventsManager;

	int vehAttributesNotSpecifiedCnt = 0;
	final int maxWarnCnt = 3;
	Set<Id> vehAttributesNotSpecified = new HashSet<Id>();
	Set<Id> vehicleIdSet = new HashSet<Id>();

	int freeFlowCounter = 0;
	int stopGoCounter = 0;
	int fractionCounter = 0;
	int emissionEventCounter = 0;
	
	double kmCounter = 0.0;
	double freeFlowKmCounter = 0.0;
	double stopGoKmCounter = 0.0;

	public static class WarmEmissionAnalysisModuleParameter {

		public Map<Integer, String> roadTypeMapping;
		public Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable;
		public Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable;

		public WarmEmissionAnalysisModuleParameter(
				Map<Integer, String> roadTypeMapping,
				Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable,
				Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable) {
			this.roadTypeMapping = roadTypeMapping;
			this.avgHbefaWarmTable = avgHbefaWarmTable;
			this.detailedHbefaWarmTable = detailedHbefaWarmTable;
		}
	}

	public WarmEmissionAnalysisModule(
			WarmEmissionAnalysisModuleParameter parameterObject,
			EventsManager emissionEventsManager) {

		this.roadTypeMapping = parameterObject.roadTypeMapping;
		this.avgHbefaWarmTable = parameterObject.avgHbefaWarmTable;
		this.detailedHbefaWarmTable = parameterObject.detailedHbefaWarmTable;
		this.eventsManager = emissionEventsManager;
	}

	public void reset() {
		logger.info("resetting counters...");
		vehAttributesNotSpecifiedCnt = 0;
		vehAttributesNotSpecified.clear();
		vehicleIdSet.clear();
	
		freeFlowCounter = 0;
		stopGoCounter = 0;
		fractionCounter = 0;
		emissionEventCounter = 0;
		
		kmCounter = 0.0;
		freeFlowKmCounter = 0.0;
		stopGoKmCounter = 0.0;
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
		warmEmissions = calculateWarmEmissions(personId, travelTime, roadType, freeVelocity, linkLength, vehicleInformationTuple);
		Event warmEmissionEvent = new WarmEmissionEventImpl(enterTime, linkId, personId, warmEmissions);
		this.eventsManager.processEvent(warmEmissionEvent);
	}

	private Map<WarmPollutant, Double> calculateWarmEmissions(
			Id personId,
			Double travelTime,
			Integer roadType,
			Double freeVelocity,
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

		double stopGoSpeed_kmh;
		double efFreeFlow_gpkm;
		double efStopGo_gpkm;

		for (WarmPollutant warmPollutant : WarmPollutant.values()) {
			double generatedEmissions;

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
					stopGoSpeed_kmh = this.detailedHbefaWarmTable.get(keyStopAndGo).getSpeed();
					efFreeFlow_gpkm = this.detailedHbefaWarmTable.get(keyFreeFlow).getWarmEmissionFactor();
					efStopGo_gpkm = this.detailedHbefaWarmTable.get(keyStopAndGo).getWarmEmissionFactor();

				} else {
					stopGoSpeed_kmh = this.avgHbefaWarmTable.get(keyStopAndGo).getSpeed();
					efFreeFlow_gpkm = this.avgHbefaWarmTable.get(keyFreeFlow).getWarmEmissionFactor();
					efStopGo_gpkm = this.avgHbefaWarmTable.get(keyStopAndGo).getWarmEmissionFactor();

					if(vehAttributesNotSpecifiedCnt < maxWarnCnt) {
						vehAttributesNotSpecifiedCnt++;
						logger.warn("Detailed vehicle attributes are not specified correctly for person " + personId + ": " + 
								"`" + vehicleInformationTuple.getSecond() + "'. Using fleet average values instead.");
						if(vehAttributesNotSpecifiedCnt == maxWarnCnt) logger.warn(Gbl.FUTURE_SUPPRESSED);
					}
					vehAttributesNotSpecified.add(personId);
				}
			} else {
				stopGoSpeed_kmh = this.avgHbefaWarmTable.get(keyStopAndGo).getSpeed();
				efFreeFlow_gpkm = this.avgHbefaWarmTable.get(keyFreeFlow).getWarmEmissionFactor();
				efStopGo_gpkm = this.avgHbefaWarmTable.get(keyStopAndGo).getWarmEmissionFactor();

				vehAttributesNotSpecified.add(personId);
			}

			double linkLength_km = linkLength / 1000;
			double travelTime_h = travelTime / 3600;
			int freeFlowSpeed_kmh = (int) Math.round(freeVelocity * 3.6);
			int averageSpeed_kmh = (int) Math.round(linkLength_km / travelTime_h);
			
			if (averageSpeed_kmh > freeFlowSpeed_kmh){
				logger.info("averageSpeed_kmh: " + averageSpeed_kmh + "; freeFlowSpeed_kmh: " + freeFlowSpeed_kmh);
				throw new RuntimeException("Average speed was higher than free flow speed; this would produce negative warm emissions. Aborting...");
			}
			if(averageSpeed_kmh == freeFlowSpeed_kmh) {
				generatedEmissions = linkLength_km * efFreeFlow_gpkm;
				freeFlowCounter++;
				freeFlowKmCounter = freeFlowKmCounter + linkLength_km;
			} else if (averageSpeed_kmh <= stopGoSpeed_kmh) {
				generatedEmissions = linkLength_km * efStopGo_gpkm;
				stopGoCounter++;
				stopGoKmCounter = stopGoKmCounter + linkLength_km;
			} else {
				double distanceStopGo_km = (linkLength_km * stopGoSpeed_kmh * (freeFlowSpeed_kmh - averageSpeed_kmh)) / (averageSpeed_kmh * (freeFlowSpeed_kmh - stopGoSpeed_kmh));
				double distanceFreeFlow_km = linkLength_km - distanceStopGo_km;

				generatedEmissions = (distanceFreeFlow_km * efFreeFlow_gpkm) + (distanceStopGo_km * efStopGo_gpkm);
				fractionCounter++;
				stopGoKmCounter = stopGoKmCounter + distanceStopGo_km;
				freeFlowKmCounter = freeFlowKmCounter + distanceFreeFlow_km;
			}
			kmCounter = kmCounter + linkLength_km;
			warmEmissionsOfEvent.put(warmPollutant, generatedEmissions);
		}
		emissionEventCounter++;
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

	public Set<Id> getVehAttributesNotSpecified() {
		return vehAttributesNotSpecified;
	}

	public Set<Id> getVehicleIdSet() {
		return vehicleIdSet;
	}

	public int getFreeFlowOccurences() {
		return freeFlowCounter / WarmPollutant.values().length;
	}

	public int getFractionOccurences() {
		return fractionCounter / WarmPollutant.values().length;
	}
	
	public int getStopGoOccurences() {
		return stopGoCounter / WarmPollutant.values().length;
	}

	public double getKmCounter() {
		return roundDouble((kmCounter / WarmPollutant.values().length), 3);
	}

	public double getFreeFlowKmCounter() {
		return roundDouble((freeFlowKmCounter / WarmPollutant.values().length), 3);
	}

	public double getStopGoKmCounter() {
		return roundDouble((stopGoKmCounter / WarmPollutant.values().length), 3);
	}

	public int getWarmEmissionEventCounter() {
		return emissionEventCounter;
	}

	public static double roundDouble(double dd, int decimalPlace){
	BigDecimal bd = new BigDecimal(Double.toString(dd));
	bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
	return bd.doubleValue();
	}
	
}