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
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.congestion.CongestionPersonAnalyzer;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;


/**
 * @author amit
 */
public class DelaysUserGroup {

	private static final double marginal_Utl_money=0.0789942;//0.062 //(for SiouxFalls =0.062 and for Munich =0.0789942);
	private static final double marginal_Utl_performing_sec=0.96/3600;
	private static final double marginal_Utl_traveling_car_sec=-0.0/3600;
	private static final double marginalUtlOfTravelTime = marginal_Utl_traveling_car_sec+marginal_Utl_performing_sec;
	private static final double vtts_car = marginalUtlOfTravelTime/marginal_Utl_money;
	
	
	public DelaysUserGroup() {
		userGroupToDelays  = new HashMap<UserGroup, Double>();
		for (UserGroup ug:UserGroup.values()) {
			this.userGroupToDelays.put(ug, 0.0);
		}
		scenario = loadScenario(populationFile, networkFile, configFile);
		userGrpToPopulation = new HashMap<UserGroup, Population>();
		lastIteration = scenario.getConfig().controler().getLastIteration();
	}

	private int lastIteration;
	private Logger logger = Logger.getLogger(DelaysUserGroup.class);
	private  String outputDir = "/Users/aagarwal/Desktop/ils4/agarwal/munich/output/1pct/ci/";/*"./output/run2/";*/
	private  String populationFile =outputDir+ "/output_plans.xml.gz";//"/network.xml";
	private  String networkFile =outputDir+ "/output_network.xml.gz";//"/network.xml";
	private  String configFile = outputDir+"/output_config.xml";//"/config.xml";//
	private Map<UserGroup, Double> userGroupToDelays;
	private Map<UserGroup, Population> userGrpToPopulation;
	private Map<Double, Map<Id, Double>> time2linkIdDelays = new HashMap<Double, Map<Id,Double>>();
	Scenario scenario;

	public static void main(String[] args) throws IOException {
		DelaysUserGroup data = new DelaysUserGroup();
		data.run();
	}

	private void run(){
		String eventFile = outputDir+"/ITERS/it."+lastIteration+"/"+lastIteration+".events.xml.gz";//"/events.xml";//
		CongestionPersonAnalyzer personAnalyzer = new CongestionPersonAnalyzer(configFile, eventFile,1);
		personAnalyzer.init(scenario);
		personAnalyzer.preProcessData();
		personAnalyzer.postProcessData();
		personAnalyzer.checkTotalDelayUsingAlternativeMethod();
		time2linkIdDelays = personAnalyzer.getCongestionPerPersonTimeInterval();
		
		getPopulationPerUserGroup();
		getTotalDelayPerUserGroup(time2linkIdDelays);
		writeTotalDelaysPerUserGroup(outputDir+"/analysis/userGrpDelays.txt");
	}

	private void writeTotalDelaysPerUserGroup(String outputFile){
		
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try{
			writer.write("userGroup \t delaySeconds \t delaysMoney \n");
			for(UserGroup ug:this.userGroupToDelays.keySet()){
				writer.write(ug+"\t"+this.userGroupToDelays.get(ug)+"\t"+this.userGroupToDelays.get(ug)*vtts_car+"\n");
			}
			writer.close();
		} catch (Exception e){
			throw new RuntimeException("Data is not written in the file. Reason - "+e);
		}
		logger.info("Finished Writing files to file "+outputFile);
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

	private void getTotalDelayPerUserGroup(Map<Double, Map<Id, Double>> delaysPerPersonPerTimeBin){
		for(double d:delaysPerPersonPerTimeBin.keySet()){
			for(Id personId : delaysPerPersonPerTimeBin.get(d).keySet()){
				UserGroup ug = getUserGroupFromPersonId(personId);
				double delaySoFar = userGroupToDelays.get(ug);
				double newDelays = delaySoFar+delaysPerPersonPerTimeBin.get(d).get(personId);
				userGroupToDelays.put(ug, newDelays);
			}
		}
	}
}
