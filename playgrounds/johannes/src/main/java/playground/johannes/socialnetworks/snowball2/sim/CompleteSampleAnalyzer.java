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
package playground.johannes.socialnetworks.snowball2.sim;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import org.matsim.contrib.sna.graph.Graph;

import playground.johannes.socialnetworks.graph.analysis.AnalyzerTask;
import playground.johannes.socialnetworks.snowball2.SampledVertexDecorator;

/**
 * @author illenberger
 *
 */
public class CompleteSampleAnalyzer extends SampleAnalyzer {

//	private int numVertex;
	
	public CompleteSampleAnalyzer(Graph graph, Map<String, AnalyzerTask> tasks, Collection<BiasedDistribution> estimators, String output) {
		super(tasks, estimators, output);
//		numVertex = graph.getVertices().size();
	}
	
	@Override
	public boolean afterSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
//		if(sampler.getNumSampledVertices() >= numVertex) {
//			File file = makeDirectories(getRootDirectory() + "/complete");
//			analyse(sampler.getSampledGraph(), file.getAbsolutePath());
//		}
		return true;
	}

	@Override
	public boolean beforeSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {		
		return true;
	}

	/* (non-Javadoc)
	 * @see playground.johannes.socialnetworks.snowball2.sim.SamplerListener#endSampling(playground.johannes.socialnetworks.snowball2.sim.Sampler)
	 */
	@Override
	public void endSampling(Sampler<?, ?, ?> sampler) {
		File file = makeDirectories(getRootDirectory() + "/complete");
		analyse(sampler.getSampledGraph(), file.getAbsolutePath());
	}

}
