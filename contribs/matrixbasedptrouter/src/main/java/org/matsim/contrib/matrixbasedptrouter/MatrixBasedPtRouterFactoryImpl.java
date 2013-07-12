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
import org.matsim.core.router.DefaultTripRouterFactoryImpl;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TripRouter;

/**
 * @author thomas
 *
 */
public class MatrixBasedPtRouterFactoryImpl implements TripRouterFactory {
	private static final Logger log = Logger.getLogger(MatrixBasedPtRouterFactoryImpl.class);



	private final PtMatrix ptMatrix;
	// The single instance of ptMatrix is passed to multiple instances of the TripRouter.  Looks to me like this will work, since
	// there is only read access to ptMatrix.  kai, may'13

	private TripRouterFactory delegate;



	private Scenario scenario;

//	private boolean firstCall = true;
	
	public MatrixBasedPtRouterFactoryImpl(final Scenario scenario, final PtMatrix ptMatrix) {
		this.ptMatrix  = ptMatrix;
		this.delegate = DefaultTripRouterFactoryImpl.createRichTripRouterFactoryImpl(scenario);
		this.scenario = scenario;
	}
	
	@Override
	public TripRouter instantiateAndConfigureTripRouter(RoutingContext iterationContext) {
	
		//initialize TripRouterFactoyImpl only once
//		if(firstCall){ // I think this does not belong here but maybe I am overlooking something??? kai, jul'13
//			log.warn("overriding default Pt-RoutingModule with PseudoPtRoutingModule. Message thrown only once.");
			if ( scenario.getConfig().scenario().isUseTransit() ) {
				log.warn("you try to use PseudoPtRoutingModule and physical transit simulation at the same time. This probably will not work!");
			}
//			firstCall = false;
//		}
		//initialize triprouter
		TripRouter tripRouter = this.delegate.instantiateAndConfigureTripRouter(iterationContext);

		// add improved pseudo-pt-routing
		tripRouter.setRoutingModule(TransportMode.pt, new MatrixBasedPtRoutingModule(scenario, ptMatrix));
		
		return tripRouter;
	}
}
