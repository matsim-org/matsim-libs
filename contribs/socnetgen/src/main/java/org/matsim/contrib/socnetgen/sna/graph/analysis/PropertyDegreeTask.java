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
package org.matsim.contrib.socnetgen.sna.graph.analysis;

import gnu.trove.map.hash.TDoubleDoubleHashMap;
import gnu.trove.map.hash.TDoubleObjectHashMap;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.stats.StatsWriter;
import org.matsim.contrib.socnetgen.sna.graph.Graph;


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
