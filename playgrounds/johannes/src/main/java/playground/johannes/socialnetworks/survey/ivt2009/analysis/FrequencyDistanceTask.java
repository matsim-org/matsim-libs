/* *********************************************************************** *
 * project: org.matsim.*
 * FrequencyDistanceTask.java
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleDoubleHashMap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.sna.math.Distribution;

import playground.johannes.socialnetworks.graph.social.SocialEdge;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.statistics.Correlations;
import playground.johannes.socialnetworks.statistics.LogDiscretizer;

/**
 * @author illenberger
 *
 */
public class FrequencyDistanceTask extends AnalyzerTask {

	/* (non-Javadoc)
	 * @see org.matsim.contrib.sna.graph.analysis.AnalyzerTask#analyze(org.matsim.contrib.sna.graph.Graph, java.util.Map)
	 */
	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		
		SocialGraph g = (SocialGraph) graph;
		TDoubleArrayList values1 = new TDoubleArrayList();
		TDoubleArrayList values2 = new TDoubleArrayList();
		for(SocialEdge edge : g.getEdges()) {
			double freq = edge.getFrequency();
			double d = edge.length();
			if(!Double.isNaN(d)) {
				values1.add(freq);
				values2.add(d);
			}
		}
		
		TDoubleDoubleHashMap map = Correlations.correlationMean(values1.toNativeArray(), values2.toNativeArray(), new LogDiscretizer(1.12, 1.0, 52));
		try {
			Correlations.writeToFile(map, getOutputDirectory() + "d_freq.txt", "frequency", "distance");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
