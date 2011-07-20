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

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;

/**
 * @author illenberger
 *
 */
public class PlansAnalyzer {

	private static final Logger logger = Logger.getLogger(PlansAnalyzer.class);
	
	public static Map<String, DescriptiveStatistics> analyze(Set<Plan> plans, PlansAnalyzerTask task) {
		Map<String, DescriptiveStatistics> stats = new HashMap<String, DescriptiveStatistics>();
		if(plans.isEmpty()) {
			logger.warn("No plans to analyze.");
		} else {
			task.analyze(plans, stats);
		}
		return stats;
		
	}
	
	public static void analyze(Set<Plan> plans, PlansAnalyzerTask task, String output) throws IOException {
		task.setOutputDirectory(output);
		Map<String, DescriptiveStatistics> map = analyze(plans, task);
		writeStatistics(map, output + "/statistics.txt");
	}
	
	public static Map<String, DescriptiveStatistics> analyzeSelectedPlans(Population population, PlansAnalyzerTask task) {
		Set<Plan> plans = new HashSet<Plan>();
		
		for(Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			if(plan != null)
				plans.add(plan);
		}
		
		return analyze(plans, task);
	}
	
	public static void analyzeSelectedPlans(Population population, Set<Trajectory> trajectories, PlansAnalyzerTask task, String output) throws IOException {
		task.setOutputDirectory(output);
		Map<String, DescriptiveStatistics> map = analyzeSelectedPlans(population, task);
		task.analyzeTrajectories(trajectories, map);
		writeStatistics(map, output + "/statistics.txt");
	}
	
	public static Map<String, DescriptiveStatistics> analyzeUnselectedPlans(Population population, PlansAnalyzerTask task) {
		Set<Plan> plans = new HashSet<Plan>();
		
		for(Person person : population.getPersons().values()) {
			for(Plan plan : person.getPlans()) {
				if(!plan.isSelected())
					plans.add(plan);
			}
		}
		
		return analyze(plans, task);
	}
	
	public static void analyzeUnselectedPlans(Population population, Set<Trajectory> trajectories, PlansAnalyzerTask task, String output) throws IOException {
		task.setOutputDirectory(output);
		Map<String, DescriptiveStatistics> map = analyzeUnselectedPlans(population, task);
		task.analyzeTrajectories(trajectories, map);
		writeStatistics(map, output + "/statistics.txt");
	}
	
	public static void writeStatistics(Map<String, DescriptiveStatistics> statsMap, String filename) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		
		writer.write("property\tmean\tmin\tmax\tmedian\tN\tvar");
		writer.newLine();
		for(Entry<String, DescriptiveStatistics> entry : statsMap.entrySet()) {
			writer.write(entry.getKey());
			writer.write("\t");
			writer.write(String.valueOf(entry.getValue().getMean()));
			writer.write("\t");
			writer.write(String.valueOf(entry.getValue().getMin()));
			writer.write("\t");
			writer.write(String.valueOf(entry.getValue().getMax()));
			writer.write("\t");
			writer.write(String.valueOf(entry.getValue().getPercentile(50)));
			writer.write("\t");
			writer.write(String.valueOf(entry.getValue().getN()));
			writer.write("\t");
			writer.write(String.valueOf(entry.getValue().getVariance()));
			writer.newLine();
		}
		
		writer.close();
	}
}
