package playground.fhuelsmann.emission;
/* *********************************************************************** *
 * project: org.matsim.*
 * FhMain.java
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.ScenarioFactoryImpl;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;

import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioLoaderImpl;

public class Main {

	// INPUT
	private static String runDirectory = "../../detailedEval/testRuns/output/run8/";
	private static String eventsFile = runDirectory + "100.events.txt.gz";
	//		private static String netFile = "../../detailedEval/Net/network-86-85-87-84_simplified.xml";
	private static String netFile = runDirectory + "output_network.xml.gz";
	private static String plansFile = runDirectory + "output_plans.xml.gz";

	//	private static String runDirectory = "../../detailedEval/testRuns/output/1pct/v0-default/run8/";
	//	private static String eventsFile = runDirectory + "ITERS/it.10/10.events.txt.gz";
	//	private static String netFile = "../../detailedEval/Net/network-86-85-87-84_simplified.xml";
	//	private static String plansFile = runDirectory + "output_plans.xml.gz";

	private static String visum2hbefaRoadTypeFile = "../../detailedEval/testRuns/input/inputEmissions/road_types.txt";
	private static String hbefaDieselEmissionFactorsFile = "../../detailedEval/testRuns/input/inputEmissions/hbefa_emission_factors_urban_rural_MW.txt";
	private static String hbefaColdEmissionFactorsFile = "../../detailedEval/testRuns/input/inputEmissions/hbefa_coldstart_emission_factors.txt";

	private static String shapeDirectory = "../../detailedEval/Net/shapeFromVISUM/urbanSuburban/";
	private static String urbanShapeFile = shapeDirectory + "urbanAreas.shp";
	private static String suburbanShapeFile = shapeDirectory + "suburbanAreas.shp";

	private final Scenario scenario;

	public Main(){
		this.scenario = new ScenarioFactoryImpl().createScenario();
	}

	public static void main (String[] args) throws Exception{
		Main main = new Main();
		main.run(args);
	}

	private void run(String[] args) {

		// load the scenario
		loadScenario();
		Network network = scenario.getNetwork();
		Population population = scenario.getPopulation();

		// ?? was passiert hier ??
		HbefaTable hbefaTable = new HbefaTable();
		hbefaTable.makeHbefaTable(hbefaDieselEmissionFactorsFile);
		
		HbefaColdTable hbefaColdTable = new HbefaColdTable();
		hbefaColdTable.makeHbefaColdTable(hbefaColdEmissionFactorsFile);

		VisumObject[] roadTypes = new VisumObject[100];
		EmissionsPerEvent emissionFactor = new EmissionsPerEvent();

		LinkAndAgentAccountAnalysisModule linkAndAgentAccount = new LinkAndAgentAccountAnalysisModule(roadTypes, emissionFactor);
		linkAndAgentAccount.createRoadTypes(visum2hbefaRoadTypeFile);

		//create an event object
		EventsManager events = new EventsManagerImpl();	
		ColdstartAnalyseModul coldstartAccount = new ColdstartAnalyseModul ();
		//create the handler 
//		TravelTimeEventHandler handler = new TravelTimeEventHandler(network, hbefaTable.getHbefaTableWithSpeedAndEmissionFactor(), linkAndAgentAccount);
		TimeAndDistanceEventHandler handler = new TimeAndDistanceEventHandler(network,hbefaColdTable,coldstartAccount);
	
	
		
		//add the handler
//		events.addHandler(handler);
		events.addHandler(handler);
		
		

		//create the reader and read the file
		MatsimEventsReader matsimEventReader = new MatsimEventsReader(events);
		matsimEventReader.readFile(eventsFile);
		
		
		//warm emissions
		Map<Id, double[]> linkId2emissionsInGrammPerType = linkAndAgentAccount.getTotalEmissionsPerLink();
		Map<Id, double[]> personId2emissionsInGrammPerType = linkAndAgentAccount.getTotalEmissionsPerPerson();
		linkAndAgentAccount.printTotalEmissionTable(linkId2emissionsInGrammPerType, runDirectory + "emissionsPerLink.txt");
		linkAndAgentAccount.printTotalEmissionTable(personId2emissionsInGrammPerType, runDirectory + "emissionsPerPerson.txt");
		
		//coldstart emissions
		coldstartAccount.printColdEmissions(runDirectory + "coldemissionsPerPerson.txt");

		//further processing of emissions
		PersonFilter filter = new PersonFilter();
		Set<Feature> urbanShape = filter.readShape(urbanShapeFile);
		Population urbanPop = filter.getRelevantPopulation(population, urbanShape);
		Set<Feature> suburbanShape = filter.readShape(suburbanShapeFile);
		Population suburbanPop = filter.getRelevantPopulation(population, suburbanShape);

		List<Double> emissionType2AvgEmissionsUrbanArea = calculateAvgEmissionsPerTypeAndArea(urbanPop, personId2emissionsInGrammPerType);
		List<Double> emissionType2AvgEmissionsSuburbanArea = calculateAvgEmissionsPerTypeAndArea(suburbanPop, personId2emissionsInGrammPerType);

//		System.out.println(emissionType2AvgEmissionsUrbanArea);
//		System.out.println(emissionType2AvgEmissionsSuburbanArea);
		
	}
	


	private List<Double> calculateAvgEmissionsPerTypeAndArea(Population population, Map<Id, double[]> personId2emissionsInGrammPerType) {
		List<Double> emissionType2AvgEmissionsUrbanArea = new ArrayList<Double>();
		double totalCo2 = 0.0;
		double totalPM = 0.0;
		double totalNox = 0.0;
		double totalNo2 = 0.0;

		double populationSize = 1.0; //population.getPersons().size();

		for(Entry<Id, double[]> entry: personId2emissionsInGrammPerType.entrySet()){
			Id personId = entry.getKey();
			if(population.getPersons().containsKey(personId)){
				double co2 = entry.getValue()[7];
				double pm = entry.getValue()[9];
				double nox = entry.getValue()[6];
				double no2 = entry.getValue()[8];

				totalCo2 = totalCo2 + co2;
				totalPM = totalPM + pm;
				totalNox = totalNox + nox;
				totalNo2 = totalNo2 + no2;
			}
		}
		emissionType2AvgEmissionsUrbanArea.add(totalCo2 / populationSize);
		emissionType2AvgEmissionsUrbanArea.add(totalPM / populationSize);
		emissionType2AvgEmissionsUrbanArea.add(totalNox / populationSize);
		emissionType2AvgEmissionsUrbanArea.add(totalNo2 / populationSize);
		return emissionType2AvgEmissionsUrbanArea;
	}

	private void loadScenario() {
		Config config = scenario.getConfig();
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile);
		ScenarioLoader scenarioLoader = new ScenarioLoaderImpl(scenario) ;
		scenarioLoader.loadScenario() ;
	}
}	

