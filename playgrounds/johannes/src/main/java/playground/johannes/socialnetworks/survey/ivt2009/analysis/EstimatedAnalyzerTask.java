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

import org.matsim.contrib.sna.graph.analysis.DegreeTask;
import org.matsim.contrib.sna.graph.analysis.TransitivityTask;
import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.sim.ProbabilityEstimator;

import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskComposite;
import playground.johannes.socialnetworks.snowball2.analysis.EstimatedDegree;
import playground.johannes.socialnetworks.snowball2.analysis.EstimatedTransitivity;
import playground.johannes.socialnetworks.snowball2.sim.Estimator1;
import playground.johannes.socialnetworks.snowball2.sim.EstimatorTask;
import playground.johannes.socialnetworks.snowball2.sim.NormalizedEstimator;
import playground.johannes.socialnetworks.snowball2.sim.deprecated.HTEstimator;

/**
 * @author illenberger
 *
 */
public class EstimatedAnalyzerTask extends AnalyzerTaskComposite {

	private final static int N = 5200000;
	
	public EstimatedAnalyzerTask(SampledGraph graph) {
//		ProbabilityEstimator estim = new Estimator1(N);
		ProbabilityEstimator estim = new NormalizedEstimator(new Estimator1(N), N);
		estim.update(graph);
		
		DegreeTask kTask = new DegreeTask();
		kTask.setModule(new EstimatedDegree(estim, new HTEstimator(N), new HTEstimator(N)));
		addTask(kTask);
		
		TransitivityTask tTask = new TransitivityTask();
		tTask.setModule(new EstimatedTransitivity(estim, new HTEstimator(N)));
		addTask(tTask);
		
		addTask(new EstimatorTask(estim));
	}
}
