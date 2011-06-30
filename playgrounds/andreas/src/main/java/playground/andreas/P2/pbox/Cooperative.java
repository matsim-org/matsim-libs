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

	private PPlan bestPlan;
	private PPlan currentPlan;
	
	public Cooperative(Id id){
		this.id = id;
	}

	public void init(SimpleScheduleProvider simpleScheduleProvider) {
		PPlan plan = new PPlan(new IdImpl("0"));
		plan.setStartStop(simpleScheduleProvider.getRandomTransitStop());
		plan.setEndStop(simpleScheduleProvider.getRandomTransitStop());
		plan.setStartTime(0.0);
		plan.setEndTime(24.0 * 3600);
		plan.setLine(simpleScheduleProvider.createInitialRandomTransitLine(this.id, plan.getStartTime(), plan.getEndTime(), 1, plan.getStartStop(), plan.getEndStop()));
		this.bestPlan = null;
		this.currentPlan = plan;
	}

	public void score(TreeMap<Id, ScoreContainer> driverId2ScoreMap) {
		double totalLineScore = 0.0;
		int totalTripsServed = 0;
		
		for (Id vehId : this.currentPlan.getVehicleIds()) {
			totalLineScore += driverId2ScoreMap.get(vehId).getTotalRevenue();
			totalTripsServed += driverId2ScoreMap.get(vehId).getTripsServed();
		}
		
		this.currentPlan.setScore(totalLineScore);
		
		for (TransitRoute route : this.currentPlan.getLine().getRoutes().values()) {
			route.setDescription(this.currentPlan.toString());
		}		
		
		if(this.bestPlan == null){
			this.bestPlan = this.currentPlan;
		} else {
			if(this.currentPlan.getScore() > this.bestPlan.getScore()){
				this.bestPlan = this.currentPlan;
			}
		}		
	}

	public void replan(SimpleScheduleProvider simpleScheduleProvider) {
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
				plan.setLine(simpleScheduleProvider.createInitialRandomTransitLine(this.id, plan.getStartTime(), plan.getEndTime(), this.bestPlan.getVehicleIds().size() + 1, plan.getStartStop(), plan.getEndStop()));
				this.currentPlan = plan;
			} else if(rnd < 0.66){
				// profitable route, increase vehicle fleet
				PPlan plan = new PPlan(new IdImpl(simpleScheduleProvider.getIteration()));
				plan.setStartStop(this.bestPlan.getStartStop());
				plan.setEndStop(this.bestPlan.getEndStop());
				plan.setStartTime(Math.max(0.0, this.bestPlan.getStartTime() + (-0.5 + MatsimRandom.getRandom().nextDouble()) * 6 * 3600));
				plan.setEndTime(this.bestPlan.getEndTime());
				plan.setLine(simpleScheduleProvider.createInitialRandomTransitLine(this.id, plan.getStartTime(), plan.getEndTime(), this.bestPlan.getVehicleIds().size(), plan.getStartStop(), plan.getEndStop()));
				this.currentPlan = plan;
			} else {
				// profitable route, increase vehicle fleet
				PPlan plan = new PPlan(new IdImpl(simpleScheduleProvider.getIteration()));
				plan.setStartStop(this.bestPlan.getStartStop());
				plan.setEndStop(this.bestPlan.getEndStop());
				plan.setStartTime(this.bestPlan.getStartTime());
				plan.setEndTime(Math.min(24 * 3600.0, this.bestPlan.getEndTime() + (-0.5 + MatsimRandom.getRandom().nextDouble()) * 6 * 3600));
				plan.setLine(simpleScheduleProvider.createInitialRandomTransitLine(this.id, plan.getStartTime(), plan.getEndTime(), this.bestPlan.getVehicleIds().size(), plan.getStartStop(), plan.getEndStop()));
				this.currentPlan = plan;
			}			
			
		} else {//(this.bestPlan.getVehicleIds().size() == 1){
			// create complete new route
			PPlan plan = new PPlan(new IdImpl(simpleScheduleProvider.getIteration()));
			plan.setStartStop(simpleScheduleProvider.getRandomTransitStop());
			plan.setEndStop(simpleScheduleProvider.getRandomTransitStop());
			plan.setStartTime(0.0);
			plan.setEndTime(24.0 * 3600);
			plan.setLine(simpleScheduleProvider.createInitialRandomTransitLine(this.id, plan.getStartTime(), plan.getEndTime(), 1, plan.getStartStop(), plan.getEndStop()));
			this.currentPlan = plan;
		}
	}

	public TransitLine getCurrentTransitLine() {
		return this.currentPlan.getLine();		
	}

}