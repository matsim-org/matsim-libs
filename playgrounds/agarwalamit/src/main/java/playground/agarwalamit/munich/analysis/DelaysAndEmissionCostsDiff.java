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
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.emissions.utils.EmissionUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.congestion.CongestionPersonAnalyzer;
import playground.agarwalamit.analysis.emission.EmissionCostFactors;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsAnalyzer;

/**
 * @author amit
 */
public class DelaysAndEmissionCostsDiff {

	private static final double marginal_Utl_money=0.0789942; //(for SiouxFalls =0.062 and for Munich =0.0789942);
	private static final double marginal_Utl_performing_sec=0.96/3600;
	private static final double marginal_Utl_traveling_car_sec=-0.0/3600;
	private static final double marginalUtlOfTravelTime = marginal_Utl_traveling_car_sec+marginal_Utl_performing_sec;
	private static final double vtts_car = marginalUtlOfTravelTime/marginal_Utl_money;
	private String BAU = "/baseCaseCtd/";
	private String [] cases = {"ei","ci","eci"};
	private String outputDir = "/Users/aagarwal/Desktop/ils4/agarwal/munich/output/1pct/";

	public static void main(String[] args) {
		new DelaysAndEmissionCostsDiff().run();
	}
	
	private void run(){
		writeMap(getEmissionsCostsDiff(), "emissionCosts");
		writeMap(getDelaysCostsDiff(), "delaysCosts");
	}
	
	private void writeMap(Map<String, Map<Id<Person>, Double>> map, String fileName){
		String fileLocation = outputDir+"/analysis/boxPlot/";
		new File(fileLocation).mkdirs();
		BufferedWriter writer;
		SortedMap<UserGroup, Population> userGrpToPopulation = getPopulationPerUserGroup();
		for(String run:map.keySet()){
			for(UserGroup ug:userGrpToPopulation.keySet()){
				writer = IOUtils.getBufferedWriter(fileLocation+run+"_"+ug+"_"+fileName+"Diff.txt");
				try {
					writer.write(run+ug.toString()+"\n");
					for(Id<Person> id : userGrpToPopulation.get(ug).getPersons().keySet()){
						if(map.get(run).containsKey(id)){
							writer.write(map.get(run).get(id)+"\n");
						}
					}
					writer.close();
				} catch (Exception e) {
					throw new RuntimeException("Data is not written in the file. Reason - "+e);
				}
			}
		}
	}

	private SortedMap<UserGroup, Population> getPopulationPerUserGroup(){
		SortedMap<UserGroup, Population> userGrpToPopulation =  new TreeMap<UserGroup, Population>();
		PersonFilter pf = new PersonFilter();
		String plansFileBAU = outputDir+BAU+"/output_plans.xml.gz";
		Scenario scenario = LoadMyScenarios.loadScenarioFromPlans(plansFileBAU);
		for(UserGroup ug : UserGroup.values()){
			userGrpToPopulation.put(ug, pf.getPopulation(scenario.getPopulation(), ug));
		}
		return userGrpToPopulation;
	}

	private Map<String, Map<Id<Person>, Double>> getDelaysCostsDiff (){
		String configFileBAU = outputDir+BAU+"/output_config.xml";
		String eventsFileBAU = outputDir+BAU+"/ITERS/it.1000/1000.events.xml.gz";
		String plansFileBAU = outputDir+BAU+"/output_plans.xml.gz";
		String networkFileBAU = outputDir+BAU+"/output_network.xml.gz";

		Scenario scBAU = LoadMyScenarios.loadScenarioFromPlansNetworkAndConfig(plansFileBAU, networkFileBAU, configFileBAU);
		Map<Id<Person>, Double> delaysCostsBAU = getDelaysPerPerson(configFileBAU, eventsFileBAU, scBAU);
		Map<String, Map<Id<Person>, Double>> runCase2PersonId2DelaysCostDiff = new HashMap<String, Map<Id<Person>,Double>>();

		for(String run : cases){
			String configFile = outputDir+run+"/output_config.xml";
			String eventsFile = outputDir+run+"/ITERS/it.1500/1500.events.xml.gz";
			String plansFile = outputDir+run+"/output_plans.xml.gz";
			String networkFile = outputDir+run+"/output_network.xml.gz";
			Scenario sc = LoadMyScenarios.loadScenarioFromPlansNetworkAndConfig(plansFile,networkFile,configFile);

			Map<Id<Person>, Double> delaysCostsRun = getDelaysPerPerson(configFile, eventsFile, sc); 

			runCase2PersonId2DelaysCostDiff.put(run, getDifferenceMaps(delaysCostsBAU, delaysCostsRun));
		}
		return runCase2PersonId2DelaysCostDiff;
	}

	private Map<String, Map<Id<Person>, Double>> getEmissionsCostsDiff(){
		String configFileBAU = outputDir+BAU+"/output_config.xml";
		String emissionEventsFileBAU = outputDir+BAU+"/ITERS/it.1000/1000.emission.events.xml.gz";
		String plansFileBAU = outputDir+BAU+"/output_plans.xml.gz";
		String networkFileBAU = outputDir+BAU+"/output_network.xml.gz";

		Scenario scBAU = LoadMyScenarios.loadScenarioFromPlansNetworkAndConfig(plansFileBAU, networkFileBAU, configFileBAU);
		Map<Id<Person>, Double> emissionsCostsBAU = getEmissionsPerPerson(emissionEventsFileBAU, scBAU);
		Map<String, Map<Id<Person>, Double>> runCase2PersonId2EmissionsCostDiff = new HashMap<String, Map<Id<Person>,Double>>();

		for(String run : cases){
			String configFile = outputDir+run+"/output_config.xml";
			String emissionEventsFile = outputDir+run+"/ITERS/it.1500/1500.emission.events.xml.gz";
			String plansFile = outputDir+run+"/output_plans.xml.gz";
			String networkFile = outputDir+run+"/output_network.xml.gz";
			Scenario sc = LoadMyScenarios.loadScenarioFromPlansNetworkAndConfig(plansFile,networkFile,configFile);

			Map<Id<Person>, Double> emissionsCostsRun = getEmissionsPerPerson(emissionEventsFile, sc);

			runCase2PersonId2EmissionsCostDiff.put(run, getDifferenceMaps(emissionsCostsBAU, emissionsCostsRun));
		}
		return runCase2PersonId2EmissionsCostDiff;
	}

	private Map<Id<Person>, Double> getDifferenceMaps(Map<Id<Person>, Double> m1, Map<Id<Person>, Double>m2){
		Map<Id<Person>, Double> outMap = new HashMap<Id<Person>, Double>();
		for(Id<Person> id : m1.keySet()){
			if(m2.containsKey(id)) outMap.put(id, m2.get(id)-m1.get(id));
		}
		return outMap;
	}

	private Map<Id<Person>, Double> getDelaysPerPerson(String configFile, String eventsFile, Scenario sc){
		CongestionPersonAnalyzer personAnalyzer = new CongestionPersonAnalyzer(configFile, eventsFile,1);
		personAnalyzer.init(sc);
		personAnalyzer.preProcessData();
		personAnalyzer.postProcessData();

		Map<Id<Person>, Double> personId2DelaysCosts= new HashMap<Id<Person>, Double>();
		Map<Id<Person>, Double> personId2DelaysInSec = personAnalyzer.getCongestionPerPersonTimeInterval().get(sc.getConfig().qsim().getEndTime());
		for(Id<Person> id :personId2DelaysInSec.keySet() ){
			personId2DelaysCosts.put(id, vtts_car*personId2DelaysInSec.get(id));
		}
		return personId2DelaysCosts;
	}

	private Map<Id<Person>, Double> getEmissionsPerPerson(String emissionsEventsFile, Scenario sc){
		EmissionsAnalyzer ema = new EmissionsAnalyzer(emissionsEventsFile);
		ema.init((ScenarioImpl)sc);
		ema.preProcessData();
		ema.postProcessData();
		EmissionUtils emu = new EmissionUtils();
		Map<Id<Person>, SortedMap<String, Double>> totalEmissions = ema.getPerson2totalEmissions();
		Map<Id<Person>, SortedMap<String, Double>> personId2Emissions = emu.setNonCalculatedEmissionsForPopulation(sc.getPopulation(), totalEmissions);
		Map<Id<Person>, Double> personId2EmissionsCosts= new HashMap<Id<Person>, Double>();
		boolean considerCO2Costs = true;

		for(Id<Person> id : personId2Emissions.keySet()){
			double totalEmissionCost = 0;
			for(EmissionCostFactors ecf:EmissionCostFactors.values()) {
				String str = ecf.toString();
				if(str.equals("CO2_TOTAL") && !considerCO2Costs){
					// do not include CO2_TOTAL costs.
				} else {
					double emissionsCosts = ecf.getCostFactor() * personId2Emissions.get(id).get(str);
					totalEmissionCost += emissionsCosts;
				}
			}
			personId2EmissionsCosts.put(id, totalEmissionCost);
		}
		return personId2EmissionsCosts;
	}

}
