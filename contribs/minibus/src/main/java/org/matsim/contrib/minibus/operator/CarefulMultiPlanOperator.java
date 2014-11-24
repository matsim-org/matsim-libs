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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * This operator has multiple plans. Each is weighted by the number of vehicles associated with.
 * The number of vehicles depends on the score per vehicle and plan. In the end, each plan should have approximately the same score.
 * In contrast to {@link MultiPlanOperator}, this operator accepts all plans as blueprints for replanning, again picking one is a weighted random draw.
 * Vehicle are shifted away from plans instantly, i.e. if a plan scored negative at least one vehicle is removed.
 * Vehicles are distributed to positive plans and new plans.
 * 
 * @author aneumann
 *
 */
public final class CarefulMultiPlanOperator extends AbstractOperator{
	
	public static final String OPERATOR_NAME = "CarefulMultiPlanOperator";

    private List<PPlan> plans;

	public CarefulMultiPlanOperator(Id<Operator> id, PConfigGroup pConfig, PFranchise franchise){
		super(id, pConfig, franchise);
		this.plans = new LinkedList<>();
	}
	
	public boolean init(PRouteProvider pRouteProvider, PStrategy initialStrategy, int iteration, double initialBudget) {
		boolean initializedSuccessfully = super.init(pRouteProvider, initialStrategy, iteration, initialBudget);
		if (initializedSuccessfully) {
			this.buyAsManyVehiclesAsPossible();
			this.plans.add(this.bestPlan);
			this.bestPlan.setNVehicles(this.bestPlan.getNVehicles() + this.numberOfVehiclesInReserve);
			this.numberOfVehiclesInReserve = 0;
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
			
			// will not return the best plan, but one random plan selected from all plans weighted by the number of vehicles of each plan
			int numberOfVehicles = 0;
			for (PPlan pPlan : this.plans) {
				numberOfVehicles += pPlan.getNVehicles();
			}

			double accumulatedWeight = 0.0;
			double rndTreshold = MatsimRandom.getRandom().nextDouble() * numberOfVehicles;
			for (PPlan pPlan : this.plans) {
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
		this.currentIteration = iteration;
		
		// First remove vehicles from all plans scored negative and add them to the reserve
		this.numberOfVehiclesInReserve += this.removeVehiclesFromAllPlansWithNegativeScore(this.plans);
		
		// Second, balance the budget
		if(this.budget < 0){
			// insufficient, try to balance from reserve vehicles
			int numberOfVehiclesToSell = -1 * Math.min(-1, (int) Math.floor(this.budget / this.costPerVehicleSell));
			
			while (this.numberOfVehiclesInReserve > 0 && numberOfVehiclesToSell > 0) {
				// sell one vehicle from the reserve
				this.numberOfVehiclesInReserve--;
				numberOfVehiclesToSell--;
				this.budget += this.costPerVehicleSell;
			}
			
			while (numberOfVehiclesToSell > 0) {
				this.findWorstPlanAndRemoveOneVehicle();
				this.budget += this.costPerVehicleSell;
				numberOfVehiclesToSell--;
			}
		}

		// Third, buy vehicles
		buyAsManyVehiclesAsPossible();
		
		// Fourth, increase the number of vehicles by one for all positive plans
		addVehiclesToPlansWithAPositiveScore();
		
		// Fifth, add new plans for all vehicles in reserve, but half the number of vehicles already in service as max
		int maxVehiclesToDistribute = 0;
		for (PPlan plan : this.plans) {
			maxVehiclesToDistribute += plan.getNVehicles();
		}
		maxVehiclesToDistribute = Math.max(1, maxVehiclesToDistribute / 2);

        int tryouts = 10;
        int remainingNumberOfTryouts = tryouts;
		
		while (this.numberOfVehiclesInReserve > 0 && maxVehiclesToDistribute > 0 && remainingNumberOfTryouts > 0) {
			
			remainingNumberOfTryouts--;
			
			// find a random plan
			this.getBestPlan();
			
			PStrategy strategy = pStrategyManager.chooseStrategy();
			if (strategy != null) {
				PPlan newPlan = strategy.run(this);
				if (newPlan != null) {
					if(this.getFranchise().planRejected(newPlan)){
						// plan is rejected by franchise system
						newPlan = null;
					}	
					
					if (newPlan != null) {
						// get one vehicle from the reserve
						this.numberOfVehiclesInReserve--;
						maxVehiclesToDistribute--;
						this.plans.add(newPlan);
					}
				}
				
				this.bestPlan = null;
			}
		}
		
		removeAllPlansWithZeroVehicles();
		
		// Fifth, reinitialize all plans
		for (PPlan plan : this.plans) {
			plan.setLine(this.routeProvider.createTransitLineFromOperatorPlan(this.id, plan));
		}
		
		this.updateCurrentTransitLine();
	}

	private void addVehiclesToPlansWithAPositiveScore() {
		List<PPlan> plansWithAPositiveScore = new LinkedList<>();
		for (PPlan plan : this.plans) {
			if (plan.getScore() > 0) {
				// positive
				plansWithAPositiveScore.add(plan);
			}
		}
		
		Collections.sort(plansWithAPositiveScore);
		
		for (PPlan plan : plansWithAPositiveScore) {
			if (this.numberOfVehiclesInReserve > 0) {
				// increase by one vehicle
				plan.setNVehicles(plan.getNVehicles() + 1);
				this.numberOfVehiclesInReserve--;
			}
		}
	}

	private void removeAllPlansWithZeroVehicles() {
		// remove all plans with no vehicles
		List<PPlan> plansToKeep = new LinkedList<>();
		for (PPlan plan : this.plans) {
			if (plan.getNVehicles() > 0) {
				plansToKeep.add(plan);
			}
		}
		if (plansToKeep.size() == 0) {
			log.info("All plans have no vehicles. This may happen, if replanning fails to return a valid new plan AND all remaining plans scored negative while having only one vehicle.");
			log.info(this.plans.toString());
			for (PPlan pPlan : this.plans) {
				if (this.numberOfVehiclesInReserve > 0) {
					log.info("Anyway, adding one vehicle to plan " + pPlan);
					pPlan.setNVehicles(pPlan.getNVehicles() + 1);
					this.numberOfVehiclesInReserve--;
				}
			}
			// Rerun removing plans - not all plans may got a vehicle
			this.removeAllPlansWithZeroVehicles();
		} else {
			this.plans = plansToKeep;
		}
	}

	private void buyAsManyVehiclesAsPossible() {
		while (this.getBudget() > this.getCostPerVehicleBuy()) {
			// budget ok, buy one
			this.setBudget(this.getBudget() - this.getCostPerVehicleBuy());
			this.numberOfVehiclesInReserve++;
		}
	}
	
	/**
	 * 
	 * @param plansToCheck
	 * @return The number of vehicles removed
	 */
	private int removeVehiclesFromAllPlansWithNegativeScore(List<PPlan> plansToCheck) {
		int numberOfRemovedVehicles = 0;
		
		for (PPlan pPlan : plansToCheck) {
			if (pPlan.getScore() < 0.0) {
				// okay plan scored negative - tackle it
				double score = Math.abs(pPlan.getScore());
				// remove as many vehicles as necessary to compensate the impact on the budget plus one to hopefully get that plan positively scored in the next iteration
				int vehiclesToRemove = (int) (score / this.costPerVehicleSell) + 1;
				if(pPlan.getNVehicles() < vehiclesToRemove) {
					// this plan cannot compensate - remove all vehicles;
					vehiclesToRemove = pPlan.getNVehicles();
				}
				
				pPlan.setNVehicles(pPlan.getNVehicles() - vehiclesToRemove);
				numberOfRemovedVehicles += vehiclesToRemove;
			}
		}
		return numberOfRemovedVehicles;
	}

		
	/**
	 * Find plan with the worst score per vehicle. Removes one vehicle. Removes the whole plan, if no vehicle is left.
	 * 
	 * @return
	 */
	private void findWorstPlanAndRemoveOneVehicle(){
		PPlan worstPlan = null;
		for (PPlan plan : this.plans) {
			if (plan.getNVehicles() > 0) {
				if (worstPlan == null) {
					worstPlan = plan;
				} else {
					if (plan.getScorePerVehicle() < worstPlan.getScorePerVehicle()) {
						worstPlan = plan;
					}
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
}