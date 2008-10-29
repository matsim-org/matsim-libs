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

import org.apache.log4j.Logger;
import org.matsim.controler.Controler;
import org.matsim.gbl.MatsimRandom;
import org.matsim.network.NetworkLayer;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.router.PlansCalcRouteLandmarks;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.scoring.PlanScorer;
import org.matsim.scoring.ScoringFunctionFactory;
import org.matsim.utils.charts.XYLineChart;
import java.util.ArrayList;
import java.io.*;




/**
 * @author Matthias Feil
 * Like PlanomatX11 but also calling TimeOptimizer.
 */

public class PlanomatX15 implements org.matsim.population.algorithms.PlanAlgorithm { 
	
	private final int						NEIGHBOURHOOD_SIZE, MAX_ITERATIONS;
	private final double					WEIGHT_CHANGE_ORDER, WEIGHT_CHANGE_NUMBER;
	private final double 					WEIGHT_INC_NUMBER;
	//private final PlanAlgorithm 			planomatAlgorithm;
	private final PlanAlgorithm				timer;
	private final PlansCalcRouteLandmarks 	router;
	private final PlanScorer 				scorer;
	private static final Logger 			log = Logger.getLogger(PlanomatX15.class);
	
	//////////////////////////////////////////////////////////////////////
	// Constructor
	//////////////////////////////////////////////////////////////////////
		
	public PlanomatX15 (LegTravelTimeEstimator legTravelTimeEstimator, NetworkLayer network, TravelCost costCalculator,
			TravelTime timeCalculator, PreProcessLandmarks commonRouterDatafinal, ScoringFunctionFactory factory) {

		//this.planomatAlgorithm 		= new PlanOptimizeTimes (legTravelTimeEstimator);
		this.router 				= new PlansCalcRouteLandmarks (network, commonRouterDatafinal, costCalculator, timeCalculator);
		//this.scorer 				= new PlanomatXPlanScorer (factory);
		this.scorer 				= new PlanScorer (factory);
		this.timer					= new TimeOptimizer10(legTravelTimeEstimator, this.scorer);
		
		this.NEIGHBOURHOOD_SIZE 	= 10;				//TODO @MF: constants to be configured externally, sum must be smaller than or equal to 1.0
		this.WEIGHT_CHANGE_ORDER 	= 0.2; 
		this.WEIGHT_CHANGE_NUMBER 	= 0.6;
		this.WEIGHT_INC_NUMBER 		= 0.5; 				//Weighing whether adding or removing activities in change number method.
		this.MAX_ITERATIONS 		= 50;
	}
	
		
	//////////////////////////////////////////////////////////////////////
	// run() method
	//////////////////////////////////////////////////////////////////////
	
	//Definition of control output file
    

	
	public void run (Plan plan){
		
		long runStartTime = System.currentTimeMillis();
		long planomatRunTime = 0;
		int numberPlanomatCalls = 0;
			
		// Instantiate all necessary lists and arrays
		PlanomatXPlan [] neighbourhood 					= new PlanomatXPlan [NEIGHBOURHOOD_SIZE+1];
		int [] notNewInNeighbourhood 					= new int [NEIGHBOURHOOD_SIZE];
		int [] tabuInNeighbourhood 						= new int [NEIGHBOURHOOD_SIZE];
		int [] scoredInNeighbourhood					= new int [NEIGHBOURHOOD_SIZE];
		ArrayList<PlanomatXPlan> nonTabuNeighbourhood 	= new ArrayList<PlanomatXPlan>();
		ArrayList<PlanomatXPlan> tabuList			 	= new ArrayList<PlanomatXPlan>();
		ArrayList<PlanomatXPlan> solution3 				= new ArrayList<PlanomatXPlan>();
		ArrayList<PlanomatXPlan> solution5 				= new ArrayList<PlanomatXPlan>();
		ArrayList<PlanomatXPlan> solution7 				= new ArrayList<PlanomatXPlan>();
		ArrayList<PlanomatXPlan> solution9 				= new ArrayList<PlanomatXPlan>();
		ArrayList<PlanomatXPlan> solution11 			= new ArrayList<PlanomatXPlan>();
		ArrayList<PlanomatXPlan> solution13				= new ArrayList<PlanomatXPlan>();
		ArrayList<PlanomatXPlan> solutionLong			= new ArrayList<PlanomatXPlan>();
		boolean warningTabu;
		double [] xs;
		double [] ys = new double [MAX_ITERATIONS+1];
		
		String outputfile = Controler.getOutputFilename(Counter.counter+"_"+plan.getPerson().getId()+"_detailed_log.xls");
		String outputfileOverview = Controler.getOutputFilename("overview_log.xls");
		
		Counter.counter++;
		PrintStream stream;
		try {
			stream = new PrintStream (new File(outputfile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		stream.println("Score\tnotNewInNeighbourhood\ttabuInNeighbourhood\tscoredInNeighbourhood\tActivity schedule");
		
		FileOutputStream fileOverview;
		PrintStream overview;
		try {
			fileOverview = new FileOutputStream(new File(outputfileOverview), true);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		overview = new PrintStream (fileOverview);
				
		// Copy the plan into all fields of the array neighbourhood
		int neighbourhoodInitialisation;
		for (neighbourhoodInitialisation = 0; neighbourhoodInitialisation < neighbourhood.length; neighbourhoodInitialisation++){
			neighbourhood[neighbourhoodInitialisation] = new PlanomatXPlan (plan.getPerson());
			neighbourhood[neighbourhoodInitialisation].copyPlan(plan);			
		}
		
		// Write the given plan into the tabuList
		tabuList.add(neighbourhood[NEIGHBOURHOOD_SIZE]);
		stream.println("0\t"+neighbourhood[NEIGHBOURHOOD_SIZE].getScore());
		ys[0]=neighbourhood[NEIGHBOURHOOD_SIZE].getScore();
		
		// Do Tabu Search iterations
		int currentIteration;
		for (currentIteration = 1; currentIteration<=MAX_ITERATIONS;currentIteration++){
			stream.println("Iteration "+currentIteration);
			//streamOverview.print("Iteration "+currentIteration+"\t");
			
			// Define the neighbourhood
			this.createNeighbourhood(neighbourhood, notNewInNeighbourhood);	
			
			// Check whether differing plans are tabu
			warningTabu = this.checkForTabuSolutions(tabuList, neighbourhood, notNewInNeighbourhood, tabuInNeighbourhood);
			if (warningTabu) {
				log.info("No non-tabu solutions availabe for person "+plan.getPerson().getId()+" at iteration "+currentIteration);
				break; 
			}
			
			// Check whether a non-tabu solution has been scored in a previous iteration			
			this.checkForScoredSolution(neighbourhood, tabuInNeighbourhood, scoredInNeighbourhood, solution3, solution5, solution7, solution9,
					solution11, solution13, solutionLong, nonTabuNeighbourhood);
			
			// Route, optimize and score all non-tabu/non-scored plans, write them into list nonTabuNeighbourhood and sort the list
			for (int x=0; x<NEIGHBOURHOOD_SIZE;x++){
				if(scoredInNeighbourhood[x]==0){
					
					//Routing
					this.router.run(neighbourhood[x]);
										
					//Optimizing the start times
					numberPlanomatCalls++;
					long planomatStartTime = System.currentTimeMillis();
					//this.planomatAlgorithm.run (neighbourhood[x]); //Calling standard Planomat to optimise start times and mode choice
					this.timer.run(neighbourhood[x]);
					planomatRunTime += (System.currentTimeMillis()-planomatStartTime);
					
					// Scoring
					//log.info("Score davor = "+neighbourhood[x].getScore());
					//neighbourhood[x].setScore(scorer.getScore(neighbourhood[x]));
					//log.info("Score danach = "+neighbourhood[x].getScore());
					
					nonTabuNeighbourhood.add(neighbourhood[x]);
					
					// Write the solution into a list so that it can be retrieved for later iterations
					PlanomatXPlan solution = new PlanomatXPlan (neighbourhood[x].getPerson());
					solution.copyPlan(neighbourhood[x]);
					
					if (solution.getActsLegs().size()==3) solution3.add(solution);
					else if (solution.getActsLegs().size()==5) solution5.add(solution);
					else if (solution.getActsLegs().size()==7) solution7.add(solution);
					else if (solution.getActsLegs().size()==9) solution9.add(solution);
					else if (solution.getActsLegs().size()==11) solution11.add(solution);
					else if (solution.getActsLegs().size()==13) solution13.add(solution);
					else solutionLong.add(solution);
				}
				stream.print(neighbourhood[x].getScore()+"\t");
				stream.print(notNewInNeighbourhood[x]+"\t");
				stream.print(tabuInNeighbourhood[x]+"\t");
				stream.print(scoredInNeighbourhood[x]+"\t");
				for (int i= 0;i<neighbourhood[x].getActsLegs().size();i=i+2){
					Act act = (Act)neighbourhood[x].getActsLegs().get(i);
					stream.print(act.getType()+"\t");
				}
				stream.println();
			}
			
			// Find best non-tabu plan. Becomes this iteration's solution. Write it into the tabuList
			java.util.Collections.sort(nonTabuNeighbourhood);
			PlanomatXPlan bestIterSolution = new PlanomatXPlan (nonTabuNeighbourhood.get(nonTabuNeighbourhood.size()-1).getPerson());
			bestIterSolution.copyPlan(nonTabuNeighbourhood.get(nonTabuNeighbourhood.size()-1));
			tabuList.add(bestIterSolution);
			
			// Statistics
			stream.println("Iteration "+currentIteration+"\t"+bestIterSolution.getScore());
			//streamOverview.println(bestIterSolution.getScore());
			ys[currentIteration]=bestIterSolution.getScore();
			

			if (this.MAX_ITERATIONS==currentIteration){
				log.info("Tabu Search regularly finished for person "+plan.getPerson().getId()+" at iteration "+currentIteration);	
			}
			else {
				// Write this iteration's solution into all neighbourhood fields for the next iteration
				for (int initialisationOfNextIteration = 0;initialisationOfNextIteration<NEIGHBOURHOOD_SIZE+1; initialisationOfNextIteration++){
					neighbourhood[initialisationOfNextIteration] = new PlanomatXPlan (bestIterSolution.getPerson());
					neighbourhood[initialisationOfNextIteration].copyPlan(bestIterSolution);
				}
				// Reset the nonTabuNeighbourhood list
				nonTabuNeighbourhood.clear();
			}	
		}
		
		// Update the plan with the final solution 		
		java.util.Collections.sort(tabuList);
		stream.println("Selected solution\t"+tabuList.get(tabuList.size()-1).getScore());
		ArrayList<Object> al = plan.getActsLegs();
		
		log.info("Finale actslegs für Person "+tabuList.get(tabuList.size()-1).getPerson().getId()+": "+tabuList.get(tabuList.size()-1).getActsLegs());
		
		xs = new double [currentIteration];
		for (int i = 0;i<xs.length;i++)xs[i]=i+1;
		
		if(al.size()>tabuList.get(tabuList.size()-1).getActsLegs().size()){ 
			int i;
			for (i = 0; i<tabuList.get(tabuList.size()-1).getActsLegs().size();i++){
				al.remove(i);
				al.add(i, tabuList.get(tabuList.size()-1).getActsLegs().get(i));	
			}
			for (int j = i; j<al.size();j=j+0){
				al.remove(j);
			}
		}
		else if(al.size()<tabuList.get(tabuList.size()-1).getActsLegs().size()){
			int i;
			for (i = 0; i<al.size();i++){
				al.remove(i);
				al.add(i, tabuList.get(tabuList.size()-1).getActsLegs().get(i));	
			}
			for (int j = i; j<tabuList.get(tabuList.size()-1).getActsLegs().size();j++){			
				al.add(j, tabuList.get(tabuList.size()-1).getActsLegs().get(j));
			}
		}
		else {
			for (int i = 0; i<al.size();i++){
			al.remove(i);
			al.add(i, tabuList.get(tabuList.size()-1).getActsLegs().get(i));	
			}
		}
		XYLineChart chart = new XYLineChart("Score Statistics", "iteration", "score");
		chart.addSeries("score", xs, ys);
		chart.addMatsimLogo();
		chart.saveAsPng(Controler.getOutputFilename(Counter.counter+"_"+plan.getPerson().getId()+"scorestats_.png"), 800, 600);
		
		stream.println("\nDauer der Planomat-Aufrufe: "+planomatRunTime);
		stream.println ("Dauer der run() Methode: "+(System.currentTimeMillis()-runStartTime));
		stream.println("Anzahl der Planomat-Aufrufe: "+numberPlanomatCalls);				
		stream.close();
		
		
		overview.println(Counter.counter+"_"+plan.getPerson().getId()+"\t"+planomatRunTime+"\t"+(System.currentTimeMillis()-runStartTime)+"\t"+numberPlanomatCalls);
		overview.close();
	}
   
				
	//////////////////////////////////////////////////////////////////////
	// Neighbourhood definition 
	//////////////////////////////////////////////////////////////////////
	
	public void createNeighbourhood (PlanomatXPlan [] neighbourhood, int[] notNewInNeighbourhood) {
		int neighbourPos;
		int [] changePositions = {2,4};
		for (neighbourPos = 0; neighbourPos<(int)(NEIGHBOURHOOD_SIZE*WEIGHT_CHANGE_ORDER); neighbourPos++){
			notNewInNeighbourhood[neighbourPos] = this.changeOrder(neighbourhood[neighbourPos], changePositions);
		}
		int[] numberPositions = {0,0,1,1};		// "where to add activity, where to remove activity, number of adding cycles, number of removing cycles"
		int[] actsToBeAdded = new int [(int)(neighbourhood[0].getActsLegs().size()/2)+1];
		for (neighbourPos = (int) (NEIGHBOURHOOD_SIZE*WEIGHT_CHANGE_ORDER); neighbourPos<(int)(NEIGHBOURHOOD_SIZE*(WEIGHT_CHANGE_ORDER+WEIGHT_CHANGE_NUMBER)); neighbourPos++){
			notNewInNeighbourhood[neighbourPos] = this.changeNumber(neighbourhood[neighbourPos], WEIGHT_INC_NUMBER, numberPositions, actsToBeAdded);
		}
		int [] typePosition = {0,1};
		typePosition[0]=(int)(MatsimRandom.random.nextDouble()*((int)(neighbourhood[0].getActsLegs().size()/2)-1))+1;
		int [] actsToBeChanged = new int [actsToBeAdded.length];
		for (int i = 0; i<actsToBeChanged.length;i++){
			actsToBeChanged[i] = (int)(MatsimRandom.random.nextDouble()* PlanomatXInitialiser.actTypes.size());
		}
		for (neighbourPos = (int)(NEIGHBOURHOOD_SIZE*(WEIGHT_CHANGE_ORDER+WEIGHT_CHANGE_NUMBER)); neighbourPos<NEIGHBOURHOOD_SIZE; neighbourPos++){
			notNewInNeighbourhood[neighbourPos] = this.changeType(neighbourhood[neighbourPos], typePosition, actsToBeChanged);
		}
	}
			
	
	
	public int changeOrder (PlanomatXPlan basePlan, int [] positions){
	
		ArrayList<Object> actslegs = basePlan.getActsLegs();
		
		if (actslegs.size()<=5){	//If true the plan has not enough activities to change their order. Do nothing.		
			return 1;
		}
		else {
			for (int planBasePos = positions[0]; planBasePos < actslegs.size()-4; planBasePos=planBasePos+2){			
				for (int planRunningPos = positions[1]; planRunningPos < actslegs.size()-2; planRunningPos=planRunningPos+2){ //Go through the "inner" acts only
					positions[1] = positions[1]+2;
					
					//Activity swapping				
					Act act0 = (Act)(actslegs.get(planBasePos));
					Act act1 = (Act)(actslegs.get(planRunningPos));
					if (act0.getType()!=act1.getType()){
							
						Act actHelp = new Act ((Act)(actslegs.get(planBasePos)));
					
						actslegs.set(planBasePos, actslegs.get(planRunningPos));
						actslegs.set(planRunningPos, actHelp);
						
						positions[0] = planBasePos;
						return 0;
					}
				}
				positions[1] = planBasePos+4;
			}
			return 1;
		}
	}
	
	public int changeNumber (PlanomatXPlan basePlan, double weight, int [] positions, int [] actsToBeAdded){
				
		if(MatsimRandom.random.nextDouble()>=weight){
			
			// removing an activity, "cycling"
			
			if (positions[3]<(int)(basePlan.getActsLegs().size()/2)){
						
				if (basePlan.getActsLegs().size()==5){
					this.removeAct(1, basePlan);
					positions[3]++;
					return 0;
				}
				else if (basePlan.getActsLegs().size()>5){
					if(positions[1]==0){
						positions[1] = (int)(MatsimRandom.random.nextDouble()*((int)(basePlan.getActsLegs().size()/2)-1))+1;
						this.removeAct(positions[1], basePlan);
						
					}
					else if(positions[1]<=(int)(basePlan.getActsLegs().size()/2)-1){
						this.removeAct(positions[1], basePlan);
						
					}
					else {
						positions[1] = 1;
						this.removeAct(positions[1], basePlan);
					}
					positions[1]++;
					positions[3]++;
					return 0;
				}
				else return 1;
			}
			else return 1;
		}
		
		else{	
			
			// adding an activity, "cycling"
			
			if (positions[2]<=PlanomatXInitialiser.actTypes.size()+(PlanomatXInitialiser.actTypes.size()-1)*((int)(basePlan.getActsLegs().size()/2)-1)){
			
				if (positions[0]==0){
					positions[0] = 1;
					for (int i = 0; i < actsToBeAdded.length;i++){
						actsToBeAdded[i] = (int)(MatsimRandom.random.nextDouble()* PlanomatXInitialiser.actTypes.size());
					}
					this.insertAct(positions[0], actsToBeAdded, basePlan);
					
				}
				else if (positions[0]<=(int)(basePlan.getActsLegs().size()/2)){
					this.insertAct(positions[0], actsToBeAdded, basePlan);				
				}
				else {
					positions[0] = 1;
					this.insertAct(positions[0], actsToBeAdded, basePlan);
					
				}
				positions[0]++;
				positions[2]++;
				return 0;
			}
			return 1;
		}
	}
	
	public int changeType (PlanomatXPlan basePlan, int [] position, int[]actsToBeChanged){
		
		if (position[1]<=(PlanomatXInitialiser.actTypes.size()-1)*(((int)(basePlan.getActsLegs().size()/2))-1)){
			if (position[0]>basePlan.getActsLegs().size()/2-1)position[0] = 1;		
			
			Act act = (Act) basePlan.getActsLegs().get(position[0]*2);
			String type;
					
			do {
				type = PlanomatXInitialiser.actTypes.get(actsToBeChanged[position[0]]);
				actsToBeChanged[position[0]]++;
				if (actsToBeChanged[position[0]]>=PlanomatXInitialiser.actTypes.size()) actsToBeChanged[position[0]] = 0;
			} while (type.equals(act.getType()));
			
			act.setType(type);
			position[0]++;
			position[1]++;
			return 0;
		}
		return 1;
	}
	
	//////////////////////////////////////////////////////////////////////
	// Help methods 
	//////////////////////////////////////////////////////////////////////
	
	public boolean checkForNoNewSolutions (int[] notNewInNeighbourhood){
		boolean warningInner = true;
		boolean warningOuter = true;
		for (int x=0; x<notNewInNeighbourhood.length;x++){
			if (notNewInNeighbourhood[x]==0){
				warningInner = false;
			}
			if (!warningInner) warningOuter = false;
		}
		return warningOuter;
	}
	
	
	public boolean checkForTabuSolutions (ArrayList<PlanomatXPlan> tabuList, PlanomatXPlan[] neighbourhood, int[] notNewInNeighbourhood, int[] tabuInNeighbourhood){
		boolean warningInner = true;
		boolean warningOuter = true;
		for (int x=0; x<tabuInNeighbourhood.length;x++){	//go through all neighbourhood solutions
			if (notNewInNeighbourhood[x]==1) {
				tabuInNeighbourhood[x] = 1;
			}
			else {
				boolean warningTabu = false;
				for (int i = 0; i<tabuList.size();i++){		//compare each neighbourhood solution with all tabu solutions
					if (checkForEquality(tabuList.get(tabuList.size()-1-i), neighbourhood[x])) {
						warningTabu = true;
						break;
					}
				}
				if (warningTabu) {
					tabuInNeighbourhood[x] = 1;
				}
				else {
					tabuInNeighbourhood[x] = 0;
					warningInner = false;
				}
			}
			if (!warningInner) warningOuter = false;
		}
		return warningOuter;
	}
	
	public void checkForScoredSolution (PlanomatXPlan [] neighbourhood, int [] tabuInNeighbourhood, int [] scoredInNeighbourhood,
				ArrayList<PlanomatXPlan> solution3, ArrayList<PlanomatXPlan> solution5, ArrayList<PlanomatXPlan> solution7,
				ArrayList<PlanomatXPlan> solution9, ArrayList<PlanomatXPlan> solution11, ArrayList<PlanomatXPlan> solution13,
				ArrayList<PlanomatXPlan> solutionLong, ArrayList<PlanomatXPlan> nonTabuNeighbourhood){
		for (int x = 0; x<scoredInNeighbourhood.length; x++){
			if (tabuInNeighbourhood[x]==1){
				scoredInNeighbourhood[x]=1;
			}
			else {
				if (neighbourhood[x].getActsLegs().size()==3){
					scoredInNeighbourhood[x]=0;
					for (int i = 0; i<solution3.size();i++) {
						if (checkForEquality3(neighbourhood[x], solution3.get(solution3.size()-1-i))){
							nonTabuNeighbourhood.add(solution3.get(solution3.size()-1-i));
							neighbourhood[x].setScore(solution3.get(solution3.size()-1-i).getScore());
							scoredInNeighbourhood[x]=1;
							log.info("Solution3 recycled!");
							break;
						}
					}					
				}
				else if (neighbourhood[x].getActsLegs().size()==5){
					scoredInNeighbourhood[x]=0;
					for (int i = 0; i<solution5.size();i++) {
						if (checkForEquality3(neighbourhood[x], solution5.get(solution5.size()-1-i))){
							nonTabuNeighbourhood.add(solution5.get(solution5.size()-1-i));
							neighbourhood[x].setScore(solution5.get(solution5.size()-1-i).getScore());
							scoredInNeighbourhood[x]=1;
							log.info("Solution5 recycled!");
							break;
						}
					}
					
				}
				else if (neighbourhood[x].getActsLegs().size()==7){
					scoredInNeighbourhood[x]=0;
					for (int i = 0; i<solution7.size();i++) {
						if (checkForEquality3(neighbourhood[x], solution7.get(solution7.size()-1-i))){
							nonTabuNeighbourhood.add(solution7.get(solution7.size()-1-i));
							neighbourhood[x].setScore(solution7.get(solution7.size()-1-i).getScore());
							scoredInNeighbourhood[x]=1;
							log.info("Solution7 recycled!");
							break;
						}
					}
					
				}
				else if (neighbourhood[x].getActsLegs().size()==9){
					scoredInNeighbourhood[x]=0;
					for (int i = 0; i<solution9.size();i++) {
						if (checkForEquality3(neighbourhood[x], solution9.get(solution9.size()-1-i))){
							nonTabuNeighbourhood.add(solution9.get(solution9.size()-1-i));
							neighbourhood[x].setScore(solution9.get(solution9.size()-1-i).getScore());
							scoredInNeighbourhood[x]=1;
							log.info("Solution9 recycled!");
							break;
						}
					}
					
				}
				else if (neighbourhood[x].getActsLegs().size()==11){
					scoredInNeighbourhood[x]=0;
					for (int i = 0; i<solution11.size();i++) {
						if (checkForEquality3(neighbourhood[x], solution11.get(solution11.size()-1-i))){
							nonTabuNeighbourhood.add(solution11.get(solution11.size()-1-i));
							neighbourhood[x].setScore(solution11.get(solution11.size()-1-i).getScore());
							scoredInNeighbourhood[x]=1;
							log.info("Solution11 recycled!");
							break;
						}
					}
					
				}
				else if (neighbourhood[x].getActsLegs().size()==13){
					scoredInNeighbourhood[x]=0;
					for (int i = 0; i<solution13.size();i++) {
						if (checkForEquality3(neighbourhood[x], solution13.get(solution13.size()-1-i))){
							nonTabuNeighbourhood.add(solution13.get(solution13.size()-1-i));
							neighbourhood[x].setScore(solution13.get(solution13.size()-1-i).getScore());
							scoredInNeighbourhood[x]=1;
							log.info("Solution13 recycled!");
							break;
						}
					}
					
				}
				else {
					for (int i = 0; i<solutionLong.size();i++) {
						scoredInNeighbourhood[x]=0;
						if (checkForEquality(neighbourhood[x], solutionLong.get(solutionLong.size()-1-i))){
							nonTabuNeighbourhood.add(solutionLong.get(solutionLong.size()-1-i));
							neighbourhood[x].setScore(solutionLong.get(solutionLong.size()-1-i).getScore());
							scoredInNeighbourhood[x]=1;
							log.info("SolutionLong recycled!");
							break;
						}
					}
				}
			}
		}
	}
	
	// Method that returns true if two plans feature the same activity order, or false otherwise
	public boolean checkForEquality (PlanomatXPlan plan1, PlanomatXPlan plan2){
		
		if (plan1.getActsLegs().size()!=plan2.getActsLegs().size()){
		
			return false;
		}
		else{
			ArrayList<String> acts1 = new ArrayList<String> ();
			ArrayList<String> acts2 = new ArrayList<String> ();
			for (int i = 0;i<plan1.getActsLegs().size();i=i+2){
				acts1.add(((Act)(plan1.getActsLegs().get(i))).getType().toString());				
			}
			for (int i = 0;i<plan2.getActsLegs().size();i=i+2){
				acts2.add(((Act)(plan2.getActsLegs().get(i))).getType().toString());				
			}
		
			return (acts1.equals(acts2));
		}
	}	
	
	// Same functionality as above but apparently slightly slower
	public boolean checkForEquality2 (PlanomatXPlan plan1, PlanomatXPlan plan2){
		if (plan1.getActsLegs().size()!=plan2.getActsLegs().size()){
			return false;
		}
		else {
			boolean warning = true;
			for (int i = 0; i<plan1.getActsLegs().size();i=i+2){
				if (!((Act)(plan1.getActsLegs().get(i))).getType().toString().equals(((Act)(plan2.getActsLegs().get(i))).getType().toString())){
					warning = false;
					break;
				}
			}
			return warning;
		}
	}
	
	// Same functionality as above but without length check (because not required by some calling methods, saves calculation time)
	public boolean checkForEquality3 (PlanomatXPlan plan1, PlanomatXPlan plan2){
		
		ArrayList<String> acts1 = new ArrayList<String> ();
		ArrayList<String> acts2 = new ArrayList<String> ();
		for (int i = 0;i<plan1.getActsLegs().size();i=i+2){
			acts1.add(((Act)(plan1.getActsLegs().get(i))).getType().toString());				
		}
		for (int i = 0;i<plan2.getActsLegs().size();i=i+2){
			acts2.add(((Act)(plan2.getActsLegs().get(i))).getType().toString());				
		}	
		return (acts1.equals(acts2));
	}	
	
	
	// Inserts an activity of random type at the given position with the given type of act (but checks whether type is allowed)
	public void insertAct (int position, int [] actToBeAdded, PlanomatXPlan basePlan){
		
		ArrayList<Object> actslegs = basePlan.getActsLegs();
		Act actHelp = new Act ((Act)(actslegs.get((position*2)-2)));
		actHelp.setDur(0);
		
		if (actToBeAdded[position]>=PlanomatXInitialiser.actTypes.size()) actToBeAdded[position] = 0;
		
		if (position!=1){
			if (PlanomatXInitialiser.actTypes.get(actToBeAdded[position]).toString().equals(((Act)(basePlan.getActsLegs().get(position*2-2))).getType().toString())){
				if (actToBeAdded[position]+1>=PlanomatXInitialiser.actTypes.size()){
					actToBeAdded[position] = 0;
				}
				else {
					actToBeAdded[position]++;
				}
			}
		}
		actHelp.setType(PlanomatXInitialiser.actTypes.get(actToBeAdded[position]));
		actToBeAdded[position]++;

		Leg legHelp = new Leg ((Leg)(actslegs.get((position*2)-1)));
		actslegs.add(position*2, legHelp);
		actslegs.add(position*2, actHelp);
	}
	
	// Removes the activity at the given position
	public void removeAct (int position, PlanomatXPlan basePlan){
		ArrayList<Object> actslegs = basePlan.getActsLegs();
		actslegs.remove(position*2);
		actslegs.remove(position*2);
	}
}
	
