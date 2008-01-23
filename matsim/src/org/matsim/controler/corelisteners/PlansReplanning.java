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

package org.matsim.controler.corelisteners;

import org.matsim.controler.Controler;
import org.matsim.controler.events.ReplanningEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.ReplanningListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.plans.Plans;
import org.matsim.replanning.StrategyManager;
import org.matsim.replanning.StrategyManagerConfigLoader;
import org.matsim.router.util.TravelCostI;
import org.matsim.router.util.TravelTimeI;

/**
 * A {@link org.matsim.controler.listener.ControlerListener} that manages the
 * replanning of plans in every iteration. Basically it integrates the
 * {@link org.matsim.replanning.StrategyManager} with the
 * {@link org.matsim.controler.Controler}.
 *
 * @author mrieser
 */
public class PlansReplanning implements StartupListener, ReplanningListener {

	private final Plans population;
	private StrategyManager strategyManager;
	private final TravelCostI costFunction;
	private final TravelTimeI timeFunction;
	private final LegTravelTimeEstimator legEstimator;

	public PlansReplanning(final Plans population, final TravelCostI costFunction,
			final TravelTimeI timeFunction, final LegTravelTimeEstimator legEstimator) {
		this.population = population;
		this.costFunction = costFunction;
		this.timeFunction = timeFunction;
		this.legEstimator = legEstimator;
	}

	public void notifyStartup(final StartupEvent event) {
		this.strategyManager = new StrategyManager();
		Controler controler = event.getControler();
		StrategyManagerConfigLoader.load(controler.getConfig(), this.strategyManager, controler.getNetwork(),
				this.costFunction, this.timeFunction, this.legEstimator);
	}

	public void notifyReplanning(final ReplanningEvent event) {
		this.strategyManager.run(this.population, event.getIteration());
	}

}
