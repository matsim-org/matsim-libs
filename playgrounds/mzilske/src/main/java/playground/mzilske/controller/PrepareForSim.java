/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * PrepareForSim.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.mzilske.controller;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonPrepareForSim;

import javax.inject.Inject;
import javax.inject.Provider;

class PrepareForSim implements Runnable {

    @Inject Scenario scenario;
    @Inject Config config;
    @Inject Provider<TripRouter> tripRouterProvider;

    public void run() {
        ParallelPersonAlgorithmRunner.run(scenario.getPopulation(), config.global().getNumberOfThreads(),
                new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
                    @Override
                    public AbstractPersonAlgorithm getPersonAlgorithm() {
                        return new PersonPrepareForSim(new PlanRouter(
                                tripRouterProvider.get(),
                                scenario.getActivityFacilities()
                        ), scenario);
                    }
                }
        );
    }

}
