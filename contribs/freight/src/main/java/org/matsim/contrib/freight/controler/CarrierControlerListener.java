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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.ScoringListener;

import javax.annotation.Nullable;
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
 *
 * // not sure if this _should_ be public, but current LSP design makes this necessary.  kai, sep'20
 */

public class CarrierControlerListener implements ScoringListener, ReplanningListener {
	// not sure if this _should_ be public, but current LSP design makes this necessary.
	// It is done analogue to CarrierAgentTracker. kmt oct'22


	private static final Logger log = LogManager.getLogger( CarrierControlerListener.class ) ;

	private final CarrierStrategyManager strategyManager;
	private final CarrierAgentTracker carrierAgentTracker;

	@Inject Scenario scenario;

	/**
	 * Constructs a controller with a set of carriers, re-planning capabilities and scoring-functions.
	 */
	@Inject CarrierControlerListener( @Nullable CarrierStrategyManager strategyManager, CarrierAgentTracker carrierAgentTracker ) {
		// The current default is bind( CarrierStrategyManager.class ).toProvider( () -> null );
		this.strategyManager = strategyManager;
		this.carrierAgentTracker = carrierAgentTracker;
	}

	@Override public void notifyScoring(ScoringEvent event) {
		carrierAgentTracker.scoreSelectedPlans();
		// (could also make CarrierAgentTracker directly a ScoringListener.  Not sure what is the better design: current design separates
		// ControlerListener and EventHandler functionality.  Other design would make AgentTracker more self-contained.  kai, jul'22)
	}

	@Override public void notifyReplanning(final ReplanningEvent event) {
		if ( strategyManager==null ) {
			throw new RuntimeException( "You need to set CarrierStrategyManager to something meaningful to run iterations." );
		}
		strategyManager.run( FreightUtils.getCarriers( scenario ).getCarriers().values() , event.getIteration(), event.getReplanningContext() );
	}

}
