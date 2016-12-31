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
package playground.agarwalamit.analysis.activity.scoring;

import java.io.BufferedWriter;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */

public class UtilityPartsPerPerson {

	public static void main(String[] args) {
		String runDir ="/Volumes/DATA 1 (WD 2 TB)/output_SelectExp1_5p_5min_1000it_Dwell_TEST/";
		String runCase = "/w8-18_hetero_1.0x/";
		String config = runDir+runCase+"//output_config.xml.gz";
		String plans = runDir+runCase+"//output_plans.xml.gz";
		String network = runDir+runCase+"//output_network.xml.gz";
		final Scenario sc = LoadMyScenarios.loadScenarioFromPlansNetworkAndConfig(plans, network, config);
		new UtilityPartsPerPerson().run(sc, runDir+runCase);
	}

	private void run(final Scenario sc, final String outputDir){
		BufferedWriter writer = IOUtils.getBufferedWriter(outputDir+"/analysis/person2Scote.txt");

		UtilityByPartsAnalyzer utilPerf = new UtilityByPartsAnalyzer(true, false, false, false);
		UtilityByPartsAnalyzer utilLeg = new UtilityByPartsAnalyzer(false, true, false, false);
		UtilityByPartsAnalyzer utilMoney = new UtilityByPartsAnalyzer(false, false, true, false);
		UtilityByPartsAnalyzer utilStuck = new UtilityByPartsAnalyzer(false, false, false, true);

		utilPerf.run(sc, outputDir);
		utilLeg.run(sc, outputDir);
		utilMoney.run(sc, outputDir);
		utilStuck.run(sc, outputDir);

		Map<Id<Person>, Double> personIdUtilPerf = utilPerf.getPerson2Score();
		Map<Id<Person>, Double> personIdUtilLeg = utilLeg.getPerson2Score();
		Map<Id<Person>, Double> personIdUtilMoney = utilMoney.getPerson2Score();
		Map<Id<Person>, Double> personIdUtilStuck = utilStuck.getPerson2Score();

		try {
			writer.write("personId \t  util_perf \t util_leg \t util_money \t util_stuck \t sum \n");

			for(Id<Person> personId : personIdUtilPerf.keySet()){

				double sum = 0;
				double uPerf = personIdUtilPerf.get(personId);
				double uLeg = personIdUtilLeg.get(personId);
				double uMoney = personIdUtilMoney.get(personId);
				double uStuck = personIdUtilStuck.get(personId);
				sum = uPerf + uLeg + uMoney + uStuck;
				writer.write(personId+"\t"+uPerf+"\t"+uLeg+"\t"+uMoney+"\t"+uStuck+"\t"+sum+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "+ e);
		}
	}
}