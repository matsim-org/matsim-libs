/* *********************************************************************** *
 * project: org.matsim.*
 * TripDuration.java
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

import java.util.Set;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;

import playground.johannes.coopsim.pysical.Trajectory;

/**
 * @author illenberger
 *
 */
public class TripDuration extends AbstractTrajectoryProperty {

	private final String whitelist;
	
	private final boolean useMode;
	
	public TripDuration(String purpose) {
		this.whitelist = purpose;
		useMode = false;
	}
	
	public TripDuration(String whitelist, boolean useMode) {
		this.whitelist = whitelist;
		this.useMode = useMode;
	}
	
	@Override
	public TObjectDoubleHashMap<Trajectory> values(Set<? extends Trajectory> trajectories) {
		TObjectDoubleHashMap<Trajectory> values = new TObjectDoubleHashMap<Trajectory>(trajectories.size());
		
		for(Trajectory trajectory : trajectories) {
			double dur_sum = 0;
			double cnt = 0;
			for(int i = 1; i < trajectory.getElements().size() - 1; i += 2) {
				Activity act = (Activity)trajectory.getElements().get(i+1);
				
				boolean match = false;
				if (useMode) {
					Leg leg = (Leg) trajectory.getElements().get(i);
					if (whitelist == null || leg.getMode().equalsIgnoreCase(whitelist)) {
						match = true;
					}
				} else {
					if (whitelist == null || act.getType().equals(whitelist)) {
						match = true;
					}
				}
				
				if(match) {
					double dur = trajectory.getTransitions().get(i + 1) - trajectory.getTransitions().get(i);
					dur_sum += dur;
					cnt++;
				}
			}
			if (cnt > 0) {
				double dur_mean = dur_sum / (double) cnt;
				values.put(trajectory, dur_mean);
			}
		}
		
		return values;
	}

}
