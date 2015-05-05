/* *********************************************************************** *
 * project: org.matsim.*
 * RandomizedTransitRouterFacotry
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
package playground.vsp.randomizedtransitrouter;

import org.matsim.core.config.Config;
import org.matsim.pt.router.*;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import playground.vsp.randomizedtransitrouter.RandomizedTransitRouterTravelTimeAndDisutility.DataCollection;

import javax.inject.Inject;


/**
 * @author dgrether
 *
 */
public class RandomizedTransitRouterFactory implements TransitRouterFactory {

	private TransitRouterConfig trConfig;
	private TransitSchedule schedule;
	private TransitRouterNetwork routerNetwork;

    @Inject
	RandomizedTransitRouterFactory(Config config, TransitSchedule schedule) {
		this.trConfig = new TransitRouterConfig(config);
		this.schedule = schedule;
		this.routerNetwork = TransitRouterNetwork.createFromSchedule(schedule, trConfig.beelineWalkConnectionDistance);
	}
	
	@Override
	public TransitRouter get() {
		RandomizedTransitRouterTravelTimeAndDisutility ttCalculator = new RandomizedTransitRouterTravelTimeAndDisutility(trConfig);
		ttCalculator.setDataCollection(DataCollection.randomizedParameters, true) ;
		ttCalculator.setDataCollection(DataCollection.additionalInformation, false) ;
		return new TransitRouterImpl(trConfig, new PreparedTransitSchedule(schedule), routerNetwork, ttCalculator, ttCalculator);
	}

}
