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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.utils.LoadMyScenarios;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author amit
 */

public class PersonUserGroupHomeLocationInfo {

	private final PersonFilter pf = new PersonFilter();

	public static void main(String[] args) {

		String outDir = "../../../repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run10/policies/bau/";
		new PersonUserGroupHomeLocationInfo().run(outDir);
		
	}

	private void run(String outputDir){

		String inputPlansFile = outputDir+"output_plans.xml.gz";

		BufferedWriter writer = IOUtils.getBufferedWriter(outputDir+"/analysis/personsHomeLocation_usrGrp.txt");
		Scenario sc = LoadMyScenarios.loadScenarioFromPlans(inputPlansFile);
		
		try {
			writer.write("personId \t userGroup \t homeX \t homeY \n");
			
			for(UserGroup ug : UserGroup.values()){
				Population pop = pf.getPopulation(sc.getPopulation(), ug);
				for(Person p:pop.getPersons().values()){
					Coord homeCoord = null;
					for(PlanElement pe : p.getSelectedPlan().getPlanElements()){
						if (pe instanceof Activity){
							homeCoord = ((Activity)pe).getCoord();
							break;
						} else homeCoord = new Coord(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
					}
					assert homeCoord != null;
					writer.write(p.getId()+"\t"+ug+"\t"+homeCoord.getX()+"\t"+homeCoord.getY()+"\n");
				}
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}
	}
}
