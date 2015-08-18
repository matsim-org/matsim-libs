/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.synpop.sim;

import playground.johannes.gsv.synPop.sim3.Mutator;
import playground.johannes.synpop.data.Person;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author johannes
 */
public abstract class PersonAttributeMutator implements Mutator {

    private final Object dataKey;

    private final AttributeChangeListener listener;

    private Object oldValue;

    private final List<Person> mutations;

    private final Random random;

    public PersonAttributeMutator(Object dataKey, Random random, AttributeChangeListener listener) {
        this.dataKey = dataKey;
        this.random = random;
        this.listener = listener;

        mutations = new ArrayList<>(1);
        mutations.add(null);
    }

    @Override
    public List<Person> select(List<Person> persons) {
        mutations.set(0, persons.get(random.nextInt(persons.size())));
        return null;
    }

    @Override
    public boolean modify(List<Person> persons) {
        return false;
    }

    @Override
    public void revert(List<Person> persons) {

    }
}
