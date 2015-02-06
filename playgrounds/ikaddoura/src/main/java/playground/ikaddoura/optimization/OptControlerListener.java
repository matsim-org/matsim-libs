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

import playground.ikaddoura.internalizationPt.CapacityDelayHandler;
import playground.ikaddoura.internalizationPt.MarginalCostPricingPtHandler;
import playground.ikaddoura.internalizationPt.TransferDelayInVehicleHandler;
import playground.ikaddoura.internalizationPt.TransferDelayWaitingHandler;
import playground.ikaddoura.optimization.users.ConstantFareHandler;
import playground.vsp.congestion.handlers.MarginalCongestionHandlerImplV3;
import playground.vsp.congestion.handlers.MarginalCostPricingCarHandler;

/**
 * @author Ihab
 *
 */

public class OptControlerListener implements StartupListener {

	private final double fare;
	private final ScenarioImpl scenario;
	private final boolean calculate_inVehicleTimeDelayEffects;
	private final boolean calculate_waitingTimeDelayEffects;
	private final boolean marginalCostPricingPt;
	private final boolean marginalCostPricingCar;
	private final boolean calculate_carCongestionEffects;
	private final boolean calculate_capacityDelayEffects;

	public OptControlerListener(double fare,
			ScenarioImpl scenario,
			boolean calculate_inVehicleTimeDelayEffects,
			boolean calculate_waitingTimeDelayEffects, 
			boolean calculate_capacityDelayEffects,

			boolean marginalCostPricingPt,
			boolean calculate_carCongestionEffects,
			boolean marginalCostPricingCar){
		
		this.fare = fare;
		this.scenario = scenario;
		this.calculate_inVehicleTimeDelayEffects = calculate_inVehicleTimeDelayEffects;
		this.calculate_waitingTimeDelayEffects = calculate_waitingTimeDelayEffects;
		this.calculate_capacityDelayEffects = calculate_capacityDelayEffects;
		this.marginalCostPricingPt = marginalCostPricingPt;
		this.calculate_carCongestionEffects = calculate_carCongestionEffects;
		this.marginalCostPricingCar = marginalCostPricingCar;
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		EventsManager eventsManager = event.getControler().getEvents();

		// pt mode
		if (this.calculate_inVehicleTimeDelayEffects) {
			event.getControler().getEvents().addHandler(new TransferDelayInVehicleHandler(eventsManager, scenario));
		}
		if (this.calculate_waitingTimeDelayEffects) {
			event.getControler().getEvents().addHandler(new TransferDelayWaitingHandler(eventsManager, scenario));
		}
		if (this.calculate_capacityDelayEffects) {
			event.getControler().getEvents().addHandler(new CapacityDelayHandler(eventsManager, scenario));
		}
		if (this.marginalCostPricingPt) {
			event.getControler().getEvents().addHandler(new MarginalCostPricingPtHandler(eventsManager, scenario));
		}
		
		// car mode
		if (this.calculate_carCongestionEffects) {
			event.getControler().getEvents().addHandler(new MarginalCongestionHandlerImplV3(eventsManager, scenario));
		}
		if (this.marginalCostPricingCar) {
			event.getControler().getEvents().addHandler(new MarginalCostPricingCarHandler(eventsManager, scenario));
		}
				
		event.getControler().getEvents().addHandler(new ConstantFareHandler(eventsManager, this.fare));
	}

}
