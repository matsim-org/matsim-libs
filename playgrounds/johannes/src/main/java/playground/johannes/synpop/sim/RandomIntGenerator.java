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

import playground.johannes.synpop.sim.data.CachedElement;

import java.util.Random;

/**
 * @author johannes
 */
public class RandomIntGenerator implements ValueGenerator {

    private final Random random;

    private final int offset;

    private final int factor;

    public RandomIntGenerator(Random random, int min, int max) {
        this.random = random;
        this.offset = min;
        this.factor = (max - min);
    }

    @Override
    public Object newValue(CachedElement element) {
        return new Double(Math.floor(offset + (random.nextDouble() * factor)));
    }
}
