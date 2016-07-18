/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.mrieser.svi.analysis;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author mrieser / Senozon AG
 */
public class Ganglinie {

	public static void main(String[] args) {
		String networkFilename = "/Volumes/Data/projects/sviDosierungsanlagen/scenarios/output_ohne36_10/output_network.xml.gz";
		String populationFilename = "/Volumes/Data/projects/sviDosierungsanlagen/scenarios/output_mit36_10/70.plans.xml.gz";
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);
		new PopulationReader(scenario).readFile(populationFilename);
		
		Map<String, int[]> countsPerActGroup = new HashMap<String, int[]>();
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			Activity prevAct = null;
			Leg prevLeg = null;
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					prevLeg = (Leg) pe;
				}
				if (pe instanceof Activity) {
					Activity act = (Activity) pe;
					if (prevAct != null && prevLeg.getMode().equals("car")) {
						String actGroup = prevAct.getType() + "-" + act.getType();
						int[] hourVolumes = countsPerActGroup.get(actGroup);
						if (hourVolumes == null) {
							hourVolumes = new int[30];
							countsPerActGroup.put(actGroup, hourVolumes);
						}
						int timeBin = (int) (prevAct.getEndTime() / 3600);
						if (timeBin >= 30) {
							timeBin = 29;
						}
						hourVolumes[timeBin]++;
					}
					prevAct = act;
				}
			}
		}
		
		for (Map.Entry<String, int[]> e : countsPerActGroup.entrySet()) {
			String group = e.getKey();
			int[] counts = e.getValue();
//			for (int i = 0; i < counts.length; i++) {
//				System.out.println(group + "\t" + i + "\t" + counts[i]);
//			}
			System.out.print(group);
			for (int i = 0; i < counts.length; i++) {
				System.out.print("\t");
				System.out.print(counts[i]);
			}
			System.out.println();
		}
	}
	
}
