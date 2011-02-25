/* *********************************************************************** *
 * project: org.matsim.*
 * PlanAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.sim.analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;

/**
 * @author illenberger
 *
 */
public class PlansAnalyzer {

	public static Map<String, Double> analyze(Set<Plan> plans, PlansAnalyzerTask task) {
		Map<String, Double> stats = new HashMap<String, Double>();
		if(plans.isEmpty()) {
			System.err.println("No plans to analyze.");
		} else {
			task.analyze(plans, stats);
		}
		return stats;
		
	}
	
	public static Map<String, Double> analyzeSelectedPlans(Population population, PlansAnalyzerTask task) {
		Set<Plan> plans = new HashSet<Plan>();
		
		for(Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			if(plan != null)
				plans.add(plan);
		}
		
		return analyze(plans, task);
	}
	
	public static Map<String, Double> analyzeUnselectedPlans(Population population, PlansAnalyzerTask task) {
		Set<Plan> plans = new HashSet<Plan>();
		
		for(Person person : population.getPersons().values()) {
			for(Plan plan : person.getPlans()) {
				if(!plan.isSelected())
					plans.add(plan);
			}
		}
		
		return analyze(plans, task);
	}
	
	public static void write(Map<String, Double> stats, String filename) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		
		for(Entry<String, Double> entry : stats.entrySet()) {
			writer.write(entry.getKey());
			writer.write("\t");
			writer.write(entry.getValue().toString());
			writer.newLine();
		}
		
		writer.close();
	}
}
