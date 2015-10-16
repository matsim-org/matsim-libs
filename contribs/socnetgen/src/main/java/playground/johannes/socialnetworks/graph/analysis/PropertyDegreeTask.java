/* *********************************************************************** *
 * project: org.matsim.*
 * TransitivityDegreeTask.java
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
package playground.johannes.socialnetworks.graph.analysis;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleObjectHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.stats.StatsWriter;
import playground.johannes.sna.graph.Graph;
import playground.johannes.sna.graph.analysis.Degree;
import playground.johannes.sna.graph.analysis.ModuleAnalyzerTask;
import playground.johannes.sna.graph.analysis.Transitivity;

import java.io.IOException;
import java.util.Map;


/**
 * @author illenberger
 * 
 */
public class PropertyDegreeTask extends ModuleAnalyzerTask<Degree> {
	
	@Override
	public void analyze(Graph graph, Map<String, DescriptiveStatistics> statsMap) {
		if (outputDirectoryNotNull()) {
			try {
				TDoubleDoubleHashMap map = VertexPropertyCorrelation.mean(Transitivity.getInstance(), module, graph.getVertices());
				StatsWriter.writeHistogram(map, "k", "c_local", getOutputDirectory() + "/c_k.mean.txt");
				
				TDoubleObjectHashMap<DescriptiveStatistics> stat = VertexPropertyCorrelation.statistics(Transitivity.getInstance(), module, graph.getVertices());
				StatsWriter.writeBoxplotStats(stat, getOutputDirectory() + "/c_k.table.txt");
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
