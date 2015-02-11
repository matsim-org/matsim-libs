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

package org.matsim.contrib.util.random;

import org.apache.commons.math3.random.*;


/**
 * Based on org.matsim.core.gbl.MatsimRandom
 */
public class RandomUtils
{
    public static final int DEFAULT_SEED = 4357;

    private static final RandomGenerator rg = new MersenneTwister(DEFAULT_SEED);

    private static final UniformRandom uniform = new UniformRandom(rg);


    public static void reset()
    {
        reset(DEFAULT_SEED);
    }


    public static void reset(final int seed)
    {
        rg.setSeed(seed);
    }


    public static RandomGenerator getGlobalGenerator()
    {
        return rg;
    }


    public static UniformRandom getGlobalUniform()
    {
        return uniform;
    }


    /**
     * Returns an instance of a random number generator, which can be used locally, e.g. in threads.
     */
    public static RandomGenerator getLocalGenerator()
    {
        return new MersenneTwister(rg.nextInt());
    }
}
