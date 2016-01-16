/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.pseudosimulation.replanning.factories;

import org.matsim.contrib.pseudosimulation.replanning.DistributedPlanMutatorStrategy;
import org.matsim.contrib.pseudosimulation.replanning.PlanCatcher;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.replanning.PlanStrategy;

import javax.inject.Provider;

/**
 * @author fouriep Creates plan selector of type T for distributed Simulation. Limits the expected value of being selected for PSim execution
 *         in a cycle to the value specified in the config for the selector, thus updating plan scores to the latest travel time information
 *         but preventing excessive repeated execution of plans during the cycle.
 *         .
 */
public class DistributedPlanMutatorStrategyFactory implements
        Provider<PlanStrategy> {
    private final String strategyName;
    private final PlanCatcher slave;
    private final char gene;
    private final boolean trackGenome;
    private final MatsimServices controler;

    public DistributedPlanMutatorStrategy getTarget() {
        return target;
    }

    private DistributedPlanMutatorStrategy target;

    public DistributedPlanMutatorStrategyFactory(PlanCatcher slave, char gene, boolean trackGenome, MatsimServices controler, String strategyName) {
        this.slave = slave;
        this.gene = gene;
        this.trackGenome = trackGenome;
        this.controler = controler;
        this.strategyName = strategyName;

    }

    @Override
    public PlanStrategy get() {
        DistributedPlanMutatorStrategy strategy = new DistributedPlanMutatorStrategy(strategyName,slave,controler,gene);
        return strategy;
    }


}
