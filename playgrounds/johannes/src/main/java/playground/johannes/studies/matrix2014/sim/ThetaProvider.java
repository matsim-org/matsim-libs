/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.johannes.studies.matrix2014.sim;

import org.apache.log4j.Logger;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.sim.Hamiltonian;
import playground.johannes.synpop.sim.MarkovEngineListener;
import playground.johannes.synpop.sim.data.CachedPerson;

import java.util.Collection;

/**
 * @author johannes
 */
public class ThetaProvider implements MarkovEngineListener{

    private static final Logger logger = Logger.getLogger(ThetaProvider.class);

    private long iteration;

    private final long interval;

    private double theta;

    private double H_old;

    private final double minTheta;

    private final double maxTheta;

    private final double alpha;

    private final Hamiltonian hamiltonian;

    public ThetaProvider(Hamiltonian hamiltonian, double alpha, long interval) {
        this(hamiltonian, alpha, interval, 0, Double.MAX_VALUE);
    }

    public ThetaProvider(Hamiltonian hamiltonian, double alpha, long interval, double min, double max) {
        this.hamiltonian = hamiltonian;
        this.alpha = alpha;
        this.interval = interval;
        this.minTheta = min;
        this.maxTheta = max;

        H_old = Double.MAX_VALUE;
    }

    public double getTheta() {
        return theta;
    }

    @Override
    public void afterStep(Collection<CachedPerson> population, Collection<? extends Attributable> mutations, boolean accepted) {


        if(iteration % interval == 0) {
            double H_new = hamiltonian.evaluate(population);
            double delta = H_old - H_new;
            delta = Math.max(delta, 0);

            double theta_new = minTheta + 1/(alpha * delta);
            theta_new = Math.min(theta_new, maxTheta);
            theta = Math.max(theta_new, theta);
//            theta = Math.max(theta, minTheta);

            H_old = H_new;

            logger.debug(String.format("New theta (%s): %s", hamiltonian.getClass().getSimpleName(), theta));
        }

        iteration++;
    }
}
