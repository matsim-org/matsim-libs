/* *********************************************************************** *
 * project: org.matsim.*
 * AccessibilityTask.java
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
package org.matsim.contrib.socnetgen.sna.graph.spatial.analysis;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.contrib.socnetgen.sna.gis.SpatialCostFunction;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.analysis.AnalyzerTask;

import java.io.IOException;
import java.util.Map;

/**
 * @author illenberger
 *
 */
public class AccessibilityTask extends AnalyzerTask {

	private final SpatialCostFunction function;
	
	public AccessibilityTask(SpatialCostFunction function) {
		this.function = function;
	}
	
	@Override
	public void analyze(Graph graph, Map<String, DescriptiveStatistics> results) {
		Accessibility access = new Accessibility(function);

		DescriptiveStatistics stats = access.statistics(graph.getVertices());
		try {
			Discretizer disc = new LinearDiscretizer(stats.getValues(), 74);
			writeHistograms(stats, disc, "A", false);
			writeHistograms(stats, "A", 74, 1);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
