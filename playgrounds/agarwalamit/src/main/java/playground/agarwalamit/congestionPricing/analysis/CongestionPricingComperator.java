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
import java.util.Set;
import java.util.SortedMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.congestion.CausedDelayAnalyzer;
import playground.agarwalamit.analysis.congestion.ExperiencedDelayAnalyzer;
import playground.agarwalamit.munich.utils.MunichPersonFilter;
import playground.agarwalamit.utils.AreaFilter;
import playground.agarwalamit.utils.LoadMyScenarios;

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

		//		

		Config config = scenario.getConfig();
		vttsCar = ((config.planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() /3600) +
				(config.planCalcScore().getPerforming_utils_hr()/3600)) 
				/ (config.planCalcScore().getMarginalUtilityOfMoney());
	}

	private final int noOfTimeBins = 1;
	private final String eventsFile ;
	private final double simulationEndTime;
	private final String runDir = "../../../repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run12/policies/";
	private final Scenario scenario;
	private final String pricingScenario;
	
	private final boolean isSortingForInsideMunich = true;
	private final AreaFilter areaFilter = new AreaFilter();
	
	private final String suffixForSoring = "_sorted";
	private final double vttsCar;
	private final MunichPersonFilter pf = new MunichPersonFilter();
	private final AreaFilter af = new AreaFilter();

	public static void main(String[] args) {
		CongestionPricingComperator analyzer = new CongestionPricingComperator("implV3");
//		analyzer.writeExperiecedAndCausingPersonDelay();
//		analyzer.writeAverageLinkTolls();
//		analyzer.writeHourlyCausedDelayForEachPerson();
	}

	/**
	 * Writes timeBin2PersonId2UserGroup2CausedDelay
	 */
	private void writeHourlyCausedDelayForEachPerson(){
		SortedMap<Double, Map<Id<Person>, Double>> timeBin2CausingPerson2Delay = getCausingPersonDelay(noOfTimeBins);
		BufferedWriter writer = IOUtils.getBufferedWriter(runDir+"/analysis/timeBin2Person2UserGroup2CausedDelay_"+pricingScenario+suffixForSoring+".txt");
		try {
			writer.write("timeBin \t personId \t userGroup \t delayInSec \n");
			for (double d : timeBin2CausingPerson2Delay.keySet()){
				for (Id<Person> personId : timeBin2CausingPerson2Delay.get(d).keySet()){
					writer.write(d+"\t"+personId+"\t"+pf.getMunichUserGroupFromPersonId(personId)+"\t"+timeBin2CausingPerson2Delay.get(d).get(personId)+"\n");
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
			writer.write("personId \t userGroup \t affectedDelayInHr \t causedDelayInHr \n");
			for (Id<Person> id :causedPerson2Delay.keySet()){
				writer.write(id+"\t"+pf.getMunichUserGroupFromPersonId(id)+"\t"+affectedperson2Delay.get(id) / 3600+"\t"+causedPerson2Delay.get(id) / 3600+"\n");
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
	 */
	private void writeAverageLinkTolls () {

		CausedDelayAnalyzer delayAnalyzer = new CausedDelayAnalyzer(eventsFile, scenario, noOfTimeBins, new AreaFilter()); // only Munich city area
		delayAnalyzer.run();

		SortedMap<Double, Map<Id<Link>, Double>> timeBin2LinkId2Delay = delayAnalyzer.getTimeBin2LinkId2Delay();//delays on each link for each time bin
		SortedMap<Double, Map<Id<Link>, Set<Id<Person>>>> timeBin2LinkCount = delayAnalyzer.getTimeBin2Link2CausingPersons();//number of congestion events.

		BufferedWriter writer = IOUtils.getBufferedWriter(runDir+"/analysis/linkId2Toll_"+noOfTimeBins+"Bins_"+pricingScenario+suffixForSoring+".txt");

		try {
			writer.write("timeBin \t linkId \t avgLinkTollEURO \n");
			for (double d:timeBin2LinkId2Delay.keySet()){
				for(Id<Link> linkId : timeBin2LinkId2Delay.get(d).keySet()){
					double delay = timeBin2LinkId2Delay.get(d).get(linkId);
					int count = timeBin2LinkCount.get(d).get(linkId).size();
					double avgToll  = 0;
					/*
					 *  if delay ==0 and count != 0 --> noone delayed (cant happen)
					 *  else if delay ==0 and count ==0 --> noone traveled on the link
					 *  else if delay!=0 and count == 0 --> cant happen
					 *  elst if delay!=0 and count !=0 --> get avg delay
					 */
					if((delay != 0 && count == 0) || (delay == 0 && count != 0))throw new RuntimeException("Zero delay with non zero person count or converese can not happen. Aborting...");
					else if(delay !=0 && count !=0 )  {
						avgToll = vttsCar * ( delay / count) ;
					}
					writer.write(d+"\t"+linkId+"\t"+avgToll+"\n");	
				}
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "+ e);
		}
	}

	private SortedMap<Double, Map<Id<Person>, Double>> getExperiencedPersonDelay(int noOfTimeBin){
		ExperiencedDelayAnalyzer personAnalyzer = new ExperiencedDelayAnalyzer(eventsFile, scenario, noOfTimeBin, areaFilter);
		personAnalyzer.run();
		return personAnalyzer.getTimeBin2AffectedPersonId2Delay();
	}

	private SortedMap<Double, Map<Id<Person>, Double>> getCausingPersonDelay(int noOfTimeBin){
		CausedDelayAnalyzer delayAnalyzer = new CausedDelayAnalyzer(eventsFile, scenario, noOfTimeBin, new AreaFilter());
		delayAnalyzer.run();
		return delayAnalyzer.getTimeBin2CausingPersonId2Delay();
	}
}
