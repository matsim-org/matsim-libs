/* *********************************************************************** *
 * project: org.matsim.*
 * IntervalSampleAnalyzer.java
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
import java.util.Collection;
import java.util.Map;

import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;

import playground.johannes.socialnetworks.snowball2.SampledVertexDecorator;

/**
 * @author illenberger
 *
 */
public class IntervalSampleAnalyzer extends SampleAnalyzer {

//	private int base;
//	
//	private double exponent = 0.5;
//	
//	private boolean dump;
	
	public IntervalSampleAnalyzer(Map<String, AnalyzerTask> tasks, Collection<ProbabilityEstimator> estimators, String output) {
		super(tasks, estimators, output);
//		this.base = interval;
	}

	@Override
	public boolean afterSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
		int n = sampler.getNumSampledVertices();
		if(n < 1000) {
			if(n % 100 == 0)
				dump(sampler);
		} else if (n < 10000) {
			if(n % 1000 == 0)
				dump(sampler);
		} else {
			if(n % 10000 == 0)
				dump(sampler);
		}
		return true;
	}

	private void dump(Sampler<?, ?, ?> sampler) {
		File file = makeDirectories(String.format("%1$s/vertex.%2$s", getRootDirectory(), sampler.getNumSampledVertices()));
		analyse(sampler.getSampledGraph(), file.getAbsolutePath());
	}
	@Override
	public boolean beforeSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
		return true;
	}

	@Override
	public void endSampling(Sampler<?, ?, ?> sampler) {
	}

}
