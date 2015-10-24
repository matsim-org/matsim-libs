/* *********************************************************************** *
 * project: org.matsim.*
 * ResponseRateTask.java
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
package org.matsim.contrib.socnetgen.socialnetworks.snowball2.analysis;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.socnetgen.sna.snowball.SampledGraph;
import org.matsim.contrib.socnetgen.sna.snowball.analysis.SnowballStatistics;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * @author illenberger
 *
 */
public class ResponseRateTask extends AnalyzerTask {

	@Override
	public void analyze(Graph g, Map<String, DescriptiveStatistics> stats) {
		SampledGraph graph = (SampledGraph) g;
		
		DescriptiveStatistics ds = new DescriptiveStatistics();
		ds.addValue(SnowballStatistics.getInstance().responseRateTotal(graph.getVertices(), SnowballStatistics.getInstance().lastIteration(graph.getVertices())));
		stats.put("responseRate", ds);
		
		if(getOutputDirectory() != null) {
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(getOutputDirectory() + "/responseRates.txt"));
				writer.write("iteration\tresponseRateTotal\tresponseRatePerIteration");
				writer.newLine();
				double[] rateTotal = SnowballStatistics.getInstance().responseRateTotal(graph.getVertices());
				double[] rate = SnowballStatistics.getInstance().responseRatePerIteration(graph.getVertices());
				
				for(int i = 0; i < rateTotal.length; i++) {
					writer.write(String.valueOf(i));
					writer.write("\t");
					writer.write(String.valueOf(rateTotal[i]));
					writer.write("\t");
					writer.write(String.valueOf(rate[i]));
					writer.newLine();
				}
				writer.close();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}

}
