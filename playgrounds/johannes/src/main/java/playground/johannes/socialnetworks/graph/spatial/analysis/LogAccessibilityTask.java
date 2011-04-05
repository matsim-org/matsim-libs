/* *********************************************************************** *
 * project: org.matsim.*
 * AccessabilityTask.java
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
package playground.johannes.socialnetworks.graph.spatial.analysis;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Distribution;

import playground.johannes.socialnetworks.gis.SpatialCostFunction;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class LogAccessibilityTask extends ModuleAnalyzerTask<LogAccessibility> {

	private Set<Point> choiceSet;;
	
	private boolean graphAsChoiceSet;
	
	private SpatialCostFunction costFunction;
	
	public LogAccessibilityTask(SpatialCostFunction costFunction) {
		setModule(new LogAccessibility());
		this.costFunction = costFunction;
		graphAsChoiceSet = true;
	}
	
	public LogAccessibilityTask(SpatialCostFunction costFunction, Set<Point> choiceSet) {
		this(costFunction);
		graphAsChoiceSet = false;
		this.choiceSet = choiceSet;
	}
	
	@Override
	public void analyze(Graph graph, Map<String, DescriptiveStatistics> statsMap) {
		if(getOutputDirectory() != null) {
			
			if(graphAsChoiceSet) {
				choiceSet = new HashSet<Point>();
				for(Vertex vertex : graph.getVertices())
					choiceSet.add(((SpatialVertex) vertex).getPoint());
			}
			
			Distribution distr = module.distribution((Set<? extends SpatialVertex>) graph.getVertices(), costFunction, choiceSet);
			
			try {
				writeHistograms(distr, (distr.max() - distr.min())/20.0, false, "access");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
