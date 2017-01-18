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
package playground.agarwalamit.analysis.modalShare;

import org.matsim.api.core.v01.Scenario;

import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */
public class ModalShareExample {

	public static void main(String[] args) {
		
		String outputDir = "../../../repos/runs-svn/siouxFalls/run203/policies/";
		String [] runCases = {"bau","v3","v4","v6"};
		
		for(String runCase :runCases){
			ModalShareExample msUG = new ModalShareExample();
			int it = 1000;
			msUG.run(outputDir+runCase+"/ITERS/it."+it+"/"+it+".plans.xml.gz", outputDir+runCase+"/modalShare_it."+it+".txt");
		}
	}

	private void run(final String populationFile, final String outputFile){
		Scenario sc = LoadMyScenarios.loadScenarioFromPlans(populationFile);
		ModalShareFromPlans msg = new ModalShareFromPlans(sc.getPopulation());
		msg.run();
		msg.writeResults(outputFile);
	}
}