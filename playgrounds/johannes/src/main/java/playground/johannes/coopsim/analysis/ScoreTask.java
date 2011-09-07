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

import gnu.trove.TObjectDoubleHashMap;

import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Person;

import playground.johannes.coopsim.eval.EvalEngine;
import playground.johannes.coopsim.pysical.Trajectory;

/**
 * @author illenberger
 * 
 */
public class ScoreTask extends TrajectoryAnalyzerTask {

	private final EvalEngine eval;
	
	public ScoreTask(EvalEngine eval) {
		this.eval = eval;
	}
	
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		DescriptiveStatistics joinScoreStats = new DescriptiveStatistics();
		DescriptiveStatistics totalScoreStats = new DescriptiveStatistics();
		
		TObjectDoubleHashMap<Person> values = eval.getJointActivityScores();
		
		for(Trajectory t : trajectories) {
			joinScoreStats.addValue(values.get(t.getPerson()));
			totalScoreStats.addValue(t.getPerson().getSelectedPlan().getScore());
		}
		
		results.put("score", totalScoreStats);
		results.put("score_join", joinScoreStats);

	}

}
