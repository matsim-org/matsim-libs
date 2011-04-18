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

package herbie.running.controler.listeners;

import herbie.running.config.KtiConfigGroup;
import herbie.running.population.algorithms.PersonDeleteNonKtiCompatibleRoutes;
import herbie.running.population.algorithms.PersonInvalidateScores;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;

public class KtiPopulationPreparation implements StartupListener {

	private final KtiConfigGroup ktiConfigGroup;
	
	public KtiPopulationPreparation(KtiConfigGroup ktiConfigGroup) {
		super();
		this.ktiConfigGroup = ktiConfigGroup;
	}

	public void notifyStartup(StartupEvent event) {

		Population pop = event.getControler().getPopulation();
		Config config = event.getControler().getConfig();
		
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
