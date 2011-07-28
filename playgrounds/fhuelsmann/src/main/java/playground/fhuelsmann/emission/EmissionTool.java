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
import java.util.ArrayList;

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

import playground.fhuelsmann.emission.objects.VisumObject;

public class EmissionTool {
	private static final Logger logger = Logger.getLogger(EmissionTool.class);

	//	private static String runDirectory = "../../run980/";
	//	private static String eventsFile = runDirectory + "ITERS/it.1000/980.1000.events.xml.gz";
	//	private static String netFile = runDirectory + "980.output_network.xml.gz";
	//	private static String plansFile = runDirectory + "980.output_plans.xml.gz";

	private static String runDirectory = "../../runs-svn/run972/";
	private static String eventsFile = runDirectory + "ITERS/it.500/500.events.txt.gz";
	private static String netFile = runDirectory + "output_network.xml.gz";
	private static String plansFile = runDirectory + "output_plans.xml.gz";

	private static String visum2hbefaRoadTypeFile = "../../detailedEval/testRuns/input/inputEmissions/road_types.txt";
	private static String visum2hbefaRoadTypeTraffcSituationFile = "../../detailedEval/testRuns/input/inputEmissions/road_types_trafficSituation.txt";
	private static String hbefaAverageFleetEmissionFactorsFile = "../../detailedEval/testRuns/input/inputEmissions/hbefa_emission_factors_urban_rural_MW.txt";
	private static String hbefaAverageFleetHdvEmissionFactorsFile = "../../detailedEval/testRuns/input/inputEmissions/hbefa_emission_factors_urban_rural_MW_hdv.txt";
	private static String hbefaColdEmissionFactorsFile = "../../detailedEval/testRuns/input/inputEmissions/hbefa_coldstart_emission_factors.txt";
	private static String hbefaHotFile = "../../detailedEval/emissions/hbefa/EFA_HOT_SubSegm_PC.txt";
	private static String vehicleFile = "../../detailedEval/pop/140k-synthetische-personen/vehicles.xml";
	
	private static String outputFile = runDirectory + "emission.events.xml.gz";

	// =======================================================================================================		
	private final ArrayList<String> listOfPollutants;
	private final Scenario scenario;

	public EmissionTool(){
		Config config = ConfigUtils.createConfig();
		this.scenario = ScenarioUtils.createScenario(config);
		this.listOfPollutants =  new ArrayList<String>();
	}

	private void run(String[] args) {
		defineListOfPollutants();
		loadScenario();
		Network network = scenario.getNetwork();
		
		// read different hbefa tables
		HbefaTable hbefaTable = new HbefaTable();
		hbefaTable.makeHbefaTable(hbefaAverageFleetEmissionFactorsFile);
		HbefaTable hbefaHdvTable = new HbefaTable();
		hbefaHdvTable.makeHbefaTable(hbefaAverageFleetHdvEmissionFactorsFile);
		HbefaColdEmissionTable hbefaColdTable = new HbefaColdEmissionTable();
		hbefaColdTable.makeHbefaColdTable(hbefaColdEmissionFactorsFile);
		HbefaHot hbefaHot = new HbefaHot();
		hbefaHot.makeHbefaHot(hbefaHotFile);

		// read the vehicle file
		Vehicles vehicles = new VehiclesImpl();
		VehicleReaderV1 vehicleReader = new VehicleReaderV1(vehicles);
		vehicleReader.readFile(vehicleFile);

		VisumObject[] roadTypes = createRoadTypes(visum2hbefaRoadTypeFile);
		String[][] roadTypesTrafficSituations = createRoadTypesTafficSituation(visum2hbefaRoadTypeTraffcSituationFile);
		WarmEmissionAnalysisModule warmEmissionAnalysisModule = new WarmEmissionAnalysisModule(listOfPollutants, roadTypes, roadTypesTrafficSituations, hbefaHot);
		ColdEmissionAnalysisModule coldEmissionAnalysisModule = new ColdEmissionAnalysisModule ();

		// create two event manager
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsManager emissionEventsManager = EventsUtils.createEventsManager();
		// create the handler
		WarmEmissionHandler warmEmissionHandler = new WarmEmissionHandler(
				vehicles,
				network,
				hbefaTable.getHbefaTableWithSpeedAndEmissionFactor(),
				hbefaHdvTable.getHbefaTableWithSpeedAndEmissionFactor(),
				warmEmissionAnalysisModule,
				emissionEventsManager);
		ColdEmissionHandler coldEmissionHandler = new ColdEmissionHandler(
				network,
				hbefaColdTable,
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

	private void loadScenario() {
		Config config = scenario.getConfig();
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile);
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(scenario);
		scenarioLoader.loadScenario();
	}

	private void defineListOfPollutants() {
		listOfPollutants.add("FC");
		listOfPollutants.add("NOx");
		listOfPollutants.add("CO2(total)");
		listOfPollutants.add("NO2");
		listOfPollutants.add("PM");
		//		listOfPollutants.add("Benzene");
		//		listOfPollutants.add("CH4");
		//		listOfPollutants.add("CO");
		//		listOfPollutants.add("CO(rep.)");
		//		listOfPollutants.add("HC");
		//		listOfPollutants.add("NH3");
		//		listOfPollutants.add("NMHC");
		//		listOfPollutants.add("Pb");
		//		listOfPollutants.add("N20");
		//		listOfPollutants.add("PN");
		//		listOfPollutants.add("SO2");
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

	VisumObject[] createRoadTypes(String filename){
		VisumObject[] roadTypes = new VisumObject[100];
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
				VisumObject obj = new VisumObject(Integer.parseInt(array[0]), array[2]);
				roadTypes[obj.getVISUM_RT_NR()] = obj;
			}
			in.close();
			return roadTypes;
		}
		catch (Exception e){
			throw new RuntimeException(e);
		}
	}

	public static void main (String[] args) throws Exception{
		EmissionTool emissionTool = new EmissionTool();
		emissionTool.run(args);
	}
}