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
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.congestion.ExperiencedDelayAnalyzer;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.vsp.airPollution.flatEmissions.EmissionCostFactors;
import playground.agarwalamit.utils.MapUtils;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsAnalyzer;

/**
 * @author amit
 */
public class DelaysAndEmissionCostsDiff {

	private static final double MARGINAL_UTIL_MONEY=0.0789942; //(for SiouxFalls =0.062 and for Munich =0.0789942);
	private static final double MARGINAL_UTIL_PERF_SEC=0.96/3600;
	private static final double MARGINAL_UTIL_TRAVEL_CAR_SEC=-0.0/3600;
	private static final double MARGINAL_UTIL_TRAVEL_TIME = MARGINAL_UTIL_TRAVEL_CAR_SEC+MARGINAL_UTIL_PERF_SEC;
	private static final double VTTS_CAR = MARGINAL_UTIL_TRAVEL_TIME/MARGINAL_UTIL_MONEY;
	private final String bau = "/baseCaseCtd/";
	private final String [] cases = {"ei","ci","eci"};
	private final String outputDir = "/Users/aagarwal/Desktop/ils4/agarwal/munich/output/1pct/";

	public static void main(String[] args) {
		new DelaysAndEmissionCostsDiff().run();
	}
	
	private void run(){
		writeMap(getEmissionsCostsDiff(), "emissionCosts");
		writeMap(getDelaysCostsDiff(), "delaysCosts");
	}
	
	private void writeMap(final Map<String, Map<Id<Person>, Double>> map, final String fileName){
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
		SortedMap<UserGroup, Population> userGrpToPopulation = new TreeMap<>();
		PersonFilter pf = new PersonFilter();
		String plansFileBAU = outputDir+bau+"/output_plans.xml.gz";
		Scenario scenario = LoadMyScenarios.loadScenarioFromPlans(plansFileBAU);
		for(UserGroup ug : UserGroup.values()){
			userGrpToPopulation.put(ug, pf.getPopulation(scenario.getPopulation(), ug));
		}
		return userGrpToPopulation;
	}

	private Map<String, Map<Id<Person>, Double>> getDelaysCostsDiff (){
		String configFileBAU = outputDir+bau+"/output_config.xml";
		String eventsFileBAU = outputDir+bau+"/ITERS/it.1000/1000.events.xml.gz";
		String plansFileBAU = outputDir+bau+"/output_plans.xml.gz";
		String networkFileBAU = outputDir+bau+"/output_network.xml.gz";

		Scenario scBAU = LoadMyScenarios.loadScenarioFromPlansNetworkAndConfig(plansFileBAU, networkFileBAU, configFileBAU);
		Map<Id<Person>, Double> delaysCostsBAU = getDelaysPerPerson(configFileBAU, eventsFileBAU, scBAU);
		Map<String, Map<Id<Person>, Double>> runCase2PersonId2DelaysCostDiff = new HashMap<>();

		for(String run : cases){
			String configFile = outputDir+run+"/output_config.xml";
			String eventsFile = outputDir+run+"/ITERS/it.1500/1500.events.xml.gz";
			String plansFile = outputDir+run+"/output_plans.xml.gz";
			String networkFile = outputDir+run+"/output_network.xml.gz";
			Scenario sc = LoadMyScenarios.loadScenarioFromPlansNetworkAndConfig(plansFile,networkFile,configFile);

			Map<Id<Person>, Double> delaysCostsRun = getDelaysPerPerson(configFile, eventsFile, sc); 

			runCase2PersonId2DelaysCostDiff.put(run, MapUtils.subtractMaps(delaysCostsBAU, delaysCostsRun));
		}
		return runCase2PersonId2DelaysCostDiff;
	}

	private Map<String, Map<Id<Person>, Double>> getEmissionsCostsDiff(){
		String configFileBAU = outputDir+bau+"/output_config.xml";
		String emissionEventsFileBAU = outputDir+bau+"/ITERS/it.1000/1000.emission.events.xml.gz";
		String plansFileBAU = outputDir+bau+"/output_plans.xml.gz";
		String networkFileBAU = outputDir+bau+"/output_network.xml.gz";

		Scenario scBAU = LoadMyScenarios.loadScenarioFromPlansNetworkAndConfig(plansFileBAU, networkFileBAU, configFileBAU);
		Map<Id<Person>, Double> emissionsCostsBAU = getEmissionsPerPerson(emissionEventsFileBAU, scBAU);
		Map<String, Map<Id<Person>, Double>> runCase2PersonId2EmissionsCostDiff = new HashMap<>();

		for(String run : cases){
			String configFile = outputDir+run+"/output_config.xml";
			String emissionEventsFile = outputDir+run+"/ITERS/it.1500/1500.emission.events.xml.gz";
			String plansFile = outputDir+run+"/output_plans.xml.gz";
			String networkFile = outputDir+run+"/output_network.xml.gz";
			Scenario sc = LoadMyScenarios.loadScenarioFromPlansNetworkAndConfig(plansFile,networkFile,configFile);

			Map<Id<Person>, Double> emissionsCostsRun = getEmissionsPerPerson(emissionEventsFile, sc);

			runCase2PersonId2EmissionsCostDiff.put(run, MapUtils.subtractMaps(emissionsCostsBAU, emissionsCostsRun));
		}
		return runCase2PersonId2EmissionsCostDiff;
	}

	private Map<Id<Person>, Double> getDelaysPerPerson(final String configFile, final String eventsFile, final Scenario sc){
		ExperiencedDelayAnalyzer personAnalyzer = new ExperiencedDelayAnalyzer(eventsFile,sc,1);
		personAnalyzer.run();

		Map<Id<Person>, Double> personId2DelaysCosts= new HashMap<>();
		Map<Id<Person>, Double> personId2DelaysInSec = personAnalyzer.getTimeBin2AffectedPersonId2Delay().get(sc.getConfig().qsim().getEndTime());
		for(Id<Person> id :personId2DelaysInSec.keySet() ){
			personId2DelaysCosts.put(id, VTTS_CAR*personId2DelaysInSec.get(id));
		}
		return personId2DelaysCosts;
	}

	private Map<Id<Person>, Double> getEmissionsPerPerson(final String emissionsEventsFile, final Scenario sc){
		EmissionsAnalyzer ema = new EmissionsAnalyzer(emissionsEventsFile);
		ema.init((MutableScenario)sc);
		ema.preProcessData();
		ema.postProcessData();
		EmissionUtils emu = new EmissionUtils();
		Map<Id<Person>, SortedMap<String, Double>> totalEmissions = ema.getPerson2totalEmissions();
		Map<Id<Person>, SortedMap<String, Double>> personId2Emissions = emu.setNonCalculatedEmissionsForPopulation(sc.getPopulation(), totalEmissions);
		Map<Id<Person>, Double> personId2EmissionsCosts= new HashMap<>();
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