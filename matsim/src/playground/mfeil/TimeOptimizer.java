/* *********************************************************************** *
 * project: org.matsim.*
 * TimeOptimizer.java
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

import java.util.ArrayList;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.apache.log4j.Logger;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.scoring.PlanScorer;
import org.matsim.controler.Controler;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Plan;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.File;
import java.io.FileNotFoundException;


/**
 * @author Matthias Feil
 * This is a lean extension of the TimeModeChoicer excluding mode choice -> time optimization only.
 * However includes initial clean-up of the schedule.
 */

public class TimeOptimizer extends TimeModeChoicer1 implements PlanAlgorithm { 
	
	protected int						MAX_ITERATIONS, STOP_CRITERION, NEIGHBOURHOOD_SIZE;
	protected int						OFFSET;
	protected final static Logger 		log = Logger.getLogger(TimeOptimizer.class);
	
	//////////////////////////////////////////////////////////////////////
	// Constructor
	//////////////////////////////////////////////////////////////////////
	
	public TimeOptimizer (LegTravelTimeEstimator estimator, PlanScorer scorer){
		
		super(estimator, scorer);
		
		this.OFFSET					= 1800;
		this.MAX_ITERATIONS 		= 30;
		this.STOP_CRITERION			= 5;
		this.NEIGHBOURHOOD_SIZE		= 10;
		
		//TODO @MF: constants to be configured externally
	}
	
		
	//////////////////////////////////////////////////////////////////////
	// run() method
	//////////////////////////////////////////////////////////////////////
	
	
	public void run (Plan basePlan){
		
		if (basePlan.getActsLegs().size()==1) return;
		
		
		/* Initial clean-up of plan for the case actslegs is not sound*/
		double move = this.cleanSchedule (((Act)(basePlan.getActsLegs().get(0))).getEndTime(), basePlan);
		int loops=1;
		while (move!=0.0){
			if (loops>3) {
				for (int i=0;i<basePlan.getActsLegs().size()-2;i+=2){
					((Act)basePlan.getActsLegs().get(i)).setDuration(this.minimumTime);
				}
				move = this.cleanSchedule(this.minimumTime, basePlan);
				if (move!=0.0){
					// TODO Check whether allowed?
					basePlan.setScore(-100000);	// Like this, PlanomatX will see that the solution is no proper solution
					log.warn("No valid initial solution found for person "+basePlan.getPerson().getId()+"!");
					return;
				}
				else break;
			}
			loops++;
			for (int i=0;i<basePlan.getActsLegs().size()-2;i=i+2){
				((Act)basePlan.getActsLegs().get(i)).setDuration(java.lang.Math.max(((Act)basePlan.getActsLegs().get(i)).getDuration()*0.9, this.minimumTime));
			}
			move = this.cleanSchedule(((Act)(basePlan.getActsLegs().get(0))).getDuration(), basePlan);
		}
		
		this.processPlan(basePlan);
	}
		
	protected void processPlan (Plan basePlan){
		// TODO Check whether allowed?
		basePlan.setScore(this.scorer.getScore(basePlan));	
		
		/* TODO: just as long as PlanomatXPlan exists. Needs then to be removed!!! */
		PlanomatXPlan plan = new PlanomatXPlan (basePlan.getPerson());
		plan.copyPlan(basePlan);
		
		/* Initializing */ 
		int neighbourhood_size = 0;
		for (int i = plan.getActsLegs().size()-1;i>0;i=i-2){
			neighbourhood_size += i;
		}
		int [][] moves 									= new int [neighbourhood_size][2];
		int [] position									= new int [2];
		ArrayList<?> [] initialNeighbourhood 			= new ArrayList [neighbourhood_size];
		ArrayList<?> [] neighbourhood 					= new ArrayList [java.lang.Math.min(NEIGHBOURHOOD_SIZE, neighbourhood_size)];
		double []score					 				= new double [neighbourhood_size];
		ArrayList<?> bestSolution						= new ArrayList<Object>();
		int pointer;
		int currentIteration							= 1;
		int lastImprovement 							= 0;
		
		/*
		String outputfile = Controler.getOutputFilename("Timer_log"+Counter.timeOptCounter+"_"+plan.getPerson().getId()+".xls");
		Counter.timeOptCounter++;
		PrintStream stream;
		try {
			stream = new PrintStream (new File(outputfile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		stream.print(plan.getScore()+"\t");
		for (int z= 0;z<plan.getActsLegs().size();z=z+2){
		Act act = (Act)plan.getActsLegs().get(z);
			stream.print(act.getType()+"\t");
		}
		stream.println();
		stream.print("\t");
		for (int z= 0;z<plan.getActsLegs().size();z=z+2){
			stream.print(((Act)(plan.getActsLegs()).get(z)).getDuration()+"\t");
		}
		stream.println();
		*/
		
		/* Copy the plan into all fields of the array neighbourhood */
		for (int i = 0; i < initialNeighbourhood.length; i++){
			initialNeighbourhood[i] = this.copyActsLegs(plan.getActsLegs());
		}
		
		/* Set the given plan as bestSolution */
		bestSolution = this.copyActsLegs(plan.getActsLegs());
		double bestScore = plan.getScore();
		
		
		/* Iteration 1 */
		this.createInitialNeighbourhood((PlanomatXPlan)plan, initialNeighbourhood, score, moves);
		
		pointer = this.findBestSolution (initialNeighbourhood, score, moves, position);
		
		if (score[pointer]>bestScore){
			bestSolution = this.copyActsLegs((ArrayList<?>)initialNeighbourhood[pointer]);
			bestScore=score[pointer];
			lastImprovement = 0;
		}
		else {
			lastImprovement++;
		}
		for (int i = 0;i<neighbourhood.length; i++){
			neighbourhood[i] = this.copyActsLegs((ArrayList<?>)initialNeighbourhood[pointer]);
		}
		
		
		/* Do Tabu Search iterations */
		for (currentIteration = 2; currentIteration<=MAX_ITERATIONS;currentIteration++){
			
	//		stream.println("Iteration "+currentIteration);
			
			this.createNeighbourhood((PlanomatXPlan)plan, neighbourhood, score, moves, position);
			pointer = this.findBestSolution (neighbourhood, score, moves, position);
			
			if (pointer==-1) {
				log.info("No valid solutions found for person "+plan.getPerson().getId()+" at iteration "+currentIteration);
				break;
			}
		
			if (score[pointer]>bestScore){
				bestSolution = this.copyActsLegs((ArrayList<?>)neighbourhood[pointer]);
				bestScore=score[pointer];
				lastImprovement = 0;
			}
			else {
				lastImprovement++;
				if (lastImprovement > STOP_CRITERION) break;
			}
			
			if (this.MAX_ITERATIONS!=currentIteration){			
				for (int i = 0;i<neighbourhood.length; i++){
					neighbourhood[i] = this.copyActsLegs((ArrayList<?>)neighbourhood[pointer]);
				}
			}
		}
		
		
		/* Update the plan with the final solution */ 		
	//	stream.println("Selected solution\t"+bestScore);
		ArrayList<Object> al = basePlan.getActsLegs();
		basePlan.setScore(bestScore);
		
		double time;
		for (int i = 0; i<al.size();i++){
			if (i%2==0){
				time = ((Act)(bestSolution.get(i))).getDuration();
				((Act)al.get(i)).setDuration(time);
				time = ((Act)(bestSolution.get(i))).getStartTime();
				((Act)al.get(i)).setStartTime(time);
				time = ((Act)(bestSolution.get(i))).getEndTime();
				((Act)al.get(i)).setEndTime(time);
			}
			else {
				time = ((Leg)(bestSolution.get(i))).getTravelTime();
				((Leg)al.get(i)).setTravelTime(time);
				time = ((Leg)(bestSolution.get(i))).getDepartureTime();
				((Leg)al.get(i)).setDepartureTime(time);
				time = ((Leg)(bestSolution.get(i))).getArrivalTime();
				((Leg)al.get(i)).setArrivalTime(time);
			}
		}
		
		/* reset legEstimator (clear hash map) */
		this.estimator.reset();
	}
	
	//////////////////////////////////////////////////////////////////////
	// Neighbourhood definition 
	//////////////////////////////////////////////////////////////////////
	
	private void createInitialNeighbourhood (PlanomatXPlan plan, ArrayList<?> [] neighbourhood, double[]score, int [][] moves) {
		
		int pos = 0;
		for (int outer=0;outer<neighbourhood[0].size()-2;outer+=2){
			for (int inner=outer+2;inner<neighbourhood[0].size();inner+=2){
				
				score[pos]=this.increaseTime(plan, neighbourhood[pos], outer, inner);
				moves [pos][0]=outer;
				moves [pos][1]=inner;
				pos++;
				
				score[pos]=this.decreaseTime(plan, neighbourhood[pos], outer, inner);
				moves [pos][0]=inner;
				moves [pos][1]=outer;
				pos++;
				
			}
		}
	}
	
	
	private void createNeighbourhood (PlanomatXPlan plan, ArrayList<?> [] neighbourhood, double[]score, int[][] moves, int[]position) {
		
		int pos = 0;
		int fieldLength = neighbourhood.length/3;
				
			for (int outer=java.lang.Math.max(position[0]-(fieldLength/2)*2,0);outer<position[0];outer+=2){
				score[pos]=this.decreaseTime(plan, neighbourhood[pos], outer, position[0]);
				moves [pos][0]=position[0];
				moves [pos][1]=outer;
				pos++;
			}
		
			OuterLoop1:
				for (int outer=position[0];outer<neighbourhood[0].size()-2;outer+=2){
					for (int inner=outer+2;inner<neighbourhood[0].size();inner+=2){
						score[pos]=this.increaseTime(plan, neighbourhood[pos], outer, inner);
						moves [pos][0]=outer;
						moves [pos][1]=inner;
						pos++;
						
						if (pos>=fieldLength) break OuterLoop1;
					}
				}
		
			for (int outer=java.lang.Math.max(position[1]-(fieldLength/2)*2,0);outer<position[1];outer+=2){
				
				if (outer!=position[0]){
					score[pos]=this.increaseTime(plan, neighbourhood[pos], outer, position[1]);
					moves [pos][0]=outer;
					moves [pos][1]=position[1];
					pos++;
				}
			}
		
			OuterLoop2:
				for (int outer=position[1];outer<neighbourhood[0].size()-2;outer+=2){
					for (int inner=outer+2;inner<neighbourhood[0].size();inner+=2){
						score[pos]=this.decreaseTime(plan, neighbourhood[pos], outer, inner);
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
								score[pos]=this.increaseTime(plan, neighbourhood[pos], outer, inner);
								moves [pos][0]=outer;
								moves [pos][1]=inner;
								pos++;
								if (pos>neighbourhood.length-1) break OuterLoop3;
							}
							else if (inner!=position[0]	||	outer!=position[1]){
								score[pos]=this.increaseTime(plan, neighbourhood[pos], outer, inner);
								moves [pos][0]=outer;
								moves [pos][1]=inner;
								pos++;
								if (pos>neighbourhood.length-1) break OuterLoop3;
							}
						}
					
						if (inner!=position[0]	&&	outer!=position[1]){
							if (position[0]>position[1]){
								score[pos]=this.decreaseTime(plan, neighbourhood[pos], outer, inner);
								moves [pos][0]=inner;
								moves [pos][1]=outer;
								pos++;
								if (pos>neighbourhood.length-1) break OuterLoop3;
							}
							else if (outer!=position[0]	||	inner!=position[1]){
								score[pos]=this.decreaseTime(plan, neighbourhood[pos], outer, inner);
								moves [pos][0]=inner;
								moves [pos][1]=outer;
								pos++;
								if (pos>neighbourhood.length-1) break OuterLoop3;
							}
						}
					}
				}		
	}
	
	
	
	private double increaseTime(PlanomatXPlan plan, ArrayList<?> actslegs, int outer, int inner){
		
		if ((((Act)(actslegs.get(inner))).getDuration()>=(this.OFFSET+this.minimumTime))	||	
				(outer==0	&&	inner==actslegs.size()-1)	||
				((inner==actslegs.size()-1) && (86400+((Act)(actslegs.get(0))).getEndTime()-((Act)(actslegs.get(actslegs.size()-1))).getStartTime())>(OFFSET+this.minimumTime))){
			
			 return this.setTimes(plan, actslegs, this.OFFSET, outer, inner, outer, inner);
		}
		else return this.swapDurations (plan, actslegs, outer, inner);
	}
	
	
	
	private double decreaseTime(PlanomatXPlan plan, ArrayList<?> actslegs, int outer, int inner){
		boolean checkFinalAct = false;
		double time = OFFSET+this.minimumTime;
		if (outer==0 && inner==actslegs.size()-1) time = OFFSET+1;
		if (outer==0 && inner!=actslegs.size()-1){
			checkFinalAct = true; // if first act is decreased always check final act also in setTimes() to be above minimum time!
			if (((Act)(actslegs.get(actslegs.size()-1))).getDuration()>=this.minimumTime) {
				time = OFFSET+1;
			}
		}
		if (((Act)(actslegs.get(outer))).getDuration()>=time){
			if (!checkFinalAct) return this.setTimes(plan, actslegs, (-1)*this.OFFSET, outer, inner, outer, inner);
			else return this.setTimes(plan, actslegs, (-1)*this.OFFSET, outer, inner, outer, actslegs.size()-1);
		}
		else return this.swapDurations(plan, actslegs, outer, inner);
	}
	
	
	private double swapDurations (PlanomatXPlan plan, ArrayList<?> actslegs, int outer, int inner){
		
		double swaptime= java.lang.Math.max(((Act)(actslegs.get(inner))).getDuration(), this.minimumTime)-((Act)(actslegs.get(outer))).getDuration();
		if (outer==0 	&&	swaptime<0) return this.setTimes(plan, actslegs, swaptime, outer, inner, outer, actslegs.size()-1); // check that first/last act does not turn below minimum time
		else return this.setTimes(plan, actslegs, swaptime, outer, inner, outer, inner);
	}
	
	
	
	//////////////////////////////////////////////////////////////////////
	// Help methods 
	//////////////////////////////////////////////////////////////////////
	
	
	private double cleanSchedule (double now, Plan plan){
		
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
				travelTime = java.lang.Math.max(((Act)(plan.getActsLegs().get(i+1))).getDuration()/*-travelTime*/, this.minimumTime);
				((Act)(plan.getActsLegs().get(i+1))).setDuration(travelTime);	
				((Act)(plan.getActsLegs().get(i+1))).setEndTime(now+travelTime);	
				now+=travelTime;
			}
			else {
				((Act)(plan.getActsLegs().get(i+1))).setStartTime(now);
				/* NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW*/
				if (86400>now+this.minimumTime){
					((Act)(plan.getActsLegs().get(i+1))).setDuration(86400-now);
					((Act)(plan.getActsLegs().get(i+1))).setEndTime(86400);
				}
				else if (86400+((Act)(plan.getActsLegs().get(0))).getDuration()>now+this.minimumTime){
					if (now<86400){
						((Act)(plan.getActsLegs().get(i+1))).setDuration(86400-now);
						((Act)(plan.getActsLegs().get(i+1))).setEndTime(86400);
					}
					else {
					((Act)(plan.getActsLegs().get(i+1))).setDuration(this.minimumTime);
					((Act)(plan.getActsLegs().get(i+1))).setEndTime(now+this.minimumTime);
					}
				}
				else {
					return (now+this.minimumTime-(86400+((Act)(plan.getActsLegs().get(0))).getDuration()));
				}
			}
		}
		return 0;
	}


	
	@SuppressWarnings("unchecked")
	private double setTimes (PlanomatXPlan plan, ArrayList<?> actslegs, double offset, int outer, int inner, int start, int stop){		
		double travelTime;
		double now = ((Leg)(actslegs.get(start+1))).getDepartureTime();
		int position = 0;	// indicates whether time setting has reached parameter "stop"
		
		/* if start < outer (mode choice) */
		for (int i=start+1;i<=outer-1;i=i+2){
			((Leg)(actslegs.get(i))).setDepartureTime(now);
			travelTime = this.estimator.getLegTravelTimeEstimation(plan.getPerson().getId(), now, (Act)(actslegs.get(i-1)), (Act)(actslegs.get(i+1)), (Leg)(actslegs.get(i)));
			
			((Leg)(actslegs.get(i))).setArrivalTime(now+travelTime);
			((Leg)(actslegs.get(i))).setTravelTime(travelTime);
			now = java.lang.Math.max(now+travelTime+this.minimumTime, ((Act)(actslegs.get(i+1))).getEndTime());
		}
		
		/* standard process */
		for (int i=outer+1;i<=inner-1;i=i+2){
			if (i==outer+1) {
				if (outer!=0) {
					now = java.lang.Math.max(now+offset, (((Leg)(actslegs.get(outer-1))).getArrivalTime())+this.minimumTime);
				}
				else now +=offset;
			}
			((Leg)(actslegs.get(i))).setDepartureTime(now);
			travelTime = this.estimator.getLegTravelTimeEstimation(plan.getPerson().getId(), now, (Act)(actslegs.get(i-1)), (Act)(actslegs.get(i+1)), (Leg)(actslegs.get(i)));
			
			((Leg)(actslegs.get(i))).setArrivalTime(now+travelTime);
			((Leg)(actslegs.get(i))).setTravelTime(travelTime);
			now+=travelTime;
			
			if (i!=inner-1){
				now = java.lang.Math.max(now+this.minimumTime, (((Act)(actslegs.get(i+1))).getEndTime()+offset));
				if (((Act)(actslegs.get(i+1))).getDuration()<this.minimumTime-2) log.warn("Eingehende duration < minimumTime! "+((Act)(actslegs.get(i+1))).getDuration());
			}
			else {
				double time1 = ((Act)(actslegs.get(i+1))).getEndTime();
				if (inner==actslegs.size()-1) {
					time1=((Leg)(actslegs.get(1))).getDepartureTime()+86400;
				}
				position = inner;
				if (time1<now+this.minimumTime){	// check whether act "inner" has at least minimum time
					if (actslegs.size()>=i+3){
						now+=this.minimumTime;
						((Leg)(actslegs.get(i+2))).setDepartureTime(now);
						travelTime = this.estimator.getLegTravelTimeEstimation(plan.getPerson().getId(), now, (Act)(actslegs.get(i+1)), (Act)(actslegs.get(i+3)), (Leg)(actslegs.get(i+2)));
						
						((Leg)(actslegs.get(i+2))).setArrivalTime(now+travelTime);
						((Leg)(actslegs.get(i+2))).setTravelTime(travelTime);
						now+=travelTime;
						double time2 = ((Act)(actslegs.get(i+3))).getEndTime();
						if (i+3==actslegs.size()-1) {
							time2=((Leg)(actslegs.get(1))).getDepartureTime()+86400;
						}
						position = i+3;
						if (time2<now+this.minimumTime){
							return -100000;
						}
					}
					else return -100000;
				}
			}
		}
		
		/* if position < stop (mode choice) */
		if (position < stop){
			now = ((Leg)(actslegs.get(position+1))).getDepartureTime();
			for (int i=position+1;i<=stop-1;i=i+2){
				((Leg)(actslegs.get(i))).setDepartureTime(now);
				travelTime = this.estimator.getLegTravelTimeEstimation(plan.getPerson().getId(), now, (Act)(actslegs.get(i-1)), (Act)(actslegs.get(i+1)), (Leg)(actslegs.get(i)));
				
				((Leg)(actslegs.get(i))).setArrivalTime(now+travelTime);
				((Leg)(actslegs.get(i))).setTravelTime(travelTime);
				now+=travelTime;
				now = java.lang.Math.max(now+this.minimumTime, ((Act)(actslegs.get(i+1))).getEndTime());
				if (i+1==actslegs.size()-1){
					double time=((Leg)(actslegs.get(1))).getDepartureTime()+86400;
					if (time<now){
						return -100000;
					}
				}
				else {
					if (now>((Act)(actslegs.get(i+1))).getEndTime()){
						((Leg)(actslegs.get(i+2))).setDepartureTime(now);
						travelTime = this.estimator.getLegTravelTimeEstimation(plan.getPerson().getId(), now, (Act)(actslegs.get(i+1)), (Act)(actslegs.get(i+3)), (Leg)(actslegs.get(i+2)));
						
						((Leg)(actslegs.get(i+2))).setArrivalTime(now+travelTime);
						((Leg)(actslegs.get(i+2))).setTravelTime(travelTime);
						now+=travelTime;
						double time3 = ((Act)(actslegs.get(i+3))).getEndTime();
						if ((i+3)==actslegs.size()-1) {
							time3=((Leg)(actslegs.get(1))).getDepartureTime()+86400;
						}
						if (time3<now+this.minimumTime){
							return -100000;
						}
					}
				}
			}
		}
		
		
		/* Scoring */
		plan.setActsLegs((ArrayList<Object>)actslegs);
		return scorer.getScore(plan);
	}
}
	

	
