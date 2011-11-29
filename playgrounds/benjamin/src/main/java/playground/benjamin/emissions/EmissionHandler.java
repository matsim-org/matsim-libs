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
package playground.benjamin.emissions;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import playground.benjamin.emissions.types.ColdPollutant;
import playground.benjamin.emissions.types.HbefaColdEmissionFactor;
import playground.benjamin.emissions.types.HbefaColdEmissionFactorKey;
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
public class EmissionHandler {
	private static final Logger logger = Logger.getLogger(EmissionHandler.class);
	
	final Scenario scenario;
	static EventWriterXML emissionEventWriter;

	//===
	static String roadTypeMappingFile;
	static String emissionVehicleFile;
	
	static String averageFleetColdEmissionFactorsFile;
	static String averageFleetWarmEmissionFactorsFile;

	static String detailedWarmEmissionFactorsFile;
	static String detailedColdEmissionFactorsFile;
	
	//===
	Map<Integer, String> roadTypeMapping;
	Vehicles emissionVehicles;
	
	Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable;
	Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable;

	Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable;
	Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable;

	public EmissionHandler(Scenario scenario) {
		this.scenario = scenario;
	}

	public void createLookupTables() {
		logger.info("entering createLookupTables");
		
		getInputFiles();
		
		roadTypeMapping = createRoadTypeMapping(roadTypeMappingFile);
		emissionVehicles = createEmissionVehicles(emissionVehicleFile);

		avgHbefaWarmTable = createAvgHbefaWarmTable(averageFleetWarmEmissionFactorsFile);
		avgHbefaColdTable = createAvgHbefaColdTable(averageFleetColdEmissionFactorsFile);
		
		if(scenario.getConfig().vspExperimental().isUsingDetailedEmissionCalculation()){
			detailedHbefaWarmTable = createDetailedHbefaWarmTable(detailedWarmEmissionFactorsFile);
			detailedHbefaColdTable = createDetailedHbefaColdTable(detailedColdEmissionFactorsFile);
		}
		else{
			logger.warn("Detailed emission calculation is switched off in " + VspExperimentalConfigGroup.GROUP_NAME + " config group; Using fleet average values for all vehicles.");
		}
		logger.info("leaving createLookupTables");
	}

	private void getInputFiles() {
		
		roadTypeMappingFile = scenario.getConfig().vspExperimental().getEmissionRoadTypeMappingFile();
		emissionVehicleFile = scenario.getConfig().vspExperimental().getEmissionVehicleFile();

		averageFleetWarmEmissionFactorsFile = scenario.getConfig().vspExperimental().getAverageWarmEmissionFactorsFile();
		averageFleetColdEmissionFactorsFile = scenario.getConfig().vspExperimental().getAverageColdEmissionFactorsFile();
		
		detailedWarmEmissionFactorsFile = scenario.getConfig().vspExperimental().getDetailedWarmEmissionFactorsFile();
		detailedColdEmissionFactorsFile = scenario.getConfig().vspExperimental().getDetailedColdEmissionFactorsFile();
	}

	public void installEmissionEventHandler(EventsManager eventsManager, String emissionEventOutputFile) {
		logger.info("entering installEmissionsEventHandler") ;
		
		EventsManager emissionEventsManager = EventsUtils.createEventsManager();
		Network network = scenario.getNetwork() ;

		// instantiate analysis modules
		WarmEmissionAnalysisModule warmEmissionAnalysisModule = new WarmEmissionAnalysisModule(
				roadTypeMapping,
				avgHbefaWarmTable,
				detailedHbefaWarmTable,
				emissionEventsManager);
		ColdEmissionAnalysisModule coldEmissionAnalysisModule = new ColdEmissionAnalysisModule (
				avgHbefaColdTable,
				detailedHbefaColdTable,
				emissionEventsManager);
		
		// create different emission handler
		WarmEmissionHandler warmEmissionHandler = new WarmEmissionHandler(
				emissionVehicles,
				network,
				warmEmissionAnalysisModule);
		ColdEmissionHandler coldEmissionHandler = new ColdEmissionHandler(
				emissionVehicles,
				network,
				coldEmissionAnalysisModule);
		
		// create the writer for emission events
		emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
		emissionEventsManager.addHandler(emissionEventWriter);
		
		// add the handler
		eventsManager.addHandler(warmEmissionHandler);
		eventsManager.addHandler(coldEmissionHandler);
		
		logger.info("leaving installEmissionsEventHandler") ;
	}

	private Map<Integer, String> createRoadTypeMapping(String filename){
		logger.info("entering createRoadTypeMapping ...") ;
		
		Map<Integer, String> roadTypeMapping = new HashMap<Integer, String>();
		try{
			BufferedReader br = IOUtils.getBufferedReader(filename);
			String strLine = br.readLine();
			Map<String, Integer> indexFromKey = createIndexFromKey(strLine, ";");
			
			while ((strLine = br.readLine()) != null){
				if ( strLine.contains("\"")) throw new RuntimeException("cannot handle this character in parsing") ;
				
				String[] inputArray = strLine.split(";");
				Integer visumRtNr = Integer.parseInt(inputArray[indexFromKey.get("VISUM_RT_NR")]);
				String hbefaRtName = (inputArray[indexFromKey.get("HBEFA_RT_NAME")]);
				
				roadTypeMapping.put(visumRtNr, hbefaRtName);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("leaving createRoadTypeMapping ...") ;
		return roadTypeMapping;
	}
	
	private Vehicles createEmissionVehicles(String emissionVehicleFile) {
		logger.info("entering createEmissionVehicles ...") ;
		emissionVehicles = VehicleUtils.createVehiclesContainer();
		VehicleReaderV1 vehicleReader = new VehicleReaderV1(emissionVehicles);
		vehicleReader.readFile(emissionVehicleFile);
		logger.info("leaving createEmissionVehicles ...") ;
		return emissionVehicles;
	}

	private Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> createAvgHbefaWarmTable(String filename){
		logger.info("entering createAvgHbefaWarmTable ...");
		
		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable = new HashMap<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor>();
		
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
				
				avgHbefaWarmTable.put(key, value);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		logger.info("leaving createAvgHbefaWarmTable ...");
		return avgHbefaWarmTable;
	}
	
	/*TODO: CO2 not directly available for cold emissions; thus it could be calculated through FC as follows:
	get("FC")*0.865 - get("CO")*0.429 - get("HC")*0.866) / 0.273;*/
	private Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> createAvgHbefaColdTable(String filename){
		logger.info("entering createAvgHbefaColdTable ...");
		
		Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable = new HashMap<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor>();
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
				
				avgHbefaColdTable.put(key, value);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("leaving createAvgHbefaColdTable ...");
		return avgHbefaColdTable;
	}
	
	private Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> createDetailedHbefaWarmTable(String filename){
		logger.info("entering createDetailedHbefaWarmTable ...");

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
		logger.info("entering createDetailedHbefaWarmTable ...");
		return hbefaWarmTableDetailed;
	}
	
	/*TODO: CO2 not directly available for cold emissions; thus it could be calculated through FC as follows:
	get("FC")*0.865 - get("CO")*0.429 - get("HC")*0.866) / 0.273;*/
	private Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> createDetailedHbefaColdTable(String filename) {
		logger.info("entering createDetailedHbefaColdTable ...");
		
		Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable = new HashMap<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor>();
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
				
				detailedHbefaColdTable.put(key, value);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("leaving createDetailedHbefaColdTable ...");
		return detailedHbefaColdTable;
	}

	private Map<String, Integer> createIndexFromKey(String strLine, String fieldSeparator) {
		String[] keys = strLine.split(fieldSeparator) ;

		Map<String, Integer> indexFromKey = new HashMap<String, Integer>() ;
		for ( int ii = 0; ii < keys.length; ii++ ) {
			indexFromKey.put(keys[ii], ii ) ;
		}
		return indexFromKey ;
	}

	private HbefaVehicleCategory mapString2HbefaVehicleCategory(String string) {
		HbefaVehicleCategory hbefaVehicleCategory = null;
		if(string.contains("pass. car")) hbefaVehicleCategory = HbefaVehicleCategory.PASSENGER_CAR;
		else if(string.contains("HGV")) hbefaVehicleCategory = HbefaVehicleCategory.HEAVY_GOODS_VEHICLE;
		else{
			logger.warn("Could not map String " + string + " to any HbefaVehicleCategory; please check syntax in file " + averageFleetWarmEmissionFactorsFile);
			throw new RuntimeException();
		}
		return hbefaVehicleCategory;
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

	private HbefaTrafficSituation mapString2HbefaTrafficSituation(String string) {
		HbefaTrafficSituation hbefaTrafficSituation = null;
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

	public EventWriterXML getEmissionEventWriter() {
		return emissionEventWriter;
	}
}