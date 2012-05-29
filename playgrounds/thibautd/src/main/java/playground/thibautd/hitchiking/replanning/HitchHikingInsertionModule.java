/* *********************************************************************** *
 * project: org.matsim.*
 * HitchHikingInsertionModule.java
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
package playground.thibautd.hitchiking.replanning;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.hitchiking.HitchHikingConfigGroup;
import playground.thibautd.hitchiking.HitchHikingSpots;
import playground.thibautd.hitchiking.HitchHikingUtils;
import playground.thibautd.hitchiking.routing.HitchHikingDriverRoutingModule;
import playground.thibautd.hitchiking.routing.HitchHikingPassengerRoutingModule;
import playground.thibautd.router.controler.MultiLegRoutingControler;
import playground.thibautd.router.TripRouter;
import playground.thibautd.router.TripRouterFactory;

/**
 * @author thibautd
 */
public class HitchHikingInsertionModule extends AbstractMultithreadedModule {
	private final MultiLegRoutingControler controler;
	private final HitchHikingSpots spots;

	public HitchHikingInsertionModule(
			final Controler controler) {
		super( controler.getConfig().global() );
		this.controler = (MultiLegRoutingControler) controler;
		this.spots = HitchHikingUtils.getSpots( controler.getScenario() );
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		TripRouterFactory factory = controler.getTripRouterFactory();

		TripRouter tripRouter = factory.createTripRouter();
		HitchHikingDriverRoutingModule driverModule =
			new HitchHikingDriverRoutingModule(
					factory.getRoutingModuleFactories().get( TransportMode.car ).createModule( TransportMode.car , factory ),
					spots,
					(HitchHikingConfigGroup) controler.getConfig().getModule( HitchHikingConfigGroup.GROUP_NAME ));
		HitchHikingPassengerRoutingModule passengerModule =
			new HitchHikingPassengerRoutingModule(
					factory.getRoutingModuleFactories().get( TransportMode.pt ).createModule( TransportMode.pt , factory ),
					spots,
					((PopulationFactoryImpl) controler.getPopulation().getFactory()).getModeRouteFactory());

		return new HitchHikingInsertionAlgorithm(
				MatsimRandom.getLocalInstance(),
				tripRouter);
	}
}

