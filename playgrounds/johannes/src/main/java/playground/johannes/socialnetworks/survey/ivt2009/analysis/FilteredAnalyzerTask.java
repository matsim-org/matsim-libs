/* *********************************************************************** *
 * project: org.matsim.*
 * FilteredAnalyzerTask.java
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

import playground.johannes.socialnetworks.graph.analysis.GraphFilter;

/**
 * @author illenberger
 *
 */
public class FilteredAnalyzerTask extends AnalyzerTask {

	private AnalyzerTask analyzer;
	
	private Map<String, GraphFilter<Graph>> filters;
	
	public FilteredAnalyzerTask(AnalyzerTask analyzer) {
		this.analyzer = analyzer;
		filters = new LinkedHashMap<String, GraphFilter<Graph>>();
	}
	
	public void addFilter(GraphFilter<? extends Graph> filter, String key) {
		filters.put(key, (GraphFilter<Graph>) filter);
	}
	
	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		for(Entry<String, GraphFilter<Graph>> entry : filters.entrySet()) {
			Graph filteredGraph = entry.getValue().apply(graph);
			String output = String.format("%1$s/%2$s", getOutputDirectory(), entry.getKey());
			new File(output).mkdirs();
			try {
				GraphAnalyzer.analyze(filteredGraph, analyzer, output);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}

	}

}
