/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ${file_name}
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) ${year} by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 *
 * ${filecomment}
 * ${package_declaration}
 *
 * ${typecomment}
 * ${type_declaration}
 */

package org.matsim.contrib.freight.controler;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlanReader;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.mobsim.CarrierAgentTracker;
import org.matsim.contrib.freight.mobsim.PickupAndDeliveryConsoleWriter;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategy;
import org.matsim.contrib.freight.replanning.PlanStrategyManager;
import org.matsim.contrib.freight.replanning.ReRouteVehicles;
import org.matsim.contrib.freight.replanning.ReScheduleVehicles;
import org.matsim.contrib.freight.vrp.NetworkTransportCosts;
import org.matsim.contrib.freight.vrp.PDTWSolverFactory;
import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.util.LeastCostPathCalculator;


public class RunMobSimWithCarrier implements StartupListener, BeforeMobsimListener, AfterMobsimListener, ScoringListener, ReplanningListener {

    private static Logger logger = Logger.getLogger(RunMobSimWithCarrier.class);

    private CarrierAgentTracker carrierAgentTracker;
    private String carrierFilename;
    private Carriers carriers;

    public RunMobSimWithCarrier(String carrierFilename) {
        this.carrierFilename = carrierFilename;
    }

    public void notifyStartup(StartupEvent event) {
        carriers = new Carriers();
        new CarrierPlanReader(carriers).read(carrierFilename);
	}
	
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		Controler controler = event.getControler();
		carrierAgentTracker = new CarrierAgentTracker(carriers, event.getControler().getNetwork());
		carrierAgentTracker.getEventsManager().addHandler(new PickupAndDeliveryConsoleWriter());
        City2000WQSimFactory mobsimFactory = new City2000WQSimFactory(carrierAgentTracker);	
		event.getControler().setMobsimFactory(mobsimFactory);
		controler.getEvents().addHandler(carrierAgentTracker);
        carrierAgentTracker.createPlans();
	}

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event) {
        Controler controler = event.getControler();
        controler.getEvents().removeHandler(carrierAgentTracker);
    }

    @Override
	public void notifyScoring(ScoringEvent event) {
		carrierAgentTracker.calculateCosts();
	}

	@Override
	public void notifyReplanning(final ReplanningEvent event) {
		CarrierPlanStrategy planStrat = new CarrierPlanStrategy();
		
		final LeastCostPathCalculator router = event.getControler().getLeastCostPathCalculatorFactory().createPathCalculator(event.getControler().getScenario().getNetwork(), event.getControler().createTravelCostCalculator(), event.getControler().getTravelTimeCalculator());
		
		Costs costs = new NetworkTransportCosts(router,event.getControler().getNetwork(), event.getControler().getConfig().travelTimeCalculator().getTraveltimeBinSize());
		
		ReScheduleVehicles vehicleReRouter = new ReScheduleVehicles(event.getControler().getNetwork(), costs, new PDTWSolverFactory());
		vehicleReRouter.setRouter(router);
		planStrat.addModule(vehicleReRouter);
		planStrat.addModule(new ReRouteVehicles(router, event.getControler().getNetwork()));
		
		PlanStrategyManager<Carrier> stratManager = new PlanStrategyManager<Carrier>();
		stratManager.addStrategy(planStrat, 1.0);
		
		for(Carrier carrier : carriers.getCarriers().values()){
			stratManager.nextStrategy().run(carrier);
		}
		
	}

}
