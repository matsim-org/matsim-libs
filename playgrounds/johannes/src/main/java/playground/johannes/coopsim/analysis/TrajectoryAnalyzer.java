/* *********************************************************************** *
 * project: org.matsim.*
 * TrajectoryAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.coopsim.analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import playground.johannes.coopsim.pysical.Trajectory;

/**
 * @author illenberger
 *
 */
public class TrajectoryAnalyzer {

	public static Map<String, DescriptiveStatistics> analyze(Set<Trajectory> trajectories, TrajectoryAnalyzerTask task) {
		Map<String, DescriptiveStatistics> results = new HashMap<String, DescriptiveStatistics>();
		task.analyze(trajectories, results);
		return results;
	}
	
	public static void analyze(Set<Trajectory> trajectories, TrajectoryAnalyzerTask task, String output) throws IOException {
		task.setOutputDirectory(output);
		Map<String, DescriptiveStatistics> results = analyze(trajectories, task);
		writeStatistics(results, output + "/statistics.txt");
	}
	
	public static void writeStatistics(Map<String, DescriptiveStatistics> statsMap, String filename) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		
		writer.write("property\tmean\tmin\tmax\tmedian\tN\tvar");
		writer.newLine();
		SortedMap<String, DescriptiveStatistics> sortedMap = new TreeMap<String, DescriptiveStatistics>(statsMap);
		for(Entry<String, DescriptiveStatistics> entry : sortedMap.entrySet()) {
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
