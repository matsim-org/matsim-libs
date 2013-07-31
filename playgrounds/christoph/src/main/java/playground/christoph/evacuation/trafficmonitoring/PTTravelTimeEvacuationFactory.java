/* *********************************************************************** *
 * project: org.matsim.*
 * PTTravelTimeEvacuationInitializer.java
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

package playground.christoph.evacuation.trafficmonitoring;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.TravelTimeFactory;

import playground.christoph.evacuation.mobsim.InformedAgentsTracker;
import playground.christoph.evacuation.pt.EvacuationTransitRouterFactory;

public class PTTravelTimeEvacuationFactory implements TravelTimeFactory {

	static final Logger log = Logger.getLogger(PTTravelTimeEvacuationFactory.class);
	
	private final EvacuationTransitRouterFactory evacuationRouterFactory;
	private final InformedAgentsTracker informedAgentsTracker;
	private final TravelTime ptTravelTime;
	private final Map<Id, Double> agentSpeedMap;
	
	public PTTravelTimeEvacuationFactory(Scenario scenario, TravelTime ptTravelTime,
			EvacuationTransitRouterFactory evacuationRouterFactory, InformedAgentsTracker informedAgentsTracker) {
		this.ptTravelTime = ptTravelTime;
		this.evacuationRouterFactory = evacuationRouterFactory;
		this.informedAgentsTracker = informedAgentsTracker;
		
		this.agentSpeedMap = new ConcurrentHashMap<Id, Double>();
	}

	@Override
	public SwissPTTravelTime createTravelTime() {
		return new PTTravelTimeEvacuation(evacuationRouterFactory, agentSpeedMap, ptTravelTime, informedAgentsTracker);
	}

}