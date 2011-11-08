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
import playground.benjamin.emissions.dataTypes.VisumRoadTypes;

/**
 * @author benjamin
 *
 */
public class EmissionHandler {
	
	private static String vehicleFile = "../../detailedEval/emissions/testScenario/input/vehicles.xml";
	
	private static String visum2hbefaRoadTypeFile = "../../detailedEval/testRuns/input/inputEmissions/road_types.txt";
	private static String visum2hbefaRoadTypeTraffcSituationFile = "../../detailedEval/testRuns/input/inputEmissions/road_types_trafficSituation.txt";
	
	private static String hbefaAverageFleetEmissionFactorsFile = "../../detailedEval/testRuns/input/inputEmissions/hbefa_emission_factors_urban_rural_MW.txt";
	private static String hbefaAverageFleetHdvEmissionFactorsFile = "../../detailedEval/testRuns/input/inputEmissions/hbefa_emission_factors_urban_rural_MW_hdv.txt";
	private static String hbefaColdEmissionFactorsFile = "../../detailedEval/testRuns/input/inputEmissions/hbefa_coldstart_emission_factors.txt";
	private static String hbefaHotFile = "../../detailedEval/emissions/hbefa/EFA_HOT_SubSegm_PC.txt";
	
	private static EventWriterXML emissionEventWriter;
	
	public void installEmissionEventHandler(Network network, EventsManager eventsManager, String emissionEventOutputFile) {
		
		EventsManager emissionEventsManager = EventsUtils.createEventsManager();
		
		// construct different hbefa tables
		HbefaWarmEmissionTableCreator hbefaAvgWarmEmissionTableCreator = new HbefaWarmEmissionTableCreator();
		hbefaAvgWarmEmissionTableCreator.makeHbefaWarmTable(hbefaAverageFleetEmissionFactorsFile);
		HbefaWarmEmissionTableCreator hbefaAvgWarmEmissionTableCreatorHDV = new HbefaWarmEmissionTableCreator();
		hbefaAvgWarmEmissionTableCreatorHDV.makeHbefaWarmTable(hbefaAverageFleetHdvEmissionFactorsFile);
		HbefaColdEmissionTableCreator hbefaAvgColdEmissionTableCreator = new HbefaColdEmissionTableCreator();
		hbefaAvgColdEmissionTableCreator.makeHbefaColdTable(hbefaColdEmissionFactorsFile);
		HbefaWarmEmissionTableCreatorDetailed hbefaWarmEmissionTableCreatorDetailed = new HbefaWarmEmissionTableCreatorDetailed();
		hbefaWarmEmissionTableCreatorDetailed.makeHbefaWarmTableDetailed(hbefaHotFile);

		// read the vehicle file
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		VehicleReaderV1 vehicleReader = new VehicleReaderV1(vehicles);
		vehicleReader.readFile(vehicleFile);

		// TODO: make the following homogeneous?!?
		VisumRoadTypes[] roadTypes = createRoadTypes(visum2hbefaRoadTypeFile);
		String[][] roadTypesTrafficSituations = createRoadTypesTafficSituation(visum2hbefaRoadTypeTraffcSituationFile);
		
		// instantiate analysis modules
		WarmEmissionAnalysisModule warmEmissionAnalysisModule = new WarmEmissionAnalysisModule(
				roadTypes,
				roadTypesTrafficSituations,
				hbefaWarmEmissionTableCreatorDetailed,
				hbefaAvgWarmEmissionTableCreator,
				hbefaAvgWarmEmissionTableCreatorHDV,
				emissionEventsManager);
		ColdEmissionAnalysisModule coldEmissionAnalysisModule = new ColdEmissionAnalysisModule ();
		
		// create the different emission handler
		WarmEmissionHandler warmEmissionHandler = new WarmEmissionHandler(
				vehicles,
				network,
				warmEmissionAnalysisModule);
		ColdEmissionHandler coldEmissionHandler = new ColdEmissionHandler(
				network,
				hbefaAvgColdEmissionTableCreator,
				coldEmissionAnalysisModule,
				emissionEventsManager);
		
		// create the writer for emission events
		emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
		emissionEventsManager.addHandler(emissionEventWriter);
		
		// add the handler
		eventsManager.addHandler(warmEmissionHandler);
		eventsManager.addHandler(coldEmissionHandler);
	}

	public EventWriterXML getEmissionEventWriter() {
		return emissionEventWriter;
	}

	private String[][] createRoadTypesTafficSituation(String filename) {
		String[][] roadTypesTrafficSituations = new String[100][4];
		int[] counter = new int[100];
		for(int i=0; i<100;i++)
			counter[i]=0;
		try{
			FileInputStream fstream = new FileInputStream(filename);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine="";
			//Read File Line By Line
			br.readLine();
	
			while ((strLine = br.readLine()) != null){
				//for all lines (whole text) we split the line to an array 
				String[] array = strLine.split(";");
				int roadtype=Integer.valueOf(array[0]);
				int traficSitIndex = counter[roadtype]++;
				roadTypesTrafficSituations[roadtype][traficSitIndex] = array[3];
			}
			in.close();
			return roadTypesTrafficSituations;
		}
		catch (Exception e){
			throw new RuntimeException(e);
		}
	}
	
	VisumRoadTypes[] createRoadTypes(String filename){
		VisumRoadTypes[] roadTypes = new VisumRoadTypes[100];
		try{
			FileInputStream fstream = new FileInputStream(filename);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			//Read File Line By Line
			br.readLine();
			while ((strLine = br.readLine()) != null){
	
				//for all lines (whole text) we split the line to an array 
				String[] array = strLine.split(",");
				VisumRoadTypes obj = new VisumRoadTypes(Integer.parseInt(array[0]), array[2]);
				roadTypes[obj.getVISUM_RT_NR()] = obj;
			}
			in.close();
			return roadTypes;
		}
		catch (Exception e){
			throw new RuntimeException(e);
		}
	}
}
