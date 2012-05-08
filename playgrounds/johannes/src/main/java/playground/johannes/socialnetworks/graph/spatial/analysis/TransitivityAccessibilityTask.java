/* *********************************************************************** *
 * project: org.matsim.*
 * TransitivityAccessibilityTask.java
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
package playground.johannes.socialnetworks.graph.spatial.analysis;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TObjectDoubleHashMap;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import playground.johannes.sna.graph.Graph;
import playground.johannes.sna.graph.Vertex;
import playground.johannes.sna.graph.analysis.ModuleAnalyzerTask;
import playground.johannes.sna.graph.analysis.Transitivity;
import playground.johannes.sna.math.Discretizer;
import playground.johannes.sna.math.FixedSampleSizeDiscretizer;
import playground.johannes.sna.util.TXTWriter;
import playground.johannes.socialnetworks.graph.analysis.VertexPropertyCorrelation;

/**
 * @author illenberger
 *
 */
public class TransitivityAccessibilityTask extends ModuleAnalyzerTask<Accessibility> {

	private TObjectDoubleHashMap<Vertex> accessValues;
	
	public TransitivityAccessibilityTask(Accessibility module) {
		setModule(module);
	}
	
	public void setAccessValues(TObjectDoubleHashMap<Vertex> accessValues) {
		this.accessValues = accessValues;
	}
	
	@Override
	public void analyze(Graph graph, Map<String, DescriptiveStatistics> statsMap) {
		if(accessValues == null) {
			accessValues = module.values(graph.getVertices());
		}
		
		TObjectDoubleHashMap<Vertex> localClustering = Transitivity.getInstance().values(graph.getVertices());
		
		Discretizer discretizer = FixedSampleSizeDiscretizer.create(accessValues.getValues(), 1, 30);
		TDoubleDoubleHashMap correl = VertexPropertyCorrelation.mean(localClustering, accessValues,	discretizer);
		
		try {
			TXTWriter.writeMap(correl, "A", "c_local_mean", getOutputDirectory() + "c_local_mean_A.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
