/* *********************************************************************** *
 * project: org.matsim.*
 * MyControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.ikaddoura.parkAndRide.pR;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.pt.replanning.TransitActsRemoverStrategy;

import playground.ikaddoura.parkAndRide.pRstrategy.ParkAndRideAddRemoveStrategy;
import playground.ikaddoura.parkAndRide.pRstrategy.ParkAndRideChangeLocationStrategy;
import playground.ikaddoura.parkAndRide.pRstrategy.ParkAndRideTimeAllocationMutator;

/**
 * @author Ihab
 *
 */
public class ParkAndRideControlerListener implements StartupListener {
	
	Controler controler;
	AdaptiveCapacityControl adaptiveControl;
	private List<ParkAndRideFacility> prFacilities = new ArrayList<ParkAndRideFacility>();
	
	ParkAndRideControlerListener(Controler ctl, AdaptiveCapacityControl adaptiveControl, List<ParkAndRideFacility> prFacilities) {
		this.controler = ctl;
		this.adaptiveControl = adaptiveControl;
		this.prFacilities = prFacilities;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		
		event.getControler().getEvents().addHandler(adaptiveControl);
		
		PlanStrategy strategyAddRemove = new PlanStrategyImpl(new RandomPlanSelector());
		strategyAddRemove.addStrategyModule(new TransitActsRemoverStrategy(controler.getConfig()));
		strategyAddRemove.addStrategyModule(new ParkAndRideAddRemoveStrategy(controler, prFacilities)); // only if car is available: P+R added (if plan doesn't contain P+R) or P+R removed (if plan contains P+R)
		strategyAddRemove.addStrategyModule(new ReRoute(controler));
		
		PlanStrategy strategyChangeLocation = new PlanStrategyImpl(new RandomPlanSelector());
		strategyChangeLocation.addStrategyModule(new TransitActsRemoverStrategy(controler.getConfig()));
		strategyChangeLocation.addStrategyModule(new ParkAndRideChangeLocationStrategy(controler, prFacilities)); // if plan contains P+R: change to other P+R location
		strategyChangeLocation.addStrategyModule(new ReRoute(controler));
		
		PlanStrategy strategyTimeAllocation = new PlanStrategyImpl(new RandomPlanSelector());
		strategyTimeAllocation.addStrategyModule(new ParkAndRideTimeAllocationMutator(controler.getConfig())); // TimeAllocation, not changing "parkAndRide" and "pt interaction"
		strategyTimeAllocation.addStrategyModule(new ReRoute(controler));

		StrategyManager manager = this.controler.getStrategyManager() ;
	
		manager.addStrategy(strategyAddRemove, 0.1);
		manager.addChangeRequest(90, strategyAddRemove, 0);
				
		manager.addStrategy(strategyChangeLocation, 0.1);
		manager.addChangeRequest(90, strategyChangeLocation, 0);
		
		manager.addStrategy(strategyTimeAllocation, 0.1);
		manager.addChangeRequest(90, strategyTimeAllocation, 0);

	}

}
