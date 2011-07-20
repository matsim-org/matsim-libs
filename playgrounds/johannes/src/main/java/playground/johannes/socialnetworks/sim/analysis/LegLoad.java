/* *********************************************************************** *
 * project: org.matsim.*
 * LegLoad.java
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
public class LegLoad {

	private Discretizer discretizer = new LinearDiscretizer(900);
	
	public Map<String, TDoubleDoubleHashMap> statistics(Set<Trajectory> trajectories) {
		Map<String, TDoubleDoubleHashMap> map = new HashMap<String, TDoubleDoubleHashMap>();
		
		for(Trajectory t : trajectories) {
			if(t.getElements().size() > 0) {
				for(int i = 1; i < t.getElements().size(); i+=2) {
//					Leg leg = (Leg) t.getElements().get(i);
					Activity act = (Activity) t.getElements().get(i + 1);
					
					String type = act.getType();
					TDoubleDoubleHashMap hist = map.get(type);
					if(hist == null) {
						hist = new TDoubleDoubleHashMap();
						map.put(type, hist);
					}
					
					double start = discretizer.discretize(t.getTransitions().get(i));
					double end = discretizer.discretize(t.getTransitions().get(i + 1));
					
					for(int k = (int) start; k <= end; k += discretizer.binWidth(k)) {
						hist.adjustOrPutValue(k, 1, 1);
					}
				}
			}
			
		}
		
		return map;
	}
}
