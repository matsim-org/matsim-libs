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
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.api.population.Plan;
import org.matsim.core.basic.v01.BasicLegImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRoute;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.scoring.PlanScorer;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;

import playground.mfeil.config.TimeModeChoicerConfigGroup;


/**
 * @author Matthias Feil
 * Like TimeOptimizer14 but first draft how to include also mode choice.
 */

public class TimeModeChoicer1 implements org.matsim.population.algorithms.PlanAlgorithm { 
	
	private final int						MAX_ITERATIONS, STOP_CRITERION, NEIGHBOURHOOD_SIZE;
	private final double					OFFSET;
	protected final Map<String, Double>		minimumTime;
	protected final Map<String, Double>		introTime;
	private final PlansCalcRoute		 	router;
	protected final PlanScorer 				scorer;
	protected final LegTravelTimeEstimator	estimator;
	private static final Logger 			log = Logger.getLogger(TimeModeChoicer1.class);
	private final double					maxWalkingDistance;
	private final String					modeChoice;
	private final TransportMode[]			possibleModes;
	private List<LinkNetworkRoute> 			routes;
	
	//////////////////////////////////////////////////////////////////////
	// Constructor
	//////////////////////////////////////////////////////////////////////
	
	public TimeModeChoicer1 (Controler controler, LegTravelTimeEstimator estimator, PlanScorer scorer){
		
		this.router 				= new PlansCalcRoute (controler.getConfig().plansCalcRoute(), controler.getNetwork(), controler.getTravelCostCalculator(), controler.getTravelTimeCalculator(), controler.getLeastCostPathCalculatorFactory());
		this.scorer 				= scorer;
		this.estimator				= estimator;
		this.OFFSET					= Double.parseDouble(TimeModeChoicerConfigGroup.getOffset());
		this.MAX_ITERATIONS 		= Integer.parseInt(TimeModeChoicerConfigGroup.getMaxIterations());
		this.STOP_CRITERION			= Integer.parseInt(TimeModeChoicerConfigGroup.getStopCriterion());
		//this.minimumTime			= Double.parseDouble(TimeModeChoicerConfigGroup.getMinimumTime());
		this.minimumTime			= new TreeMap<String, Double>();
		this.minimumTime.put("home", 7200.0);
		this.minimumTime.put("work", 3600.0);
		this.minimumTime.put("shopping", 3600.0);
		this.minimumTime.put("leisure", 3600.0);
		this.introTime				= this.minimumTime;
		this.NEIGHBOURHOOD_SIZE		= Integer.parseInt(TimeModeChoicerConfigGroup.getNeighbourhoodSize());
		this.maxWalkingDistance		= Double.parseDouble(TimeModeChoicerConfigGroup.getMaximumWalkingDistance());
		this.possibleModes			= TimeModeChoicerConfigGroup.getPossibleModes();
		this.modeChoice				= TimeModeChoicerConfigGroup.getModeChoice();
		this.routes					= null;
	}
	
	public TimeModeChoicer1 (Controler controler, DepartureDelayAverageCalculator tDepDelayCalc){
		
		this.router 				= new PlansCalcRoute (controler.getConfig().plansCalcRoute(), controler.getNetwork(), controler.getTravelCostCalculator(), controler.getTravelTimeCalculator(), controler.getLeastCostPathCalculatorFactory());
		this.scorer					= new PlanScorer (controler.getScoringFunctionFactory());
		
		this.estimator = controler.getConfig().planomat().getLegTravelTimeEstimator(
				controler.getTravelTimeCalculator(), 
				tDepDelayCalc, 
				this.router);

		this.OFFSET					= Double.parseDouble(TimeModeChoicerConfigGroup.getOffset());
		this.MAX_ITERATIONS 		= Integer.parseInt(TimeModeChoicerConfigGroup.getMaxIterations());
		this.STOP_CRITERION			= Integer.parseInt(TimeModeChoicerConfigGroup.getStopCriterion());
		//this.minimumTime			= Double.parseDouble(TimeModeChoicerConfigGroup.getMinimumTime());
		this.minimumTime			= new TreeMap<String, Double>();
		this.minimumTime.put("home", 7200.0);
		this.minimumTime.put("work", 3600.0);
		this.minimumTime.put("shopping", 3600.0);
		this.minimumTime.put("leisure", 3600.0);
		this.introTime				= this.minimumTime;
		this.NEIGHBOURHOOD_SIZE		= Integer.parseInt(TimeModeChoicerConfigGroup.getNeighbourhoodSize());
		this.maxWalkingDistance		= Double.parseDouble(TimeModeChoicerConfigGroup.getMaximumWalkingDistance());
		this.possibleModes			= TimeModeChoicerConfigGroup.getPossibleModes();
		this.modeChoice				= TimeModeChoicerConfigGroup.getModeChoice();
		this.routes					= null;
	}
	
		
	//////////////////////////////////////////////////////////////////////
	// run() method
	//////////////////////////////////////////////////////////////////////
	
	
	public void run (Plan basePlan){
		
		/*Do nothing if the plan has only one activity (=24h home)*/
		if (basePlan.getPlanElements().size()==1) return;
		
		/*Set all leg modes to car*/
		for (int z=1;z<basePlan.getPlanElements().size();z+=2){
			((Leg)(basePlan.getPlanElements().get(z))).setMode(TransportMode.car);
		}
		this.router.run(basePlan);
		
		/*
		boolean carIsIn = false;
		for (int i=0;i<this.possibleModes.length;i++){
			if (this.possibleModes[i]==TransportMode.car){
				carIsIn = true;
				break;
			}
		}
		*/
		
		/* Memorize the initial car routes.
		 * Do this in any case as the car routes are required in the setTimes() method. */
		ArrayList <LinkNetworkRoute> routes = new ArrayList<LinkNetworkRoute>();
		for (int i=1;i<basePlan.getPlanElements().size();i=i+2){
			LinkNetworkRoute r = new LinkNetworkRoute(((Leg)(basePlan.getPlanElements().get(i))).getRoute().getStartLink(), ((Leg)(basePlan.getPlanElements().get(i))).getRoute().getEndLink());
		/*	List<Id> l = new ArrayList<Id>();
			for (int j=0;j<((Leg)(basePlan.getActsLegs().get(i))).getRoute().getLinkIds().size();j++){
				l.add(((Leg)(basePlan.getActsLegs().get(i))).getRoute().getLinkIds().get(j));
			}*/
			List<Id> l = ((NetworkRoute) ((Leg)(basePlan.getPlanElements().get(i))).getRoute()).getLinkIds(); // to be checked whether this works
			r.setLinkIds(l);
			routes.add(r);
		}
		this.routes = routes;
		
		
	//	if (!carIsIn && this.possibleModes.length>0) {
	//		/* Set all legs to an - at least - valid mode.
	//		 * If the mode is score-wise crap the initial cleaning of the schedule will relieve this. */
	//		for (int i=1;i<basePlan.getPlanElements().size();i=i+2){
	//			((Leg)(basePlan.getPlanElements().get(i))).setMode(this.possibleModes[0]);
	//		}
	//	}
		
		
		/* Analysis of subtours */
		PlanAnalyzeSubtours planAnalyzeSubtours = new PlanAnalyzeSubtours();
		planAnalyzeSubtours.run(basePlan);
		
		/* Make sure that all legs of a subtour have the same mode*/
		TransportMode [] subtourModes = new TransportMode [planAnalyzeSubtours.getNumSubtours()];
		boolean [] subtourFilled = new boolean [subtourModes.length];
		int [] subtourDis = new int [subtourModes.length];
		for (int i=0;i<subtourFilled.length;i++) {
			subtourFilled[i]=false;
			subtourDis[i]=this.checksubtourDistance2(basePlan.getPlanElements(), planAnalyzeSubtours, i);
		}
		for (int i=1;i<basePlan.getPlanElements().size();i=i+2){
			if (subtourFilled[planAnalyzeSubtours.getSubtourIndexation()[(i-1)/2]]==false){
				/*Make sure that all subtours with distance = 0 are set to "walk" */
				if (subtourDis[planAnalyzeSubtours.getSubtourIndexation()[(i-1)/2]]==0) {
					((Leg)(basePlan.getPlanElements().get(i))).setMode(TransportMode.walk);
				}
				subtourModes[planAnalyzeSubtours.getSubtourIndexation()[(i-1)/2]]=(((Leg)(basePlan.getPlanElements().get(i))).getMode());
				subtourFilled[planAnalyzeSubtours.getSubtourIndexation()[(i-1)/2]]=true;
			}
			else {
				((Leg)(basePlan.getPlanElements().get(i))).setMode(subtourModes[planAnalyzeSubtours.getSubtourIndexation()[(i-1)/2]]);
			}
		}
		
		/* Initial clean-up of plan for the case actslegs is not sound*/
		double move = this.cleanSchedule (((Activity)(basePlan.getPlanElements().get(0))).getEndTime(), basePlan);
		int loops=1;
		while (move!=0.0){
			if (loops>3) {
				for (int i=0;i<basePlan.getPlanElements().size()-2;i=i+2){
					((Activity)basePlan.getPlanElements().get(i)).setDuration(this.minimumTime.get(((Activity)basePlan.getPlanElements().get(i)).getType()));
				}
				move = this.cleanSchedule(this.minimumTime.get(((Activity)basePlan.getPlanElements().get(0)).getType()), basePlan);
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
				((Activity)basePlan.getPlanElements().get(i)).setDuration(java.lang.Math.max(((Activity)basePlan.getPlanElements().get(i)).getDuration()*0.9, this.minimumTime.get(((Activity)(basePlan.getPlanElements().get(i))).getType())));
			}
			move = this.cleanSchedule(((Activity)(basePlan.getPlanElements().get(0))).getDuration(), basePlan);
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
			
	//		stream.println("Iteration "+currentIteration);
			
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
				time = ((Activity)(bestSolution.get(i))).getDuration();
				((Activity)al.get(i)).setDuration(time);
				time = ((Activity)(bestSolution.get(i))).getStartTime();
				((Activity)al.get(i)).setStartTime(time);
				time = ((Activity)(bestSolution.get(i))).getEndTime();
				((Activity)al.get(i)).setEndTime(time);
			}
			else {
				time = ((Leg)(bestSolution.get(i))).getTravelTime();
				((Leg)al.get(i)).setTravelTime(time);
				time = ((Leg)(bestSolution.get(i))).getDepartureTime();
				((Leg)al.get(i)).setDepartureTime(time);
				time = ((Leg)(bestSolution.get(i))).getArrivalTime();
				((Leg)al.get(i)).setArrivalTime(time);
				BasicLegImpl mode = new BasicLegImpl(((Leg)(bestSolution.get(i))).getMode());
				((Leg)al.get(i)).setMode(mode.getMode());
			}
		}
		this.cleanRoutes(basePlan);
		
		/* reset legEstimator (clear hash map) */
		this.estimator.reset();
	}
	
	//////////////////////////////////////////////////////////////////////
	// Neighbourhood definition 
	//////////////////////////////////////////////////////////////////////
	
	private void createInitialNeighbourhood (PlanomatXPlan plan, List<? extends BasicPlanElement> [] neighbourhood, double[]score, int [][] moves,
			PlanAnalyzeSubtours planAnalyzeSubtours, int[] subtourDis) {
		
		int pos = 0;
		for (int outer=0;outer<neighbourhood[0].size()-2;outer+=2){
			for (int inner=outer+2;inner<neighbourhood[0].size();inner+=2){
				
				score[pos]=this.increaseTime(plan, neighbourhood[pos], outer, inner, planAnalyzeSubtours, subtourDis);
				moves [pos][0]=outer;
				moves [pos][1]=inner;
				pos++;
				
				score[pos]=this.decreaseTime(plan, neighbourhood[pos], outer, inner, planAnalyzeSubtours, subtourDis);
				moves [pos][0]=inner;
				moves [pos][1]=outer;
				pos++;
				
			}
		}
	}
	
	
	private void createNeighbourhood (PlanomatXPlan plan, List<? extends BasicPlanElement> [] neighbourhood, double[]score, int[][] moves, int[]position,
			PlanAnalyzeSubtours planAnalyzeSubtours, int[] subtourDis) {
		
		int pos = 0;
		int fieldLength = neighbourhood.length/3;
				
			for (int outer=java.lang.Math.max(position[0]-(fieldLength/2)*2,0);outer<position[0];outer+=2){
				score[pos]=this.decreaseTime(plan, neighbourhood[pos], outer, position[0], planAnalyzeSubtours, subtourDis);
				moves [pos][0]=position[0];
				moves [pos][1]=outer;
				pos++;
			}
		
			OuterLoop1:
				for (int outer=position[0];outer<neighbourhood[0].size()-2;outer+=2){
					for (int inner=outer+2;inner<neighbourhood[0].size();inner+=2){
						score[pos]=this.increaseTime(plan, neighbourhood[pos], outer, inner, planAnalyzeSubtours, subtourDis);
						moves [pos][0]=outer;
						moves [pos][1]=inner;
						pos++;
						
						if (pos>=fieldLength) break OuterLoop1;
					}
				}
		
			for (int outer=java.lang.Math.max(position[1]-(fieldLength/2)*2,0);outer<position[1];outer+=2){
				
				if (outer!=position[0]){
					score[pos]=this.increaseTime(plan, neighbourhood[pos], outer, position[1], planAnalyzeSubtours, subtourDis);
					moves [pos][0]=outer;
					moves [pos][1]=position[1];
					pos++;
				}
			}
		
			OuterLoop2:
				for (int outer=position[1];outer<neighbourhood[0].size()-2;outer+=2){
					for (int inner=outer+2;inner<neighbourhood[0].size();inner+=2){
						score[pos]=this.decreaseTime(plan, neighbourhood[pos], outer, inner, planAnalyzeSubtours, subtourDis);
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
								score[pos]=this.increaseTime(plan, neighbourhood[pos], outer, inner, planAnalyzeSubtours, subtourDis);
								moves [pos][0]=outer;
								moves [pos][1]=inner;
								pos++;
								if (pos>neighbourhood.length-1) break OuterLoop3;
							}
							else if (inner!=position[0]	||	outer!=position[1]){
								score[pos]=this.increaseTime(plan, neighbourhood[pos], outer, inner, planAnalyzeSubtours, subtourDis);
								moves [pos][0]=outer;
								moves [pos][1]=inner;
								pos++;
								if (pos>neighbourhood.length-1) break OuterLoop3;
							}
						}
					
						if (inner!=position[0]	&&	outer!=position[1]){
							if (position[0]>position[1]){
								score[pos]=this.decreaseTime(plan, neighbourhood[pos], outer, inner, planAnalyzeSubtours, subtourDis);
								moves [pos][0]=inner;
								moves [pos][1]=outer;
								pos++;
								if (pos>neighbourhood.length-1) break OuterLoop3;
							}
							else if (outer!=position[0]	||	inner!=position[1]){
								score[pos]=this.decreaseTime(plan, neighbourhood[pos], outer, inner, planAnalyzeSubtours, subtourDis);
								moves [pos][0]=inner;
								moves [pos][1]=outer;
								pos++;
								if (pos>neighbourhood.length-1) break OuterLoop3;
							}
						}
					}
				}		
	}
	
	
	
	private double increaseTime(PlanomatXPlan plan, List<? extends BasicPlanElement> actslegs, int outer, int inner,
			PlanAnalyzeSubtours planAnalyzeSubtours, int[] subtourDis){
		
		if ((((Activity)(actslegs.get(inner))).getDuration()>=(this.OFFSET+this.minimumTime.get(((Activity)(actslegs.get(inner))).getType())))	||	
				(outer==0	&&	inner==actslegs.size()-1)	||
				((inner==actslegs.size()-1) && (86400+((Activity)(actslegs.get(0))).getEndTime()-((Activity)(actslegs.get(actslegs.size()-1))).getStartTime())>(OFFSET+this.minimumTime.get(((Activity)(actslegs.get(0))).getType())))){
			
			if (this.modeChoice.equals("extended_2")	|| this.modeChoice.equals("extended_3")){
				if (this.possibleModes.length>0){
					return this.chooseMode(plan, actslegs, this.OFFSET, outer, inner, planAnalyzeSubtours, subtourDis);
				}
				else return this.setTimes(plan, actslegs, this.OFFSET, outer, inner, outer, inner);
			}
			else return this.setTimes(plan, actslegs, this.OFFSET, outer, inner, outer, inner);
		}
		else return this.swapDurations (plan, actslegs, outer, inner, planAnalyzeSubtours, subtourDis);
	}
	
	
	
	private double decreaseTime(PlanomatXPlan plan, List<? extends BasicPlanElement> actslegs, int outer, int inner,
			PlanAnalyzeSubtours planAnalyzeSubtours, int[] subtourDis){
		boolean checkFinalAct = false;
		double time = OFFSET+this.minimumTime.get(((Activity)(actslegs.get(outer))).getType());
		if (outer==0 && inner==actslegs.size()-1) time = this.OFFSET+1;
		if (outer==0 && inner!=actslegs.size()-1){
			checkFinalAct = true; // if first act is decreased always check final act also in setTimes() to be above minimum time!
			if (((Activity)(actslegs.get(actslegs.size()-1))).getDuration()>=this.minimumTime.get(((Activity)(actslegs.get(0))).getType())) {
				time = OFFSET+1;
			}
		}
		if (((Activity)(actslegs.get(outer))).getDuration()>=time){
			if (this.modeChoice.equals("extended_3")){
				if (this.possibleModes.length>0){
					return this.chooseMode(plan, actslegs, (-1)*this.OFFSET, outer, inner, planAnalyzeSubtours, subtourDis);
				}
				else {
					if (!checkFinalAct) return this.setTimes(plan, actslegs, (-1)*this.OFFSET, outer, inner, outer, inner);
					else return this.setTimes(plan, actslegs, (-1)*this.OFFSET, outer, inner, outer, actslegs.size()-1);
				}
			}
			else {
				if (!checkFinalAct) return this.setTimes(plan, actslegs, (-1)*this.OFFSET, outer, inner, outer, inner);
				else return this.setTimes(plan, actslegs, (-1)*this.OFFSET, outer, inner, outer, actslegs.size()-1);
			}
		}
		else return this.swapDurations(plan, actslegs, outer, inner, planAnalyzeSubtours, subtourDis);
	}
	
	
	private double swapDurations (PlanomatXPlan plan, List<? extends BasicPlanElement> actslegs, int outer, int inner, PlanAnalyzeSubtours planAnalyzeSubtours, int[] subtourDis){
		
		double swaptime= java.lang.Math.max(((Activity)(actslegs.get(inner))).getDuration(), this.minimumTime.get(((Activity)(actslegs.get(outer))).getType()))-((Activity)(actslegs.get(outer))).getDuration();
		if (this.modeChoice.equals("extended_3")){
			if (this.possibleModes.length>0){
				return this.chooseMode(plan, actslegs, swaptime, outer, inner, planAnalyzeSubtours, subtourDis);
			}
			else {
				if (outer==0 	&&	swaptime<0) return this.setTimes(plan, actslegs, swaptime, outer, inner, outer, actslegs.size()-1); // check that first/last act does not turn below minimum time
				else return this.setTimes(plan, actslegs, swaptime, outer, inner, outer, inner);
			}
		}
		else {
			if (outer==0 	&&	swaptime<0) return this.setTimes(plan, actslegs, swaptime, outer, inner, outer, actslegs.size()-1); // check that first/last act does not turn below minimum time
			else return this.setTimes(plan, actslegs, swaptime, outer, inner, outer, inner);
		}
	}
	
	
	
	private double chooseMode (PlanomatXPlan plan, List<? extends BasicPlanElement> actslegs, double offset, int outer, int inner,
			PlanAnalyzeSubtours planAnalyzeSubtours, int[]subtourDis){
		List<? extends BasicPlanElement> actslegsResult = this.copyActsLegs(actslegs);
		double score=-100000;
		TransportMode subtour1=this.possibleModes[0];
		TransportMode subtour2=this.possibleModes[0];
		
		/* outer loop */
		int distanceOuter = subtourDis[planAnalyzeSubtours.getSubtourIndexation()[outer/2]];
		for (int i=0;i<this.possibleModes.length;i++){
	
			if (this.possibleModes[i].toString().equals("walk")){
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
				int distanceInner = subtourDis[planAnalyzeSubtours.getSubtourIndexation()[(inner/2)-1]];
				for (int j=0;j<this.possibleModes.length;j++){
					
					if (this.possibleModes[j].toString().equals("walk")){
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
						if (planAnalyzeSubtours.getSubtourIndexation()[x]==planAnalyzeSubtours.getSubtourIndexation()[(inner/2)-1]){
							if ((x*2)<start) start = x*2;
							stop2 = (x*2)+2;
							((Leg)(actslegs.get(x*2+1))).setMode(this.possibleModes[j]);
						}
					}
					List<? extends BasicPlanElement> actslegsInput = this.copyActsLegs(actslegs);
					double tmpscore = this.setTimes(plan, actslegsInput, offset, outer, inner, start, java.lang.Math.max(stop1, stop2));
		/*			if (tmpscore==-100000){
						System.out.println("In Mode Schleife 1");
						for (int z=1;z<actslegsInput.size();z=z+2){
							System.out.println(((Leg)(actslegsInput.get(z))).getDepartureTime()+" "+((Leg)(actslegsInput.get(z))).getArrivalTime()+" ");
							for (int y=0;y<((Leg)(actslegsInput.get(z))).getRoute().getLinkIds().size();y++){
								System.out.print(((Leg)(actslegsInput.get(z))).getRoute().getLinkIds().get(y)+" ");
							}
							System.out.println();
						}
					}*/
					if (tmpscore>score) {
						score = tmpscore;
						subtour1 = this.possibleModes[i];
						subtour2 = this.possibleModes[j];
						actslegsResult = this.copyActsLegs(actslegsInput);
					}
				}
			}
			else {
				List<? extends BasicPlanElement> actslegsInput = this.copyActsLegs(actslegs);
				double tmpscore = this.setTimes(plan, actslegsInput, offset, outer, inner, start, stop1);
	/*			if (tmpscore==-100000){
					System.out.println("In Mode Schleife 2");
					for (int z=1;z<actslegsInput.size();z=z+2){
						System.out.println(((Leg)(actslegsInput.get(z))).getDepartureTime()+" "+((Leg)(actslegsInput.get(z))).getArrivalTime()+" ");
						for (int y=0;y<((Leg)(actslegsInput.get(z))).getRoute().getLinkIds().size();y++){
							System.out.print(((Leg)(actslegsInput.get(z))).getRoute().getLinkIds().get(y)+" ");
						}
						System.out.println();
					}
				}*/
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
				((Leg)(actslegs.get((x*2)+1))).setMode(subtour1);
				continue;
			}
			if (planAnalyzeSubtours.getSubtourIndexation()[outer/2]!=planAnalyzeSubtours.getSubtourIndexation()[(inner/2)-1]){
				if (planAnalyzeSubtours.getSubtourIndexation()[x]==planAnalyzeSubtours.getSubtourIndexation()[(inner/2)-1]){
					((Leg)(actslegs.get((x*2)+1))).setMode(subtour2);
				}
			}
		}
		return score;
	}

	private double chooseModeAllChains (PlanomatXPlan plan, List<? extends BasicPlanElement> actslegsBase, PlanAnalyzeSubtours planAnalyzeSubtours, int[]subtourDis){
		List<? extends BasicPlanElement> actslegsResult = this.copyActsLegs(actslegsBase);
		double score=-100000;
		ArrayList<int[]> subtourDistances = new ArrayList<int[]>();
		/* Set mode "walk" for all subtours with distance 0 */
		for (int i=0;i<planAnalyzeSubtours.getNumSubtours();i++){
			subtourDistances.add(new int []{i,0,subtourDis[i]}); // subtour, mode pointer, distance
			if (subtourDistances.get(subtourDistances.size()-1)[2]==0) {
				subtourDistances.remove(subtourDistances.size()-1);
				for (int j=1;j<plan.getPlanElements().size();j=j+2){
					if (planAnalyzeSubtours.getSubtourIndexation()[(j-1)/2]==i)((Leg)(actslegsBase.get(j))).setMode(TransportMode.walk);
				}
			}
		}
		/* iterate as many times as there are possible combinations of subtours */
		int index = subtourDistances.size()-1;
		int searchSpace = (int) java.lang.Math.pow(this.possibleModes.length, index+1);
		for (int i=0; i<searchSpace;i++){
			boolean tour=false;
			for (int k=0;k<subtourDistances.size();k++){
				if (this.possibleModes[subtourDistances.get(k)[1]].toString().equals("walk")){
					if (subtourDistances.get(k)[2]==2){
						tour=true;
						break;
					}
				}
				else {
					if (subtourDistances.get(k)[2]==0){
						tour=true;
						break;
					}
				}
			}
			if (!tour){
				List<? extends BasicPlanElement> actslegs = this.copyActsLegs(actslegsBase);
				for (int x=1;x<actslegs.size();x+=2){
					for (int y=0;y<subtourDistances.size();y++){
						if (planAnalyzeSubtours.getSubtourIndexation()[(x-1)/2]==subtourDistances.get(y)[0]){
							((Leg)(actslegs.get(x))).setMode(this.possibleModes[subtourDistances.get(y)[1]]);
							break;
						}
					}
				}
				double tmpscore = this.setTimes(plan, actslegs, 0, 0, actslegs.size()-1, 0, actslegs.size()-1);
				if (tmpscore>score) {
					score = tmpscore;
					actslegsResult = this.copyActsLegs(actslegs);
				}
			}
			if (this.possibleModes.length>1){
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
			}
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
	
	
	protected int findBestSolution (List<? extends BasicPlanElement> [] neighbourhood, double[] score, int [][] moves, int[]position){
				
		int pointer=-1;
		double firstScore =-100000;
		for (int i=0;i<neighbourhood.length;i++){					
			if (score[i]>firstScore){
				firstScore=score[i];
				pointer=i;
				position[0]=moves[i][0];
				position[1]=moves[i][1];
			}
			/*
			stream.print(score[i]+"\t"+((Leg)(neighbourhood[i].get(1))).getDepartureTime()+"\t");
			stream.print(((Leg)(neighbourhood[i].get(1))).getRoute().getStartLinkId()+"\t"+((Leg)(neighbourhood[i].get(1))).getRoute().getEndLinkId()+"\t");
			stream.print(((Leg)(neighbourhood[i].get(1))).getMode()+"\t");
			for (int z= 2;z<neighbourhood[i].size()-1;z=z+2){
				stream.print((((Leg)(neighbourhood[i].get(z+1))).getDepartureTime()-((Leg)(neighbourhood[i].get(z-1))).getArrivalTime())+"\t");
				stream.print(((Leg)(neighbourhood[i].get(z+1))).getRoute().getStartLinkId()+"\t"+((Leg)(neighbourhood[i].get(z+1))).getRoute().getEndLinkId()+"\t");
				stream.print(((Leg)(neighbourhood[i].get(z+1))).getMode()+"\t");
			}
			stream.print(86400-((Leg)(neighbourhood[i].get(neighbourhood[i].size()-2))).getArrivalTime()+"\t");
			stream.println();
			*/
		}
	//	stream.println("Iteration's best score\t"+firstScore);
		
		/* clean-up acts of plan (=bestIterSolution) */
		if (pointer!=-1) this.cleanActs(neighbourhood[pointer]);
		
		return pointer;
	}
	
	
	private double cleanSchedule (double now, Plan plan){
		
		((Activity)(plan.getPlanElements().get(0))).setEndTime(now);
		((Activity)(plan.getPlanElements().get(0))).setDuration(now);
			
		double travelTime;
		for (int i=1;i<=plan.getPlanElements().size()-2;i=i+2){
			((Leg)(plan.getPlanElements().get(i))).setDepartureTime(now);
			travelTime = this.estimator.getLegTravelTimeEstimation(plan.getPerson().getId(), now, (Activity)(plan.getPlanElements().get(i-1)), (Activity)(plan.getPlanElements().get(i+1)), (Leg)(plan.getPlanElements().get(i)));
			// NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW
			if (((Leg)(plan.getPlanElements().get(i))).getMode()!=TransportMode.car){
				((Leg)(plan.getPlanElements().get(i))).setRoute(this.routes.get((int)(i/2)));
			}
			((Leg)(plan.getPlanElements().get(i))).setArrivalTime(now+travelTime);
			((Leg)(plan.getPlanElements().get(i))).setTravelTime(travelTime);
			now+=travelTime;
			
			if (i!=plan.getPlanElements().size()-2){
				((Activity)(plan.getPlanElements().get(i+1))).setStartTime(now);
				travelTime = java.lang.Math.max(((Activity)(plan.getPlanElements().get(i+1))).getDuration()/*-travelTime*/, this.minimumTime.get(((Activity)(plan.getPlanElements().get(i+1))).getType()));
				((Activity)(plan.getPlanElements().get(i+1))).setDuration(travelTime);	
				((Activity)(plan.getPlanElements().get(i+1))).setEndTime(now+travelTime);	
				now+=travelTime;
			}
			else {
				((Activity)(plan.getPlanElements().get(i+1))).setStartTime(now);
				/* NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW*/
				if (86400>now+this.minimumTime.get(((Activity)(plan.getPlanElements().get(i+1))).getType())){
					((Activity)(plan.getPlanElements().get(i+1))).setDuration(86400-now);
					((Activity)(plan.getPlanElements().get(i+1))).setEndTime(86400);
				}
				else if (86400+((Activity)(plan.getPlanElements().get(0))).getDuration()>now+this.minimumTime.get(((Activity)(plan.getPlanElements().get(i+1))).getType())){
					if (now<86400){
						((Activity)(plan.getPlanElements().get(i+1))).setDuration(86400-now);
						((Activity)(plan.getPlanElements().get(i+1))).setEndTime(86400);
					}
					else {
					((Activity)(plan.getPlanElements().get(i+1))).setDuration(this.minimumTime.get(((Activity)(plan.getPlanElements().get(i+1))).getType()));
					((Activity)(plan.getPlanElements().get(i+1))).setEndTime(now+this.minimumTime.get(((Activity)(plan.getPlanElements().get(i+1))).getType()));
					}
				}
				else {
					return (now+this.minimumTime.get(((Activity)(plan.getPlanElements().get(i+1))).getType())-(86400+((Activity)(plan.getPlanElements().get(0))).getDuration()));
				}
			}
		}
		return 0;
	}
		

	protected void cleanActs (List<? extends BasicPlanElement> actslegs){
		
		((Activity)(actslegs.get(0))).setEndTime(((Leg)(actslegs.get(1))).getDepartureTime());
		((Activity)(actslegs.get(0))).setDuration(((Leg)(actslegs.get(1))).getDepartureTime());
		
		for (int i=2;i<=actslegs.size()-1;i=i+2){
			
			if (i!=actslegs.size()-1){
				((Activity)(actslegs.get(i))).setStartTime(((Leg)(actslegs.get(i-1))).getArrivalTime());
				((Activity)(actslegs.get(i))).setEndTime(((Leg)(actslegs.get(i+1))).getDepartureTime());
				((Activity)(actslegs.get(i))).setDuration(((Leg)(actslegs.get(i+1))).getDepartureTime()-((Leg)(actslegs.get(i-1))).getArrivalTime());
				if (((Activity)(actslegs.get(i))).getDuration()<this.minimumTime.get(((Activity)(actslegs.get(i))).getType())-2) log.warn("duration < minimumTime: "+((Activity)(actslegs.get(i))).getDuration()+"; Pos = "+i+" von = "+(actslegs.size()-1));
			}
			else {
				((Activity)(actslegs.get(i))).setStartTime(((Leg)(actslegs.get(i-1))).getArrivalTime());
				if (((Leg)(actslegs.get(i-1))).getArrivalTime()>86400){
					((Activity)(actslegs.get(i))).setDuration(0);
					//((Act)(actslegs.get(i))).setStartTime(((Leg)(actslegs.get(i-1))).getArrivalTime());
					((Activity)(actslegs.get(i))).setEndTime(((Activity)(actslegs.get(i))).getStartTime()); // new
				}
				else {
					((Activity)(actslegs.get(i))).setDuration(86400-((Leg)(actslegs.get(i-1))).getArrivalTime());
					((Activity)(actslegs.get(i))).setEndTime(86400);
				}
			}
		}
	}
	
	private void cleanRoutes (Plan plan){
		
		for (int i=1;i<plan.getPlanElements().size();i=i+2){
			double travelTime = this.estimator.getLegTravelTimeEstimation(plan.getPerson().getId(), ((Leg)(plan.getPlanElements().get(i))).getDepartureTime(), ((Activity)(plan.getPlanElements().get(i-1))), ((Activity)(plan.getPlanElements().get(i+1))), ((Leg)(plan.getPlanElements().get(i))));
			if (java.lang.Math.abs(travelTime-((Leg)(plan.getPlanElements().get(i))).getTravelTime())>0) log.warn("Hier passt was nicht: Person "+plan.getPerson().getId()+", " +
					"leg "+i+" mit mode "+((Leg)(plan.getPlanElements().get(i))).getMode()+", orig traveltime "+((Leg)(plan.getPlanElements().get(i))).getTravelTime()+" " +
							"und neue travel time "+travelTime+", Distance "+((Leg)(plan.getPlanElements().get(i))).getRoute().getDistance());
		}
	}

	
	protected List<? extends BasicPlanElement> copyActsLegs (List<? extends BasicPlanElement> in){
		ArrayList<BasicPlanElement> out = new ArrayList<BasicPlanElement>();
		for (BasicPlanElement pe : in) {
			if (pe instanceof Activity) {
				out.add(new ActivityImpl ((Activity) pe));
			} else if (pe instanceof Leg) {
				Leg inl = ((Leg) pe);
				Leg l = new LegImpl (inl.getMode());
				l.setArrivalTime(inl.getArrivalTime());
				l.setDepartureTime(inl.getDepartureTime());
				l.setTravelTime(inl.getTravelTime());
				l.setRoute(inl.getRoute());
				/*if (inl.getRoute() != null) {
					CarRoute r = new NodeCarRoute((CarRoute) inl.getRoute());
					l.setRoute(r);
				}*/
				out.add(l);
			}
		}
		return out;
	}
	
	@SuppressWarnings("unchecked")
	private double setTimes (PlanomatXPlan plan, List<? extends BasicPlanElement> actslegs, double offset, int outer, int inner, int start, int stop){		
		double travelTime;
		double now = ((Leg)(actslegs.get(start+1))).getDepartureTime();
		int position = 0;	// indicates whether time setting has reached parameter "stop"
		
		/* if start < outer (mode choice) */
		for (int i=start+1;i<=outer-1;i=i+2){
			((Leg)(actslegs.get(i))).setDepartureTime(now);
			travelTime = this.estimator.getLegTravelTimeEstimation(plan.getPerson().getId(), now, (Activity)(actslegs.get(i-1)), (Activity)(actslegs.get(i+1)), (Leg)(actslegs.get(i)));
			// NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW
			if (((Leg)(actslegs.get(i))).getMode()!=TransportMode.car){
				((Leg)(actslegs.get(i))).setRoute(this.routes.get((i-1)/2));
				//log.warn(((Leg)(actslegs.get(i))).getMode());
			}
			
			((Leg)(actslegs.get(i))).setArrivalTime(now+travelTime);
			((Leg)(actslegs.get(i))).setTravelTime(travelTime);
			now = java.lang.Math.max(now+travelTime+this.minimumTime.get(((Activity)(actslegs.get(i+1))).getType()), ((Activity)(actslegs.get(i+1))).getEndTime());
		}
		
		/* standard process */
		for (int i=outer+1;i<=inner-1;i=i+2){
			if (i==outer+1) {
				if (outer!=0) {
					now = java.lang.Math.max(now+offset, (((Leg)(actslegs.get(outer-1))).getArrivalTime())+this.minimumTime.get(((Activity)(actslegs.get(outer))).getType()));
				}
				else now +=offset;
			}
			((Leg)(actslegs.get(i))).setDepartureTime(now);
			travelTime = this.estimator.getLegTravelTimeEstimation(plan.getPerson().getId(), now, (Activity)(actslegs.get(i-1)), (Activity)(actslegs.get(i+1)), (Leg)(actslegs.get(i)));
			// NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW
			if (((Leg)(actslegs.get(i))).getMode()!=TransportMode.car){
				((Leg)(actslegs.get(i))).setRoute(this.routes.get((i-1)/2));
				//log.warn(((Leg)(actslegs.get(i))).getMode());
			}
			((Leg)(actslegs.get(i))).setArrivalTime(now+travelTime);
			((Leg)(actslegs.get(i))).setTravelTime(travelTime);
			now+=travelTime;
			
			if (i!=inner-1){
				now = java.lang.Math.max(now+this.minimumTime.get(((Activity)(actslegs.get(i+1))).getType()), (((Activity)(actslegs.get(i+1))).getEndTime()+offset));
				if (((Activity)(actslegs.get(i+1))).getDuration()<this.minimumTime.get(((Activity)(actslegs.get(i+1))).getType())-2) log.warn("Eingehende duration < minimumTime! "+((Activity)(actslegs.get(i+1))).getDuration());
			}
			else {
				double time1 = ((Activity)(actslegs.get(i+1))).getEndTime();
				if (inner==actslegs.size()-1) {
					time1=((Leg)(actslegs.get(1))).getDepartureTime()+86400;
				}
				position = inner;
				if (time1<now+this.minimumTime.get(((Activity)(actslegs.get(i+1))).getType())){	// check whether act "inner" has at least minimum time
					if (actslegs.size()>=i+3){
						now+=this.minimumTime.get(((Activity)(actslegs.get(i+1))).getType());
						((Leg)(actslegs.get(i+2))).setDepartureTime(now);
						travelTime = this.estimator.getLegTravelTimeEstimation(plan.getPerson().getId(), now, (Activity)(actslegs.get(i+1)), (Activity)(actslegs.get(i+3)), (Leg)(actslegs.get(i+2)));
						// NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW
						if (((Leg)(actslegs.get(i+2))).getMode()!=TransportMode.car){
							((Leg)(actslegs.get(i+2))).setRoute(this.routes.get((i+1)/2));
						//	log.warn(((Leg)(actslegs.get(i+2))).getMode());
						}
						((Leg)(actslegs.get(i+2))).setArrivalTime(now+travelTime);
						((Leg)(actslegs.get(i+2))).setTravelTime(travelTime);
						now+=travelTime;
						double time2 = ((Activity)(actslegs.get(i+3))).getEndTime();
						if (i+3==actslegs.size()-1) {
							time2=((Leg)(actslegs.get(1))).getDepartureTime()+86400;
						}
						position = i+3;
						if (time2<now+this.minimumTime.get(((Activity)(actslegs.get(i+3))).getType())){
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
				travelTime = this.estimator.getLegTravelTimeEstimation(plan.getPerson().getId(), now, (Activity)(actslegs.get(i-1)), (Activity)(actslegs.get(i+1)), (Leg)(actslegs.get(i)));
				// NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW
				if (((Leg)(actslegs.get(i))).getMode()!=TransportMode.car){
					((Leg)(actslegs.get(i))).setRoute(this.routes.get((i-1)/2));
				}
				((Leg)(actslegs.get(i))).setArrivalTime(now+travelTime);
				((Leg)(actslegs.get(i))).setTravelTime(travelTime);
				now+=travelTime;
				now = java.lang.Math.max(now+this.minimumTime.get(((Activity)(actslegs.get(i+1))).getType()), ((Activity)(actslegs.get(i+1))).getEndTime());
				if (i+1==actslegs.size()-1){
					double time=((Leg)(actslegs.get(1))).getDepartureTime()+86400;
					if (time<now){
						return -100000;
					}
				}
				else {
					if (now>((Activity)(actslegs.get(i+1))).getEndTime()){
						((Leg)(actslegs.get(i+2))).setDepartureTime(now);
						travelTime = this.estimator.getLegTravelTimeEstimation(plan.getPerson().getId(), now, (Activity)(actslegs.get(i+1)), (Activity)(actslegs.get(i+3)), (Leg)(actslegs.get(i+2)));
						// NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW
						if (((Leg)(actslegs.get(i+2))).getMode()!=TransportMode.car){
							((Leg)(actslegs.get(i+2))).setRoute(this.routes.get((i+1)/2));
						}
						((Leg)(actslegs.get(i+2))).setArrivalTime(now+travelTime);
						((Leg)(actslegs.get(i+2))).setTravelTime(travelTime);
						now+=travelTime;
						double time3 = ((Activity)(actslegs.get(i+3))).getEndTime();
						if ((i+3)==actslegs.size()-1) {
							time3=((Leg)(actslegs.get(1))).getDepartureTime()+86400;
						}
						if (time3<now+this.minimumTime.get(((Activity)(actslegs.get(i+3))).getType())){
							return -100000;
						}
					}
				}
			}
		}
		
		
		/* Scoring */
		plan.setActsLegs(actslegs);
		return scorer.getScore(plan);
	}
	/*
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
	*/
	private int checksubtourDistance2 (List<? extends BasicPlanElement> actslegs, PlanAnalyzeSubtours planAnalyzeSubtours, int pos){
		double distance = 0;
		for (int k=0;k<((int)(actslegs.size()/2));k++){
			if ((planAnalyzeSubtours.getSubtourIndexation()[k])==pos){
				distance=distance+CoordUtils.calcDistance(((Activity)(actslegs.get(k*2))).getCoord(), ((Activity)(actslegs.get(k*2+2))).getCoord());
				if (distance>this.maxWalkingDistance) {
					return 2;
				}
			}
		}
		if (distance==0) return 0;
		return 1;	
	}
}
	

	
