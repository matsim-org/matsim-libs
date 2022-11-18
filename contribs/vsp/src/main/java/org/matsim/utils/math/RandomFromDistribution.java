/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

package org.matsim.utils.math;

import org.apache.commons.math3.random.BitsStreamGenerator;
import org.apache.commons.math3.util.FastMath;

import java.util.SplittableRandom;

/**
 * Copied from <a href="https://github.com/matsim-org/matsim-episim-libs/blob/master/src/main/java/org/matsim/episim/EpisimUtils.java">matsim-episim</a>
 */
public final class RandomFromDistribution {

    /**
     * Draw a gaussian distributed random number (mean=0, var=1).
     *
     * @param rnd splittable random instance
     * @see BitsStreamGenerator#nextGaussian()
     */
    public static double nextGaussian(SplittableRandom rnd) {
        // Normally this allows to generate two numbers, but one is thrown away because this function is stateless
        // generate a new pair of gaussian numbers
        final double x = rnd.nextDouble();
        final double y = rnd.nextDouble();
        final double alpha = 2 * FastMath.PI * x;
        final double r = FastMath.sqrt(-2 * FastMath.log(y));
        return r * FastMath.cos(alpha);
        // nextGaussian = r * FastMath.sin(alpha);
    }

    /**
     * Draws a log normal distributed random number according to X=e^{\mu+\sigma Z}, where Z is a standard normal distribution.
     *
     * @param rnd   splittable random instance
     * @param mu    mu ( median exp mu)
     * @param sigma sigma
     */
    public static double nextLogNormal(SplittableRandom rnd, double mu, double sigma) {
        if (sigma == 0)
            return Math.exp(mu);

        return Math.exp(sigma * nextGaussian(rnd) + mu);
    }

    public static double nextLogNormalFromMeanAndSigma(SplittableRandom rnd, double mean, double sigma) {
        if (mean > 0) {
            double mu = Math.log(mean) - sigma * sigma / 2;
            return nextLogNormal(rnd, mu, sigma);
        } else {
            double mu = Math.log(-mean) - sigma * sigma / 2;
            return -nextLogNormal(rnd, mu, sigma);
        }

    }
}
