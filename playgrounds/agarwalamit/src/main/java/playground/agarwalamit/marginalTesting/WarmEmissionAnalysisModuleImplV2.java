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
package playground.agarwalamit.marginalTesting;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.types.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;

import java.util.HashMap;
import java.util.Map;


/**
 * @author benjamin
 *
 */
public class WarmEmissionAnalysisModuleImplV2 {
	private static final Logger logger = Logger.getLogger(WarmEmissionAnalysisModuleImplV2.class);

	final Map<Integer, String> roadTypeMapping;

	final Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable;
	final Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable;

	final EventsManager eventsManager;
	final Double emissionEfficiencyFactor;

	int vehAttributesNotSpecifiedCnt = 0;
	int averageSpeedNegativeCnt = 0;
	int averageSpeedTooHighCnt = 0;
	final int maxWarnCnt = 3;

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
			// check if all needed tables are non-null
			if(roadTypeMapping == null){
				logger.error("Road type mapping not set. Aborting...");
				System.exit(0);
			}
			if(avgHbefaWarmTable == null && detailedHbefaWarmTable == null){
				logger.error("Neither average nor detailed table vor Hbefa warm emissions set. Aborting...");
				System.exit(0);
			}
		}
	}

	public WarmEmissionAnalysisModuleImplV2(
			WarmEmissionAnalysisModuleParameter parameterObject,
			EventsManager emissionEventsManager, Double emissionEfficiencyFactor) {

		if(parameterObject == null){
			logger.error("No warm emission analysis module parameter set. Aborting...");
			System.exit(0);
		}
		if(emissionEventsManager == null){
			logger.error("Event manager not set. Please check the configuration of your scenario. Aborting..." );
			System.exit(0);
		}
		this.roadTypeMapping = parameterObject.roadTypeMapping;
		this.avgHbefaWarmTable = parameterObject.avgHbefaWarmTable;
		this.detailedHbefaWarmTable = parameterObject.detailedHbefaWarmTable;
		this.eventsManager = emissionEventsManager;
		this.emissionEfficiencyFactor = emissionEfficiencyFactor;
	}

	public void reset() {
		logger.info("resetting counters...");
		vehAttributesNotSpecifiedCnt = 0;
		averageSpeedNegativeCnt = 0;
		averageSpeedTooHighCnt = 0;

		freeFlowCounter = 0;
		stopGoCounter = 0;
		fractionCounter = 0;
		emissionEventCounter = 0;

		kmCounter = 0.0;
		freeFlowKmCounter = 0.0;
		stopGoKmCounter = 0.0;
	}

	public void throwWarmEmissionEvent(double leaveTime, Id linkId, Id vehicleId, Map<WarmPollutant, Double> warmEmissions){
		Event warmEmissionEvent = new WarmEmissionEvent(leaveTime, linkId, vehicleId, warmEmissions);
		this.eventsManager.processEvent(warmEmissionEvent);
	}

	public Map<WarmPollutant, double[]> checkVehicleInfoAndCalculateWarmEmissions(
			Id personId,
			Integer roadType,
			Double freeVelocity,
			Double linkLength,
			Double travelTime,
			String vehicleInformation) {

		Map<WarmPollutant, double[]> warmEmissions;
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

		// a basic apporach to introduce emission reduced cars:
		if(emissionEfficiencyFactor != null){
			warmEmissions = rescaleWarmEmissions(warmEmissions);
		}
		return warmEmissions;
	}

	private Map<WarmPollutant, double[]> rescaleWarmEmissions(Map<WarmPollutant, double[]> warmEmissions) {
		Map<WarmPollutant, double[]> rescaledWarmEmissions = new HashMap<WarmPollutant, double[]>();

		for(WarmPollutant wp : warmEmissions.keySet()){
			double [] rescaledValue = new double [warmEmissions.get(wp).length];
			for(int i=0;i<warmEmissions.get(wp).length;i++){
				double orgValue = warmEmissions.get(wp)[i];
				rescaledValue[i] = emissionEfficiencyFactor * orgValue;
			}
			rescaledWarmEmissions.put(wp, rescaledValue);
		}
		return rescaledWarmEmissions;
	}

	private Map<WarmPollutant, double[]> calculateWarmEmissions(
			Id personId,
			Double travelTime,
			Integer roadType,
			Double freeVelocity,
			Double linkLength,
			Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple) {

		Map<WarmPollutant, double[]> warmEmissionsOfEvent = new HashMap<WarmPollutant, double[]>();

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

		if(this.detailedHbefaWarmTable != null){ // check if detailed emission factors file is set in config
			HbefaVehicleAttributes hbefaVehicleAttributes = new HbefaVehicleAttributes();
			hbefaVehicleAttributes.setHbefaTechnology(vehicleInformationTuple.getSecond().getHbefaTechnology());
			hbefaVehicleAttributes.setHbefaSizeClass(vehicleInformationTuple.getSecond().getHbefaSizeClass());
			hbefaVehicleAttributes.setHbefaEmConcept(vehicleInformationTuple.getSecond().getHbefaEmConcept());
			keyFreeFlow.setHbefaVehicleAttributes(hbefaVehicleAttributes);
			keyStopAndGo.setHbefaVehicleAttributes(hbefaVehicleAttributes);
		}

		double linkLength_km = linkLength / 1000;
		double travelTime_h = travelTime / 3600;
		double freeFlowSpeed_kmh = freeVelocity * 3.6;
		double averageSpeed_kmh = linkLength_km / travelTime_h;

		double stopGoSpeedFromTable_kmh;
		double efFreeFlow_gpkm;
		double efStopGo_gpkm;

		for (WarmPollutant warmPollutant : WarmPollutant.values()) {
			double generatedEmissions;

			keyFreeFlow.setHbefaComponent(warmPollutant);
			keyStopAndGo.setHbefaComponent(warmPollutant);

			if(this.detailedHbefaWarmTable != null){
				if(this.detailedHbefaWarmTable.get(keyFreeFlow) != null && this.detailedHbefaWarmTable.get(keyStopAndGo) != null){
					stopGoSpeedFromTable_kmh = this.detailedHbefaWarmTable.get(keyStopAndGo).getSpeed();
					efFreeFlow_gpkm = this.detailedHbefaWarmTable.get(keyFreeFlow).getWarmEmissionFactor();
					efStopGo_gpkm = this.detailedHbefaWarmTable.get(keyStopAndGo).getWarmEmissionFactor();

				} else {
					vehAttributesNotSpecifiedCnt++;
					stopGoSpeedFromTable_kmh = this.avgHbefaWarmTable.get(keyStopAndGo).getSpeed();
					efFreeFlow_gpkm = this.avgHbefaWarmTable.get(keyFreeFlow).getWarmEmissionFactor();
					efStopGo_gpkm = this.avgHbefaWarmTable.get(keyStopAndGo).getWarmEmissionFactor();

					if(vehAttributesNotSpecifiedCnt <= maxWarnCnt) {
						logger.warn("Detailed vehicle attributes are not specified correctly for person " + personId + ": " + 
								"`" + vehicleInformationTuple.getSecond() + "'. Using fleet average values instead.");
						if(vehAttributesNotSpecifiedCnt == maxWarnCnt) logger.warn(Gbl.FUTURE_SUPPRESSED);
					}
				}
			} else {
				stopGoSpeedFromTable_kmh = this.avgHbefaWarmTable.get(keyStopAndGo).getSpeed();
				efFreeFlow_gpkm = this.avgHbefaWarmTable.get(keyFreeFlow).getWarmEmissionFactor();
				efStopGo_gpkm = this.avgHbefaWarmTable.get(keyStopAndGo).getWarmEmissionFactor();
			}

			if(averageSpeed_kmh <= 0.0){
				throw new RuntimeException("Average speed has been calculated to 0.0 or a negative value. Aborting...");
			}
			if ((averageSpeed_kmh - freeFlowSpeed_kmh) > 1.0){
				throw new RuntimeException("Average speed has been calculated to be greater than free flow speed; this might produce negative warm emissions. Aborting...");
			}

			double matsimFreeFlowLinkTravelTime = (Math.floor(linkLength_km*3600/freeFlowSpeed_kmh)+1);

			if(travelTime < matsimFreeFlowLinkTravelTime) {
				throw new RuntimeException("Travel time from events can not be less than free flow travel on link.");
			}
			else if(matsimFreeFlowLinkTravelTime==travelTime) { // both speeds are assumed to be not very different > only freeFlow on link
				generatedEmissions = linkLength_km * efFreeFlow_gpkm; 
				freeFlowCounter++;
				freeFlowKmCounter = freeFlowKmCounter + linkLength_km;
			} else if ((averageSpeed_kmh - stopGoSpeedFromTable_kmh) <= 0.0) { // averageSpeed is less than stopGoSpeed > only stop&go on link
				generatedEmissions = linkLength_km * efStopGo_gpkm;
				stopGoCounter++;
				stopGoKmCounter = stopGoKmCounter + linkLength_km;
			} else {
				double distanceStopGo_km = (linkLength_km * stopGoSpeedFromTable_kmh * (freeFlowSpeed_kmh - averageSpeed_kmh)) / (averageSpeed_kmh * (freeFlowSpeed_kmh - stopGoSpeedFromTable_kmh));
				double distanceFreeFlow_km = linkLength_km - distanceStopGo_km;

				generatedEmissions = (distanceFreeFlow_km * efFreeFlow_gpkm) + (distanceStopGo_km * efStopGo_gpkm);
				fractionCounter++;
				stopGoKmCounter = stopGoKmCounter + distanceStopGo_km;
				freeFlowKmCounter = freeFlowKmCounter + distanceFreeFlow_km;
			}
			kmCounter = kmCounter + linkLength_km;
			double freeFlowEmission = linkLength_km*efFreeFlow_gpkm;
			double stopAndGoEmission = generatedEmissions-freeFlowEmission;
			double [] freeFlowAndStopAndGoEmission = {freeFlowEmission, stopAndGoEmission}; 
			warmEmissionsOfEvent.put(warmPollutant, freeFlowAndStopAndGoEmission);
		}
		emissionEventCounter++;
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
		return kmCounter / WarmPollutant.values().length;
	}

	public double getFreeFlowKmCounter() {
		return freeFlowKmCounter / WarmPollutant.values().length;
	}

	public double getStopGoKmCounter() {
		return stopGoKmCounter / WarmPollutant.values().length;
	}

	public int getWarmEmissionEventCounter() {
		return emissionEventCounter;
	}
}

