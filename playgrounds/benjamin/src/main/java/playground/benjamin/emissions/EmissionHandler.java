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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import playground.benjamin.emissions.dataTypes.HbefaColdEmissionTableCreator;
import playground.benjamin.emissions.dataTypes.HbefaWarmEmissionTableCreator;
import playground.benjamin.emissions.dataTypes.HbefaWarmEmissionTableCreatorDetailed;
import playground.benjamin.emissions.dataTypes.RoadTypeTrafficSit;

/**
 * @author benjamin
 *
 */
public class EmissionHandler {
	private static final Logger logger = Logger.getLogger(EmissionHandler.class);
	
	private final Scenario scenario;
	private static EventWriterXML emissionEventWriter;

	private static String roadTypesTrafficSituationsFile = null;
	private static String warmEmissionFactorsFile = null ;
	
	Map<Integer, RoadTypeTrafficSit> roadTypeMapping;
	HbefaWarmEmissionTableCreatorDetailed hbefaWarmEmissionTableCreatorDetailed = new HbefaWarmEmissionTableCreatorDetailed();
	
	HbefaColdEmissionTableCreator hbefaAvgColdEmissionTableCreator = new HbefaColdEmissionTableCreator();
	
	HbefaWarmEmissionTableCreator hbefaAvgWarmEmissionTableCreator = new HbefaWarmEmissionTableCreator();
	HbefaWarmEmissionTableCreator hbefaAvgWarmEmissionTableCreatorHDV = new HbefaWarmEmissionTableCreator();

	// TODO: include file name specification into vspExperimentalConfigGroup
	private static String vehicleFile = "../../detailedEval/emissions/testScenario/input/vehicles.xml";
	
	private static String coldEmissionFactorsFile = "../../detailedEval/emissions/hbefaForMatsim/hbefa_coldstart_emission_factors.txt";
	private static String averageFleetEmissionFactorsFile = "../../detailedEval/emissions/hbefaForMatsim/hbefa_emission_factors_urban_rural_MW.txt";
	private static String averageFleetHdvEmissionFactorsFile = "../../detailedEval/emissions/hbefaForMatsim/hbefa_emission_factors_urban_rural_MW_hdv.txt";
	
	public EmissionHandler(Scenario scenario) {
		this.scenario = scenario;
	}

	public void createLookupTables() {
		logger.info("entering createLookupTables");
		
		getInputFiles();
		
		roadTypeMapping = createRoadTypesTrafficSitMapping(roadTypesTrafficSituationsFile);
		hbefaWarmEmissionTableCreatorDetailed.makeHbefaWarmTableDetailed(warmEmissionFactorsFile);
		
		hbefaAvgColdEmissionTableCreator.makeHbefaColdTable(coldEmissionFactorsFile);

		hbefaAvgWarmEmissionTableCreator.makeHbefaWarmTable(averageFleetEmissionFactorsFile);
		hbefaAvgWarmEmissionTableCreatorHDV.makeHbefaWarmTable(averageFleetHdvEmissionFactorsFile);

		logger.info("leaving createLookupTables");
	}

	private void getInputFiles() {
		roadTypesTrafficSituationsFile = scenario.getConfig().vspExperimental().getEmissionRoadTypeMappingFile();
		warmEmissionFactorsFile = scenario.getConfig().vspExperimental().getEmissionFactorsWarmFile() ;
	}

	public void installEmissionEventHandler(EventsManager eventsManager, String emissionEventOutputFile) {
		logger.info("entering installEmissionsEventHandler") ;
		
		Network network = scenario.getNetwork() ;
		EventsManager emissionEventsManager = EventsUtils.createEventsManager();

		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		VehicleReaderV1 vehicleReader = new VehicleReaderV1(vehicles);
		vehicleReader.readFile(vehicleFile);
		
		// instantiate analysis modules
		WarmEmissionAnalysisModule warmEmissionAnalysisModule = new WarmEmissionAnalysisModule(
				roadTypeMapping,
				hbefaWarmEmissionTableCreatorDetailed,
				hbefaAvgWarmEmissionTableCreator,
				hbefaAvgWarmEmissionTableCreatorHDV,
				emissionEventsManager);
		ColdEmissionAnalysisModule coldEmissionAnalysisModule = new ColdEmissionAnalysisModule (
				hbefaAvgColdEmissionTableCreator,
				emissionEventsManager);
		
		// create the different emission handler
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

	Map<Integer, RoadTypeTrafficSit> createRoadTypesTrafficSitMapping(String filename){
		logger.info("entering createRoadTypesTrafficSitMapping ...") ;
		
		Map<Integer, RoadTypeTrafficSit> roadTypeMapping = new HashMap<Integer, RoadTypeTrafficSit>();
		try{
			FileInputStream fstream = new FileInputStream(filename);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;

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
				
				RoadTypeTrafficSit roadTypeTrafficSit = new RoadTypeTrafficSit(hbefaRtNr, trafficSit);
				
				//optional for mapping
				roadTypeTrafficSit.setVISUM_RT_NAME(inputArray[1]) ;
				roadTypeTrafficSit.setHBEFA_RT_NAME(inputArray[3]) ;
				
				roadTypeMapping.put(visumRtNr, roadTypeTrafficSit);
			}
			in.close();

			logger.info("leaving createRoadTypesTrafficSitMapping ...") ;
			return roadTypeMapping;
		}
		catch (Exception e){
			throw new RuntimeException(e);
		}
	}

	public EventWriterXML getEmissionEventWriter() {
		return emissionEventWriter;
	}
}
