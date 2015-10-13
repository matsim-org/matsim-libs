/* *********************************************************************** *
 * project: org.matsim.*
 * APLTask.java
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
package playground.johannes.socialnetworks.graph.analysis;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import playground.johannes.sna.graph.Graph;
import playground.johannes.sna.graph.Vertex;
import playground.johannes.sna.graph.analysis.AnalyzerTask;
import playground.johannes.sna.graph.matrix.AdjacencyMatrix;
import playground.johannes.socialnetworks.graph.matrix.MatrixAPL;

import java.io.IOException;
import java.util.Map;

/**
 * @author illenberger
 *
 */
public class APLTask extends AnalyzerTask {

	private final static String KEY = "apl";
	
	private final boolean calcAPLDistr;	
	
	public APLTask() {
		calcAPLDistr = true;
	}
	
	public APLTask(boolean calcDistr) {
		calcAPLDistr = calcDistr;
	}

	@Override
	public void analyze(Graph graph, Map<String, DescriptiveStatistics> results) {
		AdjacencyMatrix<Vertex> y = new AdjacencyMatrix<Vertex>(graph);
		MatrixAPL module = new MatrixAPL();
		module.setCalcAPLDistribution(calcAPLDistr);

		DescriptiveStatistics stats = module.apl(y);
		
		results.put(KEY, stats);
		printStats(stats, KEY);
		
		if(calcAPLDistr && outputDirectoryNotNull()) {
			try {
				writeHistograms(stats, new LinearDiscretizer(1.0), KEY, false);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
