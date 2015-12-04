/* *********************************************************************** *
 * project: org.matsim.													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

/**
 * 
 */
package org.matsim.contrib.matrixbasedptrouter;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.router.*;

import javax.inject.Provider;

/**
 * @author thomas
 *
 */
public final class MatrixBasedPtRouterFactoryImpl implements Provider<TripRouter> {

	private static final Logger log = Logger.getLogger(MatrixBasedPtRouterFactoryImpl.class);

	private Provider<TripRouter> delegate;

	private final PtMatrix ptMatrix; // immutable, shared between instances

	private Scenario scenario;

	public MatrixBasedPtRouterFactoryImpl(final Scenario scenario, final PtMatrix ptMatrix) {
		this.ptMatrix = ptMatrix;
		this.delegate = TripRouterFactoryBuilderWithDefaults.createDefaultTripRouterFactoryImpl(scenario);
		this.scenario = scenario;
	}

	@Override
	public TripRouter get() {
		if ( scenario.getConfig().transit().isUseTransit() ) {
			log.warn("you try to use PseudoPtRoutingModule and physical transit simulation at the same time. This probably will not work!");
		}
		TripRouter tripRouter = this.delegate.get();
		tripRouter.setRoutingModule(TransportMode.pt, new MatrixBasedPtRoutingModule(scenario, ptMatrix));
		return tripRouter;
	}
	
}
