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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.core.basic.v01.BasicLegImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.LinkNetworkRoute;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.scoring.PlanScorer;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;

import playground.mfeil.config.TimeModeChoicerConfigGroup;


/**
 * @author Matthias Feil
 * Class that inherits from TimeModeChoicer1. Features exactly the same functionality but allows for setting
 * different parameters. Required for schedule recycling where the pure PlanomatX runs with a leaner
 * TimeModeChoicer than the recycling. 
 *  */

public class TimeModeChoicer2 extends TimeModeChoicer1 implements org.matsim.population.algorithms.PlanAlgorithm { 
	
	private final int						MAX_ITERATIONS, STOP_CRITERION;
	private static final Logger 			log = Logger.getLogger(TimeModeChoicer2.class);

	
	//////////////////////////////////////////////////////////////////////
	// Constructor
	//////////////////////////////////////////////////////////////////////
	
	public TimeModeChoicer2 (Controler controler, LegTravelTimeEstimator estimator, PlanScorer scorer){
		
		super (controler, estimator, scorer);
		
		this.MAX_ITERATIONS 		= 30;
		this.STOP_CRITERION			= 5;	
	}

	
	// TODO: this is bad programming style... needs to be improved!
	public void run (PlanImpl basePlan){
		
		/*Do nothing if the plan has only one activity (=24h home)*/
		if (basePlan.getPlanElements().size()==1) return;
		
		/*Set all leg modes to car*/
		for (int z=1;z<basePlan.getPlanElements().size();z+=2){
			((LegImpl)(basePlan.getPlanElements().get(z))).setMode(TransportMode.car);
		}
		this.router.run(basePlan);
		
		/* Memorize the initial car routes.
		 * Do this in any case as the car routes are required in the setTimes() method. */
		ArrayList <LinkNetworkRoute> routes = new ArrayList<LinkNetworkRoute>();
		for (int i=1;i<basePlan.getPlanElements().size();i=i+2){
			LinkNetworkRoute r = new LinkNetworkRoute(((LegImpl)(basePlan.getPlanElements().get(i))).getRoute().getStartLink(), ((LegImpl)(basePlan.getPlanElements().get(i))).getRoute().getEndLink());
		/*	List<Id> l = new ArrayList<Id>();
			for (int j=0;j<((Leg)(basePlan.getActsLegs().get(i))).getRoute().getLinkIds().size();j++){
				l.add(((Leg)(basePlan.getActsLegs().get(i))).getRoute().getLinkIds().get(j));
			}*/
			List<Id> l = ((NetworkRoute) ((LegImpl)(basePlan.getPlanElements().get(i))).getRoute()).getLinkIds(); // to be checked whether this works
			r.setLinkIds(l);
			routes.add(r);
		}
		this.routes = routes;
		
		/* Analysis of subtours */
		PlanAnalyzeSubtours planAnalyzeSubtours = new PlanAnalyzeSubtours();
		planAnalyzeSubtours.run(basePlan);
		
		/* Make sure that all subtours with distance = 0 are set to "walk" */
		int [] subtourDis = new int [planAnalyzeSubtours.getNumSubtours()];
		for (int i=0;i<subtourDis.length;i++) {
			subtourDis[i]=this.checksubtourDistance2(basePlan.getPlanElements(), planAnalyzeSubtours, i);
		}
		for (int i=1;i<basePlan.getPlanElements().size();i=i+2){
			if (subtourDis[planAnalyzeSubtours.getSubtourIndexation()[(i-1)/2]]==0) {
				((LegImpl)(basePlan.getPlanElements().get(i))).setMode(TransportMode.walk);
			}
		}
		
		/* Initial clean-up of plan for the case actslegs is not sound*/
		double move = this.cleanSchedule (((ActivityImpl)(basePlan.getPlanElements().get(0))).getEndTime(), basePlan);
		int loops=1;
		while (move!=0.0){
			if (loops>3) {
				for (int i=0;i<basePlan.getPlanElements().size()-2;i=i+2){
					((ActivityImpl)basePlan.getPlanElements().get(i)).setDuration(this.minimumTime.get(((ActivityImpl)basePlan.getPlanElements().get(i)).getType()));
				}
				move = this.cleanSchedule(this.minimumTime.get(((ActivityImpl)basePlan.getPlanElements().get(0)).getType()), basePlan);
				if (move!=0.0){
					/*
					// TODO: whole plan copying needs to removed when there is no PlanomatXPlan any longer!
					PlanomatXPlan planAux = new PlanomatXPlan(basePlan.getPerson());
					planAux.copyPlan(basePlan);
					double tmpScore = -100000;
					if (this.possibleModes.length>0){						
						tmpScore = this.chooseModeAllChains(planAux, basePlan.getPlanElements(), planAnalyzeSubtours, subtourDis);
					}
			
					if (tmpScore!=-100000) {
						log.warn("Valid initial solution found by full mode choice run.");
						// TODO: whole plan copying needs to removed when there is no PlanomatXPlan any longer!
						basePlan.copyPlan(planAux);
						break;
					}
					else {		*/		
						// TODO Check whether allowed?
						basePlan.setScore(-100000.0);	// Like this, PlanomatX will see that the solution is no proper solution
						log.warn("No valid initial solution found for person "+basePlan.getPerson().getId()+"!");
						return;
			//		}
				}
			}
			loops++;
			for (int i=0;i<basePlan.getPlanElements().size()-2;i=i+2){
				((ActivityImpl)basePlan.getPlanElements().get(i)).setDuration(java.lang.Math.max(((ActivityImpl)basePlan.getPlanElements().get(i)).getDuration()*0.9, this.minimumTime.get(((ActivityImpl)(basePlan.getPlanElements().get(i))).getType())));
			}
			move = this.cleanSchedule(((ActivityImpl)(basePlan.getPlanElements().get(0))).getDuration(), basePlan);
		}
		// TODO Check whether allowed?
		basePlan.setScore(this.scorer.getScore(basePlan));	
		
		/* TODO: just as long as PlanomatXPlan exists. Needs then to be removed!!! */
		PlanomatXPlan plan = new PlanomatXPlan (basePlan.getPerson());
		plan.copyPlan(basePlan);
		
		/* Initializing */ 
		int neighbourhood_size = 0;
		for (int i = plan.getPlanElements().size()-1;i>0;i=i-2){
			neighbourhood_size += i;
		}
		int [][] moves 									= new int [neighbourhood_size][2];
		int [] position									= new int [2];
		List<? extends BasicPlanElement> [] initialNeighbourhood 			= new ArrayList [neighbourhood_size];
		List<? extends BasicPlanElement> [] neighbourhood 					= new ArrayList [java.lang.Math.min(NEIGHBOURHOOD_SIZE, neighbourhood_size)];
		double []score					 				= new double [neighbourhood_size];
		List<? extends BasicPlanElement> bestSolution						= new ArrayList<BasicPlanElement>();
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
		for (int z= 0;z<plan.getPlanElements().size();z=z+2){
		Activity act = (Activity)plan.getPlanElements().get(z);
			stream.print(act.getType()+"\t");
		}
		stream.println();
		stream.print("\t");
		for (int z= 0;z<plan.getPlanElements().size();z=z+2){
			stream.print(((Activity)(plan.getPlanElements()).get(z)).getDuration()+"\t");
		}
		stream.println();
		*/
		
		/* Copy the plan into all fields of the array neighbourhood */
		for (int i = 0; i < initialNeighbourhood.length; i++){
			initialNeighbourhood[i] = this.copyActsLegs(plan.getPlanElements());
		}
		
		/* Set the given plan as bestSolution */
		bestSolution = this.copyActsLegs(plan.getPlanElements());
		double bestScore = plan.getScore().doubleValue();
		
		
		/* Iteration 1 */
		this.createInitialNeighbourhood((PlanomatXPlan)plan, initialNeighbourhood, score, moves, planAnalyzeSubtours, subtourDis);
		
		pointer = this.findBestSolution (initialNeighbourhood, score, moves, position);

		/* mode choice */ 
		if (this.possibleModes.length>0){
			if (this.modeChoice.equals("standard")){
				score[pointer]=this.chooseMode(plan, initialNeighbourhood[pointer], 0, java.lang.Math.min(moves[pointer][0], moves[pointer][1]), 
						java.lang.Math.max(moves[pointer][0], moves[pointer][1]), planAnalyzeSubtours, subtourDis);
			}
			else if (this.modeChoice.equals("extended_1")){
				score[pointer]=this.chooseModeAllChains(plan, initialNeighbourhood[pointer], planAnalyzeSubtours, subtourDis);
			}
		}
		
		if (score[pointer]>bestScore){
			bestSolution = this.copyActsLegs(initialNeighbourhood[pointer]);
			bestScore=score[pointer];
			lastImprovement = 0;
		}
		else {
			lastImprovement++;
		}
		for (int i = 0;i<neighbourhood.length; i++){
			neighbourhood[i] = this.copyActsLegs(initialNeighbourhood[pointer]);
		}
		
		
		/* Do Tabu Search iterations */
		for (currentIteration = 2; currentIteration<=MAX_ITERATIONS;currentIteration++){
			
		//	stream.println("Iteration "+currentIteration);
			
			this.createNeighbourhood((PlanomatXPlan)plan, neighbourhood, score, moves, position, planAnalyzeSubtours, subtourDis);
			pointer = this.findBestSolution (neighbourhood, score, moves, position);
			
			if (pointer==-1) {
				log.info("No valid solutions found for person "+plan.getPerson().getId()+" at iteration "+currentIteration);
				break;
			}
			
			/* mode choice */ 
			if (this.possibleModes.length>0){
				if (this.modeChoice.equals("standard")){
					score[pointer]=this.chooseMode(plan, neighbourhood[pointer], 0, java.lang.Math.min(moves[pointer][0], moves[pointer][1]), 
							java.lang.Math.max(moves[pointer][0], moves[pointer][1]),planAnalyzeSubtours, subtourDis);
				}
				if (this.modeChoice.equals("extended_1")){
					score[pointer]=this.chooseModeAllChains(plan, neighbourhood[pointer], planAnalyzeSubtours, subtourDis);
				}
			}
		
			if (score[pointer]>bestScore){
				bestSolution = this.copyActsLegs(neighbourhood[pointer]);
				bestScore=score[pointer];
				lastImprovement = 0;
			}
			else {
				lastImprovement++;
				if (lastImprovement > STOP_CRITERION) break;
			}
			
			if (this.MAX_ITERATIONS!=currentIteration){			
				for (int i = 0;i<neighbourhood.length; i++){
					neighbourhood[i] = this.copyActsLegs(neighbourhood[pointer]);
				}
			}
		}
		
		
		/* Update the plan with the final solution */ 		
	//	stream.println("Selected solution\t"+bestScore);
		List<? extends BasicPlanElement> al = basePlan.getPlanElements();
		basePlan.setScore(bestScore);
		
		double time;
		for (int i = 0; i<al.size();i++){
			if (i%2==0){
				time = ((ActivityImpl)(bestSolution.get(i))).getDuration();
				((ActivityImpl)al.get(i)).setDuration(time);
				time = ((ActivityImpl)(bestSolution.get(i))).getStartTime();
				((ActivityImpl)al.get(i)).setStartTime(time);
				time = ((ActivityImpl)(bestSolution.get(i))).getEndTime();
				((ActivityImpl)al.get(i)).setEndTime(time);
			}
			else {
				time = ((LegImpl)(bestSolution.get(i))).getTravelTime();
				((LegImpl)al.get(i)).setTravelTime(time);
				time = ((LegImpl)(bestSolution.get(i))).getDepartureTime();
				((LegImpl)al.get(i)).setDepartureTime(time);
				time = ((LegImpl)(bestSolution.get(i))).getArrivalTime();
				((LegImpl)al.get(i)).setArrivalTime(time);
				BasicLegImpl mode = new BasicLegImpl(((LegImpl)(bestSolution.get(i))).getMode());
				((LegImpl)al.get(i)).setMode(mode.getMode());
			}
		}
		this.cleanRoutes(basePlan);
		
		/* reset legEstimator (clear hash map) */
		this.estimator.resetPlanSpecificInformation();
	}
	
}
	

	
