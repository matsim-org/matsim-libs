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

//package playground.mmoyo.zz_archive.TransitSimulation;
//
//import org.matsim.contrib.otfvis.OTFVis;
//import org.matsim.core.config.Config;
//import org.matsim.core.controler.Controler;
//import org.matsim.core.gbl.Gbl;
//import org.matsim.core.mobsim.qsim.QSim;
//import org.matsim.core.mobsim.qsim.QSimFactory;
//import org.matsim.core.population.PopulationFactoryImpl;
//import org.matsim.core.router.util.TravelDisutility;
//import org.matsim.core.router.util.TravelTime;
//import org.matsim.core.scenario.ScenarioImpl;
//import org.matsim.core.scenario.ScenarioLoaderImpl;
//import org.matsim.population.algorithms.PlanAlgorithm;
//import org.matsim.pt.config.TransitConfigGroup;
//import org.matsim.vis.otfvis.OTFClientLive;
//import org.matsim.vis.otfvis.OnTheFlyServer;
//
//public final class MMoyoTransitControler extends Controler {
//	boolean launchOTFDemo=false;
//	private Config config;
//	
//	public MMoyoTransitControler(final ScenarioImpl scenario, boolean launchOTFDemo){
//		super(scenario);
//		this.config = scenario.getConfig();
//		this.setOverwriteFiles(true);   
//		this.launchOTFDemo = launchOTFDemo;
//		throw new RuntimeException(Gbl.CREATE_ROUTING_ALGORITHM_WARNING_MESSAGE) ;
//	}
//	
//	@Override
//	protected void runMobSim() {
//		QSim sim = (QSim) new QSimFactory().createMobsim(this.scenarioData, this.events);
//		
//		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(this.scenarioData.getConfig(), this.scenarioData, events, sim);
//		OTFClientLive.run(this.scenarioData.getConfig(), server);
//
//		sim.run();
//		/*
//		TransitQueueSimulation sim = new TransitQueueSimulation(this.scenarioData, this.events);
//		sim.startOTFServer("livesim");
//		new OnTheFlyClientQuad("rmi:127.0.0.1:4019:" + "livesim").start();
//		sim.run();
//		*/
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
//		return new MMoyoPlansCalcTransitRoute(this.config.plansCalcRoute(), this.network, travelCosts, travelTimes,
//				this.getLeastCostPathCalculatorFactory(), ((PopulationFactoryImpl) this.population.getFactory()).getModeRouteFactory(), this.scenarioData.getTransitSchedule(), new TransitConfigGroup());
//	}
//	
//	public static void main(final String[] args) {
//		if (args.length > 0) {
//			ScenarioLoaderImpl scenarioLoader = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(args[0]); //load from configFile
//			ScenarioImpl scenario = (ScenarioImpl) scenarioLoader.getScenario();
//			scenarioLoader.loadScenario();
//			new MMoyoTransitControler(scenario, true).run();
//		} 
//	}
//	
//}
