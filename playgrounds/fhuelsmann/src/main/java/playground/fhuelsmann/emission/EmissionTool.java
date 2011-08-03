/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionTool.java
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
package playground.fhuelsmann.emission;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesImpl;

import playground.fhuelsmann.emission.objects.HbefaColdEmissionTableCreator;
import playground.fhuelsmann.emission.objects.HbefaWarmEmissionTableCreator;
import playground.fhuelsmann.emission.objects.HbefaWarmEmissionTableCreatorDetailed;
import playground.fhuelsmann.emission.objects.VisumRoadTypes;

public class EmissionTool {
	private static final Logger logger = Logger.getLogger(EmissionTool.class);

//		private static String runDirectory = "../../run980/";
//		private static String eventsFile = runDirectory + "ITERS/it.1000/980.1000.events.xml.gz";
//		private static String netFile = runDirectory + "980.output_network.xml.gz";
//		private static String plansFile = runDirectory + "980.output_plans.xml.gz";

	private static String runDirectory = "../../runs-svn/run972/";
	private static String eventsFile = runDirectory + "ITERS/it.500/500.events.txt.gz";
	private static String netFile = runDirectory + "output_network.xml.gz";
	
//	private static String runDirectory = "../../runs-svn/run973/";
//	private static String eventsFile = runDirectory + "ITERS/it.300/300.events.txt.gz";
//	private static String netFile = runDirectory + "output_network.xml.gz";
	
	// TODO: use output_events, output_vehicles

	private static String visum2hbefaRoadTypeFile = "../../detailedEval/testRuns/input/inputEmissions/road_types.txt";
	private static String visum2hbefaRoadTypeTraffcSituationFile = "../../detailedEval/testRuns/input/inputEmissions/road_types_trafficSituation.txt";
	private static String hbefaAverageFleetEmissionFactorsFile = "../../detailedEval/testRuns/input/inputEmissions/hbefa_emission_factors_urban_rural_MW.txt";
	private static String hbefaAverageFleetHdvEmissionFactorsFile = "../../detailedEval/testRuns/input/inputEmissions/hbefa_emission_factors_urban_rural_MW_hdv.txt";
	private static String hbefaColdEmissionFactorsFile = "../../detailedEval/testRuns/input/inputEmissions/hbefa_coldstart_emission_factors.txt";
	private static String hbefaHotFile = "../../detailedEval/emissions/hbefa/EFA_HOT_SubSegm_PC.txt";
	private static String vehicleFile = "../../detailedEval/pop/14k-synthetische-personen/vehicles.xml";
	
	private static String outputFile = runDirectory + "emission.events.xml.gz";

	// =======================================================================================================		
	private final Scenario scenario;

	public EmissionTool(){
		Config config = ConfigUtils.createConfig();
		this.scenario = ScenarioUtils.createScenario(config);
	}

	private void run(String[] args) {
		loadScenario();
		Network network = scenario.getNetwork();
		
		// create two event manager
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsManager emissionEventsManager = EventsUtils.createEventsManager();
		
		// read different hbefa tables
		HbefaWarmEmissionTableCreator hbefaAvgWarmEmissionTableCreator = new HbefaWarmEmissionTableCreator();
		hbefaAvgWarmEmissionTableCreator.makeHbefaWarmTable(hbefaAverageFleetEmissionFactorsFile);
		HbefaWarmEmissionTableCreator hbefaAvgWarmEmissionTableCreatorHDV = new HbefaWarmEmissionTableCreator();
		hbefaAvgWarmEmissionTableCreatorHDV.makeHbefaWarmTable(hbefaAverageFleetHdvEmissionFactorsFile);
		HbefaColdEmissionTableCreator hbefaAvgColdEmissionTableCreator = new HbefaColdEmissionTableCreator();
		hbefaAvgColdEmissionTableCreator.makeHbefaColdTable(hbefaColdEmissionFactorsFile);
		HbefaWarmEmissionTableCreatorDetailed hbefaWarmEmissionTableCreatorDetailed = new HbefaWarmEmissionTableCreatorDetailed();
		hbefaWarmEmissionTableCreatorDetailed.makeHbefaWarmTableDetailed(hbefaHotFile);

		// read the vehicle file
		Vehicles vehicles = new VehiclesImpl();
		VehicleReaderV1 vehicleReader = new VehicleReaderV1(vehicles);
		vehicleReader.readFile(vehicleFile);

		// TODO: make the following homogeneous?!?
		VisumRoadTypes[] roadTypes = createRoadTypes(visum2hbefaRoadTypeFile);
		String[][] roadTypesTrafficSituations = createRoadTypesTafficSituation(visum2hbefaRoadTypeTraffcSituationFile);
		
		WarmEmissionAnalysisModule warmEmissionAnalysisModule = new WarmEmissionAnalysisModule(
				roadTypes,
				roadTypesTrafficSituations,
				hbefaWarmEmissionTableCreatorDetailed,
				hbefaAvgWarmEmissionTableCreator,
				hbefaAvgWarmEmissionTableCreatorHDV,
				emissionEventsManager);
		ColdEmissionAnalysisModule coldEmissionAnalysisModule = new ColdEmissionAnalysisModule ();
		// create the handler
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
		EventWriterXML emissionEventWriter = new EventWriterXML(outputFile);
		// add the handler
		eventsManager.addHandler(warmEmissionHandler);
		eventsManager.addHandler(coldEmissionHandler);
		emissionEventsManager.addHandler(emissionEventWriter);
		//create the reader and read the file
		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
		matsimEventsReader.readFile(eventsFile);
		emissionEventWriter.closeFile();
		logger.info("Terminated. Output can be found in " + outputFile);
	}

	private String[][] createRoadTypesTafficSituation(
			String filename) {
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

	private void loadScenario() {
		Config config = scenario.getConfig();
		config.network().setInputFile(netFile);
//		config.plans().setInputFile(plansFile);
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(scenario);
		scenarioLoader.loadScenario();
	}

	public static void main (String[] args) throws Exception{
		EmissionTool emissionTool = new EmissionTool();
		emissionTool.run(args);
	}
}