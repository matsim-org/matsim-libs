/* *********************************************************************** *
 * project: org.matsim.*
 * LegLoadTask.java
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

import gnu.trove.TDoubleDoubleHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.common.stats.TXTWriter;
import playground.johannes.coopsim.pysical.Trajectory;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public class LegLoadTask extends TrajectoryAnalyzerTask {
	
	private static final Logger logger = Logger.getLogger(LegLoadTask.class);
	

	private final double resolution = 60;
	
	private boolean ignoreSameFacility = false;
	
	public void setIgnoreSameFacilite(boolean flag) {
		this.ignoreSameFacility = flag;
	}
	
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		Map<String, ? extends PlanElementCondition<Leg>> conditions = Conditions.getLegConditions(trajectories);
		
		for(Entry<String, ? extends PlanElementCondition<Leg>> entry : conditions.entrySet()) {
			TDoubleDoubleHashMap load = legLoad(trajectories, entry.getValue());
			try {
				TXTWriter.writeMap(load, "t", "freq", String.format("%1$s/legload.%2$s.txt", getOutputDirectory(), entry.getKey()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}

	private TDoubleDoubleHashMap legLoad(Set<Trajectory> trajectories, PlanElementCondition<Leg> condition) {
		TDoubleDoubleHashMap loadMap = new TDoubleDoubleHashMap();
		int cnt = 0;
		for (Trajectory trajectory : trajectories) {
			for (int i = 1; i < trajectory.getElements().size() - 1; i += 2) {
				Activity prev = (Activity) trajectory.getElements().get(i - 1);
				Activity next = (Activity) trajectory.getElements().get(i + 1);
//				if (type == null || next.getType().equals(type)) {
				if(condition.test(trajectory, (Leg) trajectory.getElements().get(i), i)) {
					boolean ignore = false;

					if (ignoreSameFacility) {
						if (prev.getFacilityId().equals(next.getFacilityId())) {
							ignore = true;
						}
					}
					if (!ignore) {
						int start = (int) (trajectory.getTransitions().get(i) / resolution);
						int end = (int) (trajectory.getTransitions().get(i + 1) / resolution);
						for (int time = start; time < end; time++) {
							loadMap.adjustOrPutValue(time, 1, 1);
						}
					} else {
						cnt++;
					}

				}
			}
		}
		
		if(cnt > 0) {
			logger.warn(String.format("%s trips between same facilities.", cnt));
		}
		
		return loadMap;
	}
}
