/*
 *  *********************************************************************** *
// *  * project: org.matsim.*
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

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierConfig;
import org.matsim.contrib.freight.carrier.CarrierPlanReader;
import org.matsim.contrib.freight.carrier.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.carrier.CarrierPlanWriter;
import org.matsim.contrib.freight.carrier.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.mobsim.CarrierAgentTracker;
import org.matsim.contrib.freight.mobsim.FreightQSimFactory;
import org.matsim.contrib.freight.replanning.CarrierReplanningStrategyManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;

public class CarrierControler implements StartupListener, ShutdownListener,
		BeforeMobsimListener, AfterMobsimListener, ScoringListener,
		ReplanningListener, IterationEndsListener {

	
	private static Logger logger = Logger.getLogger(CarrierControler.class);

	private final String carrierFilename;

	private CarrierScoringFunctionFactory carrierScoringFunctionFactory;
	
	private CarrierPlanStrategyManagerFactory carrierPlanStrategyManagerFactory;
	
	private CarrierAgentTracker carrierAgentTracker;
	
	private Carriers carriers;
	
	private CarrierConfig carrierConfig;

	public CarrierControler(CarrierConfig carrierConfig) {
		this.carrierFilename = carrierConfig.plans().getInputFile();
		this.carrierConfig = carrierConfig;
	}

	public Map<Id,Carrier> getCarriers() {
		return carriers.getCarriers();
	}

	public CarrierScoringFunctionFactory getCarrierScoringFunctionFactory() {
		return carrierScoringFunctionFactory;
	}

	public void setCarrierScoringFunctionFactory(
			CarrierScoringFunctionFactory carrierScoringFunctionFactory) {
		this.carrierScoringFunctionFactory = carrierScoringFunctionFactory;
	}

	public CarrierPlanStrategyManagerFactory getCarrierPlanStrategyManagerFactory() {
		return carrierPlanStrategyManagerFactory;
	}

	public void setCarrierPlanStrategyManagerFactory(CarrierPlanStrategyManagerFactory carrierPlanStrategyManagerFactory) {
		this.carrierPlanStrategyManagerFactory = carrierPlanStrategyManagerFactory;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		carriers = new Carriers();
		new CarrierPlanReader(carriers).read(carrierFilename);
		assert carrierScoringFunctionFactory != null : "carrierScoringFunctionFactory must be set";
//		assert carrierPlanStrategyManagerFactory != null : "strategyManagerFactory must be set";
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		Controler controler = event.getControler();
		carrierAgentTracker = new CarrierAgentTracker(carriers, event.getControler().getNetwork(), carrierScoringFunctionFactory);
		FreightQSimFactory mobsimFactory = new FreightQSimFactory(carrierAgentTracker);
		mobsimFactory.setWithinDayActivityReScheduling(carrierConfig.isWithinDayReScheduling());
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
		carrierAgentTracker.scoreSelectedPlans();
	}

	@Override
	public void notifyReplanning(final ReplanningEvent event) {
		if(carrierPlanStrategyManagerFactory == null){
			return;
		}
		CarrierReplanningStrategyManager strategyManager = carrierPlanStrategyManagerFactory.createStrategyManager(event.getControler());

		for (Carrier carrier : carriers.getCarriers().values()) {
			if (carrier.getSelectedPlan() == null) {
				logger.warn("carrier cannot replan since no selected plan is available");
				continue;
			}
			strategyManager.nextStrategy().run(carrier);
		}

	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		String dir = event.getControler().getControlerIO().getIterationPath(event.getIteration());
		new CarrierPlanWriter(carriers.getCarriers().values()).write(dir + "/" + event.getIteration() + ".carrierPlans.xml");
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {

	}

}
