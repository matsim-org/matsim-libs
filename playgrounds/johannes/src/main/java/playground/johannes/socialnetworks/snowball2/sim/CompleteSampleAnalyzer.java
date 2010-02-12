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

import org.matsim.contrib.sna.graph.Graph;

import playground.johannes.socialnetworks.graph.analysis.AnalyzerTask;

/**
 * @author illenberger
 *
 */
public class CompleteSampleAnalyzer extends SampleAnalyzer {

	private int numVertex;
	
	private String output;
	
	public CompleteSampleAnalyzer(Graph graph, String output, AnalyzerTask observed, AnalyzerTask estimated) {
		super(observed, estimated);
		this.output = output;
		numVertex = graph.getVertices().size();
	}
	
	@Override
	public boolean afterSampling(Sampler<?, ?, ?> sampler) {
		if(sampler.getNumSampledVertices() >= numVertex) {
			File file = new File(output + "/complete");
			file.mkdirs();
			analyse(sampler.getSampledGraph(), file.getAbsolutePath());
		}
		return true;
	}

	@Override
	public boolean beforeSampling(Sampler<?, ?, ?> sampler) {		
		return true;
	}

}
