// /* *********************************************************************** *
//  * project: org.matsim.*
//  * HerbieParkAndRideRoutingModule.java
//  *                                                                         *
//  * *********************************************************************** *
//  *                                                                         *
//  * copyright       : (C) 2012 by the members listed in the COPYING,        *
//  *                   LICENSE and WARRANTY file.                            *
//  * email           : info at matsim dot org                                *
//  *                                                                         *
//  * *********************************************************************** *
//  *                                                                         *
//  *   This program is free software; you can redistribute it and/or modify  *
//  *   it under the terms of the GNU General Public License as published by  *
//  *   the Free Software Foundation; either version 2 of the License, or     *
//  *   (at your option) any later version.                                   *
//  *   See also COPYING, LICENSE and WARRANTY file                           *
//  *                                                                         *
//  * *********************************************************************** */
// package playground.thibautd.parknride.herbiespecific;
// 
// import herbie.running.scoring.TravelScoringFunction;
// 
// import org.matsim.api.core.v01.network.Network;
// import org.matsim.api.core.v01.population.PopulationFactory;
// import org.matsim.core.api.experimental.facilities.Facility;
// import org.matsim.core.population.routes.ModeRouteFactory;
// import org.matsim.core.router.util.PersonalizableTravelTime;
// import org.matsim.core.router.util.TravelDisutility;
// import org.matsim.core.utils.geometry.CoordUtils;
// import org.matsim.pt.router.TransitRouterConfig;
// import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
// import org.matsim.pt.transitSchedule.api.TransitSchedule;
// 
// import playground.thibautd.parknride.ParkAndRideFacilities;
// import playground.thibautd.parknride.routingapproach.ParkAndRideRoutingModule;
// 
// /**
//  * Modifies the definition of the walk cost.
//  *
//  * @author thibautd
//  */
// public class HerbieParkAndRideRoutingModule extends ParkAndRideRoutingModule {
// 	private final TravelScoringFunction travelScoring;
// 
// 	public HerbieParkAndRideRoutingModule(
// 			final TravelScoringFunction travelScoring,
// 			final ModeRouteFactory routeFactory,
// 			final PopulationFactory populationFactory,
// 			final Network carNetwork,
// 			final TransitSchedule schedule,
// 			final double maxBeelineWalkConnectionDistance,
// 			final double pnrConnectionDistance,
// 			final ParkAndRideFacilities parkAndRideFacilities,
// 			final TransitRouterConfig transitRouterConfig,
// 			final TravelDisutility carCost,
// 			final PersonalizableTravelTime carTime,
// 			final TransitRouterNetworkTravelTimeAndDisutility ptTimeCost,
// 			final TravelDisutility pnrCost,
// 			final PersonalizableTravelTime pnrTime) {
// 		super(routeFactory, populationFactory, carNetwork, schedule,
// 				maxBeelineWalkConnectionDistance, pnrConnectionDistance,
// 				parkAndRideFacilities, transitRouterConfig, carCost, carTime,
// 				ptTimeCost, pnrCost, pnrTime);
// 		this.travelScoring = travelScoring;
// 	}
// 
// 	@Override
// 	protected double calcDirectWalkCost(
// 			final Facility fromFacility,
// 			final Facility toFacility) {
// 		double distance =
// 			CoordUtils.calcDistance(
// 					fromFacility.getCoord(),
// 					toFacility.getCoord() );
// 		double time = distance / transitRouterConfig.getBeelineWalkSpeed();
// 
// 		return travelScoring.getWalkScore( distance , time );
// 	}
// }
// 
