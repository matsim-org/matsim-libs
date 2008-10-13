/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatX13.java
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
import org.matsim.planomat.*;
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
import java.util.LinkedList;
import java.io.*;




/**
 * @author Matthias Feil
 * Like PlanomatX11 but first trial to implement the hybrid genetic algorithm approahc.
 */

public class PlanomatX13 implements org.matsim.population.algorithms.PlanAlgorithm { 
	
	private final int						NEIGHBOURHOOD_SIZE, MAX_ITERATIONS;
	private final double					WEIGHT_CHANGE_ORDER, WEIGHT_CHANGE_NUMBER;
	private final double 					WEIGHT_INC_NUMBER;
	private final int						POPULATION_SIZE;
	private final PlanAlgorithm 			planomatAlgorithm;
	private final PlansCalcRouteLandmarks 	router;
	private final PlanScorer 				scorer;
	private static final Logger 			log = Logger.getLogger(PlanomatX13.class);
	
	//////////////////////////////////////////////////////////////////////
	// Constructor
	//////////////////////////////////////////////////////////////////////
		
	public PlanomatX13 (LegTravelTimeEstimator legTravelTimeEstimator, NetworkLayer network, TravelCost costCalculator,
			TravelTime timeCalculator, PreProcessLandmarks commonRouterDatafinal, ScoringFunctionFactory factory) {

		this.planomatAlgorithm 		= new PlanOptimizeTimes (legTravelTimeEstimator);
		this.router 				= new PlansCalcRouteLandmarks (network, commonRouterDatafinal, costCalculator, timeCalculator);
		this.scorer 				= new PlanomatXPlanScorer (factory);
		
		this.NEIGHBOURHOOD_SIZE 	= 20;				//TODO @MF: constants to be configured externally, sum must be smaller than or equal to 1.0
		this.WEIGHT_CHANGE_ORDER 	= 0.2; 
		this.WEIGHT_CHANGE_NUMBER 	= 0.6;
		this.WEIGHT_INC_NUMBER 		= 0.5; 				//Weighing whether adding or removing activities in change number method.
		this.MAX_ITERATIONS 		= 10;
		this.POPULATION_SIZE		= 2;
		
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
		int [][] notNewInNeighbourhood 					= new int [2][NEIGHBOURHOOD_SIZE];
		int [][] tabuInNeighbourhood 					= new int [2][NEIGHBOURHOOD_SIZE];
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
		LinkedList<PlanomatXPlan> population			= new LinkedList<PlanomatXPlan>();
		PlanomatXPlan [][] offspring					= new PlanomatXPlan [2][NEIGHBOURHOOD_SIZE];
		
		double [] xs;
		double [] ys = new double [MAX_ITERATIONS];
		
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
				
		// Copy the plan into the population list
		
		population.add(new PlanomatXPlan(plan.getPerson()));
		population.getFirst().copyPlan(plan);			
		
		
		// Copy the plan into the first array of the offspring
		for (int i = 0;i<offspring[0].length;i++){
			offspring[0][i] = new PlanomatXPlan (plan.getPerson());
			offspring[0][i].copyPlan(plan);	
		}
		
		// Write the given plan into the tabuList
		//tabuList.add(neighbourhood[NEIGHBOURHOOD_SIZE]);
		//stream.println("0\t"+neighbourhood[NEIGHBOURHOOD_SIZE].getScore());
		
		// Do Tabu Search iterations
		int currentIteration;
		int offspringSize = 1;
		for (currentIteration = 1; currentIteration<=MAX_ITERATIONS;currentIteration++){
			stream.println("Iteration "+currentIteration);
			
			for (int i = 0;i<population.size();i++){
				stream.print("Population "+i+"\t\t\t");
				for (int z= 0;z<population.get(i).getActsLegs().size();z=z+2){
					stream.print(((Act)population.get(i).getActsLegs().get(z)).getType()+"\t");
				}
				stream.println();
			}
			
			// Do crossover 
			if (currentIteration>1){
				offspring = this.doCrossover(population, NEIGHBOURHOOD_SIZE);
			}
						
			// Define the neighbourhood		
			for (int i = 0;i<offspringSize;i++){
				stream.print("Offspring "+i+"\t\t\t");
				for (int z= 0;z<offspring[i][0].getActsLegs().size();z=z+2){
					stream.print(((Act)offspring[i][0].getActsLegs().get(z)).getType()+"\t");
				}
				stream.println();
				this.createNeighbourhood(offspring[i], notNewInNeighbourhood[i]);	
			}
			
			// Check whether differing plans are tabu
			boolean warningTabu = true;
			for (int i = 0;i<offspringSize;i++){
				boolean warning = this.checkForScoredSolution(offspring[i], notNewInNeighbourhood[i], tabuInNeighbourhood[i], 
							solution3, solution5, solution7, solution9,
							solution11, solution13, solutionLong);
				if (!warning) warningTabu = false;
			}
			if (warningTabu){
				log.info("No non-tabu solutions availabe for person "+plan.getPerson().getId()+" at iteration "+currentIteration);
				break; 
			}
			// Route, optimize and score all non-tabu plans, write them into list nonTabuNeighbourhood and sort the list
			for (int y=0;y<offspringSize;y++){
				for (int x=0; x<NEIGHBOURHOOD_SIZE;x++){
					if(tabuInNeighbourhood[y][x]==0){
						
						//Routing
						this.router.run(offspring[y][x]);
											
						//Optimizing the start times
						numberPlanomatCalls++;
						long planomatStartTime = System.currentTimeMillis();
						this.planomatAlgorithm.run (offspring[y][x]); //Calling standard Planomat to optimise start times and mode choice
						planomatRunTime += (System.currentTimeMillis()-planomatStartTime);
						
						// Scoring
						offspring[y][x].setScore(scorer.getScore(offspring[y][x]));
						nonTabuNeighbourhood.add(offspring[y][x]);
						
						// Write the solution into a list so that it can be retrieved for later iterations
						PlanomatXPlan solution = new PlanomatXPlan (offspring[y][x].getPerson());
						solution.copyPlan(offspring[y][x]);
						
						if (solution.getActsLegs().size()==3) solution3.add(solution);
						else if (solution.getActsLegs().size()==5) solution5.add(solution);
						else if (solution.getActsLegs().size()==7) solution7.add(solution);
						else if (solution.getActsLegs().size()==9) solution9.add(solution);
						else if (solution.getActsLegs().size()==11) solution11.add(solution);
						else if (solution.getActsLegs().size()==13) solution13.add(solution);
						else solutionLong.add(solution);
					}
					stream.print(offspring[y][x].getScore()+"\t"+notNewInNeighbourhood[y][x]+"\t"+tabuInNeighbourhood[y][x]+"\t");
					
					//stream.print(scoredInNeighbourhood[x]+"\t");
					for (int z= 0;z<offspring[y][x].getActsLegs().size();z=z+2){
						stream.print(((Act)offspring[y][x].getActsLegs().get(z)).getType()+"\t");
					}
					stream.println();
				}
			}
			// Find best non-tabu plan. Becomes this iteration's solution.
			java.util.Collections.sort(nonTabuNeighbourhood);
			//PlanomatXPlan bestIterSolution = new PlanomatXPlan (nonTabuNeighbourhood.get(nonTabuNeighbourhood.size()-1).getPerson());
			//bestIterSolution.copyPlan(nonTabuNeighbourhood.get(nonTabuNeighbourhood.size()-1));
			//tabuList.add(bestIterSolution);
			
			// Statistics
			stream.println("Iteration "+currentIteration+"\t"+nonTabuNeighbourhood.get(nonTabuNeighbourhood.size()-1).getScore());
			//streamOverview.println(nonTabuNeighbourhood.get(nonTabuNeighbourhood.size()-1).getScore());
			ys[currentIteration-1]=nonTabuNeighbourhood.get(nonTabuNeighbourhood.size()-1).getScore();
			

			// Write this iteration's solution into all population fields for the next iteration
			
			outerLoop:
			for (int m = 0;m<Math.min(nonTabuNeighbourhood.size(),POPULATION_SIZE); m++){
				for (int k = m;k<POPULATION_SIZE; k++){
					if ((nonTabuNeighbourhood.get(nonTabuNeighbourhood.size()-(m+1)).getScore())<=population.get(k).getScore()){
						break outerLoop;
					}
					else {
						population.add(k, new PlanomatXPlan (nonTabuNeighbourhood.get(nonTabuNeighbourhood.size()-(m+1)).getPerson()));
						population.get(k).copyPlan(nonTabuNeighbourhood.get(nonTabuNeighbourhood.size()-(m+1)));
						population.removeLast();
						break;
					}
				}
			}
			
			if (this.MAX_ITERATIONS==currentIteration){
				log.info("Tabu Search regularly finished for person "+plan.getPerson().getId()+" at iteration "+currentIteration);	
			}
			else {
				// Reset the nonTabuNeighbourhood list
				nonTabuNeighbourhood.clear();
			}
			if (currentIteration==1) offspringSize++;
				
		}
		
		// Update the plan with the final solution 		
		//java.util.Collections.sort(tabuList);
		stream.println("Selected solution\t"+population.getFirst().getScore());
		ArrayList<Object> al = plan.getActsLegs();
		
		xs = new double [currentIteration];
		log.info("Finale actslegs für Person "+population.getFirst().getPerson().getId()+": "+population.getFirst().getActsLegs());
		
		for (int i = 0;i<xs.length;i++)xs[i]=i+1;
		
		if(al.size()>population.getFirst().getActsLegs().size()){ 
			int i;
			for (i = 0; i<population.getFirst().getActsLegs().size();i++){
				al.remove(i);
				al.add(i, population.getFirst().getActsLegs().get(i));	
			}
			for (int j = i; j<al.size();j=j+0){
				al.remove(j);
			}
		}
		else if(al.size()<population.getFirst().getActsLegs().size()){
			int i;
			for (i = 0; i<al.size();i++){
				al.remove(i);
				al.add(i, population.getFirst().getActsLegs().get(i));	
			}
			for (int j = i; j<population.getFirst().getActsLegs().size();j++){			
				al.add(j, population.getFirst().getActsLegs().get(j));
			}
		}
		else {
			for (int i = 0; i<al.size();i++){
			al.remove(i);
			al.add(i, population.getFirst().getActsLegs().get(i));	
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
	
	private PlanomatXPlan [][] doCrossover (LinkedList<PlanomatXPlan> population, int neighbourhoodSize){
		PlanomatXPlan [][] offspring = new PlanomatXPlan [2][neighbourhoodSize];
		offspring[0][0]= new PlanomatXPlan(population.getFirst().getPerson());
		offspring[0][0].copyPlan(population.getFirst());	
		offspring[1][0]= new PlanomatXPlan(population.getLast().getPerson());
		offspring[1][0].copyPlan(population.getLast());
		
		int breakpoint;
		if (population.getFirst().getActsLegs().size()<=population.getLast().getActsLegs().size()){
			breakpoint =((int)((int)((population.getFirst().getActsLegs().size()-4)/2)*MatsimRandom.random.nextDouble())+1)*2+2;
		}
		else {
			breakpoint =((int)((int)((population.getLast().getActsLegs().size()-4)/2)*MatsimRandom.random.nextDouble())+1)*2+2;
		}
		ArrayList<Object> child0 = offspring[0][0].getActsLegs();
		for (int i = breakpoint;i<offspring[0][0].getActsLegs().size();i=i+0){
			child0.remove(i);
		}
		for (int i = breakpoint;i<population.getLast().getActsLegs().size();i++){
			child0.add(population.getLast().getActsLegs().get(i));
			if (i%2==0) {
				((Act)child0.get(i)).setStartTime(((Act)child0.get(breakpoint-2)).getStartTime());
				((Act)child0.get(i)).setDur(0);
			}
		}
		this.router.run(offspring[0][0]);
		this.planomatAlgorithm.run(offspring[0][0]);
		log.info("Nach crossover 1 für Person "+offspring[0][0].getPerson().getId()+": "+offspring[0][0].getActsLegs());
		
		ArrayList<Object> child1 = offspring[1][0].getActsLegs();
		for (int i = breakpoint;i<offspring[1][0].getActsLegs().size();i=i+0){
			child1.remove(i);
		}
		for (int i = breakpoint;i<population.getFirst().getActsLegs().size();i++){
			child1.add(population.getFirst().getActsLegs().get(i));
			if (i%2==0) {
				((Act)child1.get(i)).setStartTime(((Act)child1.get(breakpoint-2)).getStartTime());
				((Act)child1.get(i)).setDur(0);
			}
		}
		this.router.run(offspring[1][0]);
		this.planomatAlgorithm.run(offspring[1][0]);
		log.info("Nach crossover 2 für Person "+offspring[1][0].getPerson().getId()+": "+offspring[1][0].getActsLegs());
		
		for (int i = 0;i<offspring.length;i++){
			for (int j = 1;j<offspring[i].length;j++){
				offspring[i][j]= new PlanomatXPlan(offspring[i][0].getPerson());
				offspring[i][j].copyPlan(offspring[i][0]);	
			}
		}
		return offspring;
	}
	
	
	
	private void createNeighbourhood (PlanomatXPlan [] neighbourhood, int[] notNewInNeighbourhood) {
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
			
	
	
	private int changeOrder (PlanomatXPlan basePlan, int [] positions){
	
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
	
	private int changeNumber (PlanomatXPlan basePlan, double weight, int [] positions, int [] actsToBeAdded){
				
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
	
	private int changeType (PlanomatXPlan basePlan, int [] position, int[]actsToBeChanged){
		
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
	
	private boolean checkForNoNewSolutions (int[] notNewInNeighbourhood){
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
	
	
	private boolean checkForTabuSolutions (ArrayList<PlanomatXPlan> tabuList, PlanomatXPlan[] neighbourhood, int[] notNewInNeighbourhood, int[] tabuInNeighbourhood){
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
	
	private boolean checkForScoredSolution (PlanomatXPlan [] neighbourhood, int [] notNewInNeighbourhood, int [] tabuInNeighbourhood,
				ArrayList<PlanomatXPlan> solution3, ArrayList<PlanomatXPlan> solution5, ArrayList<PlanomatXPlan> solution7,
				ArrayList<PlanomatXPlan> solution9, ArrayList<PlanomatXPlan> solution11, ArrayList<PlanomatXPlan> solution13,
				ArrayList<PlanomatXPlan> solutionLong){
		
		boolean warningOuter = true;
		
		for (int x = 0; x<tabuInNeighbourhood.length; x++){
			if (notNewInNeighbourhood[x]==1){
				tabuInNeighbourhood[x]=1;
			}
			else {
				if (neighbourhood[x].getActsLegs().size()==3){
					tabuInNeighbourhood[x]=0;
					for (int i = 0; i<solution3.size();i++) {
						if (checkForEquality3(neighbourhood[x], solution3.get(solution3.size()-1-i))){
							
							neighbourhood[x].setScore(solution3.get(solution3.size()-1-i).getScore());
							tabuInNeighbourhood[x]=1;
							log.info("Solution3 recycled!");
							
							break;
						}
					}					
				}
				else if (neighbourhood[x].getActsLegs().size()==5){
					tabuInNeighbourhood[x]=0;
					for (int i = 0; i<solution5.size();i++) {
						if (checkForEquality3(neighbourhood[x], solution5.get(solution5.size()-1-i))){
							
							neighbourhood[x].setScore(solution5.get(solution5.size()-1-i).getScore());
							tabuInNeighbourhood[x]=1;
							log.info("Solution5 recycled!");
						
							break;
						}
					}
					
				}
				else if (neighbourhood[x].getActsLegs().size()==7){
					tabuInNeighbourhood[x]=0;
					for (int i = 0; i<solution7.size();i++) {
						if (checkForEquality3(neighbourhood[x], solution7.get(solution7.size()-1-i))){
							
							neighbourhood[x].setScore(solution7.get(solution7.size()-1-i).getScore());
							tabuInNeighbourhood[x]=1;
							log.info("Solution7 recycled!");
							
							break;
						}
					}
					
				}
				else if (neighbourhood[x].getActsLegs().size()==9){
					tabuInNeighbourhood[x]=0;
					for (int i = 0; i<solution9.size();i++) {
						if (checkForEquality3(neighbourhood[x], solution9.get(solution9.size()-1-i))){
							
							neighbourhood[x].setScore(solution9.get(solution9.size()-1-i).getScore());
							tabuInNeighbourhood[x]=1;
							log.info("Solution9 recycled!");
							
							break;
						}
					}
					
				}
				else if (neighbourhood[x].getActsLegs().size()==11){
					tabuInNeighbourhood[x]=0;
					for (int i = 0; i<solution11.size();i++) {
						if (checkForEquality3(neighbourhood[x], solution11.get(solution11.size()-1-i))){
						
							neighbourhood[x].setScore(solution11.get(solution11.size()-1-i).getScore());
							tabuInNeighbourhood[x]=1;
							log.info("Solution11 recycled!");
							
							break;
						}
					}
					
				}
				else if (neighbourhood[x].getActsLegs().size()==13){
					tabuInNeighbourhood[x]=0;
					for (int i = 0; i<solution13.size();i++) {
						if (checkForEquality3(neighbourhood[x], solution13.get(solution13.size()-1-i))){
						
							neighbourhood[x].setScore(solution13.get(solution13.size()-1-i).getScore());
							tabuInNeighbourhood[x]=1;
							log.info("Solution13 recycled!");
							
							break;
						}
					}
					
				}
				else {
					for (int i = 0; i<solutionLong.size();i++) {
						tabuInNeighbourhood[x]=0;
						if (checkForEquality(neighbourhood[x], solutionLong.get(solutionLong.size()-1-i))){
					
							neighbourhood[x].setScore(solutionLong.get(solutionLong.size()-1-i).getScore());
							tabuInNeighbourhood[x]=1;
							log.info("SolutionLong recycled!");
							
							break;
						}
					}
				} 
			}
			if (tabuInNeighbourhood[x]==0) warningOuter=false;
		}
		return warningOuter;
	}
	
	// Method that returns true if two plans feature the same activity order, or false otherwise
	private boolean checkForEquality (PlanomatXPlan plan1, PlanomatXPlan plan2){
		
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
	private boolean checkForEquality2 (PlanomatXPlan plan1, PlanomatXPlan plan2){
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
	private boolean checkForEquality3 (PlanomatXPlan plan1, PlanomatXPlan plan2){
		
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
	private void insertAct (int position, int [] actToBeAdded, PlanomatXPlan basePlan){
		
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
	private void removeAct (int position, PlanomatXPlan basePlan){
		ArrayList<Object> actslegs = basePlan.getActsLegs();
		actslegs.remove(position*2);
		actslegs.remove(position*2);
	}
}
	
