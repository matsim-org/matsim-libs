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
import java.util.Map;
import java.util.Set;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Distribution;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class AcceptanceProbabilityTask extends ModuleAnalyzerTask<AcceptanceProbability> {

	private final Set<Point> choiceSet;;
	
	public AcceptanceProbabilityTask(Set<Point> choiceSet) {
		this.choiceSet = choiceSet;
		setModule(new AcceptanceProbability());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		if(getOutputDirectory() != null) {
			Distribution distr = module.distribution((Set<? extends SpatialVertex>) graph.getVertices(), choiceSet);
			try {
				writeHistograms(distr, 1000, true, "p_accept");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
