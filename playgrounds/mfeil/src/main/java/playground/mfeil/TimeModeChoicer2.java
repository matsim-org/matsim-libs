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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.core.scoring.PlanScorer;
import org.matsim.planomat.costestimators.LegTravelTimeEstimatorFactory;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;


/**
 * @author Matthias Feil
 * Class that inherits from TimeModeChoicer1. Features exactly the same functionality but allows for setting
 * different parameters. Required for schedule recycling where the pure PlanomatX runs with a leaner
 * TimeModeChoicer than the recycling.
 *  */

public class TimeModeChoicer2 extends TimeModeChoicer1 implements org.matsim.population.algorithms.PlanAlgorithm {

	private final int						MAX_ITERATIONS, STOP_CRITERION;
	private static final Logger 			log = Logger.getLogger(TimeModeChoicer2.class);

	private final Network network;

	//////////////////////////////////////////////////////////////////////
	// Constructor
	//////////////////////////////////////////////////////////////////////

	public TimeModeChoicer2 (Controler controler, LegTravelTimeEstimatorFactory estimatorFactory, PlanScorer scorer){

		super (controler, estimatorFactory, scorer);
		this.MAX_ITERATIONS 		= 30;
		this.STOP_CRITERION			= 5;
		this.network = controler.getNetwork();
	}


	// TODO: this is bad programming style... needs to be improved!
	@Override
	public void run (Plan basePlan){

		/*Do nothing if the plan has only one or two activities (=24h home)*/
		if (basePlan.getPlanElements().size()<=3) return;

		/* Replace delivered plan by copy since delivered plan must not be changed until valid solution has been found */
		PlanomatXPlan plan = new PlanomatXPlan (basePlan.getPerson());
		plan.copyPlan(basePlan);

		/*Set all leg modes to car*/
		for (int z=1;z<plan.getPlanElements().size();z+=2){
			((LegImpl)(plan.getPlanElements().get(z))).setMode(TransportMode.car);
		}
		this.router.run(plan);

		/* Memorize the initial car routes.
		 * Do this in any case as the car routes are required in the setTimes() method. */
		ArrayList <LinkNetworkRouteImpl> routes = new ArrayList<LinkNetworkRouteImpl>();
		for (int i=1;i<plan.getPlanElements().size();i=i+2){
			RouteWRefs oldRoute = ((LegImpl)(plan.getPlanElements().get(i))).getRoute();
			LinkNetworkRouteImpl r = new LinkNetworkRouteImpl(oldRoute.getStartLinkId(), oldRoute.getEndLinkId(), this.network);

		/*	List<Id> l = new ArrayList<Id>();
			for (int j=0;j<((Leg)(basePlan.getActsLegs().get(i))).getRoute().getLinkIds().size();j++){
				l.add(((Leg)(basePlan.getActsLegs().get(i))).getRoute().getLinkIds().get(j));
			}*/
			List<Id> l = ((NetworkRoute) oldRoute).getLinkIds();

			r.setLinkIds(oldRoute.getStartLinkId(), l, oldRoute.getEndLinkId());
			routes.add(r);
		}
		this.routes = routes;

		// meisterk
		this.estimator = this.legTravelTimeEstimatorFactory.getLegTravelTimeEstimator(
				plan,
				this.config.getSimLegInterpretation(),
				this.config.getRoutingCapability(),
				this.router,
				this.network);

		/* Replace delivered plan by copy since delivered plan must not be changed until valid solution has been found */
		//PlanomatXPlan plan = new PlanomatXPlan (basePlan.getPerson());
		//plan.copyPlan(basePlan);

		/* Analysis of subtours */
		PlanAnalyzeSubtours planAnalyzeSubtours = new PlanAnalyzeSubtours(config);
		planAnalyzeSubtours.run(plan);

		/* Make sure that all subtours with distance = 0 are set to "walk" */
		int [] subtourDis = new int [planAnalyzeSubtours.getNumSubtours()];
		for (int i=0;i<subtourDis.length;i++) {
			subtourDis[i]=this.checksubtourDistance2(plan.getPlanElements(), planAnalyzeSubtours, i);
		}
		for (int i=1;i<plan.getPlanElements().size();i=i+2){
			if (subtourDis[planAnalyzeSubtours.getSubtourIndexation()[(i-1)/2]]==0) {
				((LegImpl)(plan.getPlanElements().get(i))).setMode(TransportMode.walk);
			}
		}

		/* Initial clean-up of plan for the case actslegs is not sound*/
		double move = this.cleanSchedule (((ActivityImpl)(plan.getPlanElements().get(0))).getEndTime(), plan);
		int loops=1;
		while (move!=0.0){
			if (loops>3) {
				for (int i=0;i<plan.getPlanElements().size()-2;i=i+2){
					((ActivityImpl)plan.getPlanElements().get(i)).setDuration(this.minimumTime.get(((ActivityImpl)plan.getPlanElements().get(i)).getType()));
				}
				move = this.cleanSchedule(this.minimumTime.get(((ActivityImpl)plan.getPlanElements().get(0)).getType()), plan);
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
			//			log.warn("No valid initial solution found for person "+plan.getPerson().getId()+"!");
						return;
			//		}
				}
			}
			loops++;
			for (int i=0;i<plan.getPlanElements().size()-2;i=i+2){
				((ActivityImpl)plan.getPlanElements().get(i)).setDuration(java.lang.Math.max(((ActivityImpl)plan.getPlanElements().get(i)).getDuration()*0.9, this.minimumTime.get(((ActivityImpl)(plan.getPlanElements().get(i))).getType())));
			}
			move = this.cleanSchedule(((ActivityImpl)(plan.getPlanElements().get(0))).getDuration(), plan);
		}

		plan.setScore(this.scorer.getScore(plan));

		/* Old copying
		PlanomatXPlan plan = new PlanomatXPlan (basePlan.getPerson());
		plan.copyPlan(basePlan); */

		/* Initializing */
		int neighbourhood_size = 0;
		for (int i = plan.getPlanElements().size()-1;i>0;i=i-2){
			neighbourhood_size += i;
		}
		int [][] moves 									= new int [neighbourhood_size][2];
		int [] position									= new int [2];
		List<? extends PlanElement> [] initialNeighbourhood 			= new ArrayList [neighbourhood_size];
		List<? extends PlanElement> [] neighbourhood 					= new ArrayList [java.lang.Math.min(NEIGHBOURHOOD_SIZE, neighbourhood_size)];
		double []score					 				= new double [neighbourhood_size];
		List<? extends PlanElement> bestSolution						= new ArrayList<PlanElement>();
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
		this.createInitialNeighbourhood(plan, initialNeighbourhood, score, moves, planAnalyzeSubtours, subtourDis);

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

			this.createNeighbourhood(plan, neighbourhood, score, moves, position, planAnalyzeSubtours, subtourDis);
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
		List<? extends PlanElement> al = basePlan.getPlanElements();
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
				((Leg)al.get(i)).setDepartureTime(time);
				time = ((LegImpl)(bestSolution.get(i))).getArrivalTime();
				((LegImpl)al.get(i)).setArrivalTime(time);
				((Leg)al.get(i)).setMode(((LegImpl)(bestSolution.get(i))).getMode());

				RouteWRefs oldRoute = ((LegImpl)(bestSolution.get(i))).getRoute();
				LinkNetworkRouteImpl r = new LinkNetworkRouteImpl(oldRoute.getStartLinkId(), oldRoute.getEndLinkId(), this.network);
				List<Id> l = ((NetworkRoute) oldRoute).getLinkIds();
				r.setLinkIds(oldRoute.getStartLinkId(), l, oldRoute.getEndLinkId());
				((LegImpl)al.get(i)).setRoute(r);

			}
		}
		this.cleanRoutes(basePlan);
	}

}



