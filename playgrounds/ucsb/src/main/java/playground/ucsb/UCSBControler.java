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

//package playground.ucsb;
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
//import org.matsim.core.router.old.PlansCalcRoute;
//import org.matsim.core.router.util.LeastCostPathCalculator;
//import org.matsim.core.router.util.TravelDisutility;
//import org.matsim.core.router.util.TravelTime;
//import org.matsim.population.algorithms.PlanAlgorithm;
//import org.matsim.pt.router.PlansCalcTransitRoute;
//
//import playground.ucsb.analysis.VolumeCounterControlerListener;
//import playground.ucsb.network.algorithms.SCAGShp2Links;
//
//public final class UCSBControler extends Controler {
//
//	private final static Logger log = Logger.getLogger(UCSBControler.class);
//
//	private boolean isFirstCall = true;
//	private final Set<String> hovModes;
//
//	public UCSBControler(final String configFilename) {
//		super(configFilename);
//
//		this.hovModes = new HashSet<String>(5);
//		this.hovModes.add(SCAGShp2Links.HOV);
//		throw new RuntimeException(Gbl.CREATE_ROUTING_ALGORITHM_WARNING_MESSAGE) ;
//	}
//
//	public UCSBControler(final Config config) {
//		super(config);
//
//		this.hovModes = new HashSet<String>(5);
//		this.hovModes.add(SCAGShp2Links.HOV);
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
//	public PlanAlgorithm createRoutingAlgorithm(TravelDisutility travelCosts, final TravelTime travelTimes) {
//		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) (this.population.getFactory())).getModeRouteFactory();
//
//		if (this.isFirstCall) {
//			routeFactory.setRouteFactory(SCAGShp2Links.HOV, new LinkNetworkRouteFactory());
//			this.isFirstCall = false;
//		}
//		
////		PlansCalcTransitRoute router = new PlansCalcTransitRoute(
////				this.config.plansCalcRoute(),
////				this.network,
////				travelCosts,
////				travelTimes,
////				this.getLeastCostPathCalculatorFactory(),
////				routeFactory,
////				this.config.transit(),
////				getTransitRouterFactory().createTransitRouter(),
////				this.scenarioData.getTransitSchedule());
//		PlansCalcRoute router = new PlansCalcRoute(
//				this.config.plansCalcRoute(),
//				this.network,
//				travelCosts,
//				travelTimes,
//				this.getLeastCostPathCalculatorFactory(),
//				routeFactory);
//		
//		LeastCostPathCalculator routeAlgo = this.getLeastCostPathCalculatorFactory().createPathCalculator(this.network, travelCosts, travelTimes);
//		if (routeAlgo instanceof IntermodalLeastCostPathCalculator) {
//			((IntermodalLeastCostPathCalculator) routeAlgo).setModeRestriction(this.hovModes);
//		} else {
//			log.warn("hov-router cannot be restricted to hov-links only!");
//		}
//		router.addLegHandler(SCAGShp2Links.HOV, new NetworkLegRouter(this.network, routeAlgo, routeFactory));
//
//		return router;
//	}
//
//	public static void main(final String[] args) {
//		String configFile = args[0] ;
//		UCSBControler controler = new UCSBControler( configFile ) ;
//		controler.addControlerListener(new VolumeCounterControlerListener());
//		controler.run();
//	}
//}
