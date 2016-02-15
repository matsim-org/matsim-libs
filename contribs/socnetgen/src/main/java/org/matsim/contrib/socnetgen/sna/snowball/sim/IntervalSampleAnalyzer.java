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
package org.matsim.contrib.socnetgen.sna.snowball.sim;

import org.matsim.contrib.socnetgen.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.socnetgen.sna.snowball.SampledVertexDecorator;
import org.matsim.contrib.socnetgen.sna.snowball.analysis.PiEstimator;

import java.io.File;
import java.util.Collection;
import java.util.Map;


/**
 * An analyzer that analysis the sampled graph in the following intervals:
 * <ul>
 * <li>after 100 additionally sampled vertices until 1000 sampled vertices</li>
 * <li>after 1000 additionally sampled vertices until 10000 sampled vertices</li>
 * <li>after 10000 additionally sampled vertices.</li>
 * </ul>
 * 
 * @author illenberger
 * 
 */
public class IntervalSampleAnalyzer extends SampleAnalyzer {

	private int prevN = 0;
	
	/**
	 * @see {@link SampleAnalyzer#SampleAnalyzer(Map, Collection, String)}
	 */
	public IntervalSampleAnalyzer(Map<String, AnalyzerTask> tasks, Collection<PiEstimator> estimators,
			String output) {
		super(tasks, estimators, output);
	}

	/**
	 * Analysis the sampled graph after a given number of vertices has been
	 * sampled.
	 * 
	 * @param sampler
	 *            a snowball sampler
	 * @param vertex
	 *            the last sampled vertex
	 * @return <tt>true</tt>
	 */
	@Override
	public boolean afterSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
		int n = sampler.getNumSampledVertices();
		if (n > prevN) {
			if (n < 200) {
				if(n % 10 == 0)
					dump(sampler);
			} else if (n < 1000) {
				if (n % 100 == 0)
					dump(sampler);
			} else if (n < 10000) {
				if (n % 1000 == 0)
					dump(sampler);
			} else {
				if (n % 5000 == 0)
					dump(sampler);
			}
		}
		prevN = n;
		return true;
	}

	protected void dump(Sampler<?, ?, ?> sampler) {
		File file = makeDirectories(String.format("%1$s/vertex.%2$s", getRootDirectory(), sampler
				.getNumSampledVertices()));
		analyze(sampler.getSampledGraph(), file.getAbsolutePath());
	}

	/**
	 * Returns always <tt>true</tt>
	 * 
	 * @return <tt>true</tt>
	 */
	@Override
	public boolean beforeSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
		return true;
	}

	/**
	 * Makes a dump with the maximum network sampled.
	 */
	@Override
	public void endSampling(Sampler<?, ?, ?> sampler) {
		dump(sampler);
	}

}
