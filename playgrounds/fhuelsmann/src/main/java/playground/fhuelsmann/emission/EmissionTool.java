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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesImpl;

import playground.benjamin.szenarios.munich.UrbanSuburbanAnalyzer;
import playground.fhuelsmann.emission.objects.VisumObject;


public class EmissionTool {
	
	private static final Logger logger = Logger.getLogger(EmissionTool.class);

	// INPUT
	private static String runDirectory = "../../detailedEval/policies/mobilTUM/baseCase/";
	private static String eventsFile = runDirectory + "ITERS/it.0/0.events.txt.gz";
	private static String netFile = runDirectory + "output_network.xml.gz";
	private static String plansFile = runDirectory + "ITERS/it.0/0.plans.xml.gz";

	//	private static String runDirectory = "../../detailedEval/testRuns/output/1pct/v0-default/run22/";
	//	private static String eventsFile = runDirectory + "ITERS/it.500/500.events.txt.gz";
	//	private static String netFile = runDirectory + "output_network.xml.gz";
	//	private static String plansFile = runDirectory + "output_plans.xml.gz";

	private static String visum2hbefaRoadTypeFile = "../../detailedEval/testRuns/input/inputEmissions/road_types.txt";
	private static String visum2hbefaRoadTypeTraffcSituationFile = "../../detailedEval/testRuns/input/inputEmissions/road_types_trafficSituation.txt";
	private static String hbefaAverageFleetEmissionFactorsFile = "../../detailedEval/testRuns/input/inputEmissions/hbefa_emission_factors_urban_rural_MW.txt";
	private static String hbefaAverageFleetHdvEmissionFactorsFile = "../../detailedEval/testRuns/input/inputEmissions/hbefa_emission_factors_urban_rural_MW_hdv.txt";
	private static String hbefaColdEmissionFactorsFile = "../../detailedEval/testRuns/input/inputEmissions/hbefa_coldstart_emission_factors.txt";
	private static String hbefaHotFile = "../../detailedEval/emissions/hbefa/EFA_HOT_SubSegm_PC.txt";
	private static String vehicleFile="../../detailedEval/pop/140k-synthetische-personen/vehicles.xml";

	private static final ArrayList<String> listOfPollutant = new ArrayList<String>();

	private static String shapeDirectory = "../../detailedEval/Net/shapeFromVISUM/urbanSuburban/";
	private static String urbanShapeFile = shapeDirectory + "urbanAreas.shp";
	private static String suburbanShapeFile = shapeDirectory + "suburbanAreas.shp";

	private final Scenario scenario;

	public EmissionTool(){
		Config config = ConfigUtils.createConfig();
		this.scenario = ScenarioUtils.createScenario(config);
	}

	public static void main (String[] args) throws Exception{
		EmissionTool emissionTool = new EmissionTool();
		emissionTool.run(args);
	}

	private void run(String[] args) {

		/*	listOfPollutant.add("Benzene");
		listOfPollutant.add("CH4");
		listOfPollutant.add("CO");
		listOfPollutant.add("CO(rep.)");
		listOfPollutant.add("CO2(total)");
		listOfPollutant.add("FC");
		listOfPollutant.add("HC");
		listOfPollutant.add("N2O");
		listOfPollutant.add("NH3");
		listOfPollutant.add("NMHC")*/;
		listOfPollutant.add("NO2");
	/*	listOfPollutant.add("NOX");
		listOfPollutant.add("Pb");
		listOfPollutant.add("PM");
		listOfPollutant.add("PN");
		listOfPollutant.add("SO2");*/

		// load the scenario
		loadScenario();
		Network network = scenario.getNetwork();
		Population population = scenario.getPopulation();

		// read different hbefa tables
		HbefaTable hbefaTable = new HbefaTable();
		hbefaTable.makeHbefaTable(hbefaAverageFleetEmissionFactorsFile);
		HbefaTable hbefaHdvTable = new HbefaTable();
		hbefaHdvTable.makeHbefaTable(hbefaAverageFleetHdvEmissionFactorsFile);
		HbefaColdEmissionTable hbefaColdTable = new HbefaColdEmissionTable();
		hbefaColdTable.makeHbefaColdTable(hbefaColdEmissionFactorsFile);
		HbefaHot hbefaHot = new HbefaHot();
		hbefaHot.makeHbefaHot(hbefaHotFile);

		Vehicles vehicles = new VehiclesImpl();
		VehicleReaderV1 vehicleReader = new VehicleReaderV1(vehicles);
		vehicleReader.readFile(vehicleFile);

		VisumObject[] visumObject = new VisumObject[100];
		EmissionsPerEvent emissionsPerEvent = new EmissionsPerEvent();
		WarmEmissionAnalysisModule warmEmissionAnalysisModule = new WarmEmissionAnalysisModule(visumObject, emissionsPerEvent,hbefaHot);
		warmEmissionAnalysisModule.createRoadTypes(visum2hbefaRoadTypeFile);
		warmEmissionAnalysisModule.createRoadTypesTafficSituation(visum2hbefaRoadTypeTraffcSituationFile);
		ColdEmissionAnalysisModule coldEmissionAnalysisModule = new ColdEmissionAnalysisModule ();

		// create an event object
		//		EventsManager eventsManager = new EventsManagerImpl();	
		EventsManager eventsManager = EventsUtils.createEventsManager();
		// create the handler 
		WarmEmissionHandler warmEmissionHandler = new WarmEmissionHandler(vehicles, network, hbefaTable.getHbefaTableWithSpeedAndEmissionFactor(), hbefaHdvTable.getHbefaTableWithSpeedAndEmissionFactor(), warmEmissionAnalysisModule);//,hbefaHot.getHbefaHot());
		warmEmissionHandler.setListOfPollutant(listOfPollutant);
		ColdEmissionHandler coldEmissionHandler = new ColdEmissionHandler(network, hbefaColdTable, coldEmissionAnalysisModule);
		// add the handler
		eventsManager.addHandler(warmEmissionHandler);
		eventsManager.addHandler(coldEmissionHandler);
		//create the reader and read the file
		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
		matsimEventsReader.readFile(eventsFile);

		// =======================================================================================================		
		// warm emissions
		Map<Id, double[]> personId2WarmEmissionsInGrammPerType = warmEmissionAnalysisModule.getWarmEmissionsPerPerson();
//		Map<Id, double[]> linkId2WarmEmissionsInGrammPerType = warmEmissionAnalysisModule.getWarmEmissionsPerLink();

		// coldstart emissions
		Map<Id, Map<String, Double>> personId2ColdEmissions = coldEmissionAnalysisModule.getColdEmissionsPerPerson();

		// sum up emissions
//		Map<Id, double[]> personId2TotalEmissionsInGrammPerType = getTotalEmissions(personId2WarmEmissionsInGrammPerType, personId2ColdEmissions);

		// print output files
		EmissionPrinter printer = new EmissionPrinter(runDirectory);
		printer.printHomeLocation2Emissions(population, personId2WarmEmissionsInGrammPerType, "EmissionsPerHomeLocationWarm.txt");
		printer.printColdEmissionTable(personId2ColdEmissions, "EmissionsPerPersonCold.txt");
//		printer.printHomeLocation2Emissions(population, personId2TotalEmissionsInGrammPerType, "EmissionsPerHomeLocationTotal.txt");

		// =======================================================================================================	
		//further processing of emissions
		UrbanSuburbanAnalyzer usa = new UrbanSuburbanAnalyzer(this.scenario);
		Set<Feature> urbanShape = usa.readShape(urbanShapeFile);
		Population urbanPop = usa.getRelevantPopulation(population, urbanShape);
		Set<Feature> suburbanShape = usa.readShape(suburbanShapeFile);
		Population suburbanPop = usa.getRelevantPopulation(population, suburbanShape);
		
//		List<Double> emissionType2AvgEmissionsUrbanArea = calculateAvgEmissionsPerTypeAndArea(urbanPop, personId2TotalEmissionsInGrammPerType);
//		List<Double> emissionType2AvgEmissionsSuburbanArea = calculateAvgEmissionsPerTypeAndArea(suburbanPop, personId2TotalEmissionsInGrammPerType);
		
//		List<Double> emissionType2AvgEmissionsUrbanArea = calculateAvgEmissionsPerTypeAndArea(urbanPop, personId2WarmEmissionsInGrammPerType);
//		List<Double> emissionType2AvgEmissionsSuburbanArea = calculateAvgEmissionsPerTypeAndArea(suburbanPop, personId2WarmEmissionsInGrammPerType);

//		System.out.println("urbanArea: " + emissionType2AvgEmissionsUrbanArea);
//		System.out.println("suburbanArea: " + emissionType2AvgEmissionsSuburbanArea);
	}

	private List<Double> calculateAvgEmissionsPerTypeAndArea(Population population, Map<Id, double[]> personId2emissionsInGrammPerType) {
		List<Double> avgEmissionsPerTypeandArea = new ArrayList<Double>();
		double totalFc = 0.0;
		double totalNox = 0.0;
		double totalCo2 = 0.0;
		double totalNo2 = 0.0;
		double totalPM = 0.0;

		Integer populationSize = population.getPersons().size();
		logger.warn(populationSize.toString());

		for(Person person : population.getPersons().values()){
			Id personId = person.getId();
			if(personId2emissionsInGrammPerType.containsKey(personId)){
				double fc =  personId2emissionsInGrammPerType.get(personId)[5];
				double nox = personId2emissionsInGrammPerType.get(personId)[6];
				double co2 = personId2emissionsInGrammPerType.get(personId)[7];
				double no2 = personId2emissionsInGrammPerType.get(personId)[8];
				double pm = personId2emissionsInGrammPerType.get(personId)[9];
				
				totalFc = totalFc + fc;
				totalNox = totalNox + nox;
				totalCo2 = totalCo2 + co2;
				totalNo2 = totalNo2 + no2;
				totalPM = totalPM + pm;
			}
		}
		avgEmissionsPerTypeandArea.add(totalFc / populationSize);
		avgEmissionsPerTypeandArea.add(totalNox / populationSize);
		avgEmissionsPerTypeandArea.add(totalCo2 / populationSize);
		avgEmissionsPerTypeandArea.add(totalNo2 / populationSize);
		avgEmissionsPerTypeandArea.add(totalPM / populationSize);
		return avgEmissionsPerTypeandArea;
	}

	private Map<Id, double[]> getTotalEmissions(Map<Id, double[]> personId2WarmEmissionsInGrammPerType,	Map<Id, Map<String, Double>> personId2ColdEmissions) {
		Map<Id, double[]> personId2totalEmissions = new HashMap<Id, double[]>();
	
		for(Entry<Id, double[]> entry : personId2WarmEmissionsInGrammPerType.entrySet()){
			Id personId = entry.getKey();
			double[] warmEmissions = entry.getValue();
			double[] totalEmissions = new double[10];
	
			if(personId2ColdEmissions.containsKey(personId)){
				// average speed based
				totalEmissions[0] = warmEmissions[0] + personId2ColdEmissions.get(personId).get("FC");
				totalEmissions[1] = warmEmissions[1] + personId2ColdEmissions.get(personId).get("NOx");
				totalEmissions[2] = warmEmissions[2] + (personId2ColdEmissions.get(personId).get("FC")*0.865 -
									personId2ColdEmissions.get(personId).get("CO")*0.429 -
									personId2ColdEmissions.get(personId).get("HC")*0.866)/0.273; //TODO: not directly //TODO: CO2 not directly available for cold emissions; try through fc!
				totalEmissions[3] = warmEmissions[3] + personId2ColdEmissions.get(personId).get("NO2");
				totalEmissions[4] = warmEmissions[4] + personId2ColdEmissions.get(personId).get("PM");

				// fraction based
				totalEmissions[5] = warmEmissions[5] + personId2ColdEmissions.get(personId).get("FC");
				totalEmissions[6] = warmEmissions[6] + personId2ColdEmissions.get(personId).get("NOx");
				totalEmissions[7] = warmEmissions[7] + (personId2ColdEmissions.get(personId).get("FC")*0.865 -
						personId2ColdEmissions.get(personId).get("CO")*0.429 -
						personId2ColdEmissions.get(personId).get("HC")*0.866)/0.273; //TODO: CO2 not directly available for cold emissions; try through fc!
				totalEmissions[8] = warmEmissions[8] + personId2ColdEmissions.get(personId).get("NO2");
				totalEmissions[9] = warmEmissions[9] + personId2ColdEmissions.get(personId).get("PM");
			}
			else{
				totalEmissions = warmEmissions;
			}
			personId2totalEmissions.put(personId, totalEmissions);
		}
		return personId2totalEmissions;
	}

	private void loadScenario() {
		Config config = scenario.getConfig();
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile);
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(scenario) ;
		scenarioLoader.loadScenario() ;
	}
}