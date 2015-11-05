/* *********************************************************************** *
 * project: org.matsim.*
 * CompleteSampleAnalyzer.java
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
 * An analyzer that analysis the sampled graph after the snowball sampling is
 * completed.
 * 
 * @author illenberger
 * 
 */
public class FinalSampleAnalyzer extends SampleAnalyzer {

	private static final String DIR_NAME = "final";

	/**
	 * @see {@link SampleAnalyzer#SampleAnalyzer(Map, Collection, String)}
	 */
	public FinalSampleAnalyzer(Map<String, AnalyzerTask> tasks, Collection<PiEstimator> estimators,
			String output) {
		super(tasks, estimators, output);
	}

	/**
	 * Returns always <tt>true</tt>.
	 * 
	 * @return <tt>true</tt>
	 */
	@Override
	public boolean afterSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
		return true;
	}

	/**
	 * Returns always <tt>true</tt>.
	 * 
	 * @return <tt>true</tt>
	 */
	@Override
	public boolean beforeSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
		return true;
	}

	/**
	 * Creates a directory named "final" for the analysis output and analysis
	 * the graph sampled by <tt>sampler</tt>.
	 * 
	 * @param sampler
	 *            a snowball sampler
	 */
	@Override
	public void endSampling(Sampler<?, ?, ?> sampler) {
		File file = makeDirectories(String.format("%1$s/%2$s", getRootDirectory(), DIR_NAME));
		analyze(sampler.getSampledGraph(), file.getAbsolutePath());		
	}

}
