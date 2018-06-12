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


import javax.inject.Provider;

import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.ChooseRandomLegModeForSubtour;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;
import org.matsim.core.population.algorithms.PermissibleModesCalculatorImpl;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.TripRouter;

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
	
	private final double probaForChangeSingleTripMode ;
	
	public enum Behavior { fromAllModesToSpecifiedModes, fromSpecifiedModesToSpecifiedModes }
	private Behavior behavior = Behavior.fromSpecifiedModesToSpecifiedModes ;

	private final Provider<TripRouter> tripRouterProvider;

	private PermissibleModesCalculator permissibleModesCalculator;
	
	private final String[] chainBasedModes;
	private final String[] modes;
	
	public SubtourModeChoice(Provider<TripRouter> tripRouterProvider, GlobalConfigGroup globalConfigGroup,
							 SubtourModeChoiceConfigGroup subtourModeChoiceConfigGroup) {
		this(globalConfigGroup.getNumberOfThreads(),
				subtourModeChoiceConfigGroup.getModes(),
				subtourModeChoiceConfigGroup.getChainBasedModes(),
				subtourModeChoiceConfigGroup.considerCarAvailability(),
				subtourModeChoiceConfigGroup.getProbaForRandomSingleTripMode(),
				tripRouterProvider
		);
		this.setBehavior( subtourModeChoiceConfigGroup.getBehavior() );
	}

	public SubtourModeChoice(
			final int numberOfThreads,
			final String[] modes,
			final String[] chainBasedModes,
			final boolean considerCarAvailability,
			double probaForChangeSingleTripMode,
			Provider<TripRouter> tripRouterProvider) {
		super(numberOfThreads);
		this.probaForChangeSingleTripMode = probaForChangeSingleTripMode;
		this.tripRouterProvider = tripRouterProvider;
		this.modes = modes.clone();
		this.chainBasedModes = chainBasedModes.clone();
		this.permissibleModesCalculator =
			new PermissibleModesCalculatorImpl(
					this.modes,
					considerCarAvailability);
	}
	
	@Deprecated // only use when backwards compatibility is needed. kai, may'18
	public final void setBehavior ( Behavior behavior ) {
		this.behavior = behavior ;
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
						MatsimRandom.getLocalInstance(), behavior, probaForChangeSingleTripMode);
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
