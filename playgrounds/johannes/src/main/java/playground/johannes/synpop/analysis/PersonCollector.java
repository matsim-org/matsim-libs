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

package playground.johannes.synpop.analysis;

import playground.johannes.synpop.data.Person;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author johannes
 */
public class PersonCollector<T> implements Collector<T> {

    private Predicate<Person> predicate;

    private final ValueProvider<T, Person> provider;

    public PersonCollector(ValueProvider<T, Person> provider) {
        this.provider = provider;
    }

    public void setPredicate(Predicate<Person> predicate) {
        this.predicate = predicate;
    }

    @Override
    public List<T> collect(Collection<? extends Person> persons) {
        List<T> values = new ArrayList<>(persons.size());
        for(Person p : persons) {
            if(predicate == null || predicate.test(p)) {
                values.add(provider.get(p));
            }
        }

        return values;
    }
}
