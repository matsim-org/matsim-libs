/* *********************************************************************** *
 * project: org.matsim.*
 * DurationChoiceLogger.java
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
package playground.johannes.studies.coopsim;

import gnu.trove.TDoubleDoubleHashMap;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.math.FixedSampleSizeDiscretizer;
import org.matsim.contrib.sna.math.Histogram;
import org.matsim.contrib.sna.util.TXTWriter;

import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTask;
import playground.johannes.coopsim.mental.choice.ChoiceSelector;
import playground.johannes.coopsim.mental.choice.DurationSelector;
import playground.johannes.coopsim.pysical.Trajectory;

/**
 * @author illenberger
 *
 */
public class DurationChoiceLogger extends TrajectoryAnalyzerTask implements ChoiceSelector {

	private DescriptiveStatistics stats = new DescriptiveStatistics();
	
	@Override
	public Map<String, Object> select(Map<String, Object> choices) {
		Double t = (Double) choices.get(DurationSelector.KEY);
		stats.addValue(t);
		return choices;
	}

	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		try {
			if(stats.getN() > 0) {
			TDoubleDoubleHashMap hist = Histogram.createHistogram(stats, FixedSampleSizeDiscretizer.create(stats.getValues(), 1, 30), true);
			TXTWriter.writeMap(hist, "t", "n", getOutputDirectory() + "dur.choices.txt");
			stats.clear();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	

}
