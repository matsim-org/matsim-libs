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
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.TripTimeAllocationMutator;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.pt.replanning.TransitActsRemoverStrategy;

import playground.ikaddoura.parkAndRide.pRstrategy.ParkAndRideAddStrategy;
import playground.ikaddoura.parkAndRide.pRstrategy.ParkAndRideChangeLocationStrategy;
import playground.ikaddoura.parkAndRide.pRstrategy.ParkAndRideRemoveStrategy;
import playground.ikaddoura.parkAndRide.pRstrategy.ParkAndRideTimeAllocationMutator;
import playground.ikaddoura.parkAndRide.pRstrategy.PlanStrategyImpl_parkAndRide;
import playground.ikaddoura.parkAndRide.pRstrategy.PlanStrategyImpl_work;
import playground.ikaddoura.parkAndRide.pRstrategy.PlanStrategyImpl_workNoPRseq;
import playground.ikaddoura.parkAndRide.pRstrategy.PrWeight;
//import playground.ikaddoura.parkAndRide.pRstrategy.ParkAndRideRemoveStrategy;

/**
 * @author Ihab
 *
 */
public class ParkAndRideControlerListener implements StartupListener {
	
	Controler controler;
	AdaptiveCapacityControl adaptiveControl;
	private Map<Id, ParkAndRideFacility> id2prFacility = new HashMap<Id, ParkAndRideFacility>();
	private Map<Id, List<PrWeight>> personId2prWeights = new HashMap<Id, List<PrWeight>>();
	
	private double addPRProb = 0.;
	private int addPRDisable = 0;
	private double changeLocationProb = 0.;
	private int changeLocationDisable = 0;
	private double removePRProb = 0.;
	private int removePRDisable = 0;
	private double timeAllocationProb = 0.;
	private int timeAllocationDisable = 0;
	private double addPRtimeAllocationProb = 0.;
	private int addPRtimeAllocationDisable = 0;
	private double reRouteProb = 0.;
	private int reRouteDisable = 0;

	private int gravity;
	
	ParkAndRideControlerListener(Controler ctl, AdaptiveCapacityControl adaptiveControl, Map<Id, ParkAndRideFacility> id2prFacility, int gravity) {
		this.controler = ctl;
		this.adaptiveControl = adaptiveControl;
		this.id2prFacility = id2prFacility;
		this.gravity = gravity;
	}

	public void setAddPRProb(double addPRProb) {
		this.addPRProb = addPRProb;
	}

	public void setAddPRDisable(int addPRDisable) {
		this.addPRDisable = addPRDisable;
	}

	public void setChangeLocationProb(double changeLocationProb) {
		this.changeLocationProb = changeLocationProb;
	}

	public void setChangeLocationDisable(int changeLocationDisable) {
		this.changeLocationDisable = changeLocationDisable;
	}

	public void setRemovePRProb(double removePRProb) {
		this.removePRProb = removePRProb;
	}

	public void setRemovePRDisable(int removePRDisable) {
		this.removePRDisable = removePRDisable;
	}

	public void setTimeAllocationProb(double timeAllocationProb) {
		this.timeAllocationProb = timeAllocationProb;
	}

	public void setTimeAllocationDisable(int timeAllocationDisable) {
		this.timeAllocationDisable = timeAllocationDisable;
	}
	
	public void setAddPRtimeAllocationProb(double addPRtimeAllocationProb) {
		this.addPRtimeAllocationProb = addPRtimeAllocationProb;
	}

	public void setAddPRtimeAllocationDisable(int addPRtimeAllocationDisable) {
		this.addPRtimeAllocationDisable = addPRtimeAllocationDisable;
	}

	public void setReRouteProb(double reRouteProb) {
		this.reRouteProb = reRouteProb;
	}

	public void setReRouteDisable(int reRouteDisable) {
		this.reRouteDisable = reRouteDisable;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		event.getControler().getEvents().addHandler(adaptiveControl);
		
		PlanStrategyImpl_workNoPRseq strategyAddPR = new PlanStrategyImpl_workNoPRseq(new RandomPlanSelector<Plan, Person>()); // only for plans with at least one home-work-home sequence without park-and-ride
		strategyAddPR.addStrategyModule(new TransitActsRemoverStrategy(controler.getConfig()));
		strategyAddPR.addStrategyModule(new ParkAndRideAddStrategy(controler, id2prFacility, personId2prWeights, gravity)); // adds P+R to a randomly chosen home-work-home sequence
		strategyAddPR.addStrategyModule(new ReRoute(controler.getScenario()));
		
		PlanStrategyImpl_parkAndRide strategyChangeLocation = new PlanStrategyImpl_parkAndRide(new RandomPlanSelector<Plan, Person>()); // only for plans with at least one home-work-home sequence with park-and-ride
		strategyChangeLocation.addStrategyModule(new TransitActsRemoverStrategy(controler.getConfig()));
		strategyChangeLocation.addStrategyModule(new ParkAndRideChangeLocationStrategy(controler, id2prFacility, personId2prWeights, gravity)); // change the P+R location of a randomly chosen home-work-home sequence
		strategyChangeLocation.addStrategyModule(new ReRoute(controler.getScenario()));
		
		PlanStrategyImpl_parkAndRide strategyTimeAllocation = new PlanStrategyImpl_parkAndRide(new RandomPlanSelector<Plan, Person>()); // only for plans with at least one home-work-home sequence with park-and-ride
		strategyTimeAllocation.addStrategyModule(new ParkAndRideTimeAllocationMutator(controler.getConfig())); // TimeAllocation, not changing "parkAndRide" and "pt interaction"
		strategyTimeAllocation.addStrategyModule(new ReRoute(controler.getScenario()));
		
		PlanStrategyImpl_workNoPRseq strategyRemovePR = new PlanStrategyImpl_workNoPRseq(new RandomPlanSelector<Plan, Person>()); // only for plans with at least one home-work-home sequence without park-and-ride
		strategyRemovePR.addStrategyModule(new TransitActsRemoverStrategy(controler.getConfig()));
		strategyRemovePR.addStrategyModule(new ParkAndRideRemoveStrategy(controler)); // removes P+R from a randomly chosen home-work-home sequence
		strategyRemovePR.addStrategyModule(new ReRoute(controler.getScenario()));
		
		PlanStrategyImpl_workNoPRseq strategyAddPRTimeAllocation = new PlanStrategyImpl_workNoPRseq(new RandomPlanSelector<Plan, Person>()); // only for plans with at least one home-work-home sequence without park-and-ride
		strategyAddPRTimeAllocation.addStrategyModule(new TripTimeAllocationMutator(controler.getConfig()));
		strategyAddPRTimeAllocation.addStrategyModule(new TransitActsRemoverStrategy(controler.getConfig()));
		strategyAddPRTimeAllocation.addStrategyModule(new ParkAndRideAddStrategy(controler, id2prFacility, personId2prWeights, gravity)); // adds a P+R to a randomly chosen home-work-home sequence
		strategyAddPRTimeAllocation.addStrategyModule(new ReRoute(controler.getScenario()));
		
		PlanStrategyImpl_work strategyReRoute = new PlanStrategyImpl_work(new RandomPlanSelector<Plan, Person>()); // only for plans with home and work activities (with and without park-and-ride)
		strategyReRoute.addStrategyModule(new ReRoute(controler.getScenario()));
		
		StrategyManager manager = this.controler.getStrategyManager() ;
	
		manager.addStrategyForDefaultSubpopulation(strategyAddPR, this.addPRProb);
		manager.addChangeRequestForDefaultSubpopulation(this.addPRDisable, strategyAddPR, 0.);
		
		manager.addStrategyForDefaultSubpopulation(strategyChangeLocation, this.changeLocationProb);
		manager.addChangeRequestForDefaultSubpopulation(this.changeLocationDisable, strategyChangeLocation, 0.);
		
		manager.addStrategyForDefaultSubpopulation(strategyTimeAllocation, this.timeAllocationProb);
		manager.addChangeRequestForDefaultSubpopulation(this.timeAllocationDisable, strategyTimeAllocation, 0.);
		
		manager.addStrategyForDefaultSubpopulation(strategyRemovePR, this.removePRProb);
		manager.addChangeRequestForDefaultSubpopulation(this.removePRDisable, strategyRemovePR, 0.);
		
		manager.addStrategyForDefaultSubpopulation(strategyAddPRTimeAllocation, this.addPRtimeAllocationProb);
		manager.addChangeRequestForDefaultSubpopulation(this.addPRtimeAllocationDisable, strategyAddPRTimeAllocation, 0.);
		
		manager.addStrategyForDefaultSubpopulation(strategyReRoute, this.reRouteProb);
		manager.addChangeRequestForDefaultSubpopulation(this.reRouteDisable, strategyReRoute, 0.);
		
	}

}
