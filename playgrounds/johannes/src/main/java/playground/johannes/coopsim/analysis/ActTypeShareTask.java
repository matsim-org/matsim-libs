/* *********************************************************************** *
 * project: org.matsim.*
 * ActTypeShareTask.java
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

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.common.stats.StatsWriter;
import playground.johannes.coopsim.pysical.Trajectory;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public class ActTypeShareTask extends TrajectoryAnalyzerTask {

	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		TObjectIntHashMap<String> hist = new TObjectIntHashMap<String>();
		
		for(Trajectory t : trajectories) {
			for(int i = 0; i < t.getElements().size(); i += 2) {
				Activity act = (Activity) t.getElements().get(i);
				hist.adjustOrPutValue(act.getType(), 1, 1);
			}
		}
		
		TObjectIntIterator<String> it = hist.iterator();
		int sumHome = 0;
		int sum = 0;
		for(int i = 0; i < hist.size(); i++) {
			it.advance();
			sumHome += it.value();
			if(!it.key().equals("home"))
				sum += it.value();
		}
		
		TObjectDoubleHashMap<String> histWHome = new TObjectDoubleHashMap<String>();
		TObjectDoubleHashMap<String> histWOHome = new TObjectDoubleHashMap<String>();
		it = hist.iterator();
		for(int i = 0; i < hist.size(); i++) {
			it.advance();
			histWHome.put(it.key(), it.value()/(double)sumHome);
			if(!it.key().equals("home"))
				histWOHome.put(it.key(), it.value()/(double)sum);
		}
		
		
		try {
			StatsWriter.writeLabeledHistogram(histWOHome, "type", "p", String.format("%1$s/actTypeShare.nohome.txt", getOutputDirectory()));
			StatsWriter.writeLabeledHistogram(histWHome, "type", "p", String.format("%1$s/actTypeShare.txt", getOutputDirectory()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
