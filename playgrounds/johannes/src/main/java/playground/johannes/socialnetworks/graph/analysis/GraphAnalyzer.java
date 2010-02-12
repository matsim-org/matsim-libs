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
package playground.johannes.socialnetworks.graph.analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Graph;

/**
 * @author illenberger
 *
 */
public class GraphAnalyzer {
	
	public static final Logger logger = Logger.getLogger(GraphAnalyzer.class);

	public static Map<String, Double> analyze(Graph graph, AnalyzerTask task) {
		Map<String, Double> stats = new LinkedHashMap<String, Double>();
		task.analyze(graph, stats);
		return stats;
	}
	
	public static void writeStats(Map<String, Double> stats, String filename) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		
		writer.write("property\tvalue");
		writer.newLine();
		for(Entry<String, Double> entry : stats.entrySet()) {
			writer.write(entry.getKey());
			writer.write("\t");
			writer.write(entry.getValue().toString());
			writer.newLine();
		}
		writer.close();
	}
}
