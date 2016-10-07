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

package playground.agarwalamit.analysis.activity;

import java.io.BufferedWriter;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.munich.utils.MunichPersonFilter;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author amit
 */

public class ActivityCounter {
	private static final MunichPersonFilter PF = new MunichPersonFilter();
	private final String inputPlansFile ;
	private final UserGroup ug;
	private final SortedMap<String, Integer> actTyp2Count = new TreeMap<>();
	private final boolean isSorting;

	public ActivityCounter(final String inputPlansFile, final UserGroup ug){
		this.inputPlansFile = inputPlansFile;
		this.ug = ug;
		this.isSorting = true;
	}

	public ActivityCounter(final String inputPlansFile){
		this.inputPlansFile = inputPlansFile;
		this.isSorting = false;
		this.ug = null;
	}

	public static void main(String[] args) {
		String outputFilesDir = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run0/baseCaseCtd/";		
		ActivityCounter ac = new ActivityCounter(outputFilesDir+"/output_plans.xml.gz",UserGroup.URBAN);
		ac.count(outputFilesDir);
		ac.writeData(outputFilesDir+"/analysis/");
	}

	public void count(final String filesDir) {
		Scenario sc = LoadMyScenarios.loadScenarioFromPlans(this.inputPlansFile);

		for(Person p : sc.getPopulation().getPersons().values()){
			if( isSorting && PF.isPersonIdFromUserGroup(p.getId(), ug) ){
				List<PlanElement> pes = p.getSelectedPlan().getPlanElements();
				for (PlanElement pe : pes){
					if(pe instanceof Activity){
						String actTyp = ((Activity) pe).getType();
						if(actTyp2Count.containsKey(actTyp)){
							actTyp2Count.put(actTyp, actTyp2Count.get(actTyp)+1);
						} else {
							actTyp2Count.put(actTyp, 1);
						}
					}
				}
			}
		}
	}

	public void writeData(final String outputFolder){
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/actTyp2Count.txt");
		try {
			writer.write("actType \t count \n");
			int sum = 0;
			for(String actType : actTyp2Count.keySet()){
				writer.write(actType+"\t"+actTyp2Count.get(actType)+"\n");
				sum += actTyp2Count.get(actType);
			}
			writer.write("total \t"+sum+"\n");
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason "+e);
		}
	}
}