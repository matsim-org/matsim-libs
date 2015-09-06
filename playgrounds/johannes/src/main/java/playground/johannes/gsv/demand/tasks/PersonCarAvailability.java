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

/**
 * 
 */
package playground.johannes.gsv.demand.tasks;

import gnu.trove.TIntDoubleHashMap;

import java.util.Random;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PersonImpl;

import playground.johannes.gsv.demand.PopulationTask;

/**
 * @author johannes
 *
 */
public class PersonCarAvailability implements PopulationTask {

	private final TIntDoubleHashMap fractions;
	
	private final Random random;
	
	public PersonCarAvailability(TIntDoubleHashMap fractions, Random random) {
		this.fractions = fractions;
		this.random = random;
	}
	
	@Override
	public void apply(Population pop) {
		for(Person person : pop.getPersons().values()) {
			double p = fractions.get(PersonImpl.getAge(person));
			
			if(random.nextDouble() < p)
				PersonImpl.setCarAvail(person, "always");
		}

	}

}
