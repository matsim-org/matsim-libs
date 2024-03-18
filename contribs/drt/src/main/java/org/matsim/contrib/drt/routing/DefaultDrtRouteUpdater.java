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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.router.DefaultMainLegRouter.RouteCreator;
import org.matsim.contrib.util.ExecutorServiceWithResource;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.utils.objectattributes.attributable.Attributes;

import com.google.common.util.concurrent.Futures;

/**
 * @author michalm (Michal Maciejewski)
 */
public class DefaultDrtRouteUpdater implements ShutdownListener, DrtRouteUpdater {
	private final DrtConfigGroup drtCfg;
	private final Network network;
	private final Population population;
	private final DrtEstimator drtEstimator;
	private final ExecutorServiceWithResource<? extends RouteCreator> executorService;

	public DefaultDrtRouteUpdater(DrtConfigGroup drtCfg, Network network, Population population, Config config,
								  Supplier<RouteCreator> drtRouteCreatorSupplier, DrtEstimator drtEstimator) {
		this.drtCfg = drtCfg;
		this.network = network;
		this.population = population;
		this.drtEstimator = drtEstimator;

		// XXX uses the global.numberOfThreads, not drt.numberOfThreads, as this is executed in the replanning phase
		executorService = new ExecutorServiceWithResource<>(IntStream.range(0, config.global().getNumberOfThreads())
				.mapToObj(i -> drtRouteCreatorSupplier.get())
				.collect(Collectors.toList()));
	}

	@Override
	public void notifyReplanning(ReplanningEvent event) {
		List<Future<?>> futures = new LinkedList<>();

		for (Person person : population.getPersons().values()) {
			for (Trip trip : TripStructureUtils.getTrips(person.getSelectedPlan())) {
				for (Leg leg : trip.getLegsOnly()) {
					if (leg.getMode().equals(drtCfg.getMode())) {
						futures.add(executorService.submitRunnable(
								router -> updateDrtRoute(router, person, trip.getTripAttributes(), leg)));
					}
				}
			}
		}

		futures.forEach(Futures::getUnchecked);
	}

	private void updateDrtRoute(RouteCreator drtRouteCreator, Person person, Attributes tripAttributes, Leg drtLeg) {
		Link fromLink = network.getLinks().get(drtLeg.getRoute().getStartLinkId());
		Link toLink = network.getLinks().get(drtLeg.getRoute().getEndLinkId());
		RouteFactories routeFactories = population.getFactory().getRouteFactories();

		Route drtRoute = drtRouteCreator.createRoute(drtLeg.getDepartureTime().seconds(), fromLink, toLink, person,
			tripAttributes, routeFactories);
		drtLeg.setRoute(drtRoute);

		// update DRT estimate
		if (drtEstimator != null) {
			DrtEstimator.Estimate estimate = drtEstimator.estimate((DrtRoute) drtRoute, drtLeg.getDepartureTime());
			DrtEstimator.setEstimateAttributes(drtLeg, estimate);
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		executorService.shutdown();
	}
}
