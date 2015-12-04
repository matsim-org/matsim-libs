/* *********************************************************************** *
 * project: org.matsim.*
 * DesiredArrivalTimeDiff.java
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

import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.matsim.api.core.v01.population.Person;
import playground.johannes.coopsim.mental.ActivityDesires;
import playground.johannes.coopsim.pysical.Trajectory;

import java.util.Map;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public abstract class DesireDifference extends AbstractTrajectoryProperty {

	private final Map<Person, ActivityDesires> desires;
	
	public DesireDifference(Map<Person, ActivityDesires> desires) {
		this.desires = desires;
	}
	
	@Override
	public TObjectDoubleHashMap<Trajectory> values(Set<? extends Trajectory> trajectories) {
		TObjectDoubleHashMap<Trajectory> values = new TObjectDoubleHashMap<Trajectory>(trajectories.size());
		
		for(Trajectory t : trajectories) {
			ActivityDesires desire = desires.get(t.getPerson());
			double diff = getDifference(t, desire);
			values.put(t, diff);
		}
		
		return values;
	}

	abstract protected double getDifference(Trajectory t, ActivityDesires desire);
	
}
