/* *********************************************************************** *
 * project: org.matsim.*
 * EstimatedAnalyzerTask.java
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis;


import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.analysis.DegreeTask;
import org.matsim.contrib.socnetgen.sna.graph.analysis.TransitivityTask;
import org.matsim.contrib.socnetgen.sna.snowball.SampledGraph;
import org.matsim.contrib.socnetgen.sna.snowball.analysis.EstimatedDegree;
import org.matsim.contrib.socnetgen.sna.snowball.analysis.EstimatedTransitivity;
import org.matsim.contrib.socnetgen.sna.snowball.analysis.PiEstimator;
import org.matsim.contrib.socnetgen.sna.snowball.analysis.SimplePiEstimator;
import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskComposite;
import playground.johannes.socialnetworks.snowball2.analysis.WSMStatsFactory;
import playground.johannes.socialnetworks.snowball2.sim.EstimatorTask;

import java.util.Map;

/**
 * @author illenberger
 *
 */
public class EstimatedAnalyzerTask extends AnalyzerTaskComposite {

	private final static int N = 5200000;
	
	private final PiEstimator estim;
	
	public EstimatedAnalyzerTask(SampledGraph graph) {
		estim = new SimplePiEstimator(N);
		
		DegreeTask kTask = new DegreeTask();
		kTask.setModule(new EstimatedDegree(estim, new WSMStatsFactory()));
		addTask(kTask);
		
		TransitivityTask tTask = new TransitivityTask();
		tTask.setModule(new EstimatedTransitivity(estim, new WSMStatsFactory(), true));
		addTask(tTask);
		
		addTask(new EstimatorTask(estim));
	}

	@Override
	public void analyze(Graph graph, Map<String, DescriptiveStatistics> statsMap) {
		estim.update((SampledGraph) graph);
		super.analyze(graph, statsMap);
	}
}
