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

import playground.thibautd.router.controler.MultiLegRoutingControler;
import playground.thibautd.router.StageActivityTypes;

/**
 * @author thibautd
 */
public class TripsToLegModule extends AbstractMultithreadedModule {
	private final MultiLegRoutingControler controler;
	private final StageActivityTypes blackList;

	public TripsToLegModule(final Controler controler) {
		this( controler , null );
	}

	public TripsToLegModule(final Controler controler, final StageActivityTypes blackList) {
		super( controler.getConfig().global() );
		this.controler = (MultiLegRoutingControler) controler;
		this.blackList = blackList;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return blackList == null ?
			new TripsToLegsAlgorithm( controler.getTripRouterFactory().createTripRouter() ) :
			new TripsToLegsAlgorithm( controler.getTripRouterFactory().createTripRouter() , blackList );
	}
}

