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
package org.matsim.contrib.improvedPseudoPt;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.TripRouterFactoryImpl;

/**
 * @author thomas
 *
 */
public class MatrixBasedPtRouterFactoryImpl implements TripRouterFactory{
	private static final Logger log = Logger
			.getLogger(MatrixBasedPtRouterFactoryImpl.class);

	private final Controler controler;

	private final PtMatrix ptMatrix;
	// The single instance of ptMatrix is passed to multiple instances of the TripRouter.  Looks to me like this will work, since
	// there is only read access to ptMatrix.  kai, may'13

	private TripRouterFactoryImpl delegate;

	private boolean firstCall = true;
	
	public MatrixBasedPtRouterFactoryImpl(final Controler controler, final PtMatrix ptMatrix) {
		this.controler = controler;
		this.ptMatrix  = ptMatrix;
	}
	
	@Override
	public TripRouter instantiateAndConfigureTripRouter() {
	
		//initialize TripRouterFactoyImpl only once
		if(firstCall){
			this.delegate = new TripRouterFactoryImpl(
					controler.getScenario(),
					controler.getTravelDisutilityFactory(), 
					controler.getLinkTravelTimes(), 
					controler.getLeastCostPathCalculatorFactory(), 
					controler.getScenario().getConfig().scenario().isUseTransit() ? controler.getTransitRouterFactory() : null);
			log.warn("overriding default Pt-RoutingModule with PseudoPtRoutingModule. Message thrown only once.");
			if ( controler.getConfig().scenario().isUseTransit() ) {
				log.warn("you try to use PseudoPtRoutingModule and physical transit simulation at the same time. This probably will not work!");
			}
			firstCall = false;
		}
		//initialize triprouter
		TripRouter tripRouter = this.delegate.instantiateAndConfigureTripRouter();

		// add improved pseudo-pt-routing
		tripRouter.setRoutingModule(TransportMode.pt, 
				new MatrixBasedPtRoutingModule(controler, ptMatrix));
		
		return tripRouter;
	}
}
