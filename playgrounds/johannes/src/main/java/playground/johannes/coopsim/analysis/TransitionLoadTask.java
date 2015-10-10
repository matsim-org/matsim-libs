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

package playground.johannes.coopsim.analysis;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleDoubleHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.FixedSampleSizeDiscretizer;
import org.matsim.contrib.common.stats.Histogram;
import org.matsim.contrib.common.stats.StatsWriter;
import playground.johannes.coopsim.pysical.Trajectory;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 * 
 */
public abstract class TransitionLoadTask extends TrajectoryAnalyzerTask {

	private PlanElementCondition<Leg> condition;

	private final String key;

	protected TransitionLoadTask(String key) {
		this.key = key;
	}

	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		Set<String> purposes = TrajectoryUtils.getTypes(trajectories);
		purposes.add(null);
		
		for (String purpose : purposes) {
			if (purpose == null) {
				condition = DefaultCondition.getInstance();
			} else {
				condition = new LegPurposeCondition(purpose);
			}

			TDoubleArrayList samples = analyze(trajectories);

			if (purpose == null)
				purpose = "all";
		
			write(samples, purpose);
		}
		
		Set<String> modes = TrajectoryUtils.getModes(trajectories);
		modes.add(null);
		
		for(String mode : modes) {
			if(mode == null) {
				condition = DefaultCondition.getInstance();
			} else {
				condition = new LegModeCondition(mode);
			}
			
			TDoubleArrayList samples = analyze(trajectories);

			if (mode == null)
				mode = "all";
		
			write(samples, mode);
			
		}
	}

	private TDoubleArrayList analyze(Collection<Trajectory> trajectories) {
		TDoubleArrayList samples = new TDoubleArrayList(trajectories.size());

		for (Trajectory t : trajectories) {
			for (int i = 1; i < t.getTransitions().size() - 1; i += 2) {
				Leg leg = (Leg) t.getElements().get(i);
				if (condition.test(t, leg, i)) {
					samples.add(getTime(t, i));
				}
			}
		}

		return samples;
	}

	private void write(TDoubleArrayList samples, String filter) {
		try {
			if (!samples.isEmpty()) {
				Discretizer disc = FixedSampleSizeDiscretizer.create(samples.toNativeArray(), 50, 100);
				TDoubleDoubleHashMap load = Histogram.createHistogram(samples.toNativeArray(), disc, true);
				StatsWriter.writeHistogram(load, "time", "n", String.format("%s/%s.%s.txt", getOutputDirectory(), key, filter));
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected abstract double getTime(Trajectory t, int idx);

}
