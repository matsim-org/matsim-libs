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
	private int numberOfRoutesCreated = 0;

	private PPlan bestPlan;
	private PPlan testPlan;

	private TransitLine currentTransitLine;
	
	public Cooperative(Id id){
		this.id = id;
	}

	public void init(SimpleCircleScheduleProvider simpleScheduleProvider) {
		PPlan plan = new PPlan(new IdImpl("0"));
		plan.setStartStop(simpleScheduleProvider.getRandomTransitStop());
		plan.setEndStop(simpleScheduleProvider.getRandomTransitStop());
		while(plan.getStartStop() == plan.getEndStop()){
			plan.setEndStop(simpleScheduleProvider.getRandomTransitStop());
		}
		
		plan.setStartTime(0.0);
		plan.setEndTime(24.0 * 3600);
		plan.setLine(simpleScheduleProvider.createBackAndForthTransitLine(this.id, plan.getStartTime(), plan.getEndTime(), 1, plan.getStartStop(), plan.getEndStop(), null));
		this.bestPlan = null;
		this.testPlan = plan;
		this.currentTransitLine = simpleScheduleProvider.createEmptyLine(id);
		for (TransitRoute route : this.testPlan.getLine().getRoutes().values()) {
			this.currentTransitLine.addRoute(route);
		}		
	}

	public void score(TreeMap<Id, ScoreContainer> driverId2ScoreMap) {
		
		if(this.bestPlan != null){
			scorePlan(driverId2ScoreMap, this.bestPlan);
		}
		scorePlan(driverId2ScoreMap, this.testPlan);

	}

	public void replan(SimpleCircleScheduleProvider simpleScheduleProvider) {
		
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
				plan.setLine(simpleScheduleProvider.createBackAndForthTransitLine(this.id, plan.getStartTime(), plan.getEndTime(), this.bestPlan.getVehicleIds().size() + 1, plan.getStartStop(), plan.getEndStop(), this.bestPlan.getId()));
			} else {
				plan.setLine(simpleScheduleProvider.createBackAndForthTransitLine(this.id, plan.getStartTime(), plan.getEndTime(), this.bestPlan.getVehicleIds().size(), plan.getStartStop(), plan.getEndStop(), this.bestPlan.getId()));
			}

			this.bestPlan = plan;
			this.testPlan = null;
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
				plan.setStartTime(Math.max(0.0, this.bestPlan.getStartTime() + (-0.5 + MatsimRandom.getRandom().nextDouble()) * 6 * 3600));
				plan.setEndTime(this.bestPlan.getEndTime());
				plan.setLine(simpleScheduleProvider.createBackAndForthTransitLine(this.id, plan.getStartTime(), plan.getEndTime(), 1, plan.getStartStop(), plan.getEndStop(), null));
				this.testPlan = plan;
			} else {
				// profitable route, change endTime
				PPlan plan = new PPlan(new IdImpl(simpleScheduleProvider.getIteration()));
				plan.setStartStop(this.bestPlan.getStartStop());
				plan.setEndStop(this.bestPlan.getEndStop());
				plan.setStartTime(this.bestPlan.getStartTime());
				plan.setEndTime(Math.min(24 * 3600.0, this.bestPlan.getEndTime() + (-0.5 + MatsimRandom.getRandom().nextDouble()) * 6 * 3600));
				plan.setLine(simpleScheduleProvider.createBackAndForthTransitLine(this.id, plan.getStartTime(), plan.getEndTime(), 1, plan.getStartStop(), plan.getEndStop(), null));
				this.testPlan = plan;
			}			
			
		} else {
			
			if(this.bestPlan.getVehicleIds().size() == 1){
				// this plan has become non profitable
				this.bestPlan = null;
//			} else if(this.bestPlan.getTripsServed() == 0){
				// plan not used anymore - delete it
//				this.bestPlan = null;
			} else {
				// decrease number of vehicles
				PPlan plan = new PPlan(this.bestPlan.getId());
				plan.setScore(this.bestPlan.getScore());

				plan.setStartStop(this.bestPlan.getStartStop());
				plan.setEndStop(this.bestPlan.getEndStop());
				plan.setStartTime(this.bestPlan.getStartTime());
				plan.setEndTime(this.bestPlan.getEndTime());

				plan.setLine(simpleScheduleProvider.createBackAndForthTransitLine(this.id, plan.getStartTime(), plan.getEndTime(), this.bestPlan.getVehicleIds().size() - 1, plan.getStartStop(), plan.getEndStop(), this.bestPlan.getId()));
				
				this.bestPlan = plan;
			}
			
			// create complete new route
			PPlan plan = new PPlan(new IdImpl(simpleScheduleProvider.getIteration()));
			plan.setStartStop(simpleScheduleProvider.getRandomTransitStop());
			plan.setEndStop(simpleScheduleProvider.getRandomTransitStop());
			while(plan.getStartStop() == plan.getEndStop()){
				plan.setEndStop(simpleScheduleProvider.getRandomTransitStop());
			}
			plan.setStartTime(0.0);
			plan.setEndTime(24.0 * 3600);
			plan.setLine(simpleScheduleProvider.createBackAndForthTransitLine(this.id, plan.getStartTime(), plan.getEndTime(), 1, plan.getStartStop(), plan.getEndStop(), null));
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

	private void scorePlan(TreeMap<Id, ScoreContainer> driverId2ScoreMap, PPlan plan) {
		double totalLineScore = 0.0;
		int totalTripsServed = 0;
		
		for (Id vehId : plan.getVehicleIds()) {
			totalLineScore += driverId2ScoreMap.get(vehId).getTotalRevenue();
			totalTripsServed += driverId2ScoreMap.get(vehId).getTripsServed();
		}
		
		plan.setScore(totalLineScore);
		plan.setTripsServed(totalTripsServed);
		
		for (TransitRoute route : plan.getLine().getRoutes().values()) {
			route.setDescription(plan.toString());
		}
	}
	

}