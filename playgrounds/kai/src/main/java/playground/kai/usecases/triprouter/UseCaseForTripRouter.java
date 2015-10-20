/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package playground.kai.usecases.triprouter;

import org.matsim.core.controler.Controler;
import org.matsim.core.router.*;

import javax.inject.Provider;

/**
 * @author nagel
 *
 */
class UseCaseForTripRouter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		final Controler ctrl = new Controler(args) ;
		
		ctrl.setTripRouterFactory( new javax.inject.Provider<org.matsim.core.router.TripRouter>() {
		
			@Override
			public TripRouter get() {
				Provider<TripRouter> trf = TripRouterFactoryBuilderWithDefaults.createDefaultTripRouterFactoryImpl(ctrl.getScenario());
				TripRouter router = trf.get() ;
				return router ;
			}
			
		}) ;
		
	}

}
