/* *********************************************************************** *
 * project: org.matsim.*
 * SubtourModeChoice.java
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

package org.matsim.core.replanning.modules;


import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.TripRouter;
import org.matsim.population.algorithms.ChooseRandomLegModeForSubtour;
import org.matsim.population.algorithms.PermissibleModesCalculator;
import org.matsim.population.algorithms.PermissibleModesCalculatorImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

import javax.inject.Provider;

/**
 * Changes the transportation mode of all legs of one randomly chosen subtour in a plan to a randomly chosen
 * different mode given a list of possible modes.
 *
 * A subtour is a consecutive subset of a plan which starts and ends at the same link.
 * 
 * Certain modes are considered only if the choice would not require some resource to appear
 * out of thin air. For example, you can only drive your car back from work if you have previously parked it
 * there. These are called chain-based modes.
 * 
 * The assumption is that each chain-based mode requires one resource (car, bike, ...) and that this
 * resource is initially positioned at home. Home is the location of the first activity in the plan.
 * 
 * If the plan initially violates this constraint, this module may (!) repair it. 
 * 
 * @author michaz
 * 
 */
public class SubtourModeChoice extends AbstractMultithreadedModule {

	private final Provider<TripRouter> tripRouterProvider;

	private PermissibleModesCalculator permissibleModesCalculator;
	
	private final String[] chainBasedModes;
	private final String[] modes;
	
	public SubtourModeChoice(final Config config, Provider<TripRouter> tripRouterProvider) {
		this( config.global().getNumberOfThreads(),
				config.subtourModeChoice().getModes(),
				config.subtourModeChoice().getChainBasedModes(),
				config.subtourModeChoice().considerCarAvailability(), tripRouterProvider);
	}

	public SubtourModeChoice(
			final int numberOfThreads,
			final String[] modes,
			final String[] chainBasedModes,
			final boolean considerCarAvailability, Provider<TripRouter> tripRouterProvider) {
		super(numberOfThreads);
		this.tripRouterProvider = tripRouterProvider;
		this.modes = modes.clone();
		this.chainBasedModes = chainBasedModes.clone();
		this.permissibleModesCalculator =
			new PermissibleModesCalculatorImpl(
					this.modes,
					considerCarAvailability);
	}
	
	protected String[] getModes() {
		return modes.clone();
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		final TripRouter tripRouter = tripRouterProvider.get();
		final ChooseRandomLegModeForSubtour chooseRandomLegMode =
				new ChooseRandomLegModeForSubtour(
						tripRouter.getStageActivityTypes(),
						tripRouter.getMainModeIdentifier(),
						this.permissibleModesCalculator,
						this.modes,
						this.chainBasedModes,
						MatsimRandom.getLocalInstance());
		chooseRandomLegMode.setAnchorSubtoursAtFacilitiesInsteadOfLinks( false );
		return chooseRandomLegMode;
	}

	/**
	 * Decides if a person may use a certain mode of transport. Can be used for car ownership.
	 * 
	 */
	public void setPermissibleModesCalculator(PermissibleModesCalculator permissibleModesCalculator) {
		this.permissibleModesCalculator = permissibleModesCalculator;
	}

}
