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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.replanning.GenericStrategyManagerImpl;

import javax.inject.Inject;

/**
 * Controls the workflow of the simulation.
 * <p></p>
 * <p>Processes the required actions during the matsim simulation workflow (replanning, scoring, sim). For example, it informs agents to
 * score their plans when it is scoring time, and it informs them to re-plan, or it injects carriers into the simulation when it is time
 * to inject them. Currently it is kept to minimum functions, i.e. injecting carrier plans into sim and the possibility
 * to set custom scoring- and replanning-functionalities.
 *
 * @author sschroeder, mzilske
 */

class CarrierControlerListener implements BeforeMobsimListener, AfterMobsimListener, ScoringListener, ReplanningListener {
	private static final Logger log = Logger.getLogger( CarrierControlerListener.class ) ;

	private final CarrierScoringFunctionFactory carrierScoringFunctionFactory;

	private final CarrierStrategyManager strategyManager;

	private CarrierAgentTracker carrierAgentTracker;

	@Inject EventsManager eventsManager;
	@Inject Scenario scenario;

	/**
	 * Constructs a controller with a set of carriers, re-planning capabilities and scoring-functions.
	 */
	@Inject CarrierControlerListener( CarrierStrategyManager strategyManagerFactory, CarrierScoringFunctionFactory scoringFunctionFactory ) {
//		log.warn( "calling ctor; scoringFunctionFactory=" + scoringFunctionFactory.getClass() );
		this.strategyManager = strategyManagerFactory;
		this.carrierScoringFunctionFactory = scoringFunctionFactory;
	}

	@Override public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		carrierAgentTracker = new CarrierAgentTracker(FreightUtils.getCarriers(scenario), carrierScoringFunctionFactory, eventsManager );
		eventsManager.addHandler(carrierAgentTracker);
		// (add and remove per mobsim run)
	}

	@Override public void notifyAfterMobsim(AfterMobsimEvent event) {
		eventsManager.removeHandler(carrierAgentTracker);
	}

	@Override public void notifyScoring(ScoringEvent event) {
		carrierAgentTracker.scoreSelectedPlans();
	}

	public CarrierAgentTracker getCarrierAgentTracker() {
		return carrierAgentTracker;
	}

	@Override public void notifyReplanning(final ReplanningEvent event) {
//		if (carrierPlanStrategyManagerFactory == null) {
//			return;
//		}
//		GenericStrategyManagerImpl<CarrierPlan, Carrier> strategyManager = carrierPlanStrategyManagerFactory.createStrategyManager();
		strategyManager.run( FreightUtils.getCarriers( scenario ).getCarriers().values() , event.getIteration(), event.getReplanningContext() );
	}

}
