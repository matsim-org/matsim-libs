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

package org.matsim.contrib.pseudosimulation.distributed.replanning.factories;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.pseudosimulation.distributed.replanning.PlanCatcher;
import org.matsim.contrib.pseudosimulation.distributed.replanning.selectors.DistributedPlanSelector;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;

/**
 * @author fouriep Creates plan selector of type T for distributed Simulation. Limits the expected value of being selected for PSim execution
 * in a cycle to the value specified in the config for the selector, thus updating plan scores to the latest travel time information
 * but preventing excessive repeated execution of plans during the cycle.
 *         .
 */
public class DistributedPlanSelectorStrategyFactory<T extends PlanStrategyFactory> implements
		PlanStrategyFactory {

    T delegate;
    PlanCatcher slave;
    boolean quickReplanning; int selectionInflationFactor;
    private Scenario scenario;
    private EventsManager eventsManager;

    public DistributedPlanSelectorStrategyFactory(PlanCatcher slave, T planStrategyFactory, boolean quickReplanning, int selectionInflationFactor, Scenario scenario, EventsManager eventsManager) {
        this.delegate = planStrategyFactory;
        this.slave = slave;
        this.quickReplanning = quickReplanning;
        this.selectionInflationFactor = selectionInflationFactor;
        this.scenario = scenario;
        this.eventsManager = eventsManager;
    }

    @Override
	public PlanStrategy get() {
		return new PlanStrategyImpl(new DistributedPlanSelector(scenario, eventsManager,delegate ,slave, quickReplanning, selectionInflationFactor));
	}

}
