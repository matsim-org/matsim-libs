package org.matsim.contrib.common.randomizedtransitrouter;/* *********************************************************************** *
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

import org.matsim.core.config.Config;
import org.matsim.pt.router.*;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import javax.inject.Inject;
import javax.inject.Provider;


/**
 * @author dgrether
 *
 */
public class RandomizingTransitRouterFactory implements Provider<TransitRouter> {

	private TransitRouterConfig trConfig;
	private TransitSchedule schedule;
	private TransitRouterNetwork routerNetwork;

    @Inject
	RandomizingTransitRouterFactory(Config config, TransitSchedule schedule) {
		this.trConfig = new TransitRouterConfig(config);
		this.schedule = schedule;
		this.routerNetwork = TransitRouterNetwork.createFromSchedule(schedule, trConfig.getBeelineWalkConnectionDistance());
	}
	
	@Override
	public TransitRouter get() {
		RandomizingTransitRouterTravelTimeAndDisutility ttCalculator = new RandomizingTransitRouterTravelTimeAndDisutility(trConfig);
		ttCalculator.setDataCollection(RandomizingTransitRouterTravelTimeAndDisutility.DataCollection.randomizedParameters, true) ;
		ttCalculator.setDataCollection(RandomizingTransitRouterTravelTimeAndDisutility.DataCollection.additionalInformation, false) ;
		return new TransitRouterImpl(trConfig, new PreparedTransitSchedule(schedule), routerNetwork, ttCalculator, ttCalculator);
	}

}
