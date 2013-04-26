/* *********************************************************************** *
 * project: org.matsim.*
 * MyControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 * 
 */

package playground.ikaddoura.optimization;

import org.matsim.core.api.experimental.events.EventsManager;

import org.matsim.core.controler.events.StartupEvent;

import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.ScenarioImpl;

import playground.ikaddoura.optimization.externalDelayEffects.InVehicleDelayHandler;
import playground.ikaddoura.optimization.externalDelayEffects.WaitingDelayHandler;
import playground.ikaddoura.optimization.handler.ConstantFareHandler;
import playground.ikaddoura.optimization.handler.MarginalCostPricingHandler;
import playground.ikaddoura.optimization.handler.PtLegHandler;

/**
 * @author Ihab
 *
 */

public class OptControlerListener implements StartupListener {

	private final double fare;
	private final PtLegHandler ptScoringHandler;
	private final ScenarioImpl scenario;
	private final boolean calculate_inVehicleTimeDelayEffects;
	private final boolean calculate_waitingTimeDelayEffects;
	private final boolean marginalCostPricing;

	public OptControlerListener(double fare, PtLegHandler ptLegHandler, ScenarioImpl scenario, boolean calculate_inVehicleTimeDelayEffects, boolean calculate_waitingTimeDelayEffects, boolean marginalCostPricing){
		this.fare = fare;
		this.ptScoringHandler = ptLegHandler;
		this.scenario = scenario;
		this.calculate_inVehicleTimeDelayEffects = calculate_inVehicleTimeDelayEffects;
		this.calculate_waitingTimeDelayEffects = calculate_waitingTimeDelayEffects;
		this.marginalCostPricing = marginalCostPricing;
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		EventsManager eventsManager = event.getControler().getEvents();

		if (this.calculate_inVehicleTimeDelayEffects) {
			event.getControler().getEvents().addHandler(new InVehicleDelayHandler(eventsManager, scenario));
		}
		
		if (this.calculate_waitingTimeDelayEffects) {
			event.getControler().getEvents().addHandler(new WaitingDelayHandler(eventsManager, scenario));
		}
		
		if (this.marginalCostPricing) {
			event.getControler().getEvents().addHandler(new MarginalCostPricingHandler(eventsManager, scenario));
		}
		
		event.getControler().getEvents().addHandler(new ConstantFareHandler(eventsManager, this.fare));
		event.getControler().getEvents().addHandler(ptScoringHandler);
	}

}
