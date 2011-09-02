/* *********************************************************************** *
 * project: org.matsim.*
 * TripDistanceAccessibilityTask.java
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
package playground.johannes.socialnetworks.sim.analysis;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TObjectDoubleHashMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.sna.math.FixedSampleSizeDiscretizer;
import org.matsim.contrib.sna.util.TXTWriter;

import playground.johannes.coopsim.analysis.TripDistanceSum;
import playground.johannes.socialnetworks.gis.GravityCostFunction;
import playground.johannes.socialnetworks.graph.analysis.VertexPropertyCorrelation;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.spatial.analysis.Accessibility;
import playground.johannes.socialnetworks.sim.gis.ActivityDistanceCalculator;

/**
 * @author illenberger
 *
 */
public class TripDistanceAccessibilityTask extends AnalyzerTask {

	private final Accessibility accessibility;
	
	private final ActivityDistanceCalculator calculator;
	
	private Map<Person, Trajectory> trajectories;
	
	public TripDistanceAccessibilityTask(ActivityDistanceCalculator calculator) {
		this.calculator = calculator;
		this.accessibility = new Accessibility(new GravityCostFunction(1.4, 0));
	}
	
	public void setTrajectories(Map<Plan, Trajectory> map) {
		trajectories = new HashMap<Person, Trajectory>(map.size());
		for(Entry<Plan, Trajectory> entry : map.entrySet()) {
			trajectories.put(entry.getKey().getPerson(), entry.getValue());
		}
	}
	
	@Override
	public void analyze(Graph graph, Map<String, DescriptiveStatistics> results) {
		SocialGraph socialGraph = (SocialGraph) graph;
		
		TObjectDoubleHashMap<Vertex> xVals = accessibility.values(socialGraph.getVertices());
		
		TripDistanceSum tripDist = new TripDistanceSum("lindoor", calculator);
		PersonTrajectoryPropertyAdaptor pAdaptor = new PersonTrajectoryPropertyAdaptor(new DualHashBidiMap(trajectories), tripDist);
		VertexPersonPropertyAdaptor vAdaptor = new VertexPersonPropertyAdaptor(socialGraph, pAdaptor);
		
		TObjectDoubleHashMap<Vertex> yVals = vAdaptor.values(socialGraph.getVertices());
		
		TDoubleDoubleHashMap correl = VertexPropertyCorrelation.mean(yVals, xVals, FixedSampleSizeDiscretizer.create(xVals.getValues(), 50, 100));
		try {
			TXTWriter.writeMap(correl, "A", "d", getOutputDirectory() + "d_mean_A.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
