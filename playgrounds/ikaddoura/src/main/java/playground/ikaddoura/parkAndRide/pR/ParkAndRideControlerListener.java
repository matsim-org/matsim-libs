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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.pt.replanning.TransitActsRemoverStrategy;

import playground.ikaddoura.parkAndRide.pRstrategy.PlanStrategyImpl_pr;
import playground.ikaddoura.parkAndRide.pRstrategy.PlanStrategyImpl_work;
import playground.ikaddoura.parkAndRide.pRstrategy.PrWeight;
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
	private Map<Id, ParkAndRideFacility> id2prFacility = new HashMap<Id, ParkAndRideFacility>();
	private Map<Id, List<PrWeight>> personId2prWeights = new HashMap<Id, List<PrWeight>>();
	private double addRemoveProb;
	private int addRemoveDisable;
	private double changeLocationProb;
	private int changeLocationDisable;
	private double timeAllocationProb;
	private int timeAllocationDisable;
	
	ParkAndRideControlerListener(Controler ctl, AdaptiveCapacityControl adaptiveControl, Map<Id, ParkAndRideFacility> id2prFacility, double addRemoveProb, int addRemoveDisable, double changeLocationProb, int changeLocationDisable, double timeAllocationProb, int timeAllocationDisable) {
		this.controler = ctl;
		this.adaptiveControl = adaptiveControl;
		this.id2prFacility = id2prFacility;
		this.addRemoveProb = addRemoveProb;
		this.addRemoveDisable = addRemoveDisable;
		this.changeLocationProb = changeLocationProb;
		this.changeLocationDisable = changeLocationDisable;
		this.timeAllocationProb = timeAllocationProb;
		this.timeAllocationDisable = timeAllocationDisable;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		event.getControler().getEvents().addHandler(adaptiveControl);
		
		PlanStrategy strategyAddRemove = new PlanStrategyImpl_work(new RandomPlanSelector());
		strategyAddRemove.addStrategyModule(new TransitActsRemoverStrategy(controler.getConfig()));
		strategyAddRemove.addStrategyModule(new ParkAndRideAddRemoveStrategy(controler, id2prFacility, personId2prWeights)); // only if car is available: P+R added (if plan doesn't contain P+R) or P+R removed (if plan contains P+R)
		strategyAddRemove.addStrategyModule(new ReRoute(controler));
		
		PlanStrategy strategyChangeLocation = new PlanStrategyImpl_pr(new RandomPlanSelector());
		strategyChangeLocation.addStrategyModule(new TransitActsRemoverStrategy(controler.getConfig()));
		strategyChangeLocation.addStrategyModule(new ParkAndRideChangeLocationStrategy(controler, id2prFacility, personId2prWeights)); // if plan contains P+R: change to other P+R location
		strategyChangeLocation.addStrategyModule(new ReRoute(controler));
		
		PlanStrategy strategyTimeAllocation = new PlanStrategyImpl_work(new RandomPlanSelector());
		strategyTimeAllocation.addStrategyModule(new ParkAndRideTimeAllocationMutator(controler.getConfig())); // TimeAllocation, not changing "parkAndRide" and "pt interaction"
		strategyTimeAllocation.addStrategyModule(new ReRoute(controler));

		StrategyManager manager = this.controler.getStrategyManager() ;
	
		manager.addStrategy(strategyAddRemove, this.addRemoveProb);
		manager.addChangeRequest(this.addRemoveDisable, strategyAddRemove, 0.);
				
		manager.addStrategy(strategyChangeLocation, this.changeLocationProb);
		manager.addChangeRequest(this.changeLocationDisable, strategyChangeLocation, 0.);
		
		manager.addStrategy(strategyTimeAllocation, this.timeAllocationProb);
		manager.addChangeRequest(this.timeAllocationDisable, strategyTimeAllocation, 0.);
		
	}

}
