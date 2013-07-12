/* *********************************************************************** *
 * project: org.matsim.*
 * TripRouterObjectProvider.java
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

package playground.christoph.evacuation.controler;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.router.TransitRouterFactory;

public class TripRouterObjectProvider {

	private final Controler controler;
	private final Scenario scenario;
	private final TravelDisutilityFactory disutilityFactory;
	private final TravelTime travelTime;
	private final LeastCostPathCalculatorFactory leastCostAlgoFactory;
	private final TransitRouterFactory transitRouterFactory;
	
	public TripRouterObjectProvider(Controler controler) {
		this.controler = controler;
		this.scenario = null;
		this.disutilityFactory = null;
		this.travelTime = null;
		this.leastCostAlgoFactory = null;
		this.transitRouterFactory = null;
	}
	
	public TripRouterObjectProvider(final Scenario scenario,
			final TravelDisutilityFactory disutilityFactory,
			final TravelTime travelTime,
			final LeastCostPathCalculatorFactory leastCostAlgoFactory,
			final TransitRouterFactory transitRouterFactory) {
		this.controler = null;
		this.scenario = scenario;
		this.disutilityFactory = disutilityFactory;
		this.travelTime = travelTime;
		this.leastCostAlgoFactory = leastCostAlgoFactory;
		this.transitRouterFactory = transitRouterFactory;
	}
	
	public Scenario getScenario() {
		if (scenario != null) return scenario;
		else return controler.getScenario();
	}

	public TravelDisutilityFactory getTravelDisutilityFactory() {
		if (disutilityFactory != null)return disutilityFactory;
		else return controler.getTravelDisutilityFactory();
	}

	public TravelTime getTravelTime() {
		if (travelTime != null) return travelTime;
		else return controler.getLinkTravelTimes();
	}

	public LeastCostPathCalculatorFactory getLeastCostPathCalculatorFactory() {
		if (leastCostAlgoFactory != null) return leastCostAlgoFactory;
		else return controler.getLeastCostPathCalculatorFactory();
	}

	public TransitRouterFactory getTransitRouterFactory() {
		if (transitRouterFactory != null) return transitRouterFactory;
		else if (controler != null) return controler.getTransitRouterFactory();
		else return null;
	}
}
