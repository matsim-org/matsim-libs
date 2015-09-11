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

import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.sim.data.CachedPerson;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author johannes
 */
public class PersonAttributeMutator implements Mutator {

    private final Object dataKey;

    private final AttributeChangeListener listener;

    private final ValueGenerator generator;

    private Object oldValue;

    private final List<Person> mutations;

    private final Random random;

    public PersonAttributeMutator(Object dataKey, Random random, ValueGenerator generator, AttributeChangeListener
            listener) {
        this.dataKey = dataKey;
        this.random = random;
        this.listener = listener;
        this.generator = generator;

        mutations = new ArrayList<>(1);
        mutations.add(null);
    }

    @Override
    public List<Person> select(List<Person> persons) {
        mutations.set(0, persons.get(random.nextInt(persons.size())));
        return mutations;
    }

    @Override
    public boolean modify(List<Person> persons) {
        CachedPerson person = (CachedPerson)persons.get(0);
        oldValue = person.getData(dataKey);
        Object newValue = generator.newValue(person);
        person.setData(dataKey, newValue);

        if(listener != null) listener.onChange(dataKey, oldValue, newValue, person);

        return true;
    }

    @Override
    public void revert(List<Person> persons) {
        CachedPerson person = (CachedPerson)persons.get(0);
        Object newValue = person.getData(dataKey);
        person.setData(dataKey, oldValue);

        if(listener != null) listener.onChange(dataKey, newValue, oldValue, person);

    }
}
