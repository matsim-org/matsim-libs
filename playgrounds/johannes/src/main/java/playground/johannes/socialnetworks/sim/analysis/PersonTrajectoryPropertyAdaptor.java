/* *********************************************************************** *
 * project: org.matsim.*
 * PersonTrajectoryPropertyAdaptor.java
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

import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections.BidiMap;
import org.matsim.api.core.v01.population.Person;

import playground.johannes.coopsim.analysis.TrajectoryProperty;

/**
 * @author illenberger
 *
 */
public class PersonTrajectoryPropertyAdaptor extends AbstractPersonProperty {

	private final BidiMap trajectories;
	
	private final TrajectoryProperty delegate;
	
	public PersonTrajectoryPropertyAdaptor(BidiMap trajectories, TrajectoryProperty delegate) {
		this.trajectories = trajectories;
		this.delegate = delegate;
	}
	
	@Override
	public TObjectDoubleHashMap<Person> values(Set<? extends Person> persons) {
		Set<Trajectory> traj = new HashSet<Trajectory>(persons.size());
		for(Person person : persons) {
			traj.add((Trajectory) trajectories.get(person));
		}
	
		TObjectDoubleHashMap<Trajectory> tValues = delegate.values(traj);
		TObjectDoubleHashMap<Person> pValues = new TObjectDoubleHashMap<Person>(tValues.size());
		
		TObjectDoubleIterator<Trajectory> it = tValues.iterator();
		for(int i = 0; i < tValues.size(); i++) {
			it.advance();
			pValues.put((Person) trajectories.getKey(it.key()), it.value());
		}
		
		return pValues;
	}
}
