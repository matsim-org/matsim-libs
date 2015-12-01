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
public class IncomeMutatorBuilder implements MutatorBuilder<CachedPerson> {

    public  final Object incomeDataKey;

    private final Random random;

    private final RandomIntGenerator generator;

    private final AttributeChangeListener listener;

    public IncomeMutatorBuilder(AttributeChangeListener listener, Random random) {
        this.random = random;
        this.listener = listener;
        generator = new RandomIntGenerator(random, 500, 8000);

        incomeDataKey = Converters.register(CommonKeys.HH_INCOME, DoubleConverter.getInstance());

    }

    @Override
    public Mutator<CachedPerson> build() {
        RandomElementMutator em = new AttributeMutator(incomeDataKey, generator, listener);
        Mutator<CachedPerson> m = new RandomPersonMutator(em, random);
        return m;

    }
}
