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

package playground.pieter.distributed.replanning.factories;

import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import playground.pieter.distributed.replanning.PlanCatcher;
import playground.pieter.distributed.replanning.modules.RegisterMutatedPlanForPSim;

/**
 * @author fouriep Creates plan selector of type T for distributed Simulation. Limits the expected value of being selected for PSim execution
 * in a cycle to the value specified in the config for the selector, thus updating plan scores to the latest travel time information
 * but preventing excessive repeated execution of plans during the cycle.
 *         .
 */
public class DistributedPlanMutatorStrategyFactory<T extends PlanStrategyFactory> implements
		PlanStrategyFactory {
    private final PlanCatcher slave;
    private final char gene;
    private final boolean trackGenome;
    private final Controler controler;
    T delegate;
    public DistributedPlanMutatorStrategyFactory(PlanCatcher slave, T delegate, char gene, boolean trackGenome, Controler controler) {
        this.delegate = delegate;
        this.slave = slave;
        this.gene=gene;
        this.trackGenome=trackGenome;
        this.controler=controler;
    }

	@Override
	public PlanStrategy get() {
        PlanStrategyImpl planStrategy = (PlanStrategyImpl) delegate.get();
        planStrategy.addStrategyModule(new RegisterMutatedPlanForPSim(slave,gene,trackGenome,controler));
        return planStrategy;
	}

}
