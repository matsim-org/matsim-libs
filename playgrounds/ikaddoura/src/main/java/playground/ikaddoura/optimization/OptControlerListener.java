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
import org.matsim.core.scenario.MutableScenario;

import playground.ikaddoura.internalizationPt.CapacityDelayHandler;
import playground.ikaddoura.internalizationPt.MarginalCostPricingPtHandler;
import playground.ikaddoura.internalizationPt.TransferDelayInVehicleHandler;
import playground.ikaddoura.internalizationPt.TransferDelayWaitingHandler;
import playground.ikaddoura.optimization.users.ConstantFareHandler;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.MarginalCongestionPricingHandler;

/**
 * @author Ihab
 *
 */

public class OptControlerListener implements StartupListener {

	private final double fare;
	private final MutableScenario scenario;
	private final boolean calculate_inVehicleTimeDelayEffects;
	private final boolean calculate_waitingTimeDelayEffects;
	private final boolean marginalCostPricingPt;
	private final boolean marginalCostPricingCar;
	private final boolean calculate_carCongestionEffects;
	private final boolean calculate_capacityDelayEffects;

	public OptControlerListener(double fare,
			MutableScenario scenario,
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
		
		EventsManager eventsManager = event.getServices().getEvents();

		// pt mode
		if (this.calculate_inVehicleTimeDelayEffects) {
			event.getServices().getEvents().addHandler(new TransferDelayInVehicleHandler(eventsManager, scenario));
		}
		if (this.calculate_waitingTimeDelayEffects) {
			event.getServices().getEvents().addHandler(new TransferDelayWaitingHandler(eventsManager, scenario));
		}
		if (this.calculate_capacityDelayEffects) {
			event.getServices().getEvents().addHandler(new CapacityDelayHandler(eventsManager, scenario));
		}
		if (this.marginalCostPricingPt) {
			event.getServices().getEvents().addHandler(new MarginalCostPricingPtHandler(eventsManager, scenario));
		}
		
		// car mode
		if (this.calculate_carCongestionEffects) {
			event.getServices().getEvents().addHandler(new CongestionHandlerImplV3(eventsManager, scenario));
		}
		if (this.marginalCostPricingCar) {
			event.getServices().getEvents().addHandler(new MarginalCongestionPricingHandler(eventsManager, scenario));
		}
				
		event.getServices().getEvents().addHandler(new ConstantFareHandler(eventsManager, this.fare));
	}

}
