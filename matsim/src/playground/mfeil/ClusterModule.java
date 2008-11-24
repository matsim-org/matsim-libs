/* *********************************************************************** *
 * project: org.matsim.*
 * ClusterModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.mfeil;

import org.matsim.gbl.Gbl;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSetSimultan;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Plan;
import org.matsim.replanning.modules.StrategyModule;
import org.matsim.replanning.modules.MultithreadedModuleA;
import org.matsim.router.PlansCalcRouteLandmarks;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.PreProcessLandmarks;

import java.util.ArrayList;

public class ClusterModule implements StrategyModule {
	
	private ArrayList<Plan> 				aList, bList;
	private MultithreadedModuleA 			module;
	private final PreProcessLandmarks		preProcessRoutingData;
	private final LocationMutatorwChoiceSet locator;
	private final PlansCalcRouteLandmarks 	router;
	private final LegTravelTimeEstimator	estimator;
	private static int						i=0;
	private final double					minimumTime;
	
	
	
	public ClusterModule (ControlerMFeil controler){
		this.module 				= new PlanomatX12Initialiser(controler);
		this.preProcessRoutingData 	= new PreProcessLandmarks(new FreespeedTravelTimeCost());
		this.preProcessRoutingData.run(controler.getNetwork());
		this.router 				= new PlansCalcRouteLandmarks (controler.getNetwork(), this.preProcessRoutingData, controler.getTravelCostCalculator(), controler.getTravelTimeCalculator());
		this.locator 				= new LocationMutatorwChoiceSetSimultan(controler.getNetwork(), controler);
		DepartureDelayAverageCalculator tDepDelayCalc = new DepartureDelayAverageCalculator(
				controler.getNetwork(), 
				controler.getTraveltimeBinSize());
		this.estimator = Gbl.getConfig().planomat().getLegTravelTimeEstimator(
				controler.getTravelTimeCalculator(), 
				controler.getTravelCostCalculator(), 
				tDepDelayCalc, 
				controler.getNetwork());
		this.minimumTime			= 1800;
		
	}
	
	public void init() {
		this.aList = new ArrayList<Plan>();
		this.bList = new ArrayList<Plan>();
		this.module.init();
	}

	public void handlePlan(final Plan plan) {	
	
		if (i%2==0){
			this.aList.add(plan);
			i++;
		}
		else {
			this.bList.add(plan);
			i++;
		}
	}

	public void finish(){
		if (!aList.isEmpty()){
			module.handlePlan(aList.get(0));
		}
		if (!bList.isEmpty()){
			module.handlePlan(bList.get(0));
		}
		module.finish();
		
		if (aList.size()>1){
			
			for (int x=1;x<aList.size();x++){
				Plan bestPlan = new Plan (aList.get(0).getPerson());
				bestPlan.copyPlan(aList.get(0));
				ArrayList<Object> al = aList.get(x).getActsLegs();
				if(al.size()>bestPlan.getActsLegs().size()){ 
					int i;
					for (i = 2; i<bestPlan.getActsLegs().size()-2;i++){
						al.remove(i);
						al.add(i, bestPlan.getActsLegs().get(i));	
					}
					for (int j = i; j<al.size()-2;j=j+0){
						al.remove(j);
					}
				}
				else if(al.size()<bestPlan.getActsLegs().size()){
					int i;
					for (i = 2; i<al.size()-2;i++){
						al.remove(i);
						al.add(i, bestPlan.getActsLegs().get(i));	
					}
					for (int j = i; j<bestPlan.getActsLegs().size()-2;j++){			
						al.add(j, bestPlan.getActsLegs().get(j));
					}
				}
				else {
					for (int i = 2; i<al.size()-2;i++){
					al.remove(i);
					al.add(i, bestPlan.getActsLegs().get(i));	
					}
				}
				this.locator.handlePlan(aList.get(x));
				this.router.run(aList.get(x));
				double move = this.cleanSchedule(((Act)(aList.get(x).getActsLegs().get(0))).getEndTime(), aList.get(x));
				int loops=1;
				while (move!=0.0){
					loops++;
					move = this.cleanSchedule(java.lang.Math.max(((Act)(aList.get(x).getActsLegs().get(0))).getEndTime()-move,0), aList.get(x));
					if (loops>3) {
						for (int i=2;i< aList.get(x).getActsLegs().size()-4;i+=2){
							((Act)aList.get(x).getActsLegs().get(i)).setDuration(this.minimumTime);
						}
						move = this.cleanSchedule(this.minimumTime, bList.get(x));
						if (move!=0.0){
							throw new IllegalArgumentException("No valid plan possible for person "+aList.get(x).getPerson().getId());
						}
					}
				}
				
			}
		}
		if (bList.size()>1){
			
			for (int x=1;x<bList.size();x++){
				Plan bestPlan = new Plan (bList.get(0).getPerson());
				bestPlan.copyPlan(bList.get(0));
				ArrayList<Object> al = bList.get(x).getActsLegs();
				if(al.size()>bestPlan.getActsLegs().size()){ 
					int i;
					for (i = 2; i<bestPlan.getActsLegs().size()-2;i++){
						al.remove(i);
						al.add(i, bestPlan.getActsLegs().get(i));	
					}
					for (int j = i; j<al.size()-2;j=j+0){
						al.remove(j);
					}
				}
				else if(al.size()<bestPlan.getActsLegs().size()){
					int i;
					for (i = 2; i<al.size()-2;i++){
						al.remove(i);
						al.add(i, bestPlan.getActsLegs().get(i));	
					}
					for (int j = i; j<bestPlan.getActsLegs().size()-2;j++){			
						al.add(j, bestPlan.getActsLegs().get(j));
					}
				}
				else {
					for (int i = 2; i<al.size()-2;i++){
					al.remove(i);
					al.add(i, bestPlan.getActsLegs().get(i));	
					}
				}
				this.locator.handlePlan(bList.get(x));
				this.router.run(bList.get(x));
				double move = this.cleanSchedule(((Act)(bList.get(x).getActsLegs().get(0))).getEndTime(), bList.get(x));
				int loops=1;
				while (move!=0.0){
					loops++;
					move = this.cleanSchedule(java.lang.Math.max(((Act)(bList.get(x).getActsLegs().get(0))).getEndTime()-move,0), bList.get(x));
					if (loops>3) {
						for (int i=2;i< bList.get(x).getActsLegs().size()-4;i+=2){
							((Act)bList.get(x).getActsLegs().get(i)).setDuration(this.minimumTime);
						}
						move = this.cleanSchedule(this.minimumTime, bList.get(x));
						if (move!=0.0){
							throw new IllegalArgumentException("No valid plan possible for person "+bList.get(x).getPerson().getId());
						}
					}
				}
			}
		}
		
		
	}
	
	public double cleanSchedule (double now, Plan plan){
		
		((Act)(plan.getActsLegs().get(0))).setEndTime(now);
		((Act)(plan.getActsLegs().get(0))).setDuration(now);
			
		double travelTime;
		for (int i=1;i<=plan.getActsLegs().size()-2;i=i+2){
			((Leg)(plan.getActsLegs().get(i))).setDepartureTime(now);
			travelTime = this.estimator.getLegTravelTimeEstimation(plan.getPerson().getId(), now, (Act)(plan.getActsLegs().get(i-1)), (Act)(plan.getActsLegs().get(i+1)), (Leg)(plan.getActsLegs().get(i)));
			((Leg)(plan.getActsLegs().get(i))).setArrivalTime(now+travelTime);
			((Leg)(plan.getActsLegs().get(i))).setTravelTime(travelTime);
			now+=travelTime;
			
			if (i!=plan.getActsLegs().size()-2){
				((Act)(plan.getActsLegs().get(i+1))).setStartTime(now);
				travelTime = java.lang.Math.max(((Act)(plan.getActsLegs().get(i+1))).getDuration()-travelTime, 0);
				((Act)(plan.getActsLegs().get(i+1))).setDuration(travelTime);	
				((Act)(plan.getActsLegs().get(i+1))).setEndTime(now+travelTime);	
				now+=travelTime;
			}
			else {
				((Act)(plan.getActsLegs().get(i+1))).setStartTime(now);
				/* NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW*/
				if (86400>now){
					((Act)(plan.getActsLegs().get(i+1))).setDuration(86400-now);
					((Act)(plan.getActsLegs().get(i+1))).setEndTime(86400);
				}
				else if (86400+((Act)(plan.getActsLegs().get(0))).getDuration()>now){
					if (now<86400){
						((Act)(plan.getActsLegs().get(i+1))).setDuration(86400-now);
						((Act)(plan.getActsLegs().get(i+1))).setEndTime(86400);
					}
					else {
					((Act)(plan.getActsLegs().get(i+1))).setDuration(0);
					((Act)(plan.getActsLegs().get(i+1))).setEndTime(now);
					}
				}
				else {
					return (now-(86400+((Act)(plan.getActsLegs().get(0))).getDuration()));
				}
			}
		}
		return 0;
	}

}
