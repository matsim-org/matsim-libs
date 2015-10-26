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

import gnu.trove.TObjectDoubleHashMap;
import org.matsim.api.core.v01.population.Leg;
import playground.johannes.coopsim.pysical.Trajectory;

import java.util.Set;

/**
 * @author johannes
 *
 */
public class TripCount extends AbstractTrajectoryProperty {

	private PlanElementCondition<Leg> condition;
	
	public TripCount() {
		condition = DefaultCondition.getInstance();
	}
	
	public TripCount(PlanElementCondition<Leg> condition) {
		this.condition = condition;
	}
	
	public void setCondition(PlanElementCondition<Leg> condition) {
		this.condition = condition;
	}
	
	@Override
	public TObjectDoubleHashMap<Trajectory> values(Set<? extends Trajectory> trajectories) {
		TObjectDoubleHashMap<Trajectory> values = new TObjectDoubleHashMap<>(trajectories.size());
		
		for(Trajectory t : trajectories) {
			for(int i = 1; i < t.getElements().size(); i+=2) {
				Leg leg = (Leg) t.getElements().get(i);
				if(condition.test(t, leg, i)) {
					values.adjustOrPutValue(t, 1, 1);
				}
			}
		}
		
		return values;
	}

}
