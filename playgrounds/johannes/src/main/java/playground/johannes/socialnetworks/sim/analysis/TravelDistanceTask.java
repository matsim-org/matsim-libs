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
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.sna.math.FixedSampleSizeDiscretizer;
import org.matsim.contrib.sna.math.Histogram;
import org.matsim.contrib.sna.math.LinearDiscretizer;
import org.matsim.contrib.sna.util.TXTWriter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * @author illenberger
 *
 */
public class TravelDistanceTask implements PlansAnalyzerTask {

	private static final String TRAVEL_DISTANCE_PREFIX = "d_mean_";
	
	private Network network;
	
	private String output;
	
	public TravelDistanceTask(Network network, String output) {
		this.network = network;
		this.output = output;
	}
	
	@Override
	public void analyze(Set<Plan> plans, Map<String, Double> stats) {
		TravelDistance distance = new TravelDistance(network);
		Map<String, DescriptiveStatistics> statsMap = distance.statistics(plans);
		
		for(Entry<String, DescriptiveStatistics> entry : statsMap.entrySet()) {
			stats.put(TRAVEL_DISTANCE_PREFIX + entry.getKey(), entry.getValue().getMean());
			TDoubleDoubleHashMap hist = Histogram.createHistogram(entry.getValue(), FixedSampleSizeDiscretizer.create(entry.getValue().getValues(), 1, 50), true);
//			TDoubleDoubleHashMap hist = Histogram.createHistogram(entry.getValue(), new LinLogDiscretizer(500.0, 2));
			hist.remove(0.0);
			Histogram.normalize(hist);
			try {
				TXTWriter.writeMap(hist, "d", "n", String.format("%1$s/d.%2$s.fixed.txt", output, entry.getKey()));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			hist = Histogram.createHistogram(entry.getValue(), new LinearDiscretizer(1000.0), false);
//			Histogram.normalize(hist);
			try {
				TXTWriter.writeMap(hist, "d", "n", String.format("%1$s/d.%2$s.1000.txt", output, entry.getKey()));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
		}
		
		distance.setGeodesicMode(true);
		statsMap = distance.statistics(plans);
		
		for(Entry<String, DescriptiveStatistics> entry : statsMap.entrySet()) {
			stats.put(TRAVEL_DISTANCE_PREFIX + "geo_" + entry.getKey(), entry.getValue().getMean());
//			TDoubleDoubleHashMap hist = Histogram.createHistogram(entry.getValue(), new LinLogDiscretizer(500.0, 2));
			TDoubleDoubleHashMap hist = Histogram.createHistogram(entry.getValue(), FixedSampleSizeDiscretizer.create(entry.getValue().getValues(), 1, 50), true);
			hist.remove(0.0);
			Histogram.normalize(hist);
			try {
				TXTWriter.writeMap(hist, "d", "n", String.format("%1$s/d.geo.%2$s.fixed.txt", output, entry.getKey()));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			hist = Histogram.createHistogram(entry.getValue(), new LinearDiscretizer(1000.0), false);
//			Histogram.normalize(hist);
			try {
				TXTWriter.writeMap(hist, "d", "n", String.format("%1$s/d.geo.%2$s.1000.txt", output, entry.getKey()));
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.err.println("Num trips: " + entry.getKey() +" = "+entry.getValue().getN());
		}
	}

	
	public static void main(String args[]) throws IOException {
		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile("/Volumes/cluster.math.tu-berlin.de/net/ils2/jillenberger/locationChoice/data/ivtch.xml");
		
		MatsimPopulationReader reader = new MatsimPopulationReader(scenario);
		reader.readFile("/Users/jillenberger/Work/socialnets/data/schweiz/mz2005/rawdata/plans.xml");
		
		TravelDistanceTask task = new TravelDistanceTask(scenario.getNetwork(), "/Users/jillenberger/Work/socialnets/data/schweiz/mz2005/rawdata/analysis/");
		Map<String, Double> stats = PlansAnalyzer.analyzeSelectedPlans(scenario.getPopulation(), task);
//		PlansAnalyzer.write(stats, "/Users/jillenberger/Work/socialnets/locationChoice/output/3.analysis/stats.txt");
	}
}
