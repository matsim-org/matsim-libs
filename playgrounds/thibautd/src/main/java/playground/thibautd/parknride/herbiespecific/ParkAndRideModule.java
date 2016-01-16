///* *********************************************************************** *
// * project: org.matsim.*
// * ParkAndRideChooseModeForSubtourModule.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2012 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//package playground.thibautd.parknride.herbiespecific;
//
//import herbie.running.config.HerbieConfigGroup;
//import herbie.running.scoring.TravelScoringFunction;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.List;
//import java.util.Set;
//import java.util.TreeSet;
//
//import org.matsim.api.core.v01.TransportMode;
//import org.matsim.api.core.v01.population.Person;
//import org.matsim.api.core.v01.population.Plan;
//import org.matsim.core.gbl.MatsimRandom;
//import org.matsim.core.population.PersonImpl;
//import org.matsim.core.population.PopulationFactoryImpl;
//import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
//import org.matsim.core.router.util.PersonalizableTravelTime;
//import org.matsim.core.router.util.TravelDisutility;
//import org.matsim.core.scoring.CharyparNagelScoringParameters;
//import org.matsim.population.algorithms.PermissibleModesCalculator;
//import org.matsim.population.algorithms.PlanAlgorithm;
//import org.matsim.pt.router.TransitRouterConfig;
//import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
//
//import playground.thibautd.parknride.ParkAndRideChooseModeForSubtour;
//import playground.thibautd.parknride.ParkAndRideConfigGroup;
//import playground.thibautd.parknride.ParkAndRideConstants;
//import playground.thibautd.parknride.routingapproach.RoutingParkAndRideIncluder;
//import playground.thibautd.parknride.routingapproach.ParkAndRideRoutingModule;
//import playground.thibautd.parknride.routingapproach.ParkAndRideUtils;
//import playground.thibautd.router.TripRouter;
//import playground.thibautd.router.TripRouterFactory;
//import playground.thibautd.router.controler.MultiLegRoutingControler;
//
///**
// * An ugly copy-pasted module, to change the cost for pnr change using the strange herbie walk cost
// * @author thibautd
// */
//public class ParkAndRideChooseModeForSubtourModule extends AbstractMultithreadedModule {
//	private final MultiLegRoutingControler controler;
//
//	public ParkAndRideChooseModeForSubtourModule(final MultiLegRoutingControler controler) {
//		super( controler.getConfig().global() );
//		this.services = services;
//	}
//
//	@Override
//	public PlanAlgorithm getPlanAlgoInstance() {
//		TripRouterFactory tripRouterFactory = controler.getTripRouterFactory();
//		TripRouter tripRouter = tripRouterFactory.createTripRouter();
//		TransitRouterConfig transitConfig =
//			new TransitRouterConfig(
//					controler.getConfig().planCalcScore(),
//					controler.getConfig().plansCalcRoute(),
//					controler.getConfig().transitRouter(),
//					controler.getConfig().vspExperimental());
//		TravelScoringFunction travelScoring = 
//			new TravelScoringFunction(
//				new CharyparNagelScoringParameters( controler.getConfig().planCalcScore() ),
//				(HerbieConfigGroup) controler.getConfig().getModule( HerbieConfigGroup.GROUP_NAME ) );
//	
//		HerbieParkAndRideCost timeCost =
//			new HerbieParkAndRideCost(
//					transitConfig,
//					travelScoring);
//	
//		PersonalizableTravelTime carTime = tripRouterFactory.getTravelTimeCalculatorFactory().createTravelTime();
//		TravelDisutility carCost =
//			tripRouterFactory.getTravelCostCalculatorFactory().createTravelDisutility(
//					carTime, controler.getConfig().planCalcScore() );
//		TransitRouterNetworkTravelTimeAndDisutility ptTimeCost =
//					new TransitRouterNetworkTravelTimeAndDisutility( transitConfig );
//
//		ParkAndRideRoutingModule routingModule =
//			new HerbieParkAndRideRoutingModule(
//					travelScoring,
//					((PopulationFactoryImpl) controler.getPopulation().getFactory()).getModeRouteFactory(),
//					controler.getPopulation().getFactory(),
//					controler.getNetwork(),
//					controler.getScenario().getTransitSchedule(),
//					transitConfig.beelineWalkConnectionDistance,
//					transitConfig.searchRadius,
//					ParkAndRideUtils.getParkAndRideFacilities( controler.getScenario() ),
//					transitConfig,
//					carCost,
//					carTime,
//					ptTimeCost,
//					timeCost,
//					timeCost);
//
//		RoutingParkAndRideIncluder includer =
//			new RoutingParkAndRideIncluder(
//					ParkAndRideUtils.getParkAndRideFacilities( controler.getScenario() ),
//					routingModule,
//					tripRouter);
//
//		ParkAndRideConfigGroup configGroup = ParkAndRideUtils.getConfigGroup( controler.getConfig() );
//		ParkAndRideChooseModeForSubtour algo =
//			new ParkAndRideChooseModeForSubtour(
//					includer,
//					tripRouter,
//					new ModesChecker( configGroup.getAvailableModes() ),
//					configGroup.getAvailableModes(),
//					configGroup.getChainBasedModes(),
//					MatsimRandom.getLocalInstance());
//
//		return algo;
//	}
//
//	private static class ModesChecker implements PermissibleModesCalculator {
//		private final Set<String> modes;
//
//		public ModesChecker(final String[] modes) {
//			this.modes = new TreeSet<String>( Arrays.asList( modes ) );
//		}
//
//		@Override
//		public Collection<String> getPermissibleModes(final Plan plan) {
//			List<String> available = new ArrayList<String>( modes );
//
//			Person person = plan.getPerson();
//			if (person instanceof PersonImpl &&
//					"never".equals( ((PersonImpl) person).getCarAvail() )) {
//				available.remove( TransportMode.car );
//				available.remove( ParkAndRideConstants.PARK_N_RIDE_LINK_MODE );
//			}
//
//			return available;
//		}
//	}
//}
//
