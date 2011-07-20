/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityLoadCurve.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.sim.analysis;

import gnu.trove.TDoubleDoubleHashMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.LinearDiscretizer;


/**
 * @author illenberger
 *
 */
public class ActivityLoad {

	private Discretizer discretizer = new LinearDiscretizer(900);
	
	public void setDiscretizer(Discretizer discretizer) {
		this.discretizer = discretizer;
	}
	
	public Map<String, TDoubleDoubleHashMap> loadCurve(Set<Trajectory> trajectories) {
		Map<String, TDoubleDoubleHashMap> map = new HashMap<String, TDoubleDoubleHashMap>();

		for (Trajectory trajectory : trajectories) {
			if(trajectory.getElements().size() > 0) {
				for(int i = 0; i < trajectory.getElements().size(); i+=2) {
					Activity element = (Activity) trajectory.getElements().get(i);
					String type = element.getType();
					
					double start = discretizer.discretize(trajectory.getTransitions().get(i));
//					if(!Double.isNaN(element.getStartTime()) && !Double.isInfinite(element.getStartTime()))
//						start = discretizer.discretize(element.getStartTime());
					
					double end = discretizer.discretize(trajectory.getTransitions().get(i + 1));
//					if(!Double.isNaN(element.getEndTime()) && !Double.isInfinite(element.getEndTime()))
//						end = discretizer.discretize((element).getEndTime());

//					start = Math.min(start, end);
					
					TDoubleDoubleHashMap hist = map.get(type);
					if (hist == null) {
						hist = new TDoubleDoubleHashMap();
						map.put(type, hist);
					}

					for (int t = (int)start; t <= end; t += discretizer.binWidth(t)) {
						hist.adjustOrPutValue(t, 1, 1);
					}

				}
			}
		}

		return map;
	}
}
