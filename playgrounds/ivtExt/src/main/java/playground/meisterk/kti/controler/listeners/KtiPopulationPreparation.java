/* *********************************************************************** *
 * project: org.matsim.*
 * InvalidRouteCleaner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.meisterk.kti.controler.listeners;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmRunner;

import playground.meisterk.kti.config.KtiConfigGroup;
import playground.meisterk.kti.population.algorithms.PersonDeleteNonKtiCompatibleRoutes;
import playground.meisterk.kti.population.algorithms.PersonInvalidateScores;

public class KtiPopulationPreparation implements StartupListener {

	private final KtiConfigGroup ktiConfigGroup;
	
	public KtiPopulationPreparation(KtiConfigGroup ktiConfigGroup) {
		super();
		this.ktiConfigGroup = ktiConfigGroup;
	}

	public void notifyStartup(StartupEvent event) {

        Population pop = event.getServices().getScenario().getPopulation();
		Config config = event.getServices().getConfig();
		
		/*
		 * make sure every pt leg has a kti pt route when the kti pt router is used
		 */
		if (this.ktiConfigGroup.isUsePlansCalcRouteKti()) {
			ParallelPersonAlgorithmRunner.run(
					pop, 
					config.global().getNumberOfThreads(),
					new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
						public AbstractPersonAlgorithm getPersonAlgorithm() {
							return new PersonDeleteNonKtiCompatibleRoutes();
						}
					});
		}
		
		/*
		 * for an explanation, see the element comment of the kti config parameter
		 */
		if (this.ktiConfigGroup.isInvalidateScores()) {
			ParallelPersonAlgorithmRunner.run(
					pop, 
					config.global().getNumberOfThreads(),
					new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
						public AbstractPersonAlgorithm getPersonAlgorithm() {
							return new PersonInvalidateScores();
						}
					});
		}
		
	}

}
