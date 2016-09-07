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
package playground.benjamin.scenarios.munich.analysis.kuhmo;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroupUtils;
import playground.benjamin.scenarios.zurich.analysis.MoneyEventHandler;
import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsAnalyzer;

/**
 * @author benjamin
 *
 */
public class MultiAnalyzer {
	private static final Logger logger = Logger.getLogger(MultiAnalyzer.class);

	//mobilTUM
//	static String runNr1 = "1";
//	static String runNr2 = "16";
//	static String runNr3 = "20";
	
	private static String [] cases = {
	
	//nectar
	"981",
	"982",
	"983",
	"984",
	"985",
			
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
	};
	
	//nectar
	private static String runDirectoryStub = "../../runs-svn/run";
//	private static String initialIterationNo = "1000";
	private static String finalIterationNo = "1500";
	
	//mobilTUM
//	private static String runDirectoryStub = "../../runs-svn/detEval/mobilTUMPaper/1pct/run";
//	private static String initialIterationNo = "1000";
//	private static String finalIterationNo = "1500";
	
	//latsis
//	private static String runDirectoryStub = "../../runs-svn/detEval/latsis/output/output_";
//	private static String initialIterationNo = "1000";
//	private static String finalIterationNo = "1500";
		
		
	
	private static String netFile;
	private static String configFile;
	private static String plansFile;
	private static String eventsFile;
	private static String emissionEventsFile;

	private final MultiAnalyzerWriter writer;
	private final Map<String, Map<Id, Double>> case2personId2carDistance;

	private final UserGroupUtils userGroupUtils;


	MultiAnalyzer(){
		this.writer = new MultiAnalyzerWriter(runDirectoryStub + cases[0] + "/");
		this.case2personId2carDistance = new HashMap<String, Map<Id,Double>>();
		this.userGroupUtils = new UserGroupUtils();
	}

	private void run() {
		
		for(String caseName : cases){
			
			String runDirectory = runDirectoryStub + caseName + "/";
			
			//mobilTUM
			netFile = runDirectory + caseName + ".output_network.xml.gz";
			configFile = runDirectory + caseName + ".output_config.xml.gz";
			
//			if(caseName.equals(cases[0])){
//				plansFile = runDirectory + "ITERS/it." + initialIterationNo + "/" + caseName + "." +  initialIterationNo + ".plans.xml.gz";
//				eventsFile = runDirectory + "ITERS/it." + initialIterationNo + "/" + caseName + "." +  initialIterationNo + ".events.xml.gz";
//				emissionEventsFile = runDirectory + "ITERS/it." + initialIterationNo + "/" + caseName + "." + initialIterationNo +  ".emission.events.xml.gz";
//			} else {
				plansFile = runDirectory + "ITERS/it." + finalIterationNo + "/" + caseName + "." +  finalIterationNo + ".plans.xml.gz";
				eventsFile = runDirectory + "ITERS/it." + finalIterationNo + "/" + caseName + "." +  finalIterationNo + ".events.xml.gz";
				emissionEventsFile = runDirectory + "ITERS/it." + finalIterationNo + "/" + caseName + "." + finalIterationNo +  ".emission.events.xml.gz";
//			}
			
			//latsis
//			netFile = runDirectory + "output_network.xml.gz";
//			configFile = runDirectory + "output_config.xml.gz";
//			plansFile = runDirectory + "ITERS/it." + finalIterationNo + "/" + finalIterationNo + ".plans.xml.gz";
//			eventsFile = runDirectory + "ITERS/it." + finalIterationNo + "/" + finalIterationNo + ".events.xml.gz";
//			emissionEventsFile = runDirectory + "ITERS/it." + finalIterationNo + "/" + finalIterationNo + ".emission.events.xml.gz";
			
//			calculateUserWelfareAndTollRevenueStatisticsByUserGroup(netFile, configFile, plansFile, eventsFile, caseName);
			calculateDistanceTimeStatisticsByUserGroup(netFile, eventsFile, caseName);
//			calculateEmissionStatisticsByUserGroup(emissionEventsFile, caseName);
		}
		calculateDistanceTimeStatisticsByUserGroupDifferences(case2personId2carDistance);
	}

	private void calculateDistanceTimeStatisticsByUserGroupDifferences(Map<String, Map<Id, Double>> case2personId2carDistance) {
		
		Map<Id, Double> personId2carDistanceBaseCase = case2personId2carDistance.get(cases[0]);
		
		for(int i=1; i<cases.length; i++){
			Map<Id, Double> personId2carDistanceDiff = new HashMap<Id, Double>();
			Map<Id, Double> personId2carDistancePolicyCase = case2personId2carDistance.get(cases[i]);
			
			for(Id personId : personId2carDistanceBaseCase.keySet()){
				Double baseCaseDist = personId2carDistanceBaseCase.get(personId);
				Double policyCaseDist;
				
				if(personId2carDistancePolicyCase.get(personId) == null){
					policyCaseDist = 0.0;
				} else {
					policyCaseDist = personId2carDistancePolicyCase.get(personId);
				}
				Double distDiff = policyCaseDist - baseCaseDist;
				personId2carDistanceDiff.put(personId, distDiff);
			}
			for(Id personId : personId2carDistancePolicyCase.keySet()){
				if(personId2carDistanceBaseCase.get(personId) == null){
					Double policyCaseDist = personId2carDistancePolicyCase.get(personId);
					personId2carDistanceDiff.put(personId, policyCaseDist);
				}
			}
			writer.setRunName(cases[i] + "-" + cases[0]);
			writer.writeDetailedCarDistanceInformation(personId2carDistanceDiff);
		}
	}

	private void calculateUserWelfareAndTollRevenueStatisticsByUserGroup(String netFile, String configFile, String plansFile, String eventsFile, String runName) {

		Scenario scenario = loadScenario(netFile, plansFile);
		Population pop = scenario.getPopulation();

		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsReaderXMLv1 eventsReader = new EventsReaderXMLv1(eventsManager);
		MoneyEventHandler moneyEventHandler = new MoneyEventHandler();
		eventsManager.addHandler(moneyEventHandler);
		eventsReader.readFile(eventsFile);

		Map<Id<Person>, Double> personId2Toll = moneyEventHandler.getPersonId2TollMap();
		
		// TODO: this could be probably done outside of the writer as follows:
//		Map<UserGroup, Double> userGroup2Size = userGroupUtils.getSizePerGroup(pop);
//		Map<UserGroup, Double> userGroup2TollPayers = userGroupUtils.getNrOfTollPayersPerGroup(personId2Toll);
//		Map<UserGroup, Double> userGroup2Welfare = userGroupUtils.getUserLogsumPerGroup(scenario);
//		Map<UserGroup, Double> userGroup2Toll = userGroupUtils.getTollPaidPerGroup(personId2Toll);

		writer.setRunName(runName);
		writer.writeWelfareTollInformation(configFile, pop, personId2Toll);
	}

	private void calculateEmissionStatisticsByUserGroup(String emissionFile, String runName) {
		EmissionsAnalyzer ema = new EmissionsAnalyzer(emissionFile);
		ema.init(null);
		ema.preProcessData();
		ema.postProcessData();
		
		Map<Id<Person>, SortedMap<String, Double>> person2totalEmissions = ema.getPerson2totalEmissions();
		SortedMap<UserGroup, SortedMap<String, Double>> group2totalEmissions = userGroupUtils.getEmissionsPerGroup(person2totalEmissions);

		writer.setRunName(runName);
		writer.writeEmissionInformation(group2totalEmissions);
	}

	private void calculateDistanceTimeStatisticsByUserGroup(String netFile, String eventsFile, String runName) {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc.getNetwork()).readFile(netFile);

		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsReaderXMLv1 eventsReader = new EventsReaderXMLv1(eventsManager);
		
		CarDistanceEventHandler carDistanceEventHandler = new CarDistanceEventHandler(sc.getNetwork());
		TravelTimePerModeEventHandler ttHandler = new TravelTimePerModeEventHandler();

		eventsManager.addHandler(carDistanceEventHandler);
		eventsManager.addHandler(ttHandler);
		eventsReader.readFile(eventsFile);
		
		Map<Id, Double> personId2carDistance = carDistanceEventHandler.getPersonId2CarDistance();
		Map<UserGroup, Double> userGroup2carTrips = carDistanceEventHandler.getUserGroup2carTrips();
		Map<String, Map<Id, Double>> mode2personId2TravelTime = ttHandler.getMode2personId2TravelTime();
		Map<UserGroup, Map<String, Double>> userGroup2mode2noOfTrips = ttHandler.getUserGroup2mode2noOfTrips();
		
		case2personId2carDistance.put(runName, personId2carDistance);
		
		logger.warn(runName + ": number of car users in distance map (users with departure events): " + personId2carDistance.size());
//		int depArrOnSameLinkCnt = carDistanceEventHandler.getDepArrOnSameLinkCnt().size();
//		logger.warn("number of car users with two activities followed one by another on the same link: +" + depArrOnSameLinkCnt);
//		int personIsDrivingADistance = 0;
//		for(Id personId : carDistanceEventHandler.getDepArrOnSameLinkCnt().keySet()){
//			if(personId2carDistance.get(personId) == null){
//				// do nothing
//			} else {
//				personIsDrivingADistance ++;
//			}
//		}
//		logger.warn(runName + ": number of car users with two activities followed one by another on the same link BUT driving to other acts: -" + personIsDrivingADistance);
		logger.warn(runName + ": number of car users in traveltime map (users with departure and arrival events): " + mode2personId2TravelTime.get(TransportMode.car).size());
		
		// TODO: this could be probably done outside of the writer (as for welfare above):
		writer.setRunName(runName);
		writer.writeAvgCarDistanceInformation(personId2carDistance, userGroup2carTrips);
		writer.writeDetailedCarDistanceInformation(personId2carDistance);
		writer.writeAvgTTInformation(mode2personId2TravelTime, userGroup2mode2noOfTrips);
	}

	private Scenario loadScenario(String netFile, String plansFile) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}

	public static void main(String[] args) {
		MultiAnalyzer ma = new MultiAnalyzer();
		ma.run();
	}
}