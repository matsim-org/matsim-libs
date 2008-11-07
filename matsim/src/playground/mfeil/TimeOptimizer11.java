/* *********************************************************************** *
 * project: org.matsim.*
 * TimeOptimizer11.java
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
import org.matsim.population.Plan;
import org.matsim.scoring.PlanScorer;
import org.matsim.population.Act;
import org.matsim.population.Leg;


/**
 * @author Matthias Feil
 * Like TimeOptimizer10 (so including leg-only consideration) but also with
 * swapping of activity durations from TimeOptimizer9.
 */

public class TimeOptimizer11 implements org.matsim.population.algorithms.PlanAlgorithm { 
	
	private final int						MAX_ITERATIONS, STOP_CRITERION, NEIGHBOURHOOD_SIZE;
	private int								OFFSET;
	private final double					minimumTime;
	private final PlanScorer 				scorer;
	private final LegTravelTimeEstimator	estimator;
	private static final Logger 			log = Logger.getLogger(TimeOptimizer11.class);
	
	//////////////////////////////////////////////////////////////////////
	// Constructor
	//////////////////////////////////////////////////////////////////////
	
	public TimeOptimizer11 (LegTravelTimeEstimator estimator, PlanScorer scorer){
		
		this.scorer 				= scorer;
		this.estimator				= estimator;
		this.OFFSET					= 1800;
		this.MAX_ITERATIONS 		= 30;
		this.STOP_CRITERION			= 3;
		this.minimumTime			= 3600;
		this.NEIGHBOURHOOD_SIZE		= 10;
		//TODO @MF: constants to be configured externally
	}
	
		
	//////////////////////////////////////////////////////////////////////
	// run() method
	//////////////////////////////////////////////////////////////////////
	
	public void run (Plan plan){
		
	//	long runStartTime = System.currentTimeMillis();
		
		// Initial clean-up of plan for the case actslegs is not sound.
		double move = this.cleanSchedule (((Act)(plan.getActsLegs().get(0))).getEndTime(), plan);
		int loops=1;
		while (move!=0.0){
			loops++;
			move = this.cleanSchedule(java.lang.Math.max(((Act)(plan.getActsLegs().get(0))).getEndTime()-move,0), plan);
		//	log.info("Move = "+move);
			if (loops>3) {
				for (int i=2;i<plan.getActsLegs().size()-4;i+=2){
					((Act)plan.getActsLegs().get(i)).setDur(this.minimumTime);
				}
				move = this.cleanSchedule(this.minimumTime, plan);
				if (move!=0.0){
					log.warn("No valid initial solution found for "+plan.getPerson().getId()+"!");
					plan.setScore(-1000);
					return;
				}
			}
		}
		
		plan.setScore(this.scorer.getScore(plan));
		
		int neighbourhood_size = 0;
		for (int i = plan.getActsLegs().size()-1;i>0;i=i-2){
			neighbourhood_size += i;
		}
		int [][] moves 									= new int [neighbourhood_size][2];
		int [] position									= new int [2];
		PlanomatXPlan [] InitialNeighbourhood 			= new PlanomatXPlan [neighbourhood_size];
		PlanomatXPlan [] neighbourhood 					= new PlanomatXPlan [java.lang.Math.min(NEIGHBOURHOOD_SIZE, neighbourhood_size)];
		int [] notNewInNeighbourhood 					= new int [neighbourhood_size];
		ArrayList<PlanomatXPlan> tabuList			 	= new ArrayList<PlanomatXPlan>();
		Object [] solutions 							= new Object [3];
		int currentIteration							= 1;
		int lastImprovement 							= 0;
		
		
		//String outputfile = Controler.getOutputFilename("Timer_log"+Counter.timeOptCounter+"_"+plan.getPerson().getId()+".xls");
		//Counter.timeOptCounter++;
		//PrintStream stream;
		//try {
		//	stream = new PrintStream (new File(outputfile));
		//} catch (FileNotFoundException e) {
		//	e.printStackTrace();
		//	return;
		//}
		//stream.print(plan.getScore()+"\t\t\t");
		//for (int z= 0;z<plan.getActsLegs().size();z=z+2){
		//Act act = (Act)plan.getActsLegs().get(z);
		//	stream.print(act.getType()+"\t");
		//}
		//stream.println();
		//stream.print("\t\t\t");
		//for (int z= 0;z<plan.getActsLegs().size();z=z+2){
		//	stream.print(((Act)(plan.getActsLegs()).get(z)).getDur()+"\t");
		//}
		//stream.println();
		
		
		// Copy the plan into all fields of the array neighbourhood
		for (int i = 0; i < InitialNeighbourhood.length; i++){
			InitialNeighbourhood[i] = new PlanomatXPlan (plan.getPerson());
			InitialNeighbourhood[i].copyPlan(plan);			
		}
		
		// Write the given plan into the tabuList
		tabuList.add(new PlanomatXPlan (((PlanomatXPlan)plan).getPerson()));
		tabuList.get(0).copyPlan((PlanomatXPlan)plan);
		
		//Set the given plan as bestSolution
		PlanomatXPlan bestSolution = new PlanomatXPlan (plan.getPerson());
		bestSolution.copyPlan(plan);
		
		// Iteration 1
		//stream.println("Iteration "+1);
		this.createInitialNeighbourhood(InitialNeighbourhood, notNewInNeighbourhood, moves);
		
		// Check whether there are new solutions (is only necessary if there is no swap = versions 8 and 10)
		boolean warningNotNew=true;
		for (int i = 0; i<neighbourhood.length;i++){
			if (notNewInNeighbourhood[i]==0) {
				warningNotNew = false;
				break;
			}
		}
		if (warningNotNew==true) {
			log.info("No new solution at TimeOptimizer iteration "+currentIteration);
			return;
		}
		
		solutions = this.checkForTabuSolutions (InitialNeighbourhood, notNewInNeighbourhood, tabuList, moves, position);
		
		tabuList.add((PlanomatXPlan)solutions[0]);
		//stream.println("Best score \t"+((PlanomatXPlan)solutions[0]).getScore());
		
		if (((PlanomatXPlan)solutions[0]).getScore()>bestSolution.getScore()){
			bestSolution = new PlanomatXPlan (((PlanomatXPlan)solutions[0]).getPerson());
			bestSolution.copyPlan((PlanomatXPlan)solutions[0]);
			lastImprovement = 0;
		}
		else {
			lastImprovement++;
		}
		for (int i = 0;i<neighbourhood.length; i++){
			neighbourhood[i] = new PlanomatXPlan (((PlanomatXPlan)solutions[0]).getPerson());
			neighbourhood[i].copyPlan((PlanomatXPlan)solutions[0]);
		}
		
		
		// Do Tabu Search iterations
		for (currentIteration = 2; currentIteration<=MAX_ITERATIONS;currentIteration++){
			
		//	stream.println("Iteration "+currentIteration);
			
			// Define the neighbourhood			
			this.createNeighbourhood(neighbourhood, notNewInNeighbourhood, moves, position);
			
			// Check whether there are new solutions (is only necessary if there is no swap = versions 8 and 10)
			warningNotNew=true;
			for (int i = 0; i<neighbourhood.length;i++){
				if (notNewInNeighbourhood[i]==0) {
					warningNotNew = false;
					break;
				}
			}
			if (warningNotNew==true) {
				log.info("No new solution at TimeOptimizer iteration "+currentIteration);
				break;
			}
						
			// Check whether plans are tabu	
			solutions = this.checkForTabuSolutions (neighbourhood, notNewInNeighbourhood, tabuList, moves, position);
			
			if (solutions[1].equals(true)) {
				log.info("No non-tabu solutions found for person "+plan.getPerson().getId()+" at iteration "+currentIteration);
				break;
			}
		
			// Find best non-tabu plan. Becomes this iteration's solution. Write it into the tabuList
			tabuList.add((PlanomatXPlan)solutions[0]);
			//stream.println("Best score \t"+((PlanomatXPlan)solutions[0]).getScore());
			
			if (((PlanomatXPlan)solutions[0]).getScore()>bestSolution.getScore()){
				bestSolution = new PlanomatXPlan (((PlanomatXPlan)solutions[0]).getPerson());
				bestSolution.copyPlan((PlanomatXPlan)solutions[0]);
				lastImprovement = 0;
			}
			else {
				lastImprovement++;
				if (lastImprovement > STOP_CRITERION) break;
				// NEW NEW NEW NEW NEW
				//if (changeInOffset)	break;
				//else {
				//	this.OFFSET /=2;
				//	changeInOffset = true;
				//}
			}
			
			if (this.MAX_ITERATIONS!=currentIteration){
			
				// Write this iteration's solution into all neighbourhood fields for the next iteration
				for (int i = 0;i<neighbourhood.length; i++){
					neighbourhood[i] = new PlanomatXPlan (((PlanomatXPlan)solutions[0]).getPerson());
					neighbourhood[i].copyPlan((PlanomatXPlan)solutions[0]);
				}
			}				
		}
	
		// Update the plan with the final solution 		
		//stream.println("Selected solution\t"+bestSolution.getScore());
		ArrayList<Object> al = plan.getActsLegs();
		plan.setScore(bestSolution.getScore());
		
		for (int i = 0; i<al.size();i++){
			al.remove(i);
			al.add(i, bestSolution.getActsLegs().get(i));	
		}
		//log.info("Person "+plan.getPerson().getId()+" runtime: "+(System.currentTimeMillis()-runStartTime));
		
	}
	
	//////////////////////////////////////////////////////////////////////
	// Neighbourhood definition 
	//////////////////////////////////////////////////////////////////////
	
	public void createInitialNeighbourhood (PlanomatXPlan [] neighbourhood, int[] notNewInNeighbourhood,
			int [][] moves) {
		
		int pos = 0;
		for (int outer=0;outer<neighbourhood[0].getActsLegs().size()-2;outer=outer+2){
			for (int inner=outer+2;inner<neighbourhood[0].getActsLegs().size();inner=inner+2){
				
				notNewInNeighbourhood[pos]=this.increaseTime(neighbourhood[pos], outer, inner);
				moves [pos][0]=outer;
				moves [pos][1]=inner;
				pos++;
				
				notNewInNeighbourhood[pos]=this.decreaseTime(neighbourhood[pos], outer, inner);
				moves [pos][0]=inner;
				moves [pos][1]=outer;
				pos++;
				
			}
		}
	}
	
	
	public void createNeighbourhood (PlanomatXPlan [] neighbourhood, int[] notNewInNeighbourhood, int[][] moves, int[]position) {
		
		int pos = 0;
		int fieldLength = neighbourhood.length/3;
				
			for (int outer=java.lang.Math.max(position[0]-(fieldLength/2)*2,0);outer<position[0];outer+=2){
				notNewInNeighbourhood[pos]=this.decreaseTime(neighbourhood[pos], outer, position[0]);
				moves [pos][0]=position[0];
				moves [pos][1]=outer;
				pos++;
			}
		
			OuterLoop1:
				for (int outer=position[0];outer<neighbourhood[0].getActsLegs().size()-2;outer+=2){
					for (int inner=outer+2;inner<neighbourhood[0].getActsLegs().size();inner+=2){
						notNewInNeighbourhood[pos]=this.increaseTime(neighbourhood[pos], outer, inner);
						moves [pos][0]=outer;
						moves [pos][1]=inner;
						pos++;
						
						if (pos>=fieldLength) break OuterLoop1;
					}
				}
		
			for (int outer=java.lang.Math.max(position[1]-(fieldLength/2)*2,0);outer<position[1];outer+=2){
				
				if (outer!=position[0]){
					notNewInNeighbourhood[pos]=this.increaseTime(neighbourhood[pos], outer, position[1]);
					moves [pos][0]=outer;
					moves [pos][1]=position[1];
					pos++;
				}
			}
		
			OuterLoop2:
				for (int outer=position[1];outer<neighbourhood[0].getActsLegs().size()-2;outer+=2){
					for (int inner=outer+2;inner<neighbourhood[0].getActsLegs().size();inner+=2){
						notNewInNeighbourhood[pos]=this.decreaseTime(neighbourhood[pos], outer, inner);
						moves [pos][0]=inner;
						moves [pos][1]=outer;
						pos++;
						
						if (pos>=fieldLength*2) break OuterLoop2;
					}
				}
		
		
			OuterLoop3:
				for (int outer=0;outer<neighbourhood[0].getActsLegs().size()-2;outer=outer+2){
					for (int inner=outer+2;inner<neighbourhood[0].getActsLegs().size();inner=inner+2){
						
						if (outer!=position[0]	&&	inner!=position[1]){
							if (position[0]<position[1]){
								notNewInNeighbourhood[pos]=this.increaseTime(neighbourhood[pos], outer, inner);
								moves [pos][0]=outer;
								moves [pos][1]=inner;
								pos++;
								if (pos>neighbourhood.length-1) break OuterLoop3;
							}
							else if (inner!=position[0]	||	outer!=position[1]){
								notNewInNeighbourhood[pos]=this.increaseTime(neighbourhood[pos], outer, inner);
								moves [pos][0]=outer;
								moves [pos][1]=inner;
								pos++;
								if (pos>neighbourhood.length-1) break OuterLoop3;
							}
						}
					
						if (inner!=position[0]	&&	outer!=position[1]){
							if (position[0]>position[1]){
								notNewInNeighbourhood[pos]=this.decreaseTime(neighbourhood[pos], outer, inner);
								moves [pos][0]=inner;
								moves [pos][1]=outer;
								pos++;
								if (pos>neighbourhood.length-1) break OuterLoop3;
							}
							else if (outer!=position[0]	||	inner!=position[1]){
								notNewInNeighbourhood[pos]=this.decreaseTime(neighbourhood[pos], outer, inner);
								moves [pos][0]=inner;
								moves [pos][1]=outer;
								pos++;
								if (pos>neighbourhood.length-1) break OuterLoop3;
							}
						}
					}
				}		
	}
	
	
	
	public int increaseTime(PlanomatXPlan basePlan, int outer, int inner){
		
		if (((Act)(basePlan.getActsLegs().get(inner))).getDur()>=(OFFSET+this.minimumTime)){
			//((Act)(basePlan.getActsLegs().get(outer))).setDur(((Act)(basePlan.getActsLegs().get(outer))).getDur()+OFFSET);
			double now =((Act)(basePlan.getActsLegs().get(outer))).getEndTime()+OFFSET;
			//((Act)(basePlan.getActsLegs().get(outer))).setEndTime(now);
			
			double travelTime;
			for (int i=outer+1;i<=inner-1;i=i+2){
				((Leg)(basePlan.getActsLegs().get(i))).setDepTime(now);
				travelTime = this.estimator.getLegTravelTimeEstimation(basePlan.getPerson().getId(), now, (Act)(basePlan.getActsLegs().get(i-1)), (Act)(basePlan.getActsLegs().get(i+1)), (Leg)(basePlan.getActsLegs().get(i)));
				((Leg)(basePlan.getActsLegs().get(i))).setArrTime(now+travelTime);
				((Leg)(basePlan.getActsLegs().get(i))).setTravTime(travelTime);
				now+=travelTime;
				
				if (i!=inner-1){
					//((Act)(basePlan.getActsLegs().get(i+1))).setStartTime(now);
					now+=((Act)(basePlan.getActsLegs().get(i+1))).getDur();
					//((Act)(basePlan.getActsLegs().get(i+1))).setEndTime(now);					
				}
				else {
					//((Act)(basePlan.getActsLegs().get(i+1))).setStartTime(now);
					
					if (((Act)(basePlan.getActsLegs().get(i+1))).getEndTime()>now){
					//	((Act)(basePlan.getActsLegs().get(i+1))).setDur(((Act)(basePlan.getActsLegs().get(i+1))).getEndTime()-now);
					}
					else {
					//	((Act)(basePlan.getActsLegs().get(i+1))).setDur(0);
						//((Act)(basePlan.getActsLegs().get(i+1))).setEndTime(now);
						if (basePlan.getActsLegs().size()>i+3){
							travelTime = this.estimator.getLegTravelTimeEstimation(basePlan.getPerson().getId(), now, (Act)(basePlan.getActsLegs().get(i+1)), (Act)(basePlan.getActsLegs().get(i+3)), (Leg)(basePlan.getActsLegs().get(i+2)));
							((Leg)(basePlan.getActsLegs().get(i+2))).setArrTime(now+travelTime);
							((Leg)(basePlan.getActsLegs().get(i+2))).setTravTime(travelTime);
							now+=travelTime;
						//	((Act)(basePlan.getActsLegs().get(i+3))).setStartTime(now);
							if (((Act)(basePlan.getActsLegs().get(i+3))).getEndTime()>now){
						//		((Act)(basePlan.getActsLegs().get(i+3))).setDur(((Act)(basePlan.getActsLegs().get(i+3))).getEndTime()-now);
							}
							else return 1;
						}
						else return 1;
					}
				}
			}
			//log.info("Actslegs davor = "+basePlan.getActsLegs());
			basePlan.setScore(scorer.getScore(basePlan));
			//log.info("Score = "+scorer.getScore(basePlan));
			return 0;
		}
		// NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW
		else return this.swapDurations (basePlan, outer, inner);	
		// else return 1;
	}
	
	
	
	public int decreaseTime(PlanomatXPlan basePlan, int outer, int inner){
		
		if (((Act)(basePlan.getActsLegs().get(outer))).getDur()>=OFFSET+this.minimumTime){
			//((Act)(basePlan.getActsLegs().get(outer))).setDur(((Act)(basePlan.getActsLegs().get(outer))).getDur()-OFFSET);
			double now =((Act)(basePlan.getActsLegs().get(outer))).getEndTime()-OFFSET;
			//((Act)(basePlan.getActsLegs().get(outer))).setEndTime(now);
			
			double travelTime;
			for (int i=outer+1;i<=inner-1;i=i+2){
				((Leg)(basePlan.getActsLegs().get(i))).setDepTime(now);
				travelTime = this.estimator.getLegTravelTimeEstimation(basePlan.getPerson().getId(), now, (Act)(basePlan.getActsLegs().get(i-1)), (Act)(basePlan.getActsLegs().get(i+1)), (Leg)(basePlan.getActsLegs().get(i)));
				((Leg)(basePlan.getActsLegs().get(i))).setArrTime(now+travelTime);
				((Leg)(basePlan.getActsLegs().get(i))).setTravTime(travelTime);
				now+=travelTime;
				
				if (i!=inner-1){
					//((Act)(basePlan.getActsLegs().get(i+1))).setStartTime(now);
					now+=((Act)(basePlan.getActsLegs().get(i+1))).getDur();
					//((Act)(basePlan.getActsLegs().get(i+1))).setEndTime(now);					
				}
				else {
					//((Act)(basePlan.getActsLegs().get(i+1))).setStartTime(now);
					
					if (((Act)(basePlan.getActsLegs().get(i+1))).getEndTime()>now){
						//((Act)(basePlan.getActsLegs().get(i+1))).setDur(((Act)(basePlan.getActsLegs().get(i+1))).getEndTime()-now);
					}
					else {
						//((Act)(basePlan.getActsLegs().get(i+1))).setDur(0);
						//((Act)(basePlan.getActsLegs().get(i+1))).setEndTime(now);
						if (basePlan.getActsLegs().size()>i+3){
							travelTime = this.estimator.getLegTravelTimeEstimation(basePlan.getPerson().getId(), now, (Act)(basePlan.getActsLegs().get(i+1)), (Act)(basePlan.getActsLegs().get(i+3)), (Leg)(basePlan.getActsLegs().get(i+2)));
							((Leg)(basePlan.getActsLegs().get(i+2))).setArrTime(now+travelTime);
							((Leg)(basePlan.getActsLegs().get(i+2))).setTravTime(travelTime);
							now+=travelTime;
							//((Act)(basePlan.getActsLegs().get(i+3))).setStartTime(now);
							if (((Act)(basePlan.getActsLegs().get(i+3))).getEndTime()>now){
								//((Act)(basePlan.getActsLegs().get(i+3))).setDur(((Act)(basePlan.getActsLegs().get(i+3))).getEndTime()-now);
							}
							else return 1;
						}
						else return 1;
					}
				}
			}
			//log.info("Actslegs davor = "+basePlan.getActsLegs());
			basePlan.setScore(scorer.getScore(basePlan));
			//log.info("Score = "+scorer.getScore(basePlan));
			return 0;
		}
		// NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW
		else return this.swapDurations(basePlan, outer, inner);
		// else return 1;
	}
	
	
	// NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW
	public int swapDurations (PlanomatXPlan basePlan, int outer, int inner){
		
		double swaptime=((Act)(basePlan.getActsLegs().get(inner))).getDur();
		((Act)(basePlan.getActsLegs().get(outer))).setDur(swaptime);
		double now =((Act)(basePlan.getActsLegs().get(outer))).getStartTime()+swaptime;
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
	
	//////////////////////////////////////////////////////////////////////
	// Help methods 
	//////////////////////////////////////////////////////////////////////
	
	
	public Object[] checkForTabuSolutions (PlanomatXPlan [] neighbourhood, int [] notNewInNeighbourhood, 
			ArrayList<PlanomatXPlan> tabuList, int [][] moves, int[]position){
		
		boolean warningOuter = true;
		boolean warningInner = true;
		Object [] out = new Object [2];
		PlanomatXPlan plan = new PlanomatXPlan (neighbourhood[0].getPerson());
		plan.setScore(-100000);
		for (int i=0;i<neighbourhood.length;i++){
		//	stream.print(neighbourhood[i].getScore()+"\t"+notNewInNeighbourhood[i]+"\t");
			if (notNewInNeighbourhood[i]==0){
				for (int j=0;j<tabuList.size();j++){
					warningInner = checkForEquality (neighbourhood[i].getActsLegs(), tabuList.get(tabuList.size()-1-j).getActsLegs());
					if (warningInner) {
		//				stream.print("1\t");
						break;
					}
				}
				if (!warningInner) {
		//			stream.print("0\t");
					warningOuter = false;
					
					if (neighbourhood[i].getScore()>plan.getScore()){
						plan = neighbourhood[i];
						position[0]=moves[i][0];
						position[1]=moves[i][1];
					}
				}
			}
		//	else stream.print("1\t");
		//	stream.print(((Leg)(neighbourhood[i].getActsLegs().get(1))).getDepTime()+"\t");
		//	for (int z= 2;z<neighbourhood[i].getActsLegs().size()-1;z=z+2){
		//		stream.print((((Leg)(neighbourhood[i].getActsLegs().get(z+1))).getDepTime()-((Leg)(neighbourhood[i].getActsLegs().get(z-1))).getArrTime())+"\t");
		//	}
		//	stream.print(86400-((Leg)(neighbourhood[i].getActsLegs().get(neighbourhood[i].getActsLegs().size()-2))).getArrTime()+"\t");
		//	stream.println();
		}
		
		// clean-up of plan (=bestIterSolution)
		if (!warningOuter) this.cleanActs(plan);
		//log.info("Clean actslegs: "+plan.getActsLegs());
		
		out[0]=plan;
		out[1]=warningOuter;
		return out;
	}
	
	
	public boolean checkForEquality (ArrayList<Object> list1, ArrayList<Object> list2){
		
		if (list1.size()!=list2.size()){
			return false;
		}
		else{
			for (int i=1;i<list1.size()-1;i=i+2){
				//if (((Act)(list1.get(i))).getDur()!=((Act)(list2.get(i))).getDur()){
				if (((Leg)(list1.get(i))).getDepTime()!=((Leg)(list2.get(i))).getDepTime()	||	((Leg)(list1.get(i))).getArrTime()!=((Leg)(list2.get(i))).getArrTime()){
					return false;
				}
			}
			return true;
		}
	}	
	
	
	public double cleanSchedule (double now, Plan plan){
		
		((Act)(plan.getActsLegs().get(0))).setEndTime(now);
		((Act)(plan.getActsLegs().get(0))).setDur(now);
			
		double travelTime;
		for (int i=1;i<=plan.getActsLegs().size()-2;i=i+2){
			((Leg)(plan.getActsLegs().get(i))).setDepTime(now);
			travelTime = this.estimator.getLegTravelTimeEstimation(plan.getPerson().getId(), now, (Act)(plan.getActsLegs().get(i-1)), (Act)(plan.getActsLegs().get(i+1)), (Leg)(plan.getActsLegs().get(i)));
			((Leg)(plan.getActsLegs().get(i))).setArrTime(now+travelTime);
			((Leg)(plan.getActsLegs().get(i))).setTravTime(travelTime);
			now+=travelTime;
			
			if (i!=plan.getActsLegs().size()-2){
				((Act)(plan.getActsLegs().get(i+1))).setStartTime(now);
				travelTime = java.lang.Math.max(((Act)(plan.getActsLegs().get(i+1))).getDur()-travelTime, this.minimumTime);
				((Act)(plan.getActsLegs().get(i+1))).setDur(travelTime);	
				((Act)(plan.getActsLegs().get(i+1))).setEndTime(now+travelTime);	
				now+=travelTime;
			}
			else {
				((Act)(plan.getActsLegs().get(i+1))).setStartTime(now);
				
				if (86400>now){
					((Act)(plan.getActsLegs().get(i+1))).setDur(86400-now);
				}
				else {
					return now-86400;
				}
			}
		}
		return 0;
	}
		

	public void cleanActs (Plan plan){
		
		((Act)(plan.getActsLegs().get(0))).setEndTime(((Leg)(plan.getActsLegs().get(1))).getDepTime());
		((Act)(plan.getActsLegs().get(0))).setDur(((Leg)(plan.getActsLegs().get(1))).getDepTime());
		
		for (int i=2;i<=plan.getActsLegs().size()-1;i=i+2){
			
			if (i!=plan.getActsLegs().size()-1){
				((Act)(plan.getActsLegs().get(i))).setStartTime(((Leg)(plan.getActsLegs().get(i-1))).getArrTime());
				((Act)(plan.getActsLegs().get(i))).setEndTime(((Leg)(plan.getActsLegs().get(i+1))).getDepTime());
				((Act)(plan.getActsLegs().get(i))).setDur(((Leg)(plan.getActsLegs().get(i+1))).getDepTime()-((Leg)(plan.getActsLegs().get(i-1))).getArrTime());
				
			}
			else {
				((Act)(plan.getActsLegs().get(i))).setStartTime(((Leg)(plan.getActsLegs().get(i-1))).getArrTime());
				((Act)(plan.getActsLegs().get(i))).setDur(86400-((Leg)(plan.getActsLegs().get(i-1))).getArrTime());
	
			}
		}
	}
	
	
}
	
