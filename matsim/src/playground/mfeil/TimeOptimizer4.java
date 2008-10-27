/* *********************************************************************** *
 * project: org.matsim.*
 * TimeOptimizer4.java
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.apache.log4j.Logger;
import org.matsim.controler.Controler;
import org.matsim.population.Plan;
import org.matsim.scoring.PlanScorer;
import org.matsim.scoring.ScoringFunctionFactory;
import org.matsim.population.Act;
import org.matsim.population.Leg;


/**
 * @author Matthias Feil
 * Like TimeOptimizer2 but with sort functions rather than "best" fields.
 */

public class TimeOptimizer4 implements org.matsim.population.algorithms.PlanAlgorithm { 
	
	private final int						MAX_ITERATIONS, OFFSET, STOP_CRITERION;
	private final PlanScorer 				scorer;
	private final LegTravelTimeEstimator	estimator;
	private static final Logger 			log = Logger.getLogger(TimeOptimizer4.class);
	
	//////////////////////////////////////////////////////////////////////
	// Constructor
	//////////////////////////////////////////////////////////////////////
	
	public TimeOptimizer4 (ScoringFunctionFactory factory, LegTravelTimeEstimator estimator){
		
		this.scorer 				= new PlanomatXPlanScorer (factory);
		this.estimator				= estimator;
		this.OFFSET					= 1800;
		//TODO @MF: constants to be configured externally, sum must be smaller than or equal to 1.0
		this.MAX_ITERATIONS 		= 30;
		this.STOP_CRITERION			= 5;
	}
	
		
	//////////////////////////////////////////////////////////////////////
	// run() method
	//////////////////////////////////////////////////////////////////////
	
	public void run (Plan plan){
		
		long runStartTime = System.currentTimeMillis();
		
		// Initial clean-up of plan for the case actslegs is not sound.
		double now =((Act)(plan.getActsLegs().get(0))).getEndTime();
		
		double travelTime;
		for (int i=1;i<=plan.getActsLegs().size()-2;i=i+2){
			((Leg)(plan.getActsLegs().get(i))).setDepTime(now);
			travelTime = this.estimator.getLegTravelTimeEstimation(plan.getPerson().getId(), now, (Act)(plan.getActsLegs().get(i-1)), (Act)(plan.getActsLegs().get(i+1)), (Leg)(plan.getActsLegs().get(i)));
			((Leg)(plan.getActsLegs().get(i))).setArrTime(now+travelTime);
			((Leg)(plan.getActsLegs().get(i))).setTravTime(travelTime);
			now+=travelTime;
			
			if (i!=plan.getActsLegs().size()-2){
				((Act)(plan.getActsLegs().get(i+1))).setStartTime(now);
				travelTime = java.lang.Math.max(((Act)(plan.getActsLegs().get(i+1))).getDur()-travelTime, 0.0);
				((Act)(plan.getActsLegs().get(i+1))).setEndTime(now+travelTime);	
				now+=travelTime;
			}
			else {
				((Act)(plan.getActsLegs().get(i+1))).setStartTime(now);
				((Act)(plan.getActsLegs().get(i+1))).setDur(86400-now);
			}
		}
		
		
		int neighbourhood_size = 0;
		for (int i = plan.getActsLegs().size()-1;i>0;i=i-2){
			neighbourhood_size += i;
		}
		PlanomatXPlan [] neighbourhood 					= new PlanomatXPlan [neighbourhood_size];
		int [] notNewInNeighbourhood 					= new int [neighbourhood_size];
		ArrayList<PlanomatXPlan> nonTabuNeighbourhood 	= new ArrayList<PlanomatXPlan>();
		ArrayList<PlanomatXPlan> tabuList			 	= new ArrayList<PlanomatXPlan>();
		
		
		String outputfile = Controler.getOutputFilename("Timer_log"+Counter.timeOptCounter+"_"+plan.getPerson().getId()+".xls");
		Counter.timeOptCounter++;
		PrintStream stream;
		try {
			stream = new PrintStream (new File(outputfile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		stream.print(plan.getScore()+"\t\t\t");
		for (int z= 0;z<plan.getActsLegs().size();z=z+2){
			Act act = (Act)plan.getActsLegs().get(z);
			stream.print(act.getType()+"\t");
		}
		stream.println();
		stream.print("\t\t\t");
		for (int z= 0;z<plan.getActsLegs().size();z=z+2){
			stream.print(((Act)(plan.getActsLegs()).get(z)).getDur()+"\t");
		}
		stream.println();
		
		
		// Copy the plan into all fields of the array neighbourhood
		for (int i = 0; i < neighbourhood.length; i++){
			neighbourhood[i] = new PlanomatXPlan (plan.getPerson());
			neighbourhood[i].copyPlan(plan);			
		}
		
		// Write the given plan into the tabuList
		tabuList.add(new PlanomatXPlan (((PlanomatXPlan)plan).getPerson()));
		tabuList.get(0).copyPlan((PlanomatXPlan)plan);
		
		//Set the given plan as bestSolution
		PlanomatXPlan bestSolution = new PlanomatXPlan (plan.getPerson());
		bestSolution.copyPlan(plan);
		
		
		// Do Tabu Search iterations
		int currentIteration;
		int lastImprovement = 0;
		for (currentIteration = 1; currentIteration<=MAX_ITERATIONS;currentIteration++){
			
			stream.println("Iteration "+currentIteration);
			
			// Define the neighbourhood
			this.createNeighbourhood(neighbourhood, notNewInNeighbourhood);	
			
			// Check whether plans are tabu
			boolean warningTabu = this.checkForTabuSolutions (neighbourhood, notNewInNeighbourhood, tabuList, nonTabuNeighbourhood, stream);
			if (warningTabu) {
				log.info("No non-tabu solutions found for person "+plan.getPerson().getId()+" at iteration "+currentIteration);
				break;
			}
		
			// Find best non-tabu plan. Becomes this iteration's solution. Write it into the tabuList
			java.util.Collections.sort(nonTabuNeighbourhood);
			tabuList.add(nonTabuNeighbourhood.get(nonTabuNeighbourhood.size()-1));
			stream.println("Best score \t"+nonTabuNeighbourhood.get(nonTabuNeighbourhood.size()-1).getScore());
			
			if (nonTabuNeighbourhood.get(nonTabuNeighbourhood.size()-1).getScore()>bestSolution.getScore()){
				bestSolution = nonTabuNeighbourhood.get(nonTabuNeighbourhood.size()-1);
				lastImprovement = 0;
			}
			else {
				lastImprovement++;
				if (lastImprovement > STOP_CRITERION) break;
			}
			
			if (this.MAX_ITERATIONS==currentIteration){
				//log.info("Tabu Search regularly finished for person "+plan.getPerson().getId()+" at iteration "+currentIteration);	
			}
			else {
				// Write this iteration's solution into all neighbourhood fields for the next iteration
				for (int initialisationOfNextIteration = 0;initialisationOfNextIteration<neighbourhood_size; initialisationOfNextIteration++){
					neighbourhood[initialisationOfNextIteration] = new PlanomatXPlan (nonTabuNeighbourhood.get(nonTabuNeighbourhood.size()-1).getPerson());
					neighbourhood[initialisationOfNextIteration].copyPlan(nonTabuNeighbourhood.get(nonTabuNeighbourhood.size()-1));
				}
				// Reset the nonTabuNeighbourhood list
				nonTabuNeighbourhood.clear();
			}				
		}
	
		// Update the plan with the final solution 		
		//java.util.Collections.sort(tabuList);
		stream.println("Selected solution\t"+bestSolution.getScore());
		ArrayList<Object> al = plan.getActsLegs();
		
		//log.info("Finale actslegs für Person "+tabuList.get(tabuList.size()-1).getPerson().getId()+": "+tabuList.get(tabuList.size()-1).getActsLegs());
		

		for (int i = 0; i<al.size();i++){
			al.remove(i);
			al.add(i, bestSolution.getActsLegs().get(i));	
		}
		log.info("Person "+plan.getPerson().getId()+" runtime: "+(System.currentTimeMillis()-runStartTime));
		
	}
	
	//////////////////////////////////////////////////////////////////////
	// Neighbourhood definition 
	//////////////////////////////////////////////////////////////////////
	
	public void createNeighbourhood (PlanomatXPlan [] neighbourhood, int[] notNewInNeighbourhood) {
		
		int pos = 0;
		for (int outer=0;outer<neighbourhood[0].getActsLegs().size()-2;outer=outer+2){
			for (int inner=outer+2;inner<neighbourhood[0].getActsLegs().size();inner=inner+2){
				
				notNewInNeighbourhood[pos]=this.increaseTime(neighbourhood[pos], outer, inner);
				pos++;
				
				notNewInNeighbourhood[pos]=this.decreaseTime(neighbourhood[pos], outer, inner);
				pos++;
				
			}
		}
	}
	
	
	
	public int increaseTime(PlanomatXPlan basePlan, int outer, int inner){
		
		if (((Act)(basePlan.getActsLegs().get(inner))).getDur()>=OFFSET){
			((Act)(basePlan.getActsLegs().get(outer))).setDur(((Act)(basePlan.getActsLegs().get(outer))).getDur()+OFFSET);
			double now =((Act)(basePlan.getActsLegs().get(outer))).getEndTime()+OFFSET;
			((Act)(basePlan.getActsLegs().get(outer))).setEndTime(now);
			
			double travelTime;
			for (int i=outer+1;i<=inner-1;i=i+2){
				((Leg)(basePlan.getActsLegs().get(i))).setDepTime(now);
				travelTime = this.estimator.getLegTravelTimeEstimation(basePlan.getPerson().getId(), now, (Act)(basePlan.getActsLegs().get(i-1)), (Act)(basePlan.getActsLegs().get(i+1)), (Leg)(basePlan.getActsLegs().get(i)));
				((Leg)(basePlan.getActsLegs().get(i))).setArrTime(now+travelTime);
				((Leg)(basePlan.getActsLegs().get(i))).setTravTime(travelTime);
				now+=travelTime;
				
				if (i!=inner-1){
					((Act)(basePlan.getActsLegs().get(i+1))).setStartTime(now);
					now+=((Act)(basePlan.getActsLegs().get(i+1))).getDur();
					((Act)(basePlan.getActsLegs().get(i+1))).setEndTime(now);					
				}
				else {
					((Act)(basePlan.getActsLegs().get(i+1))).setStartTime(now);
					
					if (((Act)(basePlan.getActsLegs().get(i+1))).getEndTime()>now){
						((Act)(basePlan.getActsLegs().get(i+1))).setDur(((Act)(basePlan.getActsLegs().get(i+1))).getEndTime()-now);
					}
					else {
						((Act)(basePlan.getActsLegs().get(i+1))).setDur(0);
						((Act)(basePlan.getActsLegs().get(i+1))).setEndTime(now);
						if (basePlan.getActsLegs().size()>i+3){
							travelTime = this.estimator.getLegTravelTimeEstimation(basePlan.getPerson().getId(), now, (Act)(basePlan.getActsLegs().get(i+1)), (Act)(basePlan.getActsLegs().get(i+3)), (Leg)(basePlan.getActsLegs().get(i+2)));
							((Leg)(basePlan.getActsLegs().get(i+2))).setArrTime(now+travelTime);
							((Leg)(basePlan.getActsLegs().get(i+2))).setTravTime(travelTime);
							now+=travelTime;
							((Act)(basePlan.getActsLegs().get(i+3))).setStartTime(now);
							if (((Act)(basePlan.getActsLegs().get(i+3))).getEndTime()>now){
								((Act)(basePlan.getActsLegs().get(i+3))).setDur(((Act)(basePlan.getActsLegs().get(i+3))).getEndTime()-now);
							}
							else return 1;
						}
						else return 1;
					}
				}
			}
			
			basePlan.setScore(scorer.getScore(basePlan));
			return 0;
		}
		else return 1;
	}
	
	
	
	public int decreaseTime(PlanomatXPlan basePlan, int outer, int inner){
		
		if (((Act)(basePlan.getActsLegs().get(outer))).getDur()>=OFFSET){
			((Act)(basePlan.getActsLegs().get(outer))).setDur(((Act)(basePlan.getActsLegs().get(outer))).getDur()-OFFSET);
			double now =((Act)(basePlan.getActsLegs().get(outer))).getEndTime()-OFFSET;
			((Act)(basePlan.getActsLegs().get(outer))).setEndTime(now);
			
			double travelTime;
			for (int i=outer+1;i<=inner-1;i=i+2){
				((Leg)(basePlan.getActsLegs().get(i))).setDepTime(now);
				travelTime = this.estimator.getLegTravelTimeEstimation(basePlan.getPerson().getId(), now, (Act)(basePlan.getActsLegs().get(i-1)), (Act)(basePlan.getActsLegs().get(i+1)), (Leg)(basePlan.getActsLegs().get(i)));
				((Leg)(basePlan.getActsLegs().get(i))).setArrTime(now+travelTime);
				((Leg)(basePlan.getActsLegs().get(i))).setTravTime(travelTime);
				now+=travelTime;
				
				if (i!=inner-1){
					((Act)(basePlan.getActsLegs().get(i+1))).setStartTime(now);
					now+=((Act)(basePlan.getActsLegs().get(i+1))).getDur();
					((Act)(basePlan.getActsLegs().get(i+1))).setEndTime(now);					
				}
				else {
					((Act)(basePlan.getActsLegs().get(i+1))).setStartTime(now);
					
					if (((Act)(basePlan.getActsLegs().get(i+1))).getEndTime()>now){
						((Act)(basePlan.getActsLegs().get(i+1))).setDur(((Act)(basePlan.getActsLegs().get(i+1))).getEndTime()-now);
					}
					else {
						((Act)(basePlan.getActsLegs().get(i+1))).setDur(0);
						((Act)(basePlan.getActsLegs().get(i+1))).setEndTime(now);
						if (basePlan.getActsLegs().size()>i+3){
							travelTime = this.estimator.getLegTravelTimeEstimation(basePlan.getPerson().getId(), now, (Act)(basePlan.getActsLegs().get(i+1)), (Act)(basePlan.getActsLegs().get(i+3)), (Leg)(basePlan.getActsLegs().get(i+2)));
							((Leg)(basePlan.getActsLegs().get(i+2))).setArrTime(now+travelTime);
							((Leg)(basePlan.getActsLegs().get(i+2))).setTravTime(travelTime);
							now+=travelTime;
							((Act)(basePlan.getActsLegs().get(i+3))).setStartTime(now);
							if (((Act)(basePlan.getActsLegs().get(i+3))).getEndTime()>now){
								((Act)(basePlan.getActsLegs().get(i+3))).setDur(((Act)(basePlan.getActsLegs().get(i+3))).getEndTime()-now);
							}
							else return 1;
						}
						else return 1;
					}
				}
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
			ArrayList<PlanomatXPlan> tabuList, ArrayList<PlanomatXPlan> nonTabuNeighbourhood, PrintStream stream){
		
		boolean warningOuter = true;
		boolean warningInner = true;
		for (int i=0;i<neighbourhood.length;i++){
			stream.print(neighbourhood[i].getScore()+"\t"+notNewInNeighbourhood[i]+"\t");
			if (notNewInNeighbourhood[i]==0){
				for (int j=0;j<tabuList.size();j++){
					warningInner = checkForEquality (neighbourhood[i].getActsLegs(), tabuList.get(tabuList.size()-1-j).getActsLegs());
					if (warningInner) {
						stream.print("1\t");
						break;
					}
				}
				if (!warningInner) {
					stream.print("0\t");
					warningOuter = false;
					nonTabuNeighbourhood.add(neighbourhood[i]);
				}
			}
			else stream.print("1\t");
			for (int z= 0;z<neighbourhood[i].getActsLegs().size();z=z+2){
				stream.print(((Act)(neighbourhood[i].getActsLegs().get(z))).getDur()+"\t");
			}
			stream.println();
		}
		return warningOuter;
	}
	
	
	public boolean checkForEquality (ArrayList<Object> list1, ArrayList<Object> list2){
		
		if (list1.size()!=list2.size()){
			return false;
		}
		else{
			for (int i=0;i<list1.size()-2;i=i+2){
				if (((Act)(list1.get(i))).getDur()!=((Act)(list2.get(i))).getDur()){
					return false;
				}
			}
			return true;
		}
	}	

	
}
	
