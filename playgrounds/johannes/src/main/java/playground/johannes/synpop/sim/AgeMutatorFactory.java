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

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.sim3.Mutator;
import playground.johannes.gsv.synPop.sim3.MutatorFactory;
import playground.johannes.synpop.sim.data.Converters;
import playground.johannes.synpop.sim.data.DoubleConverter;

import java.util.Random;

/**
 * @author johannes
 */
public class AgeMutatorFactory implements MutatorFactory {

    public static final Object AGE_DATA_KEY = new Object();

    private final Random random;

    private final RandomIntGenerator generator;

    private final AttributeChangeListener listener;

    public AgeMutatorFactory(AttributeChangeListener listener, Random random) {
        this.listener = listener;
        this.random = random;
        generator = new RandomIntGenerator(random, 0, 100);

        Converters.register(CommonKeys.PERSON_AGE, AGE_DATA_KEY, DoubleConverter.getInstance());
    }

    @Override
    public Mutator newInstance() {
        return new PersonAttributeMutator(AGE_DATA_KEY, random, generator, listener);
    }
}
