/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatX11.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

import java.util.ArrayList;

import org.apache.log4j.Logger;

import org.matsim.gbl.MatsimRandom;
import org.matsim.planomat.PlanOptimizeTimes;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.router.PlansCalcRouteLandmarks;
import org.matsim.scoring.PlanScorer;
import org.matsim.scoring.ScoringFunctionFactory;
import org.matsim.population.Act;





/**
 * @author Matthias Feil
 * TS algorithm to optimize time. Similar to Planomat.
 */

public class TimeOptimizer implements org.matsim.population.algorithms.PlanAlgorithm { 
	
	private final int						NEIGHBOURHOOD_SIZE, MAX_ITERATIONS;
	//private final double					WEIGHT_CHANGE_ORDER, WEIGHT_CHANGE_NUMBER;
	//private final double 					WEIGHT_INC_NUMBER;
	private final PlanScorer 				scorer;
	private static final Logger log = Logger.getLogger(TimeOptimizer.class);
	
	//////////////////////////////////////////////////////////////////////
	// Constructor
	//////////////////////////////////////////////////////////////////////
	
	public TimeOptimizer (ScoringFunctionFactory factory){
		
		this.scorer 				= new PlanomatXPlanScorer (factory);
		
		this.NEIGHBOURHOOD_SIZE 	= 10;				//TODO @MF: constants to be configured externally, sum must be smaller than or equal to 1.0
		//this.WEIGHT_CHANGE_ORDER 	= 0.2; 
		//this.WEIGHT_CHANGE_NUMBER 	= 0.6;
		//this.WEIGHT_INC_NUMBER 		= 0.5; 				//Weighing whether adding or removing activities in change number method.
		this.MAX_ITERATIONS 		= 10;
	}
	
		
	//////////////////////////////////////////////////////////////////////
	// run() method
	//////////////////////////////////////////////////////////////////////
	
	public void run (Plan plan){
		
		long runStartTime = System.currentTimeMillis();
		
		int neighbourhood_size = 0;
		for (int i = plan.getActsLegs().size()-1;i>0;i=i-2){
			neighbourhood_size += i;
		}
		PlanomatXPlan [] neighbourhood 					= new PlanomatXPlan [neighbourhood_size];
		int [] notNewInNeighbourhood 					= new int [neighbourhood_size];
		int [] tabuInNeighbourhood 						= new int [neighbourhood_size];
		ArrayList<PlanomatXPlan> nonTabuNeighbourhood 	= new ArrayList<PlanomatXPlan>();
		ArrayList<PlanomatXPlan> tabuList			 	= new ArrayList<PlanomatXPlan>();
		
		// Copy the plan into all fields of the array neighbourhood
		for (int i = 0; i < neighbourhood.length; i++){
			neighbourhood[i] = new PlanomatXPlan (plan.getPerson());
			neighbourhood[i].copyPlan(plan);			
		}
		
		// Write the given plan into the tabuList
		tabuList.add((PlanomatXPlan)plan);
		
		// Do Tabu Search iterations
		int currentIteration;
		for (currentIteration = 1; currentIteration<=MAX_ITERATIONS;currentIteration++){
			
			// Define the neighbourhood
			this.createNeighbourhood(neighbourhood, notNewInNeighbourhood);	
			
			// Check whether plans are tabu
			boolean warningTabu = this.checkForTabuSolutions (neighbourhood, notNewInNeighbourhood, tabuList, nonTabuNeighbourhood);
			
		
		}
		
		
	}
	
	//////////////////////////////////////////////////////////////////////
	// Neighbourhood definition 
	//////////////////////////////////////////////////////////////////////
	
	public void createNeighbourhood (PlanomatXPlan [] neighbourhood, int[] notNewInNeighbourhood) {
		
		int pos = 0;
		for (int outer=0;outer<neighbourhood[0].getActsLegs().size()-2;outer=outer+2){
			for (int inner=outer+2;outer<neighbourhood[0].getActsLegs().size();inner=inner+2){
				
				notNewInNeighbourhood[pos]=this.increaseTime(neighbourhood[pos], outer, inner);
				pos++;
				
				notNewInNeighbourhood[pos]=this.decreaseTime(neighbourhood[pos], outer, inner);
				pos++;
				
			}
		}
	}
	
	
	
	public int increaseTime(PlanomatXPlan basePlan, int outer, int inner){
		
		if (((Act)(basePlan.getActsLegs().get(inner))).getDur()>=900){
			
			((Act)(basePlan.getActsLegs().get(outer))).setDur(((Act)(basePlan.getActsLegs().get(outer))).getDur()+900);
			((Act)(basePlan.getActsLegs().get(inner))).setDur(((Act)(basePlan.getActsLegs().get(inner))).getDur()-900);
			
			((Act)(basePlan.getActsLegs().get(outer))).setEndTime(((Act)(basePlan.getActsLegs().get(outer))).getEndTime()+900);
			((Act)(basePlan.getActsLegs().get(inner))).setStartTime(((Act)(basePlan.getActsLegs().get(inner))).getStartTime()+900);
			
			for (int i=outer+2;i<=inner-2;i=i+2){
				((Act)(basePlan.getActsLegs().get(i))).setEndTime(((Act)(basePlan.getActsLegs().get(i))).getEndTime()+900);
				((Act)(basePlan.getActsLegs().get(i))).setStartTime(((Act)(basePlan.getActsLegs().get(i))).getStartTime()+900);
			}
			basePlan.setScore(scorer.getScore(basePlan));
			return 0;
		}
		else return 1;
	}
	
	
	public int decreaseTime(PlanomatXPlan basePlan, int outer, int inner){
		if (((Act)(basePlan.getActsLegs().get(outer))).getDur()>=900){
			
			((Act)(basePlan.getActsLegs().get(outer))).setDur(((Act)(basePlan.getActsLegs().get(outer))).getDur()-900);
			((Act)(basePlan.getActsLegs().get(inner))).setDur(((Act)(basePlan.getActsLegs().get(inner))).getDur()+900);
			
			((Act)(basePlan.getActsLegs().get(outer))).setEndTime(((Act)(basePlan.getActsLegs().get(outer))).getEndTime()-900);
			((Act)(basePlan.getActsLegs().get(inner))).setStartTime(((Act)(basePlan.getActsLegs().get(inner))).getStartTime()-900);
			
			for (int i=outer+2;i<=inner-2;i=i+2){
				((Act)(basePlan.getActsLegs().get(i))).setEndTime(((Act)(basePlan.getActsLegs().get(i))).getEndTime()-900);
				((Act)(basePlan.getActsLegs().get(i))).setStartTime(((Act)(basePlan.getActsLegs().get(i))).getStartTime()-900);
			}
			basePlan.setScore(scorer.getScore(basePlan));
			return 0;
		}
		else return 1;
	}
	
	
	//////////////////////////////////////////////////////////////////////
	// Help methods 
	//////////////////////////////////////////////////////////////////////
	
	
	public boolean checkForTabuSolutions (PlanomatXPlan [] neighbourhood, int [] notNewInNeighbourhood, 
			ArrayList<PlanomatXPlan> tabuList, ArrayList<PlanomatXPlan> nonTabuNeighbourhood){
		
		
		
		return false;
	}
	
}
	
