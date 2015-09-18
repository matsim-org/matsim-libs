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


import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import playground.johannes.sna.graph.Graph;
import playground.johannes.sna.graph.analysis.DegreeTask;
import playground.johannes.sna.graph.analysis.TransitivityTask;
import playground.johannes.sna.snowball.SampledGraph;
import playground.johannes.sna.snowball.analysis.EstimatedDegree;
import playground.johannes.sna.snowball.analysis.EstimatedTransitivity;
import playground.johannes.sna.snowball.analysis.PiEstimator;
import playground.johannes.sna.snowball.analysis.SimplePiEstimator;
import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskComposite;
import playground.johannes.socialnetworks.snowball2.analysis.WSMStatsFactory;
import playground.johannes.socialnetworks.snowball2.sim.EstimatorTask;

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
