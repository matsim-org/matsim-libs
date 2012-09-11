/* *********************************************************************** *
 * project: org.matsim.*
 * PTTravelTimeKTIFactory.java
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.TravelTimeFactory;

import playground.meisterk.kti.config.KtiConfigGroup;
import playground.meisterk.kti.router.PlansCalcRouteKtiInfo;

public class PTTravelTimeKTIFactory implements TravelTimeFactory {
	
	/*package*/ final Scenario scenario;
	private final TravelTime ptTravelTime;
	private final PlansCalcRouteKtiInfo plansCalcRouteKtiInfo;
	private final Map<Id, Double> agentSpeedMap;
	
	public PTTravelTimeKTIFactory(Scenario scenario, TravelTime travelTime) {
		this.scenario = scenario;
		this.ptTravelTime = travelTime;
		this.agentSpeedMap = new ConcurrentHashMap<Id, Double>();
		
		KtiConfigGroup ktiConfigGroup = (KtiConfigGroup) scenario.getConfig().getModule(KtiConfigGroup.GROUP_NAME);
		plansCalcRouteKtiInfo = new PlansCalcRouteKtiInfo(ktiConfigGroup);
		plansCalcRouteKtiInfo.prepare(scenario.getNetwork());
	}
	
	@Override
	public PTTravelTimeKTI createTravelTime() {
		return new PTTravelTimeKTI(plansCalcRouteKtiInfo, scenario.getConfig().plansCalcRoute(), agentSpeedMap,
				ptTravelTime);
	}
	
	public PlansCalcRouteKtiInfo getPlansCalcRouteKtiInfo() {
		return this.plansCalcRouteKtiInfo;
	}
}
