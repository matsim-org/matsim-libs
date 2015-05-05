/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.mmoyo.randomizerPtRouter;

import org.matsim.pt.router.*;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import playground.vsp.randomizedtransitrouter.RandomizedTransitRouterTravelTimeAndDisutility;

import javax.inject.Provider;

public class RndPtRouterFactory {

	public Provider<TransitRouter> createFactory(final TransitSchedule schedule, final TransitRouterConfig trConfig, final TransitRouterNetwork routerNetwork, final boolean rndParams, final boolean addInfo){
		return 
		new Provider<TransitRouter>() {
			@Override
			public TransitRouter get() {
				RandomizedTransitRouterTravelTimeAndDisutility ttCalculator = new RandomizedTransitRouterTravelTimeAndDisutility(trConfig);
//				ttCalculator.setDataCollection(DataCollection.randomizedParameters, rndParams) ;
//				ttCalculator.setDataCollection(DataCollection.additionalInformation, addInfo) ;
				return new TransitRouterImpl(trConfig, new PreparedTransitSchedule(schedule), routerNetwork, ttCalculator, ttCalculator);
			}
		};
	}
	
	/**
	 * This version receives already the  PreparedTransitSchedule instead of creating it 
	 */
	public static Provider<TransitRouter> createFactory (final PreparedTransitSchedule preparedSchedule, final TransitRouterConfig trConfig, final TransitRouterNetwork routerNetwork, final boolean rndParams, final boolean addInfo){
		return 
		new Provider<TransitRouter>() {
			@Override
			public TransitRouter get() {
				RandomizedTransitRouterTravelTimeAndDisutility ttCalculator = new RandomizedTransitRouterTravelTimeAndDisutility(trConfig);
				//ttCalculator.setDataCollection(DataCollection.randomizedParameters, false) ;
				//ttCalculator.setDataCollection(DataCollection.additionInformation, false) ;
				return new TransitRouterImpl(trConfig, preparedSchedule, routerNetwork, ttCalculator, ttCalculator);
			}
		};
	}
	

}