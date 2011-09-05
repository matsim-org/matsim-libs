/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.andreas.P2.pbox;

import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import playground.andreas.P2.plan.PPlan;
import playground.andreas.P2.plan.PRouteProvider;
import playground.andreas.P2.replanning.PPlanStrategy;
import playground.andreas.P2.replanning.PStrategyManager;
import playground.andreas.P2.replanning.RemoveAllVehiclesButOne;
import playground.andreas.P2.scoring.ScoreContainer;

/**
 * Manages one paratransit line
 * 
 * @author aneumann
 *
 */
public class Cooperative {
	
	private final static Logger log = Logger.getLogger(Cooperative.class);
	
	private final Id id;
	
	private PFranchise franchise;
	private final double costPerVehicle;

	private PPlan bestPlan;
	private PPlan testPlan;

	private TransitLine currentTransitLine;
	
	private double budget;
	private double lastBudget;

	public Cooperative(Id id, double costPerVehicle, PFranchise franchise){
		this.id = id;
		this.costPerVehicle = costPerVehicle;
		this.franchise = franchise;
	}

	public void init(PRouteProvider pRouteProvider) {
		this.budget = 0.0;
		
		PPlan plan;
		PPlanStrategy strategy = new RemoveAllVehiclesButOne(null);
		
		do {
			plan = new PPlan(new IdImpl("0"), pRouteProvider.getRandomTransitStop(), pRouteProvider.getRandomTransitStop(), 0.0, 24.0 * 3600); 
			while(plan.getStartStop() == plan.getEndStop()){
				plan.setEndStop(pRouteProvider.getRandomTransitStop());
			}
			plan = strategy.modifyPlan(plan, this.id, pRouteProvider);				
		} while (this.franchise.planRejected(plan));
	
		this.bestPlan = null;
		this.testPlan = plan;
		this.currentTransitLine = pRouteProvider.createEmptyLine(id);
		for (TransitRoute route : this.testPlan.getLine().getRoutes().values()) {
			this.currentTransitLine.addRoute(route);
		}		
	}

	public void score(TreeMap<Id, ScoreContainer> driverId2ScoreMap) {
		
		this.lastBudget = budget;
		
		if(this.bestPlan != null){
			scorePlan(driverId2ScoreMap, this.bestPlan);
			this.budget += this.bestPlan.getScore();
			for (TransitRoute route : this.bestPlan.getLine().getRoutes().values()) {
				route.setDescription(this.bestPlan.toString(this.budget));
			}
		}
		scorePlan(driverId2ScoreMap, this.testPlan);
		for (TransitRoute route : this.testPlan.getLine().getRoutes().values()) {
			route.setDescription(this.testPlan.toString(this.budget));
		}

	}

	public void replan(PRouteProvider pRouteProvider, PStrategyManager pStrategyManager) {	
		
		if(this.budget <= 0 && this.bestPlan != null){
			// decrease number of vehicles
			
			int numberOfVehiclesToSell = -1 * Math.min(-1, (int) (this.budget / this.costPerVehicle));
			
			if(this.bestPlan.getVehicleIds().size() - numberOfVehiclesToSell < 1){
				log.info("Best Plan set to null - restarting line " + this.id);
				this.bestPlan = null;
				this.budget = 0.0;
			} else {
				PPlan plan = new PPlan(this.bestPlan.getId(), this.bestPlan.getStartStop(), this.bestPlan.getEndStop(), this.bestPlan.getStartTime(), this.bestPlan.getEndTime());
				plan.setScore(this.bestPlan.getScore());
				plan.setLine(pRouteProvider.createTransitLine(this.id, plan.getStartTime(), plan.getEndTime(), this.bestPlan.getVehicleIds().size() - numberOfVehiclesToSell, plan.getStartStop(), plan.getEndStop(), this.bestPlan.getId()));
				
				this.budget += this.costPerVehicle * numberOfVehiclesToSell;
				log.info("Sold " + numberOfVehiclesToSell + " from line " + this.id);
				this.bestPlan = plan;
			}			
		}
		
		if(this.bestPlan == null){
			this.bestPlan = this.testPlan;
			this.testPlan = null;
		} else if (this.testPlan.getScorePerVehicle() > this.bestPlan.getScorePerVehicle()){
			// apply modification to bestPlan
			PPlan plan = new PPlan(this.bestPlan.getId(), this.testPlan.getStartStop(), this.testPlan.getEndStop(), this.testPlan.getStartTime(), this.testPlan.getEndTime());
			plan.setScore(this.bestPlan.getScore());
			
			if(this.bestPlan.isSameButVehSize(this.testPlan)){
				if(this.budget > this.costPerVehicle){
					plan.setLine(pRouteProvider.createTransitLine(this.id, plan.getStartTime(), plan.getEndTime(), this.bestPlan.getVehicleIds().size() + 1, plan.getStartStop(), plan.getEndStop(), this.bestPlan.getId()));
					this.budget -= this.costPerVehicle;
					this.bestPlan = plan;
					this.testPlan = null;
				} 
			} else if(this.bestPlan.isSameButOperationTime(this.testPlan)){
				plan.setLine(pRouteProvider.createTransitLine(this.id, plan.getStartTime(), plan.getEndTime(), this.bestPlan.getVehicleIds().size(), plan.getStartStop(), plan.getEndStop(), this.bestPlan.getId()));
				this.bestPlan = plan;
				this.testPlan = null;
			}			
		}
		
		// dumb simple replanning
		if(bestPlan.getScore() > 0){
			PPlanStrategy strategy = pStrategyManager.chooseStrategy();
			this.testPlan = strategy.modifyPlan(this.bestPlan, this.id, pRouteProvider);
		} else {
			// create complete new route
			PPlan plan;
			PPlanStrategy strategy = new RemoveAllVehiclesButOne(null);
			
			do {
				plan = new PPlan(new IdImpl(pRouteProvider.getIteration()), pRouteProvider.getRandomTransitStop(), pRouteProvider.getRandomTransitStop(), 0.0, 24.0 * 3600); 
				while(plan.getStartStop() == plan.getEndStop()){
					plan.setEndStop(pRouteProvider.getRandomTransitStop());
				}
				plan = strategy.modifyPlan(plan, this.id, pRouteProvider);				
			} while (this.franchise.planRejected(plan));
			
			this.testPlan = plan;
		}
		
		this.currentTransitLine = pRouteProvider.createEmptyLine(id);
		if(this.bestPlan != null){
			for (TransitRoute route : this.bestPlan.getLine().getRoutes().values()) {
				this.currentTransitLine.addRoute(route);
			}
		}
		for (TransitRoute route : this.testPlan.getLine().getRoutes().values()) {
			this.currentTransitLine.addRoute(route);
		}
	}
	
	public TransitLine getCurrentTransitLine() {		
		return this.currentTransitLine;		
	}

	public List<PPlan> getAllPlans(){
		List<PPlan> plans = new LinkedList<PPlan>();
		if(this.bestPlan != null){
			plans.add(this.bestPlan);
		}
		if(this.testPlan != null){
			plans.add(this.testPlan);
		}		
		return plans;
	}
	
	public double getBudget(){
		return this.budget;
	}

	private void scorePlan(TreeMap<Id, ScoreContainer> driverId2ScoreMap, PPlan plan) {
		double totalLineScore = 0.0;
		int totalTripsServed = 0;
		
		for (Id vehId : plan.getVehicleIds()) {
			totalLineScore += driverId2ScoreMap.get(vehId).getTotalRevenue();
			totalTripsServed += driverId2ScoreMap.get(vehId).getTripsServed();
		}
		
		plan.setScore(totalLineScore);
		plan.setTripsServed(totalTripsServed);
	}
}