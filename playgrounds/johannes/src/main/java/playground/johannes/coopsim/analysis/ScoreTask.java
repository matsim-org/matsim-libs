/* *********************************************************************** *
 * project: org.matsim.*
 * ScoreTask.java
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

import gnu.trove.TDoubleArrayList;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.regression.SimpleRegression;
import org.matsim.contrib.common.stats.DummyDiscretizer;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import playground.johannes.coopsim.ConvergenceCriterion;
import playground.johannes.coopsim.eval.ActivityEvaluator;
import playground.johannes.coopsim.eval.ActivityTypeEvaluator;
import playground.johannes.coopsim.eval.JointActivityEvaluator2;
import playground.johannes.coopsim.eval.LegEvaluator;
import playground.johannes.coopsim.pysical.Trajectory;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author illenberger
 * 
 */
public class ScoreTask extends TrajectoryAnalyzerTask implements ConvergenceCriterion {

	private final static int MIN_SAMPLES = 40;
	
	private final static double THRESHOLD = 0.001;
	
	private TDoubleArrayList scores = new TDoubleArrayList(1000);
	
	private boolean converged = false;
	
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		DescriptiveStatistics allScores = new DescriptiveStatistics();
		for(Trajectory t : trajectories)
			allScores.addValue(t.getPerson().getSelectedPlan().getScore());
		results.put("score", allScores);
		
		DescriptiveStatistics actScores = ActivityEvaluator.stopLogging();
		results.put("score_act", actScores);

		DescriptiveStatistics legScores = LegEvaluator.stopLogging();
		results.put("score_leg", legScores);
		
		Map<String, DescriptiveStatistics> jointScore = JointActivityEvaluator2.stopLogging();
//		Map<String, DescriptiveStatistics> jointScore = JointActivityEvaluator.stopLogging();
		for(Entry<String, DescriptiveStatistics> entry : jointScore.entrySet()) {
			results.put("score_join_" + entry.getKey(), entry.getValue());
		}
		
		
		DescriptiveStatistics typeScore = ActivityTypeEvaluator.stopLogging();
		results.put("score_type", typeScore);
			
		try {
			writeHistograms(allScores, "score", 50, 50);
			writeHistograms(actScores, "score_act", 50, 50);
			writeHistograms(legScores, "score_leg", 50, 50);
			for(Entry<String, DescriptiveStatistics> entry : jointScore.entrySet()) {
				writeHistograms(entry.getValue(), new LinearDiscretizer(0.5), "score_join_" + entry.getKey(), false);
				writeHistograms(entry.getValue(), "score_join_" + entry.getKey(), 50, 50);
			}
			
			writeHistograms(typeScore, new DummyDiscretizer(), "score_type", false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		scores.add(allScores.getMean());
		
		if (scores.size() >= MIN_SAMPLES) {
			SimpleRegression reg = new SimpleRegression();

			for (int i = scores.size() - MIN_SAMPLES; i < scores.size(); i++) {
				reg.addData(i, scores.get(i));
			}
			
			if(reg.getSlope() < THRESHOLD)
				converged = true;
		}
		
	}

	@Override
	public boolean achivedConvergence() {
		return converged;
	}

}
