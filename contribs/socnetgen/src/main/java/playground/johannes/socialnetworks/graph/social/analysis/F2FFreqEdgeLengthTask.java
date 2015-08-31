/* *********************************************************************** *
 * project: org.matsim.*
 * F2FFreqEdgeLengthTask.java
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
package playground.johannes.socialnetworks.graph.social.analysis;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TObjectDoubleHashMap;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import playground.johannes.sna.graph.Edge;
import playground.johannes.sna.graph.Graph;
import playground.johannes.sna.graph.analysis.AnalyzerTask;
import playground.johannes.sna.math.Discretizer;
import playground.johannes.sna.math.FixedSampleSizeDiscretizer;
import playground.johannes.sna.util.TXTWriter;
import playground.johannes.socialnetworks.graph.analysis.VertexPropertyCorrelation;
import playground.johannes.socialnetworks.graph.spatial.analysis.EdgeLength;

/**
 * @author illenberger
 *
 */
public class F2FFreqEdgeLengthTask extends AnalyzerTask {

	@Override
	public void analyze(Graph graph, Map<String, DescriptiveStatistics> results) {
		EdgeLength len = new EdgeLength();
		len.setIgnoreZero(true);
		
		TObjectDoubleHashMap<Edge> lenVals = len.values(graph.getEdges());
		Discretizer discretizer = FixedSampleSizeDiscretizer.create(lenVals.getValues(), 200, 100);
		
		
		TDoubleDoubleHashMap hist = VertexPropertyCorrelation.mean(F2FFrequency.getInstance(), len, graph.getEdges(), discretizer);
		try {
			TXTWriter.writeMap(hist, "d", "f", getOutputDirectory() + "/f2ffreq_d.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
