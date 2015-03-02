/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.wagonSim.pt.router;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TransitRouterWrapper;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.old.LegRouterWrapper;
import org.matsim.core.router.old.TeleportationLegRouter;
import org.matsim.pt.PtConstants;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author droeder
 *
 */
public final class WagonSimTripRouterFactoryImpl implements TripRouterFactory {

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(WagonSimTripRouterFactoryImpl.class);
//	private TripRouterFactoryImpl delegate;
	private RoutingModule walkRouter;
	private TransitSchedule transitSchedule;
	private TransitRouterFactory routerFactory;
	private Map<Id<TransitStopFacility>, Double> minShuntingTimes;
	private Network network;

	public WagonSimTripRouterFactoryImpl(
			final Scenario scenario,
//			final TravelDisutilityFactory disutilityFactory,
//			final TravelTime travelTime,
//			final LeastCostPathCalculatorFactory leastCostAlgoFactory,
			final TransitRouterFactory transitRouterFactory,
			Map<Id<TransitStopFacility>, Double> minShuntingTimes) {
//		this.delegate = new TripRouterFactoryImpl(scenario, disutilityFactory, travelTime, leastCostAlgoFactory, transitRouterFactory);
		this.routerFactory = transitRouterFactory;
		this.transitSchedule = scenario.getTransitSchedule();
		this.network = scenario.getNetwork();
		this.walkRouter = createWalkRouter(scenario.getPopulation().getFactory(), scenario.getConfig().plansCalcRoute());
		this.minShuntingTimes = minShuntingTimes;
	}
	
	private RoutingModule createWalkRouter(PopulationFactory populationFactory, PlansCalcRouteConfigGroup routeConfigGroup){
		return LegRouterWrapper.createLegRouterWrapper(TransportMode.transit_walk, populationFactory, new TeleportationLegRouter(
				((PopulationFactoryImpl) populationFactory).getModeRouteFactory(),
				routeConfigGroup.getTeleportedModeSpeeds().get( TransportMode.walk ),
				routeConfigGroup.getModeRoutingParams().get( TransportMode.walk ).getBeelineDistanceFactor()));
	}

	@Override
	public TripRouter instantiateAndConfigureTripRouter(RoutingContext routingContext) {
//		TripRouter tripRouter = this.delegate.instantiateAndConfigureTripRouter();
		TripRouter tripRouter = new TripRouter();
		tripRouter.setRoutingModule(TransportMode.pt, 
				new WagonSimRouterWrapper(routerFactory.createTransitRouter(), transitSchedule, network, walkRouter, minShuntingTimes));
		return tripRouter;
	}
	
	private static class WagonSimRouterWrapper implements RoutingModule{

		private TransitRouterWrapper delegate;
		private Map<Id<TransitStopFacility>, Double> minShuntingTimes;

		public WagonSimRouterWrapper(
				final TransitRouter router,
				final TransitSchedule transitSchedule,
				final Network network,
				final RoutingModule walkRouter,
				Map<Id<TransitStopFacility>, Double> minShuntingTimes) {
			this.delegate = new TransitRouterWrapper(router, transitSchedule, network, walkRouter);
			this.minShuntingTimes = minShuntingTimes;
		}
		
		@Override
		public List<? extends PlanElement> calcRoute(Facility fromFacility,
				Facility toFacility, double departureTime, Person person) {
			List<? extends PlanElement> pe = this.delegate.calcRoute(fromFacility, toFacility, departureTime, person);
			return duplicateStageActivities(pe);
		}

		/**
		 * @param pe
		 * @return
		 */
		private List<? extends PlanElement> duplicateStageActivities(List<? extends PlanElement> oldElements) {
			List<PlanElement> pe = new ArrayList<PlanElement>();
			for(int i = 1; i < oldElements.size() ; i += 2){
				Leg l = (Leg) oldElements.get(i-1);
				Activity a = (Activity) oldElements.get(i);
				if(l.getMode().equals(TransportMode.transit_walk) && a.getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)){
					// the initial walk, do not change
					pe.add(l);
					pe.add(a);
				}else{
					pe.add(l); // that's a pt leg
					
					// get the minimum shunting time of the stop facility
					Id<TransitStopFacility> stopFacId = ((ExperimentalTransitRoute)l.getRoute()).getEgressStopId();
					Double minShuntingTime = minShuntingTimes.get(stopFacId);
					if (minShuntingTime == null) {  throw new RuntimeException("stationId="+stopFacId+" does not have a minimum shunting time defined. Bailing out."); } 
					
					if(i<oldElements.size()-2){
						// handle all stages except the last one
						pe.add(a); // insert an additional stage act
						Leg ll = new LegImpl(TransportMode.transit_walk);
						Route r = new GenericRouteImpl(a.getLinkId(), a.getLinkId());
						r.setTravelTime(minShuntingTime);
						ll.setRoute(r);
						ll.setTravelTime(minShuntingTime);
						pe.add(ll); // insert an additional walk leg
					}
					pe.add(a); // that's the original stage act
				}
			}
			return pe;
		}
		@Override
		public StageActivityTypes getStageActivityTypes() {
			return this.delegate.getStageActivityTypes();
		}
		
	}

}

