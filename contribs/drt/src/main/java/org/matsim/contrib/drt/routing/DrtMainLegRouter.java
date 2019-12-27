/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.routing;

import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.Facility;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.name.Named;

/**
 * @author jbischoff
 * @author michalm (Michal Maciejewski)
 * @author Kai Nagel
 */
public class DrtMainLegRouter implements RoutingModule {
	private final DrtConfigGroup drtCfg;
	private final TravelTime travelTime;
	private final LeastCostPathCalculator router;
	private final PopulationFactory populationFactory;
	private final Network modalNetwork;

	public DrtMainLegRouter(DrtConfigGroup drtCfg, Network modalNetwork,
			LeastCostPathCalculatorFactory leastCostPathCalculatorFactory,
			@Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime,
			TravelDisutilityFactory travelDisutilityFactory, PopulationFactory populationFactory) {
		// constructor was public when I found it, and cannot be made package private.  Thus now passing scenario as argument so we have a bit more
		// flexibility for changes without having to change the argument list every time.  kai, jul'19
		// back to PopulationFactory. The class will be made package-protected in the future... michal

		this.drtCfg = drtCfg;
		this.travelTime = travelTime;
		this.populationFactory = populationFactory;
		this.modalNetwork = modalNetwork;

		// Euclidean with overdoFactor > 1.0 could lead to 'experiencedTT < unsharedRideTT',
		// while the benefit would be a marginal reduction of computation time ==> so stick to 1.0
		router = leastCostPathCalculatorFactory.createPathCalculator(modalNetwork,
				travelDisutilityFactory.createTravelDisutility(travelTime), travelTime);
	}

	/**
	 * Calculates the maximum travel time defined as: drtCfg.getMaxTravelTimeAlpha() * unsharedRideTime + drtCfg.getMaxTravelTimeBeta()
	 *
	 * @param drtCfg
	 * @param unsharedRideTime ride time of the direct (shortest-time) route
	 * @return maximum travel time
	 */
	static double getMaxTravelTime(DrtConfigGroup drtCfg, double unsharedRideTime) {
		return drtCfg.getMaxTravelTimeAlpha() * unsharedRideTime + drtCfg.getMaxTravelTimeBeta();
	}

	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime,
			Person person) {
		Link accessActLink = Preconditions.checkNotNull(modalNetwork.getLinks().get(fromFacility.getLinkId()),
				"link: %s does not exist in the network of mode: %s", fromFacility.getLinkId(), drtCfg.getMode());
		Link egressActLink = Preconditions.checkNotNull(modalNetwork.getLinks().get(toFacility.getLinkId()),
				"link: %s does not exist in the network of mode: %s", toFacility.getLinkId(), drtCfg.getMode());
		DrtRoute route = createDrtRoute(departureTime, accessActLink, egressActLink);

		Leg leg = populationFactory.createLeg(drtCfg.getMode());
		leg.setDepartureTime(departureTime);
		leg.setTravelTime(route.getTravelTime());
		leg.setRoute(route);
		return ImmutableList.of(leg);
	}

	DrtRoute createDrtRoute(double departureTime, Link accessActLink, Link egressActLink) {
		VrpPathWithTravelData unsharedPath = VrpPaths.calcAndCreatePath(accessActLink, egressActLink, departureTime,
				router, travelTime);
		double unsharedRideTime = unsharedPath.getTravelTime();//includes first & last link
		double maxTravelTime = getMaxTravelTime(drtCfg, unsharedRideTime);
		double unsharedDistance = VrpPaths.calcDistance(unsharedPath);//includes last link

		DrtRoute route = populationFactory.getRouteFactories()
				.createRoute(DrtRoute.class, accessActLink.getId(), egressActLink.getId());
		route.setDistance(unsharedDistance);
		route.setTravelTime(maxTravelTime);
		route.setUnsharedRideTime(unsharedRideTime);
		route.setMaxWaitTime(drtCfg.getMaxWaitTime());
		return route;
	}
}
