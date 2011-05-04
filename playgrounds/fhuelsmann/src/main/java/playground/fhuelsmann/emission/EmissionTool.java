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

package playground.fhuelsmann.emission;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesImpl;

import playground.fhuelsmann.emission.objects.VisumObject;

public class EmissionTool {

	// INPUT
		private static String runDirectory = "../../detailedEval/testRuns/output/run8/";
		private static String eventsFile = runDirectory + "100.events.txt.gz";
		//		private static String netFile = "../../detailedEval/Net/network-86-85-87-84_simplified.xml";
		private static String netFile = runDirectory + "output_network.xml.gz";
		private static String plansFile = runDirectory + "output_plans.xml.gz";

/*	private static String runDirectory = "../../detailedEval/testRuns/output/1pct/v0-default/run12/";
	private static String eventsFile = runDirectory + "ITERS/it.100/100.events.txt.gz";
	private static String netFile = runDirectory + "output_network.xml.gz";
	private static String plansFile = runDirectory + "output_plans.xml.gz";*/

	private static String visum2hbefaRoadTypeFile = "../../detailedEval/testRuns/input/inputEmissions/road_types.txt";
	private static String hbefaAverageFleetEmissionFactorsFile = "../../detailedEval/testRuns/input/inputEmissions/hbefa_emission_factors_urban_rural_MW.txt";
	private static String hbefaAverageFleetHdvEmissionFactorsFile = "../../detailedEval/testRuns/input/inputEmissions/hbefa_emission_factors_urban_rural_MW_hdv.txt";
	private static String hbefaColdEmissionFactorsFile = "../../detailedEval/testRuns/input/inputEmissions/hbefa_coldstart_emission_factors.txt";
	private static String hbefaHotFile = "../../detailedEval/emissions/hbefa/EFA_HOT_SubSegm_PC.txt";
	private static String vehicleFile="../../detailedEval/pop/befragte-personen/vehicles.xml";
	private static String householdsFile="../../detailedEval/pop/befragte-personen/households.xml";
	
	
	
	//	private static String shapeDirectory = "../../detailedEval/Net/shapeFromVISUM/urbanSuburban/";
	//	private static String urbanShapeFile = shapeDirectory + "urbanAreas.shp";
	//	private static String suburbanShapeFile = shapeDirectory + "suburbanAreas.shp";
	//	private static String shapeDistrictDirectory = "../../detailedEval/Net/shapeFromVISUM/stadtbezirke/";
	//	private static String altstadtShapeFile = shapeDistrictDirectory + "Altstadt.shp";
	//	private static String aubingShapeFile = shapeDistrictDirectory + "Aubing.shp";


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
		
		Households households = new HouseholdsImpl();
		HouseholdsReaderV10 reader = new HouseholdsReaderV10(households);
		reader.readFile(householdsFile);

		VisumObject[] visumObject = new VisumObject[100];
		EmissionsPerEvent emissionsPerEvent = new EmissionsPerEvent();

		WarmEmissionAnalysisModule warmEmissionAnalysisModule = new WarmEmissionAnalysisModule(visumObject, emissionsPerEvent);
		warmEmissionAnalysisModule.createRoadTypes(visum2hbefaRoadTypeFile);
		ColdEmissionAnalysisModule coldEmissionAnalysisModule = new ColdEmissionAnalysisModule ();

		// create an event object
//		EventsManager eventsManager = new EventsManagerImpl();	
		EventsManager eventsManager = EventsUtils.createEventsManager();
		// create the handler 
		WarmEmissionHandler warmEmissionHandler = new WarmEmissionHandler(households, vehicles, network, hbefaTable.getHbefaTableWithSpeedAndEmissionFactor(), hbefaHdvTable.getHbefaTableWithSpeedAndEmissionFactor(), warmEmissionAnalysisModule);
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
		Map<Id, double[]> linkId2WarmEmissionsInGrammPerType = warmEmissionAnalysisModule.getWarmEmissionsPerLink();
		
		//vehicles
		Map<Id, Id> personId2VehicleId = getVehicleIdFromHouseholds(households);
		Map<Id, Id> vehicleId2VehicleType = getVehicleTypeFromVehicleId(vehicles);
// coldstart emissions
		Map<Id, Map<String, Double>> personId2ColdEmissions = coldEmissionAnalysisModule.getColdEmissionsPerPerson();

		// sum up emissions
		Map<Id, double[]> personId2TotalEmissionsInGrammPerType = getTotalEmissions(personId2WarmEmissionsInGrammPerType, personId2ColdEmissions);

		// print output files
		EmissionPrinter printer = new EmissionPrinter(runDirectory);
		printer.printHomeLocation2Emissions(population, personId2WarmEmissionsInGrammPerType, "EmissionsPerHomeLocationWarm.txt");
		printer.printHomeLocation2Emissions(population, personId2TotalEmissionsInGrammPerType, "EmissionsPerHomeLocationTotal.txt");

//		printer.printEmissionTable(personId2WarmEmissionsInGrammPerType, "EmissionsPerPersonWarm.txt");
//		printer.printEmissionTable(linkId2WarmEmissionsInGrammPerType, "EmissionsPerLinkWarm.txt");
//
		printer.printColdEmissionTable(personId2ColdEmissions, "EmissionsPerPersonCold.txt");
//		
//		printer.printEmissionTable(personId2TotalEmissionsInGrammPerType, "EmissionsPerPersonTotal");
	}		

	private Map<Id, double[]> getTotalEmissions(Map<Id, double[]> personId2WarmEmissionsInGrammPerType,	Map<Id, Map<String, Double>> personId2ColdEmissions) {
		Map<Id, double[]> personId2totalEmissions = new HashMap<Id, double[]>();
		double[] totalEmissions = new double[9];
		
		for(Entry<Id, double[]> entry : personId2WarmEmissionsInGrammPerType.entrySet()){
			Id personId = entry.getKey();
			double[] warmEmissions = entry.getValue();
			
			if(personId2ColdEmissions.containsKey(personId)){
				totalEmissions[0] = warmEmissions[0] + personId2ColdEmissions.get(personId).get("FC");
				double nox_As = warmEmissions[1] + personId2ColdEmissions.get(personId).get("NOx");
				double co2_As = warmEmissions[2]; //TODO: not directly available for cold emissions; try through fc!
				double no2_As = warmEmissions[3] + personId2ColdEmissions.get(personId).get("NO2");
				double pm_As = warmEmissions[4] + personId2ColdEmissions.get(personId).get("PM");
				
				double fc_Fr = warmEmissions[5] + personId2ColdEmissions.get(personId).get("FC");
				double nox_Fr = warmEmissions[6] + personId2ColdEmissions.get(personId).get("NOx");
				double co2_Fr = warmEmissions[7]; //TODO: not directly available for cold emissions; try through fc!
				double no2_Fr = warmEmissions[8] + personId2ColdEmissions.get(personId).get("NO2");
				double pm_Fr = warmEmissions[9] + personId2ColdEmissions.get(personId).get("PM");
				
			}
			else{
				totalEmissions = warmEmissions;
			}
			personId2totalEmissions.put(personId, totalEmissions);
		}
		return personId2totalEmissions;
	}
/*	private Map<Id, Map<String, Double>> getTotalEmissions(Map<Id, double[]> personId2WarmEmissionsInGrammPerType,	Map<Id, Map<String, Double>> personId2ColdEmissions) {
		Map<Id, Map<String, Double>> personId2totalEmissions = new HashMap<Id, Map<String, Double>>();
		Map<String, Double> emissionType2Value = new HashMap<String, Double>();
		
		for(Entry<Id, double[]> entry : personId2WarmEmissionsInGrammPerType.entrySet()){
			Id personId = entry.getKey();
			double[] warmEmissions = entry.getValue();
			
			double fc_As = warmEmissions[0] + personId2ColdEmissions.get(personId).get("FC");
			double nox_As = warmEmissions[1] + personId2ColdEmissions.get(personId).get("NOx");
			double co2_As = warmEmissions[2]; //TODO: not directly available for cold emissions; try through fc!
			double no2_As = warmEmissions[3] + personId2ColdEmissions.get(personId).get("NO2");
			double pm_As = warmEmissions[4] + personId2ColdEmissions.get(personId).get("PM");
			
			double fc_Fr = warmEmissions[5] + personId2ColdEmissions.get(personId).get("FC");
			double nox_Fr = warmEmissions[6] + personId2ColdEmissions.get(personId).get("NOx");
			double co2_Fr = warmEmissions[7]; //TODO: not directly available for cold emissions; try through fc!
			double no2_Fr = warmEmissions[8] + personId2ColdEmissions.get(personId).get("NO2");
			double pm_Fr = warmEmissions[9] + personId2ColdEmissions.get(personId).get("PM");
			
			emissionType2Value.put("FC_As", fc_As);
			emissionType2Value.put("NOx_As", nox_As);
			emissionType2Value.put("CO2_As", co2_As);
			emissionType2Value.put("NO2_As", no2_As);
			emissionType2Value.put("PM_As", pm_As);
			
			emissionType2Value.put("FC_Fr", fc_Fr);
			emissionType2Value.put("NOx_Fr", nox_Fr);
			emissionType2Value.put("CO2_Fr", co2_Fr);
			emissionType2Value.put("NO2_Fr", no2_Fr);
			emissionType2Value.put("PM_Fr", pm_Fr);
			
			personId2totalEmissions.put(personId, emissionType2Value);
		}
		
		return personId2totalEmissions;
	}*/
	
	private Map<Id, Id> getVehicleTypeFromVehicleId (Vehicles vehicle) {
		Map<Id,Id> vehicleId2VehicleType = new TreeMap<Id, Id>();
		
		//iterating over every vehicle veh in order to get vehicleIds and vehcile type 
		for (Vehicle veh : vehicle.getVehicles().values()){
			Id vehicleId = veh.getId();
			Id vehicleType = veh.getType().getId();
			vehicleId2VehicleType.put(vehicleId, vehicleType);	
//			System.out.print("\n ++++++++++++++++++++++++++++++++++++"+vehicleId +"  "+ vehicleType);
		}
		
		return vehicleId2VehicleType;
	}
	
	private Map<Id, Id> getVehicleIdFromHouseholds(Households households) {
		Map<Id,Id> personId2VehicleId = new TreeMap<Id, Id>();
		
		//iterating over every household hh in order to get personIds and personal income 
	
		for (Household hh : households.getHouseholds().values()) {
			Id personId = hh.getMemberIds().get(0);
			if (hh.getVehicleIds() != null && !hh.getVehicleIds().isEmpty()){
				Id vehicleId = hh.getVehicleIds().get(0);
				personId2VehicleId.put(personId, vehicleId);
				System.out.print("\n ****************************"+personId +"  "+ vehicleId);}
		
		}
		return personId2VehicleId;
	}

/*	private Map<Id, Row> PersonId2VehicleType(Map<Id, Id> personId2VehicleId, Map<Id, Id> vehicleId2VehicleType) {
		
		Map<Id, Row> result = new TreeMap<Id, Row>();
		for(Id id : personId2VehicleId.keySet()){						
			
			//Row can be extended to all the needed information
			Row row = new Row();
			row.setPersonId(id);
			row.setVehicleId(vehicleId.get(id));
			row.setVehicleType(vehicleType.get(id));

			result.put(id, row);
		}
		return result;
	}*/

	private void loadScenario() {
		Config config = scenario.getConfig();
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile);
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(scenario) ;
		scenarioLoader.loadScenario() ;
	}
}	

//	//further processing of emissions
//	PersonFilter filter = new PersonFilter();
//	Set<Feature> urbanShape = filter.readShape(urbanShapeFile);
//	Population urbanPop = filter.getRelevantPopulation(population, urbanShape);
//	Set<Feature> suburbanShape = filter.readShape(suburbanShapeFile);
//	Population suburbanPop = filter.getRelevantPopulation(population, suburbanShape);
//	Set<Feature> altstadtShape = filter.readShape(altstadtShapeFile);
//	Population altstadtPop = filter.getRelevantPopulation(population, altstadtShape);
//	Set<Feature> aubingShape = filter.readShape(aubingShapeFile);
//	Population aubingPop = filter.getRelevantPopulation(population, aubingShape);
//
//	List<Double> emissionType2AvgEmissionsUrbanArea = calculateAvgEmissionsPerTypeAndArea(urbanPop, personId2emissionsInGrammPerType,coldEmissionsPerson);
//	List<Double> emissionType2AvgEmissionsSuburbanArea = calculateAvgEmissionsPerTypeAndArea(suburbanPop, personId2emissionsInGrammPerType,coldEmissionsPerson);
//	List<Double> emissionType2AvgEmissionsAltstadtArea = calculateAvgEmissionsPerTypeAndArea(altstadtPop, personId2emissionsInGrammPerType,coldEmissionsPerson);
//	List<Double> emissionType2AvgEmissionsAubingArea = calculateAvgEmissionsPerTypeAndArea(aubingPop, personId2emissionsInGrammPerType,coldEmissionsPerson);
//
//	System.out.println("PersonbasedEmissions" +emissionType2AvgEmissionsUrbanArea);
//	System.out.println(emissionType2AvgEmissionsSuburbanArea);
//	System.out.println(emissionType2AvgEmissionsAltstadtArea);
//	System.out.println(emissionType2AvgEmissionsAubingArea);
//	
//	LinkFilter linkfilter = new LinkFilter(network);
//	Set<Feature> urbanShapeLink = linkfilter.readShape(urbanShapeFile);
//	Network urbanNetwork = linkfilter.getRelevantNetwork(urbanShapeLink);
//	Set<Feature> suburbanShapeLink = linkfilter.readShape(suburbanShapeFile);
//	Network suburbanNetwork = linkfilter.getRelevantNetwork(suburbanShapeLink);
//	Set<Feature> altstadtShapeLink = linkfilter.readShape(altstadtShapeFile);
//	Network alstadtNetwork = linkfilter.getRelevantNetwork(altstadtShapeLink);
//	Set<Feature> aubingShapeLink = linkfilter.readShape(aubingShapeFile);
//	Network aubingNetwork = linkfilter.getRelevantNetwork(aubingShapeLink);
//	
//	
//	List<Double> emissionType2AvgEmissionsUrbanAreaLink = calculateAvgEmissionsPerTypeAndAreaLink(urbanNetwork, linkId2emissionsInGrammPerType);
//	List<Double> emissionType2AvgEmissionsSuburbanAreaLink = calculateAvgEmissionsPerTypeAndAreaLink(suburbanNetwork, linkId2emissionsInGrammPerType);
//	List<Double> emissionType2AvgEmissionsaltstadtNetwork = calculateAvgEmissionsPerTypeAndAreaLink(alstadtNetwork, linkId2emissionsInGrammPerType);
//	List<Double> emissionType2AvgEmissionsAubingNetwork = calculateAvgEmissionsPerTypeAndAreaLink(aubingNetwork, linkId2emissionsInGrammPerType);
//	
//	System.out.println("LinkbasedEmissions"+emissionType2AvgEmissionsUrbanAreaLink);
//	System.out.println(emissionType2AvgEmissionsSuburbanAreaLink);
//	System.out.println("Alstadt " + emissionType2AvgEmissionsaltstadtNetwork);
//	System.out.println("Aubing " +emissionType2AvgEmissionsAubingNetwork);

//private List<Double> calculateAvgEmissionsPerTypeAndAreaLink(Network network, Map<Id, double[]> linkId2emissionsInGrammPerType) {
//	List<Double> emissionType2AvgEmissionsUrbanAreaLink = new ArrayList<Double>();
//	double totalCo2 = 0.0;
//	double totalMassFuel = 0.0;
//	double totalPM = 0.0;
//	double totalNox = 0.0;
//	double totalNo2 = 0.0;
//
//	double populationSize = 1.0; //population.getPersons().size();
//
//	for(Entry<Id, double[]> entry: linkId2emissionsInGrammPerType.entrySet()){
//		Id linkId = entry.getKey();
//		if(network.getLinks().containsKey(linkId)){
//			double co2 = entry.getValue()[7]; //only warm emissions
//			double massfuel = entry.getValue()[5];
//			double pm = entry.getValue()[9];
//			double nox = entry.getValue()[6];
//			double no2 = entry.getValue()[8];
//
//			totalCo2 = totalCo2 + co2;
//			totalMassFuel = totalMassFuel + massfuel;
//			totalPM = totalPM + pm;
//			totalNox = totalNox + nox;
//			totalNo2 = totalNo2 + no2;
//		}
//	}
//	emissionType2AvgEmissionsUrbanAreaLink.add(totalCo2 / populationSize);
//	emissionType2AvgEmissionsUrbanAreaLink.add(totalMassFuel / populationSize);
//	emissionType2AvgEmissionsUrbanAreaLink.add(totalPM / populationSize);
//	emissionType2AvgEmissionsUrbanAreaLink.add(totalNox / populationSize);
//	emissionType2AvgEmissionsUrbanAreaLink.add(totalNo2 / populationSize);
//	return emissionType2AvgEmissionsUrbanAreaLink;
//}
//
//private List<Double> calculateAvgEmissionsPerTypeAndArea(Population population, Map<Id, double[]> personId2emissionsInGrammPerType,Map<Id, Map<String,Double>> coldEmissionsPerson) {
//	List<Double> emissionType2AvgEmissionsUrbanArea = new ArrayList<Double>();
//	double totalCo2 = 0.0;
//	double totalMassFuel = 0.0;
//	double totalPM = 0.0;
//	double totalNox = 0.0;
//	double totalNo2 = 0.0;
//
//	double populationSize = 1.0; //population.getPersons().size();
//
//	for(Entry<Id, double[]> entry: personId2emissionsInGrammPerType.entrySet()){
//		Id personId = entry.getKey();
//		if(population.getPersons().containsKey(personId)){
//			double co2 = entry.getValue()[7]; //only warm emissions
//			double massfuel =  coldEmissionsPerson.get(entry.getKey()).get("FC") + entry.getValue()[5];
//			double pm = coldEmissionsPerson.get(entry.getKey()).get("PM") + entry.getValue()[9];
//			double nox = coldEmissionsPerson.get(entry.getKey()).get("NOx") + entry.getValue()[6];
//			double no2 = coldEmissionsPerson.get(entry.getKey()).get("NO2") + entry.getValue()[8];
//
//			totalCo2 = totalCo2 + co2;
//			totalMassFuel = totalMassFuel + massfuel;
//			totalPM = totalPM + pm;
//			totalNox = totalNox + nox;
//			totalNo2 = totalNo2 + no2;
//		}
//	}
//	emissionType2AvgEmissionsUrbanArea.add(totalCo2 / populationSize);//only warm emissions
//	emissionType2AvgEmissionsUrbanArea.add(totalMassFuel / populationSize);
//	emissionType2AvgEmissionsUrbanArea.add(totalPM / populationSize);
//	emissionType2AvgEmissionsUrbanArea.add(totalNox / populationSize);
//	emissionType2AvgEmissionsUrbanArea.add(totalNo2 / populationSize);
//	return emissionType2AvgEmissionsUrbanArea;
//}

