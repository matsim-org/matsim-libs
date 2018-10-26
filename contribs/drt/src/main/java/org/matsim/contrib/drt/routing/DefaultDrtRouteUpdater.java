/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.run.Drt;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.util.ExecutorServiceWithResource;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.router.FastAStarEuclideanFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.name.Named;

/**
 * @author michalm (Michal Maciejewski)
 */
public class DefaultDrtRouteUpdater implements ShutdownListener, DrtRouteUpdater {

	private final DrtConfigGroup drtCfg;
	private final Network network;
	private final TravelTime travelTime;
	private final Population population;
	private final ExecutorServiceWithResource<LeastCostPathCalculator> executorService;

	@Inject
	public DefaultDrtRouteUpdater(DrtConfigGroup drtCfg,
			@Named(DvrpRoutingNetworkProvider.DVRP_ROUTING) Network network,
			@Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime,
			@Drt TravelDisutilityFactory travelDisutilityFactory, Population population, Config config) {
		this.drtCfg = drtCfg;
		this.network = network;
		this.travelTime = travelTime;
		this.population = population;

		// Euclidean with overdoFactor > 1.0 could lead to 'experiencedTT < unsharedRideTT',
		// while the benefit would be a marginal reduction of computation time ==> so stick to 1.0
		// XXX uses the global.numberOfThreads, not drt.numberOfThreads as this is executed in the replanning phase
		executorService = new ExecutorServiceWithResource<>(IntStream.range(0, config.global().getNumberOfThreads())
				.mapToObj(i -> new FastAStarEuclideanFactory().createPathCalculator(network,
						travelDisutilityFactory.createTravelDisutility(travelTime), travelTime))
				.collect(Collectors.toList()));
	}

	@Override
	public void notifyReplanning(ReplanningEvent event) {
		Stream<Leg> drtLegs = population.getPersons()
				.values()
				.stream()
				.flatMap(p -> p.getSelectedPlan().getPlanElements().stream())
				.filter(e -> e instanceof Leg && ((Leg)e).getMode().equals(TransportMode.drt))
				.map(e -> (Leg)e);
		executorService.submitRunnablesAndWait(drtLegs.map(l -> (router -> updateDrtRoute(router, l))));
	}

	private void updateDrtRoute(LeastCostPathCalculator router, Leg drtLeg) {
		DrtRoute drtRoute = (DrtRoute)drtLeg.getRoute();
		Link fromLink = network.getLinks().get(drtRoute.getStartLinkId());
		Link toLink = network.getLinks().get(drtRoute.getEndLinkId());

		VrpPathWithTravelData unsharedPath = VrpPaths.calcAndCreatePath(fromLink, toLink, drtLeg.getDepartureTime(),
				router, travelTime);
		double unsharedRideTime = unsharedPath.getTravelTime();//includes first & last link
		double maxTravelTime = drtCfg.getMaxTravelTimeAlpha() * unsharedRideTime + drtCfg.getMaxTravelTimeBeta();
		double unsharedDistance = VrpPaths.calcDistance(unsharedPath);//includes last link

		drtRoute.setDistance(unsharedDistance);
		drtRoute.setTravelTime(maxTravelTime);
		drtRoute.setUnsharedRideTime(unsharedRideTime);
		drtRoute.setMaxWaitTime(drtCfg.getMaxWaitTime());
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		executorService.shutdown();
	}
}
