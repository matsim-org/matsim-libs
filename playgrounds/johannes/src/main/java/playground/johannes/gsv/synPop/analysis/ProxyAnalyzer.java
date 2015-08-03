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
package playground.johannes.gsv.synPop.analysis;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import playground.johannes.synpop.data.PlainPerson;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author illenberger
 *
 */
public class ProxyAnalyzer {
	
	private static boolean append = false;
	
	public static void setAppend(boolean append) {
		ProxyAnalyzer.append = append;
	}

	public static Map<String, DescriptiveStatistics> analyze(Collection<PlainPerson> person, AnalyzerTask task) {
		Map<String, DescriptiveStatistics> results = new HashMap<String, DescriptiveStatistics>();
		task.analyze(person, results);
		return results;
	}
	
	public static void analyze(Collection<PlainPerson> persons, AnalyzerTask task, String output) throws IOException {
		task.setOutputDirectory(output);
		Map<String, DescriptiveStatistics> results = analyze(persons, task);
		writeStatistics(results, output + "/statistics.txt");
	}
	
	public static void writeStatistics(Map<String, DescriptiveStatistics> statsMap, String filename) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename, append));
		
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
