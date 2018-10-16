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

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.ColdEmissionAnalysisModule.ColdEmissionAnalysisModuleParameter;
import org.matsim.contrib.emissions.WarmEmissionAnalysisModule.WarmEmissionAnalysisModuleParameter;
import org.matsim.contrib.emissions.roadTypeMapping.HbefaRoadTypeMapping;
import org.matsim.contrib.emissions.roadTypeMapping.OsmHbefaMapping;
import org.matsim.contrib.emissions.roadTypeMapping.VisumHbefaRoadTypeMapping;
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
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.matsim.contrib.emissions.utils.EmissionUtils.createIndexFromKey;

/**
 * @author benjamin
 *
 */
public class EmissionModule {
	private static final Logger logger = Logger.getLogger(EmissionModule.class);
	
	private final Scenario scenario;
	private WarmEmissionHandler warmEmissionHandler;
	private ColdEmissionHandler coldEmissionHandler;

	private final EventsManager eventsManager;
	private final EmissionsConfigGroup emissionConfigGroup;

	//===

	private static URL averageFleetColdEmissionFactorsFile;
	private static URL averageFleetWarmEmissionFactorsFile;

	private static URL detailedWarmEmissionFactorsFile;
	private static URL detailedColdEmissionFactorsFile;
	
	//===
	private Vehicles vehicles;
	
	private Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable;
	private Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable;

	private Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable;
	private Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable;

	@Inject
	public EmissionModule(final Scenario scenario, final EventsManager eventsManager) {
		this.scenario = scenario;

		this.emissionConfigGroup = (EmissionsConfigGroup) scenario.getConfig().getModules().get(EmissionsConfigGroup.GROUP_NAME);

		if ( !emissionConfigGroup.isWritingEmissionsEvents() ) {
			logger.warn("Emission events are excluded from events file. A new events manager is created.");
			this.eventsManager = EventsUtils.createEventsManager();
		} else {
			this.eventsManager = eventsManager;
		}

		//TODO: create roadtype mapping here from config
		createLookupTables();
		createEmissionHandler();

		// Event handlers are now added to the event manager inside the respective Handlers, jm march '18
	}
	
	private void createLookupTables() {
		logger.info("entering createLookupTables");
		
		getInputFiles();

		vehicles = scenario.getVehicles();

		if( vehicles == null || vehicles.getVehicleTypes().isEmpty()) {
			throw new RuntimeException("For emissions calculations, at least vehicle type information is necessary." +
					"However, no information is provided. Aborting...");
		} else {
			for(VehicleType vehicleType : vehicles.getVehicleTypes().values()) {
				if (vehicleType.getMaximumVelocity() < 4.0/3.6 ) {
					// Historically, many emission vehicles file have maximum speed set to 1 m/s which was not used by mobsim before.
					// However, this should be removed if not set intentionally. Amit May'17
					logger.warn("The maximum speed of vehicle type "+ vehicleType+ " is less than 4 km/h. " +
							"\n Please make sure, this is really what you want because this will affect the mobility simulation.");
				}
			}
		}

//		if(scenario.getConfig().qsim().getVehiclesSource().equals(QSimConfigGroup.VehiclesSource.defaultVehicle)) {
//			logger.warn("Vehicle source in the QSim is "+ QSimConfigGroup.VehiclesSource.defaultVehicle.name()+
//							", however a vehicle file or vehicle information is provided. \n" +
//					"Therefore, switching to "+ QSimConfigGroup.VehiclesSource.fromVehiclesData.name()+".");
//			scenario.getConfig().qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.fromVehiclesData);
//			// yyyyyy code should not silently switch config options. kai, jul'18
//			logger.error("code should not silently switch config options; needs to be modified.  kai, jul'18") ;
//		}

		avgHbefaWarmTable = createAvgHbefaWarmTable(averageFleetWarmEmissionFactorsFile);
		avgHbefaColdTable = createAvgHbefaColdTable(averageFleetColdEmissionFactorsFile);

		if(emissionConfigGroup.isUsingDetailedEmissionCalculation()){
			detailedHbefaWarmTable = createDetailedHbefaWarmTable(detailedWarmEmissionFactorsFile);
			detailedHbefaColdTable = createDetailedHbefaColdTable(detailedColdEmissionFactorsFile);
		}
		else{
			logger.warn("Detailed emission calculation is switched off in " + EmissionsConfigGroup.GROUP_NAME + " config group; Using fleet average values for all vehicles.");
		}
		logger.info("leaving createLookupTables");
	}

	private void getInputFiles() {
		URL context = scenario.getConfig().getContext();

		averageFleetWarmEmissionFactorsFile = emissionConfigGroup.getAverageWarmEmissionFactorsFileURL(context);
		averageFleetColdEmissionFactorsFile = emissionConfigGroup.getAverageColdEmissionFactorsFileURL(context);
		
		if(emissionConfigGroup.isUsingDetailedEmissionCalculation()) {
			detailedWarmEmissionFactorsFile = emissionConfigGroup.getDetailedWarmEmissionFactorsFileURL(context);
			detailedColdEmissionFactorsFile = emissionConfigGroup.getDetailedColdEmissionFactorsFileURL(context);
		}
	}

	private void createEmissionHandler() {
		logger.info("entering createEmissionHandler");
		
		Network network = scenario.getNetwork() ;

		loadRoadTypeMappings();

		WarmEmissionAnalysisModuleParameter parameterObject = new WarmEmissionAnalysisModuleParameter(avgHbefaWarmTable, detailedHbefaWarmTable,
				emissionConfigGroup);
		ColdEmissionAnalysisModuleParameter parameterObject2 = new ColdEmissionAnalysisModuleParameter(avgHbefaColdTable, detailedHbefaColdTable,
				emissionConfigGroup);

		warmEmissionHandler = new WarmEmissionHandler(vehicles,	network, parameterObject, eventsManager, emissionConfigGroup
				.getEmissionEfficiencyFactor());
		coldEmissionHandler = new ColdEmissionHandler(vehicles, network, parameterObject2, eventsManager, emissionConfigGroup
				.getEmissionEfficiencyFactor());
		logger.info("leaving createEmissionHandler");
	}

	private void loadRoadTypeMappings() {
		HbefaRoadTypeMapping roadTypeMapping;

		switch (this.emissionConfigGroup.getHbefaRoadTypeSource()) {
			case fromOsm:
				logger.warn("It is recommended to directly set the HBEFA road types to link attributes and then chose HbefaRoadTypeSource: "+ EmissionsConfigGroup.HbefaRoadTypeSource.fromLinkAttributes );
				roadTypeMapping = new OsmHbefaMapping();
                roadTypeMapping.addHbefaMappings(scenario.getNetwork());
                break;
			case fromFile:
				logger.warn("It is recommended to directly set the HBEFA road types to link attributes and then chose HbefaRoadTypeSource: "+ EmissionsConfigGroup.HbefaRoadTypeSource.fromLinkAttributes );
				URL roadTypeMappingFile = this.emissionConfigGroup.getEmissionRoadTypeMappingFileURL(scenario.getConfig().getContext());
				roadTypeMapping = VisumHbefaRoadTypeMapping.createVisumRoadTypeMapping(roadTypeMappingFile);
				roadTypeMapping.addHbefaMappings(scenario.getNetwork());
				break;
			case fromLinkAttributes: //no need, road types are already there
				break;
			default:
				throw new RuntimeException(this.emissionConfigGroup.getHbefaRoadTypeSource()+ " is not implemented.");
		}

	}

	
	private Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> createAvgHbefaWarmTable(URL filename){
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
	
	private Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> createAvgHbefaColdTable(URL filename){
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

                double weighting = Double.parseDouble(array[indexFromKey.get("EFA_weighted")]); //TODO better name
				HbefaColdEmissionFactor value = new HbefaColdEmissionFactor(weighting);
//				value.setColdEmissionFactor(Double.parseDouble(array[indexFromKey.get("EFA_weighted")]));
				
				avgColdTable.put(key, value);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("leaving createAvgHbefaColdTable ...");
		return avgColdTable;
	}
	
	private Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> createDetailedHbefaWarmTable(URL filename){
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
	
	private Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> createDetailedHbefaColdTable(URL filename) {
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

				double weighting = Double.parseDouble(array[indexFromKey.get("EFA")]);
				HbefaColdEmissionFactor value = new HbefaColdEmissionFactor(weighting);
//				value.setColdEmissionFactor(Double.parseDouble(array[indexFromKey.get("EFA")]));
				
				hbefaColdTableDetailed.put(key, value);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("leaving createDetailedHbefaColdTable ...");
		return hbefaColdTableDetailed;
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
		else if(string.contains("motorcycle")) hbefaVehicleCategory = HbefaVehicleCategory.MOTORCYCLE;
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

	public WarmEmissionAnalysisModule getWarmEmissionAnalysisModule() {
		return this. warmEmissionHandler.getWarmEmissionAnalysisModule();
	}

	// probably, this is useful; e.g., emission events are not written and a few handlers must be attached to events manager
	// for the analysis purpose. Need a test. Amit Apr'17
	public EventsManager getEmissionEventsManager() {
		return eventsManager;
	}

	public void writeEmissionInformation() {
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
		
		logger.info("Emission calculation terminated. Emission events can be found in regular events file.");
	}
}