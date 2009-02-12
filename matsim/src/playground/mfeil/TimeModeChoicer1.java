/* *********************************************************************** *
 * project: org.matsim.*
 * TimeModeChoicer1.java
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
import java.util.LinkedList;
import org.apache.log4j.Logger;
import org.matsim.basic.v01.BasicLeg;
import org.matsim.gbl.Gbl;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;
import org.matsim.scoring.PlanScorer;
import org.matsim.controler.Controler;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.File;
import java.io.FileNotFoundException;


/**
 * @author Matthias Feil
 * Like TimeOptimizer14 but first draft how to include also mode choice.
 */

public class TimeModeChoicer1 implements org.matsim.population.algorithms.PlanAlgorithm { 
	
	private final int						MAX_ITERATIONS, STOP_CRITERION, NEIGHBOURHOOD_SIZE;
	private int								OFFSET;
	private final double					minimumTime;
	private final PlanScorer 				scorer;
	private final LegTravelTimeEstimator	estimator;
	private static final Logger 			log = Logger.getLogger(TimeModeChoicer1.class);
	private final double					maxWalkingDistance;
	private final String					modeChoice;
	private final BasicLeg.Mode[]			possibleModes;
	
	//////////////////////////////////////////////////////////////////////
	// Constructor
	//////////////////////////////////////////////////////////////////////
	
	public TimeModeChoicer1 (LegTravelTimeEstimator estimator, PlanScorer scorer){
		
		this.scorer 				= scorer;
		this.estimator				= estimator;
		this.OFFSET					= 1800;
		this.MAX_ITERATIONS 		= 30;
		this.STOP_CRITERION			= 5;
		this.minimumTime			= 3600;
		this.NEIGHBOURHOOD_SIZE		= 10;
		this.maxWalkingDistance		= 2000;
		this.possibleModes			= Gbl.getConfig().planomat().getPossibleModes(); //faster call at runtime
		this.modeChoice				= "standard";
		
		//TODO @MF: constants to be configured externally
	}
	
		
	//////////////////////////////////////////////////////////////////////
	// run() method
	//////////////////////////////////////////////////////////////////////
	
	public void run (Plan basePlan){
		if (basePlan.getActsLegs().size()==1) return;
		
		/* Analysis of subtours */
		if (basePlan.getPerson().getId().toString().equals("4888333")) log.warn("Person 4888333 vor planAnalyzeSubtours");
		PlanAnalyzeSubtours planAnalyzeSubtours = new PlanAnalyzeSubtours();
		planAnalyzeSubtours.run(basePlan);
		if (basePlan.getPerson().getId().toString().equals("4888333")) log.warn("Person 4888333 nach planAnalyzeSubtours");
		
		// Initial clean-up of plan for the case actslegs is not sound.
		double move = this.cleanSchedule (((Act)(basePlan.getActsLegs().get(0))).getEndTime(), basePlan);
		if (basePlan.getPerson().getId().toString().equals("4888333")) log.warn("Person 4888333 nach 1. cleanSchedule mit move = "+move);
		int loops=1;
		boolean cannotMove = false;
		while (move!=0.0){
			if (cannotMove || loops>3) {
				for (int i=2;i<basePlan.getActsLegs().size()-4;i+=2){
					((Act)basePlan.getActsLegs().get(i)).setDuration(this.minimumTime);
				}
				move = this.cleanSchedule(this.minimumTime, basePlan);
				if (basePlan.getPerson().getId().toString().equals("4888333")) log.warn("Person 4888333 nach letztem cleanSchedule mit move = "+move);
				if (move!=0.0){
					
					if (basePlan.getPerson().getId().toString().equals("4888333")) log.warn("Person 4888333 vor chooseModeAllChains.");
					// TODO: whole plan copying needs to removed when there is no PlanomatXPlan any longer!
					PlanomatXPlan planAux = new PlanomatXPlan(basePlan.getPerson());
					planAux.copyPlan(basePlan);
					double tmpScore = this.chooseModeAllChains(planAux, basePlan.getActsLegs(), planAnalyzeSubtours);
					if (basePlan.getPerson().getId().toString().equals("4888333")) log.warn("Person 4888333 nach chooseModeAllChains mit score = "+basePlan.getScore());
					if (tmpScore!=-100000) {
						log.warn("Valid initial solution found by first mode choice run.");
						// TODO: whole plan copying needs to removed when there is no PlanomatXPlan any longer!
						basePlan.copyPlan(planAux);
						break;
					}
					else {
					
						// TODO Check whether allowed?
						basePlan.setScore(-100000);	// Like this, PlanomatX will see that the solution is no proper solution
						log.warn("No valid initial solution found for person "+basePlan.getPerson().getId()+"!");
						return;
					}
				}
			}
			loops++;
			if (((Act)(basePlan.getActsLegs().get(0))).getEndTime()-move<this.minimumTime) cannotMove = true;
			move = this.cleanSchedule(java.lang.Math.max(((Act)(basePlan.getActsLegs().get(0))).getEndTime()-move,this.minimumTime), basePlan);
			if (basePlan.getPerson().getId().toString().equals("4888333")) log.warn("Person 4888333 nach "+loops+". cleanSchedule mit move = "+move+". cannotMove = "+cannotMove);
		}
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
	//	stream.println("Iteration "+1);
		this.createInitialNeighbourhood((PlanomatXPlan)plan, initialNeighbourhood, score, moves, planAnalyzeSubtours);
		
		pointer = this.findBestSolution (initialNeighbourhood, score, moves, position);
		
		/* mode choice */ 
		if (this.modeChoice=="standard"){
			score[pointer]=this.chooseMode(plan, initialNeighbourhood[pointer], 0, java.lang.Math.min(moves[pointer][0], moves[pointer][1]), java.lang.Math.max(moves[pointer][0], moves[pointer][1]),planAnalyzeSubtours);
		}
		else if (this.modeChoice=="extended_1"){
			score[pointer]=this.chooseModeAllChains(plan, initialNeighbourhood[pointer], planAnalyzeSubtours);
		}
		
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
			
			this.createNeighbourhood((PlanomatXPlan)plan, neighbourhood, score, moves, position, planAnalyzeSubtours);
			pointer = this.findBestSolution (neighbourhood, score, moves, position);
			
			if (pointer==-1) {
				log.info("No valid solutions found for person "+plan.getPerson().getId()+" at iteration "+currentIteration);
				break;
			}
			
			/* mode choice */ 
			if (this.modeChoice=="standard"){
				score[pointer]=this.chooseMode(plan, neighbourhood[pointer], 0, java.lang.Math.min(moves[pointer][0], moves[pointer][1]), java.lang.Math.max(moves[pointer][0], moves[pointer][1]),planAnalyzeSubtours);
			}
			if (this.modeChoice=="extended_1"){
				score[pointer]=this.chooseModeAllChains(plan, neighbourhood[pointer], planAnalyzeSubtours);
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
			this.estimator.reset();
		}
	
		/* Update the plan with the final solution */ 		
	//	stream.println("Selected solution\t"+bestScore);
		ArrayList<Object> al = basePlan.getActsLegs();
		basePlan.setScore(bestScore);
		
		for (int i = 0; i<al.size();i++){
			if (i%2==0){
				((Act)al.get(i)).setDuration(((Act)(bestSolution.get(i))).getDuration());
				((Act)al.get(i)).setStartTime(((Act)(bestSolution.get(i))).getStartTime());
				((Act)al.get(i)).setEndTime(((Act)(bestSolution.get(i))).getEndTime());
			}
			else {
				((Leg)al.get(i)).setTravelTime(((Leg)(bestSolution.get(i))).getTravelTime());
				((Leg)al.get(i)).setDepartureTime(((Leg)(bestSolution.get(i))).getDepartureTime());
				((Leg)al.get(i)).setArrivalTime(((Leg)(bestSolution.get(i))).getArrivalTime());
				((Leg)al.get(i)).setMode(((Leg)(bestSolution.get(i))).getMode());
			}
		}
		
		/* reset legEstimator (clear hash map) */
		this.estimator.reset();
	}
	
	//////////////////////////////////////////////////////////////////////
	// Neighbourhood definition 
	//////////////////////////////////////////////////////////////////////
	
	public void createInitialNeighbourhood (PlanomatXPlan plan, ArrayList<?> [] neighbourhood, double[]score, int [][] moves,
			PlanAnalyzeSubtours planAnalyzeSubtours) {
		
		int pos = 0;
		for (int outer=0;outer<neighbourhood[0].size()-2;outer+=2){
			for (int inner=outer+2;inner<neighbourhood[0].size();inner+=2){
				
				score[pos]=this.increaseTime(plan, neighbourhood[pos], outer, inner, planAnalyzeSubtours);
				moves [pos][0]=outer;
				moves [pos][1]=inner;
				pos++;
				
				score[pos]=this.decreaseTime(plan, neighbourhood[pos], outer, inner, planAnalyzeSubtours);
				moves [pos][0]=inner;
				moves [pos][1]=outer;
				pos++;
				
			}
		}
	}
	
	
	public void createNeighbourhood (PlanomatXPlan plan, ArrayList<?> [] neighbourhood, double[]score, int[][] moves, int[]position,
			PlanAnalyzeSubtours planAnalyzeSubtours) {
		
		int pos = 0;
		int fieldLength = neighbourhood.length/3;
				
			for (int outer=java.lang.Math.max(position[0]-(fieldLength/2)*2,0);outer<position[0];outer+=2){
				score[pos]=this.decreaseTime(plan, neighbourhood[pos], outer, position[0], planAnalyzeSubtours);
				moves [pos][0]=position[0];
				moves [pos][1]=outer;
				pos++;
			}
		
			OuterLoop1:
				for (int outer=position[0];outer<neighbourhood[0].size()-2;outer+=2){
					for (int inner=outer+2;inner<neighbourhood[0].size();inner+=2){
						score[pos]=this.increaseTime(plan, neighbourhood[pos], outer, inner, planAnalyzeSubtours);
						moves [pos][0]=outer;
						moves [pos][1]=inner;
						pos++;
						
						if (pos>=fieldLength) break OuterLoop1;
					}
				}
		
			for (int outer=java.lang.Math.max(position[1]-(fieldLength/2)*2,0);outer<position[1];outer+=2){
				
				if (outer!=position[0]){
					score[pos]=this.increaseTime(plan, neighbourhood[pos], outer, position[1], planAnalyzeSubtours);
					moves [pos][0]=outer;
					moves [pos][1]=position[1];
					pos++;
				}
			}
		
			OuterLoop2:
				for (int outer=position[1];outer<neighbourhood[0].size()-2;outer+=2){
					for (int inner=outer+2;inner<neighbourhood[0].size();inner+=2){
						score[pos]=this.decreaseTime(plan, neighbourhood[pos], outer, inner, planAnalyzeSubtours);
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
								score[pos]=this.increaseTime(plan, neighbourhood[pos], outer, inner, planAnalyzeSubtours);
								moves [pos][0]=outer;
								moves [pos][1]=inner;
								pos++;
								if (pos>neighbourhood.length-1) break OuterLoop3;
							}
							else if (inner!=position[0]	||	outer!=position[1]){
								score[pos]=this.increaseTime(plan, neighbourhood[pos], outer, inner, planAnalyzeSubtours);
								moves [pos][0]=outer;
								moves [pos][1]=inner;
								pos++;
								if (pos>neighbourhood.length-1) break OuterLoop3;
							}
						}
					
						if (inner!=position[0]	&&	outer!=position[1]){
							if (position[0]>position[1]){
								score[pos]=this.decreaseTime(plan, neighbourhood[pos], outer, inner, planAnalyzeSubtours);
								moves [pos][0]=inner;
								moves [pos][1]=outer;
								pos++;
								if (pos>neighbourhood.length-1) break OuterLoop3;
							}
							else if (outer!=position[0]	||	inner!=position[1]){
								score[pos]=this.decreaseTime(plan, neighbourhood[pos], outer, inner, planAnalyzeSubtours);
								moves [pos][0]=inner;
								moves [pos][1]=outer;
								pos++;
								if (pos>neighbourhood.length-1) break OuterLoop3;
							}
						}
					}
				}		
	}
	
	
	
	public double increaseTime(PlanomatXPlan plan, ArrayList<?> actslegs, int outer, int inner,
			PlanAnalyzeSubtours planAnalyzeSubtours){
		
		if ((((Act)(actslegs.get(inner))).getDuration()>=this.OFFSET+this.minimumTime)	||	
				(outer==0	&&	inner==actslegs.size()-1)	||
				(86400+((Act)(actslegs.get(0))).getEndTime()-((Act)(actslegs.get(actslegs.size()-1))).getStartTime())>OFFSET+this.minimumTime){
			
			if (this.modeChoice=="extended_2"	|| this.modeChoice=="extended_3"){
				if (this.possibleModes.length>0){
					return this.chooseMode(plan, actslegs, this.OFFSET, outer, inner, planAnalyzeSubtours);
				}
				else return this.setTimes(plan, actslegs, this.OFFSET, outer, inner, outer, inner);
			}
			else return this.setTimes(plan, actslegs, this.OFFSET, outer, inner, outer, inner);
		}
		else return this.swapDurations (plan, actslegs, outer, inner, planAnalyzeSubtours);
	}
	
	
	
	public double decreaseTime(PlanomatXPlan plan, ArrayList<?> actslegs, int outer, int inner,
			PlanAnalyzeSubtours planAnalyzeSubtours){
		
		double time = OFFSET+this.minimumTime;
		if (outer==0) time = OFFSET+1;
		if (((Act)(actslegs.get(outer))).getDuration()>=time){
			if (this.modeChoice=="extended_3"){
				if (this.possibleModes.length>0){
					return this.chooseMode(plan, actslegs, (-1)*this.OFFSET, outer, inner, planAnalyzeSubtours);
				}
				else return this.setTimes(plan, actslegs, (-1)*this.OFFSET, outer, inner, outer, inner);
			}
			else return this.setTimes(plan, actslegs, (-1)*this.OFFSET, outer, inner, outer, inner);
		}
		else return this.swapDurations(plan, actslegs, outer, inner, planAnalyzeSubtours);
	}
	
	
	public double swapDurations (PlanomatXPlan plan, ArrayList<?> actslegs, int outer, int inner, PlanAnalyzeSubtours planAnalyzeSubtours){
		
		double swaptime= java.lang.Math.max(((Act)(actslegs.get(inner))).getDuration(), this.minimumTime)-((Act)(actslegs.get(outer))).getDuration();
		if (this.modeChoice=="extended_3"){
			if (this.possibleModes.length>0){
				return this.chooseMode(plan, actslegs, swaptime, outer, inner, planAnalyzeSubtours);
			}
			else return this.setTimes(plan, actslegs, swaptime, outer, inner, outer, inner);
		}
		else return this.setTimes(plan, actslegs, swaptime, outer, inner, outer, inner);
	}
	
	
	
	private double chooseMode (PlanomatXPlan plan, ArrayList<?> actslegs, double offset, int outer, int inner,
			PlanAnalyzeSubtours planAnalyzeSubtours){
		ArrayList<?> actslegsResult = this.copyActsLegs(actslegs);
		double score=-100000;
		BasicLeg.Mode subtour1=this.possibleModes[0];
		BasicLeg.Mode subtour2=this.possibleModes[0];
		
		/* outer loop */
		int distanceOuter = this.checkSubtourDistance(actslegs, planAnalyzeSubtours, (outer/2));
		for (int i=0;i<this.possibleModes.length;i++){
	
			if (this.possibleModes[i].toString()=="walk"){
				if (distanceOuter==2) {
					continue;
				}
			}
			else {
				if (distanceOuter==0) {
					continue;
				}
			}
			boolean startFound = false;
			int start = -1;
			int stop1 = -1;
			for (int x=0;x<((int)(actslegs.size()/2));x++){
				if (planAnalyzeSubtours.getSubtourIndexation()[x]==planAnalyzeSubtours.getSubtourIndexation()[outer/2]){
					if (!startFound) {
						start = x*2;
						startFound = true;
					}
					stop1 = (x*2)+2;
					((Leg)(actslegs.get(x*2+1))).setMode(this.possibleModes[i]);
				}
			}
			if (planAnalyzeSubtours.getSubtourIndexation()[outer/2]!=planAnalyzeSubtours.getSubtourIndexation()[(inner/2)-1]){
				/* inner loop */
				int distanceInner = this.checkSubtourDistance(actslegs, planAnalyzeSubtours, (inner/2-1));
				for (int j=0;j<this.possibleModes.length;j++){
					
					if (this.possibleModes[i].toString()=="walk"){
						if (distanceInner==2) {
							continue;
						}
					}
					else {
						if (distanceInner==0) {
							continue;
						}
					}
					int stop2 = -1;
					for (int x=0;x<((int)(actslegs.size()/2));x++){
						if (planAnalyzeSubtours.getSubtourIndexation()[x]==planAnalyzeSubtours.getSubtourIndexation()[inner/2-1]){
							if ((x*2)<start) start = x*2;
							stop2 = (x*2)+2;
							((Leg)(actslegs.get(x*2+1))).setMode(this.possibleModes[j]);
						}
					}
					ArrayList<?> actslegsInput = this.copyActsLegs(actslegs);
					double tmpscore = this.setTimes(plan, actslegsInput, offset, outer, inner, start, java.lang.Math.max(stop1, stop2));
					if (tmpscore>score) {
						score = tmpscore;
						subtour1 = this.possibleModes[i];
						subtour2 = this.possibleModes[j];
						actslegsResult = this.copyActsLegs(actslegsInput);
					}
				}
			}
			else {
				ArrayList<?> actslegsInput = this.copyActsLegs(actslegs);
				double tmpscore = this.setTimes(plan, actslegsInput, offset, outer, inner, start, stop1);
				if (tmpscore>score) {
					score = tmpscore;
					subtour1 = this.possibleModes[i];
					actslegsResult = this.copyActsLegs(actslegsInput);
				}
			}
		}
		for (int z=1;z<actslegs.size();z+=2){
			((Leg)(actslegs.get(z))).setDepartureTime(((Leg)(actslegsResult.get(z))).getDepartureTime());
			((Leg)(actslegs.get(z))).setTravelTime(((Leg)(actslegsResult.get(z))).getTravelTime());
			((Leg)(actslegs.get(z))).setArrivalTime(((Leg)(actslegsResult.get(z))).getArrivalTime());
		}
		for (int x=0;x<((int)(actslegs.size()/2));x++){
			if (planAnalyzeSubtours.getSubtourIndexation()[x]==planAnalyzeSubtours.getSubtourIndexation()[outer/2]){
				((Leg)(actslegs.get(x*2+1))).setMode(subtour1);
				continue;
			}
			if (planAnalyzeSubtours.getSubtourIndexation()[outer/2]!=planAnalyzeSubtours.getSubtourIndexation()[inner/2-1]){
				if (planAnalyzeSubtours.getSubtourIndexation()[x]==planAnalyzeSubtours.getSubtourIndexation()[inner/2-1]){
					((Leg)(actslegs.get(x*2+1))).setMode(subtour2);
				}
			}
		}
	return score;
	}

	private double chooseModeAllChains (PlanomatXPlan plan, ArrayList<?> actslegsBase, PlanAnalyzeSubtours planAnalyzeSubtours){
		ArrayList<?> actslegsResult = this.copyActsLegs(actslegsBase);
		double score=-100000;
		ArrayList<int[]> subtourDistances = new ArrayList<int[]>();
		/* Set mode "walk" for all subtours with distance 0 */
		for (int i=0;i<planAnalyzeSubtours.getNumSubtours();i++){
			subtourDistances.add(new int []{i,0,this.checksubtourDistance2(actslegsBase, planAnalyzeSubtours, i)}); // subtour, mode pointer, distance
			if (subtourDistances.get(subtourDistances.size()-1)[2]==0) {
				subtourDistances.remove(subtourDistances.size()-1);
				for (int j=1;j<plan.getActsLegs().size();j=j+2){
					if (planAnalyzeSubtours.getSubtourIndexation()[(j-1)/2]==i)((Leg)(actslegsBase.get(j))).setMode(BasicLeg.Mode.walk);
				}
			}
		}
		/* loop as many times as there are possible combinations of subtours */
		int index = subtourDistances.size()-1;
		int searchSpace = (int) java.lang.Math.pow(this.possibleModes.length, index+1);
		log.warn("Call of method chooseModeAllChains() for person "+plan.getPerson().getId()+", searchSpace = "+searchSpace);
		for (int i=0; i<searchSpace;i++){
			if (plan.getPerson().getId().toString().equals("4888333") && i%10==0) log.warn("Person 4888333 in der "+i+". chooseModeAllChains Schleife.");
			boolean tour=false;
			for (int k=0;k<subtourDistances.size();k++){
				if (this.possibleModes[subtourDistances.get(k)[1]].toString().equals("walk")){
					if (subtourDistances.get(k)[2]==2){
						tour=true;
						//log.warn("Subtour "+subtourDistances.get(k)[0]+" has a 'walk is too far' exclusion.");
						break;
					}
				}
				else {
					if (subtourDistances.get(k)[2]==0){
						tour=true;
						//log.warn("Subtour "+subtourDistances.get(k)[0]+" has a 'non-walk is too short' exclusion.");
						break;
					}
				}
			}
			if (!tour){
				ArrayList<?> actslegs = this.copyActsLegs(actslegsBase);
				for (int x=1;x<actslegs.size();x+=2){
					for (int y=0;y<subtourDistances.size();y++){
						if (planAnalyzeSubtours.getSubtourIndexation()[(x-1)/2]==subtourDistances.get(y)[0]){
							((Leg)(actslegs.get(x))).setMode(this.possibleModes[subtourDistances.get(y)[1]]);
							break;
						}
					}
				}
				/*
				log.info("Index is "+index);
				for (int j=1;j<actslegs.size();j=j+2){
					log.info ("Iteration "+i+", leg "+j+": mode = "+((Leg)(actslegs.get(j))).getMode());
				}
				*/
				double tmpscore = this.setTimes(plan, actslegs, 0, 0, actslegs.size()-1, 0, actslegs.size()-1);
				if (tmpscore>score) {
					score = tmpscore;
					actslegsResult = this.copyActsLegs(actslegs);
				}
			}
			while (subtourDistances.get(index)[1]==this.possibleModes.length-1){
				subtourDistances.get(index)[1]=0;
				if (index!=0) {
					index--;
				}
			}
			subtourDistances.get(index)[1]++;
			if (index!=subtourDistances.size()-1){
				index=subtourDistances.size()-1;
			}
		/*	if (i>81 && iter==false){
				log.info("Iteration "+i);
				System.out.println(planAnalyzeSubtours.getNumSubtours()+" und "+java.lang.Math.pow(Gbl.getConfig().planomat().getPossibleModes().length, subtours.length));
				for (int p=0;p<actslegsBase.size();p+=2) System.out.print(((Act)(actslegsBase.get(p))).getType()+" ");
				System.out.println();
				iter=true;
			}*/
		}
	
		for (int z=1;z<actslegsBase.size();z+=2){
			((Leg)(actslegsBase.get(z))).setDepartureTime(((Leg)(actslegsResult.get(z))).getDepartureTime());
			((Leg)(actslegsBase.get(z))).setTravelTime(((Leg)(actslegsResult.get(z))).getTravelTime());
			((Leg)(actslegsBase.get(z))).setArrivalTime(((Leg)(actslegsResult.get(z))).getArrivalTime());
			((Leg)(actslegsBase.get(z))).setMode(((Leg)(actslegsResult.get(z))).getMode());
		}
	return score;
	}
	
	//////////////////////////////////////////////////////////////////////
	// Help methods 
	//////////////////////////////////////////////////////////////////////
	
	
	public int findBestSolution (ArrayList<?> [] neighbourhood, double[] score, int [][] moves, int[]position){
				
		int pointer=-1;
		ArrayList<?> actslegs = new ArrayList<Object>();
		double firstScore =-100000;
		for (int i=0;i<neighbourhood.length;i++){					
			if (score[i]>firstScore){
				actslegs = neighbourhood[i];
				firstScore=score[i];
				pointer=i;
				position[0]=moves[i][0];
				position[1]=moves[i][1];
			}
			/*
			stream.print(score[i]+"\t"+((Leg)(neighbourhood[i].get(1))).getDepartureTime()+"\t");
			stream.print(((Leg)(neighbourhood[i].get(1))).getMode()+"\t");
			for (int z= 2;z<neighbourhood[i].size()-1;z=z+2){
				stream.print((((Leg)(neighbourhood[i].get(z+1))).getDepartureTime()-((Leg)(neighbourhood[i].get(z-1))).getArrivalTime())+"\t");
				stream.print(((Leg)(neighbourhood[i].get(z+1))).getMode()+"\t");
			}
			stream.print(86400-((Leg)(neighbourhood[i].get(neighbourhood[i].size()-2))).getArrivalTime()+"\t");
			stream.println();
			*/
		}
	//	stream.println("Iteration's best score\t"+firstScore);
		
		/* clean-up acts of plan (=bestIterSolution) */
		if (pointer!=-1) this.cleanActs(actslegs);
		
		return pointer;
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
				travelTime = java.lang.Math.max(((Act)(plan.getActsLegs().get(i+1))).getDuration()-travelTime, this.minimumTime);
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
		

	public void cleanActs (ArrayList<?> actslegs){
		
		((Act)(actslegs.get(0))).setEndTime(((Leg)(actslegs.get(1))).getDepartureTime());
		((Act)(actslegs.get(0))).setDuration(((Leg)(actslegs.get(1))).getDepartureTime());
		
		for (int i=2;i<=actslegs.size()-1;i=i+2){
			
			if (i!=actslegs.size()-1){
				((Act)(actslegs.get(i))).setStartTime(((Leg)(actslegs.get(i-1))).getArrivalTime());
				((Act)(actslegs.get(i))).setEndTime(((Leg)(actslegs.get(i+1))).getDepartureTime());
				((Act)(actslegs.get(i))).setDuration(((Leg)(actslegs.get(i+1))).getDepartureTime()-((Leg)(actslegs.get(i-1))).getArrivalTime());
				if (((Act)(actslegs.get(i))).getDuration()<this.minimumTime-2) log.warn("duration < minimumTime: "+((Act)(actslegs.get(i))).getDuration()+"; Pos = "+i+" von = "+(actslegs.size()-1));
			}
			else {
				((Act)(actslegs.get(i))).setStartTime(((Leg)(actslegs.get(i-1))).getArrivalTime());
				if (((Leg)(actslegs.get(i-1))).getArrivalTime()>86400){
					((Act)(actslegs.get(i))).setDuration(0);
					//((Act)(actslegs.get(i))).setStartTime(((Leg)(actslegs.get(i-1))).getArrivalTime());
					((Act)(actslegs.get(i))).setEndTime(((Act)(actslegs.get(i))).getStartTime()); // new
				}
				else {
					((Act)(actslegs.get(i))).setDuration(86400-((Leg)(actslegs.get(i-1))).getArrivalTime());
					((Act)(actslegs.get(i))).setEndTime(86400);
				}
			}
		}
	}

	
	public ArrayList<Object> copyActsLegs (ArrayList<?> in){
		
			ArrayList<Object> out = new ArrayList<Object>();
			
			for (int i= 0; i< in.size() ; i++) {
				try {
					if (i % 2 == 0) {
						// Activity
						Act a = new Act ((Act)in.get(i));
						out.add(a);
					} else {
						// Leg
						Leg inl = ((Leg) in.get(i));
						Leg l = new Leg (inl.getMode());
						l.setArrivalTime(inl.getArrivalTime());
						l.setDepartureTime(inl.getDepartureTime());
						l.setTravelTime(inl.getTravelTime());
						l.setRoute(inl.getRoute());
						out.add(l);
					}
				} catch (Exception e) {
					Gbl.errorMsg(e);
				}
			}
		return out;
	}
	
	@SuppressWarnings("unchecked")
	private double setTimes (PlanomatXPlan plan, ArrayList<?> actslegs, double offset, int outer, int inner, int start, int stop){		
		double travelTime;
		double now = ((Leg)(actslegs.get(start+1))).getDepartureTime();
		int position = 0;	// indicates whether time setting has reached parameter "stop"
		
		/* if start < outer (mode choice) */
		for (int i=start+1;i<=outer-1;i+=2){
			((Leg)(actslegs.get(i))).setDepartureTime(now);
			travelTime = this.estimator.getLegTravelTimeEstimation(plan.getPerson().getId(), now, (Act)(actslegs.get(i-1)), (Act)(actslegs.get(i+1)), (Leg)(actslegs.get(i)));
			((Leg)(actslegs.get(i))).setArrivalTime(now+travelTime);
			((Leg)(actslegs.get(i))).setTravelTime(travelTime);
			//now+=travelTime+((Act)(actslegs.get(i+1))).getDuration();
			now = java.lang.Math.max(now+travelTime+this.minimumTime, ((Act)(actslegs.get(i+1))).getEndTime());
		}
		
		/* standard process */
		for (int i=outer+1;i<=inner-1;i+=2){
			if (i==outer+1) {
				if (outer!=0) {
					now = java.lang.Math.max(now+offset, (((Leg)(actslegs.get(outer-1))).getArrivalTime())+this.minimumTime);
				}
				else now +=offset;
			}
			((Leg)(actslegs.get(i))).setDepartureTime(now);
			//if (plan.getPerson().getId().toString().equals("110")) log.info(i+" = "+((Leg)(actslegs.get(i))).getDepartureTime());
			travelTime = this.estimator.getLegTravelTimeEstimation(plan.getPerson().getId(), now, (Act)(actslegs.get(i-1)), (Act)(actslegs.get(i+1)), (Leg)(actslegs.get(i)));
			((Leg)(actslegs.get(i))).setArrivalTime(now+travelTime);
			((Leg)(actslegs.get(i))).setTravelTime(travelTime);
			now+=travelTime;
			
			if (i!=inner-1){
				//now+=((Act)(actslegs.get(i+1))).getDuration();
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
			for (int i=position+1;i<=stop-1;i+=2){
				((Leg)(actslegs.get(i))).setDepartureTime(now);
				travelTime = this.estimator.getLegTravelTimeEstimation(plan.getPerson().getId(), now, (Act)(actslegs.get(i-1)), (Act)(actslegs.get(i+1)), (Leg)(actslegs.get(i)));
				((Leg)(actslegs.get(i))).setArrivalTime(now+travelTime);
				((Leg)(actslegs.get(i))).setTravelTime(travelTime);
				//now+=travelTime+((Act)(actslegs.get(i+1))).getDuration();
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
	
	private int checkSubtourDistance (ArrayList<?> actslegs, PlanAnalyzeSubtours planAnalyzeSubtours, int pos){
		double distance = 0;
		for (int k=0;k<((int)(actslegs.size()/2));k++){
			if (planAnalyzeSubtours.getSubtourIndexation()[k]==planAnalyzeSubtours.getSubtourIndexation()[pos]){
				distance+=((Act)(actslegs.get(k*2))).getCoord().calcDistance(((Act)(actslegs.get(k*2+2))).getCoord());
				if (distance>this.maxWalkingDistance) {
					return 2; // "2" = too long to walk
				}
			}
		}
		if (distance==0) return 0; // "0" = no distance at all, so subtour between same location
		return 1; // "1" = default rest
	}
	
	private int checksubtourDistance2 (ArrayList<?> actslegs, PlanAnalyzeSubtours planAnalyzeSubtours, int pos){
		double distance = 0;
		for (int k=0;k<((int)(actslegs.size()/2));k++){
			if (planAnalyzeSubtours.getSubtourIndexation()[k]==pos){
				distance+=((Act)(actslegs.get(k*2))).getCoord().calcDistance(((Act)(actslegs.get(k*2+2))).getCoord());
				if (distance>this.maxWalkingDistance) {
					return 2;
				}
			}
		}
		if (distance==0) return 0;
		return 1;	
	}
	
	
	/*
	public double getOffset (){
		return this.OFFSET;
	}
	 */
}
	

	
