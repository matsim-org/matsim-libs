/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripsToLegsModule.java
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
package playground.thibautd.socnetsim.router;

import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.TripRouterFactoryInternal;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * @author thibautd
 */
public class JointTripsToLegsModule extends AbstractMultithreadedModule {
	private final TripRouterFactoryInternal tripRouterFactory;

	public JointTripsToLegsModule(final Controler controler) {
		super( controler.getConfig().global() );
		this.tripRouterFactory = controler.getTripRouterFactory();
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new JointTripsToLegsAlgorithm( tripRouterFactory.instantiateAndConfigureTripRouter() );
	}
}

