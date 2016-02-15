/* *********************************************************************** *
 * project: org.matsim.*
 * TripPurposeShare.java
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

import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.common.stats.StatsWriter;
import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.studies.mz2005.io.ActivityType;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public class TripPurposeShareTask extends TrajectoryAnalyzerTask {

	private boolean ignoreSameFacility = false;
	
	public void setIgnoreSameFacility(boolean ignore) {
		this.ignoreSameFacility = ignore;
	}
	/* (non-Javadoc)
	 * @see playground.johannes.coopsim.analysis.TrajectoryAnalyzerTask#analyze(java.util.Set, java.util.Map)
	 */
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		TObjectDoubleHashMap<String> hist = new TObjectDoubleHashMap<String>();
		
		for(Trajectory t : trajectories) {
			for(int i = 1; i < t.getElements().size(); i += 2) {
				Activity prev = (Activity) t.getElements().get(i - 1);
				Activity next = (Activity) t.getElements().get(i + 1);
	
				boolean ignore = false;
				if(ignoreSameFacility) {
					if(prev.getFacilityId().equals(next.getFacilityId())) {
						ignore = true;
					}
				}
				if(!ignore) {
					hist.adjustOrPutValue(next.getType(), 1, 1);
				}
			}
		}

		TObjectDoubleIterator<String> it = hist.iterator();
		int sumHome = 0;
		int sum = 0;
		for(int i = 0; i < hist.size(); i++) {
			it.advance();
			sumHome += it.value();
			if(!it.key().equals(ActivityType.home.name()))
				sum += it.value();
		}
		
		TObjectDoubleHashMap<String> histWHome = new TObjectDoubleHashMap<String>();
		TObjectDoubleHashMap<String> histWOHome = new TObjectDoubleHashMap<String>();
		it = hist.iterator();
		for(int i = 0; i < hist.size(); i++) {
			it.advance();
			histWHome.put(it.key(), it.value()/(double)sumHome);
			if(!it.key().equals(ActivityType.home.name()))
				histWOHome.put(it.key(), it.value()/(double)sum);
		}
		
		
		try {
			StatsWriter.writeLabeledHistogram(hist, "type", "n", String.format("%1$s/tripPurpose.txt", getOutputDirectory()));
			StatsWriter.writeLabeledHistogram(histWOHome, "type", "p", String.format("%1$s/tripPurposeShare.nohome.txt", getOutputDirectory()));
			StatsWriter.writeLabeledHistogram(histWHome, "type", "p", String.format("%1$s/tripPurposeShare.txt", getOutputDirectory()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
