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

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import playground.johannes.coopsim.pysical.Trajectory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author illenberger
 *
 */
public class TrajectoryAnalyzer {
	
	private static boolean append = false;
	
	public static void setAppend(boolean append) {
		TrajectoryAnalyzer.append = append;
	}

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
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename, append));
		
//		writer.write("property\tmean\tmin\tmax\tmedian\tN\tvar");
		writer.write("property\tmean\tmin\tmax\tN\tvar");
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
//			Recently i faced situations where this methods appears to never return with large data sets. joh 11/2014
//			writer.write("\t");
//			writer.write(String.valueOf(entry.getValue().getPercentile(50)));
			writer.write("\t");
			writer.write(String.valueOf(entry.getValue().getN()));
			writer.write("\t");
			writer.write(String.valueOf(entry.getValue().getVariance()));
			writer.newLine();
		}
		
		writer.close();
	}
}
