/* *********************************************************************** *
 * project: org.matsim.*
 * GraphAnalyzer.java
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
package org.matsim.contrib.socnetgen.sna.graph.analysis;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.socnetgen.sna.graph.Graph;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Wrapper class for the execution of analyzer tasks.
 * 
 * @author illenberger
 * 
 */
public class GraphAnalyzer {

	/**
	 * Applies the analyzer task on a graph and return the results in a map.
	 * 
	 * @see {@link AnalyzerTask}
	 * @param graph
	 *            a graph.
	 * @param task
	 *            an analyzer task.
	 * @return the results of the analyzer.
	 */
//	public static Map<String, Double> analyze(Graph graph, AnalyzerTask task) {
//		Map<String, Double> stats = new LinkedHashMap<String, Double>();
//		task.analyze(graph, stats);
//		return stats;
//	}

	public static Map<String, DescriptiveStatistics> analyze(Graph graph, AnalyzerTask task) {
		Map<String, DescriptiveStatistics> statsMap = new LinkedHashMap<String, DescriptiveStatistics>();
		task.analyze(graph, statsMap);
		return statsMap;
	}
	/**
	 * Applies the analyzer task on a graph and writes the results into
	 * <tt>output/summary.txt</tt>.
	 * 
	 * @see {@link AnalyzerTask}
	 * @param graph
	 *            a graph.
	 * @param task
	 *            an analyzer task.
	 * @param output
	 *            the output directory.
	 * @throws IOException
	 *             if the output directory does not exists.
	 */
	public static void analyze(Graph graph, AnalyzerTask task, String output) throws IOException {
		task.setOutputDirectoy(output);
//		Map<String, Double> stats = analyze(graph, task);
//		writeStats(stats, String.format("%1$s/summary.txt", output));
		
		Map<String, DescriptiveStatistics> statsMap = analyze(graph, task);
		writeStatistics(statsMap, String.format("%1$s/statistics.txt", output));
	}

	/**
	 * Writes the results of an analyzer task to file.
	 * 
	 * @param stats
	 *            the results of an analyzer task.
	 * @param filename
	 *            the file where to write the results.
	 * @throws IOException
	 */
	public static void writeStats(Map<String, Double> stats, String filename) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

		writer.write("property\tvalue");
		writer.newLine();
		for (Entry<String, Double> entry : stats.entrySet()) {
			writer.write(entry.getKey());
			writer.write("\t");
			writer.write(entry.getValue().toString());
			writer.newLine();
		}
		writer.close();
	}
	
	public static void writeStatistics(Map<String, DescriptiveStatistics> statsMap, String filename) throws IOException {
		writeStatistics(statsMap, filename, false);
	}
	
	public static void writeStatistics(Map<String, DescriptiveStatistics> statsMap, String filename, boolean append) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename, append));
		
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
