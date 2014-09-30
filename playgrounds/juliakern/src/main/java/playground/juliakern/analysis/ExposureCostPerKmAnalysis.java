/* *********************************************************************** *
 * project: org.matsim.*
 * MulitAnalyzer.java
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
package playground.juliakern.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.benjamin.scenarios.munich.analysis.kuhmo.CarDistanceEventHandler;
import playground.benjamin.scenarios.munich.analysis.kuhmo.MultiAnalyzerWriter;
import playground.benjamin.scenarios.munich.analysis.kuhmo.TravelTimePerModeEventHandler;
import playground.benjamin.scenarios.zurich.analysis.MoneyEventHandler;
import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsAnalyzer;

/**
 * @author benjamin
 *
 */
public class ExposureCostPerKmAnalysis {
	private static final Logger logger = Logger.getLogger(ExposureCostPerKmAnalysis.class);

	//mobilTUM
//	static String runNr1 = "1";
//	static String runNr2 = "16";
//	static String runNr3 = "20";
	
	private static String [] cases = {
	
	//nectar
//	"981",
//	"982",
//	"983",
//	"984",
//	"985",
			
	//mobilTUM
//	runNr1,
//	runNr2,
//	runNr3
		
	//latsis	
//	"baseCase_ctd_newCode" ,
//	"policyCase_zone30" ,
//	"policyCase_pricing_newCode",
////	"policyCase_pricing_modeOnly",
//	"policyCase_pricing_fuelEff_2.5pct",
//	"policyCase_pricing_fuelEff_5pct",
//	"policyCase_pricing_fuelEff_7.5pct",
//	"policyCase_pricing_fuelEff_10pct",
//	"policyCase_pricing_fuelEff_20pct"
	
	// exposure internalization - base case and zone 30 have no money payments
	//"baseCase_ctd" ,
	//"policyCase_zone30" ,
	"policyCase_pricing",
	"policyCase_exposurePricing"	
	
	};
	
//	//nectar
//	private static String runDirectoryStub = "../../runs-svn/run";
////	private static String initialIterationNo = "1000";
//	private static String finalIterationNo = "1500";
	
	//mobilTUM
//	private static String runDirectoryStub = "../../runs-svn/detEval/mobilTUMPaper/1pct/run";
//	private static String initialIterationNo = "1000";
//	private static String finalIterationNo = "1500";
	
	//latsis
//	private static String runDirectoryStub = "../../runs-svn/detEval/latsis/output/output_";
//	private static String initialIterationNo = "1000";
//	private static String finalIterationNo = "1500";
		
	// exposure internalization
	private static String runDirectoryStub = "../../runs-svn/detEval/exposureInternalization/internalize1pct/output/output_";
//	private static String initialIterationNo = "1000";
	private static String finalIterationNo = "1500";	
	
	private final String munichShapeFile = "../../detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";
	
	private static String netFile;
	private static String configFile;
	private static String plansFile;
	private static String eventsFile;
	private static String emissionEventsFile;
	String outputDir;

	private final MultiAnalyzerWriter writer;
	private final Map<String, Map<Id, Double>> case2personId2carDistance;

	private final UserGroupUtils userGroupUtils;

	private HashMap<String, List<Double>> case2averageCostvalues;
	

	ExposureCostPerKmAnalysis(){
		this.writer = new MultiAnalyzerWriter(runDirectoryStub + cases[0] + "/");
		this.case2personId2carDistance = new HashMap<String, Map<Id,Double>>();
		this.userGroupUtils = new UserGroupUtils();
		this.case2averageCostvalues = new HashMap<String, List<Double>>();
		this.outputDir= runDirectoryStub + cases[0] + "/";
	}

	private void run() {
		
		System.out.println("---- starting up");
		
		for(String caseName : cases){
			
			String runDirectory = runDirectoryStub + caseName + "/";
			
			//mobilTUM
			netFile = runDirectory + caseName + ".output_network.xml.gz";
			configFile = runDirectory + caseName + ".output_config.xml.gz";
			
			//exposure
			netFile = runDirectory +  "output_network.xml.gz";
			configFile = runDirectory + caseName + ".output_config.xml.gz";
			
//			if(caseName.equals(cases[0])){
//				plansFile = runDirectory + "ITERS/it." + initialIterationNo + "/" + caseName + "." +  initialIterationNo + ".plans.xml.gz";
//				eventsFile = runDirectory + "ITERS/it." + initialIterationNo + "/" + caseName + "." +  initialIterationNo + ".events.xml.gz";
//				emissionEventsFile = runDirectory + "ITERS/it." + initialIterationNo + "/" + caseName + "." + initialIterationNo +  ".emission.events.xml.gz";
//			} else {
				plansFile = runDirectory + "ITERS/it." + finalIterationNo + "/" +  finalIterationNo + ".plans.xml.gz";
				eventsFile = runDirectory + "ITERS/it." + finalIterationNo + "/" +  finalIterationNo + ".events.xml.gz";
				emissionEventsFile = runDirectory + "ITERS/it." + finalIterationNo + "/"  +  finalIterationNo +  ".emission.events.xml.gz";
//			}
			
			//latsis
//			netFile = runDirectory + "output_network.xml.gz";
//			configFile = runDirectory + "output_config.xml.gz";
//			plansFile = runDirectory + "ITERS/it." + finalIterationNo + "/" + finalIterationNo + ".plans.xml.gz";
//			eventsFile = runDirectory + "ITERS/it." + finalIterationNo + "/" + finalIterationNo + ".events.xml.gz";
//			emissionEventsFile = runDirectory + "ITERS/it." + finalIterationNo + "/" + finalIterationNo + ".emission.events.xml.gz";
			
//			calculateUserWelfareAndTollRevenueStatisticsByUserGroup(netFile, configFile, plansFile, eventsFile, caseName);
//			calculateDistanceTimeStatisticsByUserGroup(netFile, eventsFile, caseName);
//			calculateEmissionStatisticsByUserGroup(emissionEventsFile, caseName);
				System.out.println("---- starting toll per km by usre group");
			calculateTollPerKmByUserGroup(netFile, eventsFile, caseName);
		}
		
//		calculateDistanceTimeStatisticsByUserGroupDifferences(case2personId2carDistance);
	}

	private void calculateTollPerKmByUserGroup(String networkFile, String eventFile, String caseName) {
		
		System.out.println("---- starting up b ");
		Scenario scenario = loadScenario(networkFile, plansFile);
		Population pop = scenario.getPopulation();

		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsReaderXMLv1 eventsReader = new EventsReaderXMLv1(eventsManager);
		TollPerKmHandler tollperKmHandler = new TollPerKmHandler(scenario.getNetwork(), munichShapeFile);
		eventsManager.addHandler(tollperKmHandler);
		eventsReader.parse(eventsFile);
		
		// combine link leave events with money events to paid toll per km 
		System.out.println("----- get person id 2 list ");
		tollperKmHandler.calculateAverages();
		Map<Id, List<Double>> personId2tpk = tollperKmHandler.getPersonId2ListOfTollPerKM();
		Map<Id, List<Double>> personId2toll = tollperKmHandler.getPersonId2ListOfToll();
		Map<Id, List<Double>> personId2km = tollperKmHandler.getPersonId2ListOfKm();
		
		// put into different lists for each user group
		Set<UserGroup> userGroups = userGroupUtils.getUserGroups(pop);
		//userGroupUtils.personFilter.isPersonIdFromUserGroup(personId, userGroup);
		Map<UserGroup, List<Double>> usergroup2listOfcostPerKm = new HashMap<UserGroup, List<Double>>();
		Map<UserGroup, List<Double>> usergroup2listOfToll = new HashMap<UserGroup, List<Double>>();
		Map<UserGroup, List<Double>> usergroup2listOfKm = new HashMap<UserGroup, List<Double>>();
		
		for(UserGroup ug: userGroups){
			usergroup2listOfcostPerKm.put(ug, new ArrayList<Double>());
			usergroup2listOfKm.put(ug, new ArrayList<Double>());
			usergroup2listOfToll.put(ug, new ArrayList<Double>());
		}
		
		for(Id personId: personId2tpk.keySet()){
			UserGroup currUg = null;
			for(UserGroup ug: userGroups){
				if(userGroupUtils.personFilter.isPersonIdFromUserGroup(personId, ug)){
					currUg = ug;
					break;
				}
			}
			if (currUg != null) {
				for (Double tollPerKm : personId2tpk.get(personId)) {
					usergroup2listOfcostPerKm.get(currUg).add(tollPerKm);
				}
				for(Double toll: personId2toll.get(personId)){
					usergroup2listOfToll.get(currUg).add(toll);
				}
				for(Double km: personId2km.get(personId)){
					usergroup2listOfKm.get(currUg).add(km);
				}
			}
		}
		// write into 1? 4? files?
		AnalysisWriter aw = new AnalysisWriter();
		aw.writeCostPerKmInformation(usergroup2listOfcostPerKm, outputDir + "analysis/inMunich" + caseName);
		
		//TODO calc total km, total paid tolls, toll/km
		
		for(UserGroup ug: userGroups){
			// total tolls
			Double ttolls = 0.0, tkm=0.0; 
			for(Double toll: usergroup2listOfToll.get(ug)){
				ttolls += toll;
			}
			for(Double km: usergroup2listOfKm.get(ug)){
				tkm+= km;
			}
			logger.info(caseName + " User group " + ug.toString() + " paid tolls [Euro] " + ttolls + " total distance [km] " + tkm + " average toll/distance [EuroCt/km] " + (ttolls/tkm*100));
		}
	}

//	private void calculateDistanceTimeStatisticsByUserGroupDifferences(Map<String, Map<Id, Double>> case2personId2carDistance) {
//		
//		Map<Id, Double> personId2carDistanceBaseCase = case2personId2carDistance.get(cases[0]);
//		
//		for(int i=1; i<cases.length; i++){
//			Map<Id, Double> personId2carDistanceDiff = new HashMap<Id, Double>();
//			Map<Id, Double> personId2carDistancePolicyCase = case2personId2carDistance.get(cases[i]);
//			
//			for(Id personId : personId2carDistanceBaseCase.keySet()){
//				Double baseCaseDist = personId2carDistanceBaseCase.get(personId);
//				Double policyCaseDist;
//				
//				if(personId2carDistancePolicyCase.get(personId) == null){
//					policyCaseDist = 0.0;
//				} else {
//					policyCaseDist = personId2carDistancePolicyCase.get(personId);
//				}
//				Double distDiff = policyCaseDist - baseCaseDist;
//				personId2carDistanceDiff.put(personId, distDiff);
//			}
//			for(Id personId : personId2carDistancePolicyCase.keySet()){
//				if(personId2carDistanceBaseCase.get(personId) == null){
//					Double policyCaseDist = personId2carDistancePolicyCase.get(personId);
//					personId2carDistanceDiff.put(personId, policyCaseDist);
//				}
//			}
//			writer.setRunName(cases[i] + "-" + cases[0]);
//			writer.writeDetailedCarDistanceInformation(personId2carDistanceDiff);
//		}
//	}
//
//	private void calculateUserWelfareAndTollRevenueStatisticsByUserGroup(String netFile, String configFile, String plansFile, String eventsFile, String runName) {
//
//		Scenario scenario = loadScenario(netFile, plansFile);
//		Population pop = scenario.getPopulation();
//
//		EventsManager eventsManager = EventsUtils.createEventsManager();
//		EventsReaderXMLv1 eventsReader = new EventsReaderXMLv1(eventsManager);
//		MoneyEventHandler moneyEventHandler = new MoneyEventHandler();
//		eventsManager.addHandler(moneyEventHandler);
//		eventsReader.parse(eventsFile);
//
//		Map<Id, Double> personId2Toll = moneyEventHandler.getPersonId2TollMap();
//		
//		// TODO: this could be probably done outside of the writer as follows:
////		Map<UserGroup, Double> userGroup2Size = userGroupUtils.getSizePerGroup(pop);
////		Map<UserGroup, Double> userGroup2TollPayers = userGroupUtils.getNrOfTollPayersPerGroup(personId2Toll);
////		Map<UserGroup, Double> userGroup2Welfare = userGroupUtils.getUserLogsumPerGroup(scenario);
////		Map<UserGroup, Double> userGroup2Toll = userGroupUtils.getTollPaidPerGroup(personId2Toll);
//
//		writer.setRunName(runName);
//		writer.writeWelfareTollInformation(configFile, pop, personId2Toll);
//	}
//
//	private void calculateEmissionStatisticsByUserGroup(String emissionFile, String runName) {
//		EmissionsAnalyzer ema = new EmissionsAnalyzer(emissionFile);
//		ema.init(null);
//		ema.preProcessData();
//		ema.postProcessData();
//		
//		Map<Id, SortedMap<String, Double>> person2totalEmissions = ema.getPerson2totalEmissions();
//		SortedMap<UserGroup, SortedMap<String, Double>> group2totalEmissions = userGroupUtils.getEmissionsPerGroup(person2totalEmissions);
//
//		writer.setRunName(runName);
//		writer.writeEmissionInformation(group2totalEmissions);
//	}
//
//	private void calculateDistanceTimeStatisticsByUserGroup(String netFile, String eventsFile, String runName) {
//		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		new MatsimNetworkReader(sc).readFile(netFile);
//
//		EventsManager eventsManager = EventsUtils.createEventsManager();
//		EventsReaderXMLv1 eventsReader = new EventsReaderXMLv1(eventsManager);
//		
//		CarDistanceEventHandler carDistanceEventHandler = new CarDistanceEventHandler(sc.getNetwork());
//		TravelTimePerModeEventHandler ttHandler = new TravelTimePerModeEventHandler();
//
//		eventsManager.addHandler(carDistanceEventHandler);
//		eventsManager.addHandler(ttHandler);
//		eventsReader.parse(eventsFile);
//		
//		Map<Id, Double> personId2carDistance = carDistanceEventHandler.getPersonId2CarDistance();
//		Map<UserGroup, Double> userGroup2carTrips = carDistanceEventHandler.getUserGroup2carTrips();
//		Map<String, Map<Id, Double>> mode2personId2TravelTime = ttHandler.getMode2personId2TravelTime();
//		Map<UserGroup, Map<String, Double>> userGroup2mode2noOfTrips = ttHandler.getUserGroup2mode2noOfTrips();
//		
//		case2personId2carDistance.put(runName, personId2carDistance);
//		
//		logger.warn(runName + ": number of car users in distance map (users with departure events): " + personId2carDistance.size());
////		int depArrOnSameLinkCnt = carDistanceEventHandler.getDepArrOnSameLinkCnt().size();
////		logger.warn("number of car users with two activities followed one by another on the same link: +" + depArrOnSameLinkCnt);
////		int personIsDrivingADistance = 0;
////		for(Id personId : carDistanceEventHandler.getDepArrOnSameLinkCnt().keySet()){
////			if(personId2carDistance.get(personId) == null){
////				// do nothing
////			} else {
////				personIsDrivingADistance ++;
////			}
////		}
////		logger.warn(runName + ": number of car users with two activities followed one by another on the same link BUT driving to other acts: -" + personIsDrivingADistance);
//		logger.warn(runName + ": number of car users in traveltime map (users with departure and arrival events): " + mode2personId2TravelTime.get(TransportMode.car).size());
//		
//		// TODO: this could be probably done outside of the writer (as for welfare above):
//		writer.setRunName(runName);
//		writer.writeAvgCarDistanceInformation(personId2carDistance, userGroup2carTrips);
//		writer.writeDetailedCarDistanceInformation(personId2carDistance);
//		writer.writeAvgTTInformation(mode2personId2TravelTime, userGroup2mode2noOfTrips);
//	}

	private Scenario loadScenario(String netFile, String plansFile) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}

	public static void main(String[] args) {
		logger.info("----- starting main");
		ExposureCostPerKmAnalysis ma = new ExposureCostPerKmAnalysis();
		ma.run();
	}
}