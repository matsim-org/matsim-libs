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
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import playground.benjamin.emissions.dataTypes.ColdPollutant;
import playground.benjamin.emissions.dataTypes.HbefaColdEmissionFactor;
import playground.benjamin.emissions.dataTypes.HbefaWarmEmissionFactors;
import playground.benjamin.emissions.dataTypes.HbefaWarmEmissionFactorsDetailed;
import playground.benjamin.emissions.dataTypes.HbefaRoadTypeTrafficSituation;

/**
 * @author benjamin
 *
 */
public class EmissionHandler {
	private static final Logger logger = Logger.getLogger(EmissionHandler.class);
	
	private final Scenario scenario;
	private static EventWriterXML emissionEventWriter;

	private static String roadTypesTrafficSituationsFile = null;
	private static String detailedWarmEmissionFactorsFile = null ;
	
	Map<Integer, HbefaRoadTypeTrafficSituation> roadTypeMapping;

	private Map<String, HbefaWarmEmissionFactorsDetailed> detailedHbefaWarmTable;
	
	private HbefaWarmEmissionFactors[][] avgHbefaWarmTable;
	private HbefaWarmEmissionFactors[][] avgHbefaWarmTableHDV;
	private Map<ColdPollutant, Map<Integer, Map<Integer, HbefaColdEmissionFactor>>> avgHbefaColdTable;

	// TODO: include file name specification into vspExperimentalConfigGroup
	private static String vehicleFile = "../../detailedEval/emissions/testScenario/input/vehicles.xml";
	
	private static String averageFleetcoldEmissionFactorsFile = "../../detailedEval/emissions/hbefaForMatsim/hbefa_coldstart_emission_factors.txt";
	private static String averageFleetwarmEmissionFactorsFile = "../../detailedEval/emissions/hbefaForMatsim/hbefa_emission_factors_urban_rural_MW.txt";
	private static String averageFleetwarmEmissionFactorsFileHDV = "../../detailedEval/emissions/hbefaForMatsim/hbefa_emission_factors_urban_rural_MW_hdv.txt";
	
	public EmissionHandler(Scenario scenario) {
		this.scenario = scenario;
	}

	public void createLookupTables() {
		logger.info("entering createLookupTables");
		
		getInputFiles();
		
		roadTypeMapping = createRoadTypesTrafficSitMapping(roadTypesTrafficSituationsFile);

		detailedHbefaWarmTable = createDetailedHbefaWarmTable(detailedWarmEmissionFactorsFile);

		avgHbefaWarmTable = createAvgHbefaWarmTable(averageFleetwarmEmissionFactorsFile);
		avgHbefaWarmTableHDV = createAvgHbefaWarmTable(averageFleetwarmEmissionFactorsFileHDV);
		avgHbefaColdTable = createAvgHbefaColdTable(averageFleetcoldEmissionFactorsFile);

		logger.info("leaving createLookupTables");
	}

	private void getInputFiles() {
		roadTypesTrafficSituationsFile = scenario.getConfig().vspExperimental().getEmissionRoadTypeMappingFile();
		detailedWarmEmissionFactorsFile = scenario.getConfig().vspExperimental().getEmissionFactorsWarmFile() ;
	}

	public void installEmissionEventHandler(EventsManager eventsManager, String emissionEventOutputFile) {
		logger.info("entering installEmissionsEventHandler") ;
		
		EventsManager emissionEventsManager = EventsUtils.createEventsManager();
		Network network = scenario.getNetwork() ;

		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		VehicleReaderV1 vehicleReader = new VehicleReaderV1(vehicles);
		vehicleReader.readFile(vehicleFile);
		
		// instantiate analysis modules
		WarmEmissionAnalysisModule warmEmissionAnalysisModule = new WarmEmissionAnalysisModule(
				roadTypeMapping,
				detailedHbefaWarmTable,
				avgHbefaWarmTable,
				avgHbefaWarmTableHDV,
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
			FileInputStream fstream = new FileInputStream(filename);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;

			// TODO: test for header line! See e.g. ReadFromUrbansimParcelModel.java by kai
			// Read and forget header line
			br.readLine();
			while ((strLine = br.readLine()) != null){
				if ( strLine.contains("\"")) {
					throw new RuntimeException("cannot handle this character in parsing") ;
				}
	
				String[] inputArray = strLine.split(";");
				
				//required for mapping
				Integer visumRtNr = Integer.parseInt(inputArray[0]);
				Integer hbefaRtNr = Integer.parseInt(inputArray[2]);
				String trafficSit = inputArray[4];
				
				HbefaRoadTypeTrafficSituation hbefaRoadTypeTrafficSituation = new HbefaRoadTypeTrafficSituation(hbefaRtNr, trafficSit);
				
				//optional for mapping
				hbefaRoadTypeTrafficSituation.setVISUM_RT_NAME(inputArray[1]) ;
				hbefaRoadTypeTrafficSituation.setHBEFA_RT_NAME(inputArray[3]) ;
				
				roadTypeMapping.put(visumRtNr, hbefaRoadTypeTrafficSituation);
			}
			in.close();

			logger.info("leaving createRoadTypesTrafficSitMapping ...") ;
			return roadTypeMapping;
		} catch (Exception e){
			throw new RuntimeException(e);
		}
	}
	
	private static HbefaWarmEmissionFactors[][] createAvgHbefaWarmTable(String filename){
		logger.info("entering createAvgHbefaWarmTable ...");
		
		HbefaWarmEmissionFactors [] [] avgHbefaWarmTable = new HbefaWarmEmissionFactors [59][4];
		try{
			FileInputStream fstream = new FileInputStream(filename);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			int place = 0;
			// TODO: test for header line! See e.g. ReadFromUrbansimParcelModel.java by kai
			// Read and forget header line
			br.readLine();
			while ((strLine = br.readLine()) != null)   {

				String[] array = strLine.split(";");
				HbefaWarmEmissionFactors row = new HbefaWarmEmissionFactors(
						Integer.parseInt(array[1])      //Road_Category
						,array[2], 					    //IDTS
						Double.parseDouble(array[4]),   //S (speed)
						Double.parseDouble(array[5]),   //RPA
						Double.parseDouble(array[6]),   //%stop
						Double.parseDouble(array[7]),   //mKr
						Double.parseDouble(array[8]),   //EF_Nox
						Double.parseDouble(array[9]),   //EF_CO2(rep.)
						Double.parseDouble(array[10]),  //EF_CO2(total)
						Double.parseDouble(array[11]),  //NO2
						Double.parseDouble(array[12])); //PM

				int rowNumber = Integer.parseInt(array[1]);

				avgHbefaWarmTable [rowNumber] [place] = row;

				place++;
				if (place==4) place =0;
			}
			in.close();
			
			logger.info("leaving createAvgHbefaWarmTable ...");
			return avgHbefaWarmTable;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private static Map<ColdPollutant, Map<Integer, Map<Integer, HbefaColdEmissionFactor>>> createAvgHbefaColdTable(String filename){
		logger.info("leaving createAvgHbefaColdTable ...");
		
		Map<ColdPollutant, Map<Integer, Map<Integer, HbefaColdEmissionFactor>>> avgHbefaColdTable =
			new TreeMap<ColdPollutant, Map<Integer, Map<Integer, HbefaColdEmissionFactor>>>();
		try{
			FileInputStream fstream = new FileInputStream(filename);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			// TODO: test for header line! See e.g. ReadFromUrbansimParcelModel.java by kai
			// Read and forget header line
			br.readLine();
			while ((strLine = br.readLine()) != null)   {
				String[] array = strLine.split(";");
				HbefaColdEmissionFactor coldEmissionFactor = new HbefaColdEmissionFactor(
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
						Map<Integer, HbefaColdEmissionFactor> tempParkingTime = new TreeMap<Integer, HbefaColdEmissionFactor>();
						tempParkingTime.put(parkingTime, coldEmissionFactor);
						avgHbefaColdTable.get(coldPollutant).put(distance, tempParkingTime);	  
					}
				}
				else{
					Map<Integer,HbefaColdEmissionFactor> tempParkingTime =	new TreeMap<Integer, HbefaColdEmissionFactor>();
					tempParkingTime.put(parkingTime, coldEmissionFactor);
					Map<Integer, Map<Integer, HbefaColdEmissionFactor>> tempDistance = new TreeMap<Integer, Map<Integer, HbefaColdEmissionFactor>>();
					tempDistance.put(parkingTime, tempParkingTime);
					avgHbefaColdTable.put(coldPollutant, tempDistance);				
				}
			}
			in.close();
			
			logger.info("leaving createAvgHbefaColdTable ...");
			return avgHbefaColdTable;
		} catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	private static Map<String, HbefaWarmEmissionFactorsDetailed> createDetailedHbefaWarmTable(String filename){
		logger.info("entering createDetailedHbefaWarmTable ...");

		Map<String, HbefaWarmEmissionFactorsDetailed> hbefaWarmTableDetailed = new HashMap<String, HbefaWarmEmissionFactorsDetailed>() ;
		try{
			FileInputStream fstream = new FileInputStream(filename);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
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
			in.close();
			
			logger.info("entering createDetailedHbefaWarmTable ...");
			return hbefaWarmTableDetailed;
		} catch(Exception e ){
			throw new RuntimeException(e);
		}
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

	public EventWriterXML getEmissionEventWriter() {
		return emissionEventWriter;
	}
}
