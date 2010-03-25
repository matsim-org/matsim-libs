/* *********************************************************************** *
 * project: org.matsim.*
 * SampleAnalyzer.java
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
package playground.johannes.socialnetworks.snowball2.sim;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import playground.johannes.socialnetworks.graph.analysis.AnalyzerTask;
import playground.johannes.socialnetworks.graph.analysis.GraphAnalyzer;
import playground.johannes.socialnetworks.snowball2.SampledGraphProjection;

/**
 * @author illenberger
 *
 */
public abstract class SampleAnalyzer implements SamplerListener {

	private Map<String, AnalyzerTask> tasks;
	
	private Collection<BiasedDistribution> estimators;
	
	private String rootDirectory;
	
	public SampleAnalyzer(Map<String, AnalyzerTask> tasks, Collection<BiasedDistribution> estimators, String rootDirectory) {
		this.tasks = tasks;
		this.rootDirectory = rootDirectory;
		this.estimators = estimators;
	}

	protected String getRootDirectory() {
		return rootDirectory;
	}
	
	protected void analyse(SampledGraphProjection<?, ?, ?> graph, String output) {
		try {
			Logger.getRootLogger().setLevel(Level.WARN);
			for(BiasedDistribution estimator : estimators) {
				estimator.update(graph);
			}
			for(Entry<String, AnalyzerTask> task : tasks.entrySet()) {
				String taskOutput = String.format("%1$s/%2$s/", output, task.getKey());
				File file = makeDirectories(taskOutput);
				task.getValue().setOutputDirectoy(file.getAbsolutePath());
				Map<String, Double> stats = GraphAnalyzer.analyze(graph, task.getValue());
				GraphAnalyzer.writeStats(stats, file.getAbsolutePath() + "/stats.txt");
			}
			Logger.getRootLogger().setLevel(Level.ALL);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected File makeDirectories(String path) {
		File file = new File(path);
		file.mkdirs();
		return file;
	}
}
