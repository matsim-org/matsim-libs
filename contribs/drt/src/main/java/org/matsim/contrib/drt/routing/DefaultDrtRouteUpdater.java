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

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.util.ExecutorServiceWithResource;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.FastAStarEuclideanFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.name.Named;

import one.util.streamex.StreamEx;

/**
 * @author michalm (Michal Maciejewski)
 */
public class DefaultDrtRouteUpdater implements ShutdownListener, DrtRouteUpdater {
	private final DrtConfigGroup drtCfg;
	private final Network network;
	private final Population population;
	private final ExecutorServiceWithResource<DrtRouteCreator> executorService;

	public DefaultDrtRouteUpdater(DrtConfigGroup drtCfg, Network network,
			@Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime,
			TravelDisutilityFactory travelDisutilityFactory, Population population, Config config) {
		this.drtCfg = drtCfg;
		this.network = network;
		this.population = population;

		// Euclidean with overdoFactor > 1.0 could lead to 'experiencedTT < unsharedRideTT',
		// while the benefit would be a marginal reduction of computation time ==> so stick to 1.0
		LeastCostPathCalculatorFactory factory = new FastAStarEuclideanFactory();
		// XXX uses the global.numberOfThreads, not drt.numberOfThreads, as this is executed in the replanning phase
		executorService = new ExecutorServiceWithResource<>(IntStream.range(0, config.global().getNumberOfThreads())
				.mapToObj(i -> new DrtRouteCreator(drtCfg, network, factory, travelTime, travelDisutilityFactory))
				.collect(Collectors.toList()));
	}

	@Override
	public void notifyReplanning(ReplanningEvent event) {
		Stream<Leg> drtLegs = StreamEx.of(population.getPersons().values())
				.flatMap(p -> p.getSelectedPlan().getPlanElements().stream())
				.select(Leg.class)
				.filterBy(Leg::getMode, drtCfg.getMode());
		executorService.submitRunnablesAndWait(drtLegs.map(l -> (router -> updateDrtRoute(router, l))));
	}

	private void updateDrtRoute(DrtRouteCreator drtRouteCreator, Leg drtLeg) {
		Link fromLink = network.getLinks().get(drtLeg.getRoute().getStartLinkId());
		Link toLink = network.getLinks().get(drtLeg.getRoute().getEndLinkId());
		RouteFactories routeFactories = population.getFactory().getRouteFactories();
		drtLeg.setRoute(drtRouteCreator.createRoute(drtLeg.getDepartureTime().seconds(), fromLink, toLink, routeFactories));
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		executorService.shutdown();
	}
}
