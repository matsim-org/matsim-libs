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

import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.sim.data.CachedPerson;
import playground.johannes.synpop.sim.data.Converters;
import playground.johannes.synpop.sim.data.DoubleConverter;

import java.util.Random;

/**
 * @author johannes
 */
public class AgeMutatorBuilder implements MutatorBuilder<CachedPerson> {

    private final Object ageDataKey;

    private final Random random;

    private final RandomIntGenerator generator;

    private final AttributeChangeListener listener;

    public AgeMutatorBuilder(AttributeChangeListener listener, Random random) {
        this.listener = listener;
        this.random = random;
        generator = new RandomIntGenerator(random, 0, 100);

        ageDataKey = Converters.register(CommonKeys.PERSON_AGE, DoubleConverter.getInstance());
    }

    @Override
    public Mutator<CachedPerson> build() {
        RandomElementMutator em = new AttributeMutator(ageDataKey, generator, listener);
        Mutator<CachedPerson> m = new RandomPersonMutator(em, random);
        return m;
    }
}
