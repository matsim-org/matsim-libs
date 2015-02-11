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
package playground.agarwalamit.analysis.scoring;

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
		String runDir ="/Users/amit/Documents/repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run9/";
		String runCase = "/baseCaseCtd/";
		String config = runDir+runCase+"//output_config.xml.gz";
		String plans = runDir+runCase+"//output_plans.xml.gz";
		String network = runDir+runCase+"//output_network.xml.gz";
		final Scenario sc = LoadMyScenarios.loadScenarioFromPlansNetworkAndConfig(plans, network, config);
		new UtilityPartsPerPerson().run(sc, runDir+runCase);
	}

	private void run(Scenario sc, String outputDir){

		BufferedWriter writer = IOUtils.getBufferedWriter(outputDir+"/analysis/person2Scote.txt");

		UtilityByPartsAnalyzer util_perf = new UtilityByPartsAnalyzer(true, false, false, false);
		UtilityByPartsAnalyzer util_leg = new UtilityByPartsAnalyzer(false, true, false, false);
		UtilityByPartsAnalyzer util_money = new UtilityByPartsAnalyzer(false, false, true, false);
		UtilityByPartsAnalyzer util_stuck = new UtilityByPartsAnalyzer(false, false, false, true);

		util_perf.run(sc, outputDir);
		util_leg.run(sc, outputDir);
		util_money.run(sc, outputDir);
		util_stuck.run(sc, outputDir);

		Map<Id<Person>, Double> personId_util_perf = util_perf.getPerson2Score();
		Map<Id<Person>, Double> personId_util_leg = util_leg.getPerson2Score();
		Map<Id<Person>, Double> personId_util_money = util_money.getPerson2Score();
		Map<Id<Person>, Double> personId_util_stuck = util_stuck.getPerson2Score();

		try {
			writer.write("personId \t  util_perf \t util_leg \t util_money \t util_stuck \t sum \n");

			for(Id<Person> personId : personId_util_perf.keySet()){

				double sum = 0;
				double u_p = personId_util_perf.get(personId);
				double u_l = personId_util_leg.get(personId);
				double u_m = personId_util_money.get(personId);
				double u_s = personId_util_stuck.get(personId);
				sum = u_p + u_l + u_m + u_s;
				writer.write(personId+"\t"+u_p+"\t"+u_l+"\t"+u_m+"\t"+u_s+"\t"+sum+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}


	}



}
