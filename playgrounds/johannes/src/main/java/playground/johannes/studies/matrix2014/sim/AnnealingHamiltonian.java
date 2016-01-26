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
public class AnnealingHamiltonian implements Hamiltonian, MarkovEngineListener {

    private final static Logger logger = Logger.getLogger(AnnealingHamiltonian.class);

    private final Hamiltonian delegate;

    private final double theta_min;

    private final double theta_max;

    private double theta_factor = 10;

    private double delta_interval = 1e7;

    private double delta_threshold = 0.005;

    private long startIteration = 0;

    private double theta = 0.0;

    private double h_old = Double.MAX_VALUE;

    private long iteration;

    public AnnealingHamiltonian(Hamiltonian delegate, double theta_min, double theta_max) {
        this.delegate = delegate;
        this.theta_min = theta_min;
        this.theta_max = theta_max;

        theta = theta_min;
    }

    public void setThetaFactor(double factor) {
        this.theta_factor = factor;
    }

    public void setDeltaInterval(long interval) {
        this.delta_interval = interval;
    }

    public void setDeltaThreshold(double threshold) {
        this.delta_threshold = threshold;
    }

    public void setStartIteration(long iteration) {
        this.startIteration = iteration;
    }

    @Override
    public double evaluate(Collection<CachedPerson> population) {
        if(iteration >= startIteration) return theta * delegate.evaluate(population);
        else return 0.0;
    }

    private void update(Collection<CachedPerson> population) {
        double h_new = delegate.evaluate(population);
        double delta = h_old - h_new;

        if (delta < delta_threshold) {
            theta = theta * theta_factor;
            theta = Math.max(theta, theta_min);
            theta = Math.min(theta, theta_max);

            logger.trace(String.format("Theta update triggered: %s", theta));
        }

        h_old = h_new;
    }

    @Override
    public void afterStep(Collection<CachedPerson> population, Collection<? extends Attributable> mutations, boolean accepted) {
        if (iteration >= startIteration && iteration % delta_interval == 0) {
            update(population);
        }

        iteration++;
    }
}
