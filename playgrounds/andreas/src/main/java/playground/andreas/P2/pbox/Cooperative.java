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
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

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

	public void init(SimpleCircleScheduleProvider simpleScheduleProvider) {
		this.budget = 0.0;
		
		PPlan plan;
		do {
			plan = new PPlan(new IdImpl("0"));
			plan.setStartStop(simpleScheduleProvider.getRandomTransitStop());
			plan.setEndStop(simpleScheduleProvider.getRandomTransitStop());
			while(plan.getStartStop() == plan.getEndStop()){
				plan.setEndStop(simpleScheduleProvider.getRandomTransitStop());
			}
			
			plan.setStartTime(0.0);
			plan.setEndTime(24.0 * 3600);
			plan.setLine(simpleScheduleProvider.createBackAndForthTransitLine(this.id, plan.getStartTime(), plan.getEndTime(), 1, plan.getStartStop(), plan.getEndStop(), null));
		} while (this.franchise.planRejected(plan));
		
		this.bestPlan = null;
		this.testPlan = plan;
		this.currentTransitLine = simpleScheduleProvider.createEmptyLine(id);
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

	public void replan(SimpleCircleScheduleProvider simpleScheduleProvider) {	
		
		if(this.budget <= 0 && this.bestPlan != null){
			// decrease number of vehicles
			
			int numberOfVehiclesToSell = -1 * Math.min(-1, (int) (this.budget / this.costPerVehicle));
			
			if(this.bestPlan.getVehicleIds().size() - numberOfVehiclesToSell < 1){
				log.info("Best Plan set to null - restarting line " + this.id);
				this.bestPlan = null;
				this.budget = 0.0;
			} else {
				PPlan plan = new PPlan(this.bestPlan.getId());
				plan.setScore(this.bestPlan.getScore());

				plan.setStartStop(this.bestPlan.getStartStop());
				plan.setEndStop(this.bestPlan.getEndStop());
				plan.setStartTime(this.bestPlan.getStartTime());
				plan.setEndTime(this.bestPlan.getEndTime());

				plan.setLine(simpleScheduleProvider.createBackAndForthTransitLine(this.id, plan.getStartTime(), plan.getEndTime(), this.bestPlan.getVehicleIds().size() - numberOfVehiclesToSell, plan.getStartStop(), plan.getEndStop(), this.bestPlan.getId()));
				
				this.budget += this.costPerVehicle * numberOfVehiclesToSell;
				log.info("Sold " + numberOfVehiclesToSell + " from line " + this.id);
				this.bestPlan = plan;
			}			
		}
		
		if(this.bestPlan == null){
			this.bestPlan = this.testPlan;
			this.testPlan = null;
//		} else if(this.testPlan.getScore() > this.bestPlan.getScore()){
//				this.bestPlan = this.testPlan;
//				this.testPlan = null;
		} else if (this.testPlan.getScorePerVehicle() > this.bestPlan.getScorePerVehicle()){
			// apply modification to bestPlan
			PPlan plan = new PPlan(this.bestPlan.getId());
			plan.setScore(this.bestPlan.getScore());
			
			plan.setStartStop(this.testPlan.getStartStop());
			plan.setEndStop(this.testPlan.getEndStop());
			plan.setStartTime(this.testPlan.getStartTime());
			plan.setEndTime(this.testPlan.getEndTime());
			
			if(this.bestPlan.isSameButVehSize(this.testPlan)){
				if(this.budget > this.costPerVehicle){
					plan.setLine(simpleScheduleProvider.createBackAndForthTransitLine(this.id, plan.getStartTime(), plan.getEndTime(), this.bestPlan.getVehicleIds().size() + 1, plan.getStartStop(), plan.getEndStop(), this.bestPlan.getId()));
					this.budget -= this.costPerVehicle;
					this.bestPlan = plan;
					this.testPlan = null;
				} 
			} else if(this.bestPlan.isSameButOperationTime(this.testPlan)){
				plan.setLine(simpleScheduleProvider.createBackAndForthTransitLine(this.id, plan.getStartTime(), plan.getEndTime(), this.bestPlan.getVehicleIds().size(), plan.getStartStop(), plan.getEndStop(), this.bestPlan.getId()));
				this.bestPlan = plan;
				this.testPlan = null;
			}			
		}
		
		// dumb simple replanning
		if(bestPlan.getScore() > 0){
			double rnd = MatsimRandom.getRandom().nextDouble();
			if( rnd < 0.33){
				// profitable route, increase vehicle fleet
				PPlan plan = new PPlan(new IdImpl(simpleScheduleProvider.getIteration()));
				plan.setStartStop(this.bestPlan.getStartStop());
				plan.setEndStop(this.bestPlan.getEndStop());
				plan.setStartTime(this.bestPlan.getStartTime());
				plan.setEndTime(this.bestPlan.getEndTime());
				plan.setLine(simpleScheduleProvider.createBackAndForthTransitLine(this.id, plan.getStartTime(), plan.getEndTime(), 1, plan.getStartStop(), plan.getEndStop(), null));
				this.testPlan = plan;
			} else if(rnd < 0.66){
				// profitable route, change startTime
				PPlan plan = new PPlan(new IdImpl(simpleScheduleProvider.getIteration()));
				plan.setStartStop(this.bestPlan.getStartStop());
				plan.setEndStop(this.bestPlan.getEndStop());
				
				// get a valid new start time
				double newStartTime = Math.max(0.0, this.bestPlan.getStartTime() + (-0.5 + MatsimRandom.getRandom().nextDouble()) * 6 * 3600);
				newStartTime = Math.min(newStartTime, this.bestPlan.getEndTime() - 1 * 3600);
				plan.setStartTime(newStartTime);
				
				plan.setEndTime(this.bestPlan.getEndTime());
				plan.setLine(simpleScheduleProvider.createBackAndForthTransitLine(this.id, plan.getStartTime(), plan.getEndTime(), 1, plan.getStartStop(), plan.getEndStop(), null));
				this.testPlan = plan;
			} else {
				// profitable route, change endTime
				PPlan plan = new PPlan(new IdImpl(simpleScheduleProvider.getIteration()));
				plan.setStartStop(this.bestPlan.getStartStop());
				plan.setEndStop(this.bestPlan.getEndStop());
				plan.setStartTime(this.bestPlan.getStartTime());
				
				// get a valid new end time
				double newEndTime = Math.min(24 * 3600.0, this.bestPlan.getEndTime() + (-0.5 + MatsimRandom.getRandom().nextDouble()) * 6 * 3600);
				newEndTime = Math.max(newEndTime, this.bestPlan.getStartTime() + 1 * 3600);
				plan.setEndTime(newEndTime);
				
				plan.setLine(simpleScheduleProvider.createBackAndForthTransitLine(this.id, plan.getStartTime(), plan.getEndTime(), 1, plan.getStartStop(), plan.getEndStop(), null));
				this.testPlan = plan;
			}			
			
		} else {
			
//			if(this.bestPlan.getVehicleIds().size() == 1){
//				// this plan has become non profitable
//				this.bestPlan = null;
////			} else if(this.bestPlan.getTripsServed() == 0){
//				// plan not used anymore - delete it
////				this.bestPlan = null;
//			} else {
//				// decrease number of vehicles
//				PPlan plan = new PPlan(this.bestPlan.getId());
//				plan.setScore(this.bestPlan.getScore());
//
//				plan.setStartStop(this.bestPlan.getStartStop());
//				plan.setEndStop(this.bestPlan.getEndStop());
//				plan.setStartTime(this.bestPlan.getStartTime());
//				plan.setEndTime(this.bestPlan.getEndTime());
//
//				plan.setLine(simpleScheduleProvider.createBackAndForthTransitLine(this.id, plan.getStartTime(), plan.getEndTime(), this.bestPlan.getVehicleIds().size() - 1, plan.getStartStop(), plan.getEndStop(), this.bestPlan.getId()));
//				
//				this.bestPlan = plan;
//			}
			
			// create complete new route
			PPlan plan;
			do {
				plan = new PPlan(new IdImpl(simpleScheduleProvider.getIteration()));
				plan.setStartStop(simpleScheduleProvider.getRandomTransitStop());
				plan.setEndStop(simpleScheduleProvider.getRandomTransitStop());
				while(plan.getStartStop() == plan.getEndStop()){
					plan.setEndStop(simpleScheduleProvider.getRandomTransitStop());
				}
				plan.setStartTime(0.0);
				plan.setEndTime(24.0 * 3600);
				plan.setLine(simpleScheduleProvider.createBackAndForthTransitLine(this.id, plan.getStartTime(), plan.getEndTime(), 1, plan.getStartStop(), plan.getEndStop(), null));
			} while (this.franchise.planRejected(plan));
			
			this.testPlan = plan;
		}
		
		this.currentTransitLine = simpleScheduleProvider.createEmptyLine(id);
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
		
//		for (TransitRoute route : plan.getLine().getRoutes().values()) {
//			route.setDescription(plan.toString(this.budget));
//		}
	}
	

}