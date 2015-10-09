/* *********************************************************************** *
 * project: org.matsim.*
 * TripDistanceDegreeTask.java
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
import org.matsim.contrib.common.stats.FixedSampleSizeDiscretizer;
import org.matsim.contrib.common.stats.TXTWriter;
import org.matsim.facilities.ActivityFacilities;
import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.sna.graph.Vertex;
import playground.johannes.sna.graph.analysis.Degree;
import playground.johannes.socialnetworks.graph.analysis.VertexPropertyCorrelation;
import playground.johannes.socialnetworks.graph.social.SocialGraph;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public class TripDistanceDegreeTask extends TrajectoryAnalyzerTask {

	private final SocialGraph graph;
	
	private final ActivityFacilities facilities;
	
	public TripDistanceDegreeTask(SocialGraph graph, ActivityFacilities facilities) {
		this.graph = graph;
		this.facilities = facilities;
	}
	
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		TObjectDoubleHashMap<Vertex> xVals = Degree.getInstance().values(graph.getVertices());

		Set<String> purposes = new HashSet<String>();
		for (Trajectory t : trajectories) {
			for (int i = 0; i < t.getElements().size(); i += 2) {
				purposes.add(((Activity) t.getElements().get(i)).getType());
			}
		}
		purposes.add(null);
		
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
			
			if(purpose == null)
				purpose = "all";
			
			try {
				TXTWriter.writeMap(correl, "k", "d", String.format("%1$s/d_mean_k.%2$s.txt", getOutputDirectory(), purpose));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
