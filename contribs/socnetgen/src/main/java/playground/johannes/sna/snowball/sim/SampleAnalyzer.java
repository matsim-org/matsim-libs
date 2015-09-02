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
package playground.johannes.sna.snowball.sim;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import playground.johannes.sna.graph.analysis.AnalyzerTask;
import playground.johannes.sna.graph.analysis.GraphAnalyzer;
import playground.johannes.sna.snowball.SampledGraphProjection;
import playground.johannes.sna.snowball.analysis.PiEstimator;

/**
 * Abstract base class for sample analyzers. Organizes the analyzers output into
 * directories and updates each estimator before analysis.
 * 
 * @author illenberger
 * 
 */
public abstract class SampleAnalyzer implements SamplerListener {

	private Map<String, AnalyzerTask> tasks;

	private Collection<PiEstimator> estimators;

	private String rootDirectory;

	/**
	 * Creates a new sample analyzer.
	 * 
	 * @param tasks
	 *            A map of analyzer tasks. The output of an analyzer task will
	 *            be written into a directory named with the entry key.
	 * @param estimators
	 *            A collection of estimators that need to be updated before
	 *            analysis.
	 * @param rootDirectory
	 *            The root directory for the analysis output.
	 */
	public SampleAnalyzer(Map<String, AnalyzerTask> tasks, Collection<PiEstimator> estimators,
			String rootDirectory) {
		this.tasks = tasks;
		this.rootDirectory = rootDirectory;
		this.estimators = estimators;
	}

	/**
	 * Returns the root directory for analysis output.
	 * 
	 * @return the root directory for analysis output.
	 */
	protected String getRootDirectory() {
		return rootDirectory;
	}

	/**
	 * Analysis the sampled graph will all analyzer tasks.
	 * 
	 * @param graph
	 *            A sampled graph.
	 * @param output
	 *            The output directory for the analysis output.
	 */
	protected void analyze(SampledGraphProjection<?, ?, ?> graph, String output) {
		try {
			/*
			 * Prevent the analyzer tasks from logging.
			 */
			Level level = Logger.getRootLogger().getLevel(); 
			Logger.getRootLogger().setLevel(Level.WARN);
			/*
			 * Update all estimators.
			 */
			for (PiEstimator estimator : estimators) {
				estimator.update(graph);
			}
			/*
			 * Analyze the graph with each analyzer taks.
			 */
			for (Entry<String, AnalyzerTask> task : tasks.entrySet()) {
				/*
				 * Create output directories.
				 */
				String taskOutput = String.format("%1$s/%2$s/", output, task.getKey());
				File file = makeDirectories(taskOutput);
				task.getValue().setOutputDirectoy(file.getAbsolutePath());
				/*
				 * Analyze.
				 */
				GraphAnalyzer.analyze(graph, task.getValue(), file.getAbsolutePath());
//				GraphAnalyzer.writeStats(stats, file.getAbsolutePath() + "/stats.txt");
			}
			
			Logger.getRootLogger().setLevel(level);
			
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
