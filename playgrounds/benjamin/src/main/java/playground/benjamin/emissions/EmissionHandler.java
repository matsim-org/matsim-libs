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
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import playground.benjamin.emissions.types.ColdPollutant;
import playground.benjamin.emissions.types.HbefaAvgColdEmissionFactors;
import playground.benjamin.emissions.types.HbefaAvgWarmEmissionFactors;
import playground.benjamin.emissions.types.HbefaAvgWarmEmissionFactorsKey;
import playground.benjamin.emissions.types.HbefaRoadTypeTrafficSituation;
import playground.benjamin.emissions.types.HbefaTrafficSituation;
import playground.benjamin.emissions.types.HbefaVehicleCategory;
import playground.benjamin.emissions.types.HbefaWarmEmissionFactorsDetailed;
import playground.benjamin.emissions.types.WarmPollutant;

/**
 * @author benjamin
 *
 */
public class EmissionHandler {
	private static final Logger logger = Logger.getLogger(EmissionHandler.class);
	
	private final Scenario scenario;
	private static EventWriterXML emissionEventWriter;

	//===
	private static String roadTypesTrafficSituationsFile;
	
	private static String averageFleetColdEmissionFactorsFile;
	private static String averageFleetWarmEmissionFactorsFile;

	private static String detailedWarmEmissionFactorsFile;
	//===
	Map<Integer, HbefaRoadTypeTrafficSituation> roadTypeMapping;
	
	private Map<HbefaAvgWarmEmissionFactorsKey, HbefaAvgWarmEmissionFactors> avgHbefaWarmTable;
	private Map<ColdPollutant, Map<Integer, Map<Integer, HbefaAvgColdEmissionFactors>>> avgHbefaColdTable;

	private Map<String, HbefaWarmEmissionFactorsDetailed> detailedHbefaWarmTable;
	//===
	private String vehicleFile;

	public EmissionHandler(Scenario scenario) {
		this.scenario = scenario;
	}

	public void createLookupTables() {
		logger.info("entering createLookupTables");
		
		getInputFiles();
		
		//TODO: reduce to RoadTypeMapping only; traffic situations could be mapped when creating the emission factors tables...
		roadTypeMapping = createRoadTypesTrafficSitMapping(roadTypesTrafficSituationsFile);

		avgHbefaWarmTable = createAvgHbefaWarmTable(averageFleetWarmEmissionFactorsFile);
		avgHbefaColdTable = createAvgHbefaColdTable(averageFleetColdEmissionFactorsFile);

		detailedHbefaWarmTable = createDetailedHbefaWarmTable(detailedWarmEmissionFactorsFile);

		logger.info("leaving createLookupTables");
	}

	private void getInputFiles() {
		
		roadTypesTrafficSituationsFile = scenario.getConfig().vspExperimental().getEmissionRoadTypeMappingFile();

		//TODO: create flag indicating which type of emission modelling should be performed...
		averageFleetWarmEmissionFactorsFile = scenario.getConfig().vspExperimental().getAverageWarmEmissionFactorsFile();
		averageFleetColdEmissionFactorsFile = scenario.getConfig().vspExperimental().getAverageColdEmissionFactorsFile();
		
		detailedWarmEmissionFactorsFile = scenario.getConfig().vspExperimental().getDetailedWarmEmissionFactorsFile() ;
	}

	public void installEmissionEventHandler(EventsManager eventsManager, String emissionEventOutputFile) {
		logger.info("entering installEmissionsEventHandler") ;
		
		EventsManager emissionEventsManager = EventsUtils.createEventsManager();
		Network network = scenario.getNetwork() ;

		//TODO: Vehicles should be read in vspExperimentalConfigGroup
		vehicleFile = "../../detailedEval/emissions/testScenario/input/vehicles.xml";
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		VehicleReaderV1 vehicleReader = new VehicleReaderV1(vehicles);
		vehicleReader.readFile(vehicleFile);
		
		// instantiate analysis modules
		WarmEmissionAnalysisModule warmEmissionAnalysisModule = new WarmEmissionAnalysisModule(
				roadTypeMapping,
				detailedHbefaWarmTable,
				avgHbefaWarmTable,
				emissionEventsManager);
		ColdEmissionAnalysisModule coldEmissionAnalysisModule = new ColdEmissionAnalysisModule (
				avgHbefaColdTable,
				emissionEventsManager);
		
		// create different emission handler
		WarmEmissionHandler warmEmissionHandler = new WarmEmissionHandler(
				vehicles,
				network,
				warmEmissionAnalysisModule);
		ColdEmissionHandler coldEmissionHandler = new ColdEmissionHandler(
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

	private static Map<Integer, HbefaRoadTypeTrafficSituation> createRoadTypesTrafficSitMapping(String filename){
		logger.info("entering createRoadTypesTrafficSitMapping ...") ;
		
		Map<Integer, HbefaRoadTypeTrafficSituation> roadTypeMapping = new HashMap<Integer, HbefaRoadTypeTrafficSituation>();
		try{
			BufferedReader br = IOUtils.getBufferedReader(filename);
			String strLine;
			// TODO: test for header line! See e.g. ReadFromUrbansimParcelModel.java by kai
			br.readLine(); // Read and forget header line
			while ((strLine = br.readLine()) != null){
				if ( strLine.contains("\"")) {
					throw new RuntimeException("cannot handle this character in parsing") ;
				}
	
				String[] inputArray = strLine.split(";");
				Map<HbefaTrafficSituation, String> trafficSitMapping = new HashMap<HbefaTrafficSituation, String>();
				
				//required for mapping
				Integer visumRtNr = Integer.parseInt(inputArray[0]);
				Integer hbefaRtNr = Integer.parseInt(inputArray[2]);
				trafficSitMapping.put(HbefaTrafficSituation.FREEFLOW, inputArray[3]);
				trafficSitMapping.put(HbefaTrafficSituation.HEAVY, inputArray[4]);
				trafficSitMapping.put(HbefaTrafficSituation.SATURATED, inputArray[5]);
				trafficSitMapping.put(HbefaTrafficSituation.STOPANDGO, inputArray[6]);
				
				HbefaRoadTypeTrafficSituation hbefaRoadTypeTrafficSituation = new HbefaRoadTypeTrafficSituation(hbefaRtNr, trafficSitMapping);
				
				//optional for mapping
				hbefaRoadTypeTrafficSituation.setVISUM_RT_NAME(inputArray[1]) ;
				
				roadTypeMapping.put(visumRtNr, hbefaRoadTypeTrafficSituation);
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("leaving createRoadTypesTrafficSitMapping ...") ;
		return roadTypeMapping;
	}
	
	//TODO: check direct output from HBEFA and adjust parser...
	private static Map<HbefaAvgWarmEmissionFactorsKey, HbefaAvgWarmEmissionFactors> createAvgHbefaWarmTable(String filename){
		logger.info("entering createAvgHbefaWarmTable ...");
		
		Map<HbefaAvgWarmEmissionFactorsKey, HbefaAvgWarmEmissionFactors> avgHbefaWarmTable = new HashMap<HbefaAvgWarmEmissionFactorsKey, HbefaAvgWarmEmissionFactors>();
		
		try{
			BufferedReader br = IOUtils.getBufferedReader(filename);
			
			String strLine = br.readLine();
			Map<String, Integer> indexFromKey = createIndexFromKey(strLine, ";");
			
			while ((strLine = br.readLine()) != null) {

				String[] array = strLine.split(";");
				
				HbefaAvgWarmEmissionFactorsKey key = new HbefaAvgWarmEmissionFactorsKey();
				key.setHbefaVehicleCategory(mapString2HbefaVehicleCategory(array[indexFromKey.get("VehCat")]));
				key.setHbefaRoadCategory(Integer.parseInt(array[indexFromKey.get("Road_Category")]));
				key.setHbefaTrafficSituation(mapString2HbefaTrafficSituation(array[indexFromKey.get("TS")]));
				
				HbefaAvgWarmEmissionFactors value = new HbefaAvgWarmEmissionFactors();
				value.setSpeed(Double.parseDouble(array[indexFromKey.get("S (speed)")]));
				value.setEmissionFactor(WarmPollutant.FC, Double.parseDouble(array[indexFromKey.get("mKr")]));
				value.setEmissionFactor(WarmPollutant.NOX, Double.parseDouble(array[indexFromKey.get("EF_Nox")]));
				value.setEmissionFactor(WarmPollutant.CO2_TOTAL, Double.parseDouble(array[indexFromKey.get("EF_CO2(total)")]));
				value.setEmissionFactor(WarmPollutant.NO2, Double.parseDouble(array[indexFromKey.get("EF_NO2")]));
				value.setEmissionFactor(WarmPollutant.PM, Double.parseDouble(array[indexFromKey.get("EF_PM")]));
				
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
	
	private static HbefaVehicleCategory mapString2HbefaVehicleCategory(String string) {
		HbefaVehicleCategory hbefaVehicleCategory = null;
		if(string.contains("pass. car")) hbefaVehicleCategory = HbefaVehicleCategory.PASSENGER_CAR;
		else if(string.contains("HDV")) hbefaVehicleCategory = HbefaVehicleCategory.HEAVY_GOODS_VEHICLE;
		else{
			logger.warn("Could not map String " + string + " to any HbefaVehicleCategory; please check syntax in file " + averageFleetWarmEmissionFactorsFile);
			throw new RuntimeException();
		}
		return hbefaVehicleCategory;
	}

	private static HbefaTrafficSituation mapString2HbefaTrafficSituation(String string) {
		HbefaTrafficSituation hbefaTrafficSituation = null;
		if(string.contains("Freeflow")) hbefaTrafficSituation = HbefaTrafficSituation.FREEFLOW;
		else if(string.contains("Heavy")) hbefaTrafficSituation = HbefaTrafficSituation.HEAVY;
		else if(string.contains("Satur.")) hbefaTrafficSituation = HbefaTrafficSituation.SATURATED;
		else if(string.contains("St+Go")) hbefaTrafficSituation = HbefaTrafficSituation.STOPANDGO;
		else {
			logger.warn("Could not map String " + string + " to any HbefaTrafficSituation; please check syntax in file " + averageFleetWarmEmissionFactorsFile);
			throw new RuntimeException();
		}
		return hbefaTrafficSituation;
	}

	private static Map<ColdPollutant, Map<Integer, Map<Integer, HbefaAvgColdEmissionFactors>>> createAvgHbefaColdTable(String filename){
		logger.info("entering createAvgHbefaColdTable ...");
		
		Map<ColdPollutant, Map<Integer, Map<Integer, HbefaAvgColdEmissionFactors>>> avgHbefaColdTable =
			new TreeMap<ColdPollutant, Map<Integer, Map<Integer, HbefaAvgColdEmissionFactors>>>();
		try{
			BufferedReader br = IOUtils.getBufferedReader(filename);
			String strLine;
			// TODO: test for header line! See e.g. ReadFromUrbansimParcelModel.java by kai
			br.readLine();// Read and forget header line
			while ((strLine = br.readLine()) != null)   {
				String[] array = strLine.split(";");
				HbefaAvgColdEmissionFactors coldEmissionFactor = new HbefaAvgColdEmissionFactors(
						array[0], //vehCat
						array[1], //component
						array[2], //parkingTime
						array[3], //distance
						Double.parseDouble(array[4]));//coldEF
				
				ColdPollutant coldPollutant = ColdPollutant.getValue(array[1]);
				int parkingTime = Integer.valueOf(array[2].split("-")[0]);
				int distance = Integer.valueOf(array[3].split("-")[0]);
				if (avgHbefaColdTable.get(coldPollutant) != null){
					if(avgHbefaColdTable.get(coldPollutant).get(distance) != null){
						avgHbefaColdTable.get(coldPollutant).get(distance).put(parkingTime, coldEmissionFactor);
					}
					else{
						Map<Integer, HbefaAvgColdEmissionFactors> tempParkingTime = new TreeMap<Integer, HbefaAvgColdEmissionFactors>();
						tempParkingTime.put(parkingTime, coldEmissionFactor);
						avgHbefaColdTable.get(coldPollutant).put(distance, tempParkingTime);	  
					}
				}
				else{
					Map<Integer,HbefaAvgColdEmissionFactors> tempParkingTime =	new TreeMap<Integer, HbefaAvgColdEmissionFactors>();
					tempParkingTime.put(parkingTime, coldEmissionFactor);
					Map<Integer, Map<Integer, HbefaAvgColdEmissionFactors>> tempDistance = new TreeMap<Integer, Map<Integer, HbefaAvgColdEmissionFactors>>();
					tempDistance.put(parkingTime, tempParkingTime);
					avgHbefaColdTable.put(coldPollutant, tempDistance);				
				}
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("leaving createAvgHbefaColdTable ...");
		return avgHbefaColdTable;
	}
	
	private static Map<String, HbefaWarmEmissionFactorsDetailed> createDetailedHbefaWarmTable(String filename){
		logger.info("entering createDetailedHbefaWarmTable ...");

		Map<String, HbefaWarmEmissionFactorsDetailed> hbefaWarmTableDetailed = new HashMap<String, HbefaWarmEmissionFactorsDetailed>() ;
		try{
			BufferedReader br = IOUtils.getBufferedReader(filename);
			String strLine;
			// TODO: test for header line! See e.g. ReadFromUrbansimParcelModel.java by kai
			// Read and forget header line
			br.readLine();
			while ((strLine = br.readLine()) != null) {

				// split is implemented below, see explanation.  
				String[] row = split(strLine,";");
				String key="";
				String[] value = new String[2];

				//create the key, the key is an array, hotKey	
				for(int i = 0; i < 13; i++)
					if (!(i == 8 || i == 9))
						key += row[i] + ";";

				//create the value, the value is an array, hotValue	
				value[0] = row[15];
				value[1] = row[18];

				hbefaWarmTableDetailed.put(key, new HbefaWarmEmissionFactorsDetailed(value));
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("entering createDetailedHbefaWarmTable ...");
		return hbefaWarmTableDetailed;
	}
	
	private static String[] split(String hbefa, String symbol) {
		String[] result = new String[27];
		int index = 0;
		String part = "";

		for(int i = 0; i < hbefa.length(); i++){
			if (hbefa.substring(i, i + 1).equals(symbol)){
				result[index++] = "" + part;
				part = "";
			} else {
				part += hbefa.substring(i, i+1);
			}
		}
		result[index++] = part;
		return result;
	}		

	private static Map<String, Integer> createIndexFromKey(String strLine, String fieldSeparator) {
		String[] keys = strLine.split(fieldSeparator) ;

		Map<String, Integer> indexFromKey = new HashMap<String, Integer>() ;
		for ( int ii = 0; ii < keys.length; ii++ ) {
			indexFromKey.put(keys[ii], ii ) ;
		}
		return indexFromKey ;
	}

	public EventWriterXML getEmissionEventWriter() {
		return emissionEventWriter;
	}
}
