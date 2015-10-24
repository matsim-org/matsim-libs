/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.analysis;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTask;
import playground.johannes.coopsim.pysical.Trajectory;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class ScoreTask extends TrajectoryAnalyzerTask {

	private static final Logger logger = Logger.getLogger(ScoreTask.class);
	
	public static final String KEY = "score";
	
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		int nullScores = 0;
		DescriptiveStatistics stats = new DescriptiveStatistics();
		
		for(Trajectory t : trajectories) {
			Double score = t.getPerson().getSelectedPlan().getScore();
			if(score == null) {
				nullScores++;
			} else {
				stats.addValue(score);
			}
		}

		if(nullScores > 0) {
			logger.info(String.format("%s unscored plans.", nullScores));
		}
		
		results.put(KEY, stats);
		
		try {
			writeHistograms(stats, KEY, 200, 50);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
