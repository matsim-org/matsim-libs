/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.minibus.operator;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.replanning.PStrategy;
import org.matsim.contrib.minibus.replanning.PStrategyManager;
import org.matsim.contrib.minibus.routeProvider.PRouteProvider;
import org.matsim.core.gbl.MatsimRandom;

import java.util.LinkedList;
import java.util.List;

/**
 * This operator has multiple plans. Each is weighted by the number of vehicles associated with.
 * The number of vehicles depends on the score per vehicle and plan. In the end, each plan should have approximately the same score.
 * 
 * @author aneumann
 * 
 * @deprecated This one is not very well tested. Use {@link CarefulMultiPlanOperator} instead.
 *
 */
public final class MultiPlanOperator extends AbstractOperator{
	
	public static final String OPERATOR_NAME = "MultiPlanOperator";
	
	private final List<PPlan> plans;

	public MultiPlanOperator(Id<Operator> id, PConfigGroup pConfig, PFranchise franchise){
		super(id, pConfig, franchise);
		this.plans = new LinkedList<>();
	}
	
	public boolean init(PRouteProvider pRouteProvider, PStrategy initialStrategy, int iteration, double initialBudget) {
		boolean initializedSuccessfully = super.init(pRouteProvider, initialStrategy, iteration, initialBudget);
		if (initializedSuccessfully) {
			this.plans.add(this.bestPlan);
			this.bestPlan = null;
		}
		return initializedSuccessfully;
	}
	
	@Override
	public List<PPlan> getAllPlans(){
		return this.plans;		
	}
	
	@Override
	public PPlan getBestPlan() {
		if (this.bestPlan == null) {
			
			// will not return the best plan, but one random plan selected from all plans with at least two vehicles
			List<PPlan> plansWithAtLeastTwoVehicles = new LinkedList<>();
			int numberOfVehicles = 0;
			for (PPlan pPlan : this.plans) {
				if (pPlan.getNVehicles() > 1) {
					plansWithAtLeastTwoVehicles.add(pPlan);
					numberOfVehicles += pPlan.getNVehicles();
				}
			}

			double accumulatedWeight = 0.0;
			double rndTreshold = MatsimRandom.getRandom().nextDouble() * numberOfVehicles;
			for (PPlan pPlan : plansWithAtLeastTwoVehicles) {
				accumulatedWeight += pPlan.getNVehicles();
				if (rndTreshold <= accumulatedWeight) {
					this.bestPlan = pPlan;
					return this.bestPlan;
				}
			}
		}
		
		return this.bestPlan;
	}

	public void replan(PStrategyManager pStrategyManager, int iteration) {
		
		// First, balance the budget
		if(this.budget < 0){
			// insufficient, sell vehicles
			int numberOfVehiclesToSell = -1 * Math.min(-1, (int) Math.floor(this.budget / this.costPerVehicleSell));
			
			while (numberOfVehiclesToSell > 0) {
				this.findWorstPlanAndRemoveOneVehicle();
				this.budget += this.costPerVehicleSell;
				numberOfVehiclesToSell--;
			}
		}

		// Second, buy vehicles
		
		int numberOfNewVehicles = 0;		
		while (this.getBudget() > this.getCostPerVehicleBuy()) {
			// budget ok, buy one
			this.setBudget(this.getBudget() - this.getCostPerVehicleBuy());
			numberOfNewVehicles++;
		}
		// distribute them among the plans
		while (numberOfNewVehicles > 0) {
			// add one vehicle to best plan
			this.findBestPlanAndAddOneVehicle();
			numberOfNewVehicles--;
		}

		
		// Third, replan
		if (this.currentIteration != iteration) {
			// new iteration - replan
			// get the best plan from the last iteration
			this.getBestPlan();
		}
		this.currentIteration = iteration;
		
		if (this.bestPlan != null) {
			PStrategy strategy = pStrategyManager.chooseStrategy();
			if (strategy != null) {
				PPlan newPlan = strategy.run(this);
				if (newPlan != null) {
					
					if(this.getFranchise().planRejected(newPlan)){
						// plan is rejected by franchise system
						newPlan = null;
					}	
					
					if (newPlan != null) {
						// remove vehicle from worst plan
//					this.findWorstPlanAndRemoveOneVehicle(this.plans);
						
						// best plan must contain at least two vehicles, see getBestPlan()
						this.bestPlan.setNVehicles(this.bestPlan.getNVehicles() - 1);
						this.plans.add(newPlan);
					}
				}
			}
			this.bestPlan = null;
		}
		
		// Fourth, move one vehicle from the worst negative plan to the best plan, if possible
		if (this.plans.size() > 1) {
			if (this.findWorstNegativePlanAndRemoveOneVehicle()) {
				this.findBestPlanAndAddOneVehicle();
			}
		}
		
		
//		// Fourth, redistribute vehicles from plans with negative score
//		List<PPlan> plansToKeep = new LinkedList<PPlan>();
//		int numberOfVehiclesToRedistribute = 0;
//		for (PPlan plan : this.plans) {
//			if (plan.getPlannedScorePerVehicle() < 0) {
//				// negative score, remove one vehicle from plan
//				plan.setNVehicles(plan.getNVehicles() - 1);
//				numberOfVehiclesToRedistribute++;
//				if (plan.getNVehicles() > 0) {
//					plansToKeep.add(plan);
//				}
//			} else {
//				// keep the plan
//				plansToKeep.add(plan);
//			}
//		}
//		if (plansToKeep.size() > 0) {
//			this.plans = plansToKeep;
//		}
//		while (numberOfVehiclesToRedistribute > 0) {
//			this.findBestPlanAndAddOneVehicle(this.plans);
//			numberOfVehiclesToRedistribute--;
//		}
		
		// remove all plans with no vehicles
		List<PPlan> plansToKeep = new LinkedList<>();
		for (PPlan plan : this.plans) {
			if (plan.getNVehicles() > 0) {
				plansToKeep.add(plan);
			}
		}
		if (plansToKeep.size() == 0) {
			log.error("Removed too many plans. There was at least one plan with no vehicles associated with.");
			log.error(this.plans.toString());
		}
		
		// Fifth, reinitialize all plans
		for (PPlan plan : this.plans) {
			plan.setLine(this.routeProvider.createTransitLineFromOperatorPlan(this.id, plan));
		}
		
		this.updateCurrentTransitLine();
	}
	
	/**
	 * Find worst negative plan (worst score per vehicle). Removes one vehicle. Removes the whole plan, if no vehicle is left.
	 * 
	 * @return true if one vehicle was removed
	 */
	private boolean findWorstNegativePlanAndRemoveOneVehicle(){
		PPlan worstPlan = null;
		for (PPlan plan : this.plans) {
			if (plan.getScorePerVehicle() < 0.0) {
				if (worstPlan == null) {
					worstPlan = plan;
				} else {
					if (plan.getScorePerVehicle() < worstPlan.getScorePerVehicle()) {
						worstPlan = plan;
					}
				}
			}
		}
		
		if (worstPlan != null) {
			// remove one vehicle
			worstPlan.setNVehicles(worstPlan.getNVehicles() - 1);
			// remove plan, if not served anymore
			if (worstPlan.getNVehicles() == 0) {
				this.plans.remove(worstPlan);
			}
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Find plan with the worst score per vehicle. Removes one vehicle. Removes the whole plan, if no vehicle is left.
	 * 
	 * @return
	 */
	private void findWorstPlanAndRemoveOneVehicle(){
		PPlan worstPlan = null;
		for (PPlan plan : this.plans) {
			if (worstPlan == null) {
				worstPlan = plan;
			} else {
				if (plan.getScorePerVehicle() < worstPlan.getScorePerVehicle()) {
					worstPlan = plan;
				}
			}
		}
		
		// remove one vehicle
		worstPlan.setNVehicles(worstPlan.getNVehicles() - 1);
		// remove plan, if not served anymore
		if (worstPlan.getNVehicles() == 0) {
			this.plans.remove(worstPlan);
		}
	}
	
	/**
	 * Find plan with the best score per vehicle. Add one vehicle.
	 * 
	 * @return
	 */
	private void findBestPlanAndAddOneVehicle(){
		PPlan bestPlan = null;
		for (PPlan plan : this.plans) {
			if (bestPlan == null) {
				bestPlan = plan;
			} else {
				if (plan.getScorePerVehicle() > bestPlan.getScorePerVehicle()) {
					bestPlan = plan;
				}
			}
		}
		
		// add one vehicle
		bestPlan.setNVehicles(bestPlan.getNVehicles() + 1);
	}
	
}