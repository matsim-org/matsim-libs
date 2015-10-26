/* *********************************************************************** *
 * project: org.matsim.*
 * LogIntervalSampleAnalyzer.java
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
package org.matsim.contrib.socnetgen.sna.snowball.sim;

import org.matsim.contrib.socnetgen.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.socnetgen.sna.snowball.SampledVertexDecorator;
import org.matsim.contrib.socnetgen.sna.snowball.analysis.PiEstimator;

import java.util.Collection;
import java.util.Map;

/**
 * @author illenberger
 * 
 */
public class LogIntervalSampleAnalyzer extends IntervalSampleAnalyzer {

	private final double base;

	private final int div;

	private final int numSeeds;
	
	private double prev = -1;

	public LogIntervalSampleAnalyzer(Map<String, AnalyzerTask> tasks, Collection<PiEstimator> estimators,
			String output, double base, int div, int numSeeds) {
		super(tasks, estimators, output);
		this.base = base;
		this.div = div;
		this.numSeeds = numSeeds;
	}

	@Override
	public boolean afterSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
		int n = sampler.getNumSampledVertices();
		if(n == numSeeds) {
			dump(sampler);
		} else if (n >= div) {
			double log_n = Math.log(n / div) / Math.log(base);
			log_n = Math.floor(log_n);

			if (log_n > prev) {
				dump(sampler);
			}

			prev = log_n;
		}
		return true;
	}

}
