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
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.PermissibleModesCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;

import playground.thibautd.parknride.mutationapproach.HeuristicParkAndRideIncluder;
import playground.thibautd.parknride.routingapproach.ParkAndRideRoutingModule;
import playground.thibautd.parknride.routingapproach.ParkAndRideTravelTimeCost;
import playground.thibautd.parknride.routingapproach.ParkAndRideUtils;
import playground.thibautd.parknride.routingapproach.RoutingParkAndRideIncluder;
import playground.thibautd.parknride.scoring.ParkAndRideScoringFunctionFactory;

/**
 * @author thibautd
 */
public class ParkAndRideModule extends AbstractMultithreadedModule {
	private final Controler controler;

	public ParkAndRideModule(final Controler controler) {
		super( controler.getConfig().global() );
		this.controler = controler;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		TripRouterFactory tripRouterFactory = controler.getTripRouterFactory();
		TripRouter tripRouter = tripRouterFactory.createTripRouter();
		ParkAndRideFacilities facilities = ParkAndRideUtils.getParkAndRideFacilities( controler.getScenario() );
		ParkAndRideIncluder includer;

		ParkAndRideConfigGroup configGroup = ParkAndRideUtils.getConfigGroup( controler.getConfig() );
		switch (configGroup.getInsertionStrategy()) {
			case Routing:
				includer = createRoutingIncluder(tripRouterFactory , tripRouter);
				break;
			case Random:
				includer = new HeuristicParkAndRideIncluder(
						facilities,
						tripRouter);
				break;
			default:
				throw new RuntimeException();
		}

		Random rand = MatsimRandom.getLocalInstance();
		ParkAndRideChooseModeForSubtour algo =
			new ParkAndRideChooseModeForSubtour(
					includer,
					new FacilityChanger(
						rand,
						tripRouter,
						((ParkAndRideScoringFunctionFactory) controler.getScoringFunctionFactory()).getPenaltyFactory(),
						facilities,
						configGroup.getLocalSearchRadius(),
						configGroup.getPriceOfDistance()),
					tripRouter,
					new ModesChecker( configGroup.getAvailableModes() ),
					configGroup.getAvailableModes(),
					configGroup.getChainBasedModes(),
					configGroup.getFacilityChangeProbability(),
					rand);

		return algo;
	}

	private ParkAndRideIncluder createRoutingIncluder(
			final TripRouterFactory tripRouterFactory,
			final TripRouter tripRouter) {
		TransitRouterConfig transitConfig =
			new TransitRouterConfig(
					controler.getConfig().planCalcScore(),
					controler.getConfig().plansCalcRoute(),
					controler.getConfig().transitRouter(),
					controler.getConfig().vspExperimental());
		ParkAndRideTravelTimeCost timeCost =
			new ParkAndRideTravelTimeCost(
					transitConfig,
					controler.getConfig().planCalcScore());

		TravelTime carTime = controler.getTravelTimeCalculator();
		TravelDisutility carCost =
			controler.getTravelDisutilityFactory().createTravelDisutility(
					carTime, controler.getConfig().planCalcScore() );
		TransitRouterNetworkTravelTimeAndDisutility ptTimeCost =
					new TransitRouterNetworkTravelTimeAndDisutility( transitConfig );

		ParkAndRideRoutingModule routingModule =
			new ParkAndRideRoutingModule(
					((PopulationFactoryImpl) controler.getPopulation().getFactory()).getModeRouteFactory(),
					controler.getPopulation().getFactory(),
					controler.getNetwork(),
					controler.getScenario().getTransitSchedule(),
					transitConfig.beelineWalkConnectionDistance,
					transitConfig.searchRadius,
					ParkAndRideUtils.getParkAndRideFacilities( controler.getScenario() ),
					transitConfig,
					carCost,
					carTime,
					ptTimeCost,
					timeCost,
					timeCost);
		return new RoutingParkAndRideIncluder(
				ParkAndRideUtils.getParkAndRideFacilities( controler.getScenario() ),
				routingModule,
				tripRouter);
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

