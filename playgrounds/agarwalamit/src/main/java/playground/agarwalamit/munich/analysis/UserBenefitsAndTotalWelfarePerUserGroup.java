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
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.LoadMyScenarios;
import playground.agarwalamit.siouxFalls.userBenefits.UserBenefitsAnalyzerAA;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.vsp.analysis.modules.monetaryTransferPayments.MonetaryPaymentsAnalyzer;
import playground.vsp.analysis.modules.userBenefits.WelfareMeasure;

/**
 * @author amit
 */
public class UserBenefitsAndTotalWelfarePerUserGroup {

	private final Logger logger = Logger.getLogger(UserBenefitsAndTotalWelfarePerUserGroup.class);
	private int lastIteration;
	private  String outputDir = "/Users/aagarwal/Desktop/ils4/agarwal/munich/output/1pct_rSeed/eci/";/*"./output/run2/";*/
	private  String populationFile =outputDir+ "/output_plans.xml.gz";//"/network.xml";
	private  String networkFile =outputDir+ "/output_network.xml.gz";//"/network.xml";
	private  String configFile = outputDir+"/output_config.xml";//"/config.xml";//

	private SortedMap<UserGroup, Population> userGrpToPopulation = new TreeMap<UserGroup, Population>();
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
		this.scenario = LoadMyScenarios.loadScenarioFromPlansNetworkAndConfig(this.populationFile, this.networkFile,this.configFile);
		this.lastIteration = LoadMyScenarios.getLastIteration(this.configFile);
		getPopulationPerUserGroup();
		getAllUserBenefits((ScenarioImpl)this.scenario);
		getMonetaryPayment((ScenarioImpl)this.scenario);
		SortedMap<UserGroup, Double> userGroupToUserWelfare_utils = getParametersPerUserGroup(this.personId2UserWelfare_utils);
		SortedMap<UserGroup, Double> userGroupToUserWelfare_money = getParametersPerUserGroup(this.personId2MonetarizedUserWelfare);
		SortedMap<UserGroup, Double> userGroupToTotalPayment = getParametersPerUserGroup(this.personId2MonetaryPayments);

		String outputFile = this.outputDir+"/analysis/userGrpWelfareAndTollPayments.txt";
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try {
			writer.write("UserGroup \t userWelfareUtils \t userWelfareMoney \t tollPayments \n");
			for(UserGroup ug : userGroupToTotalPayment.keySet()){
				writer.write(ug+"\t"+userGroupToUserWelfare_utils.get(ug)+"\t"+userGroupToUserWelfare_money.get(ug)+"\t"+userGroupToTotalPayment.get(ug)+"\n");
			}
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Data is not written into a File. Reason : "+e);
		}
		this.logger.info("Finished Writing data to file "+outputFile);		
	}

	private SortedMap<UserGroup, Double> getParametersPerUserGroup(Map<Id, Double> inputMap){
		SortedMap<UserGroup, Double> outMap = new TreeMap<UserGroup, Double>();

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

	private void getAllUserBenefits(ScenarioImpl scenarioImpl){
		this.logger.info("User welfare will be calculated using welfare measure as "+wm.toString());
		UserBenefitsAnalyzerAA userBenefitsAnalyzer = new UserBenefitsAnalyzerAA();
		userBenefitsAnalyzer.init(scenarioImpl, this.wm);
		userBenefitsAnalyzer.preProcessData();
		userBenefitsAnalyzer.postProcessData();
		userBenefitsAnalyzer.writeResults(this.outputDir+"/analysis/");
		this.personId2UserWelfare_utils = userBenefitsAnalyzer.getPersonId2UserWelfare_utils();
		this.personId2MonetarizedUserWelfare = userBenefitsAnalyzer.getPersonId2MonetarizedUserWelfare();
	}

	private void getMonetaryPayment(ScenarioImpl scenarioImpl){
		MonetaryPaymentsAnalyzer paymentsAnalyzer = new MonetaryPaymentsAnalyzer();
		paymentsAnalyzer.init(scenarioImpl);
		paymentsAnalyzer.preProcessData();

		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = paymentsAnalyzer.getEventHandler();

		for(EventHandler eh : handler){
			events.addHandler(eh);
		}

		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(this.outputDir+"/ITERS/it."+this.lastIteration+"/"+this.lastIteration+".events.xml.gz");

		paymentsAnalyzer.postProcessData();
		paymentsAnalyzer.writeResults(this.outputDir+"/analysis/");
		this.personId2MonetaryPayments = paymentsAnalyzer.getPersonId2amount();
	}

	private SortedMap<UserGroup, Population> getPopulationPerUserGroup(){
		PersonFilter pf = new PersonFilter();
		for(UserGroup ug : UserGroup.values()){
			this.userGrpToPopulation.put(ug, pf.getPopulation(this.scenario.getPopulation(), ug));
		}
		return this.userGrpToPopulation;
	}

	private UserGroup getUserGroupFromPersonId(Id personId){
		UserGroup usrgrp = null;
		for(UserGroup ug:this.userGrpToPopulation.keySet()){
			if(this.userGrpToPopulation.get(ug).getPersons().get(personId)!=null) {
				usrgrp = ug;
				break;
			}
		}
		return usrgrp;
	}
}
