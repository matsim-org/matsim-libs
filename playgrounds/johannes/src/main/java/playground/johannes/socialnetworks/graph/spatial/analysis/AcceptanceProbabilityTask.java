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
package playground.johannes.socialnetworks.graph.spatial.analysis;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.LinearDiscretizer;

import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculatorFactory;

import com.vividsolutions.jts.geom.Point;

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
	public void analyze(Graph graph, Map<String, Double> stats) {
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
				writeHistograms(distr, new LinearDiscretizer(1000.0), "p_accept");				
				writeHistograms(distr, "p_accept", 500, 100);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}