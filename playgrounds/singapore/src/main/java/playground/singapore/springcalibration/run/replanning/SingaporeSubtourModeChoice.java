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

package playground.singapore.springcalibration.run.replanning;


import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.population.algorithms.ChooseRandomLegModeForSubtour;
import org.matsim.population.algorithms.PermissibleModesCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.PtConstants;

import playground.singapore.springcalibration.run.TaxiUtils;

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
public class SingaporeSubtourModeChoice extends AbstractMultithreadedModule {

	// TODO: extend SubtourModeCoice instead of cloning it
	
	private final Provider<TripRouter> tripRouterProvider;

	private PermissibleModesCalculator permissibleModesCalculator;
	
	private final String[] chainBasedModes;
	private final String[] modes;
	
	public SingaporeSubtourModeChoice(Provider<TripRouter> tripRouterProvider, GlobalConfigGroup globalConfigGroup, SubtourModeChoiceConfigGroup subtourModeChoiceConfigGroup, Population population) {
		this(globalConfigGroup.getNumberOfThreads(),
				subtourModeChoiceConfigGroup.getModes(),
				subtourModeChoiceConfigGroup.getChainBasedModes(),
				subtourModeChoiceConfigGroup.considerCarAvailability(), 
				tripRouterProvider, 
				population);
	}

	public SingaporeSubtourModeChoice(
			final int numberOfThreads,
			final String[] modes,
			final String[] chainBasedModes,
			final boolean considerCarAvailability, 
			Provider<TripRouter> tripRouterProvider,
			Population population) {
		super(numberOfThreads);
		this.tripRouterProvider = tripRouterProvider;
		this.modes = modes.clone();
		this.chainBasedModes = chainBasedModes.clone();
		this.permissibleModesCalculator =
			new SingaporePermissibleModesCalculator(
					population,
					this.modes.clone());
	}
	
	protected String[] getModes() {
		return modes.clone();
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		final TripRouter tripRouter = tripRouterProvider.get();
		final ChooseRandomLegModeForSubtour chooseRandomLegMode =
				new ChooseRandomLegModeForSubtour(
						new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE, TaxiUtils.wait4Taxi),
						tripRouter.getMainModeIdentifier(),
						this.permissibleModesCalculator,
						this.modes,
						this.chainBasedModes,
						MatsimRandom.getLocalInstance());
		chooseRandomLegMode.setAnchorSubtoursAtFacilitiesInsteadOfLinks( false );
		return chooseRandomLegMode;
	}

}
