/* *********************************************************************** *
 * project: org.matsim.*
 * ParkAndRideModule.java
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
package playground.thibautd.parknride;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.population.algorithms.PermissibleModesCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeCost;

import playground.thibautd.router.controler.MultiLegRoutingControler;
import playground.thibautd.router.TripRouter;
import playground.thibautd.router.TripRouterFactory;

/**
 * @author thibautd
 */
public class ParkAndRideModule extends AbstractMultithreadedModule {
	private final MultiLegRoutingControler controler;
	private final PersonalizableTravelTime pnrTime;
	private final PersonalizableTravelCost pnrCost;

	public ParkAndRideModule(final MultiLegRoutingControler controler) {
		super( controler.getConfig().global() );
		this.controler = controler;

		ParkAndRideTravelTimeCost timeCost = new ParkAndRideTravelTimeCost();
		this.pnrTime = timeCost;
		this.pnrCost = timeCost;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		TripRouterFactory tripRouterFactory = controler.getTripRouterFactory();
		TripRouter tripRouter = tripRouterFactory.createTripRouter();
		TransitRouterConfig transitConfig =
			new TransitRouterConfig(
					controler.getConfig().planCalcScore(),
					controler.getConfig().plansCalcRoute(),
					controler.getConfig().transitRouter(),
					controler.getConfig().vspExperimental());

		PersonalizableTravelTime carTime = tripRouterFactory.getTravelTimeCalculatorFactory().createTravelTime();
		PersonalizableTravelCost carCost =
			tripRouterFactory.getTravelCostCalculatorFactory().createTravelCostCalculator(
					carTime, controler.getConfig().planCalcScore() );
		TransitRouterNetworkTravelTimeCost ptTimeCost =
					new TransitRouterNetworkTravelTimeCost( transitConfig );

		ParkAndRideRoutingModule routingModule =
			new ParkAndRideRoutingModule(
					((PopulationFactoryImpl) controler.getPopulation().getFactory()).getModeRouteFactory(),
					controler.getPopulation().getFactory(),
					controler.getNetwork(),
					controler.getTransitRouterFactory().createTransitRouter().getSchedule(),
					transitConfig.beelineWalkConnectionDistance,
					ParkAndRideUtils.getParkAndRideFacilities( controler.getScenario() ),
					transitConfig,
					carCost,
					carTime,
					ptTimeCost,
					pnrCost,
					pnrTime);

		ParkAndRideIncluder includer =
			new ParkAndRideIncluder(
					ParkAndRideUtils.getParkAndRideFacilities( controler.getScenario() ),
					routingModule,
					tripRouter);

		ParkAndRideConfigGroup configGroup = ParkAndRideUtils.getConfigGroup( controler.getConfig() );
		ParkAndRideChooseModeForSubtour algo =
			new ParkAndRideChooseModeForSubtour(
					includer,
					tripRouter,
					new ModesChecker( configGroup.getAvailableModes() ),
					configGroup.getAvailableModes(),
					configGroup.getChainBasedModes(),
					MatsimRandom.getLocalInstance());

		return algo;
	}

	private static class ModesChecker implements PermissibleModesCalculator {
		private final Set<String> modes;

		public ModesChecker(final String[] modes) {
			this.modes = new TreeSet<String>( Arrays.asList( modes ) );
		}

		@Override
		public Collection<String> getPermissibleModes(final Plan plan) {
			List<String> available = new ArrayList<String>( modes );

			Person person = plan.getPerson();
			if (person instanceof PersonImpl &&
					"never".equals( ((PersonImpl) person).getCarAvail() )) {
				available.remove( TransportMode.car );
				available.remove( ParkAndRideConstants.PARK_N_RIDE_LINK_MODE );
			}

			return available;
		}
	}
}

