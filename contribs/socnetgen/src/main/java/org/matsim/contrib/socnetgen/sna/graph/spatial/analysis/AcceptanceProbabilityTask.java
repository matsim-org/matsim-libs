/* *********************************************************************** *
 * project: org.matsim.*
 * AcceptanceProbabilityTask.java
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
package org.matsim.contrib.socnetgen.sna.graph.spatial.analysis;

import com.vividsolutions.jts.geom.Point;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.contrib.socnetgen.sna.gis.DistanceCalculatorFactory;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.contrib.socnetgen.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialVertex;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public class AcceptanceProbabilityTask extends ModuleAnalyzerTask<AcceptanceProbability> {

	private Set<Point> choiceSet;;
	
	private boolean graphAsChoiceSet;
	
	private boolean overwriteDistCalc = false;
	
	public AcceptanceProbabilityTask() {
		setModule(new AcceptanceProbability());
		graphAsChoiceSet = true;
	}

	public AcceptanceProbabilityTask(Set<Point> choiceSet) {
		this.choiceSet = choiceSet;
		setModule(new AcceptanceProbability());
		graphAsChoiceSet = false;
	}
	
	public void setDistanceCalculator(DistanceCalculator calculator) {
		module.setDistanceCalculator(calculator);
		overwriteDistCalc = true;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void analyze(Graph graph, Map<String, DescriptiveStatistics> statsMap) {
		if(getOutputDirectory() != null) {
			
			if(graphAsChoiceSet) {
				choiceSet = new HashSet<Point>();
				for(Vertex vertex : graph.getVertices())
					choiceSet.add(((SpatialVertex) vertex).getPoint());
			}
			
			if(!overwriteDistCalc)
				module.setDistanceCalculator(DistanceCalculatorFactory.createDistanceCalculator(((SpatialGraph)graph).getCoordinateReferenceSysten()));
			
			DescriptiveStatistics distr = module.distribution((Set<? extends SpatialVertex>) graph.getVertices(), choiceSet);
			try {
				writeHistograms(distr, new LinearDiscretizer(1000.0), "p_accept", false);				
				writeHistograms(distr, "p_accept", 100, 100);
				writeCumulativeHistograms(distr, "p_accept", 100, 200);
				writeRawData(distr, "p_accept");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}