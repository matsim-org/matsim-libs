/* *********************************************************************** *
 * project: org.matsim.*
 * TripTask.java
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.sna.math.DescriptivePiStatistics;
import org.matsim.contrib.sna.math.FixedSampleSizeDiscretizer;
import org.matsim.contrib.sna.math.Histogram;
import org.matsim.contrib.sna.util.TXTWriter;

import playground.johannes.socialnetworks.graph.spatial.analysis.EdgeLength;

/**
 * @author illenberger
 *
 */
public class TripTask extends AnalyzerTask {

	/* (non-Javadoc)
	 * @see org.matsim.contrib.sna.graph.analysis.AnalyzerTask#analyze(org.matsim.contrib.sna.graph.Graph, java.util.Map)
	 */
	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		// TODO Auto-generated method stub

	}

	@Override
	public void analyzeStats(Graph graph, Map<String, DescriptiveStatistics> statsMap) {
		EdgeLength edgeLength = new EdgeLength();
		TObjectDoubleHashMap<Edge> edgeLengths = edgeLength.values(graph.getEdges());
		
		TObjectDoubleHashMap<Edge> f2fFreq = F2FFrequency.getInstance().values(graph.getEdges());
		
		DescriptivePiStatistics stats = new DescriptivePiStatistics();
		
		TObjectDoubleIterator<Edge> it = edgeLengths.iterator();
		for(int i = 0; i < edgeLengths.size(); i++) {
			it.advance();
			double d = it.value();
			double f = f2fFreq.get(it.key());
			if(f > 0 && d > 0)
				stats.addValue(d, 1/f);
		}
		
		TDoubleDoubleHashMap hist = Histogram.createHistogram(stats, FixedSampleSizeDiscretizer.create(stats.getValues(), 100, 50), true);
		Histogram.normalize(hist);
		try {
			TXTWriter.writeMap(hist, "d", "p_trip", getOutputDirectory() + "p_trip.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
