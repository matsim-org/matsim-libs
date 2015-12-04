/* *********************************************************************** *
 * project: org.matsim.*
 * IterationSampleAnalyzer.java
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
 * Analysis the sampled graph after a snowball iteration has been completed.
 * 
 * @author illenberger
 * 
 */
public class IterationSampleAnalyzer extends SampleAnalyzer {

	private int lastIteration;

	/**
	 * @see {@link SampleAnalyzer#SampleAnalyzer(Map, Collection, String)}
	 */
	public IterationSampleAnalyzer(Map<String, AnalyzerTask> tasks, Collection<PiEstimator> estimators,
			String output) {
		super(tasks, estimators, output);
		lastIteration = 0;
	}

	/**
	 * Always returns <tt>true</tt>.
	 * 
	 * @return <tt>true</tt>
	 */
	@Override
	public boolean afterSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
		return true;
	}

	/**
	 * Analysis the sampled graph after a snowball iteration has been completed.
	 * 
	 * @param sampler
	 *            a snowball sampler
	 * @param vertex
	 *            the last sampled vertex
	 * @return <tt>true</tt>
	 */
	@Override
	public boolean beforeSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
		if (sampler.getIteration() > lastIteration) {
			File file = makeDirectories(String.format("%1$s/it.%2$s", getRootDirectory(), lastIteration));
			analyze(sampler.getSampledGraph(), file.getAbsolutePath());
			lastIteration = sampler.getIteration();
		}

		return true;
	}

	/**
	 * Does nothing.
	 */
	@Override
	public void endSampling(Sampler<?, ?, ?> sampler) {
	}

}
