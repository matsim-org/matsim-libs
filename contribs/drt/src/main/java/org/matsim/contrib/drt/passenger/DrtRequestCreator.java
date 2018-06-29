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

package org.matsim.contrib.drt.passenger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.DefaultDrtOptimizer;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.router.FastAStarEuclideanFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * @author michalm
 */
public class DrtRequestCreator implements PassengerRequestCreator {
	private final DrtConfigGroup drtCfg;
	private final TravelTime travelTime;
	private final LeastCostPathCalculator router;
	private final EventsManager eventsManager;
	private final MobsimTimer timer;

	@Inject
	public DrtRequestCreator(DrtConfigGroup drtCfg, @Named(DvrpRoutingNetworkProvider.DVRP_ROUTING) Network network,
			@Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime, EventsManager eventsManager,
			MobsimTimer timer, @Named(DefaultDrtOptimizer.DRT_OPTIMIZER) TravelDisutility travelDisutility) {
		this.drtCfg = drtCfg;
		this.travelTime = travelTime;
		this.eventsManager = eventsManager;
		this.timer = timer;
		// Euclidean with overdoFactor > 1.0 could lead to 'experiencedTT < unsharedRideTT',
		// while the benefit would be a marginal reduction of computation time ==> so stick to 1.0
		router = new FastAStarEuclideanFactory().createPathCalculator(network, travelDisutility, travelTime);
	}

	@Override
	public DrtRequest createRequest(Id<Request> id, MobsimPassengerAgent passenger, Link fromLink, Link toLink,
			double departureTime, double submissionTime) {

		//XXX this will not work if pre-booking is allowed in DRT
		Leg leg = (Leg)((PlanAgent)passenger).getCurrentPlanElement();
		DrtRoute drtRoute = (DrtRoute)leg.getRoute();

		double latestDepartureTime = departureTime + drtRoute.getMaxWaitTime();
		double latestArrivalTime = departureTime + drtRoute.getTravelTime();

		eventsManager.processEvent(
				new DrtRequestSubmittedEvent(timer.getTimeOfDay(), id, passenger.getId(), fromLink.getId(),
						toLink.getId(), drtRoute.getDirectRideTime(), drtRoute.getDistance()));

		return new DrtRequest(id, passenger, fromLink, toLink, departureTime, latestDepartureTime, latestArrivalTime,
				submissionTime);
	}
}
