/* *********************************************************************** *
 * project: org.matsim.*
 * AgeTask.java
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
package playground.johannes.socialnetworks.graph.social.analysis;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.sna.math.Distribution;

import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.statistics.Correlations;

/**
 * @author illenberger
 *
 */
public class AgeTask extends ModuleAnalyzerTask<Age> {

	private static final Logger logger = Logger.getLogger(AgeTask.class);
	
	public static final String AGE_MEAN = "age_mean";
	
	public static final String AGE_MIN = "age_min";
	
	public static final String AGE_MAX = "age_max";
	
	public AgeTask() {
		setModule(new Age());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		Age age = module;
		Distribution distr = age.distribution((Set<? extends SocialVertex>) graph.getVertices());

		double age_min = distr.min();
		double age_max = distr.max();
		double age_mean = distr.mean();
		
		logger.info(String.format("Mean age = %1$.4f, min age = %2$s, max age = %3$s.", age_mean, age_min, age_max));
		stats.put(AGE_MIN, age_min);
		stats.put(AGE_MAX, age_max);
		stats.put(AGE_MEAN, age_mean);
		
		if(getOutputDirectory() != null) {
			try {
				writeHistograms(distr, 1, false, "age.txt");
				Correlations.writeToFile(age.correlation((Set<? extends SocialVertex>) graph.getVertices()), getOutputDirectory() + "/r_age.txt", "age", "age_mean");
				
				module.boxplot((Set<? extends SocialVertex>) graph.getVertices(), getOutputDirectory() + "/age.boxplot.txt");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
