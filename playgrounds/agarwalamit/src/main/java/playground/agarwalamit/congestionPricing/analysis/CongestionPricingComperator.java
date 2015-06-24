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
package playground.agarwalamit.congestionPricing.analysis;

import java.io.BufferedWriter;
import java.util.Map;
import java.util.SortedMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.congestion.CausedDelayAnalyzer;
import playground.agarwalamit.analysis.congestion.ExperiencedDelayAnalyzer;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * (1) Read events file with desired congestion implementation.
 * (2) From events file write various analyses files.
 * @author amit
 */

public class CongestionPricingComperator {

	
	public CongestionPricingComperator(String runScenario) {
		this.pricingScenario = runScenario;
		scenario = LoadMyScenarios.loadScenarioFromOutputDir(runDir+pricingScenario+"/");
		simulationEndTime = scenario.getConfig().qsim().getEndTime();
		int lastIt = scenario.getConfig().controler().getLastIteration();
		eventsFile = runDir+pricingScenario+"/ITERS/it."+lastIt+"/"+lastIt+".events.xml.gz";
	}

	private int noOfTimeBins = 30;
	private String eventsFile ;
	private double simulationEndTime;
	private String runDir = "../../../repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run12/policies/";
	private Scenario scenario;
	private String pricingScenario;
	private final boolean isSortingForInsideMunich = true;
	private final String suffixForSoring = "_sorted";
	

	private final PersonFilter pf = new PersonFilter();
	
	public static void main(String[] args) {
		CongestionPricingComperator analyzer = new CongestionPricingComperator("implV3");
		analyzer.writeExperiecedAndCausingPersonDelay();
		analyzer.writeAverageLinkTolls();
		analyzer.writeHourlyCausedDelayForEachPerson();
	}
	
	/**
	 * Writes timeBin2PersonId2UserGroup2CausedDelay
	 */
	private void writeHourlyCausedDelayForEachPerson(){
		SortedMap<Double, Map<Id<Person>, Double>> timeBin2CausingPerson2Delay = getCausingPersonDelay(noOfTimeBins);
		BufferedWriter writer = IOUtils.getBufferedWriter(runDir+"/analysis/timeBin2Person2UserGroup2CausedDelay_"+pricingScenario+suffixForSoring+".txt");
		try {
			writer.write("timeBin \t personId \t userGroup \t delayInHr \n");
			for (double d : timeBin2CausingPerson2Delay.keySet()){
				for (Id<Person> personId : timeBin2CausingPerson2Delay.get(d).keySet()){
					writer.write(d+"\t"+personId+"\t"+getUserGroupFromPersonId(personId)+"\t"+timeBin2CausingPerson2Delay.get(d).get(personId) / 3600+"\n");
				}
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}
	}
	
	/**
	 * Writes
	 *<p> personId userGroup delayInHr
	 */
	private void writeExperiecedAndCausingPersonDelay(){
		SortedMap<Double, Map<Id<Person>, Double>> timeBin2AffectedPerson2Delay = getExperiencedPersonDelay(1);
		SortedMap<Double, Map<Id<Person>, Double>> timeBin2CausingPerson2Delay = getCausingPersonDelay(1);
		
		if (timeBin2AffectedPerson2Delay.size()!=1) throw new RuntimeException("Delay is not summed up for all time bins.");
		Map<Id<Person>, Double> affectedperson2Delay = timeBin2AffectedPerson2Delay.get(simulationEndTime);
		Map<Id<Person>, Double> causedPerson2Delay = timeBin2CausingPerson2Delay.get(simulationEndTime);
		
		BufferedWriter writer = IOUtils.getBufferedWriter(runDir+"/analysis/affectedAndCausedDelay_"+pricingScenario+suffixForSoring+".txt");
		
		try {
			writer.write("personId\tuserGroup\taffectedDelayInHr\tcausedDelayInHr\n");
			for (Id<Person> id :causedPerson2Delay.keySet()){
				writer.write(id+"\t"+getUserGroupFromPersonId(id)+"\t"+affectedperson2Delay.get(id) / 3600+"\t"+causedPerson2Delay.get(id) / 3600+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}
	}
	
	/**
	 * Writes
	 * <p> timeBin LinkId avgLinkTollPerPerson.
	 * <p> Since, these link delays are evaluated from congestion events, implementation varies.
	 * <p> !! Imp !! All agents are included in the averaging whether agent pay toll or not.
	 */
	private void writeAverageLinkTolls () {

		Config config = scenario.getConfig();
		
		double vtts_car = ((config.planCalcScore().getTraveling_utils_hr()/3600) + 
				(config.planCalcScore().getPerforming_utils_hr()/3600)) 
				/ (config.planCalcScore().getMarginalUtilityOfMoney());
		
		
		CausedDelayAnalyzer delayAnalyzer = new CausedDelayAnalyzer(eventsFile, scenario, noOfTimeBins,true); // only Munich city area
		delayAnalyzer.run();
		
		SortedMap<Double, Map<Id<Link>, Double>> timeBin2LinkId2Delay = delayAnalyzer.getTimeBin2LinkId2Delay();
		SortedMap<Double, Map<Id<Link>, Integer>> timeBin2LinkCount = delayAnalyzer.getTimeBin2Link2PersonCount();
		
		BufferedWriter writer = IOUtils.getBufferedWriter(runDir+"/analysis/linkId2Toll"+pricingScenario+suffixForSoring+".txt");
		
		try {
			writer.write("timeBin\tlinkId\tavgLinkTollEURO\n");
			for (double d:timeBin2LinkId2Delay.keySet()){
				for(Id<Link> linkId : timeBin2LinkId2Delay.get(d).keySet()){
					double delay = timeBin2LinkId2Delay.get(d).get(linkId);
					double count = timeBin2LinkCount.get(d).get(linkId);
					double avgToll  = 0;
					/*
					 *  if delay ==0 and count != 0 --> noone delayed
					 *  else if delay ==0 and count ==0 --> noone traveled on the link
					 *  else if delay!=0 and count ==0 --> cant happen
					 *  elst if delay!=0 and count !=0 --> get avg delay
					 */
					if(delay!=0 && count==0) throw new RuntimeException("Delay is not zero whereas person count is zero. Can not happen. Aborting...");
					else if(delay !=0 && count !=0 )  {
						avgToll = vtts_car * ( delay / count) ;
					}
					writer.write(d+"\t"+linkId+"\t"+avgToll+"\n");	
				}
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}
	}

	private SortedMap<Double, Map<Id<Person>, Double>> getExperiencedPersonDelay(int noOfTimeBin){
		ExperiencedDelayAnalyzer personAnalyzer = new ExperiencedDelayAnalyzer(eventsFile, noOfTimeBin, isSortingForInsideMunich);
		personAnalyzer.init(scenario);
		personAnalyzer.preProcessData();
		personAnalyzer.postProcessData();
		return personAnalyzer.getCongestionPerPersonTimeInterval();
	}
	
	private SortedMap<Double, Map<Id<Person>, Double>> getCausingPersonDelay(int noOfTimeBin){
		CausedDelayAnalyzer delayAnalyzer = new CausedDelayAnalyzer(eventsFile, scenario, noOfTimeBin,isSortingForInsideMunich);
		delayAnalyzer.run();
		return delayAnalyzer.getTimeBin2CausingPersonId2Delay();
	}
	
	private UserGroup getUserGroupFromPersonId(Id<Person> presonId){
		for(UserGroup ug :UserGroup.values()){
			if(pf.isPersonIdFromUserGroup(presonId, ug)){
				return ug;
			}
		}
		return null;
	}
}
