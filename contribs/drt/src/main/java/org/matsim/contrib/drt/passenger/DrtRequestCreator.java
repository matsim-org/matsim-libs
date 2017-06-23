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
import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.DefaultDrtOptimizerProvider;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.path.*;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.*;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.*;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.*;

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

	@Inject(optional = true)
	private @Named(DefaultDrtOptimizerProvider.DRT_OPTIMIZER) TravelDisutilityFactory travelDisutilityFactory;

	@Inject
	public DrtRequestCreator(DrtConfigGroup drtCfg, @Named(DvrpModule.DVRP_ROUTING) Network network,
			@Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime, QSim qSim) {
		this.drtCfg = drtCfg;
		this.travelTime = travelTime;
		this.eventsManager = qSim.getEventsManager();
		this.timer = qSim.getSimTimer();

		TravelDisutility travelDisutility = travelDisutilityFactory == null ? new TimeAsTravelDisutility(travelTime)
				: travelDisutilityFactory.createTravelDisutility(travelTime);

		PreProcessEuclidean preProcessEuclidean = new PreProcessEuclidean(travelDisutility);
		preProcessEuclidean.run(network);

		FastRouterDelegateFactory fastRouterFactory = new ArrayFastRouterDelegateFactory();
		RoutingNetwork routingNetwork = new ArrayRoutingNetworkFactory(preProcessEuclidean)
				.createRoutingNetwork(network);

		router = new FastAStarEuclidean(routingNetwork, preProcessEuclidean, travelDisutility, travelTime,
				drtCfg.getAStarEuclideanOverdoFactor(), fastRouterFactory);
	}

	@Override
	public DrtRequest createRequest(Id<Request> id, MobsimPassengerAgent passenger, Link fromLink, Link toLink,
			double departureTime, double submissionTime) {
		double latestDepartureTime = departureTime + drtCfg.getMaxWaitTime();

		VrpPathWithTravelData unsharedRidePath = VrpPaths.calcAndCreatePath(fromLink, toLink, departureTime, router,
				travelTime);

		double optimisticTravelTime = unsharedRidePath.getTravelTime();
		double maxTravelTime = drtCfg.getMaxTravelTimeAlpha() * optimisticTravelTime + drtCfg.getMaxTravelTimeBeta();
		double latestArrivalTime = departureTime + maxTravelTime;
		
		double unsharedDistance = VrpPaths.calcPathDistance(unsharedRidePath);
		
		eventsManager.processEvent(new DrtRequestSubmittedEvent(timer.getTimeOfDay(), id, passenger.getId(),
				fromLink.getId(), toLink.getId(), unsharedRidePath.getTravelTime(), unsharedDistance));

		return new DrtRequest(id, passenger, fromLink, toLink, departureTime, latestDepartureTime, latestArrivalTime,
				submissionTime, unsharedRidePath);
	}
}
