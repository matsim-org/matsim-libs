/* *********************************************************************** *
 * project: org.matsim.*
 * TravelDistanceTask.java
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

import gnu.trove.TDoubleDoubleHashMap;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.sna.math.FixedSampleSizeDiscretizer;
import org.matsim.contrib.sna.math.Histogram;
import org.matsim.contrib.sna.math.LinLogDiscretizer;
import org.matsim.contrib.sna.util.TXTWriter;
import org.matsim.core.population.MatsimPopulationReader;

/**
 * @author illenberger
 *
 */
public class TravelDistanceTask implements PlansAnalyzerTask {

	private static final String TRAVEL_DISTANCE_PREFIX = "d_mean_";
	
	private String output;
	@Override
	public void analyze(Set<Plan> plans, Map<String, Double> stats) {
		TravelDistance distance = new TravelDistance();
		Map<String, DescriptiveStatistics> statsMap = distance.statistics(plans);
		
		for(Entry<String, DescriptiveStatistics> entry : statsMap.entrySet()) {
			stats.put(TRAVEL_DISTANCE_PREFIX + entry.getKey(), entry.getValue().getMean());
//			TDoubleDoubleHashMap hist = Histogram.createHistogram(entry.getValue(), FixedSampleSizeDiscretizer.create(entry.getValue().getValues(), 100));
			TDoubleDoubleHashMap hist = Histogram.createHistogram(entry.getValue(), new LinLogDiscretizer(500.0, 2));
			Histogram.normalize(hist);
			try {
				TXTWriter.writeMap(hist, "d", "n", String.format("%1$s/d.%2$s.txt", output, entry.getKey()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		distance.setGeodesicMode(true);
		statsMap = distance.statistics(plans);
		
		for(Entry<String, DescriptiveStatistics> entry : statsMap.entrySet()) {
			stats.put(TRAVEL_DISTANCE_PREFIX + "geo_" + entry.getKey(), entry.getValue().getMean());
			TDoubleDoubleHashMap hist = Histogram.createHistogram(entry.getValue(), new LinLogDiscretizer(500.0, 2));
			Histogram.normalize(hist);
			try {
				TXTWriter.writeMap(hist, "d", "n", String.format("%1$s/d.geo.%2$s.txt", output, entry.getKey()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	
	public static void main(String args[]) throws IOException {
		Scenario scenario = new ScenarioImpl();
		MatsimPopulationReader reader = new MatsimPopulationReader(scenario);
		reader.readFile("/Users/jillenberger/Work/socialnets/data/schweiz/mz2005/rawdata/plans.w_dist_obj2.xml");
		
		TravelDistanceTask task = new TravelDistanceTask();
		task.output = "/Users/jillenberger/Work/socialnets/data/schweiz/mz2005/rawdata/analysis.w_dist_obj2/";
		Map<String, Double> stats = PlansAnalyzer.analyzeSelectedPlans(scenario.getPopulation(), task);
		PlansAnalyzer.write(stats, "/Users/jillenberger/Work/socialnets/data/schweiz/mz2005/rawdata/stats.txt");
	}
}
