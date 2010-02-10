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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.scoring.PlanScorer;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimatorFactory;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;

import playground.mfeil.config.TimeModeChoicerConfigGroup;


/**
 * @author Matthias Feil
 * Class that optimizes the mode choices and activity timings of a plan.
 * Standard version as of 21/06/2009.
 */

public class TimeModeChoicer1 implements org.matsim.population.algorithms.PlanAlgorithm {

	protected final int						MAX_ITERATIONS, STOP_CRITERION, NEIGHBOURHOOD_SIZE;
	protected final double					OFFSET;
	protected final Map<String, Double>		minimumTime;
	protected final Map<String, Double>		introTime;
	protected final PlansCalcRoute		 	router;
	protected final PlanScorer 				scorer;
	protected LegTravelTimeEstimator		estimator;
	protected final LegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory;
	protected final PlanomatConfigGroup 	config;
	protected static final Logger 			log = Logger.getLogger(TimeModeChoicer1.class);
	protected final double					maxWalkingDistance;
	protected final String					modeChoice;
	protected final TransportMode[]			possibleModes;
	protected List<LinkNetworkRouteImpl> 	routes;
	private final Network network;
	protected PrintStream 					stream;
	boolean 								printing = false;
  private ControlerIO controlerIO;

	//////////////////////////////////////////////////////////////////////
	// Constructor
	//////////////////////////////////////////////////////////////////////

	public TimeModeChoicer1 (Controler controler, LegTravelTimeEstimatorFactory estimatorFactory, PlanScorer scorer){

		this.router 				= new PlansCalcRoute (controler.getConfig().plansCalcRoute(), controler.getNetwork(), controler.getTravelCostCalculator(), controler.getTravelTimeCalculator(), controler.getLeastCostPathCalculatorFactory());
		this.scorer 				= scorer;
		this.OFFSET					= Double.parseDouble(TimeModeChoicerConfigGroup.getOffset());
		this.MAX_ITERATIONS 		= Integer.parseInt(TimeModeChoicerConfigGroup.getMaxIterations());
		this.STOP_CRITERION			= Integer.parseInt(TimeModeChoicerConfigGroup.getStopCriterion());
		this.minimumTime			= new TreeMap<String, Double>();
		this.minimumTime.put("home", 7200.0);
		this.minimumTime.put("work", 3600.0);
		this.minimumTime.put("shopping", 3600.0);
		this.minimumTime.put("leisure", 3600.0);
		this.minimumTime.put("education_higher", 3600.0);
		this.minimumTime.put("education_kindergarten", 3600.0);
		this.minimumTime.put("education_other", 3600.0);
		this.minimumTime.put("education_primary", 3600.0);
		this.minimumTime.put("education_secondary", 3600.0);
		this.minimumTime.put("shop", 3600.0);
		this.minimumTime.put("work_sector2", 3600.0);
		this.minimumTime.put("work_sector3", 3600.0);
		this.minimumTime.put("tta", 3600.0);
		this.minimumTime.put("w", 3600.0);
		this.minimumTime.put("h", 7200.0);
		this.introTime				= this.minimumTime;
		this.NEIGHBOURHOOD_SIZE		= Integer.parseInt(TimeModeChoicerConfigGroup.getNeighbourhoodSize());
		this.maxWalkingDistance		= Double.parseDouble(TimeModeChoicerConfigGroup.getMaximumWalkingDistance());
		this.possibleModes			= TimeModeChoicerConfigGroup.getPossibleModes();
		this.modeChoice				= TimeModeChoicerConfigGroup.getModeChoice();
		this.routes					= null;
		this.network        = controler.getNetwork();
		this.controlerIO = controler.getControlerIO();

		// meisterk
		this.legTravelTimeEstimatorFactory = estimatorFactory;
		this.config					= controler.getConfig().planomat();
	}

	public TimeModeChoicer1 (Controler controler, DepartureDelayAverageCalculator tDepDelayCalc){

		this.router 				= new PlansCalcRoute (controler.getConfig().plansCalcRoute(), controler.getNetwork(), controler.getTravelCostCalculator(), controler.getTravelTimeCalculator(), controler.getLeastCostPathCalculatorFactory());
		this.scorer					= new PlanScorer (controler.getScoringFunctionFactory());

		LegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory = new LegTravelTimeEstimatorFactory(controler.getTravelTimeCalculator(), tDepDelayCalc);

		this.OFFSET					= Double.parseDouble(TimeModeChoicerConfigGroup.getOffset());
		this.MAX_ITERATIONS 		= Integer.parseInt(TimeModeChoicerConfigGroup.getMaxIterations());
		this.STOP_CRITERION			= Integer.parseInt(TimeModeChoicerConfigGroup.getStopCriterion());
		this.minimumTime			= new TreeMap<String, Double>();
		this.minimumTime.put("home", 7200.0);
		this.minimumTime.put("work", 3600.0);
		this.minimumTime.put("shopping", 3600.0);
		this.minimumTime.put("leisure", 3600.0);
		this.minimumTime.put("education_higher", 3600.0);
		this.minimumTime.put("education_kindergarten", 3600.0);
		this.minimumTime.put("education_other", 3600.0);
		this.minimumTime.put("education_primary", 3600.0);
		this.minimumTime.put("education_secondary", 3600.0);
		this.minimumTime.put("shop", 3600.0);
		this.minimumTime.put("work_sector2", 3600.0);
		this.minimumTime.put("work_sector3", 3600.0);
		this.minimumTime.put("tta", 3600.0);
		this.minimumTime.put("w", 3600.0);
		this.minimumTime.put("h", 7200.0);
		this.introTime				= this.minimumTime;
		this.NEIGHBOURHOOD_SIZE		= Integer.parseInt(TimeModeChoicerConfigGroup.getNeighbourhoodSize());
		this.maxWalkingDistance		= Double.parseDouble(TimeModeChoicerConfigGroup.getMaximumWalkingDistance());
		this.possibleModes			= TimeModeChoicerConfigGroup.getPossibleModes();
		this.modeChoice				= TimeModeChoicerConfigGroup.getModeChoice();
		this.routes					= null;
		this.network        = controler.getNetwork();

		//meisterk
		this.config 				= controler.getConfig().planomat();
		this.legTravelTimeEstimatorFactory = null;
	}

	// Constructor for test case
	public TimeModeChoicer1 (LegTravelTimeEstimatorFactory estimatorFactory, LegTravelTimeEstimator	estimator, PlanScorer scorer, PlansCalcRoute router, Network network){

		this.router 				= router;
		this.scorer 				= scorer;
		this.OFFSET					= Double.parseDouble(TimeModeChoicerConfigGroup.getOffset());
		this.MAX_ITERATIONS 		= Integer.parseInt(TimeModeChoicerConfigGroup.getMaxIterations());
		this.STOP_CRITERION			= Integer.parseInt(TimeModeChoicerConfigGroup.getStopCriterion());
		this.minimumTime			= new TreeMap<String, Double>();
		this.minimumTime.put("home", 7200.0);
		this.minimumTime.put("work", 3600.0);
		this.minimumTime.put("shopping", 3600.0);
		this.minimumTime.put("leisure", 3600.0);
		this.minimumTime.put("education_higher", 3600.0);
		this.minimumTime.put("education_kindergarten", 3600.0);
		this.minimumTime.put("education_other", 3600.0);
		this.minimumTime.put("education_primary", 3600.0);
		this.minimumTime.put("education_secondary", 3600.0);
		this.minimumTime.put("shop", 3600.0);
		this.minimumTime.put("work_sector2", 3600.0);
		this.minimumTime.put("work_sector3", 3600.0);
		this.minimumTime.put("tta", 3600.0);
		this.minimumTime.put("w", 3600.0);
		this.minimumTime.put("h", 7200.0);
		this.introTime				= this.minimumTime;
		this.NEIGHBOURHOOD_SIZE		= Integer.parseInt(TimeModeChoicerConfigGroup.getNeighbourhoodSize());
		this.maxWalkingDistance		= Double.parseDouble(TimeModeChoicerConfigGroup.getMaximumWalkingDistance());
		this.possibleModes			= TimeModeChoicerConfigGroup.getPossibleModes();
		this.modeChoice				= TimeModeChoicerConfigGroup.getModeChoice();
		this.routes					= null;
		this.network        = network;

		// meisterk
		this.legTravelTimeEstimatorFactory = estimatorFactory;
		this.config					= Gbl.getConfig().planomat();
		this.estimator = estimator;
	}


	//////////////////////////////////////////////////////////////////////
	// run() method
	//////////////////////////////////////////////////////////////////////


	public void run (Plan basePlan){

		if (printing){
			String outputfile = this.controlerIO.getOutputFilename("Timer_log"+Counter.timeOptCounter+"_"+basePlan.getPerson().getId()+".xls");
			Counter.timeOptCounter++;
			try {
				stream = new PrintStream (new File(outputfile));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return;
			}
			stream.print(basePlan.getScore()+"\t");
			for (int z= 0;z<basePlan.getPlanElements().size();z=z+2){
			ActivityImpl act = (ActivityImpl)basePlan.getPlanElements().get(z);
				stream.print(act.getType()+"\t");
			}
			stream.println();
			stream.print("\t");
			for (int z= 0;z<basePlan.getPlanElements().size();z=z+2){
				stream.print(((ActivityImpl)(basePlan.getPlanElements()).get(z)).getDuration()+"\t");
			}
			stream.println();
		}


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
			List<Id> l = ((NetworkRouteWRefs) oldRoute).getLinkIds();
			r.setLinkIds(oldRoute.getStartLinkId(), l, oldRoute.getEndLinkId());
			routes.add(r);
		}
		this.routes = routes;

		// meisterk
		//for (int z=1;z<plan.getPlanElements().size();z+=2) System.out.println("initial leg "+plan.getActLegIndex(plan.getPlanElements().get(z)));
		this.estimator = this.legTravelTimeEstimatorFactory.getLegTravelTimeEstimator(
				plan,
				this.config.getSimLegInterpretation(),
				this.config.getRoutingCapability(),
				this.router,
				this.network);

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

		/* Initial clean-up of plan in case actslegs is not sound*/
		double move = this.cleanSchedule (((ActivityImpl)(plan.getPlanElements().get(0))).getEndTime(), plan);
		int loops=1;
		while (move!=0.0){
			if (loops>3) {
				for (int i=0;i<plan.getPlanElements().size()-2;i=i+2){
					((ActivityImpl)plan.getPlanElements().get(i)).setDuration(this.minimumTime.get(((ActivityImpl)plan.getPlanElements().get(i)).getType()));
				}
				move = this.cleanSchedule(this.minimumTime.get(((ActivityImpl)plan.getPlanElements().get(0)).getType()), plan);
				if (move!=0.0){
					// TODO Check whether allowed?
					basePlan.setScore(-100000.0);	// Like this, PlanomatX will see that the solution is no proper solution
				//	log.info("No valid initial solution found for person "+plan.getPerson().getId()+"!");
				/*	for (int i=0;i<plan.getPlanElements().size();i++){
						if (i%2==0){
							ActivityImpl act = ((ActivityImpl) (plan.getPlanElements().get(i)));
							log.info("act "+i+" = "+act.getType()+", "+act.getStartTime()+", "+act.calculateDuration()+", "+act.getEndTime());
						}
						else{
							LegImpl leg = ((LegImpl) (plan.getPlanElements().get(i)));
							log.info("leg "+i+" = "+leg.getMode()+", "+leg.getRoute().getDistance()+", "+leg.getDepartureTime()+", "+leg.getTravelTime()+", "+leg.getArrivalTime());
						}
					}*/
					return;
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

			if (printing) stream.println("Iteration "+currentIteration);

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
		if (printing) stream.println("Selected solution\t"+bestScore);
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
				((LegImpl)al.get(i)).setDepartureTime(time);
				time = ((LegImpl)(bestSolution.get(i))).getArrivalTime();
				((LegImpl)al.get(i)).setArrivalTime(time);
				((LegImpl)al.get(i)).setMode(((LegImpl)(bestSolution.get(i))).getMode());

				RouteWRefs oldRoute = ((LegImpl)(bestSolution.get(i))).getRoute();
				LinkNetworkRouteImpl r = new LinkNetworkRouteImpl(oldRoute.getStartLinkId(), oldRoute.getEndLinkId(), this.network);
				List<Id> l = ((NetworkRouteWRefs) oldRoute).getLinkIds();
				r.setLinkIds(oldRoute.getStartLinkId(), l, oldRoute.getEndLinkId());
				((LegImpl)al.get(i)).setRoute(r);

			}
		}
		this.cleanRoutes(basePlan);

	}



	//////////////////////////////////////////////////////////////////////
	// Neighbourhood definition
	//////////////////////////////////////////////////////////////////////

	protected void createInitialNeighbourhood (PlanomatXPlan plan, List<? extends PlanElement> [] neighbourhood, double[]score, int [][] moves,
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


	protected void createNeighbourhood (PlanomatXPlan plan, List<? extends PlanElement> [] neighbourhood, double[]score, int[][] moves, int[]position,
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



	protected double increaseTime(PlanomatXPlan plan, List<? extends PlanElement> actslegs, int outer, int inner,
			PlanAnalyzeSubtours planAnalyzeSubtours, int[] subtourDis){

		if ((((ActivityImpl)(actslegs.get(inner))).getDuration()>=(this.OFFSET+this.minimumTime.get(((ActivityImpl)(actslegs.get(inner))).getType())))	||
				(outer==0	&&	inner==actslegs.size()-1)	||
				((inner==actslegs.size()-1) && (86400+((ActivityImpl)(actslegs.get(0))).getEndTime()-((ActivityImpl)(actslegs.get(actslegs.size()-1))).getStartTime())>(OFFSET+this.minimumTime.get(((ActivityImpl)(actslegs.get(0))).getType())))){

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



	protected double decreaseTime(PlanomatXPlan plan, List<? extends PlanElement> actslegs, int outer, int inner,
			PlanAnalyzeSubtours planAnalyzeSubtours, int[] subtourDis){
		boolean checkFinalAct = false;
		double time = OFFSET+this.minimumTime.get(((ActivityImpl)(actslegs.get(outer))).getType());
		if (outer==0 && inner==actslegs.size()-1) time = this.OFFSET+1;
		if (outer==0 && inner!=actslegs.size()-1){
			checkFinalAct = true; // if first act is decreased always check final act also in setTimes() to be above minimum time!
			if (((ActivityImpl)(actslegs.get(actslegs.size()-1))).getDuration()>=this.minimumTime.get(((ActivityImpl)(actslegs.get(0))).getType())) {
				time = OFFSET+1;
			}
		}
		if (((ActivityImpl)(actslegs.get(outer))).getDuration()>=time){
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


	protected double swapDurations (PlanomatXPlan plan, List<? extends PlanElement> actslegs, int outer, int inner, PlanAnalyzeSubtours planAnalyzeSubtours, int[] subtourDis){

		double swaptime= java.lang.Math.max(((ActivityImpl)(actslegs.get(inner))).getDuration(), this.minimumTime.get(((ActivityImpl)(actslegs.get(outer))).getType()))-((ActivityImpl)(actslegs.get(outer))).getDuration();
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



	protected double chooseMode (PlanomatXPlan plan, List<? extends PlanElement> actslegs, double offset, int outer, int inner,
			PlanAnalyzeSubtours planAnalyzeSubtours, int[]subtourDis){
		List<? extends PlanElement> actslegsResult = this.copyActsLegs(actslegs);
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
			for (int x=0;x<((actslegs.size()/2));x++){
				if (planAnalyzeSubtours.getSubtourIndexation()[x]==planAnalyzeSubtours.getSubtourIndexation()[outer/2]){
					if (!startFound) {
						start = x*2;
						startFound = true;
					}
					stop1 = (x*2)+2;
					((LegImpl)(actslegs.get(x*2+1))).setMode(this.possibleModes[i]);
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
					for (int x=0;x<((actslegs.size()/2));x++){
						if (planAnalyzeSubtours.getSubtourIndexation()[x]==planAnalyzeSubtours.getSubtourIndexation()[(inner/2)-1]){
							if ((x*2)<start) start = x*2;
							stop2 = (x*2)+2;
							((LegImpl)(actslegs.get(x*2+1))).setMode(this.possibleModes[j]);
						}
					}
					List<? extends PlanElement> actslegsInput = this.copyActsLegs(actslegs);
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
				List<? extends PlanElement> actslegsInput = this.copyActsLegs(actslegs);
				double tmpscore = this.setTimes(plan, actslegsInput, offset, outer, inner, start, stop1);

				if (tmpscore>score) {
					score = tmpscore;
					subtour1 = this.possibleModes[i];
					actslegsResult = this.copyActsLegs(actslegsInput);
				}
			}
		}
		for (int z=1;z<actslegs.size();z+=2){
			((LegImpl)(actslegs.get(z))).setDepartureTime(((LegImpl)(actslegsResult.get(z))).getDepartureTime());
			((LegImpl)(actslegs.get(z))).setTravelTime(((LegImpl)(actslegsResult.get(z))).getTravelTime());
			((LegImpl)(actslegs.get(z))).setArrivalTime(((LegImpl)(actslegsResult.get(z))).getArrivalTime());
		}
		for (int x=0;x<((actslegs.size()/2));x++){
			if (planAnalyzeSubtours.getSubtourIndexation()[x]==planAnalyzeSubtours.getSubtourIndexation()[outer/2]){
				((LegImpl)(actslegs.get((x*2)+1))).setMode(subtour1);
				continue;
			}
			if (planAnalyzeSubtours.getSubtourIndexation()[outer/2]!=planAnalyzeSubtours.getSubtourIndexation()[(inner/2)-1]){
				if (planAnalyzeSubtours.getSubtourIndexation()[x]==planAnalyzeSubtours.getSubtourIndexation()[(inner/2)-1]){
					((LegImpl)(actslegs.get((x*2)+1))).setMode(subtour2);
				}
			}
		}
		return score;
	}

	protected double chooseModeAllChains (PlanomatXPlan plan, List<? extends PlanElement> actslegsBase, PlanAnalyzeSubtours planAnalyzeSubtours, int[]subtourDis){
		List<? extends PlanElement> actslegsResult = this.copyActsLegs(actslegsBase);
		double score=-100000;
		ArrayList<int[]> subtourDistances = new ArrayList<int[]>();
		/* Set mode "walk" for all subtours with distance 0 */
		for (int i=0;i<planAnalyzeSubtours.getNumSubtours();i++){
			subtourDistances.add(new int []{i,0,subtourDis[i]}); // subtour, mode pointer, distance
			if (subtourDistances.get(subtourDistances.size()-1)[2]==0) {
				subtourDistances.remove(subtourDistances.size()-1);
				for (int j=1;j<plan.getPlanElements().size();j=j+2){
					if (planAnalyzeSubtours.getSubtourIndexation()[(j-1)/2]==i)((LegImpl)(actslegsBase.get(j))).setMode(TransportMode.walk);
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
				List<? extends PlanElement> actslegs = this.copyActsLegs(actslegsBase);
				for (int x=1;x<actslegs.size();x+=2){
					for (int y=0;y<subtourDistances.size();y++){
						if (planAnalyzeSubtours.getSubtourIndexation()[(x-1)/2]==subtourDistances.get(y)[0]){
							((LegImpl)(actslegs.get(x))).setMode(this.possibleModes[subtourDistances.get(y)[1]]);
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
			((LegImpl)(actslegsBase.get(z))).setDepartureTime(((LegImpl)(actslegsResult.get(z))).getDepartureTime());
			((LegImpl)(actslegsBase.get(z))).setTravelTime(((LegImpl)(actslegsResult.get(z))).getTravelTime());
			((LegImpl)(actslegsBase.get(z))).setArrivalTime(((LegImpl)(actslegsResult.get(z))).getArrivalTime());
			((LegImpl)(actslegsBase.get(z))).setMode(((LegImpl)(actslegsResult.get(z))).getMode());
		}
	return score;
	}

	//////////////////////////////////////////////////////////////////////
	// Help methods
	//////////////////////////////////////////////////////////////////////


	protected int findBestSolution (List<? extends PlanElement> [] neighbourhood, double[] score, int [][] moves, int[]position){

		int pointer=-1;
		double firstScore =-100000;
		for (int i=0;i<neighbourhood.length;i++){
			if (score[i]>firstScore){
				firstScore=score[i];
				pointer=i;
				position[0]=moves[i][0];
				position[1]=moves[i][1];
			}
			if (printing){
				stream.print(score[i]+"\t"+((LegImpl)(neighbourhood[i].get(1))).getDepartureTime()+"\t");
				stream.print(((LegImpl)(neighbourhood[i].get(1))).getRoute().getStartLinkId()+"\t"+((LegImpl)(neighbourhood[i].get(1))).getRoute().getEndLinkId()+"\t");
				stream.print(((LegImpl)(neighbourhood[i].get(1))).getMode()+"\t");
				for (int z= 2;z<neighbourhood[i].size()-1;z=z+2){
					stream.print((((LegImpl)(neighbourhood[i].get(z+1))).getDepartureTime()-((LegImpl)(neighbourhood[i].get(z-1))).getArrivalTime())+"\t");
					stream.print(((LegImpl)(neighbourhood[i].get(z+1))).getRoute().getStartLinkId()+"\t"+((LegImpl)(neighbourhood[i].get(z+1))).getRoute().getEndLinkId()+"\t");
					stream.print(((LegImpl)(neighbourhood[i].get(z+1))).getMode()+"\t");
				}
				stream.print(86400-((LegImpl)(neighbourhood[i].get(neighbourhood[i].size()-2))).getArrivalTime()+"\t");
				stream.println();
			}
		}
		if (printing) stream.println("Iteration's best score\t"+firstScore);

		/* clean-up acts of plan (=bestIterSolution) */
		if (pointer!=-1) this.cleanActs(neighbourhood[pointer]);

		return pointer;
	}


	protected double cleanSchedule (double now, PlanImpl plan){

		((ActivityImpl)(plan.getPlanElements().get(0))).setEndTime(now);
		((ActivityImpl)(plan.getPlanElements().get(0))).setDuration(now);

		double travelTime;
		for (int i=1;i<=plan.getPlanElements().size()-2;i=i+2){
		//	log.info("Urspr�ngliche Reisezeit: "+(((LegImpl)(plan.getPlanElements().get(i))).getArrivalTime()-((LegImpl)(plan.getPlanElements().get(i))).getDepartureTime()));
			((LegImpl)(plan.getPlanElements().get(i))).setDepartureTime(now);
//			statement was replaced by the one below
//			travelTime = this.estimator.getInterpolation(plan.getPerson().getId(), now, (ActivityImpl)(plan.getPlanElements().get(i-1)), (ActivityImpl)(plan.getPlanElements().get(i+1)), (LegImpl)(plan.getPlanElements().get(i)));
		//	log.info("clean schedule leg "+plan.getActLegIndex(plan.getPlanElements().get(i)));
			travelTime = this.estimator.getLegTravelTimeEstimation(
					plan.getPerson().getId(),
					now,
					(ActivityImpl)(plan.getPlanElements().get(i-1)),
					(ActivityImpl)(plan.getPlanElements().get(i+1)),
					(LegImpl)(plan.getPlanElements().get(i)),
					false);
			if (((LegImpl)(plan.getPlanElements().get(i))).getMode()!=TransportMode.car){
				((LegImpl)(plan.getPlanElements().get(i))).setRoute(this.routes.get((i/2)));
			}
			//log.info("Neue Reisezeit: "+travelTime);
			((LegImpl)(plan.getPlanElements().get(i))).setArrivalTime(now+travelTime);
			((LegImpl)(plan.getPlanElements().get(i))).setTravelTime(travelTime);
			now+=travelTime;

			if (i!=plan.getPlanElements().size()-2){
				((ActivityImpl)(plan.getPlanElements().get(i+1))).setStartTime(now);
				//travelTime = java.lang.Math.max(((Leg)(plan.getPlanElements().get(i+2))).getDepartureTime()-((Leg)(plan.getPlanElements().get(i))).getArrivalTime()/*-travelTime*/, this.minimumTime.get(((Activity)(plan.getPlanElements().get(i+1))).getType()));
				travelTime = java.lang.Math.max(((ActivityImpl)(plan.getPlanElements().get(i+1))).getDuration()/*-travelTime*/, this.minimumTime.get(((ActivityImpl)(plan.getPlanElements().get(i+1))).getType()));
				((ActivityImpl)(plan.getPlanElements().get(i+1))).setDuration(travelTime);
				((ActivityImpl)(plan.getPlanElements().get(i+1))).setEndTime(now+travelTime);
				now+=travelTime;
			}
			else {
				((ActivityImpl)(plan.getPlanElements().get(i+1))).setStartTime(now);
				if (86400>now+this.minimumTime.get(((ActivityImpl)(plan.getPlanElements().get(i+1))).getType())){
					((ActivityImpl)(plan.getPlanElements().get(i+1))).setDuration(86400-now);
					((ActivityImpl)(plan.getPlanElements().get(i+1))).setEndTime(86400);
				}
				else if (86400+((ActivityImpl)(plan.getPlanElements().get(0))).getDuration()>now+this.minimumTime.get(((ActivityImpl)(plan.getPlanElements().get(i+1))).getType())){
					if (now<86400){
						((ActivityImpl)(plan.getPlanElements().get(i+1))).setDuration(86400-now);
						((ActivityImpl)(plan.getPlanElements().get(i+1))).setEndTime(86400);
					}
					else {
					((ActivityImpl)(plan.getPlanElements().get(i+1))).setDuration(this.minimumTime.get(((ActivityImpl)(plan.getPlanElements().get(i+1))).getType()));
					((ActivityImpl)(plan.getPlanElements().get(i+1))).setEndTime(now+this.minimumTime.get(((ActivityImpl)(plan.getPlanElements().get(i+1))).getType()));
					}
				}
				else {
					return (now+this.minimumTime.get(((ActivityImpl)(plan.getPlanElements().get(i+1))).getType())-(86400+((ActivityImpl)(plan.getPlanElements().get(0))).getDuration()));
				}
			}
		}
		return 0;
	}


	protected void cleanActs (List<? extends PlanElement> actslegs){

		((ActivityImpl)(actslegs.get(0))).setEndTime(((LegImpl)(actslegs.get(1))).getDepartureTime());
		((ActivityImpl)(actslegs.get(0))).setDuration(((LegImpl)(actslegs.get(1))).getDepartureTime());

		for (int i=2;i<=actslegs.size()-1;i=i+2){

			if (i!=actslegs.size()-1){
				((ActivityImpl)(actslegs.get(i))).setStartTime(((LegImpl)(actslegs.get(i-1))).getArrivalTime());
				((ActivityImpl)(actslegs.get(i))).setEndTime(((LegImpl)(actslegs.get(i+1))).getDepartureTime());
				((ActivityImpl)(actslegs.get(i))).setDuration(((LegImpl)(actslegs.get(i+1))).getDepartureTime()-((LegImpl)(actslegs.get(i-1))).getArrivalTime());
				if (((ActivityImpl)(actslegs.get(i))).getDuration()<this.minimumTime.get(((ActivityImpl)(actslegs.get(i))).getType())-2) log.warn("duration < minimumTime: "+((ActivityImpl)(actslegs.get(i))).getDuration()+"; Pos = "+i+" von = "+(actslegs.size()-1));
			}
			else {
				((ActivityImpl)(actslegs.get(i))).setStartTime(((LegImpl)(actslegs.get(i-1))).getArrivalTime());
				if (((LegImpl)(actslegs.get(i-1))).getArrivalTime()>86400){
					((ActivityImpl)(actslegs.get(i))).setDuration(0);
					//((Act)(actslegs.get(i))).setStartTime(((Leg)(actslegs.get(i-1))).getArrivalTime());
					((ActivityImpl)(actslegs.get(i))).setEndTime(((ActivityImpl)(actslegs.get(i))).getStartTime()); // new
				}
				else {
					((ActivityImpl)(actslegs.get(i))).setDuration(86400-((LegImpl)(actslegs.get(i-1))).getArrivalTime());
					((ActivityImpl)(actslegs.get(i))).setEndTime(86400);
				}
			}
		}
	}

	protected void cleanRoutes (Plan plan){

		for (int i=1;i<plan.getPlanElements().size();i=i+2){
//			statement was replaced by the one below
//			double travelTime = this.estimator.getInterpolation(
//					plan.getPerson().getId(),
//					((LegImpl)(plan.getPlanElements().get(i))).getDepartureTime(),
//					((ActivityImpl)(plan.getPlanElements().get(i-1))),
//					((ActivityImpl)(plan.getPlanElements().get(i+1))),
//					((LegImpl)(plan.getPlanElements().get(i))));
			//System.out.println("clean routes leg "+plan.getActLegIndex(plan.getPlanElements().get(i)));
			double travelTime = this.estimator.getLegTravelTimeEstimation(
					plan.getPerson().getId(),
					((LegImpl)(plan.getPlanElements().get(i))).getDepartureTime(),
					((ActivityImpl)(plan.getPlanElements().get(i-1))),
					((ActivityImpl)(plan.getPlanElements().get(i+1))),
					((LegImpl)(plan.getPlanElements().get(i))),
					false);
			if (java.lang.Math.abs(travelTime-((LegImpl)(plan.getPlanElements().get(i))).getTravelTime())>0) log.warn("Hier passt was nicht: Person "+plan.getPerson().getId()+", " +
					"leg "+i+" mit mode "+((LegImpl)(plan.getPlanElements().get(i))).getMode()+", orig traveltime "+((LegImpl)(plan.getPlanElements().get(i))).getTravelTime()+" " +
							"und neue travel time "+travelTime+", Distance "+((LegImpl)(plan.getPlanElements().get(i))).getRoute().getDistance());
		}
	}


	protected List<? extends PlanElement> copyActsLegs (List<? extends PlanElement> in){
		ArrayList<PlanElement> out = new ArrayList<PlanElement>();
		for (PlanElement pe : in) {
			if (pe instanceof ActivityImpl) {
				out.add(new ActivityImpl ((ActivityImpl) pe));
			} else if (pe instanceof LegImpl) {
				LegImpl inl = ((LegImpl) pe);
				LegImpl l = new LegImpl (inl.getMode());
				l.setArrivalTime(inl.getArrivalTime());
				l.setDepartureTime(inl.getDepartureTime());
				l.setTravelTime(inl.getTravelTime());
				l.setRoute(inl.getRoute());

				out.add(l);
			}
		}
		return out;
	}

	@SuppressWarnings("unchecked")
	protected double setTimes (PlanomatXPlan plan, List<? extends PlanElement> actslegs, double offset, int outer, int inner, int start, int stop){
		double travelTime;
		double now = ((LegImpl)(actslegs.get(start+1))).getDepartureTime();
		int position = 0;	// indicates whether time setting has reached parameter "stop"

		/* if start < outer (mode choice) */
		for (int i=start+1;i<=outer-1;i=i+2){
			((LegImpl)(actslegs.get(i))).setDepartureTime(now);
//			statement was replaced by the one below
//			travelTime = this.estimator.getInterpolation(
//					plan.getPerson().getId(),
//					now,
//					(ActivityImpl)(actslegs.get(i-1)),
//					(ActivityImpl)(actslegs.get(i+1)),
//					(LegImpl)(actslegs.get(i))
//					);
			//System.out.println("setTimes1 leg "+plan.getActLegIndex(actslegs.get(i)));
			travelTime = this.estimator.getLegTravelTimeEstimation(
					plan.getPerson().getId(),
					now,
					(ActivityImpl)(actslegs.get(i-1)),
					(ActivityImpl)(actslegs.get(i+1)),
					(LegImpl)(actslegs.get(i)),
					false);
			if (((LegImpl)(actslegs.get(i))).getMode()!=TransportMode.car){
				((LegImpl)(actslegs.get(i))).setRoute(this.routes.get((i-1)/2));

			}

			((LegImpl)(actslegs.get(i))).setArrivalTime(now+travelTime);
			((LegImpl)(actslegs.get(i))).setTravelTime(travelTime);
			now = java.lang.Math.max(now+travelTime+this.minimumTime.get(((ActivityImpl)(actslegs.get(i+1))).getType()), ((ActivityImpl)(actslegs.get(i+1))).getEndTime());
		}

		/* standard process */
		for (int i=outer+1;i<=inner-1;i=i+2){
			if (i==outer+1) {
				if (outer!=0) {
					now = java.lang.Math.max(now+offset, (((LegImpl)(actslegs.get(outer-1))).getArrivalTime())+this.minimumTime.get(((ActivityImpl)(actslegs.get(outer))).getType()));
				}
				else now +=offset;
			}
			((LegImpl)(actslegs.get(i))).setDepartureTime(now);
//			statement was replaced by the one below
//			travelTime = this.estimator.getInterpolation(
//					plan.getPerson().getId(),
//					now,
//					(ActivityImpl)(actslegs.get(i-1)),
//					(ActivityImpl)(actslegs.get(i+1)),
//					(LegImpl)(actslegs.get(i))
//					);
			//System.out.println("setTimes2 leg "+plan.getActLegIndex(actslegs.get(i)));
			travelTime = this.estimator.getLegTravelTimeEstimation(
					plan.getPerson().getId(),
					now,
					(ActivityImpl)(actslegs.get(i-1)),
					(ActivityImpl)(actslegs.get(i+1)),
					(LegImpl)(actslegs.get(i)),
					false);
			//System.out.println("travel time "+travelTime);
			if (((LegImpl)(actslegs.get(i))).getMode()!=TransportMode.car){
				((LegImpl)(actslegs.get(i))).setRoute(this.routes.get((i-1)/2));

			}
			((LegImpl)(actslegs.get(i))).setArrivalTime(now+travelTime);
			((LegImpl)(actslegs.get(i))).setTravelTime(travelTime);
			now+=travelTime;

			if (i!=inner-1){
				now = java.lang.Math.max(now+this.minimumTime.get(((ActivityImpl)(actslegs.get(i+1))).getType()), (((ActivityImpl)(actslegs.get(i+1))).getEndTime()+offset));
				if (((ActivityImpl)(actslegs.get(i+1))).getDuration()<this.minimumTime.get(((ActivityImpl)(actslegs.get(i+1))).getType())-2) log.warn("Eingehende duration < minimumTime! "+((ActivityImpl)(actslegs.get(i+1))).getDuration());
			}
			else {
				double time1 = ((ActivityImpl)(actslegs.get(i+1))).getEndTime();
				if (inner==actslegs.size()-1) {
					time1=((LegImpl)(actslegs.get(1))).getDepartureTime()+86400;
				}
				position = inner;
				if (time1<now+this.minimumTime.get(((ActivityImpl)(actslegs.get(i+1))).getType())){	// check whether act "inner" has at least minimum time
					if (actslegs.size()>=i+3){
						now+=this.minimumTime.get(((ActivityImpl)(actslegs.get(i+1))).getType());
						((LegImpl)(actslegs.get(i+2))).setDepartureTime(now);
//						statement was replaced by the one below
//						travelTime = this.estimator.getInterpolation(
//								plan.getPerson().getId(),
//								now,
//								(ActivityImpl)(actslegs.get(i+1)),
//								(ActivityImpl)(actslegs.get(i+3)),
//								(LegImpl)(actslegs.get(i+2))
//								);
						//System.out.println("setTimes3 leg "+plan.getActLegIndex(actslegs.get(i+2)));
						travelTime = this.estimator.getLegTravelTimeEstimation(
								plan.getPerson().getId(),
								now,
								(ActivityImpl)(actslegs.get(i+1)),
								(ActivityImpl)(actslegs.get(i+3)),
								(LegImpl)(actslegs.get(i+2)),
								false);
						if (((LegImpl)(actslegs.get(i+2))).getMode()!=TransportMode.car){
							((LegImpl)(actslegs.get(i+2))).setRoute(this.routes.get((i+1)/2));
						}
						((LegImpl)(actslegs.get(i+2))).setArrivalTime(now+travelTime);
						((LegImpl)(actslegs.get(i+2))).setTravelTime(travelTime);
						now+=travelTime;
						double time2 = ((ActivityImpl)(actslegs.get(i+3))).getEndTime();
						if (i+3==actslegs.size()-1) {
							time2=((LegImpl)(actslegs.get(1))).getDepartureTime()+86400;
						}
						position = i+3;
						if (time2<now+this.minimumTime.get(((ActivityImpl)(actslegs.get(i+3))).getType())){
							return -100000;
						}
					}
					else return -100000;
				}
			}
		}

		/* if position < stop (mode choice) */
		if (position < stop){
			now = ((LegImpl)(actslegs.get(position+1))).getDepartureTime();
			for (int i=position+1;i<=stop-1;i=i+2){
				((LegImpl)(actslegs.get(i))).setDepartureTime(now);
//				statement was replaced by the one below
//				travelTime = this.estimator.getInterpolation(
//						plan.getPerson().getId(),
//						now,
//						(ActivityImpl)(actslegs.get(i-1)),
//						(ActivityImpl)(actslegs.get(i+1)),
//						(LegImpl)(actslegs.get(i))
//						);
				//System.out.println("setTimes4 leg "+plan.getActLegIndex(actslegs.get(i)));
				travelTime = this.estimator.getLegTravelTimeEstimation(
						plan.getPerson().getId(),
						now,
						(ActivityImpl)(actslegs.get(i-1)),
						(ActivityImpl)(actslegs.get(i+1)),
						(LegImpl)(actslegs.get(i)),
						false);
				if (((LegImpl)(actslegs.get(i))).getMode()!=TransportMode.car){
					((LegImpl)(actslegs.get(i))).setRoute(this.routes.get((i-1)/2));
				}
				((LegImpl)(actslegs.get(i))).setArrivalTime(now+travelTime);
				((LegImpl)(actslegs.get(i))).setTravelTime(travelTime);
				now+=travelTime;
				now = java.lang.Math.max(now+this.minimumTime.get(((ActivityImpl)(actslegs.get(i+1))).getType()), ((ActivityImpl)(actslegs.get(i+1))).getEndTime());
				if (i+1==actslegs.size()-1){
					double time=((LegImpl)(actslegs.get(1))).getDepartureTime()+86400;
					if (time<now){
						return -100000;
					}
				}
				else {
					if (now>((ActivityImpl)(actslegs.get(i+1))).getEndTime()){
						((LegImpl)(actslegs.get(i+2))).setDepartureTime(now);
//						statement was replaced by the one below
//						travelTime = this.estimator.getInterpolation(
//								plan.getPerson().getId(),
//								now,
//								(ActivityImpl)(actslegs.get(i+1)),
//								(ActivityImpl)(actslegs.get(i+3)),
//								(LegImpl)(actslegs.get(i+2))
//								);
						//System.out.println("setTimes5 leg "+plan.getActLegIndex(actslegs.get(i+2)));
						travelTime = this.estimator.getLegTravelTimeEstimation(
								plan.getPerson().getId(),
								now,
								(ActivityImpl)(actslegs.get(i+1)),
								(ActivityImpl)(actslegs.get(i+3)),
								(LegImpl)(actslegs.get(i+2)),
								false);
						if (((LegImpl)(actslegs.get(i+2))).getMode()!=TransportMode.car){
							((LegImpl)(actslegs.get(i+2))).setRoute(this.routes.get((i+1)/2));
						}
						((LegImpl)(actslegs.get(i+2))).setArrivalTime(now+travelTime);
						((LegImpl)(actslegs.get(i+2))).setTravelTime(travelTime);
						now+=travelTime;
						double time3 = ((ActivityImpl)(actslegs.get(i+3))).getEndTime();
						if ((i+3)==actslegs.size()-1) {
							time3=((LegImpl)(actslegs.get(1))).getDepartureTime()+86400;
						}
						if (time3<now+this.minimumTime.get(((ActivityImpl)(actslegs.get(i+3))).getType())){
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
	protected int checksubtourDistance2 (List<? extends PlanElement> actslegs, PlanAnalyzeSubtours planAnalyzeSubtours, int pos){
		double distance = 0;
		for (int k=0;k<((actslegs.size()/2));k++){
			if ((planAnalyzeSubtours.getSubtourIndexation()[k])==pos){
				distance=distance+CoordUtils.calcDistance(((ActivityImpl)(actslegs.get(k*2))).getCoord(), ((ActivityImpl)(actslegs.get(k*2+2))).getCoord());
				if (distance>this.maxWalkingDistance) {
					return 2;
				}
			}
		}
		if (distance==0) return 0;
		return 1;
	}
}



