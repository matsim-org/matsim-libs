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
import java.util.Map;

import playground.johannes.socialnetworks.graph.analysis.AnalyzerTask;
import playground.johannes.socialnetworks.graph.analysis.GraphAnalyzer;
import playground.johannes.socialnetworks.snowball2.SampledGraphProjection;

/**
 * @author illenberger
 *
 */
public abstract class SampleAnalyzer implements SamplerListener {

	private AnalyzerTask observed;
	
	private AnalyzerTask estimated;
	
	public SampleAnalyzer(AnalyzerTask observed, AnalyzerTask estimated) {
		this.observed = observed;
		this.estimated = estimated;
	}

	protected void analyse(SampledGraphProjection<?, ?, ?> graph, String output) {
		try {
			String obsOutput = String.format("%1$s/obs/", output);
			File file = new File(obsOutput);
			file.mkdirs();
			observed.setOutputDirectoy(file.getAbsolutePath());
			Map<String, Double> stats = GraphAnalyzer.analyze(graph, observed);
			GraphAnalyzer.writeStats(stats, file.getAbsolutePath() + "/stats.txt");
			
			String estimOutput = String.format("%1$s/estim/", output);
			file = new File(estimOutput);
			file.mkdirs();
			estimated.setOutputDirectoy(file.getAbsolutePath());
			stats = GraphAnalyzer.analyze(graph, estimated);
			GraphAnalyzer.writeStats(stats, file.getAbsolutePath() + "/stats.txt");
		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
