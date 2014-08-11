/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.munich.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.siouxFalls.userBenefits.UserBenefitsAnalyzerAA;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.vsp.analysis.modules.monetaryTransferPayments.MonetaryPaymentsAnalyzer;
import playground.vsp.analysis.modules.userBenefits.WelfareMeasure;

/**
 * @author amit
 */
public class UserBenefitsAndTotalWelfarePerUserGroup {

	public UserBenefitsAndTotalWelfarePerUserGroup() {
		
	}

	private final static Logger logger = Logger.getLogger(UserBenefitsAndTotalWelfarePerUserGroup.class);
	private int lastIteration;
	private  String outputDir = "/Users/aagarwal/Desktop/ils4/agarwal/munich/output/1pct/ci/";/*"./output/run2/";*/
	private  String populationFile =outputDir+ "/output_plans.xml.gz";//"/network.xml";
	private  String networkFile =outputDir+ "/output_network.xml.gz";//"/network.xml";
	private  String configFile = outputDir+"/output_config.xml";//"/config.xml";//

	private Map<UserGroup, Population> userGrpToPopulation = new HashMap<UserGroup, Population>();
	private Map<Id, Double> personId2UserWelfare_utils = new HashMap<Id,Double>();
	private Map<Id, Double> personId2MonetarizedUserWelfare= new HashMap<Id,Double>();
	private Map<Id, Double> personId2MonetaryPayments = new HashMap<Id,Double>();
	private Scenario scenario;

	private final WelfareMeasure wm = WelfareMeasure.SELECTED;

	public static void main(String[] args) {
		UserBenefitsAndTotalWelfarePerUserGroup ubtwug = new  UserBenefitsAndTotalWelfarePerUserGroup();
		ubtwug.run();
	}

	private void run(){
		scenario = loadScenario(populationFile, networkFile,configFile);
		lastIteration = scenario.getConfig().controler().getLastIteration();
		getPopulationPerUserGroup();
		getAllUserBenefits((ScenarioImpl)scenario);
		getMonetaryPayment((ScenarioImpl)scenario);
		Map<UserGroup, Double> userGroupToUserWelfare_utils = getParametersPerUserGroup(personId2UserWelfare_utils);
		Map<UserGroup, Double> userGroupToUserWelfare_money = getParametersPerUserGroup(personId2MonetarizedUserWelfare);
		Map<UserGroup, Double> userGroupToTotalPayment = getParametersPerUserGroup(personId2MonetaryPayments);


		BufferedWriter writer = IOUtils.getBufferedWriter(outputDir+"/analysis/userGrpWelfareAndTollPayments.txt");
		try {
			writer.write("UserGroup \t userWelfareUtils \t userWelfareMoney \t tollPayments \n");
			for(UserGroup ug : userGroupToTotalPayment.keySet()){
				writer.write(ug+"\t"+userGroupToUserWelfare_utils.get(ug)+"\t"+userGroupToUserWelfare_money.get(ug)+"\t"+userGroupToTotalPayment.get(ug)+"\n");
			}
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Data is not written into a File. Reason : "+e);
		}
	}

	private Map<UserGroup, Double> getParametersPerUserGroup(Map<Id, Double> inputMap){
		Map<UserGroup, Double> outMap = new HashMap<UserGroup, Double>();

		for(UserGroup ug : UserGroup.values()){
			outMap.put(ug, 0.0);
		}
		
		for(Id id:inputMap.keySet()){
			UserGroup ug = getUserGroupFromPersonId(id);
			double valueSoFar = outMap.get(ug);
			double newValue = inputMap.get(id)+valueSoFar;
			outMap.put(ug, newValue);
		}
		return outMap;
	}

	private Scenario loadScenario(String populationFile, String networkFile, String configFile) {
		Config config = new Config();
		config.addCoreModules();
		MatsimConfigReader configReader = new MatsimConfigReader(config);
		configReader.readFile(configFile);
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}

	public void getAllUserBenefits(ScenarioImpl scenarioImpl){
		logger.info("User welfare will be calculated using welfare measure as "+wm.toString());
		UserBenefitsAnalyzerAA userBenefitsAnalyzer = new UserBenefitsAnalyzerAA();
		userBenefitsAnalyzer.init(scenarioImpl, wm);
		userBenefitsAnalyzer.preProcessData();
		userBenefitsAnalyzer.postProcessData();
		userBenefitsAnalyzer.writeResults(outputDir+"/analysis/");
		personId2UserWelfare_utils = userBenefitsAnalyzer.getPersonId2UserWelfare_utils();
		personId2MonetarizedUserWelfare = userBenefitsAnalyzer.getPersonId2MonetarizedUserWelfare();
	}

	public void getMonetaryPayment(ScenarioImpl scenarioImpl){
		MonetaryPaymentsAnalyzer paymentsAnalyzer = new MonetaryPaymentsAnalyzer();
		paymentsAnalyzer.init(scenarioImpl);
		paymentsAnalyzer.preProcessData();

		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = paymentsAnalyzer.getEventHandler();

		for(EventHandler eh : handler){
			events.addHandler(eh);
		}

		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(outputDir+"/ITERS/it."+lastIteration+"/"+lastIteration+".events.xml.gz");

		paymentsAnalyzer.postProcessData();
		paymentsAnalyzer.writeResults(outputDir+"/analysis/");
		personId2MonetaryPayments = paymentsAnalyzer.getPersonId2amount();
	}

	private Map<UserGroup, Population> getPopulationPerUserGroup(){
		PersonFilter pf = new PersonFilter();
		for(UserGroup ug : UserGroup.values()){
			userGrpToPopulation.put(ug, pf.getPopulation(scenario.getPopulation(), ug));
		}
		return userGrpToPopulation;
	}

	private UserGroup getUserGroupFromPersonId(Id personId){
		UserGroup usrgrp = null;
		for(UserGroup ug:userGrpToPopulation.keySet()){
			if(userGrpToPopulation.get(ug).getPersons().get(personId)!=null) {
				usrgrp = ug;
				break;
			}
		}
		return usrgrp;
	}
}
