/* *********************************************************************** *
 * project: org.matsim.*
 * AnalyzerTaskArray.java
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.sna.graph.analysis.GraphAnalyzer;

/**
 * @author illenberger
 *
 */
public class AnalyzerTaskArray extends AnalyzerTask {

	private Map<String, AnalyzerTask> analyzers;
	
	public AnalyzerTaskArray() {
		analyzers = new LinkedHashMap<String, AnalyzerTask>();
		
	}
	
	public void addAnalyzerTask(AnalyzerTask task, String key) {
		analyzers.put(key, task);
	}
	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		for(Entry<String, AnalyzerTask> entry : analyzers.entrySet()) {
			try {
				String output = String.format("%1$s/%2$s/", getOutputDirectory(), entry.getKey());
				new File(output).mkdirs();
				GraphAnalyzer.analyze(graph, entry.getValue(), output);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
