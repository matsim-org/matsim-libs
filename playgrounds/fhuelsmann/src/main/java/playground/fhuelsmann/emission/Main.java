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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

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
	private static String hbefaAverageFleetEmissionFactorsFile = "../../detailedEval/testRuns/input/inputEmissions/hbefa_emission_factors_urban_rural_MW.txt";
	private static String hbefaColdEmissionFactorsFile = "../../detailedEval/testRuns/input/inputEmissions/hbefa_coldstart_emission_factors.txt";

	private static String shapeDirectory = "../../detailedEval/Net/shapeFromVISUM/urbanSuburban/";
	private static String urbanShapeFile = shapeDirectory + "urbanAreas.shp";
	private static String suburbanShapeFile = shapeDirectory + "suburbanAreas.shp";
	private static String shapeDistrictDirectory = "../../detailedEval/Net/shapeFromVISUM/stadtbezirke/";
	private static String altstadtShapeFile = shapeDistrictDirectory + "Altstadt.shp";
	private static String aubingShapeFile = shapeDistrictDirectory + "Aubing.shp";
	

	private final Scenario scenario;

	public Main(){
		Config config = ConfigUtils.createConfig();
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
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
		hbefaTable.makeHbefaTable(hbefaAverageFleetEmissionFactorsFile);
		
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
		TravelTimeEventHandler handler = new TravelTimeEventHandler(network, hbefaTable.getHbefaTableWithSpeedAndEmissionFactor(), linkAndAgentAccount);
		TimeAndDistanceEventHandler handler2 = new TimeAndDistanceEventHandler(network,hbefaColdTable,coldstartAccount);
	
	
		
		//add the handler
		events.addHandler(handler);
		events.addHandler(handler2);
		
		

		//create the reader and read the file
		MatsimEventsReader matsimEventReader = new MatsimEventsReader(events);
		matsimEventReader.readFile(eventsFile);
		
		
		//warm emissions
		Map<Id, double[]> linkId2emissionsInGrammPerType = linkAndAgentAccount.getTotalEmissionsPerLink();
		Map<Id, double[]> personId2emissionsInGrammPerType = linkAndAgentAccount.getTotalEmissionsPerPerson();
		linkAndAgentAccount.printTotalEmissionTable(linkId2emissionsInGrammPerType, runDirectory + "emissionsPerLink.txt");
		linkAndAgentAccount.printTotalEmissionTable(personId2emissionsInGrammPerType, runDirectory + "emissionsPerPerson.txt");
		
//		coldstart emissions
		coldstartAccount.printColdEmissions(runDirectory + "coldemissionsPerPerson.txt");
		Map<Id, Map<String, Double>> coldEmissionsPerson = coldstartAccount.getColdEmissionsPerson();
		
		warmAndColdEmissions(personId2emissionsInGrammPerType,coldEmissionsPerson, runDirectory + "coldAndWarmEmissionsPerPerson.txt");
		
		//further processing of emissions
		PersonFilter filter = new PersonFilter();
		Set<Feature> urbanShape = filter.readShape(urbanShapeFile);
		Population urbanPop = filter.getRelevantPopulation(population, urbanShape);
		Set<Feature> suburbanShape = filter.readShape(suburbanShapeFile);
		Population suburbanPop = filter.getRelevantPopulation(population, suburbanShape);
		Set<Feature> altstadtShape = filter.readShape(altstadtShapeFile);
		Population altstadtPop = filter.getRelevantPopulation(population, altstadtShape);
		Set<Feature> aubingShape = filter.readShape(aubingShapeFile);
		Population aubingPop = filter.getRelevantPopulation(population, aubingShape);

		List<Double> emissionType2AvgEmissionsUrbanArea = calculateAvgEmissionsPerTypeAndArea(urbanPop, personId2emissionsInGrammPerType,coldEmissionsPerson);
		List<Double> emissionType2AvgEmissionsSuburbanArea = calculateAvgEmissionsPerTypeAndArea(suburbanPop, personId2emissionsInGrammPerType,coldEmissionsPerson);
		List<Double> emissionType2AvgEmissionsAltstadtArea = calculateAvgEmissionsPerTypeAndArea(altstadtPop, personId2emissionsInGrammPerType,coldEmissionsPerson);
		List<Double> emissionType2AvgEmissionsAubingArea = calculateAvgEmissionsPerTypeAndArea(aubingPop, personId2emissionsInGrammPerType,coldEmissionsPerson);

		System.out.println(emissionType2AvgEmissionsUrbanArea);
		System.out.println(emissionType2AvgEmissionsSuburbanArea);
		System.out.println(emissionType2AvgEmissionsAltstadtArea);
		System.out.println(emissionType2AvgEmissionsAubingArea);
		
/*		LinkFilter linkfilter = new LinkFilter(network);
		Set<Feature> urbanShapeLink = linkfilter.readShape(urbanShapeFile);
		Network urbanNetwork = linkfilter.getRelevantNetwork(urbanShapeLink);
		Set<Feature> suburbanShapeLink = linkfilter.readShape(urbanShapeFile);
		Network suburbanNetwork = linkfilter.getRelevantNetwork(suburbanShapeLink);
		
		
		List<Double> emissionType2AvgEmissionsUrbanAreaLink = calculateAvgEmissionsPerTypeAndAreaLink(urbanNetwork, linkId2emissionsInGrammPerType);
		List<Double> emissionType2AvgEmissionsSuburbanAreaLink = calculateAvgEmissionsPerTypeAndAreaLink(suburbanNetwork, linkId2emissionsInGrammPerType);
		
		System.out.println("+++++++++++++++++++++++++++++++++++"+emissionType2AvgEmissionsUrbanAreaLink);
		System.out.println(emissionType2AvgEmissionsSuburbanAreaLink);*/
	}
	
/*	private List<Double> calculateAvgEmissionsPerTypeAndAreaLink(Network network, Map<Id, double[]> linkId2emissionsInGrammPerType) {
		List<Double> emissionType2AvgEmissionsUrbanAreaLink = new ArrayList<Double>();
		double totalCo2 = 0.0;
		double totalPM = 0.0;
		double totalNox = 0.0;
		double totalNo2 = 0.0;

		double populationSize = 1.0; //population.getPersons().size();

		for(Entry<Id, double[]> entry: linkId2emissionsInGrammPerType.entrySet()){
			Id linkId = entry.getKey();
			if(network.getLinks().containsKey(linkId)){
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
		emissionType2AvgEmissionsUrbanAreaLink.add(totalCo2 / populationSize);
		emissionType2AvgEmissionsUrbanAreaLink.add(totalPM / populationSize);
		emissionType2AvgEmissionsUrbanAreaLink.add(totalNox / populationSize);
		emissionType2AvgEmissionsUrbanAreaLink.add(totalNo2 / populationSize);
		return emissionType2AvgEmissionsUrbanAreaLink;
	}*/

	private List<Double> calculateAvgEmissionsPerTypeAndArea(Population population, Map<Id, double[]> personId2emissionsInGrammPerType,Map<Id, Map<String,Double>> coldEmissionsPerson) {
		List<Double> emissionType2AvgEmissionsUrbanArea = new ArrayList<Double>();
		double totalCo2 = 0.0;
		double totalMassFuel = 0.0;
		double totalPM = 0.0;
		double totalNox = 0.0;
		double totalNo2 = 0.0;

		double populationSize = 1.0; //population.getPersons().size();

		for(Entry<Id, double[]> entry: personId2emissionsInGrammPerType.entrySet()){
			Id personId = entry.getKey();
			if(population.getPersons().containsKey(personId)){
				double co2 = entry.getValue()[7]; //only warm emissions
				double massfuel =  coldEmissionsPerson.get(entry.getKey()).get("FC") + entry.getValue()[5];
				double pm = coldEmissionsPerson.get(entry.getKey()).get("PM") + entry.getValue()[9];
				double nox = coldEmissionsPerson.get(entry.getKey()).get("NOx") + entry.getValue()[6];
				double no2 = coldEmissionsPerson.get(entry.getKey()).get("NO2") + entry.getValue()[8];

				totalCo2 = totalCo2 + co2;
				totalMassFuel = totalMassFuel + massfuel;
				totalPM = totalPM + pm;
				totalNox = totalNox + nox;
				totalNo2 = totalNo2 + no2;
			}
		}
		emissionType2AvgEmissionsUrbanArea.add(totalCo2 / populationSize);//only warm emissions
		emissionType2AvgEmissionsUrbanArea.add(totalMassFuel / populationSize);
		emissionType2AvgEmissionsUrbanArea.add(totalPM / populationSize);
		emissionType2AvgEmissionsUrbanArea.add(totalNox / populationSize);
		emissionType2AvgEmissionsUrbanArea.add(totalNo2 / populationSize);
		return emissionType2AvgEmissionsUrbanArea;
	}

	private void loadScenario() {
		Config config = scenario.getConfig();
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile);
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(scenario) ;
		scenarioLoader.loadScenario() ;
	}

	//**************warmAndColdStartEmissions*********************
	public static void warmAndColdEmissions ( Map<Id, double[]> warmEmissionsPerson, Map<Id, Map<String,Double>> coldEmissionsPerson,String outputFile ){
		
		try{
			FileWriter fstream = new FileWriter(outputFile);			
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("Id \t CO \t FCav \t FCfr \t HC \t NO2av \t NO2fr  \t NOxav  \t NOxfr  \t PMav  \t PMfr \n");  
		
			for(Entry<Id,double[]> personId :  warmEmissionsPerson.entrySet()){
				
				double CO = coldEmissionsPerson.get(personId.getKey()).get("CO");
				double FCav =  coldEmissionsPerson.get(personId.getKey()).get("FC") +  personId.getValue()[0];
				double FCfr =  coldEmissionsPerson.get(personId.getKey()).get("FC") + personId.getValue()[5];
				double HC = coldEmissionsPerson.get(personId.getKey()).get("HC");
				double NO2av = coldEmissionsPerson.get(personId.getKey()).get("NO2") + personId.getValue()[3];
				double NO2fr = coldEmissionsPerson.get(personId.getKey()).get("NO2") + personId.getValue()[8];
				double NOxav = coldEmissionsPerson.get(personId.getKey()).get("NOx") + personId.getValue()[1] ;
				double NOxfr = coldEmissionsPerson.get(personId.getKey()).get("NOx") + personId.getValue()[6];
				double PMav = coldEmissionsPerson.get(personId.getKey()).get("PM") + personId.getValue()[4] ;
				double PMfr = coldEmissionsPerson.get(personId.getKey()).get("PM") + personId.getValue()[9];
				out.append(personId +"\t" + CO + "\t"+ FCav + "\t"+ FCfr +"\t"+ HC+ "\t" + NO2av+ "\t"+ NO2fr+  "\t"+ NOxav  +"\t"+ NOxfr+  "\t"+ PMav+  "\t"+ PMfr + "\n");
			}
			out.close();
			System.out.println("Finished writing emission file to " + outputFile);

		}catch (Exception e){
			System.err.println("Error: " + e.getMessage());				
			}
			
		}
}	

