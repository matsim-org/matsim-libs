/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.marginalTesting;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.ColdEmissionHandler;
import org.matsim.contrib.emissions.ColdEmissionAnalysisModule.ColdEmissionAnalysisModuleParameter;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.HbefaColdEmissionFactor;
import org.matsim.contrib.emissions.types.HbefaColdEmissionFactorKey;
import org.matsim.contrib.emissions.types.HbefaTrafficSituation;
import org.matsim.contrib.emissions.types.HbefaVehicleAttributes;
import org.matsim.contrib.emissions.types.HbefaVehicleCategory;
import org.matsim.contrib.emissions.types.HbefaWarmEmissionFactor;
import org.matsim.contrib.emissions.types.HbefaWarmEmissionFactorKey;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import playground.agarwalamit.marginalTesting.WarmEmissionAnalysisModuleImplV2.WarmEmissionAnalysisModuleParameter;

/**
 * @author amit
 */
public class MyEmissionModule {

	private final Logger logger = Logger.getLogger(MyEmissionModule.class);

	private final Scenario scenario;
	private WarmEmissionHandlerImplV2 warmEmissionHandler;
	private ColdEmissionHandler coldEmissionHandler;
	private EventsManager emissionEventsManager;
	private Double emissionEfficiencyFactor;

	//===
	private  String roadTypeMappingFile;
	private  String emissionVehicleFile;

	private  String averageFleetColdEmissionFactorsFile;
	private  String averageFleetWarmEmissionFactorsFile;

	private  String detailedWarmEmissionFactorsFile;
	private  String detailedColdEmissionFactorsFile;

	//===
	private Map<Integer, String> roadTypeMapping;
	private Vehicles emissionVehicles;

	private Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable;
	private Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable;

	private Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable;
	private Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable;


	public MyEmissionModule(Scenario scenario) {
		this.scenario = scenario;
	}

	public MyEmissionModule(Scenario scenario, Vehicles emissionVehicles) {
		this.scenario = scenario;
		this.emissionVehicles = emissionVehicles;
	}

	public void createLookupTables() {
		this.logger.info("entering createLookupTables");

		getInputFiles();

		this.roadTypeMapping = createRoadTypeMapping(this.roadTypeMappingFile);
		if(this.emissionVehicles == null){
			this.emissionVehicles = createEmissionVehicles(this.emissionVehicleFile);
		}

		this.avgHbefaWarmTable = createAvgHbefaWarmTable(this.averageFleetWarmEmissionFactorsFile);
		this.avgHbefaColdTable = createAvgHbefaColdTable(this.averageFleetColdEmissionFactorsFile);
		EmissionsConfigGroup ecg = (EmissionsConfigGroup)this.scenario.getConfig().getModule("emissions");
		if(ecg.isUsingDetailedEmissionCalculation()){
			this.detailedHbefaWarmTable = createDetailedHbefaWarmTable(this.detailedWarmEmissionFactorsFile);
			this.detailedHbefaColdTable = createDetailedHbefaColdTable(this.detailedColdEmissionFactorsFile);
		}
		else{
			this.logger.warn("Detailed emission calculation is switched off in " + VspExperimentalConfigGroup.GROUP_NAME + " config group; Using fleet average values for all vehicles.");
		}
		this.logger.info("leaving createLookupTables");
	}

	private void getInputFiles() {
		EmissionsConfigGroup ecg = (EmissionsConfigGroup)scenario.getConfig().getModule("emissions");

		this.roadTypeMappingFile = ecg.getEmissionRoadTypeMappingFile();
		this.emissionVehicleFile = ecg.getEmissionVehicleFile();

		this.averageFleetWarmEmissionFactorsFile = ecg.getAverageWarmEmissionFactorsFile();
		this.averageFleetColdEmissionFactorsFile = ecg.getAverageColdEmissionFactorsFile();

		this.detailedWarmEmissionFactorsFile = ecg.getDetailedWarmEmissionFactorsFile();
		this.detailedColdEmissionFactorsFile = ecg.getDetailedColdEmissionFactorsFile();
	}

	public void createEmissionHandler() {
		this.logger.info("entering createEmissionHandler");

		this.emissionEventsManager = EventsUtils.createEventsManager();
		Network network = this.scenario.getNetwork() ;

		WarmEmissionAnalysisModuleParameter parameterObject = new WarmEmissionAnalysisModuleParameter(roadTypeMapping, avgHbefaWarmTable, detailedHbefaWarmTable);
		ColdEmissionAnalysisModuleParameter parameterObject2 = new ColdEmissionAnalysisModuleParameter(avgHbefaColdTable, detailedHbefaColdTable);

		this.warmEmissionHandler = new WarmEmissionHandlerImplV2(this.emissionVehicles, parameterObject, this.emissionEventsManager, this.emissionEfficiencyFactor,(ScenarioImpl)this.scenario);
		this.coldEmissionHandler = new ColdEmissionHandler(this.emissionVehicles, network, parameterObject2, this.emissionEventsManager, this.emissionEfficiencyFactor);
		this.logger.info("leaving createEmissionHandler");
	}

	private Map<Integer, String> createRoadTypeMapping(String filename){
		this.logger.info("entering createRoadTypeMapping ...") ;

		Map<Integer, String> mapping = new HashMap<Integer, String>();
		try{
			BufferedReader br = IOUtils.getBufferedReader(filename);
			String strLine = br.readLine();
			Map<String, Integer> indexFromKey = createIndexFromKey(strLine, ";");

			while ((strLine = br.readLine()) != null){
				if ( strLine.contains("\"")) throw new RuntimeException("cannot handle this character in parsing") ;

				String[] inputArray = strLine.split(";");
				Integer visumRtNr = Integer.parseInt(inputArray[indexFromKey.get("VISUM_RT_NR")]);
				String hbefaRtName = (inputArray[indexFromKey.get("HBEFA_RT_NAME")]);

				mapping.put(visumRtNr, hbefaRtName);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.logger.info("leaving createRoadTypeMapping ...") ;
		return mapping;
	}

	private Vehicles createEmissionVehicles(String emissionVehicleFilename) {
		this.logger.info("entering createEmissionVehicles ...") ;
		this.emissionVehicles = VehicleUtils.createVehiclesContainer();
		VehicleReaderV1 vehicleReader = new VehicleReaderV1(this.emissionVehicles);
		vehicleReader.readFile(emissionVehicleFilename);
		this.logger.info("leaving createEmissionVehicles ...") ;
		return this.emissionVehicles;
	}

	private Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> createAvgHbefaWarmTable(String filename){
		this.logger.info("entering createAvgHbefaWarmTable ...");

		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgWarmTable = new HashMap<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor>();

		try{
			BufferedReader br = IOUtils.getBufferedReader(filename);
			String strLine = br.readLine();
			Map<String, Integer> indexFromKey = createIndexFromKey(strLine, ";");

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
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.logger.info("leaving createAvgHbefaWarmTable ...");
		return avgWarmTable;
	}

	private Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> createAvgHbefaColdTable(String filename){
		this.logger.info("entering createAvgHbefaColdTable ...");

		Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgColdTable = new HashMap<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor>();
		try{
			BufferedReader br = IOUtils.getBufferedReader(filename);
			String strLine = br.readLine();
			Map<String, Integer> indexFromKey = createIndexFromKey(strLine, ";");

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
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.logger.info("leaving createAvgHbefaColdTable ...");
		return avgColdTable;
	}

	private Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> createDetailedHbefaWarmTable(String filename){
		this.logger.info("entering createDetailedHbefaWarmTable ...");

		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> hbefaWarmTableDetailed = new HashMap<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor>() ;
		try{
			BufferedReader br = IOUtils.getBufferedReader(filename);
			String strLine = br.readLine();

			Map<String, Integer> indexFromKey = createIndexFromKey(strLine, ";");

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
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.logger.info("entering createDetailedHbefaWarmTable ...");
		return hbefaWarmTableDetailed;
	}

	private Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> createDetailedHbefaColdTable(String filename) {
		this.logger.info("entering createDetailedHbefaColdTable ...");

		Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> hbefaColdTableDetailed = new HashMap<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor>();
		try{
			BufferedReader br = IOUtils.getBufferedReader(filename);
			String strLine = br.readLine();
			Map<String, Integer> indexFromKey = createIndexFromKey(strLine, ";");

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
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.logger.info("leaving createDetailedHbefaColdTable ...");
		return hbefaColdTableDetailed;
	}

	private Map<String, Integer> createIndexFromKey(String strLine, String fieldSeparator) {
		String[] keys = strLine.split(fieldSeparator) ;

		Map<String, Integer> indexFromKey = new HashMap<String, Integer>() ;
		for ( int ii = 0; ii < keys.length; ii++ ) {
			indexFromKey.put(keys[ii], ii ) ;
		}
		return indexFromKey ;
	}

	private Integer mapAmbientCondPattern2Distance(String string) {
		Integer distance = null;
		String distanceString = string.split(",")[2];
		String upperbound = distanceString.split("-")[1];
		distance = Integer.parseInt(upperbound.split("k")[0]);
		return distance;
	}

	private Integer mapAmbientCondPattern2ParkingTime(String string) {
		Integer parkingTime = null;
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
			if(string.equals(wp.getText())) warmPollutant = wp;
			else continue;
		}
		return warmPollutant;
	}

	private ColdPollutant mapComponent2ColdPollutant(String string) {
		ColdPollutant coldPollutant = null;
		for(ColdPollutant cp : ColdPollutant.values()){
			if(string.equals(cp.getText())) coldPollutant = cp;
			else continue;
		}
		return coldPollutant;
	}

	private String mapString2HbefaRoadCategory(String string) {
		String hbefaRoadCategory = null;
		String[] parts = string.split("/");
		hbefaRoadCategory = parts[0] + "/" + parts[1] + "/" + parts[2];
		return hbefaRoadCategory;
	}

	private HbefaVehicleCategory mapString2HbefaVehicleCategory(String string) {
		HbefaVehicleCategory hbefaVehicleCategory = null;
		if(string.contains("pass. car")) hbefaVehicleCategory = HbefaVehicleCategory.PASSENGER_CAR;
		else if(string.contains("HGV")) hbefaVehicleCategory = HbefaVehicleCategory.HEAVY_GOODS_VEHICLE;
		else{
			this.logger.warn("Could not map String " + string + " to any HbefaVehicleCategory; please check syntax in file " + this.averageFleetWarmEmissionFactorsFile);
			throw new RuntimeException();
		}
		return hbefaVehicleCategory;
	}

	private HbefaTrafficSituation mapString2HbefaTrafficSituation(String string) {
		HbefaTrafficSituation hbefaTrafficSituation = null;
		if(string.endsWith("Freeflow")) hbefaTrafficSituation = HbefaTrafficSituation.FREEFLOW;
		else if(string.endsWith("Heavy")) hbefaTrafficSituation = HbefaTrafficSituation.HEAVY;
		else if(string.endsWith("Satur.")) hbefaTrafficSituation = HbefaTrafficSituation.SATURATED;
		else if(string.endsWith("St+Go")) hbefaTrafficSituation = HbefaTrafficSituation.STOPANDGO;
		else {
			this.logger.warn("Could not map String " + string + " to any HbefaTrafficSituation; please check syntax in file " + this.averageFleetWarmEmissionFactorsFile);
			throw new RuntimeException();
		}
		return hbefaTrafficSituation;
	}

	public WarmEmissionHandlerImplV2 getWarmEmissionHandler() {
		return this.warmEmissionHandler;	
	}

	public ColdEmissionHandler getColdEmissionHandler() {
		return this.coldEmissionHandler;
	}

	public EventsManager getEmissionEventsManager() {
		return this.emissionEventsManager;
	}

	public Vehicles getEmissionVehicles() {
		return this.emissionVehicles;
	}

	public double getTotalDelaysInHours(){
		return this.warmEmissionHandler.getTotalDelaysInSecs()/3600;
	}

	public Double getEmissionEfficiencyFactor() {
		return this.emissionEfficiencyFactor;
	}

	public void setEmissionEfficiencyFactor(Double emissionEfficiencyFactor) {
		this.emissionEfficiencyFactor = emissionEfficiencyFactor;
		this.logger.info("Emission efficiency for the whole fleet is globally set to " + this.emissionEfficiencyFactor);
		//		logger.warn("Emission efficiency for the whole fleet is globally set to " + this.emissionEfficiencyFactor);
	}

	public void writeEmissionInformation(String emissionEventOutputFile) {
		this.logger.info("Warm emissions were not calculated for " + this.warmEmissionHandler.getLinkLeaveWarnCnt() + " of " +
				this.warmEmissionHandler.getLinkLeaveCnt() + " link leave events (no corresponding link enter event).");

		WarmEmissionAnalysisModuleImplV2 wam = warmEmissionHandler.getWarmEmissionAnalysisModule();
		//		ColdEmissionAnalysisModule cam = coldEmissionHandler.getColdEmissionAnalysisModule();

		//		logger.info("Average speed was calculated to 0.0 or a negative value for " + wam.getAverageSpeedNegativeCnt() + " of " + 
		//				wam.getWarmEmissionEventCounter() + " warm emission events.");
		//		logger.info("Average speed was calculated greater than free flow speed for " + wam.getAverageSpeedTooHighCnt() + " of " +
		//				wam.getWarmEmissionEventCounter() + " warm emission events.");

		this.logger.info("Emission calculation based on `Free flow only' occured for " + wam.getFreeFlowOccurences() + " of " +
				wam.getWarmEmissionEventCounter() + " warm emission events.");
		this.logger.info("Emission calculation based on `Stop&Go only' occured for " + wam.getStopGoOccurences() + " of " +
				wam.getWarmEmissionEventCounter() + " warm emission events.");
		this.logger.info("Emission calculation based on `Fractions' occured for " + wam.getFractionOccurences() + " of " +
				wam.getWarmEmissionEventCounter() + " warm emission events.");

		this.logger.info("Free flow occured on " + wam.getFreeFlowKmCounter() + " km of total " + 
				wam.getKmCounter() + " km, where emissions were calculated.");
		this.logger.info("Stop&Go occured on " + wam.getStopGoKmCounter() + " km of total " +
				wam.getKmCounter() + " km, where emissions were calculated.");

		//		logger.info("Detailed vehicle attributes for warm emission calculation were not specified correctly for "
		//				+ wam.getVehAttributesNotSpecified().size() + " of "
		//				+ wam.getVehicleIdSet().size() + " vehicles.");
		//		logger.info("Detailed vehicle attributes for cold emission calculation were not specified correctly for "
		//				+ cam.getVehAttributesNotSpecified().size() + " of "
		//				+ cam.getVehicleIdSet().size() + " vehicles.");

		this.logger.info("Emission calculation terminated. Output can be found in " + emissionEventOutputFile);
	}
}
