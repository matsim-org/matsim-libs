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
import org.matsim.core.replanning.StrategyManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * A {@link org.matsim.core.controler.listener.ControlerListener} that manages the
 * replanning of plans in every iteration. Basically it integrates the
 * {@link org.matsim.core.replanning.StrategyManager} with the
 * {@link org.matsim.core.controler.Controler}.
 *
 * @author mrieser
 */
@Singleton
public final class PlansReplanningImpl implements PlansReplanning, ReplanningListener {
	
	private Population population ;
	private StrategyManager strategyManager ;
	
	@Inject
	public PlansReplanningImpl( StrategyManager strategyManager, Population pop ) {
		this.population = pop ;
		this.strategyManager = strategyManager ;
	}

	@Override
	public void notifyReplanning(final ReplanningEvent event) {
		strategyManager.run(population, event.getIteration(), event.getReplanningContext());
	}

}
