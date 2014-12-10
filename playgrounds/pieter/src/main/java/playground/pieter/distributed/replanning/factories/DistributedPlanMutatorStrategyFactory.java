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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
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
    T delegate;
    public DistributedPlanMutatorStrategyFactory(PlanCatcher slave, T delegate) {
        this.delegate = delegate;
        this.slave = slave;
    }

	@Override
	public PlanStrategy createPlanStrategy(Scenario scenario, EventsManager eventsManager) {
        PlanStrategyImpl planStrategy = (PlanStrategyImpl) delegate.createPlanStrategy(scenario, eventsManager);
        planStrategy.addStrategyModule(new RegisterMutatedPlanForPSim(slave));
        return planStrategy;
	}

}
