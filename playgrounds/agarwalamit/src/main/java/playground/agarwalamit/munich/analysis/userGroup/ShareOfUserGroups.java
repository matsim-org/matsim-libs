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
package playground.agarwalamit.munich.analysis.userGroup;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.utils.LoadMyScenarios;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;


/**
 * @author amit
 */
public class ShareOfUserGroups {

	private static final String outputDir = "/Users/amit/Documents/repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run9//";/*"./output/run2/";*/
//	private static String populationFile =outputDir+ "/input/mergedPopulation_All_1pct_scaledAndMode_workStartingTimePeakAllCommuter0800Var2h_gk4.xml.gz";//"/network.xml";
	private static final String populationFile = outputDir+"/baseCaseCtd/output_plans.xml.gz";
	private static final String networkFile =outputDir+ "/baseCaseCtd/output_network.xml.gz";//"/network.xml";

	public static void main(String[] args) {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputDir+"/analysis/subPopulationShare.txt");
		Scenario sc = LoadMyScenarios.loadScenarioFromPlansAndNetwork(populationFile, networkFile);
		PersonFilter pf = new PersonFilter();
		Population wholePop = sc.getPopulation();
		try {
			writer.write("UserGroup \t PopulationSize \t percentageShare \n");
			for(UserGroup ug:UserGroup.values()){
				Population pop = pf.getPopulation(wholePop,ug);
				double share = (double) pop.getPersons().size()*100/ (double) wholePop.getPersons().size();
				writer.write(ug.toString()+"\t"+pop.getPersons().size()+"\t"+share+"\n");
			}
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Data is not written to a file. Reason - "+e);
		}
	}

}
