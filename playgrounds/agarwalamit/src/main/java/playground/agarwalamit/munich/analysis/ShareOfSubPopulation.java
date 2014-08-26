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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.LoadMyScenarios;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;


/**
 * @author amit
 */
public class ShareOfSubPopulation {

	private static String outputDir = "/Users/aagarwal/Desktop/ils4/agarwal/munich/";/*"./output/run2/";*/
//	private static String populationFile =outputDir+ "/input/mergedPopulation_All_1pct_scaledAndMode_workStartingTimePeakAllCommuter0800Var2h_gk4.xml.gz";//"/network.xml";
	private static String populationFile = outputDir+"/output/1pct/baseCase2/output_plans.xml.gz";
	private static String networkFile =outputDir+ "/input/network-86-85-87-84_simplifiedWithStrongLinkMerge---withLanes.xml";//"/network.xml";

	public static void main(String[] args) {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputDir+"/output/1pct/analysis/subPopulationShare.txt");
		Scenario sc = LoadMyScenarios.loadScenarioFromNetworkAndPlans(populationFile, networkFile);
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
