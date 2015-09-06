/* *********************************************************************** *
 * project: org.matsim.*
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

//package playground.mrieser;
//
//import java.util.HashSet;
//import java.util.Set;
//
//import org.apache.log4j.Logger;
//import org.matsim.core.config.Config;
//import org.matsim.core.controler.Controler;
//import org.matsim.core.gbl.Gbl;
//import org.matsim.core.population.PopulationFactoryImpl;
//import org.matsim.core.population.routes.LinkNetworkRouteFactory;
//import org.matsim.core.population.routes.ModeRouteFactory;
//import org.matsim.core.router.IntermodalLeastCostPathCalculator;
//import playground.johannes.utils.NetworkLegRouter;
//import org.matsim.core.router.util.LeastCostPathCalculator;
//import org.matsim.core.router.util.TravelDisutility;
//import org.matsim.core.router.util.TravelTime;
//import org.matsim.population.algorithms.PlanAlgorithm;
//import org.matsim.pt.router.PlansCalcTransitRoute;
//
///**
// * @author mrieser / senozon
// */
//public final class HovControler extends Controler {
//
//	private final static Logger log = Logger.getLogger(HovControler.class);
//
//	private boolean isFirstCall = true;
//	private final Set<String> hovModes;
//
//	public HovControler(final String configFilename) {
//		super(configFilename);
//
//		this.hovModes = new HashSet<String>(5);
//		this.hovModes.add("hov");
//		throw new RuntimeException(Gbl.CREATE_ROUTING_ALGORITHM_WARNING_MESSAGE) ;
//	}
//
//	public HovControler(final Config config) {
//		super(config);
//
//		this.hovModes = new HashSet<String>(5);
//		this.hovModes.add("hov");
//		throw new RuntimeException(Gbl.CREATE_ROUTING_ALGORITHM_WARNING_MESSAGE) ;
//	}
//
////	@Override
////	public PlanAlgorithm createRoutingAlgorithm() {
////		return createRoutingAlgorithm(
////				this.createTravelCostCalculator(),
////				this.getLinkTravelTimes());
////	}
//
//	public PlanAlgorithm createRoutingAlgorithm(final TravelDisutility travelCosts, final TravelTime travelTimes) {
//		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) (this.population.getFactory())).getModeRouteFactory();
//
//		if (this.isFirstCall) {
//			routeFactory.setRouteFactory("hov", new LinkNetworkRouteFactory());
//			this.isFirstCall = false;
//		}
//
//		PlansCalcTransitRoute router = new PlansCalcTransitRoute(this.config.plansCalcRoute(), this.network, travelCosts,
//				travelTimes, this.getLeastCostPathCalculatorFactory(),routeFactory, this.config.transit(),
//				getTransitRouterFactory().createTransitRouter(), this.scenarioData.getTransitSchedule());
//
//		LeastCostPathCalculator routeAlgo = this.getLeastCostPathCalculatorFactory().createPathCalculator(this.network, travelCosts, travelTimes);
//		if (routeAlgo instanceof IntermodalLeastCostPathCalculator) {
//			((IntermodalLeastCostPathCalculator) routeAlgo).setModeRestriction(this.hovModes);
//		} else {
//			log.warn("hov-router cannot be restricted to hov-links only!");
//		}
//		router.addLegHandler("hov", new NetworkLegRouter(this.network, routeAlgo, routeFactory));
//
//
//		return router;
//	}
//
//}
