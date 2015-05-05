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

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.pseudosimulation.replanning.PlanCatcher;
import org.matsim.contrib.pseudosimulation.replanning.selectors.DistributedPlanSelector;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;

import javax.inject.Provider;

/**
 * @author fouriep Creates plan selector for distributed Simulation. Limits the expected value of being selected for PSim execution
 *         in a cycle to the value specified in the config for the selector, thus updating plan scores to the latest travel time information
 *         but preventing excessive repeated execution of plans during the cycle.
 *         .
 */
public class DistributedPlanSelectorStrategyFactory implements Provider<PlanStrategy> {

    private final String strategyName;
    private final Controler controler;
    boolean quickReplanning;
    int selectionInflationFactor;
    private PlanCatcher slave;
    private Scenario scenario;

    public DistributedPlanSelectorStrategyFactory(PlanCatcher slave, boolean quickReplanning, int selectionInflationFactor, Controler controler, String strategyName) {
        this.slave = slave;
        this.quickReplanning = quickReplanning;
        this.selectionInflationFactor = selectionInflationFactor;
        this.controler = controler;
        this.strategyName = strategyName;
    }

    @Override
    public PlanStrategy get() {
        PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(
                new DistributedPlanSelector( controler, strategyName, slave, quickReplanning,  selectionInflationFactor)
        );
        return builder.build();
    }

}
