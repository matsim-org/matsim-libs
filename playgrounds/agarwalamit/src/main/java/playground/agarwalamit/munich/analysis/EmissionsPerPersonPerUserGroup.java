/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionsPerPersonAnalysis.java
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
package playground.agarwalamit.munich.analysis;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.contrib.emissions.utils.EmissionUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.LoadMyScenarios;
import playground.agarwalamit.analysis.emission.EmissionCostFactors;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsAnalyzer;

/**
 * @author amit
 *
 */
public class EmissionsPerPersonPerUserGroup {

	private final Logger logger = Logger.getLogger(EmissionsPerPersonPerUserGroup.class);

	public EmissionsPerPersonPerUserGroup() {
		this.scenario = LoadMyScenarios.loadScenarioFromNetworkPlansAndConfig(this.networkFile, this.populationFile, this.configFile);

		for(UserGroup ug:UserGroup.values()){
			SortedMap<String, Double> pollutantToValue = new TreeMap<String, Double>();
			for(WarmPollutant wm:WarmPollutant.values()){ //because ('warmPollutants' U 'coldPollutants') = 'warmPollutants'
				pollutantToValue.put(wm.toString(), 0.0);
			}
			this.userGroupToEmissions.put(ug, pollutantToValue);
		}
		this.lastIteration = LoadMyScenarios.getLastIteration(this.configFile);
	}

	private  int lastIteration;
	private  String outputDir = "/Users/aagarwal/Desktop/ils4/agarwal/munich/output/1pct/ci/";/*"./output/run2/";*/
	private  String populationFile =outputDir+ "/output_plans.xml.gz";//"/network.xml";
	private  String networkFile =outputDir+ "/output_network.xml.gz";//"/network.xml";
	private  String configFile = outputDir+"/output_config.xml";
	private SortedMap<UserGroup, SortedMap<String, Double>> userGroupToEmissions = new TreeMap<UserGroup, SortedMap<String,Double>>();
	private SortedMap<UserGroup, Population> userGrpToPopulation = new TreeMap<UserGroup, Population>();
	private Scenario scenario;
	private Map<Id<Person>, SortedMap<String, Double>> emissionsPerPerson = new HashMap<>();
	
	public static void main(String[] args) {
		EmissionsPerPersonPerUserGroup eppa = new EmissionsPerPersonPerUserGroup();
		eppa.run();
	}

	private void run() {
		String emissionEventFile = this.outputDir+"/ITERS/it."+this.lastIteration+"/"+this.lastIteration+".emission.events.xml.gz";//"/events.xml";//
		EmissionsAnalyzer ema = new EmissionsAnalyzer(emissionEventFile);
		ema.init((ScenarioImpl) this.scenario);
		ema.preProcessData();
		ema.postProcessData();

		EmissionUtils emu = new EmissionUtils();
		Map<Id<Person>, SortedMap<String, Double>> totalEmissions = ema.getPerson2totalEmissions();
		emissionsPerPerson = emu.setNonCalculatedEmissionsForPopulation(scenario.getPopulation(), totalEmissions);

		getPopulationPerUserGroup();
		getTotalEmissionsPerUserGroup(this.emissionsPerPerson);
		writeTotalEmissionsPerUserGroup(this.outputDir+"/analysis/userGrpEmissions.txt");
		writeTotalEmissionsCostsPerUserGroup(this.outputDir+"/analysis/userGrpEmissionsCosts.txt");
	}

	private void writeTotalEmissionsCostsPerUserGroup(String outputFile){
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try{
			writer.write("userGroup \t");
			for(EmissionCostFactors ecf:EmissionCostFactors.values()){
				writer.write(ecf.toString()+"\t");
			}
			writer.write("total \n");
			for(UserGroup ug:this.userGroupToEmissions.keySet()){
				double totalEmissionCost =0. ;
				writer.write(ug+"\t");
				for(EmissionCostFactors ecf:EmissionCostFactors.values()){
					double ec = this.userGroupToEmissions.get(ug).get(ecf.toString()) * ecf.getCostFactor();
					writer.write(ec+"\t");
					totalEmissionCost += ec;
				}
				writer.write(+totalEmissionCost+"\n");
			}
			writer.close();
		} catch (Exception e){
			throw new RuntimeException("Data is not written in the file. Reason - "+e);
		}
		this.logger.info("Finished Writing data to file "+outputFile);		
	}

	private void writeTotalEmissionsPerUserGroup(String outputFile) {

		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try{
			writer.write("userGroup \t");
			for(String str:this.userGroupToEmissions.get(UserGroup.URBAN).keySet()){
				writer.write(str+"\t");
			}
			writer.newLine();
			for(UserGroup ug:this.userGroupToEmissions.keySet()){
				writer.write(ug+"\t");
				for(String str:this.userGroupToEmissions.get(ug).keySet()){
					writer.write(this.userGroupToEmissions.get(ug).get(str)+"\t");
				}
				writer.newLine();
			}
			writer.close();
		} catch (Exception e){
			throw new RuntimeException("Data is not written in the file. Reason - "+e);
		}
		this.logger.info("Finished Writing files to file "+outputFile);		
	}

	private void getTotalEmissionsPerUserGroup(
			Map<Id<Person>, SortedMap<String, Double>> emissionsPerPerson) {
		for(Id<Person> personId: scenario.getPopulation().getPersons().keySet()){
			UserGroup ug = getUserGroupFromPersonId(personId);
			SortedMap<String, Double> emissionsNewValue = new TreeMap<String, Double>();
			for(String str: emissionsPerPerson.get(personId).keySet()){
				double emissionSoFar = this.userGroupToEmissions.get(ug).get(str);
				double emissionNewValue = emissionSoFar+emissionsPerPerson.get(personId).get(str);
				emissionsNewValue.put(str, emissionNewValue);
			}
			this.userGroupToEmissions.put(ug, emissionsNewValue);
		}
	}

	private SortedMap<UserGroup, Population> getPopulationPerUserGroup(){
		PersonFilter pf = new PersonFilter();
		for(UserGroup ug : UserGroup.values()){
			this.userGrpToPopulation.put(ug, pf.getPopulation(this.scenario.getPopulation(), ug));
		}
		return this.userGrpToPopulation;
	}

	private UserGroup getUserGroupFromPersonId(Id<Person> personId){
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
