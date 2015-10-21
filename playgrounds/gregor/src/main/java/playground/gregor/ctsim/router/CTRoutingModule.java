package playground.gregor.ctsim.router;
/* *********************************************************************** *
 * project: org.matsim.*
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.facilities.Facility;

import java.util.List;

/**
 * Created by laemmel on 21/10/15.
 */
public class CTRoutingModule implements RoutingModule {


	private final RoutingModule delegate;

	public CTRoutingModule(Controler c) {
		this.delegate = DefaultRoutingModules.createNetworkRouter("walkct", c.getScenario().getPopulation()
				.getFactory(), c.getScenario().getNetwork(), createRoutingAlgo(c));
	}

	private LeastCostPathCalculator createRoutingAlgo(Controler c) {
		return new AStarLandmarksFactory(
				c.getScenario().getNetwork(),
				new FreespeedTravelTimeAndDisutility(c.getConfig().planCalcScore()),
				c.getConfig().global().getNumberOfThreads()).createPathCalculator(c.getScenario().getNetwork(), c.createTravelDisutilityCalculator(), c.getLinkTravelTimes());
	}

	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime, Person person) {
		return delegate.calcRoute(fromFacility, toFacility, departureTime, person);
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return delegate.getStageActivityTypes();
	}
}
