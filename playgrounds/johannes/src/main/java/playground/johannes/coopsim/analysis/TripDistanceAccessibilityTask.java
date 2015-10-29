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
package playground.johannes.coopsim.analysis;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TObjectDoubleHashMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.common.stats.FixedSampleSizeDiscretizer;
import org.matsim.contrib.common.stats.StatsWriter;
import org.matsim.contrib.socnetgen.sna.gis.GravityCostFunction;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.contrib.socnetgen.sna.graph.analysis.VertexPropertyCorrelation;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialGraph;
import org.matsim.contrib.socnetgen.sna.graph.spatial.analysis.Accessibility;
import org.matsim.contrib.socnetgen.sna.graph.spatial.analysis.GridAccessibility;
import org.matsim.facilities.ActivityFacilities;
import playground.johannes.coopsim.pysical.Trajectory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author illenberger
 * 
 */
public class TripDistanceAccessibilityTask extends TrajectoryAnalyzerTask {

	private final SocialGraph graph;

	private final Accessibility accessibility;

	private final ActivityFacilities facilities;

	public TripDistanceAccessibilityTask(SocialGraph graph, ActivityFacilities facilities) {
		this.facilities = facilities;
		this.graph = graph;
		this.accessibility = new GridAccessibility(new GravityCostFunction(1.4, 0, CartesianDistanceCalculator.getInstance()), 2000);
	}

	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		TObjectDoubleHashMap<Vertex> xVals = accessibility.values(graph.getVertices());

		Set<String> purposes = new HashSet<String>();
		for (Trajectory t : trajectories) {
			for (int i = 0; i < t.getElements().size(); i += 2) {
				purposes.add(((Activity) t.getElements().get(i)).getType());
			}
		}

		DualHashBidiMap bidiMap = new DualHashBidiMap();
		for (Trajectory t : trajectories) {
			bidiMap.put(t.getPerson(), t);
		}
		
		for (String purpose : purposes) {
			TripDistanceMean tripDist = new TripDistanceMean(purpose, facilities);
			PersonTrajectoryPropertyAdaptor pAdaptor = new PersonTrajectoryPropertyAdaptor(bidiMap, tripDist);
			VertexPersonPropertyAdaptor vAdaptor = new VertexPersonPropertyAdaptor(graph, pAdaptor);

			TObjectDoubleHashMap<Vertex> yVals = vAdaptor.values(graph.getVertices());

			TDoubleDoubleHashMap correl = VertexPropertyCorrelation.mean(yVals, xVals,
					FixedSampleSizeDiscretizer.create(xVals.getValues(), 50, 100));
			try {
				StatsWriter.writeHistogram(correl, "A", "d", getOutputDirectory() + "d_mean_A.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
