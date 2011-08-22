/* *********************************************************************** *
 * project: org.matsim.*
 * ScoreAnalyzerTask.java
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

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import playground.johannes.socialnetworks.sim.interaction.JointActivityScoringFunctionFactory;
import playground.johannes.socialnetworks.sim.locationChoice.MultiPlanSelector;

/**
 * @author illenberger
 *
 */
public class ScoreAnalyzerTask extends TrajectoryAnalyzerTask {

	private MultiPlanSelector selector;
	
	private JointActivityScoringFunctionFactory factory;
	
	public ScoreAnalyzerTask(MultiPlanSelector selector, JointActivityScoringFunctionFactory factory) {
		this.selector = selector;
		this.factory = factory;
	}
	
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		results.put("scores_old", selector.getOldScores());
		results.put("scores_new", selector.getNewScores());
		results.put("scores_delta", selector.getDeltaScores());
		results.put("scores_accepted", selector.getAcceptedScores());
		results.put("scores_rejected", selector.getRejectedScores());
		results.put("transitionproba", selector.getTransitionProbas());
		
		DescriptiveStatistics stats = factory.socialScoreStatistics();
		results.put("scores_social", stats);
		
		try {
			writeHistograms(selector.getOldScores(), "scores_old", 50, 1);
			writeHistograms(selector.getNewScores(), "scores_new", 50, 1);
			writeHistograms(selector.getDeltaScores(), "scores_delta", 50, 1);
			writeHistograms(selector.getAcceptedScores(), "scores_accepted", 50, 1);
			writeHistograms(selector.getRejectedScores(), "scores_rejected", 50, 1);
			writeHistograms(selector.getTransitionProbas(), "transitionproba", 50, 1);
			writeHistograms(stats, "scores_social", 50, 1);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
