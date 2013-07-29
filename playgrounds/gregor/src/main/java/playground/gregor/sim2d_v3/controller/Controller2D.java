/* *********************************************************************** *
 * project: org.matsim.*
 * Controller2D.java
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

package playground.gregor.sim2d_v3.controller;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.contrib.multimodal.router.util.WalkTravelTimeFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.core.router.old.LegRouter;
import org.matsim.core.router.old.PlanRouterAdapter;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.signalsystems.controler.DefaultSignalsControllerListenerFactory;

import playground.gregor.sim2d_v3.events.XYDataWriter;
import playground.gregor.sim2d_v3.router.Walk2DLegRouter;
import playground.gregor.sim2d_v3.scenario.ScenarioLoader2DImpl;
import playground.gregor.sim2d_v3.simulation.HybridQ2DMobsimFactory;
import playground.gregor.sim2d_v3.trafficmonitoring.MSATravelTimeCalculatorFactory;

@Deprecated // should not be derived from Controler
public final class Controller2D extends Controler implements StartupListener {

	public Controller2D(String[] args) {
		super(args[0]);
		setOverwriteFiles(true);
//		this.config.addQSimConfigGroup(new QSimConfigGroup());
//		this.config.getQSimConfigGroup().setEndTime( 9*3600 + 5* 60);
		setTravelTimeCalculatorFactory(new MSATravelTimeCalculatorFactory());
		this.addMobsimFactory("hybridQ2D", new HybridQ2DMobsimFactory());
		throw new RuntimeException(Gbl.CREATE_ROUTING_ALGORITHM_WARNING_MESSAGE) ;
	}

	public Controller2D(Scenario sc) {
		super(sc);
		setOverwriteFiles(true);
		setTravelTimeCalculatorFactory(new MSATravelTimeCalculatorFactory());
		HybridQ2DMobsimFactory factory = new HybridQ2DMobsimFactory();
		this.addMobsimFactory("hybridQ2D",factory);
		if (this.config.scenario().isUseSignalSystems()) {
//			this.setSignalsControllerListenerFactory(new Sim2DSignalsControllerListenerFactory(factory));
//			this.setSignalsControllerListenerFactory(new DefaultSignalsControllerListenerFactory());
			
			HybridSignalsControllerListenerFactory signalsFactory = new HybridSignalsControllerListenerFactory();
			signalsFactory.addSignalsControllerListenerFactory(new Sim2DSignalsControllerListenerFactory(factory));
			signalsFactory.addSignalsControllerListenerFactory(new DefaultSignalsControllerListenerFactory());
			this.setSignalsControllerListenerFactory(signalsFactory);
		}
		
		this.addCoreControlerListener(this);
		throw new RuntimeException(Gbl.CREATE_ROUTING_ALGORITHM_WARNING_MESSAGE) ;
	}

	@Override
	protected void loadData() {
		super.loadData();
		ScenarioLoader2DImpl loader = new ScenarioLoader2DImpl(this.scenarioData);
		loader.load2DScenario();
	}

//	@Override
//	public PlanAlgorithm createRoutingAlgorithm() {
//		return createRoutingAlgorithm(
//				this.createTravelCostCalculator(),
//				this.getLinkTravelTimes());
//	}

	public PlanAlgorithm createRoutingAlgorithm(TravelDisutility travelCosts, TravelTime travelTimes) {
		
		PlanRouterAdapter plansCalcRoute = new PlanRouterAdapter( this );
		
		TravelTime travelTime = new WalkTravelTimeFactory(config.plansCalcRoute()).createTravelTime();
		LegRouter walk2DLegRouter = new Walk2DLegRouter(network, travelTime, (IntermodalLeastCostPathCalculator) plansCalcRoute.getLeastCostPathCalculator());
		plansCalcRoute.addLegHandler("walk2d", walk2DLegRouter);
		
		return plansCalcRoute;
	}

	public static void main(String[] args) {

		String configFile = args[0];
		Config c = ConfigUtils.loadConfig(configFile);
//		c.addQSimConfigGroup(new QSimConfigGroup());
//		c.getQSimConfigGroup().setEndTime(  24*3600);

		Scenario sc = ScenarioUtils.createScenario(c);
		((PopulationFactoryImpl)sc.getPopulation().getFactory()).setRouteFactory("walk2d", new LinkNetworkRouteFactory());
		((PopulationFactoryImpl)sc.getPopulation().getFactory()).setRouteFactory(TransportMode.walk, new LinkNetworkRouteFactory());

        // TODO: Refactored out of core config
        // Please just create and add the config group instead.
        MultiModalConfigGroup multiModalConfigGroup1 = (MultiModalConfigGroup) c.getModule(MultiModalConfigGroup.GROUP_NAME);
        if (multiModalConfigGroup1 == null) {
            multiModalConfigGroup1 = new MultiModalConfigGroup();
            c.addModule(multiModalConfigGroup1);
        }
        if (multiModalConfigGroup1.isMultiModalSimulationEnabled()) {
            // TODO: Refactored out of core config
            // Please just create and add the config group instead.
            MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) c.getModule(MultiModalConfigGroup.GROUP_NAME);
            if (multiModalConfigGroup == null) {
                multiModalConfigGroup = new MultiModalConfigGroup();
                c.addModule(multiModalConfigGroup);
            }
            for (String transportMode : CollectionUtils.stringToArray(multiModalConfigGroup.getSimulatedModes())) {
				((PopulationFactoryImpl)sc.getPopulation().getFactory()).setRouteFactory(transportMode, new LinkNetworkRouteFactory());
			}	
		}
				
		ScenarioUtils.loadScenario(sc);

		Controler controller = new Controller2D(sc);
		controller.run();
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		
		XYDataWriter xyDataWriter = new XYDataWriter();
		this.getEvents().addHandler(xyDataWriter);
		this.addControlerListener(xyDataWriter);
		this.getMobsimListeners().add(xyDataWriter);
	}

}