/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionHandler.java
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
 * *********************************************************************** */
package org.matsim.contrib.emissions;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.ColdEmissionAnalysisModule.ColdEmissionAnalysisModuleParameter;
import org.matsim.contrib.emissions.WarmEmissionAnalysisModule.WarmEmissionAnalysisModuleParameter;
import org.matsim.contrib.emissions.types.*;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicles;


/**
 * @author benjamin
 *
 */
public class EmissionModule {
	private static final Logger logger = Logger.getLogger(EmissionModule.class);
	
	private final Scenario scenario;
	private WarmEmissionHandler warmEmissionHandler;
	private ColdEmissionHandler coldEmissionHandler;
	public EventsManager emissionEventsManager;
	private Double emissionEfficiencyFactor; // i think, this can also go to EmissionsConfigGroup. Amit Sep 2016

	private final EmissionsConfigGroup ecg;

	//===
	private static String roadTypeMappingFile;

	private static String averageFleetColdEmissionFactorsFile;
	private static String averageFleetWarmEmissionFactorsFile;

	private static String detailedWarmEmissionFactorsFile;
	private static String detailedColdEmissionFactorsFile;
	
	//===
    private Map<Integer, String> roadTypeMapping;
	private Vehicles emissionVehicles;
	
	private Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable;
	private Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable;

	private Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable;
	private Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable;


	public EmissionModule(Scenario scenario) {
		this.scenario = scenario;
		ecg = (EmissionsConfigGroup)scenario.getConfig().getModule(EmissionsConfigGroup.GROUP_NAME);
	}
	
	public EmissionModule(Scenario scenario, Vehicles emissionVehicles) { // TODO : probably we dont need this anymore. Amit sep 16
		this.scenario = scenario;
		this.emissionVehicles = emissionVehicles;
		ecg = (EmissionsConfigGroup)scenario.getConfig().getModule(EmissionsConfigGroup.GROUP_NAME);
	}

	public void createLookupTables() {
		logger.info("entering createLookupTables");
		
		getInputFiles();
		
		roadTypeMapping = createRoadTypeMapping(roadTypeMappingFile);
		if(this.emissionVehicles == null){
			emissionVehicles = scenario.getVehicles();
		}

		avgHbefaWarmTable = createAvgHbefaWarmTable(averageFleetWarmEmissionFactorsFile);
		avgHbefaColdTable = createAvgHbefaColdTable(averageFleetColdEmissionFactorsFile);

		if(ecg.isUsingDetailedEmissionCalculation()){
			detailedHbefaWarmTable = createDetailedHbefaWarmTable(detailedWarmEmissionFactorsFile);
			detailedHbefaColdTable = createDetailedHbefaColdTable(detailedColdEmissionFactorsFile);
		}
		else{
			logger.warn("Detailed emission calculation is switched off in " + EmissionsConfigGroup.GROUP_NAME + " config group; Using fleet average values for all vehicles.");
		}
		logger.info("leaving createLookupTables");
	}

	private void getInputFiles() {
	    EmissionsConfigGroup ecg = (EmissionsConfigGroup)scenario.getConfig().getModule("emissions");

		URL context = scenario.getConfig().getContext();

		roadTypeMappingFile = ecg.getEmissionRoadTypeMappingFileURL(context).getFile();

		averageFleetWarmEmissionFactorsFile = ecg.getAverageWarmEmissionFactorsFileURL(context).getFile();
		averageFleetColdEmissionFactorsFile = ecg.getAverageColdEmissionFactorsFileURL(context).getFile();
		
		if(ecg.isUsingDetailedEmissionCalculation()) {
			detailedWarmEmissionFactorsFile = ecg.getDetailedWarmEmissionFactorsFileURL(context).getFile();
			detailedColdEmissionFactorsFile = ecg.getDetailedColdEmissionFactorsFileURL(context).getFile();
		}
	}

	public void createEmissionHandler() {
		logger.info("entering createEmissionHandler");
		
		emissionEventsManager = EventsUtils.createEventsManager();
		Network network = scenario.getNetwork() ;

		WarmEmissionAnalysisModuleParameter parameterObject = new WarmEmissionAnalysisModuleParameter(roadTypeMapping, avgHbefaWarmTable, detailedHbefaWarmTable, ecg );
		ColdEmissionAnalysisModuleParameter parameterObject2 = new ColdEmissionAnalysisModuleParameter(avgHbefaColdTable, detailedHbefaColdTable, ecg);
		
		warmEmissionHandler = new WarmEmissionHandler(emissionVehicles,	network, parameterObject, emissionEventsManager, emissionEfficiencyFactor);
		coldEmissionHandler = new ColdEmissionHandler(emissionVehicles, network, parameterObject2, emissionEventsManager, emissionEfficiencyFactor);
		logger.info("leaving createEmissionHandler");
	}

	private Map<Integer, String> createRoadTypeMapping(String filename){
		logger.info("entering createRoadTypeMapping ...") ;
		
		Map<Integer, String> mapping = new HashMap<>();
		try{
			BufferedReader br = IOUtils.getBufferedReader(filename);
			String strLine = br.readLine();
			Map<String, Integer> indexFromKey = createIndexFromKey(strLine);
			
			while ((strLine = br.readLine()) != null){
				if ( strLine.contains("\"")) throw new RuntimeException("cannot handle this character in parsing") ;
				
				String[] inputArray = strLine.split(";");
				Integer visumRtNr = Integer.parseInt(inputArray[indexFromKey.get("VISUM_RT_NR")]);
				String hbefaRtName = (inputArray[indexFromKey.get("HBEFA_RT_NAME")]);
				
				mapping.put(visumRtNr, hbefaRtName);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("leaving createRoadTypeMapping ...") ;
		return mapping;
	}
	
	private Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> createAvgHbefaWarmTable(String filename){
		logger.info("entering createAvgHbefaWarmTable ...");
		
		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgWarmTable = new HashMap<>();
		
		try{
			BufferedReader br = IOUtils.getBufferedReader(filename);
			String strLine = br.readLine();
			Map<String, Integer> indexFromKey = createIndexFromKey(strLine);
			
			while ((strLine = br.readLine()) != null) {
				String[] array = strLine.split(";");
				
				HbefaWarmEmissionFactorKey key = new HbefaWarmEmissionFactorKey();
				key.setHbefaVehicleCategory(mapString2HbefaVehicleCategory(array[indexFromKey.get("VehCat")]));
				key.setHbefaComponent(mapComponent2WarmPollutant(array[indexFromKey.get("Component")]));
				key.setHbefaRoadCategory(mapString2HbefaRoadCategory(array[indexFromKey.get("TrafficSit")]));
				key.setHbefaTrafficSituation(mapString2HbefaTrafficSituation(array[indexFromKey.get("TrafficSit")]));
				key.setHbefaVehicleAttributes(new HbefaVehicleAttributes());
				
				HbefaWarmEmissionFactor value = new HbefaWarmEmissionFactor();
				value.setSpeed(Double.parseDouble(array[indexFromKey.get("V_weighted")]));
				value.setWarmEmissionFactor(Double.parseDouble(array[indexFromKey.get("EFA_weighted")]));
				
				avgWarmTable.put(key, value);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		logger.info("leaving createAvgHbefaWarmTable ...");
		return avgWarmTable;
	}
	
	private Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> createAvgHbefaColdTable(String filename){
		logger.info("entering createAvgHbefaColdTable ...");
		
		Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgColdTable = new HashMap<>();
		try{
			BufferedReader br = IOUtils.getBufferedReader(filename);
			String strLine = br.readLine();
			Map<String, Integer> indexFromKey = createIndexFromKey(strLine);
			
			while ((strLine = br.readLine()) != null)   {
				String[] array = strLine.split(";");
				
				HbefaColdEmissionFactorKey key = new HbefaColdEmissionFactorKey();
				key.setHbefaVehicleCategory(mapString2HbefaVehicleCategory(array[indexFromKey.get("VehCat")]));
				key.setHbefaComponent(mapComponent2ColdPollutant(array[indexFromKey.get("Component")]));
				key.setHbefaParkingTime(mapAmbientCondPattern2ParkingTime(array[indexFromKey.get("AmbientCondPattern")]));
				key.setHbefaDistance(mapAmbientCondPattern2Distance(array[indexFromKey.get("AmbientCondPattern")]));
				key.setHbefaVehicleAttributes(new HbefaVehicleAttributes());

				HbefaColdEmissionFactor value = new HbefaColdEmissionFactor();
				value.setColdEmissionFactor(Double.parseDouble(array[indexFromKey.get("EFA_weighted")]));
				
				avgColdTable.put(key, value);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("leaving createAvgHbefaColdTable ...");
		return avgColdTable;
	}
	
	private Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> createDetailedHbefaWarmTable(String filename){
		logger.info("entering createDetailedHbefaWarmTable ...");

		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> hbefaWarmTableDetailed = new HashMap<>() ;
		try{
			BufferedReader br = IOUtils.getBufferedReader(filename);
			String strLine = br.readLine();

			Map<String, Integer> indexFromKey = createIndexFromKey(strLine);

			while ((strLine = br.readLine()) != null) {
				String[] array = strLine.split(";");

				HbefaWarmEmissionFactorKey key = new HbefaWarmEmissionFactorKey();
				key.setHbefaVehicleCategory(mapString2HbefaVehicleCategory(array[indexFromKey.get("VehCat")]));
				key.setHbefaComponent(mapComponent2WarmPollutant(array[indexFromKey.get("Component")]));
				key.setHbefaRoadCategory(mapString2HbefaRoadCategory(array[indexFromKey.get("TrafficSit")]));
				key.setHbefaTrafficSituation(mapString2HbefaTrafficSituation(array[indexFromKey.get("TrafficSit")]));
				HbefaVehicleAttributes hbefaVehicleAttributes = new HbefaVehicleAttributes();
				hbefaVehicleAttributes.setHbefaTechnology(array[indexFromKey.get("Technology")]);
				hbefaVehicleAttributes.setHbefaSizeClass(array[indexFromKey.get("SizeClasse")]);
				hbefaVehicleAttributes.setHbefaEmConcept(array[indexFromKey.get("EmConcept")]);
				key.setHbefaVehicleAttributes(hbefaVehicleAttributes);

				HbefaWarmEmissionFactor value = new HbefaWarmEmissionFactor();
				value.setSpeed(Double.parseDouble(array[indexFromKey.get("V")]));
				value.setWarmEmissionFactor(Double.parseDouble(array[indexFromKey.get("EFA")]));

				hbefaWarmTableDetailed.put(key, value);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("entering createDetailedHbefaWarmTable ...");
		return hbefaWarmTableDetailed;
	}
	
	private Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> createDetailedHbefaColdTable(String filename) {
		logger.info("entering createDetailedHbefaColdTable ...");
		
		Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> hbefaColdTableDetailed = new HashMap<>();
		try{
			BufferedReader br = IOUtils.getBufferedReader(filename);
			String strLine = br.readLine();
			Map<String, Integer> indexFromKey = createIndexFromKey(strLine);
			
			while ((strLine = br.readLine()) != null)   {
				String[] array = strLine.split(";");
				
				HbefaColdEmissionFactorKey key = new HbefaColdEmissionFactorKey();
				key.setHbefaVehicleCategory(mapString2HbefaVehicleCategory(array[indexFromKey.get("VehCat")]));
				key.setHbefaComponent(mapComponent2ColdPollutant(array[indexFromKey.get("Component")]));
				key.setHbefaParkingTime(mapAmbientCondPattern2ParkingTime(array[indexFromKey.get("AmbientCondPattern")]));
				key.setHbefaDistance(mapAmbientCondPattern2Distance(array[indexFromKey.get("AmbientCondPattern")]));
				HbefaVehicleAttributes hbefaVehicleAttributes = new HbefaVehicleAttributes();
				hbefaVehicleAttributes.setHbefaTechnology(array[indexFromKey.get("Technology")]);
				hbefaVehicleAttributes.setHbefaSizeClass(array[indexFromKey.get("SizeClasse")]);
				hbefaVehicleAttributes.setHbefaEmConcept(array[indexFromKey.get("EmConcept")]);
				key.setHbefaVehicleAttributes(hbefaVehicleAttributes);

				HbefaColdEmissionFactor value = new HbefaColdEmissionFactor();
				value.setColdEmissionFactor(Double.parseDouble(array[indexFromKey.get("EFA")]));
				
				hbefaColdTableDetailed.put(key, value);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("leaving createDetailedHbefaColdTable ...");
		return hbefaColdTableDetailed;
	}

	private Map<String, Integer> createIndexFromKey(String strLine) {
		String[] keys = strLine.split(";") ;

		Map<String, Integer> indexFromKey = new HashMap<>() ;
		for ( int ii = 0; ii < keys.length; ii++ ) {
			indexFromKey.put(keys[ii], ii ) ;
		}
		return indexFromKey ;
	}

	private Integer mapAmbientCondPattern2Distance(String string) {
		Integer distance;
		String distanceString = string.split(",")[2];
		String upperbound = distanceString.split("-")[1];
		distance = Integer.parseInt(upperbound.split("k")[0]);
		return distance;
	}

	private Integer mapAmbientCondPattern2ParkingTime(String string) {
		Integer parkingTime;
		String parkingTimeString = string.split(",")[1];
		if(parkingTimeString.equals(">12h")){
			parkingTime = 13 ;
		} else {
			String upperbound = parkingTimeString.split("-")[1];
			parkingTime = Integer.parseInt(upperbound.split("h")[0]);
		}
		return parkingTime;
	}
	
	private WarmPollutant mapComponent2WarmPollutant(String string) {
		WarmPollutant warmPollutant = null;
		for(WarmPollutant wp : WarmPollutant.values()){
			if(string.equals(wp.getText())) {
                warmPollutant = wp;
            }
		}
		return warmPollutant;
	}
	
	private ColdPollutant mapComponent2ColdPollutant(String string) {
		ColdPollutant coldPollutant = null;
		for(ColdPollutant cp : ColdPollutant.values()){
			if(string.equals(cp.getText())) {
                coldPollutant = cp;
            }
		}
		return coldPollutant;
	}

	private String mapString2HbefaRoadCategory(String string) {
		String hbefaRoadCategory;
		String[] parts = string.split("/");
		hbefaRoadCategory = parts[0] + "/" + parts[1] + "/" + parts[2];
		return hbefaRoadCategory;
	}

	private HbefaVehicleCategory mapString2HbefaVehicleCategory(String string) {
		HbefaVehicleCategory hbefaVehicleCategory;
		if(string.contains("pass. car")) hbefaVehicleCategory = HbefaVehicleCategory.PASSENGER_CAR;
		else if(string.contains("HGV")) hbefaVehicleCategory = HbefaVehicleCategory.HEAVY_GOODS_VEHICLE;
		// TODO not sure, if some key is present in HBEFA database corresponding to ZEV; need to confirm. Amit sep 16
		else{
			logger.warn("Could not map String " + string + " to any HbefaVehicleCategory; please check syntax in file " + averageFleetWarmEmissionFactorsFile);
			throw new RuntimeException();
		}
		return hbefaVehicleCategory;
	}

	private HbefaTrafficSituation mapString2HbefaTrafficSituation(String string) {
		HbefaTrafficSituation hbefaTrafficSituation;
		if(string.endsWith("Freeflow")) hbefaTrafficSituation = HbefaTrafficSituation.FREEFLOW;
		else if(string.endsWith("Heavy")) hbefaTrafficSituation = HbefaTrafficSituation.HEAVY;
		else if(string.endsWith("Satur.")) hbefaTrafficSituation = HbefaTrafficSituation.SATURATED;
		else if(string.endsWith("St+Go")) hbefaTrafficSituation = HbefaTrafficSituation.STOPANDGO;
		else {
			logger.warn("Could not map String " + string + " to any HbefaTrafficSituation; please check syntax in file " + averageFleetWarmEmissionFactorsFile);
			throw new RuntimeException();
		}
		return hbefaTrafficSituation;
	}

	public WarmEmissionHandler getWarmEmissionHandler() {
		return warmEmissionHandler;	
	}

	public ColdEmissionHandler getColdEmissionHandler() {
		return coldEmissionHandler;
	}

	public EventsManager getEmissionEventsManager() {
		return emissionEventsManager;
	}

	@Deprecated // use scenario.getVehicles() instead.
	public Vehicles getEmissionVehicles() {
		return emissionVehicles;
	}

    public void setEmissionEfficiencyFactor(Double emissionEfficiencyFactor) {
		this.emissionEfficiencyFactor = emissionEfficiencyFactor;
		logger.info("Emission efficiency for the whole fleet is globally set to " + this.emissionEfficiencyFactor);
//		logger.warn("Emission efficiency for the whole fleet is globally set to " + this.emissionEfficiencyFactor);
	}

	public void writeEmissionInformation(String emissionEventOutputFile) {
		logger.info("Warm emissions were not calculated for " + warmEmissionHandler.getLinkLeaveWarnCnt() + " of " +
				warmEmissionHandler.getLinkLeaveCnt() + " link leave events (no corresponding link enter event).");
		
		WarmEmissionAnalysisModule wam = warmEmissionHandler.getWarmEmissionAnalysisModule();
//		ColdEmissionAnalysisModule cam = coldEmissionHandler.getColdEmissionAnalysisModule();
		
//		logger.info("Average speed was calculated to 0.0 or a negative value for " + wam.getAverageSpeedNegativeCnt() + " of " + 
//				wam.getWarmEmissionEventCounter() + " warm emission events.");
//		logger.info("Average speed was calculated greater than free flow speed for " + wam.getAverageSpeedTooHighCnt() + " of " +
//				wam.getWarmEmissionEventCounter() + " warm emission events.");
		
		logger.info("Emission calculation based on `Free flow only' occured for " + wam.getFreeFlowOccurences() + " of " +
				wam.getWarmEmissionEventCounter() + " warm emission events.");
		logger.info("Emission calculation based on `Stop&Go only' occured for " + wam.getStopGoOccurences() + " of " +
				wam.getWarmEmissionEventCounter() + " warm emission events.");
		logger.info("Emission calculation based on `Fractions' occured for " + wam.getFractionOccurences() + " of " +
				wam.getWarmEmissionEventCounter() + " warm emission events.");
		
		logger.info("Free flow occured on " + wam.getFreeFlowKmCounter() + " km of total " + 
				wam.getKmCounter() + " km, where emissions were calculated.");
		logger.info("Stop&Go occured on " + wam.getStopGoKmCounter() + " km of total " +
				wam.getKmCounter() + " km, where emissions were calculated.");
		
//		logger.info("Detailed vehicle attributes for warm emission calculation were not specified correctly for "
//				+ wam.getVehAttributesNotSpecified().size() + " of "
//				+ wam.getVehicleIdSet().size() + " vehicles.");
//		logger.info("Detailed vehicle attributes for cold emission calculation were not specified correctly for "
//				+ cam.getVehAttributesNotSpecified().size() + " of "
//				+ cam.getVehicleIdSet().size() + " vehicles.");
		
		logger.info("Emission calculation terminated. Output can be found in " + emissionEventOutputFile);
	}
}