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
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.sna.graph.analysis.Transitivity;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.FixedSampleSizeDiscretizer;
import org.matsim.contrib.sna.util.TXTWriter;

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
		
		Discretizer discretizer = FixedSampleSizeDiscretizer.create(accessValues.getValues(), 50, 200);
		TDoubleDoubleHashMap correl = VertexPropertyCorrelation.mean(localClustering, accessValues,	discretizer);
		
		try {
			TXTWriter.writeMap(correl, "A", "c_local_mean", getOutputDirectory() + "c_local_mean_A.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
