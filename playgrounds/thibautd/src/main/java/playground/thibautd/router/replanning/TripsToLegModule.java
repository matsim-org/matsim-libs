/* *********************************************************************** *
 * project: org.matsim.*
 * TripsToLegModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.router.replanning;

import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.router.CompositeStageActivityTypes;
import playground.thibautd.router.controler.MultiLegRoutingControler;
import playground.thibautd.router.StageActivityTypes;
import playground.thibautd.router.TripRouter;

/**
 * Removes trips and replaces them by legs.
 * The aim is to simplify the plan before passing it to plan algorithms
 * unable to handle multi-planElement trips.
 * The plan must be re-routed before execution!
 * @author thibautd
 */
public class TripsToLegModule extends AbstractMultithreadedModule {
	private final MultiLegRoutingControler controler;
	private final StageActivityTypes additionalBlackList;

	/**
	 * Initializes an instance using the stage activity types from the controler
	 * @param controler
	 */
	public TripsToLegModule(final Controler controler) {
		this( controler , null );
	}

	/**
	 * Initializes an instance, allowing to specify additional activity types to
	 * consider as stage activities.
	 * @param controler
	 * @param additionalBlackList a {@link StageActivityTypes} instance identifying
	 * the additionnal types
	 */
	public TripsToLegModule(final Controler controler, final StageActivityTypes additionalBlackList) {
		super( controler.getConfig().global() );
		this.controler = (MultiLegRoutingControler) controler;
		this.additionalBlackList = additionalBlackList;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		TripRouter router = controler.getTripRouterFactory().createTripRouter();
		StageActivityTypes blackListToUse = router.getStageActivityTypes();

		if (additionalBlackList != null) {
			CompositeStageActivityTypes composite = new CompositeStageActivityTypes();
			composite.addActivityTypes( blackListToUse );
			composite.addActivityTypes( additionalBlackList );
			blackListToUse = composite;
		}

		return new TripsToLegsAlgorithm( router , blackListToUse );
	}
}

