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

package playground.johannes.gsv.synPop.sim3;

import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.sim.Mutator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SwitchOperator implements Mutator {

	private final SwitchMutator mutator;
	
	private final Random random;
	
	private final List<Person> mutations;

	public SwitchOperator(SwitchMutator mutator, Random random) {
		this.mutator = mutator;
		this.random = random;
		
		mutations = new ArrayList<>(2);
		mutations.add(null);
		mutations.add(null);
	}
	
	@Override
	public List<Person> select(List<Person> persons) {
		Person person1 = persons.get(random.nextInt(persons.size()));
		Person person2 = persons.get(random.nextInt(persons.size()));
		
		mutations.set(0, person1);
		mutations.set(1, person2);
			
		return mutations;
		
	}
	
	public boolean modify(List<Person> persons) {
		return mutator.mutate(persons.get(0), persons.get(1));
	}

	@Override
	public void revert(List<Person> persons) {
		mutator.revert(persons.get(0), persons.get(1));
	}

}
