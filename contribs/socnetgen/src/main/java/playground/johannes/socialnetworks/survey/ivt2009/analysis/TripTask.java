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
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.stats.DescriptivePiStatistics;
import org.matsim.contrib.common.stats.FixedSampleSizeDiscretizer;
import org.matsim.contrib.common.stats.Histogram;
import org.matsim.contrib.common.stats.StatsWriter;
import playground.johannes.sna.graph.Edge;
import playground.johannes.sna.graph.Graph;
import playground.johannes.sna.graph.analysis.AnalyzerTask;
import playground.johannes.socialnetworks.graph.social.analysis.F2FFrequency;
import playground.johannes.socialnetworks.graph.spatial.analysis.EdgeLength;
import playground.johannes.socialnetworks.snowball2.analysis.WSMStatsFactory;

import java.io.IOException;
import java.util.Map;

/**
 * @author illenberger
 *
 */
public class TripTask extends AnalyzerTask {

	@Override
	public void analyze(Graph graph, Map<String, DescriptiveStatistics> statsMap) {
		EdgeLength edgeLength = new EdgeLength();
		TObjectDoubleHashMap<Edge> edgeLengths = edgeLength.values(graph.getEdges());
		
		TObjectDoubleHashMap<Edge> f2fFreq = F2FFrequency.getInstance().values(graph.getEdges());
		
		DescriptivePiStatistics stats = new WSMStatsFactory().newInstance();
		
		int count = 0;
		
		TObjectDoubleIterator<Edge> it = edgeLengths.iterator();
		for(int i = 0; i < edgeLengths.size(); i++) {
			it.advance();
			double d = it.value();
			double f = f2fFreq.get(it.key());
			if(f > 0 && d > 0) {
				stats.addValue(d, 1/f);
				count++;
			}
		}
		
		System.out.println("Number of edges with dist and freq: " + count);
		statsMap.put("trips", stats);
		TDoubleDoubleHashMap hist = Histogram.createHistogram(stats, FixedSampleSizeDiscretizer.create(stats.getValues(), 1, 50), true);
		Histogram.normalize(hist);
		try {
			StatsWriter.writeHistogram(hist, "d", "p_trip", getOutputDirectory() + "p_trip.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
