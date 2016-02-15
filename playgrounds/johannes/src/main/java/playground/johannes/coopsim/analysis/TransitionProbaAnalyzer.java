/* *********************************************************************** *
 * project: org.matsim.*
 * TransitionProbaAnalyzer.java
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

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import playground.johannes.coopsim.mental.MentalEngine;
import playground.johannes.coopsim.pysical.Trajectory;

import java.util.Map;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public class TransitionProbaAnalyzer extends TrajectoryAnalyzerTask {

	private final MentalEngine mentalEngine;
	
	private final int dumpInterval;
	
	public TransitionProbaAnalyzer(MentalEngine mentalEngine, int dumpInterval) {
		this.mentalEngine = mentalEngine;
		this.dumpInterval = dumpInterval;
	}
	
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		
		double pi = mentalEngine.getTotalPiSum()/(double)dumpInterval;
		
		stats.addValue(pi);
		
		results.put("pi", stats);

		mentalEngine.cleatTotalPiSum();
	}

}
