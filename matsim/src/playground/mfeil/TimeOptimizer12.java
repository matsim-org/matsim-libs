/* *********************************************************************** *
 * project: org.matsim.*
 * TimeOptimizer12.java
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
import org.matsim.gbl.Gbl;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.apache.log4j.Logger;
import org.matsim.population.Plan;
import org.matsim.scoring.PlanScorer;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.controler.Controler;


/**
 * @author Matthias Feil
 * Like TimeOptimizer1! trying to take advantage of no copyPlan(). Well, does not give any runtime advantages for the moment...
 */

public class TimeOptimizer12 implements org.matsim.population.algorithms.PlanAlgorithm { 
	
	private final int						MAX_ITERATIONS, STOP_CRITERION, NEIGHBOURHOOD_SIZE;
	private int								OFFSET;
	private final double					minimumTime;
	private final PlanScorer 				scorer;
	private final LegTravelTimeEstimator	estimator;
	private static final Logger 			log = Logger.getLogger(TimeOptimizer12.class);
	
	//////////////////////////////////////////////////////////////////////
	// Constructor
	//////////////////////////////////////////////////////////////////////
	
	public TimeOptimizer12 (LegTravelTimeEstimator estimator, PlanScorer scorer){
		
		this.scorer 				= scorer;
		this.estimator				= estimator;
		this.OFFSET					= 1800;
		this.MAX_ITERATIONS 		= 30;
		this.STOP_CRITERION			= 5;
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
		ArrayList<?> [] InitialNeighbourhood 			= new ArrayList [neighbourhood_size];
		ArrayList<?> [] neighbourhood 					= new ArrayList [java.lang.Math.min(NEIGHBOURHOOD_SIZE, neighbourhood_size)];
		double [][] notNewInNeighbourhood 				= new double [neighbourhood_size][2];
		ArrayList<ArrayList<?>> tabuList			 	= new ArrayList<ArrayList<?>>();
		ArrayList<?> bestSolution						= new ArrayList<Object>();
		ArrayList<Object> bestIterSolution = 			new ArrayList<Object> ();
		int pointer;
		int currentIteration							= 1;
		int lastImprovement 							= 0;
		
		
	//	String outputfile = Controler.getOutputFilename("Timer_log"+Counter.timeOptCounter+"_"+plan.getPerson().getId()+".xls");
	//	Counter.timeOptCounter++;
	//	PrintStream stream;
	//	try {
	//		stream = new PrintStream (new File(outputfile));
	//	} catch (FileNotFoundException e) {
	//		e.printStackTrace();
	//		return;
	//	}
	//	stream.print(plan.getScore()+"\t\t\t");
	//	for (int z= 0;z<plan.getActsLegs().size();z=z+2){
	//	Act act = (Act)plan.getActsLegs().get(z);
	//		stream.print(act.getType()+"\t");
	//	}
	//	stream.println();
	//	stream.print("\t\t\t");
	//	for (int z= 0;z<plan.getActsLegs().size();z=z+2){
	//		stream.print(((Act)(plan.getActsLegs()).get(z)).getDur()+"\t");
	//	}
	//	stream.println();
		
		
		// Copy the plan into all fields of the array neighbourhood
		for (int i = 0; i < InitialNeighbourhood.length; i++){
			InitialNeighbourhood[i] = this.copyActsLegs(plan.getActsLegs());
		}
		
		//((Leg)(InitialNeighbourhood[0].get(1))).setTravTime(0);
		//for (int i=0;i<InitialNeighbourhood.length;i++){
			//System.out.println(((Leg)(InitialNeighbourhood[i].get(1))).getTravTime());
		//}
		
		// Write the given plan into the tabuList
		tabuList.add(new ArrayList<Object> (plan.getActsLegs()));
		//tabuList.get(0).copyPlan((PlanomatXPlan)plan);
		
		//Set the given plan as bestSolution
		bestSolution = this.copyActsLegs(plan.getActsLegs());
		double bestScore = plan.getScore();
		
		// Iteration 1
	//	stream.println("Iteration "+1);
		this.createInitialNeighbourhood((PlanomatXPlan)plan, InitialNeighbourhood, notNewInNeighbourhood, moves);
		
		//for (int i=0;i<InitialNeighbourhood.length;i++){
		//	System.out.println("Solution "+i+" = "+((Leg)(InitialNeighbourhood[i].get(1))).getDepTime()+" "+((Leg)(InitialNeighbourhood[i].get(3))).getDepTime()+" "+((Leg)(InitialNeighbourhood[i].get(5))).getDepTime()+" "+((Leg)(InitialNeighbourhood[i].get(7))).getDepTime());
		//}
		
		// Check whether there are new solutions (is only necessary if there is no swap = versions 8 and 10)
		boolean warningNotNew=true;
		for (int i = 0; i<neighbourhood.length;i++){
			if (notNewInNeighbourhood[i][0]==0) {
				warningNotNew = false;
				break;
			}
		}
		if (warningNotNew==true) {
			log.info("No new solution at TimeOptimizer iteration "+currentIteration);
			return;
		}
		
		pointer = this.checkForTabuSolutions (InitialNeighbourhood, notNewInNeighbourhood, tabuList, moves, position);
		
		bestIterSolution = this.copyActsLegs(InitialNeighbourhood[pointer]);		
		tabuList.add(bestIterSolution);
	//	for (int i=0;i<tabuList.size();i++){
	//		stream.print(((Leg)(tabuList.get(i).get(1))).getDepTime()+"\t");
	//		for (int z=2;z<tabuList.get(i).size()-1;z+=2){
	//			stream.print((((Leg)(tabuList.get(i).get(z+1))).getDepTime()-((Leg)(tabuList.get(i).get(z-1))).getArrTime())+"\t");
	//		}
	//		stream.print(86400-(((Leg)(tabuList.get(i).get(tabuList.get(i).size()-2))).getArrTime())+"\t");
	//		stream.println();
	//	}
		//stream.println("Best score \t"+((PlanomatXPlan)solutions[0]).getScore());
		
		if (notNewInNeighbourhood[pointer][1]>bestScore){
			bestSolution = this.copyActsLegs((ArrayList<?>)InitialNeighbourhood[pointer]);
			bestScore=notNewInNeighbourhood[pointer][1];
			lastImprovement = 0;
		}
		else {
			lastImprovement++;
		}
		for (int i = 0;i<neighbourhood.length; i++){
			neighbourhood[i] = this.copyActsLegs((ArrayList<?>)InitialNeighbourhood[pointer]);
		}
		
		
		// Do Tabu Search iterations
		for (currentIteration = 2; currentIteration<=MAX_ITERATIONS;currentIteration++){
			
	//		stream.println("Iteration "+currentIteration);
			
			// Define the neighbourhood			
			this.createNeighbourhood((PlanomatXPlan)plan, neighbourhood, notNewInNeighbourhood, moves, position);
			
			// Check whether there are new solutions (is only necessary if there is no swap = versions 8 and 10)
			warningNotNew=true;
			for (int i = 0; i<neighbourhood.length;i++){
				if (notNewInNeighbourhood[i][0]==0) {
					warningNotNew = false;
					break;
				}
			}
			if (warningNotNew==true) {
				log.info("No new solution at TimeOptimizer iteration "+currentIteration);
				break;
			}
						
			// Check whether plans are tabu	
			pointer = this.checkForTabuSolutions (neighbourhood, notNewInNeighbourhood, tabuList, moves, position);
			
			if (pointer==-1) {
				log.info("No non-tabu solutions found for person "+plan.getPerson().getId()+" at iteration "+currentIteration);
				break;
			}
		
			// Find best non-tabu plan. Becomes this iteration's solution. Write it into the tabuList			
			bestIterSolution = this.copyActsLegs(InitialNeighbourhood[pointer]);			
			tabuList.add(bestIterSolution);
	//		for (int i=0;i<tabuList.size();i++){
	//			stream.print(((Leg)(tabuList.get(i).get(1))).getDepTime()+"\t");
	//			for (int z=2;z<tabuList.get(i).size()-1;z+=2){
	//				stream.print((((Leg)(tabuList.get(i).get(z+1))).getDepTime()-((Leg)(tabuList.get(i).get(z-1))).getArrTime())+"\t");
	//			}
	//			stream.print(86400-(((Leg)(tabuList.get(i).get(tabuList.get(i).size()-2))).getArrTime())+"\t");
	//			stream.println();
	//		}
			if (notNewInNeighbourhood[pointer][1]>bestScore){
				bestSolution = this.copyActsLegs((ArrayList<?>)neighbourhood[pointer]);
				bestScore=notNewInNeighbourhood[pointer][1];
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
					neighbourhood[i] = this.copyActsLegs((ArrayList<?>)neighbourhood[pointer]);
				}
			}				
		}
	
		// Update the plan with the final solution 		
		//
		//stream.println("Selected solution\t"+bestSolution.getScore());
		ArrayList<Object> al = plan.getActsLegs();
		plan.setScore(bestScore);
		
		for (int i = 0; i<al.size();i++){
			al.remove(i);
			al.add(i, bestSolution.get(i));	
		}
		//log.info("Person "+plan.getPerson().getId()+" runtime: "+(System.currentTimeMillis()-runStartTime));
		
	}
	
	//////////////////////////////////////////////////////////////////////
	// Neighbourhood definition 
	//////////////////////////////////////////////////////////////////////
	
	public void createInitialNeighbourhood (PlanomatXPlan plan, ArrayList<?> [] neighbourhood, double[][] notNewInNeighbourhood,
			int [][] moves) {
		
		int pos = 0;
		for (int outer=0;outer<neighbourhood[0].size()-2;outer+=2){
			for (int inner=outer+2;inner<neighbourhood[0].size();inner+=2){
				
				notNewInNeighbourhood[pos][0]=this.increaseTime(plan, neighbourhood[pos], notNewInNeighbourhood[pos],outer, inner);
				moves [pos][0]=outer;
				moves [pos][1]=inner;
				pos++;
				
				notNewInNeighbourhood[pos][0]=this.decreaseTime(plan, neighbourhood[pos], notNewInNeighbourhood[pos], outer, inner);
				moves [pos][0]=inner;
				moves [pos][1]=outer;
				pos++;
				
			}
		}
	}
	
	
	public void createNeighbourhood (PlanomatXPlan plan, ArrayList<?> [] neighbourhood, double[][] notNewInNeighbourhood, int[][] moves, int[]position) {
		
		int pos = 0;
		int fieldLength = neighbourhood.length/3;
				
			for (int outer=java.lang.Math.max(position[0]-(fieldLength/2)*2,0);outer<position[0];outer+=2){
				notNewInNeighbourhood[pos][0]=this.decreaseTime(plan, neighbourhood[pos], notNewInNeighbourhood[pos], outer, position[0]);
				moves [pos][0]=position[0];
				moves [pos][1]=outer;
				pos++;
			}
		
			OuterLoop1:
				for (int outer=position[0];outer<neighbourhood[0].size()-2;outer+=2){
					for (int inner=outer+2;inner<neighbourhood[0].size();inner+=2){
						notNewInNeighbourhood[pos][0]=this.increaseTime(plan, neighbourhood[pos], notNewInNeighbourhood[pos], outer, inner);
						moves [pos][0]=outer;
						moves [pos][1]=inner;
						pos++;
						
						if (pos>=fieldLength) break OuterLoop1;
					}
				}
		
			for (int outer=java.lang.Math.max(position[1]-(fieldLength/2)*2,0);outer<position[1];outer+=2){
				
				if (outer!=position[0]){
					notNewInNeighbourhood[pos][0]=this.increaseTime(plan, neighbourhood[pos], notNewInNeighbourhood[pos], outer, position[1]);
					moves [pos][0]=outer;
					moves [pos][1]=position[1];
					pos++;
				}
			}
		
			OuterLoop2:
				for (int outer=position[1];outer<neighbourhood[0].size()-2;outer+=2){
					for (int inner=outer+2;inner<neighbourhood[0].size();inner+=2){
						notNewInNeighbourhood[pos][0]=this.decreaseTime(plan, neighbourhood[pos], notNewInNeighbourhood[pos], outer, inner);
						moves [pos][0]=inner;
						moves [pos][1]=outer;
						pos++;
						
						if (pos>=fieldLength*2) break OuterLoop2;
					}
				}
		
		
			OuterLoop3:
				for (int outer=0;outer<neighbourhood[0].size()-2;outer=outer+2){
					for (int inner=outer+2;inner<neighbourhood[0].size();inner=inner+2){
						
						if (outer!=position[0]	&&	inner!=position[1]){
							if (position[0]<position[1]){
								notNewInNeighbourhood[pos][0]=this.increaseTime(plan, neighbourhood[pos], notNewInNeighbourhood[pos], outer, inner);
								moves [pos][0]=outer;
								moves [pos][1]=inner;
								pos++;
								if (pos>neighbourhood.length-1) break OuterLoop3;
							}
							else if (inner!=position[0]	||	outer!=position[1]){
								notNewInNeighbourhood[pos][0]=this.increaseTime(plan, neighbourhood[pos], notNewInNeighbourhood[pos], outer, inner);
								moves [pos][0]=outer;
								moves [pos][1]=inner;
								pos++;
								if (pos>neighbourhood.length-1) break OuterLoop3;
							}
						}
					
						if (inner!=position[0]	&&	outer!=position[1]){
							if (position[0]>position[1]){
								notNewInNeighbourhood[pos][0]=this.decreaseTime(plan, neighbourhood[pos], notNewInNeighbourhood[pos], outer, inner);
								moves [pos][0]=inner;
								moves [pos][1]=outer;
								pos++;
								if (pos>neighbourhood.length-1) break OuterLoop3;
							}
							else if (outer!=position[0]	||	inner!=position[1]){
								notNewInNeighbourhood[pos][0]=this.decreaseTime(plan, neighbourhood[pos], notNewInNeighbourhood[pos], outer, inner);
								moves [pos][0]=inner;
								moves [pos][1]=outer;
								pos++;
								if (pos>neighbourhood.length-1) break OuterLoop3;
							}
						}
					}
				}		
	}
	
	
	
	public int increaseTime(PlanomatXPlan plan, ArrayList<?> actslegs, double[] score, int outer, int inner){
		
		if (((Act)(actslegs.get(inner))).getDur()>=(OFFSET+this.minimumTime)){
			//((Act)(basePlan.getActsLegs().get(outer))).setDur(((Act)(basePlan.getActsLegs().get(outer))).getDur()+OFFSET);
			double now =((Act)(actslegs.get(outer))).getEndTime()+OFFSET;
			//((Act)(basePlan.getActsLegs().get(outer))).setEndTime(now);
			
			double travelTime;
			for (int i=outer+1;i<=inner-1;i=i+2){
				((Leg)(actslegs.get(i))).setDepTime(now);
				travelTime = this.estimator.getLegTravelTimeEstimation(plan.getPerson().getId(), now, (Act)(actslegs.get(i-1)), (Act)(actslegs.get(i+1)), (Leg)(actslegs.get(i)));
				((Leg)(actslegs.get(i))).setArrTime(now+travelTime);
				((Leg)(actslegs.get(i))).setTravTime(travelTime);
				now+=travelTime;
				
				if (i!=inner-1){
					//((Act)(basePlan.getActsLegs().get(i+1))).setStartTime(now);
					now+=((Act)(actslegs.get(i+1))).getDur();
					//((Act)(basePlan.getActsLegs().get(i+1))).setEndTime(now);					
				}
				else {
					//((Act)(basePlan.getActsLegs().get(i+1))).setStartTime(now);
					
					if (((Act)(actslegs.get(i+1))).getEndTime()>now){
					//	((Act)(basePlan.getActsLegs().get(i+1))).setDur(((Act)(basePlan.getActsLegs().get(i+1))).getEndTime()-now);
					}
					else {
					//	((Act)(basePlan.getActsLegs().get(i+1))).setDur(0);
						//((Act)(basePlan.getActsLegs().get(i+1))).setEndTime(now);
						if (actslegs.size()>i+3){
							travelTime = this.estimator.getLegTravelTimeEstimation(plan.getPerson().getId(), now, (Act)(actslegs.get(i+1)), (Act)(actslegs.get(i+3)), (Leg)(actslegs.get(i+2)));
							((Leg)(actslegs.get(i+2))).setArrTime(now+travelTime);
							((Leg)(actslegs.get(i+2))).setTravTime(travelTime);
							now+=travelTime;
						//	((Act)(basePlan.getActsLegs().get(i+3))).setStartTime(now);
							if (((Act)(actslegs.get(i+3))).getEndTime()>now){
						//		((Act)(basePlan.getActsLegs().get(i+3))).setDur(((Act)(basePlan.getActsLegs().get(i+3))).getEndTime()-now);
							}
							else return 1;
						}
						else return 1;
					}
				}
			}
			//log.info("Actslegs davor = "+basePlan.getActsLegs());
			plan.setActsLegs((ArrayList<Object>)actslegs);
			score[1]= scorer.getScore(plan);
			//log.info("Score = "+scorer.getScore(basePlan));
			return 0;
		}
		// NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW
		else return this.swapDurations (plan, actslegs, score, outer, inner);	
		// else return 1;
	}
	
	
	
	public int decreaseTime(PlanomatXPlan plan, ArrayList<?> actslegs, double[]score, int outer, int inner){
		
		if (((Act)(actslegs.get(outer))).getDur()>=OFFSET+this.minimumTime){
			//((Act)(basePlan.getActsLegs().get(outer))).setDur(((Act)(basePlan.getActsLegs().get(outer))).getDur()-OFFSET);
			double now =((Act)(actslegs.get(outer))).getEndTime()-OFFSET;
			//((Act)(basePlan.getActsLegs().get(outer))).setEndTime(now);
			
			double travelTime;
			for (int i=outer+1;i<=inner-1;i=i+2){
				((Leg)(actslegs.get(i))).setDepTime(now);
				travelTime = this.estimator.getLegTravelTimeEstimation(plan.getPerson().getId(), now, (Act)(actslegs.get(i-1)), (Act)(actslegs.get(i+1)), (Leg)(actslegs.get(i)));
				((Leg)(actslegs.get(i))).setArrTime(now+travelTime);
				((Leg)(actslegs.get(i))).setTravTime(travelTime);
				now+=travelTime;
				
				if (i!=inner-1){
					//((Act)(basePlan.getActsLegs().get(i+1))).setStartTime(now);
					now+=((Act)(actslegs.get(i+1))).getDur();
					//((Act)(basePlan.getActsLegs().get(i+1))).setEndTime(now);					
				}
				else {
					//((Act)(basePlan.getActsLegs().get(i+1))).setStartTime(now);
					
					if (((Act)(actslegs.get(i+1))).getEndTime()>now){
						//((Act)(basePlan.getActsLegs().get(i+1))).setDur(((Act)(basePlan.getActsLegs().get(i+1))).getEndTime()-now);
					}
					else {
						//((Act)(basePlan.getActsLegs().get(i+1))).setDur(0);
						//((Act)(basePlan.getActsLegs().get(i+1))).setEndTime(now);
						if (actslegs.size()>i+3){
							travelTime = this.estimator.getLegTravelTimeEstimation(plan.getPerson().getId(), now, (Act)(actslegs.get(i+1)), (Act)(actslegs.get(i+3)), (Leg)(actslegs.get(i+2)));
							((Leg)(actslegs.get(i+2))).setArrTime(now+travelTime);
							((Leg)(actslegs.get(i+2))).setTravTime(travelTime);
							now+=travelTime;
							//((Act)(basePlan.getActsLegs().get(i+3))).setStartTime(now);
							if (((Act)(actslegs.get(i+3))).getEndTime()>now){
								//((Act)(basePlan.getActsLegs().get(i+3))).setDur(((Act)(basePlan.getActsLegs().get(i+3))).getEndTime()-now);
							}
							else return 1;
						}
						else return 1;
					}
				}
			}
			//log.info("Actslegs davor = "+basePlan.getActsLegs());
			plan.setActsLegs((ArrayList<Object>)actslegs);
			score[1]= scorer.getScore(plan);
			//log.info("Score = "+scorer.getScore(basePlan));
			return 0;
		}
		// NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW
		else return this.swapDurations(plan, actslegs, score, outer, inner);
		// else return 1;
	}
	
	
	// NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW
	public int swapDurations (PlanomatXPlan plan, ArrayList<?> basePlan, double[]score, int outer, int inner){
		
		double swaptime=((Act)(basePlan.get(inner))).getDur();
		((Act)(basePlan.get(outer))).setDur(swaptime);
		double now =((Act)(basePlan.get(outer))).getStartTime()+swaptime;
		((Act)(basePlan.get(outer))).setEndTime(now);
		
		double travelTime;
		for (int i=outer+1;i<=inner-1;i=i+2){
			((Leg)(basePlan.get(i))).setDepTime(now);
			travelTime = this.estimator.getLegTravelTimeEstimation(plan.getPerson().getId(), now, (Act)(basePlan.get(i-1)), (Act)(basePlan.get(i+1)), (Leg)(basePlan.get(i)));
			((Leg)(basePlan.get(i))).setArrTime(now+travelTime);
			((Leg)(basePlan.get(i))).setTravTime(travelTime);
			now+=travelTime;
			
			if (i!=inner-1){
				((Act)(basePlan.get(i+1))).setStartTime(now);
				now+=((Act)(basePlan.get(i+1))).getDur();
				((Act)(basePlan.get(i+1))).setEndTime(now);					
			}
			else {
				((Act)(basePlan.get(i+1))).setStartTime(now);
				
				if (((Act)(basePlan.get(i+1))).getEndTime()>now){
					((Act)(basePlan.get(i+1))).setDur(((Act)(basePlan.get(i+1))).getEndTime()-now);
				}
				else {
					((Act)(basePlan.get(i+1))).setDur(0);
					((Act)(basePlan.get(i+1))).setEndTime(now);
					if (basePlan.size()>i+3){
						travelTime = this.estimator.getLegTravelTimeEstimation(plan.getPerson().getId(), now, (Act)(basePlan.get(i+1)), (Act)(basePlan.get(i+3)), (Leg)(basePlan.get(i+2)));
						((Leg)(basePlan.get(i+2))).setArrTime(now+travelTime);
						((Leg)(basePlan.get(i+2))).setTravTime(travelTime);
						now+=travelTime;
						((Act)(basePlan.get(i+3))).setStartTime(now);
						if (((Act)(basePlan.get(i+3))).getEndTime()>now){
							((Act)(basePlan.get(i+3))).setDur(((Act)(basePlan.get(i+3))).getEndTime()-now);
						}
						else return 1;
					}
					else return 1;
				}
			}
		}
		
		plan.setActsLegs((ArrayList<Object>)basePlan);
		score[1]= scorer.getScore(plan);
		return 0;
		
	}
	
	//////////////////////////////////////////////////////////////////////
	// Help methods 
	//////////////////////////////////////////////////////////////////////
	
	
	public int checkForTabuSolutions (ArrayList<?> [] neighbourhood, double[][] notNewInNeighbourhood, 
			ArrayList<ArrayList<?>> tabuList, int [][] moves, int[]position){
		
		boolean warningInner = true;
		int pointer=-1;
		ArrayList<?> actslegs = new ArrayList<Object>();
		double score =-100000;
		for (int i=0;i<neighbourhood.length;i++){
	//		stream.print(notNewInNeighbourhood[i][1]+"\t"+notNewInNeighbourhood[i][0]+"\t");
			if (notNewInNeighbourhood[i][0]==0){
				for (int j=0;j<tabuList.size();j++){
					warningInner = checkForEquality (neighbourhood[i], tabuList.get(tabuList.size()-1-j));
					if (warningInner) {
	//					stream.print("1\t");
						break;
					}
				}
				if (!warningInner) {
	//				stream.print("0\t");
					
					if (notNewInNeighbourhood[i][1]>score){
						actslegs = neighbourhood[i];
						score=notNewInNeighbourhood[i][1];
						pointer=i;
						position[0]=moves[i][0];
						position[1]=moves[i][1];
					}
				}
			}
	//		else stream.print("1\t");
			
	//		stream.print(((Leg)(neighbourhood[i].get(1))).getDepTime()+"\t");
	//		for (int z= 2;z<neighbourhood[i].size()-1;z=z+2){
	//			stream.print((((Leg)(neighbourhood[i].get(z+1))).getDepTime()-((Leg)(neighbourhood[i].get(z-1))).getArrTime())+"\t");
	//		}
	//		stream.print(86400-((Leg)(neighbourhood[i].get(neighbourhood[i].size()-2))).getArrTime()+"\t");
	//		stream.println();
		}
	//	stream.println("Iteration's best score\t"+score+"\t"+notNewInNeighbourhood[pointer][1]);
		// clean-up of plan (=bestIterSolution)
		if (pointer!=-1) this.cleanActs(actslegs);
		
		return pointer;
	}
	
	
	public boolean checkForEquality (ArrayList<?> list1, ArrayList<?> list2){
		
		if (list1.size()!=list2.size()){
			return false;
		}
		else{
			for (int i=1;i<list1.size()-1;i=i+2){
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
		

	public void cleanActs (ArrayList<?> actslegs){
		
		((Act)(actslegs.get(0))).setEndTime(((Leg)(actslegs.get(1))).getDepTime());
		((Act)(actslegs.get(0))).setDur(((Leg)(actslegs.get(1))).getDepTime());
		
		for (int i=2;i<=actslegs.size()-1;i=i+2){
			
			if (i!=actslegs.size()-1){
				((Act)(actslegs.get(i))).setStartTime(((Leg)(actslegs.get(i-1))).getArrTime());
				((Act)(actslegs.get(i))).setEndTime(((Leg)(actslegs.get(i+1))).getDepTime());
				((Act)(actslegs.get(i))).setDur(((Leg)(actslegs.get(i+1))).getDepTime()-((Leg)(actslegs.get(i-1))).getArrTime());
				
			}
			else {
				((Act)(actslegs.get(i))).setStartTime(((Leg)(actslegs.get(i-1))).getArrTime());
				((Act)(actslegs.get(i))).setDur(86400-((Leg)(actslegs.get(i-1))).getArrTime());
	
			}
		}
	}

	
	public ArrayList<Object> copyActsLegs (ArrayList<?> in){
		
			ArrayList<Object> out = new ArrayList<Object>();
			
			for (int i= 0; i< in.size() ; i++) {
				try {
					if (i % 2 == 0) {
						// activity
						Act a = new Act ((Act)in.get(i));
						out.add(a);
					} else {
						// Leg
						Leg inl = ((Leg) in.get(i));
						Leg l = new Leg (inl.getMode());
						l.setArrTime(inl.getArrTime());
						l.setDepTime(inl.getDepTime());
						//l.setTravTime(inl.getTravTime());
						l.setRoute(((Leg)in.get(i)).getRoute());
						out.add(l);
					}
				} catch (Exception e) {
					// copying a plan is fairly basic. if an exception occurs here, something
					// must be definitively wrong -- exit with an error
					Gbl.errorMsg(e);
				}
			}
		return out;
	}

}
	

	
