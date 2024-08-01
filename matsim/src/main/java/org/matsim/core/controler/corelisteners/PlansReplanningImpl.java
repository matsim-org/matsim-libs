/* *********************************************************************** *
 * project: org.matsim.*
 * PlansReplanner.java.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.controler.corelisteners;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.conflicts.ConflictManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import jakarta.inject.Provider;

/**
 * A {@link org.matsim.core.controler.listener.ControlerListener} that manages the
 * replanning of plans in every iteration. Basically it integrates the
 * {@link org.matsim.core.replanning.StrategyManager} with the
 * {@link org.matsim.core.controler.Controler}.
 *
 * @author mrieser
 */
@Singleton
final class PlansReplanningImpl implements PlansReplanning, ReplanningListener {
	private final Provider<ReplanningContext> replanningContextProvider;
	private final Population population;
	private final StrategyManager strategyManager;
	private final ConflictManager conflictManager;

	@Inject
	PlansReplanningImpl(StrategyManager strategyManager, ConflictManager conflictManager, Population pop,
			Provider<ReplanningContext> replanningContextProvider) {
		this.population = pop;
		this.strategyManager = strategyManager;
		this.conflictManager = conflictManager;
		this.replanningContextProvider = replanningContextProvider;
	}

	@Override
	public void notifyReplanning(final ReplanningEvent event) {
		conflictManager.initializeReplanning(population);
		strategyManager.run(population, event.getIteration(), replanningContextProvider.get());
		conflictManager.run(population, event.getIteration());
	}
}
