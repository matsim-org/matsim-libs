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
	private final ScheduleCleaner			cleaner;
	
	
	
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
		this.cleaner				= new ScheduleCleaner (this.estimator, this.minimumTime);
		
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
				
				this.writePlan(aList.get(0), aList.get(x));
			
				this.locator.handlePlan(aList.get(x));
				this.router.run(aList.get(x));
				this.cleanUpPlan(aList.get(x));
			}
		}
		if (bList.size()>1){
			
			for (int x=1;x<bList.size();x++){
				
				this.writePlan(bList.get(0), bList.get(x));
				
				this.locator.handlePlan(bList.get(x));
				this.router.run(bList.get(x));
				this.cleanUpPlan(bList.get(x));
			}
		}
	}
	
	private void cleanUpPlan (Plan plan){
		double move = this.cleaner.run(((Act)(plan.getActsLegs().get(0))).getEndTime(), plan);
		int loops=1;
		while (move!=0.0){
			loops++;
			move = this.cleaner.run(java.lang.Math.max(((Act)(plan.getActsLegs().get(0))).getEndTime()-move,0), plan);
			if (loops>3) {
				for (int i=2;i< plan.getActsLegs().size()-4;i+=2){
					((Act)(plan.getActsLegs().get(i))).setDuration(this.minimumTime);
				}
				move = this.cleaner.run(this.minimumTime, plan);
				if (move!=0.0){
					throw new IllegalArgumentException("No valid plan possible for person "+plan.getPerson().getId());
				}
			}
		}
	}
	
	
	private void writePlan (Plan in, Plan out){
		Plan bestPlan = new Plan (in.getPerson());
		bestPlan.copyPlan(in);
		ArrayList<Object> al = out.getActsLegs();
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
	}

}
